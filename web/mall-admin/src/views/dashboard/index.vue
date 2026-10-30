<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { statsApi } from '@/api/stats'
import type { OrderStatusStat, Overview, TopGoods, TrendPoint } from '@/types'
import { useUserStore } from '@/stores/user'
import { useEcharts } from '@/composables/useEcharts'
import { compactNumber, cssVar, money, percentChange } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const user = useUserStore()

const overview = ref<Overview | null>(null)
const trend = shallowRef<TrendPoint[]>([])
const topGoods = shallowRef<TopGoods[]>([])
const orderStatus = shallowRef<OrderStatusStat[]>([])
const days = ref<7 | 30>(7)
const loading = ref({ overview: true, trend: true, top: true, status: true })

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})
const today = dayjs().format('YYYY年M月D日 dddd')

interface Kpi {
  key: string
  label: string
  value: string
  icon: string
  tone: string
  change?: number | null
  sub?: string
  to?: string
}

const kpis = computed<Kpi[]>(() => {
  const o = overview.value
  if (!o) return []
  return [
    { key: 'sales', label: '今日销售额', value: money(o.todaySales), icon: 'Wallet', tone: 'primary', change: percentChange(o.todaySales, o.yesterdaySales), sub: `昨日 ${money(o.yesterdaySales)}` },
    { key: 'orders', label: '今日订单', value: compactNumber(o.todayOrders), icon: 'Tickets', tone: 'purple', change: percentChange(o.todayOrders, o.yesterdayOrders), sub: `昨日 ${o.yesterdayOrders} 单` },
    { key: 'members', label: '今日新会员', value: compactNumber(o.todayMembers), icon: 'UserFilled', tone: 'success', sub: `累计 ${compactNumber(o.totalMembers)} · 在售商品 ${o.onSaleGoods}` },
    { key: 'pay', label: '待付款', value: String(o.pendingPay), icon: 'Clock', tone: 'warning', sub: '超时将自动关闭', to: '/trade/order?status=PENDING_PAY' },
    { key: 'delivery', label: '待发货', value: String(o.pendingDelivery), icon: 'Van', tone: 'cyan', sub: '请尽快安排发货', to: '/trade/order?status=PENDING_DELIVERY' },
    { key: 'aftersale', label: '待处理售后', value: String(o.pendingAftersale), icon: 'RefreshLeft', tone: 'danger', sub: '及时处理提升满意度', to: '/trade/aftersale?status=APPLYING' },
  ]
})

function goKpi(k: Kpi) {
  if (k.to) router.push(k.to)
}

// ---------------- 图表 ----------------
const trendEl = ref<HTMLElement>()
const statusEl = ref<HTMLElement>()

function palette() {
  return [1, 2, 3, 4, 5, 6].map((i) => cssVar(`--app-chart-${i}`))
}

interface AxisParam {
  axisValue: string
  marker: string
  seriesName: string
  value: number
  seriesIndex: number
}

const { render: renderTrend } = useEcharts(trendEl, () => {
  const [c1, c2] = palette()
  const axis = cssVar('--app-chart-axis')
  const grid = cssVar('--app-chart-grid')
  const text = cssVar('--app-text')
  const bg = cssVar('--el-bg-color-overlay')
  const data = trend.value
  return {
    color: [c1, c2],
    grid: { left: 8, right: 8, top: 48, bottom: 8, containLabel: true },
    tooltip: {
      trigger: 'axis',
      backgroundColor: bg,
      borderColor: grid,
      textStyle: { color: text },
      axisPointer: { type: 'shadow', shadowStyle: { color: cssVar('--app-chart-pointer') } },
      formatter: (params: AxisParam[]) =>
        `<div style="font-weight:600;margin-bottom:4px">${params[0]?.axisValue}</div>` +
        params.map((p) => `${p.marker}${p.seriesName}　<b>${p.seriesIndex === 0 ? money(p.value) : p.value + ' 单'}</b>`).join('<br/>'),
    },
    legend: { top: 0, right: 0, icon: 'roundRect', itemWidth: 12, itemHeight: 8, textStyle: { color: axis } },
    xAxis: {
      type: 'category',
      data: data.map((d) => dayjs(d.date).format('MM-DD')),
      axisLine: { lineStyle: { color: grid } },
      axisTick: { show: false },
      axisLabel: { color: axis },
    },
    yAxis: [
      {
        type: 'value',
        name: '销售额',
        nameTextStyle: { color: axis, align: 'left' },
        splitLine: { lineStyle: { color: grid, type: 'dashed' } },
        axisLabel: { color: axis, formatter: (v: number) => compactNumber(v) },
      },
      { type: 'value', name: '订单数', nameTextStyle: { color: axis, align: 'right' }, splitLine: { show: false }, axisLabel: { color: axis }, minInterval: 1 },
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: data.length <= 10,
        lineStyle: { width: 3 },
        z: 3,
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: c1 + '40' },
              { offset: 1, color: c1 + '00' },
            ],
          },
        },
        data: data.map((d) => d.sales),
      },
      {
        name: '订单数',
        type: 'bar',
        yAxisIndex: 1,
        barMaxWidth: 18,
        itemStyle: { borderRadius: [4, 4, 0, 0], color: c2, opacity: 0.75 },
        data: data.map((d) => d.orders),
      },
    ],
  }
})

