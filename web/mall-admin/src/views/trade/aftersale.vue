<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { aftersaleApi } from '@/api/trade'
import type { Aftersale } from '@/types'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { formatTime, money } from '@/utils/format'
import { AFTERSALE_STATUS, AFTERSALE_TYPE } from '@/utils/constants'
import { confirm, toast } from '@/utils/message'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const canAudit = computed(() => user.hasPerm('trade:aftersale:audit'))

const { query, list, total, page, size, loading, load, search, reset, onPageChange, onSizeChange } = useTable(aftersaleApi.page, {
  aftersaleNo: '',
  orderNo: '',
  type: '',
  status: typeof route.query.status === 'string' ? route.query.status : '',
})

const statusTab = computed({
  get: () => query.status || 'ALL',
  set: (v: string) => {
    query.status = v === 'ALL' ? '' : v
    search()
  },
})

// ---------- 详情抽屉 ----------
const drawer = ref(false)
const detail = ref<Aftersale | null>(null)
const detailLoading = ref(false)
const remark = ref('')
const acting = ref(false)

async function open(row: Aftersale) {
  drawer.value = true
  detail.value = row
  remark.value = ''
  detailLoading.value = true
  try {
    detail.value = await aftersaleApi.detail(row.id)
  } finally {
    detailLoading.value = false
  }
}

const steps = computed(() => {
  const d = detail.value
  if (!d) return []
  const base = [{ title: '买家申请', time: d.createdAt }]
  if (d.type === 'RETURN_REFUND') {
    base.push({ title: '商家审核', time: d.auditTime }, { title: '买家退货', time: '' }, { title: '退款完成', time: d.refundTime })
  } else {
    base.push({ title: '商家审核', time: d.auditTime }, { title: '退款完成', time: d.refundTime })
  }
  return base
})

const activeStep = computed(() => {
  const d = detail.value
  if (!d) return 0
  const isReturn = d.type === 'RETURN_REFUND'
  switch (d.status) {
    case 'APPLYING':
      return 1
    case 'WAIT_RETURN':
      return 2
    case 'RETURNING':
      return 3
    case 'REFUNDED':
      return isReturn ? 4 : 3
    default:
      return d.auditTime ? 2 : 1
  }
})

