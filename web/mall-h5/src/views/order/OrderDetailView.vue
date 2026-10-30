<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { Order, OrderItem } from '@/types'
import { getOrder } from '@/api/order'
import { ACTION_TEXT, canApplyAftersale, orderButtons, useOrderActions, type OrderAction } from '@/composables/useOrderActions'
import { ORDER_STATUS_DESC, ORDER_STATUS_TEXT, ITEM_AFTERSALE_TEXT, PAY_TYPE_TEXT } from '@/utils/dict'
import { copyText, fmtTime, money } from '@/utils'
import NavBar from '@/components/NavBar.vue'
import Price from '@/components/Price.vue'
import CountDown from '@/components/CountDown.vue'
import EmptyState from '@/components/EmptyState.vue'
import CancelSheet from '@/components/CancelSheet.vue'

const route = useRoute()
const router = useRouter()
const id = route.params.id as string
const order = ref<Order | null>(null)
const loading = ref(true)

async function load() {
  try {
    order.value = await getOrder(id)
  } finally {
    loading.value = false
  }
}
onMounted(load)

const cancelShow = ref(false)
const { handle, doCancel } = useOrderActions((type) => {
  if (type === 'delete') router.replace({ name: 'order-list' })
  else load()
})

/** 详情页不显示「查看物流」（物流信息已在页面上） */
const buttons = computed(() => (order.value ? orderButtons(order.value).filter((b) => b !== 'logistics') : []))
const statusText = computed(() => (order.value ? order.value.statusText || ORDER_STATUS_TEXT[order.value.status] : ''))
const statusIcon = computed(() => {
  switch (order.value?.status) {
    case 'PENDING_PAY':
      return 'pending-payment'
    case 'PENDING_DELIVERY':
      return 'send-gift-o'
    case 'DELIVERED':
      return 'logistics'
    case 'COMPLETED':
      return 'passed'
    default:
      return 'close'
  }
})

function onAction(type: OrderAction) {
  if (!order.value) return
  handle(type, order.value, () => (cancelShow.value = true))
}

function itemAftersale(item: OrderItem) {
  if (!order.value) return
  if (canApplyAftersale(order.value, item)) {
    router.push({ name: 'aftersale-apply', query: { orderId: order.value.id, itemId: item.id } })
  } else if (item.aftersaleId) {
    router.push({ name: 'aftersale-detail', params: { id: item.aftersaleId } })
  } else {
    router.push({ name: 'aftersale-list' })
  }
}

function itemReview(item: OrderItem) {
  router.push({ name: 'review-create', query: { orderId: id, itemId: item.id } })
}
</script>

