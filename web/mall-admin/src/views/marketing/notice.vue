<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { noticeApi } from '@/api/marketing'
import type { ID, Notice, Switch } from '@/types'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { formatTime } from '@/utils/format'
import { confirmDelete, toast } from '@/utils/message'
import RichEditor from '@/components/RichEditor.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'

const user = useUserStore()
const canEdit = computed(() => user.hasPerm('marketing:notice:edit'))

const { query, list, total, page, size, loading, load, search, reset, onPageChange, onSizeChange } = useTable(noticeApi.page, {
  keyword: '',
  status: '' as Switch | '',
})

async function toggleStatus(row: Notice) {
  const status = (row.status ? 0 : 1) as Switch
  await noticeApi.update(row.id, { ...row, status })
  row.status = status
  list.value = [...list.value]
  toast.success(status ? '已发布' : '已撤回')
}

async function remove(row: Notice) {
  if (await confirmDelete(row.title, () => noticeApi.remove(row.id))) load()
}

const visible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<ID>('')
const form = reactive({ title: '', content: '', sort: 0, status: 1 as Switch })
const rules: FormRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
}

function open(row?: Notice) {
  editingId.value = row?.id || ''
  Object.assign(form, { title: row?.title || '', content: row?.content || '', sort: row?.sort ?? 0, status: row?.status ?? 1 })
  visible.value = true
}

async function submit() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  if (!form.content.replace(/<[^>]+>/g, '').trim() && !/<img/i.test(form.content)) return toast.warning('请输入公告内容')
  saving.value = true
  try {
    if (editingId.value) await noticeApi.update(editingId.value, { ...form })
    else await noticeApi.create({ ...form })
    toast.success('保存成功')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

// 预览
const previewVisible = ref(false)
const previewing = ref<Notice | null>(null)
function preview(row: Notice) {
  previewing.value = row
  previewVisible.value = true
}
</script>

<template>
  <div class="page">
    <div class="card">
      <el-form :model="query" inline class="search-bar" @submit.prevent="search">
        <el-form-item label="标题">
          <el-input v-model="query.keyword" placeholder="公告标题" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option label="已发布" :value="1" />
            <el-option label="未发布" :value="0" />
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
          <el-button v-perm="'marketing:notice:edit'" type="primary" :icon="'Plus'" @click="open()">发布公告</el-button>
        </div>
        <div class="toolbar__right">
          <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="load" /></el-tooltip>
        </div>
      </div>
      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="标题" min-width="260">
          <template #default="{ row }: { row: any }">
            <span class="notice-title" @click="preview(row)"><el-icon><Bell /></el-icon>{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="发布" width="80" align="center">
          <template #default="{ row }: { row: any }">
            <el-switch :model-value="row.status === 1" :disabled="!canEdit" @change="toggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }: { row: any }"><span class="text-secondary">{{ formatTime(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button link type="primary" @click="preview(row)">预览</el-button>
            <el-button v-if="canEdit" link type="primary" @click="open(row)">编辑</el-button>
            <el-button v-if="canEdit" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState v-if="!loading" text="暂无公告" /><div v-else class="table-placeholder" /></template>
      </el-table>
      <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <el-dialog v-model="visible" :title="editingId ? '编辑公告' : '发布公告'" width="860px" destroy-on-close top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="70px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" required>
          <RichEditor v-model="form.content" :height="320" biz-type="notice" placeholder="请输入公告内容" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发布">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" :title="previewing?.title" width="600px">
      <div class="notice-preview" v-html="previewing?.content" />
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.notice-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--app-text);
  .el-icon {
    color: var(--app-warning);
  }
  &:hover {
    color: var(--el-color-primary);
  }
}
.notice-preview {
  line-height: 1.8;
  color: var(--app-text-regular);
  max-height: 60vh;
  overflow: auto;
  :deep(img) {
    max-width: 100%;
  }
}
</style>
