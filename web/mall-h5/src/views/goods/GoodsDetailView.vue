<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showImagePreview, showSuccessToast, showToast } from 'vant'
import type { Coupon, GoodsDetail, SkuAction, Sku } from '@/types'
import { getAvailableCoupons, getGoodsDetail } from '@/api/public'
import { addFavorite, removeFavorite } from '@/api/member'
import { addCart } from '@/api/cart'
import { useAppStore } from '@/stores/app'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { useReceiveCoupon } from '@/composables/useCoupon'
import { fmtSales, fmtTime, money, ratePercent, sanitizeHtml } from '@/utils'
import Price from '@/components/Price.vue'
import CountDown from '@/components/CountDown.vue'
import SkuPopup from '@/components/SkuPopup.vue'
import CouponCard from '@/components/CouponCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const app = useAppStore()
const cart = useCartStore()
const user = useUserStore()

const goods = ref<GoodsDetail | null>(null)
const loading = ref(true)
const notFound = ref(false)
const swipeIndex = ref(0)

const skuShow = ref(false)
const skuAction = ref<'all' | SkuAction>('all')
const skuId = ref('')
const quantity = ref(1)

const couponShow = ref(false)
const coupons = ref<Coupon[]>([])
const { receive, canReceive, statusText } = useReceiveCoupon()

async function load() {
  loading.value = true
  try {
    goods.value = await getGoodsDetail(route.params.id as string)
    app.loadConfig()
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
  getAvailableCoupons()
    .then((c) => (coupons.value = c || []))
    .catch(() => {})
}
onMounted(load)

/* ---------------- 顶栏随滚动渐显 ---------------- */
const headerOpacity = ref(0)
function onScroll() {
  headerOpacity.value = Math.min(1, window.scrollY / 260)
}
onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))

const album = computed(() => {
  const g = goods.value
  if (!g) return []
  const list = g.album?.length ? g.album : [g.pic]
  return list.filter(Boolean)
})

const seckill = computed(() => goods.value?.seckill || null)
const seckillOngoing = computed(() => seckill.value?.state === 'ONGOING')
const seckillPercent = computed(() => {
  const s = seckill.value
  return s && s.stock ? Math.min(100, Math.round((s.sold / s.stock) * 100)) : 0
})

const detailHtml = computed(() => sanitizeHtml(goods.value?.detail))

const selectedSku = computed<Sku | undefined>(() => goods.value?.skus.find((s) => s.id === skuId.value))
const selectedText = computed(() => {
  const g = goods.value
  if (!g) return ''
  if (!g.specItems?.length) return `${quantity.value}${g.unit || '件'}`
  if (!selectedSku.value) return `请选择 ${g.specItems.map((s) => s.name).join(' ')}`
  return `${selectedSku.value.specValues.join('，')}，${quantity.value}${g.unit || '件'}`
})

const soldOut = computed(() => !!goods.value && goods.value.skus.every((s) => s.stock <= 0))
const offSale = computed(() => goods.value?.status === 'OFF_SALE')

const freightText = computed(() => {
  const c = app.config
  const amount = Number(c.freightAmount || 0)
  const free = Number(c.freightFreeThreshold || 0)
  if (amount <= 0) return '全场包邮'
  return free > 0 ? `运费 ¥${money(amount)}，满 ¥${money(free)} 包邮` : `运费 ¥${money(amount)}`
})

const couponPreview = computed(() =>
  coupons.value.slice(0, 2).map((c) =>
    c.type === 'DISCOUNT'
      ? `${+(Number(c.discount) * 10).toFixed(1)}折`
      : Number(c.threshold) > 0
        ? `满${Number(c.threshold)}减${Number(c.amount)}`
        : `立减${Number(c.amount)}`,
  ),
)

function preview(i: number) {
  showImagePreview({ images: album.value, startPosition: i, closeable: true })
}

function openSku(action: 'all' | SkuAction) {
  if (offSale.value) return showToast('商品已下架')
  skuAction.value = action
  skuShow.value = true
}

