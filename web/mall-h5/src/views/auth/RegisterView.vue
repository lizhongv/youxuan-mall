<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { register } from '@/api/auth'
import { useAuthRedirect } from '@/composables/useAuthRedirect'
import { isMobile } from '@/utils'
import NavBar from '@/components/NavBar.vue'
import SmsCodeButton from '@/components/SmsCodeButton.vue'
import AgreementCheck from '@/components/AgreementCheck.vue'

const route = useRoute()
const router = useRouter()
const { onLogin } = useAuthRedirect()

const mobile = ref('')
const code = ref('')
const password = ref('')
const nickname = ref('')
/** 协议默认不勾选（合规要求） */
const agree = ref(false)
const loading = ref(false)

async function submit() {
  if (!isMobile(mobile.value)) return showToast('请输入正确的手机号')
  if (!/^\d{6}$/.test(code.value)) return showToast('请输入 6 位验证码')
  if (!/^.{6,20}$/.test(password.value)) return showToast('密码长度 6-20 位')
  if (!agree.value) return showToast('请先阅读并同意用户协议')
  loading.value = true
  try {
    const token = await register({
      mobile: mobile.value,
      code: code.value,
      password: password.value,
      nickname: nickname.value.trim() || undefined,
    })
    await onLogin(token, '注册成功')
  } catch {
    /* 已提示 */
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page auth">
    <NavBar />
    <div class="auth__head">
      <div class="auth__title">注册新账号</div>
      <div class="auth__sub">注册即可领取新人专享好礼</div>
    </div>
    <form class="auth__form" @submit.prevent="submit">
      <div class="auth__field">
        <span class="auth__prefix">+86</span>
        <input v-model="mobile" type="tel" maxlength="11" placeholder="请输入手机号" autocomplete="tel" />
      </div>
      <div class="auth__field">
        <input v-model="code" type="tel" maxlength="6" placeholder="请输入验证码" autocomplete="one-time-code" />
        <SmsCodeButton :mobile="mobile" scene="REGISTER" @filled="(c: string) => (code = c)" />
      </div>
      <div class="auth__field">
        <input v-model="password" type="password" maxlength="20" placeholder="设置 6-20 位登录密码" autocomplete="new-password" />
      </div>
      <div class="auth__field">
        <input v-model="nickname" type="text" maxlength="20" placeholder="昵称（选填）" />
      </div>
      <van-button block round native-type="submit" class="btn-gradient auth__submit" :loading="loading">
        注册并登录
      </van-button>
    </form>
    <div class="auth__links">
      <span @click="router.replace({ name: 'login', query: route.query })">已有账号？去登录</span>
    </div>
    <AgreementCheck v-model="agree" />
  </div>
</template>
