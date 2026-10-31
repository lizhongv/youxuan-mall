<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { GoodsItem } from '@/types'
import { getGoodsPage } from '@/api/public'
import { getPayStatus } from '@/api/order'
import { money } from '@/utils'
import { PAY_TYPE_TEXT } from '@/utils/dict'
import { clearPayingOrder, readPayingOrder } from '@/utils/pay'
import NavBar from '@/components/NavBar.vue'
import GoodsWaterfall from '@/components/GoodsWaterfall.vue'

/**
 * 支付结果：同步渠道（余额 / 模拟）直接成功；
 * 异步渠道（微信 / 支付宝）回到本页后每 2 秒轮询 pay-status，最多 60 秒，超时给出「我已支付 / 重新支付」。
 */
const route = useRoute()
const router = useRouter()
const remembered = readPayingOrder()
const orderId = (route.query.orderId as string) || remembered?.orderId || ''
const amount = ref((route.query.amount as string) || remembered?.amount || '')
const type = (route.query.type as string) || remembered?.type || ''
const needPoll = route.query.poll === '1' || (!route.query.orderId && !!remembered)

type State = 'checking' | 'success' | 'timeout' | 'closed'
const state = ref<State>(needPoll ? 'checking' : 'success')
const elapsed = ref(0)
const guess = ref<GoodsItem[]>([])

const INTERVAL = 2000
const MAX = 60000
let timer = 0
let stopped = false

async function checkOnce() {
  try {
    const s = await getPayStatus(orderId)
    if (s.paid) {
      state.value = 'success'
      clearPayingOrder()
      return true
    }
    if (s.status === 'CANCELLED') {
      state.value = 'closed'
      clearPayingOrder()
      return true
    }
  } catch {
    /* 网络抖动：继续轮询 */
  }
  return false
}

async function poll() {
  stopped = false
  state.value = 'checking'
  elapsed.value = 0
  const started = Date.now()
  const tick = async () => {
    if (stopped) return
    if (await checkOnce()) return
    elapsed.value = Date.now() - started
    if (elapsed.value >= MAX) {
      state.value = 'timeout'
      return
    }
    timer = window.setTimeout(tick, INTERVAL)
  }
  tick()
}

onMounted(() => {
  if (!orderId) {
    state.value = 'timeout'
  } else if (needPoll) {
    poll()
  } else {
    clearPayingOrder()
  }
  getGoodsPage({ page: 1, size: 10, sort: 'DEFAULT' })
    .then((r) => (guess.value = r?.records || []))
    .catch(() => {})
})
onBeforeUnmount(() => {
  stopped = true
  clearTimeout(timer)
})

const typeText = computed(() => PAY_TYPE_TEXT[type] || '')

function toOrder() {
  if (orderId) router.replace({ name: 'order-detail', params: { id: orderId } })
  else router.replace({ name: 'order-list' })
}
function repay() {
  if (orderId) router.replace({ name: 'cashier', params: { orderId } })
  else router.replace({ name: 'order-list' })
}
</script>

<template>
  <div class="page result">
    <NavBar title="支付结果" :on-back="toOrder" />
    <div class="result__head">
      <template v-if="state === 'checking'">
        <van-loading size="56" class="result__loading" />
        <div class="result__title">支付结果确认中</div>
        <div class="result__tip">请稍候，正在向支付渠道确认（{{ Math.ceil(elapsed / 1000) }}s）</div>
      </template>
      <template v-else-if="state === 'success'">
        <div class="result__icon"><van-icon name="success" /></div>
        <div class="result__title">支付成功</div>
        <div v-if="amount" class="result__amount">{{ typeText }} ¥{{ money(amount) }}</div>
        <div class="result__tip">商家将尽快为您发货，请耐心等待</div>
        <div class="result__btns">
          <van-button round plain size="small" class="result__btn" @click="toOrder">查看订单</van-button>
          <van-button round size="small" class="btn-gradient result__btn" @click="router.replace('/')">继续购物</van-button>
        </div>
      </template>
      <template v-else-if="state === 'closed'">
        <div class="result__icon result__icon--gray"><van-icon name="cross" /></div>
        <div class="result__title">订单已关闭</div>
        <div class="result__tip">订单已取消或超时关闭，如已扣款将自动原路退回</div>
        <div class="result__btns">
          <van-button round plain size="small" class="result__btn" @click="toOrder">查看订单</van-button>
          <van-button round size="small" class="btn-gradient result__btn" @click="router.replace('/')">继续购物</van-button>
        </div>
      </template>
      <template v-else>
        <div class="result__icon result__icon--warn"><van-icon name="question-o" /></div>
        <div class="result__title">暂未查询到支付结果</div>
        <div class="result__tip">如果您已完成支付，请点击「我已支付」刷新；未支付可重新发起支付</div>
        <div class="result__btns">
          <van-button round plain size="small" class="result__btn" @click="repay">重新支付</van-button>
          <van-button round size="small" class="btn-gradient result__btn" :disabled="!orderId" @click="poll">我已支付</van-button>
        </div>
      </template>
    </div>
    <template v-if="guess.length && state !== 'checking'">
      <div class="result__guess">— 为你推荐 —</div>
      <GoodsWaterfall :list="guess" />
    </template>
  </div>
</template>

<style scoped lang="scss">
.result {
  padding-bottom: 20px;
  &__head {
    padding: 32px $gap 24px;
    text-align: center;
    background: $card;
  }
  &__icon {
    width: 64px;
    height: 64px;
    margin: 0 auto;
    border-radius: 50%;
    background: $gradient;
    color: #fff;
    font-size: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    animation: pop 0.4s ease-out;
  }
  &__icon--gray {
    background: $disabled;
  }
  &__icon--warn {
    background: $warning;
  }
  &__loading {
    display: flex;
    justify-content: center;
    height: 64px;
    align-items: center;
  }
  &__title {
    margin-top: 14px;
    font-size: 20px;
    font-weight: 700;
  }
  &__amount {
    margin-top: 6px;
    font-size: 14px;
    color: $text-2;
  }
  &__tip {
    margin-top: 6px;
    font-size: 12px;
    color: $text-3;
  }
  &__btns {
    display: flex;
    justify-content: center;
    gap: 16px;
    margin-top: 20px;
  }
  &__btn {
    min-width: 110px;
  }
  &__guess {
    padding: 20px 0 12px;
    text-align: center;
    font-size: 14px;
    color: $text-2;
  }
}
@keyframes pop {
  0% {
    transform: scale(0.3);
    opacity: 0;
  }
  70% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
