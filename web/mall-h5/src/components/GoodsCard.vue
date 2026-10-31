<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { GoodsItem } from '@/types'
import { fmtSales, num } from '@/utils'
import Price from './Price.vue'

/** 商品卡片：grid 双列卡片 / list 单列横排 */
const props = withDefaults(defineProps<{ goods: GoodsItem; mode?: 'grid' | 'list' }>(), { mode: 'grid' })
const router = useRouter()

function open() {
  router.push({ name: 'goods', params: { id: props.goods.id } })
}
</script>

<template>
  <div class="goods-card" :class="`goods-card--${mode}`" @click="open">
    <div class="goods-card__img">
      <van-image :src="goods.pic" fit="cover" lazy-load width="100%" height="100%" />
    </div>
    <div class="goods-card__body">
      <div class="goods-card__name">
        <span v-if="goods.isHot" class="tag tag--hot">热卖</span>
        <span v-else-if="goods.isNew" class="tag tag--new">新品</span>
        {{ goods.name }}
      </div>
      <div v-if="mode === 'list' && goods.subtitle" class="goods-card__sub ellipsis">{{ goods.subtitle }}</div>
      <div class="goods-card__foot">
        <div class="goods-card__price">
          <Price :value="goods.price" size="sm" />
          <Price v-if="num(goods.originalPrice) > num(goods.price)" :value="goods.originalPrice" strike class="goods-card__origin" />
        </div>
        <span class="goods-card__sales">已售{{ fmtSales(goods.sales) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.goods-card {
  background: $card;
  border-radius: $radius;
  overflow: hidden;
  &__img {
    position: relative;
    background: $bg;
    overflow: hidden;
  }
  &__name {
    font-size: 13px;
    line-height: 18px;
    color: $text;
    @include ellipsis(2);
  }
  &__sub {
    margin-top: 4px;
    font-size: 12px;
    color: $text-3;
  }
  &__foot {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 4px;
    margin-top: 6px;
  }
  /*
   * 价格区固定一行高、允许换行并裁掉第二行：放不下时划线价整体折到隐藏行，
   * 不会出现「¥1499.0(」这种半截价格；主价格始终完整。
   */
  &__price {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    column-gap: 4px;
    min-width: 0;
    height: 18px;
    overflow: hidden;
  }
  &__origin {
    line-height: 18px;
    white-space: nowrap;
  }
  &__sales {
    flex-shrink: 0;
    font-size: 11px;
    color: $text-3;
  }

  &--grid {
    .goods-card__img {
      width: 100%;
      aspect-ratio: 1 / 1;
    }
    .goods-card__body {
      padding: 8px;
    }
  }

  &--list {
    display: flex;
    padding: 10px;
    gap: 10px;
    .goods-card__img {
      flex-shrink: 0;
      width: 110px;
      height: 110px;
      border-radius: $radius-sm;
    }
    .goods-card__body {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
    }
    .goods-card__foot {
      margin-top: auto;
    }
  }
}
.tag {
  display: inline-block;
  padding: 0 4px;
  margin-right: 2px;
  border-radius: 3px;
  font-size: 10px;
  line-height: 15px;
  color: #fff;
  vertical-align: 1px;
  &--hot {
    background: $gradient;
  }
  &--new {
    background: $success;
  }
}
</style>
