<script setup lang="ts">
import { computed, onActivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { Banner, Coupon, GoodsItem, HomeData, Seckill } from '@/types'
import { getAvailableCoupons, getGoodsPage, getHome } from '@/api/public'
import { useAppStore } from '@/stores/app'
import { usePageList } from '@/composables/usePageList'
import { useReceiveCoupon } from '@/composables/useCoupon'
import Price from '@/components/Price.vue'
import CountDown from '@/components/CountDown.vue'
import GoodsWaterfall from '@/components/GoodsWaterfall.vue'
import EmptyState from '@/components/EmptyState.vue'
import { parseTime } from '@/utils'

defineOptions({ name: 'HomeView' })

const router = useRouter()
const app = useAppStore()
const home = ref<HomeData | null>(null)
const coupons = ref<Coupon[]>([])
const loadingHome = ref(true)
const homeError = ref(false)
const { receive, canReceive, statusText } = useReceiveCoupon()

const recommend = usePageList<GoodsItem>((page, size) => getGoodsPage({ page, size, sort: 'DEFAULT' }), 10)

async function loadHome() {
  homeError.value = false
  try {
    const [h, c] = await Promise.all([getHome(), getAvailableCoupons().catch(() => [] as Coupon[])])
    home.value = h
    coupons.value = (c || []).slice(0, 6)
  } catch {
    homeError.value = !home.value
  } finally {
    loadingHome.value = false
  }
}

onMounted(loadHome)

async function onRefresh() {
  await Promise.all([loadHome(), recommend.refresh()])
  recommend.refreshing.value = false
}

/* ---------------- 吸顶搜索：滚动后加深背景 ---------------- */
const scrolled = ref(false)
function onScroll() {
  scrolled.value = window.scrollY > 40
}
onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onActivated(onScroll)

/* ---------------- 轮播跳转 ---------------- */
function openBanner(b: Banner) {
  if (!b.linkValue || b.linkType === 'NONE') return
  if (b.linkType === 'GOODS') router.push({ name: 'goods', params: { id: b.linkValue } })
  else if (b.linkType === 'CATEGORY') router.push({ name: 'goods-list', query: { categoryId: b.linkValue, title: b.title } })
  else if (b.linkType === 'URL') {
    if (/^https?:\/\//.test(b.linkValue)) window.location.href = b.linkValue
    else router.push(b.linkValue)
  }
}

/* ---------------- 秒杀 ---------------- */
const seckills = computed(() => home.value?.seckills || [])
/** 头部倒计时取最近的一个时间点：进行中取结束时间，未开始取开始时间 */
const seckillHead = computed(() => {
  const ongoing = seckills.value.filter((s) => s.state === 'ONGOING')
  if (ongoing.length) {
    const s = ongoing.reduce((a, b) => (parseTime(a.endAt) < parseTime(b.endAt) ? a : b))
    return { label: '距结束', end: s.endAt }
  }
  const next = seckills.value.filter((s) => s.state === 'NOT_STARTED')
  if (next.length) {
    const s = next.reduce((a, b) => (parseTime(a.startAt) < parseTime(b.startAt) ? a : b))
    return { label: '距开始', end: s.startAt }
  }
  return null
})
function percent(s: Seckill) {
  if (!s.stock) return 100
  return Math.min(100, Math.round((s.sold / s.stock) * 100))
}

const categories = computed(() => (home.value?.categories || []).slice(0, 10))
const noticeList = computed(() => home.value?.notices || [])
</script>

<template>
  <div class="home page">
    <!-- 吸顶搜索 -->
    <header class="home__header" :class="{ 'is-scrolled': scrolled }">
      <div class="home__brand">
        <span class="home__logo">{{ app.config.mallName || '商城' }}</span>
      </div>
      <div class="home__search" @click="router.push({ name: 'search' })">
        <van-icon name="search" size="16" />
        <span>搜索商品</span>
        <span class="home__search-btn">搜索</span>
      </div>
    </header>

    <van-pull-refresh v-model="recommend.refreshing.value" class="home__refresh" @refresh="onRefresh">
      <div class="home__bg" />

      <!-- 骨架 -->
      <div v-if="loadingHome" class="home__skeleton">
        <van-skeleton class="home__sk-banner" :row="0" />
        <van-skeleton :row="3" />
      </div>
      <EmptyState v-else-if="homeError" image="network" description="网络开小差了" button-text="重新加载" @click="loadHome" />

      <template v-else-if="home">
        <!-- 轮播 -->
        <div v-if="home.banners.length" class="home__banner">
          <van-swipe :autoplay="4000" indicator-color="white" lazy-render>
            <van-swipe-item v-for="b in home.banners" :key="b.id" @click="openBanner(b)">
              <van-image :src="b.image" fit="cover" width="100%" height="100%" />
            </van-swipe-item>
          </van-swipe>
        </div>

        <!-- 公告 -->
        <div v-if="noticeList.length" class="home__notice">
          <span class="home__notice-tag">公告</span>
          <van-swipe vertical class="home__notice-swipe" :autoplay="3000" :show-indicators="false" :touchable="false">
            <van-swipe-item
              v-for="n in noticeList"
              :key="n.id"
              class="home__notice-item ellipsis"
              @click="router.push({ name: 'notice', params: { id: n.id } })"
            >
              {{ n.title }}
            </van-swipe-item>
          </van-swipe>
          <van-icon name="arrow" class="text-3" />
        </div>

        <!-- 类目宫格 -->
        <div v-if="categories.length" class="home__cats">
          <div
            v-for="c in categories"
            :key="c.id"
            class="home__cat"
            @click="router.push({ name: 'goods-list', query: { categoryId: c.id, title: c.name } })"
          >
            <div class="home__cat-icon">
              <van-image v-if="c.icon" :src="c.icon" width="100%" height="100%" fit="cover" round />
              <span v-else>{{ c.name.slice(0, 1) }}</span>
            </div>
            <span class="home__cat-name ellipsis">{{ c.name }}</span>
          </div>
        </div>

        <!-- 优惠券条 -->
        <div v-if="coupons.length" class="home__coupons">
          <div class="home__coupons-head">
            <span class="home__coupons-title">领券中心</span>
            <span class="home__coupons-more" @click="router.push({ name: 'coupon-center' })">
              更多好券 <van-icon name="arrow" />
            </span>
          </div>
          <div class="hscroll">
            <div
              v-for="c in coupons"
              :key="c.id"
              class="mini-coupon"
              :class="{ 'is-disabled': !canReceive(c) }"
              @click="receive(c)"
            >
              <div class="mini-coupon__value">
                <template v-if="c.type === 'DISCOUNT'">
                  <b>{{ +(Number(c.discount) * 10).toFixed(1) }}</b><small>折</small>
                </template>
                <template v-else>
                  <small>¥</small><b>{{ Number(c.amount) }}</b>
                </template>
              </div>
              <div class="mini-coupon__cond ellipsis">
                {{ Number(c.threshold) > 0 ? `满${Number(c.threshold)}可用` : '无门槛' }}
              </div>
              <div class="mini-coupon__btn">{{ statusText(c) }}</div>
            </div>
          </div>
        </div>

        <!-- 秒杀专区 -->
        <div v-if="seckills.length" class="home__block home__seckill">
          <div class="section-title">
            <div class="home__seckill-title">
              <span class="home__seckill-name">限时秒杀</span>
              <template v-if="seckillHead">
                <span class="home__seckill-label">{{ seckillHead.label }}</span>
                <CountDown :end="seckillHead.end" @finish="loadHome" />
              </template>
            </div>
            <span class="more" @click="router.push({ name: 'seckill' })">更多 <van-icon name="arrow" /></span>
          </div>
          <div class="hscroll">
            <div
              v-for="s in seckills"
              :key="s.id"
              class="sk-item"
              @click="router.push({ name: 'goods', params: { id: s.goodsId } })"
            >
              <div class="sk-item__img">
                <van-image :src="s.goodsPic" width="100%" height="100%" fit="cover" lazy-load radius="6" />
                <span v-if="s.state === 'NOT_STARTED'" class="sk-item__badge">即将开抢</span>
              </div>
              <Price :value="s.seckillPrice" size="sm" />
              <div class="sk-item__progress">
                <div class="sk-item__bar" :style="{ width: percent(s) + '%' }" />
                <span class="sk-item__percent">已抢{{ percent(s) }}%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 新品 / 热卖 -->
        <div v-for="sec in [
          { key: 'new', title: '新品首发', sub: '抢先体验', list: home.newGoods, sort: 'NEW' },
          { key: 'hot', title: '热卖榜单', sub: '大家都在买', list: home.hotGoods, sort: 'SALES' },
        ]" :key="sec.key">
          <div v-if="sec.list && sec.list.length" class="home__block">
            <div class="section-title">
              <span>{{ sec.title }}<small class="home__sub">{{ sec.sub }}</small></span>
              <span class="more" @click="router.push({ name: 'goods-list', query: { sort: sec.sort, title: sec.title } })">
                更多 <van-icon name="arrow" />
              </span>
            </div>
            <div class="hscroll">
              <div
                v-for="(g, gi) in sec.list"
                :key="g.id"
                class="h-goods"
                @click="router.push({ name: 'goods', params: { id: g.id } })"
              >
                <div class="h-goods__img">
                  <van-image :src="g.pic" width="100%" height="100%" fit="cover" lazy-load radius="6" />
                  <span v-if="sec.key === 'hot' && gi < 3" class="h-goods__rank">TOP{{ gi + 1 }}</span>
                </div>
                <div class="h-goods__name ellipsis">{{ g.name }}</div>
                <Price :value="g.price" size="sm" />
              </div>
            </div>
          </div>
        </div>

        <!-- 为你推荐 -->
        <div class="home__rec-title">
          <span class="home__rec-line" />
          <van-icon name="like" class="text-primary" />
          为你推荐
          <span class="home__rec-line" />
        </div>
        <van-list
          v-model:loading="recommend.loading.value"
          v-model:error="recommend.error.value"
          :finished="recommend.finished.value"
          finished-text="— 我也是有底线的 —"
          error-text="加载失败，点击重试"
          @load="recommend.load"
        >
          <GoodsWaterfall :list="recommend.list.value" />
        </van-list>
      </template>
    </van-pull-refresh>
  </div>
</template>

<style scoped lang="scss">
.home {
  &__header {
    position: sticky;
    top: 0;
    z-index: 50;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px $gap;
    padding-top: calc(8px + env(safe-area-inset-top));
    background: $gradient;
    transition: box-shadow 0.2s;
    &.is-scrolled {
      box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
    }
  }
  &__brand {
    flex-shrink: 0;
    max-width: 30%;
  }
  &__logo {
    display: block;
    color: #fff;
    font-size: 17px;
    font-weight: 700;
    letter-spacing: 1px;
    @include ellipsis(1);
  }
  &__search {
    flex: 1;
    min-width: 0;
    height: 34px;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0 3px 0 12px;
    border-radius: 999px;
    background: $card;
    color: $text-3;
    font-size: 13px;
    span:nth-child(2) {
      flex: 1;
    }
  }
  &__search-btn {
    height: 28px;
    line-height: 28px;
    padding: 0 14px;
    border-radius: 999px;
    background: $gradient;
    color: #fff;
    font-size: 13px;
  }
  &__refresh {
    position: relative;
    min-height: calc(100vh - 100px);
  }
  &__bg {
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
    height: 110px;
    background: $gradient;
    border-radius: 0 0 50% 50% / 0 0 24px 24px;
  }
  &__skeleton {
    position: relative;
    padding: 8px 0;
  }
  &__sk-banner {
    height: 140px;
    margin: 0 $gap 12px;
    border-radius: $radius;
    background: $card;
  }
  &__banner {
    position: relative;
    margin: 4px $gap 0;
    height: 0;
    padding-bottom: 42%;
    border-radius: $radius;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    transform: translateZ(0);
    .van-swipe {
      position: absolute;
      inset: 0;
    }
    :deep(.van-swipe__indicator) {
      width: 6px;
      height: 3px;
      border-radius: 2px;
      transition: width 0.3s;
    }
    :deep(.van-swipe__indicator--active) {
      width: 14px;
    }
  }
  &__notice {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 10px $gap 0;
    padding: 0 10px;
    height: 34px;
    background: $card;
    border-radius: $radius;
    font-size: 13px;
  }
  &__notice-tag {
    flex-shrink: 0;
    padding: 1px 6px;
    font-size: 11px;
    font-weight: 600;
    color: $primary;
    background: $primary-light;
    border-radius: 4px;
  }
  &__notice-swipe {
    flex: 1;
    min-width: 0;
    height: 34px;
  }
  &__notice-item {
    line-height: 34px;
    color: $text;
  }
  &__cats {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    row-gap: 12px;
    margin: 10px $gap 0;
    padding: 14px 4px;
    background: $card;
    border-radius: $radius;
  }
  &__cat {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    min-width: 0;
  }
  &__cat-icon {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: $primary-light;
    color: $primary;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    font-weight: 600;
    overflow: hidden;
  }
  &__cat-name {
    max-width: 100%;
    font-size: 12px;
    color: $text;
  }
  &__coupons {
    margin: 10px $gap 0;
    padding: 10px;
    border-radius: $radius;
    background: linear-gradient(180deg, $primary-light, $card);
  }
  &__coupons-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
  }
  &__coupons-title {
    font-size: 15px;
    font-weight: 700;
    color: $primary;
  }
  &__coupons-more {
    font-size: 12px;
    color: $text-3;
  }
  &__block {
    margin: 10px $gap 0;
    padding: 12px 10px;
    background: $card;
    border-radius: $radius;
  }
  &__sub {
    margin-left: 6px;
    font-size: 12px;
    font-weight: normal;
    color: $text-3;
  }
  &__seckill-title {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
  }
  &__seckill-name {
    font-style: italic;
    font-weight: 800;
    background: $gradient;
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
  }
  &__seckill-label {
    font-size: 12px;
    font-weight: normal;
    color: $text-2;
  }
  &__rec-title {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 18px 0 12px;
    font-size: 16px;
    font-weight: 700;
  }
  &__rec-line {
    width: 28px;
    height: 1px;
    background: $border-strong;
  }
}

