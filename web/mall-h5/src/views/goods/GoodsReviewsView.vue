<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { Review, ReviewStat, ReviewType } from '@/types'
import { getGoodsReviews, getReviewStat } from '@/api/public'
import { usePageList } from '@/composables/usePageList'
import { ratePercent } from '@/utils'
import NavBar from '@/components/NavBar.vue'
import ReviewItem from '@/components/ReviewItem.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const id = route.params.id as string
const type = ref<ReviewType>((route.query.type as ReviewType) || 'ALL')
const stat = ref<ReviewStat | null>(null)

const { list, loading, finished, error, firstLoaded, load, reset } = usePageList<Review>(
  (page, size) => getGoodsReviews(id, { page, size, type: type.value }),
  10,
)

onMounted(() => {
  getReviewStat(id)
    .then((s) => (stat.value = s))
    .catch(() => {})
  reset()
})

const tags = computed(() => {
  const s = stat.value
  return [
    { type: 'ALL' as const, label: '全部', count: s?.total },
    { type: 'GOOD' as const, label: '好评', count: s?.good },
    { type: 'MEDIUM' as const, label: '中评', count: s?.medium },
    { type: 'BAD' as const, label: '差评', count: s?.bad },
    { type: 'IMAGE' as const, label: '有图', count: s?.image },
  ]
})

function select(t: ReviewType) {
  if (t === type.value) return
  type.value = t
  reset()
}
</script>

<template>
  <div class="page reviews">
    <NavBar title="商品评价" />
    <div class="reviews__top">
      <div class="reviews__rate">
        好评率 <b>{{ ratePercent(stat?.goodRate) }}%</b>
      </div>
      <div class="reviews__tags">
        <span
          v-for="t in tags"
          :key="t.type"
          class="reviews__tag"
          :class="{ active: type === t.type }"
          @click="select(t.type)"
        >
          {{ t.label }}<template v-if="t.count != null"> {{ t.count }}</template>
        </span>
      </div>
    </div>

    <EmptyState v-if="firstLoaded && !list.length && !error" description="暂无相关评价" />
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
      <ReviewItem v-for="r in list" :key="r.id" :review="r" class="reviews__item" />
    </van-list>
  </div>
</template>

<style scoped lang="scss">
.reviews {
  &__top {
    position: sticky;
    top: calc(46px + env(safe-area-inset-top));
    z-index: 10;
    padding: 12px;
    background: $card;
    border-bottom: 1px solid $border;
  }
  &__rate {
    font-size: 13px;
    color: $text-2;
    b {
      font-size: 18px;
      color: $primary;
    }
  }
  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 10px;
  }
  &__tag {
    padding: 5px 12px;
    font-size: 12px;
    border-radius: 999px;
    background: $primary-light;
    color: $text;
    &.active {
      background: $gradient;
      color: #fff;
    }
  }
  &__item {
    margin-top: 8px;
  }
}
</style>
