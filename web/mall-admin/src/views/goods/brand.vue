<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { brandApi } from '@/api/goods'
import type { Brand, ID, Switch } from '@/types'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { ENABLE_STATUS } from '@/utils/constants'
import { confirmDelete, toast } from '@/utils/message'
import ImageUpload from '@/components/ImageUpload.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'

const user = useUserStore()
const canEdit = computed(() => user.hasPerm('goods:brand:edit'))
const view = ref<'card' | 'table'>('card')

const { query, list, total, page, size, loading, load, search, reset, onPageChange, onSizeChange } = useTable(brandApi.page, {
  keyword: '',
  status: '' as Switch | '',
})

const visible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<ID>('')
const form = reactive({ name: '', logo: '', firstLetter: '', description: '', sort: 0, status: 1 as Switch })
const rules: FormRules = {
  name: [{ required: true, message: '请输入品牌名称', trigger: 'blur' }],
  logo: [{ required: true, message: '请上传品牌 Logo', trigger: 'change' }],
  firstLetter: [{ pattern: /^[A-Z]?$/, message: '请填写一个大写字母', trigger: 'blur' }],
}

function open(row?: Brand) {
  editingId.value = row?.id || ''
  Object.assign(form, {
    name: row?.name || '',
    logo: row?.logo || '',
    firstLetter: row?.firstLetter || '',
    description: row?.description || '',
    sort: row?.sort ?? 0,
    status: row?.status ?? 1,
  })
  visible.value = true
}

async function submit() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  saving.value = true
  try {
    if (editingId.value) await brandApi.update(editingId.value, { ...form })
    else await brandApi.create({ ...form })
    toast.success('保存成功')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: Brand) {
  const status = (row.status ? 0 : 1) as Switch
  await brandApi.update(row.id, { ...row, status })
  row.status = status
  list.value = [...list.value]
  toast.success(status ? '已启用' : '已停用')
}

async function remove(row: Brand) {
  if (await confirmDelete(row.name, () => brandApi.remove(row.id))) load()
}
</script>

<template>
  <div class="page">
    <div class="card">
      <el-form :model="query" inline class="search-bar" @submit.prevent="search">
        <el-form-item label="品牌">
          <el-input v-model="query.keyword" placeholder="品牌名称" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option v-for="o in ENABLE_STATUS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="search">搜索</el-button>
          <el-button :icon="'Refresh'" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="toolbar__left">
          <el-button v-perm="'goods:brand:edit'" type="primary" :icon="'Plus'" @click="open()">新增品牌</el-button>
        </div>
        <div class="toolbar__right">
          <el-radio-group v-model="view" size="small">
            <el-radio-button value="card"><el-icon><Grid /></el-icon></el-radio-button>
            <el-radio-button value="table"><el-icon><List /></el-icon></el-radio-button>
          </el-radio-group>
          <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="load" /></el-tooltip>
        </div>
      </div>

      <div v-if="view === 'card'" v-loading="loading" class="brand-grid">
        <div v-for="b in list" :key="b.id" class="brand-card" :class="{ 'is-off': !b.status }">
          <div class="brand-card__logo">
            <el-image :src="b.logo" fit="contain" />
            <span v-if="b.firstLetter" class="brand-card__letter">{{ b.firstLetter }}</span>
          </div>
          <div class="brand-card__body">
            <div class="brand-card__name">
              <span class="ellipsis">{{ b.name }}</span>
              <el-tag v-if="!b.status" size="small" type="info">已停用</el-tag>
            </div>
            <div class="brand-card__desc">{{ b.description || '暂无介绍' }}</div>
          </div>
          <div v-if="canEdit" class="brand-card__actions">
            <el-button link type="primary" @click="open(b)">编辑</el-button>
            <el-button link @click="toggleStatus(b)">{{ b.status ? '停用' : '启用' }}</el-button>
            <el-button link type="danger" @click="remove(b)">删除</el-button>
          </div>
        </div>
        <EmptyState v-if="!loading && !list.length" class="brand-grid__empty" text="暂无品牌" />
      </div>

      <el-table v-else v-loading="loading" :data="list" row-key="id">
        <el-table-column label="Logo" width="100">
          <template #default="{ row }: { row: any }">
            <el-image :src="row.logo" fit="contain" class="brand-logo" :preview-src-list="[row.logo]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="品牌名称" min-width="140" />
        <el-table-column prop="firstLetter" label="首字母" width="80" align="center" />
        <el-table-column prop="description" label="介绍" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }: { row: any }">
            <el-switch :model-value="row.status === 1" :disabled="!canEdit" @change="toggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column v-if="canEdit" label="操作" width="130" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button link type="primary" @click="open(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState v-if="!loading" text="暂无品牌" /><div v-else class="table-placeholder" /></template>
      </el-table>
      <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <el-dialog v-model="visible" :title="editingId ? '编辑品牌' : '新增品牌'" width="540px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="品牌名称" prop="name">
          <el-input v-model="form.name" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="品牌 Logo" prop="logo">
          <ImageUpload v-model="form.logo" biz-type="brand" :size="96" tip="建议 200×200，白底或透明背景" />
        </el-form-item>
        <el-form-item label="首字母" prop="firstLetter">
          <el-input v-model="form.firstLetter" maxlength="1" style="width: 100px" @input="form.firstLetter = form.firstLetter.toUpperCase()" />
          <span class="form-tip" style="margin-left: 10px">用于商城品牌索引</span>
        </el-form-item>
        <el-form-item label="品牌介绍">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
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
.brand-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 14px;
  min-height: 160px;
  &__empty {
    grid-column: 1 / -1;
  }
}
.brand-card {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  overflow: hidden;
  transition:
    box-shadow 0.2s,
    transform 0.2s;
  &:hover {
    box-shadow: var(--app-shadow-hover);
    transform: translateY(-2px);
  }
  &.is-off {
    opacity: 0.65;
  }
  &__logo {
    position: relative;
    height: 110px;
    padding: 18px;
    background: var(--app-card-bg-muted);
    display: flex;
    align-items: center;
    justify-content: center;
    .el-image {
      width: 100%;
      height: 100%;
    }
  }
  &__letter {
    position: absolute;
    top: 10px;
    left: 10px;
    width: 24px;
    height: 24px;
    border-radius: 6px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 700;
    color: var(--el-color-primary);
    background: var(--app-primary-soft);
  }
  &__body {
    padding: 12px 14px 4px;
    flex: 1;
    min-width: 0;
  }
  &__name {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 600;
    color: var(--app-text);
  }
  &__desc {
    margin-top: 4px;
    font-size: 12px;
    line-height: 1.6;
    color: var(--app-text-secondary);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    min-height: 38px;
  }
  &__actions {
    display: flex;
    justify-content: flex-end;
    padding: 6px 10px 10px;
  }
}
.brand-logo {
  width: 56px;
  height: 40px;
}
</style>
