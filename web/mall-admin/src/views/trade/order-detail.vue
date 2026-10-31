<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { FormInstance } from 'element-plus'
import { orderApi } from '@/api/trade'
import type { Logistics, OrderDetail } from '@/types'
import { useDict } from '@/composables/useDict'
import { formatTime, fullAddress, maskMobile, money } from '@/utils/format'
import { ITEM_AFTERSALE_STATUS, LOGISTICS_STATE, ORDER_STATUS, ORDER_TYPE, PAY_TYPE, toMap } from '@/utils/constants'
import { promptText, toast } from '@/utils/message'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import DeliverDialog from './components/DeliverDialog.vue'

const route = useRoute()
const id = computed(() => route.params.id as string)

const order = ref<OrderDetail | null>(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    order.value = await orderApi.detail(id.value)
    showMobile.value = false
    if (order.value?.deliveryNo) loadLogistics()
    else logistics.value = null
  } finally {
    loading.value = false
  }
}
onMounted(load)

// ---------- 物流轨迹 ----------
const logistics = ref<Logistics | null>(null)
const logisticsLoading = ref(false)
const logisticsError = ref('')
const tracesExpanded = ref(false)
const TRACE_FOLD = 5

async function loadLogistics() {
  if (!order.value) return
  logisticsLoading.value = true
  logisticsError.value = ''
  try {
    logistics.value = await orderApi.logistics(order.value.id)
  } catch (e) {
    logistics.value = null
    logisticsError.value = (e as Error).message || '物流信息暂时无法查询，请稍后再试'
  } finally {
    logisticsLoading.value = false
  }
}

const visibleTraces = computed(() => {
  const t = logistics.value?.traces || []
  return tracesExpanded.value ? t : t.slice(0, TRACE_FOLD)
})

function copyText(text: string) {
  navigator.clipboard?.writeText(text).then(
    () => toast.success('已复制'),
    () => toast.warning('复制失败，请手动选择复制'),
  )
}

// ---------- 买家手机号：默认脱敏，与列表一致 ----------
const showMobile = ref(false)

const statusMap = toMap(ORDER_STATUS)
const payTypeMap = toMap(PAY_TYPE)

// ---------- 步骤条 ----------
const steps = computed(() => {
  const o = order.value
  return [
    { title: '买家下单', time: o?.createdAt },
    { title: '买家付款', time: o?.payTime },
    { title: '商家发货', time: o?.deliveryTime },
    { title: '确认收货', time: o?.receiveTime },
    { title: '交易完成', time: o?.status === 'COMPLETED' ? o?.receiveTime : '' },
  ]
})

const activeStep = computed(() => {
  const o = order.value
  if (!o) return 0
  switch (o.status) {
    case 'PENDING_PAY':
      return 1
    case 'PENDING_DELIVERY':
      return 2
    case 'DELIVERED':
      return 3
    case 'COMPLETED':
      return 5
    case 'CANCELLED':
      // 已取消：停在已到达的阶段
      return o.deliveryTime ? 3 : o.payTime ? 2 : 1
    default:
      return 0
  }
})

// ---------- 金额 ----------
const amountRows = computed(() => {
  const o = order.value
  if (!o) return []
  const rows = [
    { label: '商品总额', value: money(o.goodsAmount) },
    { label: '运费', value: Number(o.freightAmount) ? `+ ${money(o.freightAmount)}` : '包邮' },
  ]
  if (Number(o.discountAmount)) rows.push({ label: '会员 / 活动优惠', value: `- ${money(o.discountAmount)}` })
  if (Number(o.couponAmount)) rows.push({ label: '优惠券', value: `- ${money(o.couponAmount)}` })
  if (Number(o.adjustAmount)) rows.push({ label: '商家改价', value: `${o.adjustAmount > 0 ? '+' : '-'} ${money(Math.abs(o.adjustAmount))}` })
  return rows
})

// ---------- 操作 ----------
const deliverVisible = ref(false)

async function editRemark() {
  if (!order.value) return
  const v = await promptText('商家备注', { initial: order.value.adminRemark, placeholder: '仅商家可见', textarea: true })
  if (v === null) return
  await orderApi.remark(order.value.id, v)
  toast.success('备注已保存')
  load()
}

