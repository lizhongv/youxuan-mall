<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showLoadingToast, closeToast, showToast } from 'vant'
import type { Address, ConfirmResult, CouponOption, OrderSubmit } from '@/types'
import { confirmOrder, submitOrder } from '@/api/order'
import { getAddressList, getDefaultAddress } from '@/api/member'
import { ApiError, CODE, errorMessage } from '@/api/request'
import { useAppStore } from '@/stores/app'
import { useCartStore } from '@/stores/cart'
import { fmtSpec, money, num, uuid } from '@/utils'
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
/** 整页加载失败（网络等）的文案 */
const loadError = ref('')
/** 收货地址相关的业务错误（如「收货地址不在「xx」的配送范围内」），显示在地址下方 */
const addrError = ref('')
/** 出错时用户选中的地址（结算预览失败拿不到出参 address，从地址列表取来展示） */
const pickedAddress = ref<Address | null>(null)
const submitting = ref(false)
const remark = ref(payload.value?.remark || '')
const couponShow = ref(false)
const couponTab = ref(0)
/**
 * 幂等键：同一份结算参数下重复点击/网络重试复用同一个键，
 * 参数变化（换地址、换券）后重新生成。
 */
let idemKey = uuid()

/** 业务错误（非网络 / 非登录失效）且与地址相关 */
function isAddressError(e: unknown) {
  return (
    e instanceof ApiError &&
    e.code !== CODE.NETWORK &&
    e.code !== CODE.UNAUTHORIZED &&
    /地址|配送|省份|收货/.test(e.message)
  )
}

async function refresh() {
  if (!payload.value) {
    loading.value = false
    return
  }
  loadError.value = ''
  try {
    const res = await confirmOrder({ ...payload.value, remark: remark.value })
    data.value = res
    addrError.value = ''
    pickedAddress.value = null
    // 未传地址时后端回退到默认地址：记住它，提交时使用同一个地址
    if (res.address?.id && !payload.value.addressId) payload.value.addressId = res.address.id
    idemKey = uuid()
  } catch (e) {
    if (payload.value.addressId && isAddressError(e)) {
      addrError.value = errorMessage(e)
      const list = await getAddressList().catch(() => [] as Address[])
      pickedAddress.value = list.find((a) => a.id === payload.value?.addressId) || null
    } else if (data.value) {
      // 已有结算数据（如切换优惠券失败）：保留页面，只提示
      showToast(errorMessage(e, '计算失败，请重试'))
    } else {
      loadError.value = errorMessage(e)
    }
  } finally {
    loading.value = false
  }
}

async function init() {
  if (payload.value) {
    if (app.selectedAddressId) {
      // 地址列表（选择模式）/ 新增地址写回的地址优先
      payload.value.addressId = app.selectedAddressId
      persist()
    } else if (!payload.value.addressId) {
      // 主动带出默认地址（后端未传 addressId 时也会回退默认地址，这里保证提交与预览一致）
      const def = await getDefaultAddress().catch(() => null)
      if (def?.id) payload.value.addressId = def.id
    }
  }
  await refresh()
}
onMounted(init)

function retry() {
  loading.value = true
  init()
}

/** 地址卡片展示：出错时展示用户选中的地址 */
const shownAddress = computed<Address | null>(() => (addrError.value ? pickedAddress.value : data.value?.address || null))
const canSubmit = computed(() => !!data.value && !addrError.value)

const usableCoupons = computed(() => (data.value?.coupons || []).filter((c) => c.usable))
const unusableCoupons = computed(() => (data.value?.coupons || []).filter((c) => !c.usable))
const totalQty = computed(() => (data.value?.items || []).reduce((a, i) => a + num(i.quantity), 0))
const selectedCouponId = computed(() => (data.value?.memberCouponId ? String(data.value.memberCouponId) : ''))

const couponText = computed(() => {
  const d = data.value
  if (!d) return ''
  if (num(d.couponAmount) > 0) return `-¥${money(d.couponAmount)}`
  if (usableCoupons.value.length) return payload.value?.memberCouponId === '0' ? '不使用优惠券' : `${usableCoupons.value.length}张可用`
  return '暂无可用'
})

function persist() {
  if (!payload.value) return
  // setCheckout 会同步 selectedAddressId，保持与当前选择一致
  app.setCheckout({ ...payload.value, remark: remark.value })
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
  router.push({ name: 'address', query: { select: '1', id: shownAddress.value?.id || payload.value?.addressId || '' } })
}