async function act(kind: 'approve' | 'reject' | 'receive') {
  const d = detail.value
  if (!d) return
  if (kind === 'reject' && !remark.value.trim()) return toast.warning('请填写拒绝原因')
  const tips = {
    approve: d.type === 'REFUND' ? `同意后将直接退款 ${money(d.amount)}，确定吗？` : '同意后买家需寄回商品，确定同意退货吗？',
    reject: '确定拒绝该售后申请吗？',
    receive: `确认已收到退货并退款 ${money(d.amount)} 吗？`,
  }
  if (!(await confirm(tips[kind], '售后处理'))) return
  acting.value = true
  try {
    if (kind === 'approve') await aftersaleApi.approve(d.id, remark.value.trim())
    else if (kind === 'reject') await aftersaleApi.reject(d.id, remark.value.trim())
    else await aftersaleApi.receive(d.id)
    toast.success('处理成功')
    load()
    await open(d)
  } finally {
    acting.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="card">
      <el-form :model="query" inline class="search-bar" @submit.prevent="search">
        <el-form-item label="售后单号">
          <el-input v-model="query.aftersaleNo" placeholder="请输入" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="请输入" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="售后类型">
          <el-select v-model="query.type" placeholder="全部" clearable>
            <el-option v-for="o in AFTERSALE_TYPE" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
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
        <el-tab-pane v-for="s in AFTERSALE_STATUS" :key="s.value" :name="s.value" :label="s.label" />
      </el-tabs>
      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="售后商品" min-width="280">
          <template #default="{ row }: { row: any }">
            <div class="goods-cell">
              <el-image :src="row.item?.pic" fit="cover" class="goods-cell__img" />
              <div class="goods-cell__body">
                <div class="goods-cell__name">{{ row.item?.goodsName || '-' }}</div>
                <div class="goods-cell__sub">{{ row.item?.specText }} × {{ row.quantity }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="售后单号 / 订单号" min-width="200">
          <template #default="{ row }: { row: any }">
            <div class="mono">{{ row.aftersaleNo }}</div>
            <div class="mono text-secondary sub link" @click="router.push(`/trade/order/${row.orderId}`)">{{ row.orderNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="买家" prop="memberNickname" min-width="110" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }: { row: any }"><StatusTag :value="row.type" :options="AFTERSALE_TYPE" :dot="false" /></template>
        </el-table-column>
        <el-table-column label="原因" prop="reason" min-width="140" show-overflow-tooltip />
        <el-table-column label="退款金额" width="120" align="right">
          <template #default="{ row }: { row: any }"><span class="money money--danger">{{ money(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }: { row: any }"><StatusTag :value="row.status" :options="AFTERSALE_STATUS" :label="row.statusText || undefined" /></template>
        </el-table-column>
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }: { row: any }"><span class="text-secondary">{{ formatTime(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button link type="primary" @click="open(row)">{{ canAudit && ['APPLYING', 'RETURNING'].includes(row.status) ? '处理' : '查看' }}</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState text="暂无售后单" /></template>
      </el-table>
      <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <el-drawer v-model="drawer" title="售后详情" size="560px">
      <div v-if="detail" v-loading="detailLoading" class="as-detail">
        <div class="as-head">
          <div>
            <div class="as-head__no mono">{{ detail.aftersaleNo }}</div>
            <div class="text-secondary sub">申请于 {{ formatTime(detail.createdAt) }}</div>
          </div>
          <StatusTag :value="detail.status" :options="AFTERSALE_STATUS" :label="detail.statusText || undefined" />
        </div>

        <el-steps v-if="!['REJECTED', 'CANCELLED'].includes(detail.status)" :active="activeStep" finish-status="success" align-center class="as-steps">
          <el-step v-for="s in steps" :key="s.title" :title="s.title" :description="s.time ? formatTime(s.time, 'MM-DD HH:mm') : ''" />
        </el-steps>
        <el-alert
          v-else
          :type="detail.status === 'REJECTED' ? 'error' : 'info'"
          :closable="false"
          show-icon
          :title="detail.status === 'REJECTED' ? `已拒绝：${detail.auditRemark || '—'}` : '买家已撤销申请'"
        />

        <section class="as-section">
          <h4>售后商品</h4>
          <div class="goods-cell">
            <el-image :src="detail.item?.pic" fit="cover" class="goods-cell__img" />
            <div class="goods-cell__body">
              <div class="goods-cell__name">{{ detail.item?.goodsName }}</div>
              <div class="goods-cell__sub">{{ detail.item?.specText }} · 实付 {{ money(detail.item?.payAmount) }} · 数量 {{ detail.quantity }}</div>
            </div>
          </div>
        </section>

        <section class="as-section">
          <h4>申请信息</h4>
          <div class="desc-grid">
            <div class="desc-item"><span class="desc-item__label">售后类型</span><span class="desc-item__value"><StatusTag :value="detail.type" :options="AFTERSALE_TYPE" :dot="false" /></span></div>
            <div class="desc-item"><span class="desc-item__label">退款金额</span><span class="desc-item__value money money--danger">{{ money(detail.amount) }}</span></div>
            <div class="desc-item"><span class="desc-item__label">买家</span><span class="desc-item__value">{{ detail.memberNickname }}</span></div>
            <div class="desc-item"><span class="desc-item__label">关联订单</span><span class="desc-item__value mono link" @click="router.push(`/trade/order/${detail.orderId}`)">{{ detail.orderNo }}</span></div>
            <div class="desc-item"><span class="desc-item__label">申请原因</span><span class="desc-item__value">{{ detail.reason }}</span></div>
            <div v-if="detail.returnCompany" class="desc-item"><span class="desc-item__label">退货物流</span><span class="desc-item__value">{{ detail.returnCompany }} {{ detail.returnNo }}</span></div>
          </div>
          <div v-if="detail.description" class="as-desc">{{ detail.description }}</div>
          <div v-if="detail.images?.length" class="as-images">
            <el-image
              v-for="(img, i) in detail.images"
              :key="img"
              :src="img"
              fit="cover"
              :preview-src-list="detail.images"
              :initial-index="i"
              preview-teleported
              class="as-images__item"
            />
          </div>
        </section>

        <section v-if="detail.auditRemark && detail.status !== 'REJECTED'" class="as-section">
          <h4>处理备注</h4>
          <div class="as-desc">{{ detail.auditRemark }} <span class="text-secondary">· {{ formatTime(detail.auditTime) }}</span></div>
        </section>

        <section v-if="canAudit && detail.status === 'APPLYING'" class="as-section">
          <h4>处理意见</h4>
          <el-input v-model="remark" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="同意时可选填；拒绝时必填，买家可见" />
        </section>
      </div>
      <template v-if="detail && canAudit && ['APPLYING', 'RETURNING'].includes(detail.status)" #footer>
        <template v-if="detail.status === 'APPLYING'">
          <el-button type="danger" plain :loading="acting" @click="act('reject')">拒绝</el-button>
          <el-button type="primary" :loading="acting" @click="act('approve')">{{ detail.type === 'REFUND' ? '同意并退款' : '同意退货' }}</el-button>
        </template>
        <el-button v-else type="primary" :loading="acting" @click="act('receive')">确认收货并退款</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.status-tabs {
  margin-top: -8px;
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
.as-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.as-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: var(--app-radius);
  background: var(--app-card-bg-muted);
  &__no {
    font-weight: 600;
    color: var(--app-text);
  }
}
.as-steps {
  margin: 4px 0;
}
.as-section {
  h4 {
    margin: 0 0 12px;
    font-size: 14px;
    color: var(--app-text);
  }
  .desc-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
.as-desc {
  margin-top: 12px;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.6;
  border-radius: var(--app-radius);
  background: var(--app-card-bg-muted);
  color: var(--app-text-regular);
}
.as-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
  &__item {
    width: 84px;
    height: 84px;
    border-radius: var(--app-radius);
    border: 1px solid var(--app-border);
    cursor: zoom-in;
  }
}
</style>