// 改价：adjustAmount 为订单的调价总额（负数减价），实付 = 原应付 - 原调价 + 新调价
const priceVisible = ref(false)
const priceSaving = ref(false)
const priceForm = reactive({ adjustAmount: 0 })
const basePay = computed(() => (order.value ? Number(order.value.payAmount) - Number(order.value.adjustAmount || 0) : 0))
const newPay = computed(() => Math.round((basePay.value + Number(priceForm.adjustAmount || 0)) * 100) / 100)
/** 改价下限：运费 + 0.01 × 商品件数（每件至少分摊 0.01） */
const itemCount = computed(() => (order.value?.items || []).reduce((n, i) => n + Number(i.quantity || 0), 0))
const minPay = computed(() => Math.round((Number(order.value?.freightAmount || 0) + 0.01 * Math.max(1, itemCount.value)) * 100) / 100)
const priceError = computed(() =>
  newPay.value < minPay.value
    ? `调整后应付不能低于 ${money(minPay.value)}（运费 ${money(order.value?.freightAmount)} + 每件商品至少 ¥0.01 × ${itemCount.value} 件）`
    : '',
)

function openPrice() {
  priceForm.adjustAmount = Number(order.value?.adjustAmount || 0)
  priceVisible.value = true
}

async function savePrice() {
  if (!order.value) return
  if (priceError.value) return toast.warning(priceError.value)
  priceSaving.value = true
  try {
    await orderApi.adjustPrice(order.value.id, priceForm.adjustAmount)
    toast.success('改价成功')
    priceVisible.value = false
    load()
  } finally {
    priceSaving.value = false
  }
}

// 取消
const { options: cancelReasons } = useDict('cancel_reason')
const cancelVisible = ref(false)
const cancelSaving = ref(false)
const cancelFormRef = ref<FormInstance>()
const cancelForm = reactive({ reason: '' })

async function doCancel() {
  if (!order.value) return
  if (!(await cancelFormRef.value?.validate().catch(() => false))) return
  cancelSaving.value = true
  try {
    await orderApi.cancel(order.value.id, cancelForm.reason)
    toast.success('订单已取消')
    cancelVisible.value = false
    load()
  } finally {
    cancelSaving.value = false
  }
}

function logTitle(log: { toStatus?: string; remark?: string }) {
  return (log.toStatus && statusMap[log.toStatus]?.label) || log.remark || '状态变更'
}
</script>

