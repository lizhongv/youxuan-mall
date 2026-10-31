<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { Aftersale } from '@/types'
import { getAftersalePage } from '@/api/aftersale'
import { usePageList } from '@/composables/usePageList'
import { AFTERSALE_STATUS_TEXT, AFTERSALE_TYPE_TEXT } from '@/utils/dict'
import { fmtSpec, fmtTime, money } from '@/utils'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const { list, loading, finished, error, refreshing, firstLoaded, load, reset, refresh } = usePageList<Aftersale>(
  (page, size) => getAftersalePage({ page, size }),
  10,
)
onMounted(reset)
</script>

<template>
  <div class="page as-list">
    <NavBar title="售后服务" />
    <van-pull-refresh v-model="refreshing" @refresh="refresh">
      <EmptyState v-if="firstLoaded && !list.length && !error" description="暂无售后记录" />
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
        <div
          v-for="a in list"
          :key="a.id"
          class="as-card"
          @click="router.push({ name: 'aftersale-detail', params: { id: a.id } })"
        >
          <div class="as-card__head">
            <span class="as-card__type">
              <van-icon :name="a.type === 'REFUND' ? 'balance-o' : 'exchange'" />
              {{ AFTERSALE_TYPE_TEXT[a.type] || a.type }}
            </span>
            <span class="as-card__status" :class="`is-${a.status}`">{{ a.statusText || AFTERSALE_STATUS_TEXT[a.status] }}</span>
          </div>
          <div v-if="a.item" class="as-card__goods">
            <van-image :src="a.item.pic" width="64" height="64" radius="6" fit="cover" lazy-load />
            <div class="as-card__goods-body">
              <div class="ellipsis-2">{{ a.item.goodsName }}</div>
              <div class="as-card__spec ellipsis">{{ fmtSpec(a.item.specText) }}</div>
            </div>
          </div>
          <div class="as-card__foot">
            <span>{{ fmtTime(a.createdAt) }}</span>
            <span>退款金额 <b class="text-primary">¥{{ money(a.amount) }}</b></span>
          </div>
        </div>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<style scoped lang="scss">
.as-list {
  padding-bottom: 20px;
  :deep(.van-pull-refresh) {
    min-height: calc(100vh - 46px);
  }
}
.as-card {
  @include card;
  margin: 10px $gap 0;
  padding: 12px;
  &__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;
  }
  &__type {
    display: flex;
    align-items: center;
    gap: 4px;
    font-weight: 600;
  }
  &__status {
    color: $primary;
    &.is-REFUNDED {
      color: $success;
    }
    &.is-REJECTED,
    &.is-CANCELLED {
      color: $text-3;
    }
  }
  &__goods {
    display: flex;
    gap: 10px;
    margin-top: 10px;
    font-size: 13px;
    line-height: 18px;
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
  &__foot {
    display: flex;
    justify-content: space-between;
    margin-top: 10px;
    padding-top: 10px;
    border-top: 1px solid $border;
    font-size: 12px;
    color: $text-3;
    b {
      font-size: 14px;
    }
  }
}
</style>
