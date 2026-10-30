<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderApi } from '@/api/trade'
import type { Order, OrderStatusCount } from '@/types'
import { useTable } from '@/composables/useTable'
import { formatTime, money } from '@/utils/format'
import { ORDER_STATUS, ORDER_TYPE } from '@/utils/constants'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'
import DeliverDialog from './components/DeliverDialog.vue'

const route = useRoute()
const router = useRouter()

const initialStatus = typeof route.query.status === 'string' ? route.query.status : ''

const { query, list, total, page, size, loading, load, search, reset, onPageChange, onSizeChange } = useTable(
  orderApi.page,
  { orderNo: '', receiverMobile: '', orderType: '', status: initialStatus, range: [] as string[] },
  {
    // 时间范围拆成 startTime / endTime
    beforeFetch: ({ range, ...rest }) => {
      const r = range as string[] | undefined
      return { ...rest, startTime: r?.[0], endTime: r?.[1] }
    },
  },
)

const counts = ref<OrderStatusCount>({})
async function loadCounts() {
  try {
    counts.value = (await orderApi.statusCount()) || {}
  } catch {
    /* 角标失败不影响列表 */
  }
}
onMounted(loadCounts)

const statusTab = computed({
  get: () => query.status || 'ALL',
  set: (v: string) => {
    query.status = v === 'ALL' ? '' : v
    router.replace({ query: v === 'ALL' ? {} : { status: v } })
    search()
  },
})

function refresh() {
  load()
  loadCounts()
}

function onReset() {
  reset()
  loadCounts()
}

const dateShortcuts = [
  { text: '今天', value: () => [startOf(0), new Date()] },
  { text: '近 7 天', value: () => [startOf(6), new Date()] },
  { text: '近 30 天', value: () => [startOf(29), new Date()] },
]
function startOf(daysAgo: number) {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  d.setDate(d.getDate() - daysAgo)
  return d
}

const deliverVisible = ref(false)
const deliverOrder = ref<Order | null>(null)
function openDeliver(row: Order) {
  deliverOrder.value = row
  deliverVisible.value = true
}

function detail(row: Order) {
  router.push(`/trade/order/${row.id}`)
}

const itemCount = (o: Order) => (o.items || []).reduce((n, i) => n + i.quantity, 0)
</script>

<template>
  <div class="page">
    <div class="card">
      <el-form :model="query" inline class="search-bar" @submit.prevent="search">
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="请输入订单号" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="query.receiverMobile" placeholder="收货人手机号" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="订单类型">
          <el-select v-model="query.orderType" placeholder="全部" clearable>
            <el-option v-for="o in ORDER_TYPE" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="下单时间">
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
          <el-button :icon="'Refresh'" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-tabs v-model="statusTab" class="status-tabs">
        <el-tab-pane name="ALL" label="全部订单" />
        <el-tab-pane v-for="s in ORDER_STATUS" :key="s.value" :name="s.value">
          <template #label>
            <span class="tab-label">
              {{ s.label }}
              <el-badge
                v-if="counts[s.value as keyof OrderStatusCount] && s.value !== 'COMPLETED' && s.value !== 'CANCELLED'"
                :value="counts[s.value as keyof OrderStatusCount]"
                :max="999"
                :type="s.value === 'PENDING_DELIVERY' ? 'danger' : 'warning'"
                class="tab-badge"
              />
              <span v-else-if="counts[s.value as keyof OrderStatusCount]" class="tab-count">{{ counts[s.value as keyof OrderStatusCount] }}</span>
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <div class="toolbar">
        <div class="toolbar__left text-secondary">共 {{ total }} 笔订单</div>
        <div class="toolbar__right">
          <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="refresh" /></el-tooltip>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" row-key="id" class="order-table">
        <el-table-column label="商品" min-width="300">
          <template #default="{ row }: { row: any }">
            <div class="order-goods">
              <div class="order-goods__pics">
                <el-image v-for="it in (row.items || []).slice(0, 3)" :key="it.id" :src="it.pic" fit="cover" class="order-goods__pic" />
              </div>
              <div class="order-goods__info">
                <div class="goods-cell__name">{{ row.items?.[0]?.goodsName || '-' }}</div>
                <div class="goods-cell__sub">
                  <template v-if="(row.items || []).length > 1">等 {{ row.items.length }} 种商品，</template>共 {{ itemCount(row) }} 件
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="订单号 / 下单时间" min-width="200">
          <template #default="{ row }: { row: any }">
            <div class="mono order-no" @click="detail(row)">{{ row.orderNo }}</div>
            <div class="text-secondary sub">{{ formatTime(row.createdAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="买家 / 收货人" min-width="170">
          <template #default="{ row }: { row: any }">
            <div>{{ row.memberNickname || '-' }}</div>
            <div class="text-secondary sub">{{ row.receiverName }} {{ row.receiverMobile }}</div>
          </template>
        </el-table-column>
        <el-table-column label="实付金额" width="130" align="right">
          <template #default="{ row }: { row: any }">
            <div class="money money--danger">{{ money(row.payAmount) }}</div>
            <div class="text-secondary sub">{{ Number(row.freightAmount) ? `含运费 ${money(row.freightAmount)}` : '包邮' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }: { row: any }"><StatusTag :value="row.orderType" :options="ORDER_TYPE" :dot="false" /></template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }: { row: any }"><StatusTag :value="row.status" :options="ORDER_STATUS" :label="row.statusText || undefined" /></template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button link type="primary" @click="detail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING_DELIVERY'" v-perm="'trade:order:deliver'" link type="primary" @click="openDeliver(row)">发货</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState text="暂无订单" /></template>
      </el-table>
      <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <DeliverDialog v-model="deliverVisible" :order="deliverOrder" @success="refresh" />
  </div>
</template>

<style scoped lang="scss">
.status-tabs {
  margin-top: -8px;
  :deep(.el-tabs__header) {
    margin-bottom: 12px;
  }
  :deep(.el-tabs__item) {
    padding-right: 28px;
  }
}
.tab-label {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.tab-badge {
  position: absolute;
  right: -24px;
  top: -10px;
}
.tab-count {
  font-size: 12px;
  color: var(--app-text-secondary);
}
.order-goods {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  &__pics {
    display: flex;
    flex: none;
    .order-goods__pic + .order-goods__pic {
      margin-left: -18px;
    }
  }
  &__pic {
    width: 48px;
    height: 48px;
    border-radius: var(--app-radius-sm);
    border: 2px solid var(--app-card-bg);
    box-shadow: 0 0 0 1px var(--app-border);
    background: var(--app-card-bg-muted);
  }
  &__info {
    min-width: 0;
    line-height: 1.5;
  }
}
.order-no {
  color: var(--el-color-primary);
  cursor: pointer;
  &:hover {
    text-decoration: underline;
  }
}
.sub {
  font-size: 12px;
  margin-top: 2px;
}
</style>
