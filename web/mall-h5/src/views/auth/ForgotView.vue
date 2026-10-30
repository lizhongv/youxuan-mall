<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import { resetPassword } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { isMobile } from '@/utils'
import NavBar from '@/components/NavBar.vue'
import SmsCodeButton from '@/components/SmsCodeButton.vue'

const router = useRouter()
const user = useUserStore()
const mobile = ref('')
const code = ref('')
const password = ref('')
const confirm = ref('')
const loading = ref(false)

async function submit() {
  if (!isMobile(mobile.value)) return showToast('请输入正确的手机号')
  if (!/^\d{6}$/.test(code.value)) return showToast('请输入 6 位验证码')
  if (!/^.{6,20}$/.test(password.value)) return showToast('密码长度 6-20 位')
  if (password.value !== confirm.value) return showToast('两次输入的密码不一致')
  loading.value = true
  try {
    await resetPassword({ mobile: mobile.value, code: code.value, password: password.value })
    showSuccessToast('密码已重置，请登录')
    user.clear()
    router.replace({ name: 'login' })
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
      <div class="auth__title">找回密码</div>
      <div class="auth__sub">通过手机验证码重置登录密码</div>
    </div>
    <form class="auth__form" @submit.prevent="submit">
      <div class="auth__field">
        <span class="auth__prefix">+86</span>
        <input v-model="mobile" type="tel" maxlength="11" placeholder="请输入手机号" autocomplete="tel" />
      </div>
      <div class="auth__field">
        <input v-model="code" type="tel" maxlength="6" placeholder="请输入验证码" autocomplete="one-time-code" />
        <SmsCodeButton :mobile="mobile" scene="RESET" @filled="(c: string) => (code = c)" />
      </div>
      <div class="auth__field">
        <input v-model="password" type="password" maxlength="20" placeholder="设置 6-20 位新密码" autocomplete="new-password" />
      </div>
      <div class="auth__field">
        <input v-model="confirm" type="password" maxlength="20" placeholder="再次输入新密码" autocomplete="new-password" />
      </div>
      <van-button block round native-type="submit" class="btn-gradient auth__submit" :loading="loading">
        重置密码
      </van-button>
    </form>
  </div>
</template>
