<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import type { HistoryItem } from '@/types'
import { clearHistory, getHistoryPage } from '@/api/member'
import { usePageList } from '@/composables/usePageList'
import { groupByDate } from '@/utils'
import NavBar from '@/components/NavBar.vue'
import Price from '@/components/Price.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const { list, loading, finished, error, firstLoaded, load, reset } = usePageList<HistoryItem>(
  (page, size) => getHistoryPage({ page, size }),
  20,
)
onMounted(reset)

/** 足迹按最近浏览时间 browsedAt 分组（见 api.md §5） */
const groups = computed(() => groupByDate(list.value, (h) => h.browsedAt || h.updatedAt || h.createdAt))

async function clear() {
  try {
    await showConfirmDialog({ title: '清空足迹', message: '确定清空全部浏览记录吗？' })
    await clearHistory()
    list.value = []
    showToast('已清空')
  } catch {
    /* 取消或已提示 */
  }
}

function open(h: HistoryItem) {
  router.push({ name: 'goods', params: { id: h.goodsId || h.id } })
}
</script>

<template>
  <div class="page history">
    <NavBar title="浏览足迹">
      <template v-if="list.length" #right><span class="history__clear" @click="clear">清空</span></template>
    </NavBar>
    <EmptyState
      v-if="firstLoaded && !list.length && !error"
      description="暂无浏览记录"
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
      <section v-for="g in groups" :key="g.label" class="history__group">
        <div class="history__date">{{ g.label }}</div>
        <div class="history__grid">
          <div v-for="h in g.items" :key="h.id" class="history__item" @click="open(h)">
            <van-image :src="h.pic" fit="cover" radius="6" lazy-load class="history__img" />
            <div class="history__name ellipsis">{{ h.name }}</div>
            <Price :value="h.price" size="xs" />
          </div>
        </div>
      </section>
    </van-list>
  </div>
</template>

<style scoped lang="scss">
.history {
  &__clear {
    color: $text-2;
    font-size: 14px;
  }
  &__group {
    margin: 10px $gap 0;
    padding: 12px;
    background: $card;
    border-radius: $radius;
  }
  &__date {
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 10px;
  }
  &__grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px 8px;
  }
  &__item {
    min-width: 0;
  }
  &__img {
    width: 100%;
    aspect-ratio: 1 / 1;
  }
  &__name {
    margin: 4px 0 2px;
    font-size: 12px;
  }
}
</style>
