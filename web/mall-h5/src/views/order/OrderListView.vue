<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { Order, OrderQueryStatus } from '@/types'
import { getOrderPage } from '@/api/order'
import { usePageList } from '@/composables/usePageList'
import { useOrderActions, type OrderAction } from '@/composables/useOrderActions'
import NavBar from '@/components/NavBar.vue'
import OrderCard from '@/components/OrderCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import CancelSheet from '@/components/CancelSheet.vue'

const route = useRoute()
const router = useRouter()

const TABS: { name: string; title: string }[] = [
  { name: 'ALL', title: '全部' },
  { name: 'PENDING_PAY', title: '待付款' },
  { name: 'PENDING_DELIVERY', title: '待发货' },
  { name: 'DELIVERED', title: '待收货' },
  { name: 'TO_REVIEW', title: '待评价' },
]

const status = ref<string>((route.query.status as string) || 'ALL')

const { list, loading, finished, error, refreshing, firstLoaded, load, reset, refresh } = usePageList<Order>(
  (page, size) => getOrderPage({ page, size, status: (status.value === 'ALL' ? '' : status.value) as OrderQueryStatus }),
  10,
)
onMounted(reset)

function onTab(name: string) {
  router.replace({ query: name !== 'ALL' ? { status: name } : {} })
  reset()
}

const cancelShow = ref(false)
let cancelTarget: Order | null = null
const { handle, doCancel } = useOrderActions((type, order) => {
  if (type === 'delete') list.value = list.value.filter((o) => o.id !== order.id)
  else refresh()
})

function onAction(type: OrderAction, order: Order) {
  handle(type, order, (o) => {
    cancelTarget = o
    cancelShow.value = true
  })
}

function onCancelReason(reason: string) {
  if (cancelTarget) doCancel(cancelTarget, reason)
}
</script>

<template>
  <div class="page orders">
    <NavBar title="我的订单" />
    <van-tabs
      v-model:active="status"
      sticky
      offset-top="46"
      swipeable
      line-width="20"
      class="orders__tabs"
      @change="onTab"
    >
      <van-tab v-for="t in TABS" :key="t.name" :title="t.title" :name="t.name" />
    </van-tabs>

    <van-pull-refresh v-model="refreshing" @refresh="refresh">
      <EmptyState
        v-if="firstLoaded && !list.length && !error"
        description="暂无相关订单"
        button-text="去逛逛"
        @click="router.push('/')"
      />
      <van-list
        v-else
        v-model:loading="loading"
        v-model:error="error"
        :finished="finished"
        :immediate-check="false"
        finished-text="没有更多了"
        error-text="加载失败，点击重试"
        @load="load"
      >
        <OrderCard v-for="o in list" :key="o.id" :order="o" @action="onAction" @expired="refresh" />
      </van-list>
    </van-pull-refresh>

    <CancelSheet v-model:show="cancelShow" @select="onCancelReason" />
  </div>
</template>

<style scoped lang="scss">
.orders {
  padding-bottom: 20px;
  :deep(.van-pull-refresh) {
    min-height: calc(100vh - 90px);
  }
}
</style>
