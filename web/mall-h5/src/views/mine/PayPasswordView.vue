<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import { setPayPassword } from '@/api/member'
import { ApiError } from '@/api/request'
import { useUserStore } from '@/stores/user'
import NavBar from '@/components/NavBar.vue'
import SmsCodeButton from '@/components/SmsCodeButton.vue'

/**
 * 设置 / 修改余额支付密码：
 * ① 图形验证码 + 短信验证码（发到当前账号手机）→ ② 输入 6 位数字 → ③ 再次确认 → 提交。
 * 首次设置与修改同一流程，不需要旧密码。
 * 从收银台进入时带 redirect，设置成功后返回收银台。
 */
const route = useRoute()
const router = useRouter()
const user = useUserStore()

const step = ref<1 | 2 | 3>(1)
const code = ref('')
const pwd = ref('')
const confirmPwd = ref('')
const keyboard = ref(true)
const submitting = ref(false)
const errorInfo = ref('')

onMounted(() => user.fetchProfile())

const masked = computed(() => user.profile?.mobile || '')
const isReset = computed(() => !!user.profile?.hasPayPassword)

function next() {
  if (!/^\d{6}$/.test(code.value)) return showToast('请输入 6 位短信验证码')
  step.value = 2
  keyboard.value = true
}

function isWeak(p: string) {
  return /^(\d)\1{5}$/.test(p) || '0123456789'.includes(p) || '9876543210'.includes(p)
}

watch(pwd, (v) => {
  errorInfo.value = ''
  if (v.length < 6) return
  if (isWeak(v)) {
    errorInfo.value = '密码过于简单，请勿使用连续或重复的数字'
    pwd.value = ''
    return
  }
  step.value = 3
})

watch(confirmPwd, async (v) => {
  errorInfo.value = ''
  if (v.length < 6) return
  if (v !== pwd.value) {
    errorInfo.value = '两次输入的密码不一致，请重新设置'
    pwd.value = ''
    confirmPwd.value = ''
    step.value = 2
    return
  }
  await submit()
})

async function submit() {
  submitting.value = true
  try {
    await setPayPassword({ code: code.value, payPassword: pwd.value })
    showSuccessToast(isReset.value ? '支付密码已修改' : '支付密码已设置')
    if (user.profile) user.profile.hasPayPassword = true
    const redirect = route.query.redirect
    if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')) router.replace(redirect)
    else router.back()
  } catch (e) {
    pwd.value = ''
    confirmPwd.value = ''
    if (e instanceof ApiError && /简单|弱|连续|相同/.test(e.message)) {
      // 弱密码在消耗验证码前被拒绝：验证码仍有效，直接重新设置
      errorInfo.value = e.message
      step.value = 2
    } else {
      // 验证码错误 / 过期等：回到第一步重新获取
      code.value = ''
      step.value = 1
    }
  } finally {
    submitting.value = false
  }
}

const current = computed({
  get: () => (step.value === 3 ? confirmPwd.value : pwd.value),
  set: (v: string) => {
    if (step.value === 3) confirmPwd.value = v
    else pwd.value = v
  },
})
</script>

<template>
  <div class="page ppwd">
    <NavBar :title="isReset ? '修改支付密码' : '设置支付密码'" />

    <template v-if="step === 1">
      <div class="ppwd__tip">
        支付密码用于余额支付，为保障资金安全，请先验证手机号<template v-if="masked">（{{ masked }}）</template>
      </div>
      <div class="auth ppwd__form">
        <div class="auth__field ppwd__mobile">
          <span class="auth__prefix">手机号</span>
          <span>{{ masked || '当前账号绑定的手机号' }}</span>
        </div>
        <div class="auth__field">
          <input v-model="code" type="tel" maxlength="6" placeholder="请输入短信验证码" autocomplete="one-time-code" />
          <SmsCodeButton scene="PAY_PASSWORD" @filled="(c: string) => (code = c)" />
        </div>
      </div>
      <div class="ppwd__btn">
        <van-button block round class="btn-gradient" @click="next">下一步</van-button>
      </div>
    </template>

    <template v-else>
      <div class="ppwd__title">{{ step === 2 ? '请设置 6 位数字支付密码' : '请再次输入以确认' }}</div>
      <van-password-input
        :value="current"
        :focused="keyboard"
        :error-info="errorInfo"
        info="请勿使用生日、连续或重复的数字"
        @focus="keyboard = true"
      />
      <van-loading v-if="submitting" class="ppwd__loading" size="20">提交中...</van-loading>
      <van-number-keyboard
        v-model="current"
        :show="keyboard"
        :maxlength="6"
        random-key-order
        safe-area-inset-bottom
        @blur="keyboard = false"
      />
    </template>
  </div>
</template>

<style scoped lang="scss">
.ppwd {
  background: $card;
  &__tip {
    padding: 16px 28px 0;
    font-size: 13px;
    line-height: 20px;
    color: $text-2;
  }
  &__form {
    padding: 4px 28px 0;
  }
  &__btn {
    padding: 32px 28px;
  }
  &__title {
    padding: 48px 0 24px;
    text-align: center;
    font-size: 16px;
    font-weight: 600;
  }
  &__loading {
    margin-top: 16px;
    text-align: center;
  }
}
</style>