const statusTotal = computed(() => orderStatus.value.reduce((s, x) => s + (x.count || 0), 0))

const { render: renderStatus } = useEcharts(statusEl, () => {
  const text = cssVar('--app-text')
  const axis = cssVar('--app-chart-axis')
  const card = cssVar('--app-card-bg')
  const bg = cssVar('--el-bg-color-overlay')
  return {
    color: palette(),
    tooltip: { trigger: 'item', backgroundColor: bg, borderColor: cssVar('--app-chart-grid'), textStyle: { color: text }, formatter: '{b}：{c} 单（{d}%）' },
    legend: { bottom: 0, left: 'center', icon: 'circle', itemWidth: 8, itemHeight: 8, itemGap: 14, textStyle: { color: axis } },
    title: {
      text: String(statusTotal.value),
      subtext: '订单总数',
      left: 'center',
      top: '33%',
      textStyle: { color: text, fontSize: 24, fontWeight: 700 },
      subtextStyle: { color: axis, fontSize: 12 },
      itemGap: 4,
    },
    series: [
      {
        type: 'pie',
        radius: ['52%', '72%'],
        center: ['50%', '42%'],
        avoidLabelOverlap: true,
        padAngle: 2,
        itemStyle: { borderRadius: 6, borderColor: card, borderWidth: 2 },
        label: { show: false },
        emphasis: { scale: true, scaleSize: 6 },
        data: orderStatus.value.map((s) => ({ name: s.statusText || s.status, value: s.count })),
      },
    ],
  }
})

async function loadOverview() {
  loading.value.overview = true
  try {
    overview.value = await statsApi.overview()
  } finally {
    loading.value.overview = false
  }
}

async function loadTrend() {
  loading.value.trend = true
  try {
    trend.value = (await statsApi.trend(days.value)) || []
    renderTrend()
  } finally {
    loading.value.trend = false
  }
}

async function loadTop() {
  loading.value.top = true
  try {
    topGoods.value = (await statsApi.topGoods(30, 10)) || []
  } finally {
    loading.value.top = false
  }
}

async function loadStatus() {
  loading.value.status = true
  try {
    orderStatus.value = (await statsApi.orderStatus()) || []
    renderStatus()
  } finally {
    loading.value.status = false
  }
}

function refreshAll() {
  loadOverview()
  loadTrend()
  loadTop()
  loadStatus()
}

onMounted(refreshAll)

const maxTop = computed(() => Math.max(1, ...topGoods.value.map((g) => g.amount || 0)))
</script>

<template>
  <div class="page dashboard">
    <section class="welcome card">
      <div>
        <h2>{{ greeting }}，{{ user.displayName }}</h2>
        <p>{{ today }} · 这是店铺今天的经营概况</p>
      </div>
      <el-button :icon="'Refresh'" round @click="refreshAll">刷新数据</el-button>
    </section>

    <section v-loading="loading.overview" class="kpis">
      <template v-if="kpis.length">
        <div v-for="k in kpis" :key="k.key" class="kpi card" :class="[`tone-${k.tone}`, { 'is-link': !!k.to }]" @click="goKpi(k)">
          <div class="kpi__head">
            <span class="kpi__label">{{ k.label }}</span>
            <span class="kpi__icon"><el-icon><component :is="k.icon" /></el-icon></span>
          </div>
          <div class="kpi__value">{{ k.value }}</div>
          <div class="kpi__foot">
            <template v-if="k.change !== undefined">
              <span v-if="k.change === null" class="kpi__trend is-flat">昨日无数据</span>
              <span v-else class="kpi__trend" :class="k.change > 0 ? 'is-up' : k.change < 0 ? 'is-down' : 'is-flat'">
                <el-icon><CaretTop v-if="k.change > 0" /><CaretBottom v-else-if="k.change < 0" /><SemiSelect v-else /></el-icon>
                {{ Math.abs(k.change).toFixed(1) }}%
              </span>
              <span class="kpi__sub">较{{ k.sub }}</span>
            </template>
            <span v-else class="kpi__sub">{{ k.sub }}</span>
            <el-icon v-if="k.to" class="kpi__go"><ArrowRight /></el-icon>
          </div>
        </div>
      </template>
      <div v-else-if="!loading.overview" class="card kpis__empty"><EmptyState text="暂无概览数据" /></div>
    </section>

    <section class="charts">
      <div class="card">
        <div class="card-title">
          <span class="card-title__text">销售趋势</span>
          <el-radio-group v-model="days" size="small" @change="loadTrend">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
          </el-radio-group>
        </div>
        <div v-loading="loading.trend" class="chart-box">
          <div ref="trendEl" class="chart" />
          <EmptyState v-if="!loading.trend && !trend.length" class="chart-empty" text="暂无趋势数据" />
        </div>
      </div>
      <div class="card">
        <div class="card-title"><span class="card-title__text">订单状态分布</span></div>
        <div v-loading="loading.status" class="chart-box">
          <div ref="statusEl" class="chart" />
          <EmptyState v-if="!loading.status && !orderStatus.length" class="chart-empty" text="暂无订单" />
        </div>
      </div>
    </section>

    <section class="card">
      <div class="card-title">
        <span class="card-title__text">热销商品 Top 10 <small class="text-secondary">近 30 天</small></span>
        <el-button v-perm="'goods:goods:view'" link type="primary" @click="router.push('/goods/list')">全部商品</el-button>
      </div>
      <el-table v-loading="loading.top" :data="topGoods" row-key="goodsId">
        <el-table-column label="排名" width="70" align="center">
          <template #default="{ $index }">
            <span class="rank" :class="`rank--${$index < 3 ? $index + 1 : 'n'}`">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div class="goods-cell">
              <el-image :src="row.pic" fit="cover" class="goods-cell__img" :preview-src-list="[row.pic]" preview-teleported />
              <div class="goods-cell__body"><div class="goods-cell__name">{{ row.goodsName }}</div></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="销量" prop="quantity" width="110" align="right" />
        <el-table-column label="销售额" min-width="220">
          <template #default="{ row }">
            <div class="amount-bar">
              <span class="money">{{ money(row.amount) }}</span>
              <el-progress :percentage="Math.round((row.amount / maxTop) * 100)" :show-text="false" :stroke-width="6" />
            </div>
          </template>
        </el-table-column>
        <template #empty><EmptyState text="近 30 天暂无销售" /></template>
      </el-table>
    </section>
  </div>
