<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { wechatBind } from '@/api/auth'
import { useAuthRedirect } from '@/composables/useAuthRedirect'
import { isMobile } from '@/utils'
import NavBar from '@/components/NavBar.vue'
import SmsCodeButton from '@/components/SmsCodeButton.vue'
import EmptyState from '@/components/EmptyState.vue'

/** 微信首次登录绑定手机号：短信验证码 scene=LOGIN，未注册的手机号自动注册 */
const BIND_TOKEN_KEY = 'mall_h5_wechat_bind'
const router = useRouter()
const { onLogin, redirectTarget } = useAuthRedirect()

function readToken() {
  try {
    return sessionStorage.getItem(BIND_TOKEN_KEY) || ''
  } catch {
    return ''
  }
}
const bindToken = readToken()
const mobile = ref('')
const code = ref('')
const loading = ref(false)

async function submit() {
  if (!isMobile(mobile.value)) return showToast('请输入正确的手机号')
  if (!/^\d{6}$/.test(code.value)) return showToast('请输入 6 位验证码')
  loading.value = true
  try {
    const token = await wechatBind({ bindToken, mobile: mobile.value, code: code.value })
    try {
      sessionStorage.removeItem(BIND_TOKEN_KEY)
    } catch {
      /* ignore */
    }
    await onLogin(token, '绑定成功')
  } catch {
    /* 已提示（绑定凭证 10 分钟有效，过期需重新授权） */
  } finally {
    loading.value = false
  }
}

function toLogin() {
  router.replace({ name: 'login', query: { redirect: redirectTarget() } })
}
</script>

<template>
  <div class="page auth">
    <NavBar title="" :on-back="toLogin" />
    <EmptyState v-if="!bindToken" image="error" description="绑定已失效，请重新使用微信登录" button-text="返回登录" @click="toLogin" />
    <template v-else>
      <div class="auth__head">
        <div class="auth__title">绑定手机号</div>
        <div class="auth__sub">首次使用微信登录，请绑定手机号；未注册的手机号将自动创建账号</div>
      </div>
      <form class="auth__form" @submit.prevent="submit">
        <div class="auth__field">
          <span class="auth__prefix">+86</span>
          <input v-model="mobile" type="tel" maxlength="11" placeholder="请输入手机号" autocomplete="tel" />
        </div>
        <div class="auth__field">
          <input v-model="code" type="tel" maxlength="6" placeholder="请输入验证码" autocomplete="one-time-code" />
          <SmsCodeButton :mobile="mobile" scene="LOGIN" @filled="(c: string) => (code = c)" />
        </div>
        <van-button block round native-type="submit" class="btn-gradient auth__submit" :loading="loading">
          绑定并登录
        </van-button>
      </form>
    </template>
  </div>
</template>
