<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showImagePreview, showSuccessToast, showToast } from 'vant'
import type { Aftersale } from '@/types'
import { cancelAftersale, fillReturn, getAftersale } from '@/api/aftersale'
import { AFTERSALE_STATUS_TEXT, AFTERSALE_TYPE_TEXT, EXPRESS_COMPANIES } from '@/utils/dict'
import { copyText, fmtSpec, fmtTime, money } from '@/utils'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadState from '@/components/LoadState.vue'
import { useDict } from '@/composables/useDict'
import { errorMessage } from '@/api/request'

const route = useRoute()
const router = useRouter()
const id = route.params.id as string
const data = ref<Aftersale | null>(null)
const loading = ref(true)
const loadError = ref('')

const returnShow = ref(false)
const companyShow = ref(false)
const returnCompany = ref('')
const returnNo = ref('')
const submitting = ref(false)

async function load() {
  loadError.value = ''
  try {
    data.value = await getAftersale(id)
  } catch (e) {
    loadError.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}
onMounted(load)

/** 进度步骤：按类型生成流程，并定位当前步骤 */
const steps = computed(() => {
  const a = data.value
  if (!a) return { list: [] as string[], active: 0 }
  if (a.status === 'REJECTED') return { list: ['提交申请', '商家审核', '已拒绝'], active: 2 }
  if (a.status === 'CANCELLED') return { list: ['提交申请', '已撤销'], active: 1 }
  if (a.type === 'REFUND') {
    const list = ['提交申请', '商家审核', '退款成功']
    return { list, active: a.status === 'REFUNDED' || a.status === 'REFUNDING' ? 2 : 1 }
  }
  const list = ['提交申请', '商家审核', '寄回商品', '商家收货', '退款成功']
  const map: Record<string, number> = { APPLYING: 1, WAIT_RETURN: 2, RETURNING: 3, REFUNDING: 4, REFUNDED: 4 }
  return { list, active: map[a.status] ?? 0 }
})

const statusDesc = computed(() => {
  switch (data.value?.status) {
    case 'APPLYING':
      return '您的申请已提交，请等待商家审核'
    case 'WAIT_RETURN':
      return '商家已同意退货，请寄回商品并填写物流单号'
    case 'RETURNING':
      return '商品寄回中，商家确认收货后将为您退款'
    case 'REFUNDING':
      return '商家已同意退款，退款处理中，预计 1-3 个工作日原路退回'
    case 'REFUNDED':
      return '退款已原路退回，请注意查收'
    case 'REJECTED':
      return '商家拒绝了您的申请'
    case 'CANCELLED':
      return '您已撤销本次售后申请'
    default:
      return ''
  }
})

const canCancel = computed(() => data.value && ['APPLYING', 'WAIT_RETURN'].includes(data.value.status))
/** 快递公司取字典 express_company（label 中文名），失败时用内置列表 */
const companies = useDict('express_company', EXPRESS_COMPANIES)
const companyActions = computed(() => [...companies.value.map((c) => ({ name: c.label })), { name: '其他' }])

/** 商家退货地址（退货退款、待寄回及之后返回）；缺失项为空串 */
const returnAddr = computed(() => {
  const r = data.value?.returnAddress
  if (!r || (!r.name && !r.mobile && !r.address)) return null
  return r
})
const returnAddrText = computed(() => {
  const r = returnAddr.value
  return r ? [r.name, r.mobile, r.address].filter(Boolean).join(' ') : ''
})

async function cancel() {
  try {
    await showConfirmDialog({ title: '撤销申请', message: '撤销后本次售后将关闭，确定撤销吗？' })
  } catch {
    return
  }
  try {
    await cancelAftersale(id)
    showSuccessToast('已撤销')
    load()
  } catch {
    /* 已提示 */
  }
}

async function submitReturn() {
  if (!returnCompany.value) return showToast('请选择快递公司')
  if (!returnNo.value.trim()) return showToast('请填写快递单号')
  submitting.value = true
  try {
    await fillReturn(id, { returnCompany: returnCompany.value, returnNo: returnNo.value.trim() })
    returnShow.value = false
    showSuccessToast('提交成功')
    load()
  } catch {
    /* 已提示 */
  } finally {
    submitting.value = false
  }
}
function onCompany(a: { name: string }) {
  returnCompany.value = a.name
}
</script>

<template>
  <div class="page asd">
    <NavBar title="售后详情" />
    <LoadState v-if="!data" :loading="loading" :error="loadError" @retry="(loading = true), load()">
      <EmptyState description="售后单不存在" />
    </LoadState>
    <template v-else>
      <div class="asd__head">
        <div class="asd__status">{{ data.statusText || AFTERSALE_STATUS_TEXT[data.status] }}</div>
        <div class="asd__desc">{{ statusDesc }}</div>
      </div>

      <div class="card asd__steps">
        <van-steps :active="steps.active" active-icon="checked">
          <van-step v-for="s in steps.list" :key="s">{{ s }}</van-step>
        </van-steps>
      </div>

      <div v-if="data.auditRemark" class="card asd__remark">
        <div class="asd__label">商家处理意见</div>
        <div class="asd__remark-text">{{ data.auditRemark }}</div>
        <div v-if="data.auditTime" class="asd__time">{{ fmtTime(data.auditTime) }}</div>
      </div>

      <!-- 商家退货地址 -->
      <div v-if="returnAddr" class="card asd__raddr">
        <div class="asd__raddr-head">
          <span class="asd__label">商家退货地址</span>
          <span class="asd__copy" @click="copyText(returnAddrText)">一键复制</span>
        </div>
        <div class="asd__raddr-user">
          {{ returnAddr.name || '-' }}<span>{{ returnAddr.mobile }}</span>
        </div>
        <div class="asd__raddr-detail">{{ returnAddr.address || '商家暂未填写退货地址，请联系客服' }}</div>
      </div>

      <!-- 待寄回 -->
      <div v-if="data.status === 'WAIT_RETURN'" class="card asd__return">
        <div class="asd__label">请寄回商品</div>
        <div class="asd__return-tip">请在商品包装内附上售后单号 {{ data.aftersaleNo }}，寄出后填写物流信息</div>
        <van-button round block class="btn-gradient" @click="returnShow = true">填写退货物流</van-button>
      </div>
      <div v-if="data.returnNo" class="card asd__rows">
        <div class="asd__row"><span>退货快递</span><span>{{ data.returnCompany }}</span></div>
        <div class="asd__row">
          <span>退货单号</span>
          <span>{{ data.returnNo }} <span class="asd__copy" @click="copyText(data.returnNo!)">复制</span></span>
        </div>
      </div>

      <div class="card">
        <div v-if="data.item" class="asd__goods" @click="router.push({ name: 'order-detail', params: { id: data.orderId } })">
          <van-image :src="data.item.pic" width="64" height="64" radius="6" fit="cover" />
          <div class="asd__goods-body">
            <div class="ellipsis-2">{{ data.item.goodsName }}</div>
            <div class="asd__spec ellipsis">{{ fmtSpec(data.item.specText) }} x{{ data.quantity || data.item.quantity }}</div>
          </div>
        </div>
        <div class="asd__rows asd__rows--inner">
          <div class="asd__row"><span>售后类型</span><span>{{ AFTERSALE_TYPE_TEXT[data.type] }}</span></div>
          <div class="asd__row"><span>退款金额</span><span class="text-primary">¥{{ money(data.amount) }}</span></div>
          <div class="asd__row"><span>申请原因</span><span>{{ data.reason }}</span></div>
          <div v-if="data.description" class="asd__row"><span>问题描述</span><span>{{ data.description }}</span></div>
          <div class="asd__row">
            <span>售后单号</span>
            <span>{{ data.aftersaleNo }} <span class="asd__copy" @click="copyText(data.aftersaleNo)">复制</span></span>
          </div>
          <div class="asd__row"><span>订单编号</span><span>{{ data.orderNo }}</span></div>
          <div class="asd__row"><span>申请时间</span><span>{{ fmtTime(data.createdAt, 'YYYY-MM-DD HH:mm:ss') }}</span></div>
          <div v-if="data.refundTime" class="asd__row">
            <span>退款时间</span><span>{{ fmtTime(data.refundTime, 'YYYY-MM-DD HH:mm:ss') }}</span>
          </div>
        </div>
        <div v-if="data.images?.length" class="asd__imgs">
          <van-image
            v-for="(img, i) in data.images"
            :key="img"
            :src="img"
            width="64"
            height="64"
            radius="4"
            fit="cover"
            @click="showImagePreview({ images: data.images, startPosition: i })"
          />
        </div>
      </div>

      <template v-if="canCancel">
        <div class="asd__space" />
        <div class="asd__bar">
          <van-button round size="small" class="asd__btn" @click="cancel">撤销申请</van-button>
          <van-button
            v-if="data.status === 'WAIT_RETURN'"
            round
            size="small"
            class="asd__btn asd__btn--primary"
            @click="returnShow = true"
          >
            填写物流
          </van-button>
        </div>
      </template>

      <!-- 退货物流表单 -->
      <van-popup v-model:show="returnShow" position="bottom" round closeable safe-area-inset-bottom>
        <div class="asd__form">
          <div class="asd__form-title">填写退货物流</div>
          <van-cell-group>
            <van-field
              :model-value="returnCompany"
              label="快递公司"
              placeholder="请选择快递公司"
              readonly
              is-link
              @click="companyShow = true"
            />
            <van-field v-model="returnNo" label="快递单号" placeholder="请输入快递单号" maxlength="40" clearable />
          </van-cell-group>
          <div class="asd__form-btn">
            <van-button round block class="btn-gradient" :loading="submitting" @click="submitReturn">提交</van-button>
          </div>
        </div>
      </van-popup>
      <van-action-sheet
        v-model:show="companyShow"
        :actions="companyActions"
        cancel-text="取消"
        close-on-click-action
        @select="onCompany"
      />
    </template>
  </div>
</template>

<style scoped lang="scss">
.asd {
  &__loading {
    padding: 60px 0;
  }
  &__raddr-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  &__raddr-user {
    margin-top: 8px;
    font-size: 15px;
    font-weight: 600;
    span {
      margin-left: 8px;
      font-size: 13px;
      font-weight: normal;
      color: $text-2;
    }
  }
  &__raddr-detail {
    margin-top: 4px;
    font-size: 13px;
    line-height: 19px;
    color: $text-2;
    word-break: break-all;
  }
  &__head {
    padding: 20px 20px 24px;
    background: $gradient;
    color: #fff;
  }
  &__status {
    font-size: 20px;
    font-weight: 700;
  }
  &__desc {
    margin-top: 6px;
    font-size: 12px;
    opacity: 0.92;
  }
  &__steps {
    padding: 4px 0;
    overflow: hidden;
  }
  &__label {
    font-size: 14px;
    font-weight: 600;
  }
  &__remark-text {
    margin-top: 8px;
    font-size: 13px;
    line-height: 19px;
    color: $text-2;
  }
  &__time {
    margin-top: 6px;
    font-size: 11px;
    color: $text-3;
  }
  &__return-tip {
    margin: 8px 0 12px;
    font-size: 12px;
    color: $text-2;
    line-height: 18px;
  }
  &__goods {
    display: flex;
    gap: 10px;
    font-size: 13px;
    line-height: 18px;
    padding-bottom: 10px;
    border-bottom: 1px solid $border;
  }
  &__goods-body {
    flex: 1;
    min-width: 0;
  }
  &__spec {
    margin-top: 4px;
    font-size: 12px;
    color: $text-3;
  }
  &__rows {
    padding: 4px 12px;
    &--inner {
      padding: 4px 0;
    }
  }
  &__row {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    padding: 7px 0;
    font-size: 13px;
    > span:first-child {
      flex-shrink: 0;
      color: $text-2;
    }
    > span:last-child {
      text-align: right;
      word-break: break-all;
    }
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
  &__imgs {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    padding-top: 6px;
  }
  &__space {
    height: calc(64px + env(safe-area-inset-bottom));
  }
  &__bar {
    @include fixed-bottom;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 8px;
    height: calc(54px + env(safe-area-inset-bottom));
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
  &__form-title {
    padding: 14px;
    text-align: center;
    font-size: 16px;
    font-weight: 600;
  }
  &__form-btn {
    padding: 16px;
  }
}
</style>