<template>
  <div class="page od">
    <NavBar title="订单详情" />
    <van-loading v-if="loading" class="od__loading" vertical>加载中...</van-loading>
    <EmptyState v-else-if="!order" description="订单不存在" />
    <template v-else>
      <!-- 状态头 -->
      <div class="od__status">
        <div class="od__status-main">
          <div>
            <div class="od__status-text">{{ statusText }}</div>
            <div class="od__status-desc">
              <template v-if="order.status === 'PENDING_PAY' && order.payExpireAt">
                剩余 <CountDown :end="order.payExpireAt" theme="white" @finish="load" /> 自动关闭
              </template>
              <template v-else-if="order.status === 'CANCELLED' && order.cancelReason">
                取消原因：{{ order.cancelReason }}
              </template>
              <template v-else>{{ ORDER_STATUS_DESC[order.status] }}</template>
            </div>
          </div>
          <van-icon :name="statusIcon" class="od__status-icon" />
        </div>
      </div>

      <!-- 物流 -->
      <div v-if="order.deliveryNo" class="card od__logistics">
        <van-icon name="logistics" class="od__ico" />
        <div class="od__logi-body">
          <div class="od__logi-title">{{ order.deliveryCompany || '快递' }}</div>
          <div class="od__logi-no">
            运单号：{{ order.deliveryNo }}
            <span class="od__copy" @click="copyText(order.deliveryNo!)">复制</span>
          </div>
          <div v-if="order.deliveryTime" class="od__logi-time">发货时间 {{ fmtTime(order.deliveryTime) }}</div>
        </div>
      </div>

      <!-- 地址 -->
      <div class="card od__addr">
        <van-icon name="location-o" class="od__ico" />
        <div class="od__addr-body">
          <div class="od__addr-user">{{ order.receiverName }} <span>{{ order.receiverMobile }}</span></div>
          <div class="od__addr-detail">
            {{ order.receiverProvince }}{{ order.receiverCity }}{{ order.receiverDistrict }} {{ order.receiverDetail }}
          </div>
        </div>
      </div>

      <!-- 商品 -->
      <div class="card">
        <div v-for="it in order.items" :key="it.id" class="od__item">
          <div class="od__item-main" @click="router.push({ name: 'goods', params: { id: it.goodsId } })">
            <van-image :src="it.pic" width="80" height="80" radius="6" fit="cover" />
            <div class="od__item-body">
              <div class="od__item-name ellipsis-2">{{ it.goodsName }}</div>
              <div v-if="it.specText" class="od__item-spec ellipsis">{{ it.specText }}</div>
            </div>
            <div class="od__item-right">
              <Price :value="it.price" size="xs" color="inherit" />
              <div class="text-3">x{{ it.quantity }}</div>
              <div v-if="it.aftersaleStatus && ITEM_AFTERSALE_TEXT[it.aftersaleStatus]" class="od__item-as">
                {{ ITEM_AFTERSALE_TEXT[it.aftersaleStatus] }}
              </div>
            </div>
          </div>
          <div class="od__item-actions">
            <van-button
              v-if="canApplyAftersale(order, it)"
              size="mini"
              round
              class="od__mini"
              @click="itemAftersale(it)"
            >
              申请售后
            </van-button>
            <van-button
              v-else-if="it.aftersaleStatus && it.aftersaleStatus !== 'NONE'"
              size="mini"
              round
              class="od__mini"
              @click="itemAftersale(it)"
            >
              查看售后
            </van-button>
            <van-button
              v-if="order.status === 'COMPLETED' && !it.reviewed"
              size="mini"
              round
              class="od__mini od__mini--primary"
              @click="itemReview(it)"
            >
              评价
            </van-button>
          </div>
        </div>
      </div>

      <!-- 金额 -->
      <div class="card od__rows">
        <div class="od__row"><span>商品金额</span><span>¥{{ money(order.goodsAmount) }}</span></div>
        <div v-if="Number(order.discountAmount) > 0" class="od__row">
          <span>会员折扣</span><span class="text-primary">-¥{{ money(order.discountAmount) }}</span>
        </div>
        <div v-if="Number(order.couponAmount) > 0" class="od__row">
          <span>优惠券</span><span class="text-primary">-¥{{ money(order.couponAmount) }}</span>
        </div>
        <div v-if="order.adjustAmount && Number(order.adjustAmount) !== 0" class="od__row">
          <span>商家改价</span>
          <span>{{ Number(order.adjustAmount) > 0 ? '+' : '-' }}¥{{ money(Math.abs(Number(order.adjustAmount))) }}</span>
        </div>
        <div class="od__row">
          <span>运费</span><span>{{ Number(order.freightAmount) > 0 ? `+¥${money(order.freightAmount)}` : '包邮' }}</span>
        </div>
        <div class="od__row od__row--total">
          <span>{{ order.status === 'PENDING_PAY' ? '需付款' : '实付款' }}</span>
          <Price :value="order.payAmount" size="md" />
        </div>
      </div>

      <!-- 订单信息 -->
      <div class="card od__rows">
        <div class="od__row">
          <span>订单编号</span>
          <span>{{ order.orderNo }} <span class="od__copy" @click="copyText(order.orderNo)">复制</span></span>
        </div>
        <div class="od__row"><span>下单时间</span><span>{{ fmtTime(order.createdAt, 'YYYY-MM-DD HH:mm:ss') }}</span></div>
        <div v-if="order.payTime" class="od__row">
          <span>支付时间</span><span>{{ fmtTime(order.payTime, 'YYYY-MM-DD HH:mm:ss') }}</span>
        </div>
        <div v-if="order.payType" class="od__row">
          <span>支付方式</span><span>{{ PAY_TYPE_TEXT[order.payType] || order.payType }}</span>
        </div>
        <div v-if="order.receiveTime" class="od__row">
          <span>成交时间</span><span>{{ fmtTime(order.receiveTime, 'YYYY-MM-DD HH:mm:ss') }}</span>
        </div>
        <div v-if="order.cancelTime" class="od__row">
          <span>取消时间</span><span>{{ fmtTime(order.cancelTime, 'YYYY-MM-DD HH:mm:ss') }}</span>
        </div>
        <div v-if="order.remark" class="od__row"><span>订单备注</span><span class="od__remark">{{ order.remark }}</span></div>
      </div>

      <div class="od__space" />
      <div v-if="buttons.length" class="od__bar">
        <van-button
          v-for="(b, i) in buttons"
          :key="b"
          size="small"
          round
          :class="i === buttons.length - 1 ? 'od__btn od__btn--primary' : 'od__btn'"
          @click="onAction(b)"
        >
          {{ ACTION_TEXT[b] }}
        </van-button>
      </div>
      <CancelSheet v-model:show="cancelShow" @select="(r: string) => order && doCancel(order, r)" />
    </template>
  </div>
