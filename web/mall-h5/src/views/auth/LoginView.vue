<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { login, loginBySms } from '@/api/auth'
import { useAppStore } from '@/stores/app'
import { useAuthRedirect } from '@/composables/useAuthRedirect'
import { isMobile } from '@/utils'
import NavBar from '@/components/NavBar.vue'
import SmsCodeButton from '@/components/SmsCodeButton.vue'

const route = useRoute()
const router = useRouter()
const app = useAppStore()
const { onLogin } = useAuthRedirect()

const mode = ref<'password' | 'sms'>('password')
const mobile = ref('')
const password = ref('')
const code = ref('')
const showPwd = ref(false)
const agree = ref(true)
const loading = ref(false)

async function submit() {
  if (!isMobile(mobile.value)) return showToast('请输入正确的手机号')
  if (mode.value === 'password' && !password.value) return showToast('请输入密码')
  if (mode.value === 'sms' && !/^\d{6}$/.test(code.value)) return showToast('请输入 6 位验证码')
  if (!agree.value) return showToast('请先阅读并同意用户协议')
  loading.value = true
  try {
    const token =
      mode.value === 'password'
        ? await login({ mobile: mobile.value, password: password.value })
        : await loginBySms({ mobile: mobile.value, code: code.value })
    await onLogin(token)
  } catch {
    /* 已提示 */
  } finally {
    loading.value = false
  }
}

function close() {
  if (window.history.state?.back) router.back()
  else router.replace('/')
}

function goRegister() {
  router.replace({ name: 'register', query: route.query })
}
</script>

<template>
  <div class="page auth">
    <NavBar :on-back="close" />
    <div class="auth__head">
      <div class="auth__title">欢迎来到{{ app.config.mallName || '商城' }}</div>
      <div class="auth__sub">{{ mode === 'sms' ? '未注册的手机号验证后将自动创建账号' : '登录后享受更多会员权益' }}</div>
    </div>

    <div class="auth__tabs">
      <span :class="{ active: mode === 'password' }" @click="mode = 'password'">密码登录</span>
      <span :class="{ active: mode === 'sms' }" @click="mode = 'sms'">验证码登录</span>
    </div>

    <form class="auth__form" @submit.prevent="submit">
      <div class="auth__field">
        <span class="auth__prefix">+86</span>
        <input v-model="mobile" type="tel" maxlength="11" placeholder="请输入手机号" autocomplete="tel" />
        <van-icon v-if="mobile" name="clear" class="auth__clear" @click="mobile = ''" />
      </div>
      <div v-if="mode === 'password'" class="auth__field">
        <input
          v-model="password"
          :type="showPwd ? 'text' : 'password'"
          maxlength="20"
          placeholder="请输入密码"
          autocomplete="current-password"
        />
        <van-icon :name="showPwd ? 'eye-o' : 'closed-eye'" class="auth__clear" @click="showPwd = !showPwd" />
      </div>
      <div v-else class="auth__field">
        <input v-model="code" type="tel" maxlength="6" placeholder="请输入验证码" autocomplete="one-time-code" />
        <SmsCodeButton :mobile="mobile" scene="LOGIN" @filled="(c: string) => (code = c)" />
      </div>

      <van-button block round native-type="submit" class="btn-gradient auth__submit" :loading="loading">
        登录
      </van-button>
    </form>

    <div class="auth__links">
      <span @click="goRegister">注册新账号</span>
      <span @click="router.push({ name: 'forgot' })">忘记密码</span>
    </div>

    <div class="auth__agree">
      <van-checkbox v-model="agree" icon-size="14">
        我已阅读并同意<span class="text-primary">《用户协议》</span>和<span class="text-primary">《隐私政策》</span>
      </van-checkbox>
    </div>
  </div>
</template>