<template>
  <div v-loading="loading" class="page order-detail">
    <PageHeader title="订单详情" back="/trade/order">
      <template #tag>
        <StatusTag v-if="order" :value="order.status" :options="ORDER_STATUS" :label="order.statusText || undefined" />
        <StatusTag v-if="order?.orderType === 'SECKILL'" value="SECKILL" :options="ORDER_TYPE" :dot="false" />
      </template>
      <template #subtitle>
        <span v-if="order">订单号 <span class="mono">{{ order.orderNo }}</span> · 下单于 {{ formatTime(order.createdAt) }}</span>
      </template>
      <template v-if="order" #extra>
        <el-button v-if="order.status === 'PENDING_DELIVERY'" v-perm="'trade:order:deliver'" type="primary" :icon="'Van'" @click="deliverVisible = true">发货</el-button>
        <el-button v-if="order.status === 'PENDING_PAY'" v-perm="'trade:order:edit'" :icon="'PriceTag'" @click="openPrice">改价</el-button>
        <el-button v-perm="'trade:order:edit'" :icon="'EditPen'" @click="editRemark">备注</el-button>
        <el-button v-if="order.status === 'PENDING_PAY'" v-perm="'trade:order:edit'" type="danger" plain :icon="'CircleClose'" @click="cancelVisible = true">取消订单</el-button>
      </template>
    </PageHeader>

    <template v-if="order">
      <div class="card">
        <el-steps :active="activeStep" align-center finish-status="success" :process-status="order.status === 'CANCELLED' ? 'error' : 'process'">
          <el-step v-for="(s, i) in steps" :key="s.title" :title="order.status === 'CANCELLED' && i === activeStep ? '已取消' : s.title">
            <template #description>
              <span v-if="order.status === 'CANCELLED' && i === activeStep">{{ formatTime(order.cancelTime) }}</span>
              <span v-else>{{ s.time ? formatTime(s.time) : '' }}</span>
            </template>
          </el-step>
        </el-steps>
        <el-alert v-if="order.status === 'CANCELLED'" type="info" :closable="false" show-icon class="mt-16" :title="`取消原因：${order.cancelReason || '—'}`" />
        <el-alert
          v-else-if="order.status === 'PENDING_PAY' && order.payExpireAt"
          type="warning"
          :closable="false"
          show-icon
          class="mt-16"
          :title="`等待买家付款，${formatTime(order.payExpireAt)} 前未付款将自动关闭`"
        />
      </div>

      <div class="info-grid">
        <div class="card">
          <h3 class="card-title"><span class="card-title__text">订单信息</span></h3>
          <div class="desc-grid">
            <div class="desc-item"><span class="desc-item__label">订单编号</span><span class="desc-item__value mono">{{ order.orderNo }}</span></div>
            <div class="desc-item"><span class="desc-item__label">订单类型</span><span class="desc-item__value">{{ toMap(ORDER_TYPE)[order.orderType]?.label || order.orderType }}</span></div>
            <div class="desc-item"><span class="desc-item__label">买家</span><span class="desc-item__value">{{ order.member?.nickname || order.memberNickname || '-' }} <span class="text-secondary">{{ maskMobile(order.member?.mobile) }}</span></span></div>
            <div class="desc-item"><span class="desc-item__label">支付方式</span><span class="desc-item__value">{{ order.payType ? payTypeMap[order.payType]?.label || order.payType : '未支付' }}</span></div>
            <div class="desc-item"><span class="desc-item__label">支付时间</span><span class="desc-item__value">{{ formatTime(order.payTime) }}</span></div>
            <div class="desc-item"><span class="desc-item__label">物流</span><span class="desc-item__value">{{ order.deliveryCompany ? `${logistics?.companyName || order.deliveryCompany} ${order.deliveryNo}` : '-' }}</span></div>
            <div class="desc-item"><span class="desc-item__label">买家留言</span><span class="desc-item__value">{{ order.remark || '-' }}</span></div>
            <div class="desc-item">
              <span class="desc-item__label">商家备注</span>
              <span class="desc-item__value text-warning">{{ order.adminRemark || '-' }}</span>
            </div>
          </div>
        </div>
        <div class="card">
          <h3 class="card-title"><span class="card-title__text">收货信息</span></h3>
          <div class="receiver">
            <div class="receiver__icon"><el-icon><Location /></el-icon></div>
            <div>
              <div class="receiver__name">
                {{ order.receiverName }}
                <span class="mono">{{ showMobile ? order.receiverMobile : maskMobile(order.receiverMobile) }}</span>
                <el-button v-if="maskMobile(order.receiverMobile) !== order.receiverMobile" link type="primary" size="small" @click="showMobile = !showMobile">
                  {{ showMobile ? '隐藏' : '显示' }}
                </el-button>
              </div>
              <div class="receiver__addr">{{ fullAddress(order) }}</div>
            </div>
          </div>
        </div>
        <div class="card amount-card">
          <h3 class="card-title"><span class="card-title__text">金额明细</span></h3>
          <div v-for="r in amountRows" :key="r.label" class="amount-row">
            <span class="text-secondary">{{ r.label }}</span><span class="money">{{ r.value }}</span>
          </div>
          <div class="amount-row amount-row--total">
            <span>实付金额</span><span class="money money--danger">{{ money(order.payAmount) }}</span>
          </div>
        </div>
      </div>

      <div v-if="order.deliveryNo" v-loading="logisticsLoading" class="card">
        <h3 class="card-title">
          <span class="card-title__text">物流跟踪</span>
          <el-button link type="primary" :icon="'Refresh'" :loading="logisticsLoading" @click="loadLogistics">刷新</el-button>
        </h3>
        <div class="ship-head">
          <div class="ship-head__icon"><el-icon><Van /></el-icon></div>
          <div class="ship-head__main">
            <div class="ship-head__title">
              {{ logistics?.companyName || order.deliveryCompany }}
              <span class="mono">{{ logistics?.trackingNo || order.deliveryNo }}</span>
              <el-button link type="primary" size="small" :icon="'DocumentCopy'" @click="copyText(logistics?.trackingNo || order.deliveryNo)">复制单号</el-button>
            </div>
            <div class="text-secondary sub">发货时间 {{ formatTime(order.deliveryTime) }}</div>
          </div>
          <StatusTag v-if="logistics?.state" :value="logistics.state" :options="LOGISTICS_STATE" :label="logistics.stateText || undefined" />
        </div>
        <el-timeline v-if="logistics?.traces?.length" class="traces">
          <el-timeline-item
            v-for="(t, i) in visibleTraces"
            :key="i"
            :timestamp="t.time"
            placement="top"
            :type="i === 0 ? 'primary' : undefined"
            :hollow="i !== 0"
            :size="i === 0 ? 'large' : 'normal'"
          >
            <div class="trace" :class="{ 'is-latest': i === 0 }">
              {{ t.context }}<span v-if="t.location" class="text-secondary"> · {{ t.location }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
        <div v-if="(logistics?.traces?.length || 0) > TRACE_FOLD" class="trace-more">
          <el-button link type="primary" @click="tracesExpanded = !tracesExpanded">
            {{ tracesExpanded ? '收起' : `展开全部 ${logistics!.traces.length} 条轨迹` }}
          </el-button>
        </div>
        <div v-else-if="!logisticsLoading && !logistics?.traces?.length" class="trace-empty">
          <el-icon><InfoFilled /></el-icon>
          {{ logistics?.message || logisticsError || '暂无物流轨迹' }}
          <router-link v-if="logistics?.message === '未开通物流查询'" to="/system/config?group=logistics" class="inline-link">去开通</router-link>
        </div>
      </div>

      <div class="card">
        <h3 class="card-title"><span class="card-title__text">商品明细</span></h3>
        <el-table :data="order.items" row-key="id">
          <el-table-column label="商品" min-width="300">
            <template #default="{ row }">
              <div class="goods-cell">
                <el-image :src="row.pic" fit="cover" class="goods-cell__img" :preview-src-list="[row.pic]" preview-teleported />
                <div class="goods-cell__body">
                  <div class="goods-cell__name">{{ row.goodsName }}</div>
                  <div class="goods-cell__sub">{{ row.specText || '默认规格' }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120" align="right"><template #default="{ row }">{{ money(row.price) }}</template></el-table-column>
          <el-table-column label="数量" width="80" align="center"><template #default="{ row }">× {{ row.quantity }}</template></el-table-column>
          <el-table-column label="小计" width="120" align="right"><template #default="{ row }">{{ money(row.totalAmount) }}</template></el-table-column>
          <el-table-column label="分摊实付" width="120" align="right"><template #default="{ row }"><span class="money">{{ money(row.payAmount) }}</span></template></el-table-column>
          <el-table-column label="售后" width="100" align="center">
            <template #default="{ row }">
              <StatusTag v-if="row.aftersaleStatus && row.aftersaleStatus !== 'NONE'" :value="row.aftersaleStatus" :options="ITEM_AFTERSALE_STATUS" />
              <span v-else class="text-secondary">-</span>
            </template>
          </el-table-column>
          <template #empty><EmptyState text="无商品" /></template>
        </el-table>
      </div>

      <div class="card">
        <h3 class="card-title"><span class="card-title__text">操作日志</span></h3>
        <el-timeline v-if="order.logs?.length" class="logs">
          <el-timeline-item v-for="(log, i) in [...order.logs].reverse()" :key="log.id || i" :timestamp="formatTime(log.createdAt)" placement="top" :type="i === 0 ? 'primary' : undefined" :hollow="i !== 0">
            <div class="log">
              <b>{{ logTitle(log) }}</b>
              <span v-if="log.operator" class="text-secondary">· {{ log.operator }}</span>
              <div v-if="log.remark && log.remark !== logTitle(log)" class="log__remark">{{ log.remark }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <EmptyState v-else text="暂无日志" />
      </div>
    </template>

    <DeliverDialog v-model="deliverVisible" :order="order" @success="load" />

    <el-dialog v-model="priceVisible" title="订单改价" width="460px">
      <el-form label-width="100px" @submit.prevent>
        <el-form-item label="原应付金额"><span class="money">{{ money(basePay) }}</span></el-form-item>
        <el-form-item label="调整金额">
          <el-input-number v-model="priceForm.adjustAmount" :precision="2" :step="1" controls-position="right" style="width: 200px" />
          <div class="form-tip">负数为减价，正数为加价</div>
        </el-form-item>
        <el-form-item label="调整后实付">
          <span class="money" :class="priceError ? 'text-danger' : 'money--danger'" style="font-size: 18px">{{ money(newPay) }}</span>
          <div v-if="priceError" class="price-error">{{ priceError }}</div>
          <div v-else class="form-tip" style="width: 100%">最低可改至 {{ money(minPay) }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priceVisible = false">取消</el-button>
        <el-button type="primary" :loading="priceSaving" :disabled="!!priceError" @click="savePrice">确认改价</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="cancelVisible" title="取消订单" width="460px">
      <el-alert type="warning" :closable="false" show-icon title="取消后将回补库存、退还优惠券，且不可恢复" style="margin-bottom: 16px" />
      <el-form ref="cancelFormRef" :model="cancelForm" label-width="80px" @submit.prevent>
        <el-form-item label="取消原因" prop="reason" :rules="[{ required: true, message: '请选择或填写取消原因', trigger: 'change' }]">
          <el-select v-model="cancelForm.reason" filterable allow-create placeholder="请选择或输入" class="w-full">
            <el-option v-for="r in cancelReasons" :key="r.value" :label="r.label" :value="r.label" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelVisible = false">再想想</el-button>
        <el-button type="danger" :loading="cancelSaving" @click="doCancel">确认取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.info-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--app-gap);
}
.receiver {
  display: flex;
  gap: 12px;
  &__icon {
    flex: none;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    color: var(--el-color-primary);
    background: var(--app-primary-soft);
  }
  &__name {
    font-weight: 600;
    color: var(--app-text);
  }
  &__addr {
    margin-top: 6px;
    line-height: 1.6;
    color: var(--app-text-regular);
    font-size: 13px;
  }
}
.amount-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 13px;
  &--total {
    margin-top: 8px;
    padding-top: 12px;
    border-top: 1px dashed var(--app-border-strong);
    font-weight: 600;
    .money {
      font-size: 20px;
    }
  }
}
.logs {
  padding: 4px 0 0 4px;
}
.sub {
  font-size: 12px;
  margin-top: 2px;
}
.price-error {
  width: 100%;
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--app-danger);
}
.ship-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 18px;
  border-radius: var(--app-radius);
  background: var(--app-card-bg-muted);
  &__icon {
    flex: none;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    color: var(--el-color-primary);
    background: var(--app-primary-soft);
  }
  &__main {
    flex: 1;
    min-width: 0;
  }
  &__title {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    font-weight: 600;
    color: var(--app-text);
  }
}
.traces {
  padding: 0 0 0 4px;
}
.trace {
  font-size: 13px;
  line-height: 1.6;
  color: var(--app-text-regular);
  &.is-latest {
    color: var(--app-text);
    font-weight: 600;
  }
}
.trace-more {
  padding-left: 28px;
}
.trace-empty {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 4px;
  font-size: 13px;
  color: var(--app-text-secondary);
}
.inline-link {
  color: var(--el-color-primary);
  margin-left: 4px;
}
.log {
  font-size: 13px;
  &__remark {
    margin-top: 4px;
    color: var(--app-text-secondary);
  }
}

@media (max-width: 1200px) {
  .info-grid {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    .card:first-child {
      grid-column: 1 / -1;
    }
  }
}
@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: minmax(0, 1fr);
  }
  :deep(.el-steps) {
    overflow-x: auto;
  }
}
</style>
