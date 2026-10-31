<script setup lang="ts">
import { nextTick, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules, InputInstance } from 'element-plus'
import { authApi } from '@/api/auth'
import { ApiError, ResultCode } from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { useTabsStore } from '@/stores/tabs'
import { StorageKey, storage } from '@/utils/storage'
import { firstAccessiblePath } from '@/router'
import { toast } from '@/utils/message'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const app = useAppStore()
const tabs = useTabsStore()

const logoUrl = `${import.meta.env.BASE_URL}logo.svg`
const savedName = storage.get(StorageKey.rememberUser) || ''
const formRef = ref<FormInstance>()
const loading = ref(false)
const remember = ref(!!savedName)
const form = reactive({ username: savedName, password: '', captchaCode: '' })

// ---------- 图形验证码：登录失败 2 次后服务端要求（needCaptcha） ----------
const needCaptcha = ref(false)
const captcha = reactive({ id: '', image: '', loading: false })
const captchaInput = ref<InputInstance>()

async function refreshCaptcha() {
  if (captcha.loading) return
  captcha.loading = true
  try {
    const res = await authApi.captcha()
    captcha.id = res.captchaId
    captcha.image = res.image
    // demo 模式返回答案，自动填入方便体验
    form.captchaCode = res.devCode || ''
  } catch {
    captcha.image = ''
  } finally {
    captcha.loading = false
  }
}

async function requireCaptcha() {
  const first = !needCaptcha.value
  needCaptcha.value = true
  await refreshCaptcha()
  if (first) await nextTick()
  captchaInput.value?.focus()
}

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }],
}