function requireLogin() {
  if (user.isLogin) return true
  router.push({ name: 'login', query: { redirect: route.fullPath } })
  return false
}

async function onSkuConfirm(p: { sku: Sku; quantity: number; action: SkuAction; seckillId?: string }) {
  if (!requireLogin()) return
  if (p.action === 'cart') {
    try {
      await addCart(p.sku.id, p.quantity)
      skuShow.value = false
      showSuccessToast('已加入购物车')
      cart.refreshCount()
    } catch {
      /* 已提示 */
    }
    return
  }
  skuShow.value = false
  app.setCheckout({
    source: 'BUY_NOW',
    items: [{ skuId: p.sku.id, quantity: p.quantity }],
    seckillId: p.seckillId,
  })
  router.push({ name: 'order-confirm' })
}

async function toggleFavorite() {
  const g = goods.value
  if (!g || !requireLogin()) return
  try {
    if (g.favorited) {
      await removeFavorite(g.id)
      g.favorited = false
      showToast('已取消收藏')
    } else {
      await addFavorite(g.id)
      g.favorited = true
      showToast('收藏成功')
    }
  } catch {
    /* 已提示 */
  }
}

function callService() {
  const phone = app.config.servicePhone
  if (!phone) return showToast('暂无客服电话')
  window.location.href = `tel:${phone}`
}

function back() {
  if (window.history.state?.back) router.back()
  else router.replace('/')
}
</script>

