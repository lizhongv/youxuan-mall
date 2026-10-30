<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showLoadingToast, closeToast, showToast } from 'vant'
import type { ConfirmResult, CouponOption, OrderSubmit } from '@/types'
import { confirmOrder, submitOrder } from '@/api/order'
import { useAppStore } from '@/stores/app'
import { useCartStore } from '@/stores/cart'
import { money, uuid } from '@/utils'
import NavBar from '@/components/NavBar.vue'
import Price from '@/components/Price.vue'
import CouponCard from '@/components/CouponCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const app = useAppStore()
const cart = useCartStore()

const payload = ref<OrderSubmit | null>(app.checkout ? { ...app.checkout } : null)
const data = ref<ConfirmResult | null>(null)
const loading = ref(true)
const failed = ref(false)
const submitting = ref(false)
const remark = ref(payload.value?.remark || '')
const couponShow = ref(false)
const couponTab = ref(0)
/**
 * 幂等键：同一份结算参数下重复点击/网络重试复用同一个键，
 * 参数变化（换地址、换券）后重新生成。
 */
let idemKey = uuid()

async function refresh() {
  if (!payload.value) {
    loading.value = false
    return
  }
  // 地址列表选择模式写回的地址优先
  if (app.selectedAddressId) payload.value.addressId = app.selectedAddressId
  failed.value = false
  try {
    data.value = await confirmOrder({ ...payload.value, remark: remark.value })
    if (data.value.address?.id && !payload.value.addressId) payload.value.addressId = data.value.address.id
    idemKey = uuid()
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}
onMounted(refresh)

const usableCoupons = computed(() => (data.value?.coupons || []).filter((c) => c.usable))
const unusableCoupons = computed(() => (data.value?.coupons || []).filter((c) => !c.usable))
const totalQty = computed(() => (data.value?.items || []).reduce((a, i) => a + i.quantity, 0))
const selectedCouponId = computed(() => (data.value?.memberCouponId ? String(data.value.memberCouponId) : ''))

const couponText = computed(() => {
  const d = data.value
  if (!d) return ''
  if (Number(d.couponAmount) > 0) return `-¥${money(d.couponAmount)}`
  if (usableCoupons.value.length) return payload.value?.memberCouponId === '0' ? '不使用优惠券' : `${usableCoupons.value.length}张可用`
  return '暂无可用'
})

function persist() {
  if (payload.value) app.setCheckout({ ...payload.value, remark: remark.value })
}

async function chooseCoupon(c: CouponOption | null) {
  if (!payload.value) return
  if (c && !c.usable) return
  payload.value.memberCouponId = c ? String(c.memberCouponId) : '0'
  couponShow.value = false
  persist()
  showLoadingToast({ message: '计算中...', forbidClick: true })
  await refresh()
  closeToast()
}

function chooseAddress() {
  persist()
  router.push({ name: 'address', query: { select: '1', id: data.value?.address?.id || '' } })
}

async function submit() {
  const d = data.value
  if (!payload.value || !d) return
  if (!d.address) {
    showToast('请先添加收货地址')
    chooseAddress()
    return
  }
  submitting.value = true
  try {
    const res = await submitOrder(
      { ...payload.value, addressId: d.address.id, memberCouponId: payload.value.memberCouponId ?? (d.memberCouponId ? String(d.memberCouponId) : '0'), remark: remark.value },
      idemKey,
    )
    if (payload.value.source === 'CART') cart.refreshCount()
    sessionStorage.removeItem('mall_h5_checkout')
    router.replace({ name: 'cashier', params: { orderId: res.orderId } })
  } catch {
    /* 已提示；保留幂等键，重试时服务端可识别重复提交 */
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page confirm">
    <NavBar title="确认订单" />

    <van-loading v-if="loading" class="confirm__loading" vertical>加载中...</van-loading>
    <EmptyState
      v-else-if="!payload"
      description="没有要结算的商品"
      button-text="去购物车"
      @click="router.replace({ name: 'cart' })"
    />
    <EmptyState v-else-if="failed && !data" image="error" description="结算信息加载失败" button-text="重试" @click="refresh" />

    <template v-else-if="data">
      <!-- 地址 -->
      <div class="confirm__address" @click="chooseAddress">
        <template v-if="data.address">
          <van-icon name="location" class="confirm__addr-icon" />
          <div class="confirm__addr-body">
            <div class="confirm__addr-region ellipsis">
              <span v-if="data.address.isDefault" class="confirm__default">默认</span>
              {{ data.address.province }} {{ data.address.city }} {{ data.address.district }}
            </div>
            <div class="confirm__addr-detail ellipsis-2">{{ data.address.detail }}</div>
            <div class="confirm__addr-user">{{ data.address.name }} {{ data.address.mobile }}</div>
          </div>
          <van-icon name="arrow" class="text-3" />
        </template>
        <template v-else>
          <van-icon name="add-o" class="confirm__addr-icon" />
          <div class="confirm__addr-body confirm__addr-empty">添加收货地址</div>
          <van-icon name="arrow" class="text-3" />
        </template>
        <div class="confirm__addr-stripe" />
      </div>

      <!-- 商品 -->
      <div class="card">
        <div v-for="it in data.items" :key="it.skuId" class="confirm__goods">
          <van-image :src="it.pic" width="80" height="80" radius="6" fit="cover" />
          <div class="confirm__goods-body">
            <div class="confirm__goods-name ellipsis-2">
              <span v-if="payload.seckillId" class="confirm__sk">秒杀</span>{{ it.goodsName }}
            </div>
            <div v-if="it.specText" class="confirm__goods-spec ellipsis">{{ it.specText }}</div>
            <div class="confirm__goods-foot">
              <Price :value="it.price" size="sm" color="inherit" />
              <span class="text-3">x{{ it.quantity }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 金额 -->
      <div class="card confirm__cells">
        <div class="confirm__cell">
          <span>商品金额</span>
          <Price :value="data.goodsAmount" size="xs" color="inherit" />
        </div>
        <div v-if="Number(data.discountAmount) > 0" class="confirm__cell">
          <span>会员折扣</span>
          <span class="text-primary">-¥{{ money(data.discountAmount) }}</span>
        </div>
        <div class="confirm__cell" @click="data.coupons?.length && (couponShow = true)">
          <span>优惠券</span>
          <span :class="Number(data.couponAmount) > 0 || usableCoupons.length ? 'text-primary' : 'text-3'">
            {{ couponText }}
            <van-icon v-if="data.coupons?.length" name="arrow" class="text-3" />
          </span>
        </div>
        <div class="confirm__cell">
          <span>运费</span>
          <span>{{ Number(data.freightAmount) > 0 ? `+¥${money(data.freightAmount)}` : '包邮' }}</span>
        </div>
        <div class="confirm__cell confirm__cell--total">
          <span />
          <span>
            共 {{ totalQty }} 件，小计：<Price :value="data.payAmount" size="md" />
          </span>
        </div>
      </div>

      <!-- 留言 -->
      <div class="card confirm__remark">
        <van-field
          v-model="remark"
          label="订单备注"
          placeholder="选填，请先和商家协商一致"
          maxlength="200"
          :border="false"
          @blur="persist"
        />
      </div>
      <div class="confirm__space" />

      <!-- 提交栏 -->
      <div class="confirm__bar">
        <div class="confirm__bar-sum">
          <span>合计：</span>
          <Price :value="data.payAmount" size="lg" />
          <span v-if="Number(data.couponAmount) + Number(data.discountAmount) > 0" class="confirm__bar-save">
            已优惠 ¥{{ money(Number(data.couponAmount) + Number(data.discountAmount)) }}
          </span>
        </div>
        <van-button round class="btn-gradient confirm__submit" :loading="submitting" loading-text="提交中" @click="submit">
          提交订单
        </van-button>
      </div>

      <!-- 优惠券选择 -->
      <van-popup v-model:show="couponShow" position="bottom" round closeable safe-area-inset-bottom>
        <div class="cp">
          <div class="cp__title">选择优惠券</div>
          <van-tabs v-model:active="couponTab" shrink>
            <van-tab :title="`可用(${usableCoupons.length})`" />
            <van-tab :title="`不可用(${unusableCoupons.length})`" />
          </van-tabs>
          <div class="cp__list">
            <template v-if="couponTab === 0">
              <EmptyState v-if="!usableCoupons.length" description="暂无可用优惠券" />
              <CouponCard
                v-for="c in usableCoupons"
                :key="c.memberCouponId"
                :coupon="c"
                :selected="selectedCouponId === String(c.memberCouponId)"
                class="cp__item"
                @click="chooseCoupon(c)"
              >
                <van-radio :name="String(c.memberCouponId)" :model-value="selectedCouponId" />
              </CouponCard>
            </template>
            <template v-else>
              <EmptyState v-if="!unusableCoupons.length" description="没有不可用的优惠券" />
              <CouponCard
                v-for="c in unusableCoupons"
                :key="c.memberCouponId"
                :coupon="c"
                disabled
                :reason="c.unusableReason || '不满足使用条件'"
                class="cp__item"
              />
            </template>
          </div>
          <div v-if="couponTab === 0" class="cp__foot">
            <van-button round block plain type="danger" @click="chooseCoupon(null)">不使用优惠券</van-button>
          </div>
        </div>
      </van-popup>
    </template>
  </div>
</template>

<style scoped lang="scss">
.confirm {
  &__loading {
    padding: 60px 0;
  }
  &__address {
    position: relative;
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 10px $gap 0;
    padding: 14px 12px 18px;
    background: $card;
    border-radius: $radius;
    overflow: hidden;
  }
  &__addr-icon {
    flex-shrink: 0;
    width: 32px;
    height: 32px;
    line-height: 32px;
    text-align: center;
    border-radius: 50%;
    background: $gradient;
    color: #fff;
    font-size: 16px;
  }
  &__addr-body {
    flex: 1;
    min-width: 0;
  }
  &__addr-empty {
    font-size: 15px;
    font-weight: 600;
  }
  &__addr-region {
    font-size: 12px;
    color: $text-2;
  }
  &__default {
    display: inline-block;
    padding: 0 4px;
    margin-right: 4px;
    font-size: 10px;
    line-height: 15px;
    color: #fff;
    background: $primary;
    border-radius: 3px;
  }
  &__addr-detail {
    margin: 4px 0;
    font-size: 15px;
    font-weight: 600;
    line-height: 20px;
  }
  &__addr-user {
    font-size: 13px;
    color: $text-2;
  }
  &__addr-stripe {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 3px;
    background: repeating-linear-gradient(
      -45deg,
      $primary 0 12px,
      transparent 12px 16px,
      $blue 16px 28px,
      transparent 28px 32px
    );
  }
  &__goods {
    display: flex;
    gap: 10px;
    & + & {
      margin-top: 12px;
    }
  }
  &__goods-body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }
  &__goods-name {
    font-size: 13px;
    line-height: 18px;
  }
  &__sk {
    display: inline-block;
    padding: 0 4px;
    margin-right: 4px;
    font-size: 10px;
    line-height: 15px;
    color: #fff;
    background: $gradient;
    border-radius: 3px;
  }
  &__goods-spec {
    margin-top: 4px;
    font-size: 12px;
    color: $text-3;
  }
  &__goods-foot {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: auto;
  }
  &__cells {
    padding: 4px 12px;
  }
  &__cell {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 0;
    font-size: 13px;
    &--total {
      border-top: 1px solid $border;
    }
  }
  &__remark {
    padding: 0;
    overflow: hidden;
  }
  &__space {
    height: calc(70px + env(safe-area-inset-bottom));
  }
  &__bar {
    @include fixed-bottom;
    display: flex;
    align-items: center;
    gap: 10px;
    height: calc(56px + env(safe-area-inset-bottom));
    padding-left: $gap;
    padding-right: $gap;
    border-top: 1px solid $border;
  }
  &__bar-sum {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: baseline;
    flex-wrap: wrap;
    justify-content: flex-end;
    column-gap: 2px;
    font-size: 13px;
  }
  &__bar-save {
    width: 100%;
    text-align: right;
    font-size: 11px;
    color: $primary;
  }
  &__submit {
    flex-shrink: 0;
    width: 110px;
    height: 40px;
  }
}
.cp {
  display: flex;
  flex-direction: column;
  height: 70vh;
  background: $bg;
  &__title {
    padding: 14px;
    text-align: center;
    font-size: 16px;
    font-weight: 600;
    background: $card;
  }
  &__list {
    flex: 1;
    overflow-y: auto;
    padding: 0 $gap 12px;
  }
  &__item {
    margin-top: 10px;
  }
  &__foot {
    padding: 8px $gap;
    background: $card;
  }
}
</style>
