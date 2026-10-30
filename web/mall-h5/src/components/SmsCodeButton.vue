<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { showToast } from 'vant'
import type { SmsScene } from '@/types'
import { sendSmsCode } from '@/api/auth'
import { isMobile } from '@/utils'

/**
 * 获取验证码按钮：60 秒倒计时（与后端「同一手机号 60 秒内只能发一次」一致）。
 * 开发模式后端返回 devCode 时通过 filled 事件自动填入。
 */
const props = defineProps<{ mobile: string; scene: SmsScene }>()
const emit = defineEmits<{ filled: [code: string] }>()

const COOLDOWN = 60
const left = ref(0)
const sending = ref(false)
let timer = 0

async function send() {
  if (left.value > 0 || sending.value) return
  if (!isMobile(props.mobile)) return showToast('请输入正确的手机号')
  sending.value = true
  try {
    const res = await sendSmsCode(props.mobile, props.scene)
    left.value = COOLDOWN
    timer = window.setInterval(() => {
      left.value -= 1
      if (left.value <= 0) clearInterval(timer)
    }, 1000)
    if (res?.devCode) {
      emit('filled', res.devCode)
      showToast({ message: `开发模式验证码：${res.devCode}，已自动填入`, duration: 3000 })
    } else {
      showToast('验证码已发送')
    }
  } catch {
    /* 已提示 */
  } finally {
    sending.value = false
  }
}

onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <span class="sms-btn" :class="{ 'is-disabled': left > 0 || sending }" @click="send">
    {{ sending ? '发送中...' : left > 0 ? `${left}s 后重发` : '获取验证码' }}
  </span>
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
</style>
