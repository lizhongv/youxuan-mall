<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { firstAccessiblePath } from '@/router'
import { toast } from '@/utils/message'

/**
 * 强制修改初始密码（全屏，不进入布局）。
 * 首次登录、被他人新建或重置密码的账号必须先改密，其余管理端接口返回 40301；路由守卫保证无法绕过。
 */
const router = useRouter()
const user = useUserStore()
const app = useAppStore()

const logoUrl = `${import.meta.env.BASE_URL}logo.svg`
const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前（初始）密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{6,32}$/, message: '6~32 位，需同时包含字母和数字', trigger: 'blur' },
    { validator: (_r, v: string, cb) => (v && v === form.oldPassword ? cb(new Error('新密码不能与初始密码相同')) : cb()), trigger: 'blur' },
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: (_r, v: string, cb) => (v === form.newPassword ? cb() : cb(new Error('两次输入不一致'))), trigger: 'blur' },
  ],
}

/** 密码强度 0~3：长度 + 字符类别 */
function strength(p: string) {
  let s = 0
  if (p.length >= 8) s++
  if (p.length >= 12) s++
  if (/[a-z]/.test(p) && /[A-Z]/.test(p)) s++
  if (/\d/.test(p)) s++
  if (/[^A-Za-z0-9]/.test(p)) s++
  return Math.min(3, Math.ceil(s / 1.7))
}

async function submit() {
  if (saving.value) return
  if (!(await formRef.value?.validate().catch(() => false))) return
  saving.value = true
  try {
    const kept = await user.changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    if (kept) {
      toast.success('密码已修改，欢迎使用')
      router.replace(firstAccessiblePath())
    } else {
      toast.success('密码已修改，请使用新密码登录')
      await user.logout(false)
      router.replace('/login')
    }
  } catch {
    form.oldPassword = ''
  } finally {
    saving.value = false
  }
}

async function logout() {
  await user.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="force-pwd">
    <button class="force-pwd__theme" :title="app.theme === 'dark' ? '切换浅色' : '切换深色'" @click="app.setTheme(app.theme === 'dark' ? 'light' : 'dark')">
      <el-icon :size="18"><Sunny v-if="app.theme === 'dark'" /><Moon v-else /></el-icon>
    </button>
    <div class="force-pwd__card">
      <div class="force-pwd__head">
        <img :src="logoUrl" alt="" class="force-pwd__logo" />
        <h2>首次登录请修改密码</h2>
        <p>
          为保障账号安全，<b>{{ user.info?.nickname || user.info?.username }}</b> 需要先把初始密码修改为只有你知道的新密码，修改后才能使用后台。
        </p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" label-position="top" @submit.prevent="submit">
        <el-form-item label="初始密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password autocomplete="current-password" placeholder="管理员提供的初始密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" placeholder="6~32 位，包含字母和数字" />
          <div v-if="form.newPassword" class="strength" :class="`is-${strength(form.newPassword)}`">
            <i /><i /><i />
            <span>密码强度：{{ ['弱', '弱', '中', '强'][strength(form.newPassword)] }}</span>
          </div>
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirm">
          <el-input v-model="form.confirm" type="password" show-password autocomplete="new-password" placeholder="再次输入新密码" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="saving" class="force-pwd__submit">确认修改并进入后台</el-button>
      </el-form>
      <div class="force-pwd__foot">
        <span class="text-secondary">不是你的账号？</span>
        <el-button link type="primary" @click="logout">退出登录</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.force-pwd {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: var(--app-login-bg);

  &__theme {
    position: absolute;
    top: 20px;
    right: 20px;
    width: 38px;
    height: 38px;
    border-radius: 50%;
    border: 1px solid var(--app-border);
    background: var(--app-card-bg);
    color: var(--app-text-regular);
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    box-shadow: var(--app-shadow);
  }
  &__card {
    width: 100%;
    max-width: 440px;
    padding: 36px 36px 24px;
    border-radius: var(--app-radius-lg);
    background: var(--app-card-bg);
    border: 1px solid var(--app-border);
    box-shadow: var(--app-shadow-pop);
  }
  &__head {
    margin-bottom: 20px;
    h2 {
      margin: 14px 0 8px;
      font-size: 22px;
      color: var(--app-text);
    }
    p {
      margin: 0;
      font-size: 13px;
      line-height: 1.7;
      color: var(--app-text-secondary);
      b {
        color: var(--app-text);
      }
    }
  }
  &__logo {
    width: 44px;
    height: 44px;
  }
  &__submit {
    width: 100%;
    height: 44px;
    margin-top: 6px;
    font-size: 15px;
    border: none;
    background: var(--app-gradient);
  }
  &__foot {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    margin-top: 16px;
    font-size: 13px;
  }
}
.strength {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  width: 100%;
  font-size: 12px;
  color: var(--app-text-secondary);
  i {
    width: 40px;
    height: 4px;
    border-radius: 2px;
    background: var(--app-border);
  }
  span {
    margin-left: 6px;
  }
  &.is-0 i:nth-child(-n + 1),
  &.is-1 i:nth-child(-n + 1) {
    background: var(--app-danger);
  }
  &.is-2 i:nth-child(-n + 2) {
    background: var(--app-warning);
  }
  &.is-3 i:nth-child(-n + 3) {
    background: var(--app-success);
  }
}
@media (max-width: 480px) {
  .force-pwd__card {
    padding: 28px 20px 20px;
  }
}
</style>
