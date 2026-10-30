<script setup lang="ts">
import { computed } from 'vue'
import type { EnumOption, Tone } from '@/utils/constants'

/** 状态标签：带色点的柔和底色胶囊，颜色由 tone 决定，全站统一 */
const props = withDefaults(
  defineProps<{
    value?: string | number | null
    options?: EnumOption<string | number>[]
    /** 直接指定文字与色调（不传 options 时） */
    label?: string
    tone?: Tone
    dot?: boolean
  }>(),
  { dot: true },
)

const matched = computed(() => props.options?.find((o) => String(o.value) === String(props.value)))
const text = computed(() => props.label ?? matched.value?.label ?? (props.value ?? '-'))
const toneClass = computed(() => `is-${props.tone ?? matched.value?.tone ?? 'info'}`)
</script>

<template>
  <span class="status-tag" :class="toneClass">
    <i v-if="dot" class="status-tag__dot" />
    <span>{{ text }}</span>
  </span>
</template>

<style scoped lang="scss">
.status-tag {
  --tone: var(--app-info);
  --tone-soft: var(--app-info-soft);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 24px;
  padding: 0 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
  color: var(--tone);
  background: var(--tone-soft);

  &__dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: currentColor;
  }

  &.is-primary {
    --tone: var(--el-color-primary);
    --tone-soft: var(--app-primary-soft);
  }
  &.is-success {
    --tone: var(--app-success);
    --tone-soft: var(--app-success-soft);
  }
  &.is-warning {
    --tone: var(--app-warning);
    --tone-soft: var(--app-warning-soft);
  }
  &.is-danger {
    --tone: var(--app-danger);
    --tone-soft: var(--app-danger-soft);
  }
  &.is-purple {
    --tone: var(--app-purple);
    --tone-soft: var(--app-purple-soft);
  }
  &.is-cyan {
    --tone: var(--app-cyan);
    --tone-soft: var(--app-cyan-soft);
  }
}
</style>
