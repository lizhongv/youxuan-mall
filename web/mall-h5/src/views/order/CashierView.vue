<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showDialog } from 'vant'
import type { Order, PayType, Profile } from '@/types'
import { getOrder, payOrder } from '@/api/order'
import { getProfile } from '@/api/member'
import { money, parseTime, uuid } from '@/utils'
import NavBar from '@/components/NavBar.vue'
import Price from '@/components/Price.vue'
import CountDown from '@/components/CountDown.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const orderId = route.params.orderId as string

const order = ref<Order | null>(null)
const profile = ref<Profile | null>(null)
const loading = ref(true)
const paying = ref(false)
const expired = ref(false)
const payType = ref<PayType>('MOCK')
/** 支付幂等键：本页生命周期内复用，避免重复点击/重试导致重复扣款 */
const idemKey = uuid()

async function load() {
  loading.value = true
  try {
    const [o, p] = await Promise.all([getOrder(orderId), getProfile(true).catch(() => null)])
    order.value = o
    profile.value = p
    if (o.status !== 'PENDING_PAY') {
      router.replace({ name: 'order-detail', params: { id: orderId } })
      return
    }
    expired.value = !!o.payExpireAt && parseTime(o.payExpireAt) <= Date.now()
    payType.value = balanceEnough.value ? 'BALANCE' : 'MOCK'
  } finally {
    loading.value = false
  }
}
onMounted(load)

const balance = computed(() => Number(profile.value?.balance || 0))
const balanceEnough = computed(() => !!order.value && balance.value >= Number(order.value.payAmount))

function onExpire() {
  expired.value = true
  showDialog({ title: '订单已关闭', message: '支付超时，订单已自动关闭' }).then(() =>
    router.replace({ name: 'order-detail', params: { id: orderId } }),
  )
}

async function pay() {
  if (!order.value || expired.value) return
  paying.value = true
  try {
    await payOrder(orderId, payType.value, idemKey)
    router.replace({ name: 'pay-result', query: { orderId, amount: String(order.value.payAmount), type: payType.value } })
  } catch {
    /* 已提示 */
  } finally {
    paying.value = false
  }
}

function onBack() {
  showConfirmDialog({
    title: '确认离开收银台？',
    message: '订单未支付，超时后将自动取消，请尽快完成支付',
    confirmButtonText: '继续支付',
    cancelButtonText: '确认离开',
  }).catch(() => router.replace({ name: 'order-detail', params: { id: orderId } }))
}
</script>

<template>
  <div class="page cashier">
    <NavBar title="收银台" :on-back="onBack" />
    <van-loading v-if="loading" class="cashier__loading" vertical>加载中...</van-loading>
    <EmptyState v-else-if="!order" description="订单不存在" />
    <template v-else>
      <div class="cashier__head">
        <div class="cashier__label">实付金额</div>
        <Price :value="order.payAmount" size="xl" color="inherit" class="cashier__amount" />
        <div class="cashier__timer">
          <template v-if="expired">订单已超时关闭</template>
          <template v-else-if="order.payExpireAt">
            支付剩余时间 <CountDown :end="order.payExpireAt" theme="dark" @finish="onExpire" />
          </template>
        </div>
        <div class="cashier__no">订单号：{{ order.orderNo }}</div>
      </div>

      <van-radio-group v-model="payType" class="card cashier__methods">
        <div class="cashier__method" :class="{ 'is-disabled': !balanceEnough }" @click="balanceEnough && (payType = 'BALANCE')">
          <span class="cashier__icon cashier__icon--balance"><van-icon name="balance-o" /></span>
          <div class="cashier__method-body">
            <div>余额支付</div>
            <div class="cashier__method-sub">
              可用余额 ¥{{ money(balance) }}<template v-if="!balanceEnough">，余额不足</template>
            </div>
          </div>
          <van-radio name="BALANCE" :disabled="!balanceEnough" />
        </div>
        <div class="cashier__method" @click="payType = 'MOCK'">
          <span class="cashier__icon cashier__icon--mock"><van-icon name="gold-coin-o" /></span>
          <div class="cashier__method-body">
            <div>模拟支付</div>
            <div class="cashier__method-sub">演示环境，直接支付成功</div>
          </div>
          <van-radio name="MOCK" />
        </div>
      </van-radio-group>

      <div class="cashier__bar">
        <van-button
          block
          round
          class="btn-gradient"
          :disabled="expired"
          :loading="paying"
          loading-text="支付中..."
          @click="pay"
        >
          {{ expired ? '订单已关闭' : `确认支付 ¥${money(order.payAmount)}` }}
        </van-button>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.cashier {
  &__loading {
    padding: 60px 0;
  }
  &__head {
    padding: 28px $gap 24px;
    text-align: center;
    background: $card;
  }
  &__label {
    font-size: 13px;
    color: $text-2;
  }
  &__amount {
    margin-top: 10px;
    color: $text;
  }
  &__timer {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    margin-top: 12px;
    font-size: 13px;
    color: $text-2;
  }
  &__no {
    margin-top: 8px;
    font-size: 12px;
    color: $text-3;
  }
  &__methods {
    padding: 0 12px;
  }
  &__method {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 0;
    & + & {
      border-top: 1px solid $border;
    }
    &.is-disabled {
      opacity: 0.5;
    }
  }
  &__icon {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    color: #fff;
    font-size: 18px;
    &--balance {
      background: $orange-gradient;
    }
    &--mock {
      background: $gradient;
    }
  }
  &__method-body {
    flex: 1;
    min-width: 0;
    font-size: 15px;
  }
  &__method-sub {
    margin-top: 2px;
    font-size: 12px;
    color: $text-3;
  }
  &__bar {
    @include fixed-bottom;
    padding-left: $gap;
    padding-right: $gap;
    padding-top: 8px;
    padding-bottom: calc(8px + env(safe-area-inset-bottom));
  }
}
</style>