async function submit() {
  if (loading.value) return
  if (!(await formRef.value?.validate().catch(() => false))) return
  loading.value = true
  try {
    await user.login({
      username: form.username.trim(),
      password: form.password,
      ...(needCaptcha.value ? { captchaId: captcha.id, captchaCode: form.captchaCode.trim() } : {}),
    })
    if (remember.value) storage.set(StorageKey.rememberUser, form.username.trim())
    else storage.remove(StorageKey.rememberUser)
    await user.fetchInfo()
    tabs.reset()
    if (user.mustChangePassword) {
      router.replace({ name: 'ChangePassword' })
      return
    }
    toast.success(`欢迎回来，${user.displayName}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    // 只允许站内相对路径，避免开放重定向
    const target = redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : firstAccessiblePath()
    router.replace(target)
  } catch (e) {
    const data = e instanceof ApiError ? (e.data as { needCaptcha?: boolean } | undefined) : undefined
    if (e instanceof ApiError && (e.code === ResultCode.NEED_CAPTCHA || data?.needCaptcha)) {
      // 验证码一次性：任何失败后都换新
      await requireCaptcha()
      if (e.code !== ResultCode.NEED_CAPTCHA) form.password = ''
    } else {
      form.password = ''
      if (needCaptcha.value) refreshCaptcha()
    }
  } finally {
    loading.value = false
  }
}

const features = [
  { icon: 'DataLine', text: '实时经营数据看板' },
  { icon: 'Goods', text: '多规格商品与库存管理' },
  { icon: 'Present', text: '优惠券 · 秒杀 · 轮播营销' },
  { icon: 'Lock', text: '细粒度角色与按钮权限' },
]
</script>

<template>
  <div class="login">
    <button class="login__theme" :title="app.theme === 'dark' ? '切换浅色' : '切换深色'" @click="app.setTheme(app.theme === 'dark' ? 'light' : 'dark')">
      <el-icon :size="18"><Sunny v-if="app.theme === 'dark'" /><Moon v-else /></el-icon>
    </button>

    <!-- 左侧品牌插画：纯 CSS / SVG 绘制 -->
    <section class="login__art">
      <div class="blob blob--1" />
      <div class="blob blob--2" />
      <div class="blob blob--3" />
      <div class="grid-bg" />
      <div class="login__brand">
        <img :src="logoUrl" alt="" />
        <span>商城管理后台</span>
      </div>
      <div class="login__hero">
        <h1>让每一笔生意<br />都清晰可见</h1>
        <p>商品、订单、营销、会员一站式运营平台</p>
        <ul class="login__features">
          <li v-for="f in features" :key="f.text">
            <span class="login__feature-icon"><el-icon><component :is="f.icon" /></el-icon></span>
            {{ f.text }}
          </li>
        </ul>
      </div>
      <svg class="login__illus" viewBox="0 0 420 300" aria-hidden="true">
        <defs>
          <linearGradient id="lg-card" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" style="stop-color: var(--app-login-card)" stop-opacity=".95" />
            <stop offset="1" style="stop-color: var(--app-login-card)" stop-opacity=".75" />
          </linearGradient>
          <linearGradient id="lg-bar" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" style="stop-color: var(--app-login-bar-1)" />
            <stop offset="1" style="stop-color: var(--app-chart-1)" />
          </linearGradient>
          <linearGradient id="lg-area" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" style="stop-color: var(--app-login-art-3)" stop-opacity=".45" />
            <stop offset="1" style="stop-color: var(--app-login-art-3)" stop-opacity="0" />
          </linearGradient>
        </defs>
        <!-- 主卡片：柱状 + 折线 -->
        <rect x="40" y="40" width="260" height="190" rx="18" fill="url(#lg-card)" />
        <rect x="62" y="62" width="70" height="8" rx="4" style="fill: var(--app-login-soft-1)" />
        <rect x="62" y="78" width="110" height="14" rx="7" style="fill: var(--app-chart-1)" opacity=".85" />
        <g fill="url(#lg-bar)">
          <rect x="70" y="160" width="18" height="50" rx="5" />
          <rect x="104" y="140" width="18" height="70" rx="5" />
          <rect x="138" y="170" width="18" height="40" rx="5" />
          <rect x="172" y="120" width="18" height="90" rx="5" />
          <rect x="206" y="132" width="18" height="78" rx="5" />
          <rect x="240" y="105" width="18" height="105" rx="5" />
        </g>
        <path d="M70 150 L113 128 L147 150 L181 108 L215 120 L262 88 L262 210 L70 210Z" fill="url(#lg-area)" />
        <path d="M70 150 L113 128 L147 150 L181 108 L215 120 L262 88" fill="none" style="stroke: var(--app-login-art-3)" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
        <circle cx="262" cy="88" r="5" style="fill: var(--app-login-card); stroke: var(--app-login-art-3)" stroke-width="3" />
        <!-- 环形卡片 -->
        <rect x="250" y="168" width="140" height="110" rx="16" fill="url(#lg-card)" />
        <circle cx="300" cy="223" r="28" fill="none" style="stroke: var(--app-login-soft-2)" stroke-width="10" />
        <circle cx="300" cy="223" r="28" fill="none" style="stroke: var(--app-login-art-2)" stroke-width="10" stroke-dasharray="120 176" transform="rotate(-90 300 223)" stroke-linecap="round" />
        <rect x="340" y="205" width="36" height="7" rx="3.5" style="fill: var(--app-login-soft-1)" />
        <rect x="340" y="220" width="26" height="7" rx="3.5" style="fill: var(--app-login-soft-3)" />
        <rect x="340" y="235" width="32" height="7" rx="3.5" style="fill: var(--app-login-soft-4)" />
        <!-- 订单气泡 -->
        <rect x="290" y="20" width="118" height="54" rx="14" fill="url(#lg-card)" />
        <circle cx="314" cy="47" r="12" style="fill: var(--app-success)" opacity=".9" />
        <path d="M308 47l4 4 8-8" style="stroke: var(--app-login-card)" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round" />
        <rect x="334" y="38" width="58" height="7" rx="3.5" style="fill: var(--app-login-soft-1)" />
        <rect x="334" y="52" width="40" height="7" rx="3.5" style="fill: var(--app-login-soft-2)" />
      </svg>
      <div class="login__copy">© {{ new Date().getFullYear() }} Mall Admin · 安全登录</div>
    </section>

    <!-- 右侧登录卡片 -->
    <section class="login__panel">
      <div class="login__card">
        <div class="login__card-head">
          <img :src="logoUrl" alt="" class="login__card-logo" />
          <h2>欢迎登录</h2>
          <p>使用管理员账号进入后台</p>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="submit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" autocomplete="username" clearable :prefix-icon="'User'" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" autocomplete="current-password" show-password :prefix-icon="'Lock'" />
          </el-form-item>
          <el-form-item v-if="needCaptcha" prop="captchaCode">
            <div class="captcha">
              <el-input
                ref="captchaInput"
                v-model="form.captchaCode"
                placeholder="图形验证码"
                maxlength="8"
                autocomplete="off"
                :prefix-icon="'Key'"
                class="captcha__input"
              />
              <button type="button" class="captcha__img" title="看不清？点击换一张" @click="refreshCaptcha">
                <img v-if="captcha.image" :src="captcha.image" alt="图形验证码" />
                <span v-else-if="captcha.loading"><el-icon class="is-loading"><Loading /></el-icon></span>
                <span v-else class="captcha__retry">点击获取</span>
              </button>
            </div>
          </el-form-item>
          <div class="login__row">
            <el-checkbox v-model="remember">记住用户名</el-checkbox>
            <span class="text-secondary">连续失败 5 次将锁定 15 分钟</span>
          </div>
          <el-button type="primary" native-type="submit" :loading="loading" class="login__submit">登 录</el-button>
        </el-form>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.login {
  position: relative;
  display: flex;
  min-height: 100vh;
  background: var(--app-login-bg);

  &__theme {
    position: absolute;
    top: 20px;
    right: 20px;
    z-index: 5;
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

  &__art {
    position: relative;
    flex: 1.15;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    padding: 40px 56px;
    color: var(--app-login-card);
    background: linear-gradient(140deg, var(--app-login-art-1) 0%, var(--app-login-art-mid) 45%, var(--app-login-art-2) 100%);
  }
  .blob {
    position: absolute;
    border-radius: 50%;
    filter: blur(2px);
    opacity: 0.55;
    animation: float 12s ease-in-out infinite;
    &--1 {
      width: 420px;
      height: 420px;
      top: -140px;
      right: -120px;
      background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.45), transparent 65%);
    }
    &--2 {
      width: 320px;
      height: 320px;
      bottom: -100px;
      left: -80px;
      background: radial-gradient(circle at 60% 40%, var(--app-login-art-3), transparent 70%);
      animation-delay: -4s;
    }
    &--3 {
      width: 180px;
      height: 180px;
      top: 42%;
      left: 46%;
      background: radial-gradient(circle, rgba(255, 255, 255, 0.3), transparent 70%);
      animation-delay: -8s;
    }
  }
  .grid-bg {
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(rgba(255, 255, 255, 0.07) 1px, transparent 1px),
      linear-gradient(90deg, rgba(255, 255, 255, 0.07) 1px, transparent 1px);
    background-size: 36px 36px;
    mask-image: radial-gradient(ellipse at 30% 40%, #000 30%, transparent 75%);
  }

  &__brand {
    position: relative;
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 18px;
    font-weight: 700;
    img {
      width: 38px;
      height: 38px;
      border-radius: 10px;
      box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
    }
  }
  &__hero {
    position: relative;
    margin-top: 8vh;
    h1 {
      margin: 0;
      font-size: 38px;
      line-height: 1.3;
      font-weight: 700;
      letter-spacing: 1px;
    }
    p {
      margin: 14px 0 0;
      font-size: 16px;
      opacity: 0.85;
    }
  }
  &__features {
    list-style: none;
    padding: 0;
    margin: 28px 0 0;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 220px));
    gap: 12px 20px;
    li {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 14px;
      opacity: 0.92;
    }
  }
  &__feature-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 30px;
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.18);
    backdrop-filter: blur(4px);
  }
  &__illus {
    position: relative;
    width: min(460px, 90%);
    margin: auto 0 0 auto;
    filter: drop-shadow(0 20px 40px rgba(30, 27, 75, 0.25));
    animation: float 9s ease-in-out infinite;
  }
  &__copy {
    position: relative;
    margin-top: 20px;
    font-size: 12px;
    opacity: 0.7;
  }

  &__panel {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px 24px;
  }
  &__card {
    width: 100%;
    max-width: 400px;
    padding: 40px 36px 32px;
    border-radius: 16px;
    background: var(--app-card-bg);
    box-shadow: var(--app-shadow-pop);
    border: 1px solid var(--app-border);
  }
  &__card-head {
    margin-bottom: 28px;
    h2 {
      margin: 16px 0 6px;
      font-size: 24px;
      color: var(--app-text);
    }
    p {
      margin: 0;
      color: var(--app-text-secondary);
    }
  }
  &__card-logo {
    width: 44px;
    height: 44px;
  }
  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin: -4px 0 22px;
    font-size: 12px;
  }
  &__submit {
    width: 100%;
    height: 44px;
    font-size: 15px;
    letter-spacing: 2px;
    border: none;
    background: var(--app-gradient);
    box-shadow: 0 8px 20px rgba(79, 110, 247, 0.35);
    &:hover {
      opacity: 0.92;
    }
  }
}

.captcha {
  display: flex;
  gap: 10px;
  width: 100%;
  &__input {
    flex: 1;
    min-width: 0;
  }
  &__img {
    flex: none;
    width: 120px;
    height: 40px;
    padding: 0;
    border: 1px solid var(--app-border-strong);
    border-radius: var(--el-border-radius-base);
    background: var(--app-card-bg-muted);
    cursor: pointer;
    overflow: hidden;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: var(--app-text-secondary);
    font-size: 12px;
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
    &:hover {
      border-color: var(--el-color-primary);
    }
  }
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-14px);
  }
}

@media (max-width: 992px) {
  .login__art {
    display: none;
  }
  .login__panel {
    background: var(--app-gradient);
  }
}
@media (max-width: 480px) {
  .login__card {
    padding: 32px 22px 24px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .blob,
  .login__illus {
    animation: none;
  }
}
</style>
