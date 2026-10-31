<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref } from 'vue'
import { showToast } from 'vant'
import type { SmsScene } from '@/types'
import { sendSmsCode } from '@/api/auth'
import { sendPayPasswordSms } from '@/api/member'
import { isMobile } from '@/utils'
import CaptchaInput from './CaptchaInput.vue'

/**
 * scene=PAY_PASSWORD 时走会员接口（发到当前登录会员手机，不需要 mobile）。
 * 获取短信验证码按钮：点击先弹出图形验证码，通过后发送；60 秒倒计时（与后端限频一致）。
 * 图形验证码一次性，发送失败自动换新。demo 模式返回 devCode 时通过 filled 事件自动填入。
 */
const props = withDefaults(defineProps<{ mobile?: string; scene: SmsScene }>(), { mobile: '' })
const emit = defineEmits<{ filled: [code: string]; sent: [] }>()

const COOLDOWN = 60
const left = ref(0)
const sending = ref(false)
const show = ref(false)
const captchaCode = ref('')
const captchaRef = ref<InstanceType<typeof CaptchaInput>>()
let timer = 0

function open() {
  if (left.value > 0 || sending.value) return
  if (props.scene !== 'PAY_PASSWORD' && !isMobile(props.mobile)) return showToast('请输入正确的手机号')
  show.value = true
  nextTick(() => captchaRef.value?.refresh())
}

async function send() {
  const answer = captchaRef.value?.answer()
  if (!answer?.captchaId) return showToast('图形验证码加载中，请稍候')
  if (!answer.captchaCode) return showToast('请输入图形验证码')
  sending.value = true
  try {
    const res =
      props.scene === 'PAY_PASSWORD' ? await sendPayPasswordSms(answer) : await sendSmsCode(props.mobile, props.scene, answer)
    show.value = false
    left.value = COOLDOWN
    clearInterval(timer)
    timer = window.setInterval(() => {
      left.value -= 1
      if (left.value <= 0) clearInterval(timer)
    }, 1000)
    emit('sent')
    if (res?.devCode) {
      emit('filled', res.devCode)
      showToast({ message: `演示环境验证码：${res.devCode}，已自动填入`, duration: 3000 })
    } else {
      showToast('验证码已发送')
    }
  } catch {
    // 已提示；图形验证码一次性，失败后换一张
    captchaRef.value?.refresh()
  } finally {
    sending.value = false
  }
}

onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <span class="sms-btn" :class="{ 'is-disabled': left > 0 || sending }" @click="open">
    {{ sending ? '发送中...' : left > 0 ? `${left}s 后重发` : '获取验证码' }}
  </span>
  <van-popup v-model:show="show" round class="sms-captcha" :close-on-click-overlay="!sending" teleport="body">
    <div class="sms-captcha__title">安全验证</div>
    <div class="sms-captcha__desc">请输入图形验证码，点击图片可换一张</div>
    <CaptchaInput ref="captchaRef" v-model="captchaCode" :autoload="false" class="sms-captcha__field" placeholder="图形验证码" />
    <div class="sms-captcha__btns">
      <van-button round block @click="show = false">取消</van-button>
      <van-button round block class="btn-gradient" :loading="sending" @click="send">发送验证码</van-button>
    </div>
  </van-popup>
</template>

<style scoped lang="scss">
.sms-btn {
  flex-shrink: 0;
  font-size: 14px;
  color: $primary;
  white-space: nowrap;
  &.is-disabled {
    color: $text-3;
  }
}
.sms-captcha {
  width: 300px;
  padding: 20px 18px 16px;
  &__title {
    text-align: center;
    font-size: 16px;
    font-weight: 600;
  }
  &__desc {
    margin-top: 6px;
    text-align: center;
    font-size: 12px;
    color: $text-3;
  }
  &__field {
    height: 48px;
    margin-top: 14px;
    padding: 0 4px 0 12px;
    border: 1px solid $border;
    border-radius: $radius-sm;
  }
  &__btns {
    display: flex;
    gap: 10px;
    margin-top: 18px;
  }
}
</style>
