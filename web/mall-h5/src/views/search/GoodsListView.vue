<script setup lang="ts">
import { computed, onActivated, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { GoodsItem, GoodsQuery } from '@/types'
import { getGoodsPage } from '@/api/public'
import { usePageList } from '@/composables/usePageList'
import GoodsCard from '@/components/GoodsCard.vue'
import EmptyState from '@/components/EmptyState.vue'

defineOptions({ name: 'GoodsListView' })

type Sort = NonNullable<GoodsQuery['sort']>

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const categoryId = ref('')
const title = ref('')
const sort = ref<Sort>('DEFAULT')
const layout = ref<'grid' | 'list'>('grid')

const { list, loading, finished, error, firstLoaded, load, reset } = usePageList<GoodsItem>(
  (page, size) =>
    getGoodsPage({
      page,
      size,
      sort: sort.value,
      keyword: keyword.value || undefined,
      categoryId: categoryId.value || undefined,
    }),
  20,
)

/** 页面被缓存：每次进入时比较查询参数，变化则重新加载 */
let lastKey = ''
function syncQuery() {
  const key = route.fullPath
  if (key === lastKey) return
  lastKey = key
  keyword.value = (route.query.keyword as string) || ''
  categoryId.value = (route.query.categoryId as string) || ''
  title.value = (route.query.title as string) || ''
  sort.value = ((route.query.sort as Sort) || 'DEFAULT') as Sort
  window.scrollTo(0, 0)
  reset()
}
onActivated(syncQuery)

const priceSort = computed(() => (sort.value === 'PRICE_ASC' ? 'asc' : sort.value === 'PRICE_DESC' ? 'desc' : ''))

function changeSort(s: 'DEFAULT' | 'SALES' | 'PRICE' | 'NEW') {
  let next: Sort
  if (s === 'PRICE') next = sort.value === 'PRICE_ASC' ? 'PRICE_DESC' : 'PRICE_ASC'
  else next = s
  if (next === sort.value) return
  sort.value = next
  window.scrollTo(0, 0)
  reset()
}

function back() {
  if (window.history.state?.back) router.back()
  else router.replace('/')
}

function toSearch() {
  router.push({ name: 'search', query: keyword.value ? { keyword: keyword.value } : {} })
}
</script>

<template>
  <div class="page glist">
    <div class="glist__top">
      <div class="glist__bar">
        <van-icon name="arrow-left" size="20" class="glist__back" @click="back" />
        <div class="glist__search" @click="toSearch">
          <van-icon name="search" size="16" />
          <span v-if="keyword" class="glist__kw ellipsis">{{ keyword }}</span>
          <span v-else class="ellipsis text-3">{{ title ? `在「${title}」中搜索` : '搜索商品' }}</span>
        </div>
        <van-icon
          :name="layout === 'grid' ? 'bars' : 'apps-o'"
          size="20"
          class="glist__layout"
          @click="layout = layout === 'grid' ? 'list' : 'grid'"
        />
      </div>
      <div class="glist__sorts">
        <span :class="{ active: sort === 'DEFAULT' }" @click="changeSort('DEFAULT')">综合</span>
        <span :class="{ active: sort === 'SALES' }" @click="changeSort('SALES')">销量</span>
        <span :class="{ active: !!priceSort }" class="glist__price" @click="changeSort('PRICE')">
          价格
          <span class="glist__arrows">
            <i class="up" :class="{ on: priceSort === 'asc' }" />
            <i class="down" :class="{ on: priceSort === 'desc' }" />
          </span>
        </span>
        <span :class="{ active: sort === 'NEW' }" @click="changeSort('NEW')">新品</span>
      </div>
    </div>

    <EmptyState
      v-if="firstLoaded && !list.length && !error"
      image="search"
      :description="keyword ? `没有找到「${keyword}」相关商品` : '暂无商品'"
      button-text="去首页逛逛"
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
      <div :class="`glist__${layout}`">
        <GoodsCard v-for="g in list" :key="g.id" :goods="g" :mode="layout" />
      </div>
    </van-list>
  </div>
</template>

<style scoped lang="scss">
.glist {
  &__top {
    position: sticky;
    top: 0;
    z-index: 20;
    background: $card;
    padding-top: env(safe-area-inset-top);
  }
  &__bar {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px $gap 8px 8px;
  }
  &__back,
  &__layout {
    flex-shrink: 0;
    padding: 2px;
    color: $text;
  }
  &__search {
    flex: 1;
    min-width: 0;
    height: 34px;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0 12px;
    background: $bg;
    border-radius: 999px;
    font-size: 13px;
    color: $text-3;
  }
  &__kw {
    padding: 2px 8px;
    border-radius: 999px;
    background: $card;
    color: $text;
  }
  &__sorts {
    display: flex;
    border-bottom: 1px solid $border;
    > span {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      height: 40px;
      font-size: 14px;
      color: $text-2;
      &.active {
        color: $primary;
        font-weight: 600;
      }
    }
  }
  &__arrows {
    display: inline-flex;
    flex-direction: column;
    gap: 2px;
    margin-left: 3px;
    i {
      width: 0;
      height: 0;
      border-left: 4px solid transparent;
      border-right: 4px solid transparent;
    }
    .up {
      border-bottom: 4px solid $disabled;
      &.on {
        border-bottom-color: $primary;
      }
    }
    .down {
      border-top: 4px solid $disabled;
      &.on {
        border-top-color: $primary;
      }
    }
  }
  &__grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
    padding: 8px $gap;
  }
  &__list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 8px $gap;
  }
}
</style>
