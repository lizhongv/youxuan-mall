<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { dictApi } from '@/api/system'
import type { Dict, ID, Switch } from '@/types'
import { useTable } from '@/composables/useTable'
import { clearDictCache } from '@/composables/useDict'
import { useUserStore } from '@/stores/user'
import { ENABLE_STATUS } from '@/utils/constants'
import { confirmDelete, toast } from '@/utils/message'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'

const user = useUserStore()
const canEdit = computed(() => user.hasPerm('system:dict:edit'))

/** 已知字典类型的中文说明（种子数据），其余类型直接显示编码 */
const TYPE_LABELS: Record<string, string> = {
  express_company: '快递公司',
  aftersale_reason: '售后原因',
  cancel_reason: '取消原因',
}

const types = ref<string[]>([])
const typeKeyword = ref('')
const typesLoading = ref(false)

const { query, list, total, page, size, loading, load, search, onPageChange, onSizeChange } = useTable(dictApi.page, { dictType: '', keyword: '' })

async function loadTypes() {
  typesLoading.value = true
  try {
    types.value = (await dictApi.types()) || []
  } finally {
    typesLoading.value = false
  }
}
onMounted(loadTypes)

const filteredTypes = computed(() => {
  const k = typeKeyword.value.trim().toLowerCase()
  return k ? types.value.filter((t) => t.toLowerCase().includes(k) || (TYPE_LABELS[t] || '').includes(k)) : types.value
})

function selectType(t: string) {
  query.dictType = query.dictType === t ? '' : t
  search()
}

// ---------------- 表单 ----------------
const visible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<ID>('')
const form = reactive({ dictType: '', label: '', value: '', sort: 0, status: 1 as Switch, remark: '' })
const rules: FormRules = {
  dictType: [
    { required: true, message: '请输入字典类型', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]{1,63}$/, message: '小写字母开头，仅含小写字母、数字、下划线', trigger: 'blur' },
  ],
  label: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  value: [{ required: true, message: '请输入字典值', trigger: 'blur' }],
}

function open(row?: Dict) {
  editingId.value = row?.id || ''
  Object.assign(form, {
    dictType: row?.dictType || query.dictType || '',
    label: row?.label || '',
    value: row?.value || '',
    sort: row?.sort ?? 0,
    status: row?.status ?? 1,
    remark: row?.remark || '',
  })
  visible.value = true
  setTimeout(() => formRef.value?.clearValidate())
}

async function submit() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  saving.value = true
  try {
    if (editingId.value) await dictApi.update(editingId.value, { ...form })
    else await dictApi.create({ ...form })
    clearDictCache(form.dictType)
    toast.success('保存成功')
    visible.value = false
    load()
    if (!types.value.includes(form.dictType)) loadTypes()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: Dict) {
  const status = (row.status ? 0 : 1) as Switch
  await dictApi.update(row.id, { ...row, status })
  row.status = status
  list.value = [...list.value]
  clearDictCache(row.dictType)
}

async function remove(row: Dict) {
  if (await confirmDelete(`${row.dictType} / ${row.label}`, () => dictApi.remove(row.id))) {
    clearDictCache(row.dictType)
    load()
  }
}
</script>

