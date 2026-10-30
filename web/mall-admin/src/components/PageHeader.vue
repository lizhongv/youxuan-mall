<script setup lang="ts">
import { useRouter } from 'vue-router'

/** 详情 / 表单页顶部：返回 + 标题 + 副标题 + 右侧操作 */
const props = withDefaults(defineProps<{ title: string; subtitle?: string; back?: boolean | string }>(), { back: true })
const router = useRouter()

function goBack() {
  if (typeof props.back === 'string') router.push(props.back)
  else if (window.history.state?.back) router.back()
  else router.push('/')
}
</script>

<template>
  <div class="page-header card">
    <div class="page-header__main">
      <el-button v-if="back" class="page-header__back" circle @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <div class="page-header__titles">
        <div class="page-header__title">
          {{ title }}
          <slot name="tag" />
        </div>
        <div v-if="subtitle || $slots.subtitle" class="page-header__sub">
          <slot name="subtitle">{{ subtitle }}</slot>
        </div>
      </div>
    </div>
    <div v-if="$slots.extra" class="page-header__extra">
      <slot name="extra" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px 20px;

  &__main {
    display: flex;
    align-items: center;
    gap: 14px;
    min-width: 0;
  }
  &__titles {
    min-width: 0;
  }
  &__title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 18px;
    font-weight: 600;
    color: var(--app-text);
  }
  &__sub {
    margin-top: 4px;
    font-size: 13px;
    color: var(--app-text-secondary);
  }
  &__extra {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    .el-button + .el-button {
      margin-left: 0;
    }
  }
}
</style>
