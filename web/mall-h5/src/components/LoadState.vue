<script setup lang="ts">
/**
 * 页面加载状态容器：加载中 → 失败（可重试）→ 空 → 内容。
 * 只有尚未拿到数据时才显示整页 loading / 错误，已有数据的刷新失败不打断页面。
 */
withDefaults(
  defineProps<{
    loading?: boolean
    /** 错误文案；非空即视为失败 */
    error?: string
    /** 是否已有可展示的数据 */
    ready?: boolean
    /** 数据为空 */
    empty?: boolean
    emptyText?: string
    emptyImage?: 'default' | 'search' | 'network' | 'error'
  }>(),
  { loading: false, error: '', ready: false, empty: false, emptyText: '暂无数据', emptyImage: 'default' },
)
const emit = defineEmits<{ retry: [] }>()
</script>

<template>
  <van-loading v-if="loading && !ready" class="load-state__loading" vertical>加载中...</van-loading>
  <van-empty v-else-if="error && !ready" image="network" class="load-state__error">
    <template #description>
      <div class="load-state__title">加载失败</div>
      <div class="load-state__msg">{{ error }}</div>
    </template>
    <van-button round size="small" class="btn-gradient load-state__btn" @click="emit('retry')">重新加载</van-button>
  </van-empty>
  <template v-else-if="empty">
    <slot name="empty">
      <van-empty :image="emptyImage" :description="emptyText" class="load-state__empty" />
    </slot>
  </template>
  <slot v-else />
</template>

<style scoped lang="scss">
.load-state {
  &__loading {
    padding: 60px 0;
  }
  &__error,
  &__empty {
    padding: 40px 0;
  }
  &__title {
    font-size: 15px;
    color: $text;
  }
  &__msg {
    margin-top: 4px;
    font-size: 13px;
    color: $text-3;
  }
  &__btn {
    min-width: 120px;
  }
}
</style>
