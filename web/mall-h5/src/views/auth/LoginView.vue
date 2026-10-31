<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { closeToast, showConfirmDialog, showLoadingToast, showToast } from 'vant'
import { getWechatAuthorizeUrl, login, loginBySms } from '@/api/auth'
import { errorMessage, needCaptcha } from '@/api/request'
import { useAppStore } from '@/stores/app'
import { useAuthRedirect } from '@/composables/useAuthRedirect'
import { isMobile, isWechat } from '@/utils'
import NavBar from '@/components/NavBar.vue'
import SmsCodeButton from '@/components/SmsCodeButton.vue'
import CaptchaInput from '@/components/CaptchaInput.vue'
import AgreementCheck from '@/components/AgreementCheck.vue'

const route = useRoute()
const router = useRouter()
const app = useAppStore()
const { onLogin, redirectTarget } = useAuthRedirect()

const mode = ref<'password' | 'sms'>('password')
const mobile = ref('')
const password = ref('')
const code = ref('')
const showPwd = ref(false)
/** 协议默认不勾选（合规要求） */
const agree = ref(false)
const loading = ref(false)
/** 密码登录失败多次后需要图形验证码 */
const captchaVisible = ref(false)
const captchaCode = ref('')
const captchaRef = ref<InstanceType<typeof CaptchaInput>>()
const inWechat = isWechat()
const wechatLoading = ref(false)

async function ensureAgree() {
  if (agree.value) return true
  try {
    await showConfirmDialog({
      title: '用户协议与隐私政策',
      message: '登录前请阅读并同意《用户协议》和《隐私政策》',
      confirmButtonText: '同意并继续',
    })
    agree.value = true
    return true
  } catch {
    return false
  }
}

async function submit() {
  if (!isMobile(mobile.value)) return showToast('请输入正确的手机号')
  if (mode.value === 'password' && !password.value) return showToast('请输入密码')
  if (mode.value === 'sms' && !/^\d{6}$/.test(code.value)) return showToast('请输入 6 位验证码')
  if (mode.value === 'password' && captchaVisible.value && !captchaCode.value.trim()) return showToast('请输入图形验证码')
  if (!(await ensureAgree())) return
  loading.value = true
  try {
    const token =
      mode.value === 'password'
        ? await login({
            mobile: mobile.value,
            password: password.value,
            ...(captchaVisible.value ? captchaRef.value?.answer() : {}),
          })
        : await loginBySms({ mobile: mobile.value, code: code.value })
    await onLogin(token)
  } catch (e) {
    if (mode.value === 'password') {
      // 密码登录接口为静默请求：在这里提示，并按 needCaptcha 显示 / 刷新图形验证码
      showToast(errorMessage(e, '登录失败'))
      if (needCaptcha(e)) {
        if (captchaVisible.value) captchaRef.value?.refresh()
        else {
          captchaVisible.value = true
          await nextTick()
        }
      }
    }
  } finally {
    loading.value = false
  }
}

/** 微信内一键登录：跳转公众号授权页，授权后回到 /auth/wechat 完成登录 */
async function wechatLogin() {
  if (!(await ensureAgree())) return
  wechatLoading.value = true
  showLoadingToast({ message: '正在跳转微信授权...', forbidClick: true })
  try {
    const target = router.resolve({ name: 'wechat-callback', query: { redirect: redirectTarget() } }).href
    const res = await getWechatAuthorizeUrl(window.location.origin + target)
    window.location.href = res.url
  } catch {
    closeToast()
    wechatLoading.value = false
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
      <template v-if="mode === 'password'">
        <div class="auth__field">
          <input
            v-model="password"
            :type="showPwd ? 'text' : 'password'"
            maxlength="20"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
          <van-icon :name="showPwd ? 'eye-o' : 'closed-eye'" class="auth__clear" @click="showPwd = !showPwd" />
        </div>
        <CaptchaInput v-if="captchaVisible" ref="captchaRef" v-model="captchaCode" class="auth__field" />
      </template>
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

    <div v-if="inWechat" class="auth__wechat">
      <div class="auth__divider"><span>其他登录方式</span></div>
      <van-button block round class="auth__wechat-btn" icon="wechat" :loading="wechatLoading" @click="wechatLogin">
        微信一键登录
      </van-button>
    </div>

    <AgreementCheck v-model="agree" />
  </div>
</template>
