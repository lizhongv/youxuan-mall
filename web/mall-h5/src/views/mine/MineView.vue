<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { GoodsItem } from '@/types'
import { getGoodsPage } from '@/api/public'
import { useUserStore } from '@/stores/user'
import { money } from '@/utils'
import GoodsWaterfall from '@/components/GoodsWaterfall.vue'

const router = useRouter()
const user = useUserStore()
const guess = ref<GoodsItem[]>([])
const refreshing = ref(false)

onMounted(() => {
  user.fetchProfile()
  getGoodsPage({ page: 1, size: 10, sort: 'DEFAULT' })
    .then((r) => (guess.value = r?.records || []))
    .catch(() => {})
})

async function onRefresh() {
  await user.fetchProfile()
  refreshing.value = false
}

const p = computed(() => user.profile)
const oc = computed(() => p.value?.orderCount)

function go(name: string, query?: Record<string, string>) {
  router.push({ name, query })
}

function badge(v?: number | string) {
  const n = Number(v || 0)
  return n > 0 ? (n > 99 ? '99+' : n) : undefined
}

const orderEntries = computed(() => [
  { icon: 'pending-payment', text: '待付款', status: 'PENDING_PAY', count: oc.value?.PENDING_PAY },
  { icon: 'send-gift-o', text: '待发货', status: 'PENDING_DELIVERY', count: oc.value?.PENDING_DELIVERY },
  { icon: 'logistics', text: '待收货', status: 'DELIVERED', count: oc.value?.DELIVERED },
  { icon: 'comment-o', text: '待评价', status: 'TO_REVIEW', count: oc.value?.TO_REVIEW },
  { icon: 'after-sale', text: '退款/售后', status: 'AFTERSALE', count: oc.value?.AFTERSALE },
])

const services = [
  { icon: 'location-o', text: '收货地址', name: 'address' },
  { icon: 'star-o', text: '我的收藏', name: 'favorite' },
  { icon: 'clock-o', text: '浏览足迹', name: 'history' },
  { icon: 'chat-o', text: '我的评价', name: 'review-mine' },
  { icon: 'service-o', text: '售后服务', name: 'aftersale-list' },
  { icon: 'coupon-o', text: '领券中心', name: 'coupon-center' },
  { icon: 'bill-o', text: '账户明细', name: 'account-log' },
  { icon: 'setting-o', text: '设置', name: 'settings' },
]

function openOrder(status: string) {
  if (status === 'AFTERSALE') go('aftersale-list')
  else go('order-list', { status })
}
</script>