<template>
  <div class="page gd">
    <!-- 顶栏：滚动渐显 -->
    <div class="gd__header">
      <div class="gd__header-bg" :style="{ opacity: headerOpacity }" />
      <span class="gd__circle" :class="{ 'is-solid': headerOpacity > 0.6 }" @click="back">
        <van-icon name="arrow-left" />
      </span>
      <span class="gd__header-title" :style="{ opacity: headerOpacity }">商品详情</span>
      <span class="gd__circle" :class="{ 'is-solid': headerOpacity > 0.6 }" @click="router.push({ name: 'cart' })">
        <van-icon name="shopping-cart-o" />
      </span>
    </div>

    <div v-if="loading" class="gd__skeleton">
      <div class="gd__sk-img" />
      <van-skeleton title :row="4" />
    </div>
    <EmptyState
      v-else-if="notFound || !goods"
      description="商品不存在或已下架"
      button-text="去首页逛逛"
      @click="router.replace('/')"
    />

    <template v-else>
      <!-- 图片轮播 -->
      <div class="gd__swipe">
        <van-swipe :show-indicators="false" @change="(i: number) => (swipeIndex = i)">
          <van-swipe-item v-for="(img, i) in album" :key="img + i" @click="preview(i)">
            <van-image :src="img" width="100%" height="100%" fit="cover" />
          </van-swipe-item>
        </van-swipe>
        <span class="gd__indicator">{{ swipeIndex + 1 }}/{{ album.length }}</span>
      </div>

      <!-- 秒杀横幅 -->
      <div v-if="seckill && seckillOngoing" class="gd__seckill">
        <div class="gd__seckill-left">
          <div class="gd__seckill-price">
            <span class="gd__seckill-tag">秒杀价</span>
            <Price :value="seckill.seckillPrice" size="xl" color="white" />
          </div>
          <div class="gd__seckill-sub">
            <span class="gd__seckill-origin">¥{{ money(goods.price) }}</span>
            <span>已抢{{ seckillPercent }}%</span>
            <span v-if="seckill.limitPerMember">限购{{ seckill.limitPerMember }}件</span>
          </div>
        </div>
        <div class="gd__seckill-right">
          <div class="gd__seckill-label">距结束仅剩</div>
          <CountDown :end="seckill.endAt" theme="white" @finish="load" />
        </div>
      </div>
      <div v-else-if="seckill && seckill.state === 'NOT_STARTED'" class="gd__seckill-pre">
        <van-icon name="clock-o" />
        <span class="ellipsis">秒杀预告：{{ fmtTime(seckill.startAt, 'M月D日 HH:mm') }} 开抢，秒杀价 ¥{{ money(seckill.seckillPrice) }}</span>
      </div>

      <!-- 价格 / 名称 -->
      <div class="gd__card gd__base">
        <div v-if="!seckillOngoing" class="gd__price-row">
          <Price :value="selectedSku?.price ?? goods.price" size="xl" />
          <Price
            v-if="goods.originalPrice && goods.originalPrice > goods.price"
            :value="selectedSku?.originalPrice ?? goods.originalPrice"
            strike
          />
          <span class="gd__sales">已售 {{ fmtSales(goods.sales) }}</span>
        </div>
        <div v-else class="gd__price-row">
          <span class="gd__sales gd__sales--left">已售 {{ fmtSales(goods.sales) }}</span>
        </div>
        <h1 class="gd__name">
          <span v-if="goods.brandName" class="gd__brand">{{ goods.brandName }}</span>{{ goods.name }}
        </h1>
        <p v-if="goods.subtitle" class="gd__subtitle">{{ goods.subtitle }}</p>
      </div>

      <!-- 领券 / 规格 / 配送 -->
      <div class="gd__card gd__cells">
        <div v-if="coupons.length" class="gd__cell" @click="couponShow = true">
          <span class="gd__cell-label">领券</span>
          <div class="gd__cell-value">
            <span v-for="t in couponPreview" :key="t" class="gd__coupon-tag">{{ t }}</span>
          </div>
          <span class="gd__cell-link">领取 <van-icon name="arrow" /></span>
        </div>
        <div class="gd__cell" @click="openSku('all')">
          <span class="gd__cell-label">已选</span>
          <div class="gd__cell-value ellipsis">{{ selectedText }}</div>
          <van-icon name="arrow" class="text-3" />
        </div>
        <div class="gd__cell">
          <span class="gd__cell-label">配送</span>
          <div class="gd__cell-value">
            <div>{{ freightText }}</div>
            <div class="gd__cell-tip">
              <van-icon name="passed" /> 正品保障 <van-icon name="passed" /> 极速退款 <van-icon name="passed" /> 售后无忧
            </div>
          </div>
        </div>
      </div>

      <!-- 评价摘要 -->
      <div class="gd__card">
        <div class="section-title" @click="router.push({ name: 'goods-reviews', params: { id: goods.id } })">
          <span>商品评价({{ goods.reviewCount || 0 }})</span>
          <span class="more">
            <template v-if="goods.reviewCount">好评率 <b class="text-primary">{{ ratePercent(goods.goodRate) }}%</b></template>
            查看全部 <van-icon name="arrow" />
          </span>
        </div>
        <div v-if="!goods.latestReviews?.length" class="gd__no-review">暂无评价，快来抢沙发吧~</div>
        <div v-for="r in goods.latestReviews" :key="r.id" class="gd__review">
          <div class="gd__review-user">
            <van-image :src="r.avatar" round width="24" height="24" fit="cover">
              <template #error><van-icon name="user-o" /></template>
            </van-image>
            <span class="ellipsis">{{ r.nickname }}</span>
            <van-rate :model-value="r.rating" readonly size="10" gutter="1" />
          </div>
          <div class="gd__review-content ellipsis-2">{{ r.content || '用户未填写评价内容' }}</div>
          <div v-if="r.images?.length" class="gd__review-imgs">
            <van-image
              v-for="(img, ii) in r.images.slice(0, 3)"
              :key="img"
              :src="img"
              width="64"
              height="64"
              radius="4"
              fit="cover"
              lazy-load
              @click="showImagePreview({ images: r.images, startPosition: ii })"
            />
          </div>
        </div>
      </div>

      <!-- 详情 -->
      <div class="gd__detail">
        <div class="gd__detail-title"><span>商品详情</span></div>
        <div v-if="detailHtml" class="rich-text gd__detail-html" v-html="detailHtml" />
        <div v-else class="gd__no-review">暂无详情</div>
      </div>

      <!-- 底部操作栏 -->
      <van-action-bar safe-area-inset-bottom placeholder>
        <van-action-bar-icon icon="wap-home-o" text="首页" @click="router.push('/')" />
        <van-action-bar-icon icon="service-o" text="客服" @click="callService" />
        <van-action-bar-icon
          :icon="goods.favorited ? 'like' : 'like-o'"
          :text="goods.favorited ? '已收藏' : '收藏'"
          :color="goods.favorited ? 'var(--mall-primary)' : undefined"
          @click="toggleFavorite"
        />
        <van-action-bar-icon
          icon="cart-o"
          text="购物车"
          :badge="cart.count > 0 ? (cart.count > 99 ? '99+' : cart.count) : undefined"
          @click="router.push({ name: 'cart' })"
        />
        <template v-if="offSale || soldOut">
          <van-action-bar-button type="danger" disabled :text="offSale ? '商品已下架' : '已售罄'" />
        </template>
        <template v-else>
          <van-action-bar-button type="warning" text="加入购物车" @click="openSku('cart')" />
          <van-action-bar-button type="danger" :text="seckillOngoing ? '立即抢购' : '立即购买'" @click="openSku('buy')" />
        </template>
      </van-action-bar>

      <SkuPopup
        v-model:show="skuShow"
        v-model:sku-id="skuId"
        v-model:quantity="quantity"
        :goods="goods"
        :action="skuAction"
        @confirm="onSkuConfirm"
      />

      <!-- 领券弹层 -->
      <van-popup v-model:show="couponShow" position="bottom" round closeable safe-area-inset-bottom>
        <div class="gd__coupon-pop">
          <div class="gd__coupon-pop-title">优惠券</div>
          <div class="gd__coupon-pop-list">
            <CouponCard v-for="c in coupons" :key="c.id" :coupon="c" class="gd__coupon-item">
              <van-button
                size="mini"
                round
                class="btn-gradient"
                :disabled="!canReceive(c)"
                @click="receive(c)"
              >
                {{ statusText(c) }}
              </van-button>
            </CouponCard>
          </div>
        </div>
      </van-popup>
    </template>
  </div>
