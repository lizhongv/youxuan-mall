<script setup lang="ts">
import { computed } from 'vue'
import { parseTime } from '@/utils'

/**
 * 倒计时：传入截止时间（字符串或毫秒）。
 * variant=block 为秒杀样式数字块（时:分:秒，超过 1 天显示「x天」）；text 为纯文本。
 */
const props = withDefaults(
  defineProps<{
    end: string | number | null | undefined
    variant?: 'block' | 'text'
    theme?: 'primary' | 'white' | 'dark'
  }>(),
  { variant: 'block', theme: 'primary' },
)

const emit = defineEmits<{ finish: [] }>()

const remain = computed(() => {
  const endMs = typeof props.end === 'number' ? props.end : parseTime(props.end)
  return Math.max(0, endMs - Date.now())
})

const pad = (n: number) => String(n).padStart(2, '0')
</script>

<template>
  <van-count-down :time="remain" @finish="emit('finish')">
    <template #default="t">
      <span v-if="variant === 'block'" class="cd" :class="`cd--${theme}`">
        <template v-if="t.days > 0"><span class="cd__day">{{ t.days }}天</span></template>
        <span class="cd__num">{{ pad(t.hours) }}</span><span class="cd__sep">:</span>
        <span class="cd__num">{{ pad(t.minutes) }}</span><span class="cd__sep">:</span>
        <span class="cd__num">{{ pad(t.seconds) }}</span>
      </span>
      <span v-else class="cd-text">
        <template v-if="t.days > 0">{{ t.days }}天</template>{{ pad(t.hours) }}:{{ pad(t.minutes) }}:{{ pad(t.seconds) }}
      </span>
    </template>
  </van-count-down>
</template>

<style scoped lang="scss">
.cd {
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  line-height: 1;
  &__num {
    display: inline-block;
    min-width: 18px;
    padding: 3px 2px;
    border-radius: 3px;
    text-align: center;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }
  &__sep {
    margin: 0 2px;
    font-weight: 600;
  }
  &__day {
    margin-right: 3px;
  }
  &--primary {
    .cd__num {
      background: $primary;
      color: #fff;
    }
    .cd__sep,
    .cd__day {
      color: $primary;
    }
  }
  &--white {
    .cd__num {
      background: #fff;
      color: $primary;
    }
    .cd__sep,
    .cd__day {
      color: #fff;
    }
  }
  &--dark {
    .cd__num {
      background: $text;
      color: #fff;
    }
    .cd__sep,
    .cd__day {
      color: $text;
    }
  }
}
.cd-text {
  font-variant-numeric: tabular-nums;
}
</style>
