<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { closeToast, showConfirmDialog, showDialog, showLoadingToast, showToast } from 'vant'
import type { Order, PayChannel, PayType, Profile } from '@/types'
import { getOrder, getPayChannels, payOrder } from '@/api/order'
import { getProfile } from '@/api/member'
import { ApiError, CODE, errorMessage } from '@/api/request'
import { isWechat, money, parseTime, uuid } from '@/utils'
import { invokeWechatPay, rememberPayingOrder } from '@/utils/pay'
import NavBar from '@/components/NavBar.vue'
import Price from '@/components/Price.vue'
import CountDown from '@/components/CountDown.vue'
import LoadState from '@/components/LoadState.vue'

const route = useRoute()
const router = useRouter()
const orderId = route.params.orderId as string
const inWechat = isWechat()

const order = ref<Order | null>(null)
const channels = ref<PayChannel[]>([])
const profile = ref<Profile | null>(null)
const loading = ref(true)
const loadError = ref('')
const paying = ref(false)
const expired = ref(false)
/** 不默认选模拟支付；余额不足时也不自动切换，由用户选择 */
const payType = ref<PayType | ''>('')

/**
 * 支付幂等键：同一支付方式在网络重试 / 重复点击时复用；
 * 服务端已明确返回失败（密码错误、金额变化等）后换新键。
 */
const idemKeys = new Map<PayType, string>()
function idemKey(t: PayType) {
  let k = idemKeys.get(t)
  if (!k) {
    k = uuid()
    idemKeys.set(t, k)
  }
  return k
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const [o, ch, p] = await Promise.all([
      getOrder(orderId),
      getPayChannels(orderId, inWechat),
      getProfile(true).catch(() => null),
    ])
    order.value = o
    channels.value = ch || []
    profile.value = p
    if (o.status !== 'PENDING_PAY') {
      router.replace({ name: 'order-detail', params: { id: orderId } })
      return
    }
    expired.value = !!o.payExpireAt && parseTime(o.payExpireAt) <= Date.now()
    const cur = channels.value.find((c) => c.type === payType.value && c.enabled)
    if (!cur) {
      const first = channels.value.find((c) => c.enabled && c.type !== 'MOCK')
      payType.value = first ? first.type : ''
    }
  } catch (e) {
    loadError.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}
onMounted(load)

const ICONS: Record<string, { name: string; cls: string }> = {
  balance: { name: 'balance-o', cls: 'balance' },
  wechat: { name: 'wechat-pay', cls: 'wechat' },
  alipay: { name: 'alipay', cls: 'alipay' },
  mock: { name: 'gold-coin-o', cls: 'mock' },
}
const iconOf = (c: PayChannel) => ICONS[c.icon] || ICONS.mock

function channelSub(c: PayChannel) {
  if (!c.enabled) return c.reason || '暂不可用'
  if (c.type === 'BALANCE') return `可用余额 ¥${money(profile.value?.balance)}`
  if (c.type === 'MOCK') return '演示环境，直接支付成功'
  return ''
}

function choose(c: PayChannel) {
  if (!c.enabled) {
    if (c.reason) showToast(c.reason)
    return
  }
  payType.value = c.type
}

const selected = computed(() => channels.value.find((c) => c.type === payType.value && c.enabled) || null)

function onExpire() {
  expired.value = true
  showDialog({ title: '订单已关闭', message: '支付超时，订单已自动关闭' }).then(() =>
    router.replace({ name: 'order-detail', params: { id: orderId } }),
  )
}

function toResult(type: string, poll: boolean) {
  router.replace({
    name: 'pay-result',
    query: { orderId, amount: String(order.value?.payAmount ?? ''), type, ...(poll ? { poll: '1' } : {}) },
  })
}

/** 金额变化（商家改价与支付并发）：刷新订单后让用户确认新金额 */
function isAmountChanged(e: unknown) {
  return e instanceof ApiError && /金额已变化|刷新后重试/.test(e.message)
}

