<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getCaptcha } from '@/api/auth'
import type { CaptchaAnswer } from '@/types'

/**
 * 图形验证码输入：输入框 + 验证码图片（点击刷新）。
 * 验证码一次性：每次提交失败后调用方应 refresh()。demo 模式返回 devCode 时自动填入。
 */
const code = defineModel<string>({ default: '' })
const props = withDefaults(defineProps<{ autoload?: boolean; placeholder?: string }>(), {
  autoload: true,
  placeholder: '请输入图形验证码',
})

const captchaId = ref('')
const image = ref('')
const loading = ref(false)
const failed = ref(false)

async function refresh() {
  if (loading.value) return
  loading.value = true
  failed.value = false
  code.value = ''
  try {
    const c = await getCaptcha()
    captchaId.value = c.captchaId
    image.value = c.image
    if (c.devCode) code.value = c.devCode
  } catch {
    failed.value = true
    image.value = ''
  } finally {
    loading.value = false
  }
}

function answer(): CaptchaAnswer {
  return { captchaId: captchaId.value, captchaCode: code.value.trim() }
}

onMounted(() => {
  if (props.autoload) refresh()
})

defineExpose({ refresh, answer })
</script>

<template>
  <div class="captcha-input">
    <input v-model="code" type="text" maxlength="6" :placeholder="placeholder" autocomplete="off" autocapitalize="off" />
    <div class="captcha-input__img" @click="refresh">
      <van-loading v-if="loading" size="16" />
      <img v-else-if="image" :src="image" alt="图形验证码" />
      <span v-else class="captcha-input__retry">{{ failed ? '加载失败，点击重试' : '点击获取' }}</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.captcha-input {
  display: flex;
  align-items: center;
  gap: 10px;
  input {
    flex: 1;
    min-width: 0;
    height: 100%;
    border: none;
    outline: none;
    background: transparent;
    font-size: 16px;
    color: $text;
    &::placeholder {
      color: $disabled;
    }
  }
  &__img {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100px;
    height: 36px;
    border-radius: $radius-sm;
    background: $bg;
    overflow: hidden;
    img {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
  }
  &__retry {
    font-size: 11px;
    color: $text-3;
    text-align: center;
    line-height: 14px;
    padding: 0 4px;
  }
}
</style>
