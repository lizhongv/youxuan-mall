<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { paymentApi } from '@/api/trade'
import type { Payment } from '@/types'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { formatTime, money } from '@/utils/format'
import { PAY_CHANNEL, PAY_TRADE_TYPE, PAYMENT_STATUS, toMap } from '@/utils/constants'
import { toast } from '@/utils/message'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'

/** 支付流水：对账、排查回调丢失；「查单」向渠道主动查询并同步结果 */
const router = useRouter()
const user = useUserStore()
const canSync = computed(() => user.hasPerm('trade:payment:edit'))
const tradeTypeMap = toMap(PAY_TRADE_TYPE)

const { query, list, total, page, size, loading, load, search, reset, onPageChange, onSizeChange } = useTable(
  paymentApi.page,
  { payNo: '', orderNo: '', channel: '', status: '', range: [] as string[] },
  {
    beforeFetch: ({ range, ...rest }) => {
      const r = range as string[] | undefined
      return { ...rest, startTime: r?.[0], endTime: r?.[1] }
    },
  },
)

const statusTab = computed({
  get: () => query.status || 'ALL',
  set: (v: string) => {
    query.status = v === 'ALL' ? '' : v
    search()
  },
})

const dateShortcuts = [
  { text: '今天', value: () => [dayjs().startOf('day').toDate(), dayjs().endOf('day').toDate()] },
  { text: '近 7 天', value: () => [dayjs().subtract(6, 'day').startOf('day').toDate(), dayjs().endOf('day').toDate()] },
  { text: '近 30 天', value: () => [dayjs().subtract(29, 'day').startOf('day').toDate(), dayjs().endOf('day').toDate()] },
]

/** 只有微信 / 支付宝的异步渠道可以查单 */
const syncable = (row: Payment) => row.channel === 'WECHAT' || row.channel === 'ALIPAY'
const syncing = ref<Record<string, boolean>>({})

async function sync(row: Payment) {
  syncing.value[row.id] = true
  try {
    const latest = await paymentApi.sync(row.id)
    const before = row.status
    if (latest) {
      list.value = list.value.map((r) => (r.id === row.id ? { ...r, ...latest } : r))
      if (detail.value?.id === row.id) detail.value = { ...detail.value, ...latest }
    }
    const after = latest?.status || before
    if (after !== before) toast.success(`查单完成：${latest?.statusText || toMap(PAYMENT_STATUS)[after]?.label || after}`)
    else toast.success('查单完成，状态无变化')
  } finally {
    syncing.value[row.id] = false
  }
}

// ---------- 详情 ----------
const drawer = ref(false)
const detail = ref<Payment | null>(null)
function open(row: Payment) {
  detail.value = row
  drawer.value = true
}

function copy(text: string) {
  navigator.clipboard?.writeText(text).then(
    () => toast.success('已复制'),
    () => toast.warning('复制失败，请手动选择复制'),
  )
}
</script>

