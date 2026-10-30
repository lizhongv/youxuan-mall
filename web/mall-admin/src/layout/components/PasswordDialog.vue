<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { toast } from '@/utils/message'

const visible = defineModel<boolean>({ default: false })
const router = useRouter()
const user = useUserStore()

const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '长度 6~32 位', trigger: 'blur' },
    {
      validator: (_r, v: string, cb) => (/^(?=.*[A-Za-z])(?=.*\d).+$/.test(v) ? cb() : cb(new Error('需同时包含字母和数字'))),
      trigger: 'blur',
    },
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: (_r, v: string, cb) => (v === form.newPassword ? cb() : cb(new Error('两次输入不一致'))), trigger: 'blur' },
  ],
}

async function submit() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  saving.value = true
  try {
    await authApi.updatePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    toast.success('密码已修改，请重新登录')
    visible.value = false
    await user.logout(false)
    router.replace('/login')
  } finally {
    saving.value = false
  }
}

function onClosed() {
  formRef.value?.resetFields()
}
</script>

<template>
  <el-dialog v-model="visible" title="修改密码" width="440px" append-to-body @closed="onClosed">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" @submit.prevent>
      <el-form-item label="当前密码" prop="oldPassword">
        <el-input v-model="form.oldPassword" type="password" show-password autocomplete="current-password" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirm">
        <el-input v-model="form.confirm" type="password" show-password autocomplete="new-password" @keyup.enter="submit" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">确认修改</el-button>
    </template>
  </el-dialog>
</template>
