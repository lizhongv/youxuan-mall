<script setup lang="ts">
import { computed } from 'vue'
import type { GoodsItem } from '@/types'
import GoodsCard from './GoodsCard.vue'

/**
 * 双列瀑布流：按估算高度（标题行数）把商品分到较矮的一列，避免两列高低悬殊。
 * 估算不依赖图片加载完成，追加数据时已有项目不会跳列。
 */
const props = defineProps<{ list: GoodsItem[] }>()

const columns = computed(() => {
  const cols: GoodsItem[][] = [[], []]
  const heights = [0, 0]
  props.list.forEach((g) => {
    const lines = g.name.length > 14 ? 2 : 1
    const h = 100 + lines * 10 + 30
    const i = heights[0] <= heights[1] ? 0 : 1
    cols[i].push(g)
    heights[i] += h
  })
  return cols
})
</script>

<template>
  <div class="waterfall">
    <div v-for="(col, ci) in columns" :key="ci" class="waterfall__col">
      <GoodsCard v-for="g in col" :key="g.id" :goods="g" mode="grid" class="waterfall__item" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.waterfall {
  display: flex;
  gap: 8px;
  padding: 0 $gap;
  &__col {
    flex: 1;
    min-width: 0;
  }
  &__item {
    margin-bottom: 8px;
  }
}
</style>