async function submit() {
  const d = data.value
  if (!payload.value || !d) return
  if (addrError.value) {
    showToast(addrError.value)
    return
  }
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
    app.selectedAddressId = ''
    router.replace({ name: 'cashier', params: { orderId: res.orderId } })
  } catch (e) {
    /* 已提示；保留幂等键，重试时服务端可识别重复提交。地址类错误同时显示在页面上 */
    if (isAddressError(e)) {
      addrError.value = errorMessage(e)
      pickedAddress.value = d.address
    }
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
    <EmptyState
      v-else-if="loadError && !data && !addrError"
      image="network"
      :description="`结算信息加载失败：${loadError}`"
      button-text="重新加载"
      @click="retry"
    />

    <template v-else-if="data || addrError">
      <!-- 地址 -->
      <div class="confirm__address" @click="chooseAddress">
        <template v-if="shownAddress">
          <van-icon name="location" class="confirm__addr-icon" />
          <div class="confirm__addr-body">
            <div class="confirm__addr-region ellipsis">
              <span v-if="num(shownAddress.isDefault)" class="confirm__default">默认</span>
              {{ shownAddress.province }} {{ shownAddress.city }} {{ shownAddress.district }}
            </div>
            <div class="confirm__addr-detail ellipsis-2">{{ shownAddress.detail }}</div>
            <div class="confirm__addr-user">{{ shownAddress.name }} {{ shownAddress.mobile }}</div>
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
      <div v-if="addrError" class="confirm__addr-error">
        <van-icon name="warning" />
        <span class="confirm__addr-error-text">{{ addrError }}</span>
        <span class="confirm__addr-error-btn" @click="chooseAddress">更换地址</span>
      </div>
      <EmptyState
        v-if="!data"
        image="error"
        description="当前地址无法配送，请更换收货地址"
        button-text="更换收货地址"
        @click="chooseAddress"
      />

      <template v-if="data">
      <!-- 商品 -->
      <div class="card">
        <div v-for="it in data.items" :key="it.skuId" class="confirm__goods">
          <van-image :src="it.pic" width="80" height="80" radius="6" fit="cover" />
          <div class="confirm__goods-body">
            <div class="confirm__goods-name ellipsis-2">
              <span v-if="payload.seckillId" class="confirm__sk">秒杀</span>{{ it.goodsName }}
            </div>
            <div v-if="it.specText" class="confirm__goods-spec ellipsis">{{ fmtSpec(it.specText) }}</div>
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
        <div v-if="num(data.discountAmount) > 0" class="confirm__cell">
          <span>会员折扣</span>
          <span class="text-primary">-¥{{ money(data.discountAmount) }}</span>
        </div>
        <div class="confirm__cell" @click="data.coupons?.length && (couponShow = true)">
          <span>优惠券</span>
          <span :class="num(data.couponAmount) > 0 || usableCoupons.length ? 'text-primary' : 'text-3'">
            {{ couponText }}
            <van-icon v-if="data.coupons?.length" name="arrow" class="text-3" />
          </span>
        </div>
        <div class="confirm__cell">
          <span>运费</span>
          <span v-if="addrError" class="text-3">无法配送</span>
          <span v-else-if="!data.address" class="text-3">
            {{ num(data.freightAmount) > 0 ? `预估 +¥${money(data.freightAmount)}` : '包邮' }}
          </span>
          <span v-else>{{ num(data.freightAmount) > 0 ? `+¥${money(data.freightAmount)}` : '包邮' }}</span>
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
          <span v-if="Number(data.couponAmount) + num(data.discountAmount) > 0" class="confirm__bar-save">
            已优惠 ¥{{ money(num(data.couponAmount) + num(data.discountAmount)) }}
          </span>
        </div>
        <van-button
          round
          class="btn-gradient confirm__submit"
          :disabled="!canSubmit"
          :loading="submitting"
          loading-text="提交中"
          @click="submit"
        >
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
  &__addr-error {
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 8px $gap 0;
    padding: 8px 10px;
    font-size: 12px;
    line-height: 17px;
    color: $primary;
    background: $primary-light;
    border-radius: $radius-sm;
  }
  &__addr-error-text {
    flex: 1;
    min-width: 0;
  }
  &__addr-error-btn {
    flex-shrink: 0;
    padding: 2px 8px;
    border: 1px solid $primary;
    border-radius: 999px;
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
