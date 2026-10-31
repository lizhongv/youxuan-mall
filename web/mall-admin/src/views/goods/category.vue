<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowRef } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { categoryApi } from '@/api/goods'
import type { Category, ID, Switch } from '@/types'
import { useUserStore } from '@/stores/user'
import { ENABLE_STATUS } from '@/utils/constants'
import { confirmDelete, toast } from '@/utils/message'
import ImageUpload from '@/components/ImageUpload.vue'
import EmptyState from '@/components/EmptyState.vue'

const user = useUserStore()
const canEdit = computed(() => user.hasPerm('goods:category:edit'))

const tree = shallowRef<Category[]>([])
const loading = ref(false)
const keyword = ref('')
const expandAll = ref(true)
const tableKey = ref(0)

async function load() {
  loading.value = true
  try {
    tree.value = (await categoryApi.tree()) || []
  } finally {
    loading.value = false
  }
}
onMounted(load)

const isRoot = (c: Pick<Category, 'parentId'>) => !c.parentId || c.parentId === '0'

/** 关键字过滤：保留命中节点及其父节点 */
const filtered = computed(() => {
  const k = keyword.value.trim()
  if (!k) return tree.value
  return tree.value
    .map((c) => {
      const children = (c.children || []).filter((s) => s.name.includes(k))
      return c.name.includes(k) || children.length ? { ...c, children: c.name.includes(k) ? c.children : children } : null
    })
    .filter(Boolean) as Category[]
})

function toggleExpand() {
  expandAll.value = !expandAll.value
  tableKey.value++
}

// ---------- 表单 ----------
const visible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<ID>('')
const form = reactive({ parentId: '0' as ID, name: '', icon: '', sort: 0, status: 1 as Switch })
const rules: FormRules = {
  name: [
    { required: true, message: '请输入类目名称', trigger: 'blur' },
    { max: 32, message: '不超过 32 个字符', trigger: 'blur' },
  ],
}
const parentOptions = computed(() => tree.value.filter((c) => c.id !== editingId.value))
/** 最多两级：已有子类目的一级类目不能再挂到别的类目下 */
const parentLocked = computed(() => {
  if (!editingId.value) return false
  const self = tree.value.find((c) => c.id === editingId.value)
  return !!self?.children?.length
})

function open(row?: Category, parent?: Category) {
  editingId.value = row?.id || ''
  Object.assign(form, {
    parentId: row ? row.parentId || '0' : parent?.id || '0',
    name: row?.name || '',
    icon: row?.icon || '',
    sort: row?.sort ?? 0,
    status: row?.status ?? 1,
  })
  visible.value = true
}

async function submit() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  saving.value = true
  try {
    if (editingId.value) await categoryApi.update(editingId.value, { ...form })
    else await categoryApi.create({ ...form })
    toast.success('保存成功')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: Category, v: string | number | boolean) {
  await categoryApi.update(row.id, { ...row, children: undefined, status: (v ? 1 : 0) as Switch })
  row.status = (v ? 1 : 0) as Switch
  toast.success(v ? '已启用' : '已停用')
}

async function remove(row: Category) {
  if (row.children?.length) return toast.warning('请先删除该类目下的子类目')
  if (await confirmDelete(row.name, () => categoryApi.remove(row.id))) load()
}
</script>

<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <div class="toolbar__left">
          <el-button v-perm="'goods:category:edit'" type="primary" :icon="'Plus'" @click="open()">新增一级类目</el-button>
          <el-button :icon="expandAll ? 'Fold' : 'Expand'" @click="toggleExpand">{{ expandAll ? '全部折叠' : '全部展开' }}</el-button>
        </div>
        <div class="toolbar__right">
          <el-input v-model="keyword" placeholder="搜索类目名称" clearable :prefix-icon="'Search'" style="width: 220px" />
          <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="load" /></el-tooltip>
        </div>
      </div>
      <el-table :key="tableKey" v-loading="loading" :data="filtered" row-key="id" :default-expand-all="expandAll" :tree-props="{ children: 'children' }">
        <el-table-column label="类目名称" min-width="260">
          <template #default="{ row }: { row: any }">
            <span class="cat-name">
              <el-image v-if="row.icon" :src="row.icon" fit="cover" class="cat-name__icon" />
              <span v-else class="cat-name__icon cat-name__icon--empty"><el-icon><Picture /></el-icon></span>
              <span>{{ row.name }}</span>
              <el-tag v-if="isRoot(row)" size="small" type="info" effect="plain">一级</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="子类目数" width="100" align="center">
          <template #default="{ row }: { row: any }">{{ isRoot(row) ? row.children?.length || 0 : '-' }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }: { row: any }">
            <el-switch :model-value="row.status === 1" :disabled="!canEdit" @change="(v: string | number | boolean) => toggleStatus(row, v)" />
          </template>
        </el-table-column>
        <el-table-column v-if="canEdit" label="操作" width="220" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button v-if="isRoot(row)" link type="primary" :icon="'Plus'" @click="open(undefined, row)">子类目</el-button>
            <el-button link type="primary" @click="open(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState v-if="!loading" text="暂无类目" /><div v-else class="table-placeholder" /></template>
      </el-table>
    </div>

    <el-dialog v-model="visible" :title="editingId ? '编辑类目' : '新增类目'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级类目">
          <el-select v-model="form.parentId" :disabled="parentLocked" class="w-full">
            <el-option label="无（作为一级类目）" value="0" />
            <el-option v-for="c in parentOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <div v-if="parentLocked" class="form-tip">该类目下已有子类目，类目最多两级，不能调整上级</div>
        </el-form-item>
        <el-form-item label="类目名称" prop="name">
          <el-input v-model="form.name" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="类目图标">
          <ImageUpload v-model="form.icon" biz-type="category" :size="80" tip="建议 200×200 透明 PNG，用于商城分类入口" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
          <span class="form-tip" style="margin-left: 10px">数字越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio v-for="o in ENABLE_STATUS" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.cat-name {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  vertical-align: middle;
  &__icon {
    width: 32px;
    height: 32px;
    border-radius: var(--app-radius-sm);
    flex: none;
    border: 1px solid var(--app-border);
    &--empty {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      color: var(--app-text-placeholder);
      background: var(--app-card-bg-muted);
    }
  }
}
</style>