</template>

<style scoped lang="scss">
.od {
  :deep(.van-nav-bar) {
    background: $gradient;
    .van-nav-bar__title,
    .van-icon {
      color: #fff;
    }
  }
  &__loading {
    padding: 60px 0;
  }
  &__status {
    padding: 16px 20px 40px;
    background: $gradient;
    color: #fff;
  }
  &__status-main {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  &__status-text {
    font-size: 20px;
    font-weight: 700;
  }
  &__status-desc {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-top: 6px;
    font-size: 12px;
    opacity: 0.92;
  }
  &__status-icon {
    font-size: 44px;
    opacity: 0.85;
  }
  &__status + .card {
    margin-top: -28px;
    position: relative;
  }
  &__logistics,
  &__addr {
    display: flex;
    gap: 10px;
  }
  &__ico {
    flex-shrink: 0;
    font-size: 20px;
    color: $primary;
    margin-top: 2px;
  }
  &__logi-body,
  &__addr-body {
    flex: 1;
    min-width: 0;
  }
  &__logi-title {
    font-size: 14px;
    font-weight: 600;
  }
  &__logi-no,
  &__logi-time {
    margin-top: 4px;
    font-size: 12px;
    color: $text-2;
    word-break: break-all;
  }
  &__copy {
    display: inline-block;
    margin-left: 6px;
    padding: 0 6px;
    font-size: 11px;
    line-height: 16px;
    border: 1px solid $border-strong;
    border-radius: 999px;
    color: $text-2;
  }
  &__addr-user {
    font-size: 15px;
    font-weight: 600;
    span {
      margin-left: 8px;
      font-size: 13px;
      font-weight: normal;
      color: $text-2;
    }
  }
  &__addr-detail {
    margin-top: 4px;
    font-size: 13px;
    color: $text-2;
    line-height: 18px;
    word-break: break-all;
  }
  &__item {
    & + & {
      margin-top: 14px;
    }
  }
  &__item-main {
    display: flex;
    gap: 10px;
  }
  &__item-body {
    flex: 1;
    min-width: 0;
  }
  &__item-name {
    font-size: 13px;
    line-height: 18px;
  }
  &__item-spec {
    margin-top: 4px;
    font-size: 12px;
    color: $text-3;
  }
  &__item-right {
    flex-shrink: 0;
    text-align: right;
    font-size: 12px;
    line-height: 20px;
  }
  &__item-as {
    color: $primary;
  }
  &__item-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    &:not(:empty) {
      margin-top: 8px;
    }
  }
  &__mini {
    padding: 0 10px;
    color: $text-2;
    border-color: $border-strong;
    &--primary {
      color: $primary;
      border-color: $primary;
    }
  }
  &__rows {
    padding: 4px 12px;
  }
  &__row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    padding: 8px 0;
    font-size: 13px;
    > span:first-child {
      flex-shrink: 0;
      color: $text-2;
    }
    > span:last-child {
      text-align: right;
      word-break: break-all;
    }
    &--total {
      border-top: 1px solid $border;
      padding-top: 10px;
    }
  }
  &__remark {
    color: $text-2;
  }
  &__space {
    height: calc(64px + env(safe-area-inset-bottom));
  }
  &__bar {
    @include fixed-bottom;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    min-height: calc(54px + env(safe-area-inset-bottom));
    padding-left: $gap;
    padding-right: $gap;
    border-top: 1px solid $border;
  }
  &__btn {
    min-width: 80px;
    color: $text;
    border-color: $border-strong;
    &--primary {
      color: $primary;
      border-color: $primary;
    }
  }
}
</style>
