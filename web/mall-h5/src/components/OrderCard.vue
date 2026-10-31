<script setup lang="ts">
import { fmtSpec } from '@/utils'
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { Order } from '@/types'
import { ACTION_TEXT, orderButtons, type OrderAction } from '@/composables/useOrderActions'
import { ORDER_STATUS_TEXT } from '@/utils/dict'
import Price from './Price.vue'
import CountDown from './CountDown.vue'

/** 订单卡片：店铺式头部（订单号 + 状态）、商品缩略、合计、按状态的操作按钮 */
const props = defineProps<{ order: Order }>()
const emit = defineEmits<{ action: [type: OrderAction, order: Order]; expired: [order: Order] }>()
const router = useRouter()

const buttons = computed(() => orderButtons(props.order))
const totalQty = computed(() => props.order.items.reduce((a, i) => a + i.quantity, 0))
const statusText = computed(() => props.order.statusText || ORDER_STATUS_TEXT[props.order.status] || props.order.status)
const multi = computed(() => props.order.items.length > 1)

function open() {
  router.push({ name: 'order-detail', params: { id: props.order.id } })
}
</script>

<template>
  <div class="order-card" @click="open">
    <div class="order-card__head">
      <div class="order-card__shop">
        <van-icon name="shop-o" />
        <span class="ellipsis">订单号 {{ order.orderNo }}</span>
      </div>
      <span class="order-card__status" :class="`is-${order.status}`">{{ statusText }}</span>
    </div>

    <div v-if="multi" class="order-card__thumbs">
      <div class="order-card__thumb-list">
        <van-image v-for="it in order.items" :key="it.id" :src="it.pic" width="72" height="72" radius="6" fit="cover" lazy-load />
      </div>
      <div class="order-card__thumb-count">共{{ totalQty }}件</div>
    </div>
    <div v-else v-for="it in order.items" :key="it.id" class="order-card__item">
      <van-image :src="it.pic" width="80" height="80" radius="6" fit="cover" lazy-load />
      <div class="order-card__item-info">
        <div class="order-card__item-name ellipsis-2">{{ it.goodsName }}</div>
        <div v-if="it.specText" class="order-card__item-spec ellipsis">{{ fmtSpec(it.specText) }}</div>
      </div>
      <div class="order-card__item-right">
        <Price :value="it.price" size="xs" color="inherit" />
        <div class="order-card__item-qty">x{{ it.quantity }}</div>
      </div>
    </div>

    <div class="order-card__sum">
      <span v-if="order.status === 'PENDING_PAY' && order.payExpireAt" class="order-card__expire">
        剩余 <CountDown :end="order.payExpireAt" variant="text" @finish="emit('expired', order)" />
      </span>
      <span class="order-card__sum-text">
        {{ order.status === 'PENDING_PAY' ? '需付款' : '实付款' }}
        <Price :value="order.payAmount" size="sm" color="inherit" />
      </span>
    </div>

    <div v-if="buttons.length" class="order-card__actions" @click.stop>
      <van-button
        v-for="(b, i) in buttons"
        :key="b"
        size="small"
        round
        :class="i === buttons.length - 1 ? 'order-card__btn--primary' : 'order-card__btn'"
        @click="emit('action', b, order)"
      >
        {{ ACTION_TEXT[b] }}
      </van-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.order-card {
  @include card;
  margin: 10px $gap 0;
  padding: 12px;
  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 10px;
  }
  &__shop {
    display: flex;
    align-items: center;
    gap: 4px;
    min-width: 0;
    font-size: 13px;
    color: $text-2;
  }
  &__status {
    flex-shrink: 0;
    font-size: 13px;
    color: $primary;
    &.is-COMPLETED,
    &.is-CANCELLED {
      color: $text-3;
    }
  }
  &__thumbs {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  &__thumb-list {
    flex: 1;
    min-width: 0;
    display: flex;
    gap: 6px;
    overflow-x: auto;
    scrollbar-width: none;
    &::-webkit-scrollbar {
      display: none;
    }
    :deep(.van-image) {
      flex-shrink: 0;
    }
  }
  &__thumb-count {
    flex-shrink: 0;
    font-size: 12px;
    color: $text-3;
  }
  &__item {
    display: flex;
    gap: 10px;
  }
  &__item-info {
    flex: 1;
    min-width: 0;
  }
  &__item-name {
    font-size: 13px;
    line-height: 18px;
  }
  &__item-spec {
    margin-top: 6px;
    display: inline-block;
    max-width: 100%;
    padding: 2px 6px;
    font-size: 11px;
    color: $text-3;
    background: $bg;
    border-radius: 3px;
  }
  &__item-right {
    flex-shrink: 0;
    text-align: right;
    color: $text;
  }
  &__item-qty {
    margin-top: 6px;
    font-size: 12px;
    color: $text-3;
  }
  &__sum {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 10px;
    font-size: 12px;
    color: $text-2;
  }
  &__expire {
    margin-right: auto;
    color: $primary;
    display: inline-flex;
    align-items: center;
    gap: 2px;
    :deep(.van-count-down) {
      color: $primary;
      font-size: 12px;
    }
  }
  &__sum-text {
    color: $text;
  }
  &__actions {
    display: flex;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 12px;
  }
  &__btn {
    min-width: 76px;
    color: $text;
    border-color: $border-strong;
  }
  &__btn--primary {
    min-width: 76px;
    color: $primary;
    border-color: $primary;
    background: $card;
  }
}
</style>
