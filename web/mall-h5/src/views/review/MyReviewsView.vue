<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { Review } from '@/types'
import { getMyReviews } from '@/api/review'
import { usePageList } from '@/composables/usePageList'
import NavBar from '@/components/NavBar.vue'
import ReviewItem from '@/components/ReviewItem.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const { list, loading, finished, error, firstLoaded, load, reset } = usePageList<Review>(
  (page, size) => getMyReviews({ page, size }),
  10,
)
onMounted(reset)
</script>

<template>
  <div class="page">
    <NavBar title="我的评价" />
    <EmptyState v-if="firstLoaded && !list.length && !error" description="还没有发表过评价" />
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
      <ReviewItem
        v-for="r in list"
        :key="r.id"
        :review="r"
        show-goods
        class="my-review"
        @goods="(id: string) => router.push({ name: 'goods', params: { id } })"
      />
    </van-list>
  </div>
</template>

<style scoped lang="scss">
.my-review {
  margin: 10px $gap 0;
  border-radius: $radius;
}
</style>