async function handlePayError(e: unknown, type: PayType) {
  if (e instanceof ApiError && e.code !== CODE.NETWORK) idemKeys.delete(type)
  if (isAmountChanged(e)) {
    await showDialog({ title: '订单金额已变化', message: '商家已调整订单金额，请确认新的金额后重新支付' }).catch(() => {})
    await load()
    return
  }
  if (e instanceof ApiError && /已支付/.test(e.message)) {
    showToast(e.message)
    router.replace({ name: 'order-detail', params: { id: orderId } })
    return
  }
  showToast(errorMessage(e, '支付失败，请重试'))
}

/* ---------------- 余额支付：6 位支付密码 ---------------- */
const pwdShow = ref(false)
const pwd = ref('')
const pwdError = ref('')
const pwdSubmitting = ref(false)

function guideSetPayPassword(message = '为保障资金安全，余额支付需要先设置 6 位支付密码') {
  showConfirmDialog({ title: '请先设置支付密码', message, confirmButtonText: '去设置', cancelButtonText: '换种方式' })
    .then(() => router.push({ name: 'pay-password', query: { redirect: route.fullPath } }))
    .catch(() => {})
}

function openBalancePay() {
  if (profile.value && profile.value.hasPayPassword === false) {
    guideSetPayPassword()
    return
  }
  pwd.value = ''
  pwdError.value = ''
  pwdShow.value = true
}

watch(pwd, (v) => {
  if (v.length) pwdError.value = ''
  if (v.length === 6) submitBalance()
})

async function submitBalance() {
  if (pwdSubmitting.value) return
  pwdSubmitting.value = true
  try {
    await payOrder(orderId, 'BALANCE', idemKey('BALANCE'), pwd.value)
    pwdShow.value = false
    toResult('BALANCE', false)
  } catch (e) {
    if (!(e instanceof ApiError && e.code === CODE.NETWORK)) idemKeys.delete('BALANCE')
    pwd.value = ''
    if (e instanceof ApiError && e.code === CODE.PAY_PWD_WRONG) {
      pwdError.value = e.message || '支付密码错误'
    } else if (e instanceof ApiError && e.code === CODE.PAY_PWD_UNSET) {
      pwdShow.value = false
      guideSetPayPassword(e.message)
    } else if (e instanceof ApiError && e.code === CODE.PAY_PWD_LOCKED) {
      pwdShow.value = false
      showDialog({ title: '支付密码已锁定', message: e.message || '错误次数过多，请 30 分钟后再试或使用其他支付方式' })
    } else {
      pwdShow.value = false
      await handlePayError(e, 'BALANCE')
    }
  } finally {
    pwdSubmitting.value = false
  }
}

