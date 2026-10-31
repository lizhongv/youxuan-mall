<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowRef } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { menuApi } from '@/api/system'
import type { ID, Menu, MenuType, Switch } from '@/types'
import { useUserStore } from '@/stores/user'
import { ENABLE_STATUS, MENU_TYPE } from '@/utils/constants'
import { confirmDelete, toast } from '@/utils/message'
import StatusTag from '@/components/StatusTag.vue'
import IconSelect from '@/components/IconSelect.vue'
import EmptyState from '@/components/EmptyState.vue'

const user = useUserStore()
const canEdit = computed(() => user.hasPerm('system:menu:edit'))

const tree = shallowRef<Menu[]>([])
const loading = ref(false)
const keyword = ref('')
const expandAll = ref(false)
const tableKey = ref(0)

async function load() {
  loading.value = true
  try {
    tree.value = (await menuApi.tree()) || []
  } finally {
    loading.value = false
  }
}
onMounted(load)

function filterTree(nodes: Menu[], k: string): Menu[] {
  return nodes
    .map((n) => {
      const children = filterTree(n.children || [], k)
      const hit = n.name.includes(k) || n.permission?.includes(k) || n.path?.includes(k)
      return hit || children.length ? { ...n, children: hit ? n.children : children } : null
    })
    .filter(Boolean) as Menu[]
}

const filtered = computed(() => {
  const k = keyword.value.trim()
  return k ? filterTree(tree.value, k) : tree.value
})

function toggleExpand() {
  expandAll.value = !expandAll.value
  tableKey.value++
}

/** 上级菜单只能是目录或菜单（按钮下不能再挂节点） */
function toParentOptions(nodes: Menu[], excludeId: ID): Menu[] {
  return nodes
    .filter((n) => n.type !== 'BUTTON' && n.id !== excludeId)
    .map((n) => ({ ...n, children: toParentOptions(n.children || [], excludeId) }))
}

// ---------------- 表单 ----------------
const visible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<ID>('')
const form = reactive({
  parentId: '0' as ID,
  type: 'MENU' as MenuType,
  name: '',
  path: '',
  component: '',
  icon: '',
  permission: '',
  sort: 0,
  visible: 1 as Switch,
  status: 1 as Switch,
})

const parentOptions = computed(() => [{ id: '0', name: '根目录', children: toParentOptions(tree.value, editingId.value) }])

const rules = computed<FormRules>(() => ({
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  path: form.type !== 'BUTTON' ? [{ required: true, message: '请输入路由地址', trigger: 'blur' }] : [],
  permission:
    form.type === 'BUTTON'
      ? [
          { required: true, message: '按钮必须填写权限标识', trigger: 'blur' },
          { pattern: /^[a-z]+(:[a-z-]+)+$/, message: '格式如 goods:goods:edit', trigger: 'blur' },
        ]
      : [{ pattern: /^$|^[a-z]+(:[a-z-]+)+$/, message: '格式如 goods:goods:view', trigger: 'blur' }],
}))

function open(row?: Menu, parent?: Menu) {
  editingId.value = row?.id || ''
  const defaultType: MenuType = parent ? (parent.type === 'DIR' ? 'MENU' : 'BUTTON') : 'DIR'
  Object.assign(form, {
    parentId: row ? row.parentId || '0' : parent?.id || '0',
    type: row?.type || defaultType,
    name: row?.name || '',
    path: row?.path || '',
    component: row?.component || '',
    icon: row?.icon || '',
    permission: row?.permission || '',
    sort: row?.sort ?? 0,
    visible: row?.visible ?? 1,
    status: row?.status ?? 1,
  })
  visible.value = true
  setTimeout(() => formRef.value?.clearValidate())
}

async function submit() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  const payload: Partial<Menu> = { ...form }
  // 类型联动：清理不适用的字段
  if (form.type === 'BUTTON') {
    Object.assign(payload, { path: '', component: '', icon: '', visible: 1 })
  } else if (form.type === 'DIR') {
    payload.component = ''
  }
  saving.value = true
  try {
    if (editingId.value) await menuApi.update(editingId.value, payload)
    else await menuApi.create(payload)
    toast.success('保存成功')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Menu) {
  if (row.children?.length) return toast.warning('请先删除下级菜单')
  if (await confirmDelete(row.name, () => menuApi.remove(row.id))) load()
}
</script>