<template>
  <div class="dict-page">
    <aside class="card type-panel">
      <div class="card-title" style="margin-bottom: 12px"><span class="card-title__text">字典类型</span></div>
      <el-input v-model="typeKeyword" placeholder="搜索类型" clearable size="small" :prefix-icon="'Search'" />
      <el-scrollbar v-loading="typesLoading" class="type-panel__list">
        <div class="type-item" :class="{ 'is-active': !query.dictType }" @click="selectType('')">
          <el-icon><Collection /></el-icon><span>全部类型</span>
        </div>
        <div v-for="t in filteredTypes" :key="t" class="type-item" :class="{ 'is-active': query.dictType === t }" @click="selectType(t)">
          <el-icon><Notebook /></el-icon>
          <div class="type-item__text">
            <span>{{ TYPE_LABELS[t] || t }}</span>
            <small v-if="TYPE_LABELS[t]" class="mono">{{ t }}</small>
          </div>
        </div>
        <EmptyState v-if="!typesLoading && !filteredTypes.length" text="无匹配类型" :size="80" />
      </el-scrollbar>
    </aside>

    <section class="card dict-main">
      <div class="toolbar">
        <div class="toolbar__left">
          <el-button v-perm="'system:dict:edit'" type="primary" :icon="'Plus'" @click="open()">新增字典项</el-button>
          <el-tag v-if="query.dictType" closable @close="selectType('')">{{ TYPE_LABELS[query.dictType] || query.dictType }}</el-tag>
        </div>
        <div class="toolbar__right">
          <el-input v-model="query.keyword" placeholder="标签 / 值" clearable :prefix-icon="'Search'" style="width: 200px" @keyup.enter="search" @clear="search" />
          <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="load" /></el-tooltip>
        </div>
      </div>
      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="字典类型" min-width="140">
          <template #default="{ row }: { row: any }"><span class="mono">{{ row.dictType }}</span></template>
        </el-table-column>
        <el-table-column label="标签" prop="label" min-width="130" />
        <el-table-column label="值" min-width="125">
          <template #default="{ row }: { row: any }"><span class="mono">{{ row.value }}</span></template>
        </el-table-column>
        <el-table-column label="排序" prop="sort" width="70" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }: { row: any }">
            <el-switch v-if="canEdit" :model-value="row.status === 1" @change="toggleStatus(row)" />
            <StatusTag v-else :value="row.status" :options="ENABLE_STATUS" />
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip />
        <el-table-column v-if="canEdit" label="操作" width="120" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button link type="primary" @click="open(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState v-if="!loading" text="暂无字典数据" /><div v-else class="table-placeholder" /></template>
      </el-table>
      <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </section>

    <el-dialog v-model="visible" :title="editingId ? '编辑字典项' : '新增字典项'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="字典类型" prop="dictType">
          <el-select v-model="form.dictType" filterable allow-create default-first-option placeholder="选择或输入新类型" class="w-full">
            <el-option v-for="t in types" :key="t" :label="TYPE_LABELS[t] ? `${TYPE_LABELS[t]}（${t}）` : t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" prop="label">
          <el-input v-model="form.label" maxlength="64" placeholder="展示文字，如：顺丰速运" />
        </el-form-item>
        <el-form-item label="值" prop="value">
          <el-input v-model="form.value" maxlength="64" placeholder="存储值，如：SF" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio v-for="o in ENABLE_STATUS" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
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
.dict-page {
  display: flex;
  gap: var(--app-gap);
  align-items: flex-start;
}
.type-panel {
  flex: none;
  width: 240px;
  position: sticky;
  top: 0;
  padding: 16px;
  &__list {
    margin-top: 10px;
    max-height: calc(100vh - 260px);
  }
}
.type-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  margin-bottom: 2px;
  border-radius: var(--app-radius);
  font-size: 13px;
  color: var(--app-text-regular);
  cursor: pointer;
  transition: background 0.15s;
  &__text {
    display: flex;
    flex-direction: column;
    min-width: 0;
    small {
      font-size: 11px;
      color: var(--app-text-secondary);
    }
  }
  &:hover {
    background: var(--app-sidebar-item-hover);
  }
  &.is-active {
    color: var(--el-color-primary);
    background: var(--app-primary-soft);
    font-weight: 500;
  }
}
.dict-main {
  flex: 1;
  min-width: 0;
}
@media (max-width: 992px) {
  .dict-page {
    flex-direction: column;
    align-items: stretch;
  }
  .type-panel {
    width: 100%;
    position: static;
    &__list {
      max-height: 180px;
    }
  }
}
</style>
