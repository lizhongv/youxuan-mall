<script setup lang="ts">
import { showImagePreview } from 'vant'
import type { Review } from '@/types'
import { fmtSpec, fmtTime } from '@/utils'

/** 单条评价：头像昵称、星级、内容、图片九宫格（点击预览）、商家回复 */
withDefaults(defineProps<{ review: Review; showGoods?: boolean }>(), { showGoods: false })
const emit = defineEmits<{ goods: [id: string] }>()
</script>

<template>
  <div class="review">
    <div class="review__head">
      <van-image :src="review.avatar" round width="32" height="32" fit="cover">
        <template #error><van-icon name="user-o" size="18" /></template>
        <template #loading><van-icon name="user-o" size="18" /></template>
      </van-image>
      <div class="review__user">
        <div class="review__name ellipsis">{{ review.anonymous ? '匿名用户' : review.nickname }}</div>
        <van-rate :model-value="review.rating" readonly size="11" gutter="2" />
      </div>
      <span class="review__time">{{ fmtTime(review.createdAt, 'YYYY-MM-DD') }}</span>
    </div>
    <div v-if="review.specText" class="review__spec ellipsis">{{ fmtSpec(review.specText) }}</div>
    <div class="review__content">{{ review.content || '用户未填写评价内容' }}</div>
    <div v-if="review.images?.length" class="review__imgs">
      <van-image
        v-for="(img, i) in review.images"
        :key="img + i"
        :src="img"
        fit="cover"
        radius="6"
        lazy-load
        class="review__img"
        @click="showImagePreview({ images: review.images, startPosition: i })"
      />
    </div>
    <div v-if="review.reply" class="review__reply"><b>商家回复：</b>{{ review.reply }}</div>
    <div v-if="showGoods && review.goodsId" class="review__goods" @click="emit('goods', review.goodsId)">
      <van-image :src="review.goodsPic" width="40" height="40" radius="4" fit="cover" />
      <span class="ellipsis">{{ review.goodsName }}</span>
      <van-icon name="arrow" class="text-3" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.review {
  padding: 12px;
  background: $card;
  &__head {
    display: flex;
    align-items: center;
    gap: 8px;
    :deep(.van-image) {
      flex-shrink: 0;
      background: $bg;
    }
  }
  &__user {
    flex: 1;
    min-width: 0;
  }
  &__name {
    font-size: 13px;
    margin-bottom: 2px;
  }
  &__time {
    flex-shrink: 0;
    font-size: 11px;
    color: $text-3;
  }
  &__spec {
    margin-top: 8px;
    font-size: 12px;
    color: $text-3;
  }
  &__content {
    margin-top: 6px;
    font-size: 14px;
    line-height: 20px;
    word-break: break-all;
    white-space: pre-wrap;
  }
  &__imgs {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 6px;
    margin-top: 8px;
  }
  &__img {
    width: 100%;
    aspect-ratio: 1 / 1;
  }
  &__reply {
    margin-top: 8px;
    padding: 8px 10px;
    font-size: 12px;
    line-height: 18px;
    color: $text-2;
    background: $bg;
    border-radius: 6px;
  }
  &__goods {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 8px;
    padding: 6px;
    background: $bg;
    border-radius: 6px;
    font-size: 12px;
    span {
      flex: 1;
      min-width: 0;
    }
  }
}
</style>
