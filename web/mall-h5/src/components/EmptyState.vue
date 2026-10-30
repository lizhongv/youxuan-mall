<script setup lang="ts">
/** 空状态：统一插画 + 文案 + 可选按钮 */
withDefaults(
  defineProps<{
    image?: 'default' | 'search' | 'network' | 'error'
    description?: string
    buttonText?: string
  }>(),
  { image: 'default', description: '暂无数据', buttonText: '' },
)
const emit = defineEmits<{ click: [] }>()
</script>

<template>
  <van-empty :image="image" :description="description" class="empty-state">
    <van-button v-if="buttonText" round size="small" class="empty-state__btn btn-gradient" @click="emit('click')">
      {{ buttonText }}
    </van-button>
    <slot />
  </van-empty>
</template>

<style scoped lang="scss">
.empty-state {
  padding: 40px 0;
  &__btn {
    min-width: 120px;
  }
}
</style>