<template>
  <div class="page">
    <div class="card">
      <el-form :model="query" inline class="search-bar" @submit.prevent="search">
        <el-form-item label="支付单号">
          <el-input v-model="query.payNo" placeholder="精确匹配" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="精确匹配" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="支付渠道">
          <el-select v-model="query.channel" placeholder="全部渠道" clearable>
            <el-option v-for="o in PAY_CHANNEL" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="发起时间">
          <el-date-picker
            v-model="query.range"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            :default-time="[new Date(2000, 0, 1, 0, 0, 0), new Date(2000, 0, 1, 23, 59, 59)]"
            :shortcuts="dateShortcuts"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="search">搜索</el-button>
          <el-button :icon="'Refresh'" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-tabs v-model="statusTab" class="status-tabs">
        <el-tab-pane name="ALL" label="全部" />
        <el-tab-pane v-for="s in PAYMENT_STATUS" :key="s.value" :name="s.value" :label="s.label" />
      </el-tabs>
      <div class="toolbar">
        <div class="toolbar__left text-secondary">
          <el-icon><InfoFilled /></el-icon>
          微信 / 支付宝流水 5 分钟仍未回调时系统会自动查单；回调丢失或对账时可手动「查单」同步渠道结果。
        </div>
        <div class="toolbar__right">
          <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="load" /></el-tooltip>
        </div>
      </div>
      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="支付单号 / 订单号" min-width="210">
          <template #default="{ row }: { row: any }">
            <div class="mono link" @click="open(row)">{{ row.payNo }}</div>
            <div class="mono text-secondary sub link" @click="router.push(`/trade/order/${row.orderId}`)">{{ row.orderNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="渠道" width="130">
          <template #default="{ row }: { row: any }">
            <StatusTag :value="row.channel" :options="PAY_CHANNEL" :label="row.channelText || undefined" :dot="false" />
            <div v-if="row.tradeType" class="text-secondary sub">{{ tradeTypeMap[row.tradeType]?.label || row.tradeType }}</div>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }: { row: any }"><span class="money">{{ money(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="164">
          <template #default="{ row }: { row: any }">
            <StatusTag :value="row.status" :options="PAYMENT_STATUS" :label="row.statusText || undefined" />
            <el-tooltip v-if="row.remark" :content="row.remark" placement="top">
              <el-icon class="remark-icon"><Warning /></el-icon>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="渠道流水号" min-width="136" show-overflow-tooltip>
          <template #default="{ row }: { row: any }"><span class="mono sub">{{ row.channelNo || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="发起 / 到账时间" width="160">
          <template #default="{ row }: { row: any }">
            <div class="sub">{{ formatTime(row.createdAt) }}</div>
            <div class="text-secondary sub">{{ row.paidAt ? formatTime(row.paidAt) : '未到账' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button link type="primary" @click="open(row)">详情</el-button>
            <el-tooltip v-if="canSync" :disabled="syncable(row)" content="余额 / 模拟支付为同步渠道，无需查单" placement="top">
              <span>
                <el-button link type="primary" :disabled="!syncable(row)" :loading="syncing[row.id]" @click="sync(row)">查单</el-button>
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
        <template #empty><EmptyState v-if="!loading" text="暂无支付流水" /><div v-else class="table-placeholder" /></template>
      </el-table>
      <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <el-drawer v-model="drawer" title="支付流水详情" size="480px">
      <div v-if="detail" class="pay-detail">
        <div class="pay-head">
          <div>
            <div class="text-secondary sub">支付金额</div>
            <div class="pay-head__amount money">{{ money(detail.amount) }}</div>
          </div>
          <StatusTag :value="detail.status" :options="PAYMENT_STATUS" :label="detail.statusText || undefined" />
        </div>
        <el-alert v-if="detail.status === 'REFUNDED_AFTER_CLOSE'" type="warning" :closable="false" show-icon title="到账时订单已关闭或已被其他支付单付清，系统已自动原路退款" />
        <div class="kv">
          <div class="kv__row"><span>支付单号</span><span class="mono">{{ detail.payNo }} <el-button link type="primary" :icon="'DocumentCopy'" @click="copy(detail.payNo)" /></span></div>
          <div class="kv__row"><span>订单号</span><span class="mono link" @click="router.push(`/trade/order/${detail.orderId}`)">{{ detail.orderNo }}</span></div>
          <div class="kv__row"><span>会员 ID</span><span class="mono">{{ detail.memberId || '-' }}</span></div>
          <div class="kv__row"><span>支付渠道</span><span>{{ detail.channelText || toMap(PAY_CHANNEL)[detail.channel]?.label || detail.channel }}<template v-if="detail.tradeType"> · {{ tradeTypeMap[detail.tradeType]?.label || detail.tradeType }}</template></span></div>
          <div class="kv__row">
            <span>渠道流水号</span>
            <span class="mono">{{ detail.channelNo || '-' }} <el-button v-if="detail.channelNo" link type="primary" :icon="'DocumentCopy'" @click="copy(detail.channelNo)" /></span>
          </div>
          <div class="kv__row"><span>发起时间</span><span>{{ formatTime(detail.createdAt) }}</span></div>
          <div class="kv__row"><span>到账时间</span><span>{{ formatTime(detail.paidAt) }}</span></div>
          <div class="kv__row"><span>备注</span><span>{{ detail.remark || '-' }}</span></div>
        </div>
      </div>
      <template v-if="detail && canSync && syncable(detail)" #footer>
        <el-button type="primary" :icon="'Refresh'" :loading="syncing[detail.id]" @click="sync(detail)">向渠道查单</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.status-tabs {
  margin-top: -8px;
}
.toolbar__left {
  font-size: 12px;
  gap: 6px;
}
.sub {
  font-size: 12px;
  margin-top: 2px;
}
.link {
  cursor: pointer;
  &:hover {
    color: var(--el-color-primary);
  }
}
.remark-icon {
  margin-left: 6px;
  vertical-align: -2px;
  color: var(--app-warning);
  cursor: help;
}
.pay-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.pay-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-radius: var(--app-radius);
  background: var(--app-card-bg-muted);
  &__amount {
    margin-top: 4px;
    font-size: 24px;
  }
}
.kv {
  display: flex;
  flex-direction: column;
  &__row {
    display: flex;
    gap: 16px;
    padding: 10px 0;
    font-size: 13px;
    border-bottom: 1px dashed var(--app-border);
    > span:first-child {
      flex: none;
      width: 80px;
      color: var(--app-text-secondary);
    }
    > span:last-child {
      flex: 1;
      min-width: 0;
      color: var(--app-text);
      word-break: break-all;
    }
  }
}
</style>
