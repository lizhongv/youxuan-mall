<script setup lang="ts">
import { computed } from 'vue'
import { money } from '@/utils'

/**
 * 价格：¥ 小号、整数大号、小数小号。
 * size 控制整数字号；strike 用于划线原价；color 为 inherit 时继承父级颜色。
 */
const props = withDefaults(
  defineProps<{
    value: number | string | null | undefined
    size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
    color?: 'primary' | 'inherit' | 'white'
    strike?: boolean
    prefix?: string
  }>(),
  { size: 'md', color: 'primary', strike: false, prefix: '' },
)

const parts = computed(() => {
  const [int, dec] = money(props.value).split('.')
  return { int, dec }
})
</script>

<template>
  <span v-if="strike" class="price-strike">¥{{ money(value) }}</span>
  <span v-else class="price" :class="[`price--${size}`, `price--${color}`]">
    <span v-if="prefix" class="price__prefix">{{ prefix }}</span>
    <span class="price__symbol">¥</span><span class="price__int">{{ parts.int }}</span
    ><span class="price__dec">.{{ parts.dec }}</span>
  </span>
</template>

<style scoped lang="scss">
.price {
  display: inline-flex;
  align-items: baseline;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  &--primary {
    color: $primary;
  }
  &--inherit {
    color: inherit;
  }
  &--white {
    color: #fff;
  }
  &__prefix {
    font-size: 12px;
    margin-right: 2px;
    font-weight: normal;
  }
  &__symbol {
    font-size: 11px;
    margin-right: 1px;
  }
  &__int {
    font-size: 16px;
  }
  &__dec {
    font-size: 11px;
  }
  &--xs .price__int {
    font-size: 13px;
  }
  &--xs .price__dec,
  &--xs .price__symbol {
    font-size: 10px;
  }
  &--sm .price__int {
    font-size: 15px;
  }
  &--lg .price__int {
    font-size: 22px;
  }
  &--lg .price__symbol,
  &--lg .price__dec {
    font-size: 13px;
  }
  &--xl .price__int {
    font-size: 32px;
  }
  &--xl .price__symbol,
  &--xl .price__dec {
    font-size: 16px;
  }
}
.price-strike {
  font-size: 11px;
  color: $text-3;
  text-decoration: line-through;
  white-space: nowrap;
}
</style>