</template>

<style scoped lang="scss">
.welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: var(--app-gradient);
  color: var(--el-color-white);
  h2 {
    margin: 0 0 6px;
    font-size: 20px;
  }
  p {
    margin: 0;
    opacity: 0.88;
    font-size: 13px;
  }
  .el-button {
    flex: none;
  }
}

.kpis {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: var(--app-gap);
  min-height: 132px;
  &__empty {
    grid-column: 1 / -1;
  }
}

.kpi {
  --tone: var(--el-color-primary);
  --tone-soft: var(--app-primary-soft);
  position: relative;
  overflow: hidden;
  padding: 18px;
  transition:
    transform 0.2s,
    box-shadow 0.2s;

  &.tone-purple {
    --tone: var(--app-purple);
    --tone-soft: var(--app-purple-soft);
  }
  &.tone-success {
    --tone: var(--app-success);
    --tone-soft: var(--app-success-soft);
  }
  &.tone-warning {
    --tone: var(--app-warning);
    --tone-soft: var(--app-warning-soft);
  }
  &.tone-cyan {
    --tone: var(--app-cyan);
    --tone-soft: var(--app-cyan-soft);
  }
  &.tone-danger {
    --tone: var(--app-danger);
    --tone-soft: var(--app-danger-soft);
  }
  &.is-link {
    cursor: pointer;
    &:hover {
      transform: translateY(-2px);
      box-shadow: var(--app-shadow-hover);
      .kpi__go {
        transform: translateX(3px);
        color: var(--tone);
      }
    }
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  &__label {
    color: var(--app-text-secondary);
    font-size: 13px;
  }
  &__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: 10px;
    font-size: 18px;
    color: var(--tone);
    background: var(--tone-soft);
  }
  &__value {
    margin: 8px 0 10px;
    font-size: 26px;
    font-weight: 700;
    color: var(--app-text);
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  &__foot {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    min-width: 0;
  }
  &__trend {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    font-weight: 600;
    &.is-up {
      color: var(--app-success);
    }
    &.is-down {
      color: var(--app-danger);
    }
    &.is-flat {
      color: var(--app-text-secondary);
    }
  }
  &__sub {
    color: var(--app-text-secondary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  &__go {
    margin-left: auto;
    color: var(--app-text-placeholder);
    transition: transform 0.2s;
  }
}

.charts {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: var(--app-gap);
}
.chart-box {
  position: relative;
}
.chart {
  height: 320px;
}
.chart-empty {
  position: absolute;
  inset: 0;
  justify-content: center;
}

.rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  color: var(--app-text-secondary);
  background: var(--app-card-bg-muted);
  &--1 {
    color: var(--el-color-white);
    background: var(--app-danger);
  }
  &--2 {
    color: var(--el-color-white);
    background: var(--app-warning);
  }
  &--3 {
    color: var(--el-color-white);
    background: var(--el-color-primary);
  }
}
.amount-bar {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

@media (max-width: 1400px) {
  .kpis {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 1100px) {
  .charts {
    grid-template-columns: minmax(0, 1fr);
  }
}
@media (max-width: 640px) {
  .kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .kpi__value {
    font-size: 20px;
  }
  .welcome {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
