<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowRef } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { adminApi, roleApi } from '@/api/system'
import type { Admin, ID, Role, Switch } from '@/types'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { formatTime } from '@/utils/format'
import { ENABLE_STATUS } from '@/utils/constants'
import { confirm, confirmDelete, promptText, toast } from '@/utils/message'
import ImageUpload from '@/components/ImageUpload.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'

const user = useUserStore()
const canEdit = computed(() => user.hasPerm('system:admin:edit'))
/** 不能停用 / 删除自己和内置 admin（契约约束，前端提前拦截） */
const isProtected = (row: Admin) => row.id === user.info?.id || row.username === 'admin'

const { query, list, total, page, size, loading, load, search, reset, onPageChange, onSizeChange } = useTable(adminApi.page, {
  keyword: '',
  status: '' as Switch | '',
})

const roles = shallowRef<Role[]>([])
onMounted(async () => {
  try {
    roles.value = (await roleApi.list()) || []
  } catch {
    /* 忽略 */
  }
})

async function toggleStatus(row: Admin) {
  const status = (row.status ? 0 : 1) as Switch
  if (!status && !(await confirm(`停用后「${row.nickname || row.username}」将无法登录，确定吗？`))) return
  await adminApi.setStatus(row.id, status)
  row.status = status
  list.value = [...list.value]
  toast.success(status ? '已启用' : '已停用')
}

async function resetPwd(row: Admin) {
  const pwd = await promptText(`重置「${row.username}」的密码`, { placeholder: '新密码：6~32 位，含字母和数字', required: true })
  if (pwd === null) return
  if (!/^(?=.*[A-Za-z])(?=.*\d).{6,32}$/.test(pwd)) return toast.warning('密码需 6~32 位且同时包含字母和数字')
  await adminApi.resetPassword(row.id, pwd)
  toast.success('密码已重置')
}

async function remove(row: Admin) {
  if (await confirmDelete(row.username, () => adminApi.remove(row.id))) load()
}

// ---------------- 表单 ----------------
const visible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<ID>('')
const form = reactive({ username: '', nickname: '', password: '', avatar: '', mobile: '', email: '', roleIds: [] as ID[], status: 1 as Switch, remark: '' })

const rules = computed<FormRules>(() => ({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]{3,31}$/, message: '字母开头，4~32 位字母、数字或下划线', trigger: 'blur' },
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: editingId.value
    ? []
    : [
        { required: true, message: '请输入初始密码', trigger: 'blur' },
        { pattern: /^(?=.*[A-Za-z])(?=.*\d).{6,32}$/, message: '6~32 位，需包含字母和数字', trigger: 'blur' },
      ],
  mobile: [{ pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  roleIds: [{ type: 'array', required: true, min: 1, message: '请至少选择一个角色', trigger: 'change' }],
}))

async function open(row?: Admin) {
  editingId.value = row?.id || ''
  Object.assign(form, {
    username: row?.username || '',
    nickname: row?.nickname || '',
    password: '',
    avatar: row?.avatar || '',
    mobile: row?.mobile || '',
    email: row?.email || '',
    roleIds: [...(row?.roleIds || [])],
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
    const { password, ...rest } = form
    if (editingId.value) await adminApi.update(editingId.value, rest)
    else await adminApi.create({ ...rest, password })
    toast.success('保存成功')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="card">
      <el-form :model="query" inline class="search-bar" @submit.prevent="search">
        <el-form-item label="管理员">
          <el-input v-model="query.keyword" placeholder="用户名 / 昵称 / 手机号" clearable @keyup.enter="search" />
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
          <el-button v-perm="'system:admin:edit'" type="primary" :icon="'Plus'" @click="open()">新增管理员</el-button>
        </div>
        <div class="toolbar__right">
          <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="load" /></el-tooltip>
        </div>
      </div>
      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="管理员" min-width="200">
          <template #default="{ row }: { row: any }">
            <div class="flex-center">
              <el-avatar :size="36" :src="row.avatar || undefined">{{ (row.nickname || row.username).slice(0, 1) }}</el-avatar>
              <div>
                <div class="name">
                  {{ row.nickname }}
                  <el-tag v-if="row.id === user.info?.id" size="small" effect="plain">我</el-tag>
                </div>
                <div class="text-secondary sub">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }: { row: any }">
            <div class="roles">
              <el-tag v-for="r in row.roleNames || []" :key="r" size="small" type="primary" effect="light">{{ r }}</el-tag>
              <span v-if="!row.roleNames?.length" class="text-secondary">-</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="手机号" prop="mobile" width="130" />
        <el-table-column label="邮箱" prop="email" min-width="170" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }: { row: any }">
            <el-switch :model-value="row.status === 1" :disabled="!canEdit || isProtected(row)" @change="toggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="160">
          <template #default="{ row }: { row: any }"><span class="text-secondary">{{ formatTime(row.lastLoginAt) }}</span></template>
        </el-table-column>
        <el-table-column v-if="canEdit" label="操作" width="200" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button link type="primary" @click="open(row)">编辑</el-button>
            <el-button link type="primary" @click="resetPwd(row)">重置密码</el-button>
            <el-button link type="danger" :disabled="isProtected(row)" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState text="暂无管理员" /></template>
      </el-table>
      <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <el-dialog v-model="visible" :title="editingId ? '编辑管理员' : '新增管理员'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="头像">
              <ImageUpload v-model="form.avatar" biz-type="avatar" :size="72" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" :disabled="!!editingId" autocomplete="off" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col v-if="!editingId" :xs="24" :sm="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password autocomplete="new-password" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="手机号" prop="mobile">
              <el-input v-model="form.mobile" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="o in ENABLE_STATUS" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色" prop="roleIds">
              <el-select v-model="form.roleIds" multiple placeholder="请选择角色" class="w-full">
                <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id">
                  <span>{{ r.name }}</span>
                  <span class="text-secondary" style="float: right; font-size: 12px">{{ r.code }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.name {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--app-text);
  font-weight: 500;
}
.sub {
  font-size: 12px;
}
.roles {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