/* ---------------- 发起支付 ---------------- */
async function pay() {
  const ch = selected.value
  if (!order.value || expired.value) return
  if (!ch) return showToast('请选择支付方式')
  if (ch.type === 'BALANCE') return openBalancePay()

  paying.value = true
  try {
    const res = await payOrder(orderId, ch.type, idemKey(ch.type))
    if (res.paid) {
      toResult(ch.type, false)
      return
    }
    const params = res.payParams || {}
    // 跳出到渠道前记住订单，渠道回跳（如支付宝 return_url）缺少 orderId 时结果页从这里取
    rememberPayingOrder(orderId, String(order.value.payAmount), ch.type)
    const resultUrl =
      window.location.origin + router.resolve({ name: 'pay-result', query: { orderId, type: ch.type, poll: '1' } }).href
    if (ch.type === 'WECHAT_JSAPI') {
      const r = await invokeWechatPay(params)
      if (r === 'cancel') showToast('已取消支付')
      else toResult(ch.type, true) // 成功或失败都以服务端为准，去结果页轮询
    } else if (ch.type === 'WECHAT_H5' && params.h5Url) {
      showLoadingToast({ message: '正在跳转微信支付...', forbidClick: true, duration: 3000 })
      window.location.href = params.h5Url + '&redirect_url=' + encodeURIComponent(resultUrl)
    } else if (ch.type === 'ALIPAY_WAP' && params.payUrl) {
      showLoadingToast({ message: '正在跳转支付宝...', forbidClick: true, duration: 3000 })
      window.location.href = params.payUrl
    } else {
      showToast('支付参数异常，请换种方式支付')
    }
  } catch (e) {
    closeToast()
    await handlePayError(e, ch.type)
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
    <LoadState :loading="loading" :error="loadError" :ready="!!order" @retry="load">
      <template v-if="order">
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

        <div class="card cashier__methods">
          <div v-if="!channels.length" class="cashier__none">暂无可用的支付方式，请联系客服</div>
          <div
            v-for="c in channels"
            :key="c.type"
            class="cashier__method"
            :class="{ 'is-disabled': !c.enabled }"
            @click="choose(c)"
          >
            <span class="cashier__icon" :class="`cashier__icon--${iconOf(c).cls}`">
              <van-icon :name="iconOf(c).name" />
            </span>
            <div class="cashier__method-body">
              <div>{{ c.name }}</div>
              <div v-if="channelSub(c)" class="cashier__method-sub" :class="{ 'is-reason': !c.enabled }">
                {{ channelSub(c) }}
              </div>
            </div>
            <van-radio :name="c.type" :model-value="payType" :disabled="!c.enabled" />
          </div>
        </div>

        <div class="cashier__bar">
          <van-button
            block
            round
            class="btn-gradient"
            :disabled="expired || !selected"
            :loading="paying"
            loading-text="支付中..."
            @click="pay"
          >
            {{ expired ? '订单已关闭' : selected ? `${selected.name} ¥${money(order.payAmount)}` : '请选择支付方式' }}
          </van-button>
        </div>

        <!-- 余额支付密码 -->
        <van-popup
          v-model:show="pwdShow"
          position="bottom"
          round
          closeable
          :close-on-click-overlay="!pwdSubmitting"
          class="cashier__pwd"
        >
          <div class="cashier__pwd-title">请输入支付密码</div>
          <div class="cashier__pwd-amount">
            <Price :value="order.payAmount" size="lg" color="inherit" />
          </div>
          <div class="cashier__pwd-sub">余额支付 · 可用 ¥{{ money(profile?.balance) }}</div>
          <van-password-input :value="pwd" :focused="pwdShow" :error-info="pwdError" />
          <div class="cashier__pwd-links">
            <van-loading v-if="pwdSubmitting" size="16">支付中...</van-loading>
            <span v-else @click="router.push({ name: 'pay-password', query: { redirect: route.fullPath } })">忘记密码？</span>
          </div>
          <van-number-keyboard v-model="pwd" :show="pwdShow" :maxlength="6" :hide-on-click-outside="false" />
          <div class="cashier__pwd-kb" />
        </van-popup>
      </template>
    </LoadState>
  </div>
</template>

<style scoped lang="scss">
.cashier {
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
  &__none {
    padding: 20px 0;
    text-align: center;
    font-size: 13px;
    color: $text-3;
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
      .cashier__icon,
      .cashier__method-body > div:first-child {
        opacity: 0.45;
      }
    }
  }
  &__icon {
    flex-shrink: 0;
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
    &--wechat {
      background: $success;
    }
    &--alipay {
      background: $blue;
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
    &.is-reason {
      color: $warning;
    }
  }
  &__bar {
    @include fixed-bottom;
    padding-left: $gap;
    padding-right: $gap;
    padding-top: 8px;
    padding-bottom: calc(8px + env(safe-area-inset-bottom));
  }
  &__pwd {
    padding-top: 18px;
  }
  &__pwd-title {
    text-align: center;
    font-size: 16px;
    font-weight: 600;
  }
  &__pwd-amount {
    margin-top: 14px;
    text-align: center;
    color: $text;
  }
  &__pwd-sub {
    margin: 6px 0 18px;
    text-align: center;
    font-size: 12px;
    color: $text-3;
  }
  &__pwd-links {
    display: flex;
    justify-content: flex-end;
    padding: 12px 16px 16px;
    font-size: 13px;
    color: $blue;
  }
  /* 为数字键盘留出空间（键盘固定在底部，高度约 245px + 安全区） */
  &__pwd-kb {
    height: calc(250px + env(safe-area-inset-bottom));
  }
}
</style>