<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <div class="toolbar__left">
          <el-button v-perm="'system:menu:edit'" type="primary" :icon="'Plus'" @click="open()">新增菜单</el-button>
          <el-button :icon="expandAll ? 'Fold' : 'Expand'" @click="toggleExpand">{{ expandAll ? '全部折叠' : '全部展开' }}</el-button>
        </div>
        <div class="toolbar__right">
          <el-input v-model="keyword" placeholder="名称 / 权限标识 / 路由" clearable :prefix-icon="'Search'" style="width: 240px" />
          <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="load" /></el-tooltip>
        </div>
      </div>
      <el-table :key="tableKey" v-loading="loading" :data="filtered" row-key="id" :default-expand-all="expandAll || !!keyword">
        <el-table-column label="菜单名称" min-width="200">
          <template #default="{ row }: { row: any }">
            <span class="menu-name">
              <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
              {{ row.name }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }: { row: any }"><StatusTag :value="row.type" :options="MENU_TYPE" :dot="false" /></template>
        </el-table-column>
        <el-table-column label="路由 / 组件" min-width="190">
          <template #default="{ row }: { row: any }">
            <div class="mono">{{ row.path || '-' }}</div>
            <div v-if="row.component" class="mono text-secondary sub">{{ row.component }}</div>
          </template>
        </el-table-column>
        <el-table-column label="权限标识" min-width="160">
          <template #default="{ row }: { row: any }"><span class="mono text-secondary">{{ row.permission || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="排序" prop="sort" width="70" align="center" />
        <el-table-column label="显示" width="70" align="center">
          <template #default="{ row }: { row: any }">
            <el-icon v-if="row.type === 'BUTTON'" class="text-secondary"><Minus /></el-icon>
            <el-icon v-else-if="row.visible" class="text-success"><View /></el-icon>
            <el-icon v-else class="text-secondary"><Hide /></el-icon>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }: { row: any }"><StatusTag :value="row.status" :options="ENABLE_STATUS" /></template>
        </el-table-column>
        <el-table-column v-if="canEdit" label="操作" width="200" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button v-if="row.type !== 'BUTTON'" link type="primary" :icon="'Plus'" @click="open(undefined, row)">新增</el-button>
            <el-button link type="primary" @click="open(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState v-if="!loading" text="暂无菜单" /><div v-else class="table-placeholder" /></template>
      </el-table>
    </div>

    <el-dialog v-model="visible" :title="editingId ? '编辑菜单' : '新增菜单'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            default-expand-all
            :render-after-expand="false"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="菜单类型">
          <el-radio-group v-model="form.type">
            <el-radio-button v-for="t in MENU_TYPE" :key="t.value" :value="t.value">{{ t.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item :label="form.type === 'BUTTON' ? '按钮名称' : '菜单名称'" prop="name">
              <el-input v-model="form.name" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.type !== 'BUTTON'" :xs="24" :sm="12">
            <el-form-item label="图标">
              <IconSelect v-model="form.icon" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.type !== 'BUTTON'" :xs="24" :sm="12">
            <el-form-item label="路由地址" prop="path">
              <el-input v-model="form.path" :placeholder="form.type === 'DIR' ? '/goods' : '/goods/list'" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.type === 'MENU'" :xs="24" :sm="12">
            <el-form-item label="组件路径">
              <el-input v-model="form.component" placeholder="goods/list" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.type !== 'DIR'" :xs="24" :sm="12">
            <el-form-item label="权限标识" prop="permission">
              <el-input v-model="form.permission" :placeholder="form.type === 'BUTTON' ? 'goods:goods:edit' : 'goods:goods:view'" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.type !== 'BUTTON'" :xs="24" :sm="12">
            <el-form-item label="是否显示">
              <el-radio-group v-model="form.visible">
                <el-radio :value="1">显示</el-radio>
                <el-radio :value="0">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="o in ENABLE_STATUS" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="后台使用静态路由：此处菜单用于角色授权与权限码管理，路由地址需与前端路由保持一致"
        />
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.menu-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  vertical-align: middle;
}
.sub {
  font-size: 12px;
}
</style>
