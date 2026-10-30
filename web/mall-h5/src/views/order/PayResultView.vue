<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { GoodsItem } from '@/types'
import { getGoodsPage } from '@/api/public'
import { money } from '@/utils'
import { PAY_TYPE_TEXT } from '@/utils/dict'
import NavBar from '@/components/NavBar.vue'
import GoodsWaterfall from '@/components/GoodsWaterfall.vue'

const route = useRoute()
const router = useRouter()
const orderId = route.query.orderId as string
const amount = route.query.amount as string
const type = route.query.type as string
const guess = ref<GoodsItem[]>([])

onMounted(() => {
  getGoodsPage({ page: 1, size: 10, sort: 'DEFAULT' })
    .then((r) => (guess.value = r?.records || []))
    .catch(() => {})
})
</script>

<template>
  <div class="page result">
    <NavBar title="支付结果" :on-back="() => router.replace({ name: 'order-detail', params: { id: orderId } })" />
    <div class="result__head">
      <div class="result__icon"><van-icon name="success" /></div>
      <div class="result__title">支付成功</div>
      <div v-if="amount" class="result__amount">
        {{ PAY_TYPE_TEXT[type] || '' }} ¥{{ money(amount) }}
      </div>
      <div class="result__tip">商家将尽快为您发货，请耐心等待</div>
      <div class="result__btns">
        <van-button round plain size="small" class="result__btn" @click="router.replace({ name: 'order-detail', params: { id: orderId } })">
          查看订单
        </van-button>
        <van-button round size="small" class="btn-gradient result__btn" @click="router.replace('/')">继续购物</van-button>
      </div>
    </div>
    <template v-if="guess.length">
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