<template>
  <div class="page mine">
    <van-pull-refresh v-model="refreshing" :disabled="!user.isLogin" @refresh="onRefresh">
      <!-- 用户卡 -->
      <div class="mine__head">
        <div class="mine__top-actions">
          <van-icon name="setting-o" size="20" @click="go('settings')" />
        </div>
        <div v-if="user.isLogin" class="mine__user" @click="go('profile')">
          <van-image :src="p?.avatar" round width="60" height="60" fit="cover" class="mine__avatar">
            <template #error><van-icon name="user-o" size="28" /></template>
            <template #loading><van-icon name="user-o" size="28" /></template>
          </van-image>
          <div class="mine__info">
            <div class="mine__name ellipsis">{{ p?.nickname || '会员' }}</div>
            <div class="mine__meta">
              <span v-if="p?.levelName" class="mine__level"><van-icon name="diamond-o" /> {{ p.levelName }}</span>
              <span class="mine__mobile">{{ p?.mobile }}</span>
            </div>
          </div>
          <van-icon name="arrow" class="mine__arrow" />
        </div>
        <div v-else class="mine__user" @click="go('login')">
          <div class="mine__avatar mine__avatar--empty"><van-icon name="user-o" size="28" /></div>
          <div class="mine__info">
            <div class="mine__name">登录/注册</div>
            <div class="mine__meta">登录后享受更多会员权益</div>
          </div>
          <van-icon name="arrow" class="mine__arrow" />
        </div>

        <!-- 资产 -->
        <div class="mine__assets">
          <div class="mine__asset" @click="go('account-log', { type: 'BALANCE' })">
            <b>{{ user.isLogin ? money(p?.balance) : '--' }}</b><span>余额</span>
          </div>
          <div class="mine__asset" @click="go('account-log', { type: 'POINTS' })">
            <b>{{ user.isLogin ? (p?.points ?? 0) : '--' }}</b><span>积分</span>
          </div>
          <div class="mine__asset" @click="go('my-coupons')">
            <b>{{ user.isLogin ? (p?.couponCount ?? 0) : '--' }}</b><span>优惠券</span>
          </div>
          <div class="mine__asset" @click="go('favorite')">
            <b>{{ user.isLogin ? (p?.favoriteCount ?? 0) : '--' }}</b><span>收藏</span>
          </div>
        </div>
      </div>

      <!-- 我的订单 -->
      <div class="mine__card mine__orders">
        <div class="mine__card-head" @click="go('order-list')">
          <span>我的订单</span>
          <span class="mine__more">全部订单 <van-icon name="arrow" /></span>
        </div>
        <div class="mine__grid mine__grid--5">
          <div v-for="e in orderEntries" :key="e.status" class="mine__grid-item" @click="openOrder(e.status)">
            <van-badge :content="badge(e.count)" :offset="[-2, 2]">
              <van-icon :name="e.icon" size="26" class="mine__grid-icon" />
            </van-badge>
            <span>{{ e.text }}</span>
          </div>
        </div>
      </div>

      <!-- 服务 -->
      <div class="mine__card">
        <div class="mine__card-head"><span>我的服务</span></div>
        <div class="mine__grid mine__grid--4">
          <div v-for="s in services" :key="s.name" class="mine__grid-item" @click="go(s.name)">
            <van-icon :name="s.icon" size="24" class="mine__grid-icon mine__grid-icon--service" />
            <span>{{ s.text }}</span>
          </div>
        </div>
      </div>

      <template v-if="guess.length">
        <div class="mine__guess">— 为你推荐 —</div>
        <GoodsWaterfall :list="guess" />
      </template>
    </van-pull-refresh>
  </div>
</template>

<style scoped lang="scss">
.mine {
  padding-bottom: 12px;
  &__head {
    position: relative;
    padding: calc(12px + env(safe-area-inset-top)) $gap 0;
    background: $gradient;
    color: #fff;
    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: -30px;
      height: 30px;
      background: $gradient;
      border-radius: 0 0 50% 50% / 0 0 30px 30px;
      z-index: 0;
    }
  }
  &__top-actions {
    display: flex;
    justify-content: flex-end;
  }
  &__user {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 0 16px;
  }
  &__avatar {
    flex-shrink: 0;
    border: 2px solid rgba(255, 255, 255, 0.7);
    background: rgba(255, 255, 255, 0.25);
    color: #fff;
    &--empty {
      width: 60px;
      height: 60px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }
  &__info {
    flex: 1;
    min-width: 0;
  }
  &__name {
    font-size: 19px;
    font-weight: 700;
  }
  &__meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 6px;
    font-size: 12px;
    opacity: 0.95;
  }
  &__level {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    padding: 1px 8px;
    border-radius: 999px;
    background: linear-gradient(90deg, $gold, $orange);
    color: $text;
    font-weight: 600;
  }
  &__arrow {
    opacity: 0.8;
  }
  &__assets {
    position: relative;
    z-index: 1;
    display: flex;
    padding: 4px 0 6px;
  }
  &__asset {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    b {
      font-size: 17px;
      font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
    }
    span {
      font-size: 12px;
      opacity: 0.9;
    }
  }
  &__card {
    position: relative;
    z-index: 1;
    @include card;
    margin: 10px $gap 0;
    padding: 12px 8px;
  }
  &__orders {
    margin-top: 0;
  }
  &__card-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 4px 10px;
    font-size: 15px;
    font-weight: 600;
  }
  &__more {
    font-size: 12px;
    font-weight: normal;
    color: $text-3;
  }
  &__grid {
    display: grid;
    row-gap: 14px;
    &--5 {
      grid-template-columns: repeat(5, 1fr);
    }
    &--4 {
      grid-template-columns: repeat(4, 1fr);
    }
  }
  &__grid-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    min-width: 0;
    font-size: 12px;
    color: $text;
    white-space: nowrap;
  }
  &__grid-icon {
    color: $text;
    &--service {
      color: $primary;
    }
  }
  &__guess {
    padding: 20px 0 12px;
    text-align: center;
    font-size: 14px;
    color: $text-2;
  }
}
</style>
