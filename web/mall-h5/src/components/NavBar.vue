<script setup lang="ts">
import { useRouter } from 'vue-router'

/** 顶部导航：带返回；无历史记录时回首页（直接打开深链接的场景） */
const props = withDefaults(
  defineProps<{
    title?: string
    back?: boolean
    transparent?: boolean
    border?: boolean
    onBack?: () => void
  }>(),
  { title: '', back: true, transparent: false, border: false, onBack: undefined },
)

const router = useRouter()

function goBack() {
  if (props.onBack) return props.onBack()
  if (window.history.state?.back) router.back()
  else router.replace('/')
}
</script>

<template>
  <van-nav-bar
    :title="title"
    :left-arrow="back"
    fixed
    placeholder
    safe-area-inset-top
    :border="border"
    :class="{ 'nav--transparent': transparent }"
    z-index="99"
    @click-left="back && goBack()"
  >
    <template v-if="$slots.title" #title><slot name="title" /></template>
    <template v-if="$slots.right" #right><slot name="right" /></template>
  </van-nav-bar>
</template>

<style scoped lang="scss">
.nav--transparent {
  background: transparent;
}
</style>
