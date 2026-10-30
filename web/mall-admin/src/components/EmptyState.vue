<script setup lang="ts">
/** 空状态：纯 SVG 插画，颜色取自主题变量 */
withDefaults(defineProps<{ text?: string; size?: number }>(), { text: '暂无数据', size: 120 })
</script>

<template>
  <div class="empty-state">
    <svg :width="size" :height="size * 0.75" viewBox="0 0 160 120" fill="none" aria-hidden="true">
      <ellipse cx="80" cy="106" rx="56" ry="8" class="e-shadow" />
      <rect x="34" y="28" width="92" height="70" rx="10" class="e-box" />
      <path d="M34 50h92" class="e-line" />
      <rect x="46" y="60" width="40" height="6" rx="3" class="e-bar" />
      <rect x="46" y="74" width="62" height="6" rx="3" class="e-bar e-bar--light" />
      <circle cx="45" cy="39" r="3" class="e-dot" />
      <circle cx="55" cy="39" r="3" class="e-dot" />
      <circle cx="65" cy="39" r="3" class="e-dot" />
      <circle cx="118" cy="24" r="12" class="e-badge" />
      <path d="M113 24h10M118 19v10" class="e-plus" />
    </svg>
    <p>{{ text }}</p>
    <slot />
  </div>
</template>

<style scoped lang="scss">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0;
  color: var(--app-text-secondary);
  font-size: 13px;
  line-height: 1.5;

  p {
    margin: 8px 0 0;
  }
  .e-shadow {
    fill: var(--app-border);
  }
  .e-box {
    fill: var(--app-card-bg-muted);
    stroke: var(--app-border-strong);
    stroke-width: 2;
  }
  .e-line {
    stroke: var(--app-border-strong);
    stroke-width: 2;
  }
  .e-bar {
    fill: var(--el-color-primary-light-7);
    &--light {
      fill: var(--app-border);
    }
  }
  .e-dot {
    fill: var(--app-border-strong);
  }
  .e-badge {
    fill: var(--el-color-primary);
    opacity: 0.9;
  }
  .e-plus {
    stroke: var(--el-color-white);
    stroke-width: 2;
    stroke-linecap: round;
  }
}
</style>
