<script setup lang="ts">
import { useRouter } from 'vue-router'

/** 用户协议勾选：默认不勾选，协议名可点开查看 */
const agree = defineModel<boolean>({ default: false })
const router = useRouter()
</script>

<template>
  <div class="agreement-check">
    <van-checkbox v-model="agree" icon-size="14">
      我已阅读并同意<span class="text-primary" @click.stop="router.push({ name: 'agreement', params: { type: 'user' } })">《用户协议》</span>和<span
        class="text-primary"
        @click.stop="router.push({ name: 'agreement', params: { type: 'privacy' } })"
        >《隐私政策》</span
      >
    </van-checkbox>
  </div>
</template>

<style scoped lang="scss">
.agreement-check {
  display: flex;
  justify-content: center;
  padding: 40px 28px calc(24px + env(safe-area-inset-bottom));
  :deep(.van-checkbox__label) {
    color: $text-3;
    font-size: 12px;
  }
}
</style>
