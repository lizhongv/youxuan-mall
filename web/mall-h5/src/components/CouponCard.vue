<script setup lang="ts">
import { computed } from 'vue'
import type { CouponLike } from '@/types'
import { fmtTime } from '@/utils'

/**
 * 票券卡片：左侧金额/折扣，右侧名称、条件、有效期；右下角插槽放操作按钮。
 * disabled 时整体置灰（已使用、已过期、不可用）；stamp 显示印章文字。
 */

const props = withDefaults(
  defineProps<{ coupon: CouponLike; disabled?: boolean; stamp?: string; reason?: string; selected?: boolean }>(),
  { disabled: false, stamp: '', reason: '', selected: false },
)

const isDiscount = computed(() => props.coupon.type === 'DISCOUNT')

const discountText = computed(() => {
  const d = Number(props.coupon.discount || 1) * 10
  return Number.isInteger(d) ? String(d) : d.toFixed(1)
})

const condition = computed(() => {
  const c = props.coupon
  const th = Number(c.threshold || 0)
  if (c.type === 'NO_THRESHOLD' || th <= 0) return '无门槛'
  return `满${th}元可用`
})

const validity = computed(() => {
  const c = props.coupon
  if (c.validType === 'DAYS' && c.validDays) return `领取后${c.validDays}天内有效`
  if (c.startAt && c.endAt) return `${fmtTime(c.startAt, 'YYYY.MM.DD')} - ${fmtTime(c.endAt, 'YYYY.MM.DD')}`
  if (c.endAt) return `${fmtTime(c.endAt, 'YYYY.MM.DD HH:mm')} 到期`
  return ''
})
</script>

<template>
  <div class="coupon" :class="{ 'coupon--disabled': disabled, 'coupon--selected': selected }">
    <div class="coupon__left">
      <div v-if="isDiscount" class="coupon__value">
        <span class="coupon__num">{{ discountText }}</span><span class="coupon__unit">折</span>
      </div>
      <div v-else class="coupon__value">
        <span class="coupon__unit">¥</span><span class="coupon__num">{{ Number(coupon.amount) }}</span>
      </div>
      <div class="coupon__cond">{{ condition }}</div>
    </div>
    <div class="coupon__right">
      <div class="coupon__name ellipsis-2">
        <span class="coupon__type">{{ isDiscount ? '折扣券' : coupon.type === 'NO_THRESHOLD' ? '无门槛' : '满减券' }}</span>
        {{ coupon.name }}
      </div>
      <div v-if="isDiscount && coupon.maxAmount && Number(coupon.maxAmount) > 0" class="coupon__desc">
        最高减{{ Number(coupon.maxAmount) }}元
      </div>
      <div v-else-if="coupon.description" class="coupon__desc ellipsis">{{ coupon.description }}</div>
      <div class="coupon__foot">
        <span class="coupon__time">{{ validity }}</span>
        <div class="coupon__action"><slot /></div>
      </div>
      <div v-if="reason" class="coupon__reason">{{ reason }}</div>
    </div>
    <div v-if="stamp" class="coupon__stamp">{{ stamp }}</div>
  </div>
</template>

<style scoped lang="scss">
.coupon {
  position: relative;
  display: flex;
  min-height: 96px;
  background: $card;
  border-radius: $radius;
  overflow: hidden;
  &__left {
    position: relative;
    flex: 0 0 100px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #fff;
    background: $gradient;
    // 票券锯齿：左侧打孔效果
    &::after {
      content: '';
      position: absolute;
      top: 0;
      bottom: 0;
      right: -4px;
      width: 8px;
      background: radial-gradient(circle at 4px 6px, $card 3px, transparent 3.5px) 0 0 / 8px 12px repeat-y;
    }
  }
  &__value {
    display: flex;
    align-items: baseline;
    font-weight: 700;
  }
  &__num {
    font-size: 30px;
    line-height: 1;
  }
  &__unit {
    font-size: 13px;
    margin: 0 1px;
  }
  &__cond {
    margin-top: 6px;
    font-size: 11px;
    opacity: 0.92;
  }
  &__right {
    flex: 1;
    min-width: 0;
    padding: 10px 12px 10px 14px;
    display: flex;
    flex-direction: column;
  }
  &__name {
    font-size: 14px;
    font-weight: 600;
    color: $text;
    line-height: 19px;
  }
  &__type {
    display: inline-block;
    padding: 0 4px;
    margin-right: 2px;
    border: 1px solid $primary;
    color: $primary;
    border-radius: 3px;
    font-size: 10px;
    line-height: 14px;
    font-weight: normal;
    vertical-align: 1px;
  }
  &__desc {
    margin-top: 4px;
    font-size: 12px;
    color: $text-3;
  }
  &__foot {
    margin-top: auto;
    padding-top: 6px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 6px;
  }
  &__time {
    font-size: 11px;
    color: $text-3;
    min-width: 0;
  }
  &__action {
    flex-shrink: 0;
  }
  &__reason {
    margin-top: 6px;
    padding-top: 6px;
    border-top: 1px dashed $border;
    font-size: 11px;
    color: $warning;
  }
  &__stamp {
    position: absolute;
    right: 10px;
    top: 8px;
    width: 52px;
    height: 52px;
    border: 2px solid $text-3;
    border-radius: 50%;
    color: $text-3;
    font-size: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    transform: rotate(-20deg);
    opacity: 0.6;
  }
  &--selected {
    box-shadow: 0 0 0 1px $primary inset;
  }
  &--disabled {
    .coupon__left {
      background: $disabled;
    }
    .coupon__type {
      border-color: $text-3;
      color: $text-3;
    }
    .coupon__name {
      color: $text-2;
    }
  }
}
</style>