</template>

<style scoped lang="scss">
.gd {
  &__header {
    position: fixed;
    top: 0;
    left: 50%;
    transform: translateX(-50%);
    width: 100%;
    max-width: $page-max;
    z-index: 90;
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: calc(46px + env(safe-area-inset-top));
    padding: env(safe-area-inset-top) 10px 0;
  }
  &__header-bg {
    position: absolute;
    inset: 0;
    background: $card;
    box-shadow: 0 1px 0 $border;
    pointer-events: none;
  }
  &__header-title {
    position: relative;
    font-size: 16px;
    font-weight: 600;
  }
  &__circle {
    position: relative;
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.35);
    color: #fff;
    font-size: 18px;
    transition: all 0.2s;
    &.is-solid {
      background: transparent;
      color: $text;
    }
  }
  &__skeleton {
    background: $card;
    padding-bottom: 20px;
  }
  &__sk-img {
    width: 100%;
    aspect-ratio: 1 / 1;
    background: $bg;
    margin-bottom: 16px;
  }
  &__swipe {
    position: relative;
    width: 100%;
    aspect-ratio: 1 / 1;
    background: $card;
    .van-swipe {
      height: 100%;
    }
  }
  &__indicator {
    position: absolute;
    right: 12px;
    bottom: 12px;
    padding: 2px 10px;
    border-radius: 999px;
    background: rgba(0, 0, 0, 0.35);
    color: #fff;
    font-size: 12px;
  }
  &__seckill {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: 10px $gap;
    background: $gradient;
    color: #fff;
  }
  &__seckill-left {
    min-width: 0;
  }
  &__seckill-price {
    display: flex;
    align-items: baseline;
    gap: 6px;
  }
  &__seckill-tag {
    padding: 1px 5px;
    font-size: 11px;
    border: 1px solid rgba(255, 255, 255, 0.8);
    border-radius: 3px;
    align-self: center;
  }
  &__seckill-sub {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 4px;
    font-size: 11px;
    opacity: 0.92;
  }
  &__seckill-origin {
    text-decoration: line-through;
  }
  &__seckill-right {
    flex-shrink: 0;
    text-align: center;
  }
  &__seckill-label {
    font-size: 11px;
    margin-bottom: 4px;
  }
  &__seckill-pre {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px $gap;
    font-size: 12px;
    color: $primary;
    background: $primary-light;
  }
  &__card {
    @include card;
    margin: 10px $gap 0;
    padding: 12px;
  }
  &__base {
    margin-top: 0;
    border-radius: 0 0 $radius $radius;
  }
  &__price-row {
    display: flex;
    align-items: baseline;
    gap: 8px;
  }
  &__sales {
    margin-left: auto;
    font-size: 12px;
    color: $text-3;
    &--left {
      margin-left: 0;
    }
  }
  &__name {
    margin: 10px 0 0;
    font-size: 16px;
    line-height: 22px;
    font-weight: 600;
    word-break: break-all;
  }
  &__brand {
    display: inline-block;
    padding: 0 5px;
    margin-right: 4px;
    font-size: 11px;
    line-height: 17px;
    font-weight: normal;
    color: #fff;
    background: $text;
    border-radius: 3px;
    vertical-align: 2px;
  }
  &__subtitle {
    margin: 6px 0 0;
    font-size: 13px;
    color: $text-3;
    line-height: 18px;
  }
  &__cells {
    padding: 0 12px;
  }
  &__cell {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 12px 0;
    font-size: 13px;
    & + & {
      border-top: 1px solid $border;
    }
  }
  &__cell-label {
    flex-shrink: 0;
    color: $text-3;
  }
  &__cell-value {
    flex: 1;
    min-width: 0;
    color: $text;
  }
  &__cell-tip {
    margin-top: 6px;
    font-size: 11px;
    color: $text-3;
    .van-icon {
      color: $primary;
    }
  }
  &__cell-link {
    flex-shrink: 0;
    color: $primary;
    font-size: 12px;
  }
  &__coupon-tag {
    display: inline-block;
    margin-right: 6px;
    padding: 0 6px;
    font-size: 11px;
    line-height: 18px;
    color: $primary;
    background: $primary-light;
    border: 1px dashed $primary;
    border-radius: 3px;
  }
  &__no-review {
    padding: 8px 0;
    font-size: 13px;
    color: $text-3;
    text-align: center;
  }
  &__review {
    padding: 8px 0;
    & + & {
      border-top: 1px solid $border;
    }
  }
  &__review-user {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: $text-2;
    > span {
      max-width: 50%;
    }
  }
  &__review-content {
    margin-top: 6px;
    font-size: 13px;
    line-height: 19px;
  }
  &__review-imgs {
    display: flex;
    gap: 6px;
    margin-top: 6px;
  }
  &__detail {
    margin-top: 10px;
    background: $card;
  }
  &__detail-title {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 14px 0;
    font-size: 14px;
    color: $text-2;
    &::before,
    &::after {
      content: '';
      width: 40px;
      height: 1px;
      margin: 0 10px;
      background: $border-strong;
    }
  }
  &__detail-html {
    padding: 0 0 12px;
    :deep(p) {
      padding: 0 12px;
    }
    :deep(img) {
      padding: 0;
    }
  }
  &__coupon-pop {
    display: flex;
    flex-direction: column;
    max-height: 70vh;
    background: $bg;
  }
  &__coupon-pop-title {
    padding: 14px;
    text-align: center;
    font-size: 16px;
    font-weight: 600;
    background: $card;
  }
  &__coupon-pop-list {
    flex: 1;
    overflow-y: auto;
    padding: 0 $gap 12px;
  }
  &__coupon-item {
    margin-top: 10px;
  }
}
</style>