/* 横向滚动容器 */
.hscroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  &::-webkit-scrollbar {
    display: none;
  }
}

.mini-coupon {
  flex: 0 0 86px;
  padding: 8px 4px 6px;
  border-radius: 8px;
  background: $card;
  border: 1px solid $primary-light;
  text-align: center;
  color: $primary;
  &__value {
    line-height: 1;
    b {
      font-size: 22px;
    }
    small {
      font-size: 11px;
    }
  }
  &__cond {
    margin-top: 4px;
    font-size: 11px;
    color: $text-2;
  }
  &__btn {
    margin-top: 6px;
    padding: 2px 0;
    font-size: 11px;
    color: #fff;
    background: $gradient;
    border-radius: 999px;
  }
  &.is-disabled {
    color: $text-3;
    .mini-coupon__btn {
      background: $disabled;
    }
  }
}

.sk-item {
  flex: 0 0 96px;
  width: 96px;
  &__img {
    position: relative;
    width: 96px;
    height: 96px;
    margin-bottom: 6px;
  }
  &__badge {
    position: absolute;
    left: 0;
    bottom: 0;
    right: 0;
    text-align: center;
    font-size: 11px;
    line-height: 18px;
    color: #fff;
    background: rgba(0, 0, 0, 0.45);
    border-radius: 0 0 6px 6px;
  }
  &__progress {
    position: relative;
    height: 14px;
    margin-top: 6px;
    border-radius: 7px;
    background: $primary-light;
    overflow: hidden;
  }
  &__bar {
    height: 100%;
    background: $gradient;
    border-radius: 7px;
    transition: width 0.6s;
  }
  &__percent {
    position: absolute;
    inset: 0;
    font-size: 10px;
    line-height: 14px;
    text-align: center;
    color: $text;
  }
}

.h-goods {
  flex: 0 0 100px;
  width: 100px;
  &__img {
    position: relative;
    width: 100px;
    height: 100px;
  }
  &__rank {
    position: absolute;
    left: 0;
    top: 0;
    padding: 1px 5px;
    font-size: 10px;
    font-weight: 700;
    color: #fff;
    background: $gradient;
    border-radius: 6px 0 6px 0;
  }
  &__name {
    margin: 6px 0 4px;
    font-size: 12px;
  }
}
</style>
