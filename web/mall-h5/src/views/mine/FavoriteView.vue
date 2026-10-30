<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import type { GoodsItem } from '@/types'
import { getFavoritePage, removeFavorite } from '@/api/member'
import { usePageList } from '@/composables/usePageList'
import NavBar from '@/components/NavBar.vue'
import GoodsCard from '@/components/GoodsCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const { list, loading, finished, error, firstLoaded, load, reset } = usePageList<GoodsItem>(
  (page, size) => getFavoritePage({ page, size }),
  10,
)
onMounted(reset)

async function remove(g: GoodsItem) {
  try {
    await showConfirmDialog({ message: '确定取消收藏该商品吗？' })
    await removeFavorite(g.id)
    list.value = list.value.filter((i) => i.id !== g.id)
    showToast('已取消收藏')
  } catch {
    /* 取消或已提示 */
  }
}
</script>

<template>
  <div class="page fav">
    <NavBar title="我的收藏" />
    <EmptyState
      v-if="firstLoaded && !list.length && !error"
      description="还没有收藏的商品"
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
      <van-swipe-cell v-for="g in list" :key="g.id" class="fav__cell">
        <GoodsCard :goods="g" mode="list" />
        <template #right>
          <van-button square type="danger" text="取消收藏" class="fav__del" @click="remove(g)" />
        </template>
      </van-swipe-cell>
    </van-list>
  </div>
</template>

<style scoped lang="scss">
.fav__cell {
  margin: 10px $gap 0;
  border-radius: $radius;
  overflow: hidden;
}
.fav__del {
  height: 100%;
}
</style>
