<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import type { Order, OrderItem } from '@/types'
import { getOrder } from '@/api/order'
import { createReview } from '@/api/review'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import ImageUploader from '@/components/ImageUploader.vue'

const route = useRoute()
const router = useRouter()
const orderId = route.query.orderId as string
let itemId = route.query.itemId as string

const order = ref<Order | null>(null)
const item = ref<OrderItem | null>(null)
const loading = ref(true)
const submitting = ref(false)

const rating = ref(5)
const content = ref('')
const images = ref<string[]>([])
const anonymous = ref(false)

const RATING_TEXT = ['', '非常差', '差', '一般', '好', '非常好']
const ratingText = computed(() => RATING_TEXT[rating.value] || '')

onMounted(async () => {
  try {
    order.value = await getOrder(orderId)
    item.value = order.value.items.find((i) => i.id === itemId) || null
  } finally {
    loading.value = false
  }
})

async function submit() {
  if (!item.value) return
  if (!rating.value) return showToast('请选择评分')
  if (content.value.trim().length < 5 && !images.value.length) return showToast('说点什么吧，至少 5 个字')
  submitting.value = true
  try {
    await createReview({
      orderItemId: item.value.id,
      rating: rating.value,
      content: content.value.trim(),
      images: images.value,
      anonymous: anonymous.value,
    })
    showSuccessToast('评价成功')
    // 同一订单还有未评价的商品则继续评价下一件
    const next = order.value?.items.find((i) => !i.reviewed && i.id !== item.value!.id)
    if (next) {
      // 同一路由仅查询参数变化不会重建组件，这里手动切换到下一件
      item.value!.reviewed = 1
      itemId = next.id
      item.value = next
      rating.value = 5
      content.value = ''
      images.value = []
      router.replace({ name: 'review-create', query: { orderId, itemId: next.id } })
      window.scrollTo(0, 0)
    } else router.replace({ name: 'order-detail', params: { id: orderId } })
  } catch {
    /* 已提示 */
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page rc">
    <NavBar title="发表评价" />
    <van-loading v-if="loading" class="rc__loading" vertical>加载中...</van-loading>
    <EmptyState v-else-if="!item" description="订单商品不存在" />
    <template v-else>
      <div class="card">
        <div class="rc__goods">
          <van-image :src="item.pic" width="56" height="56" radius="6" fit="cover" />
          <div class="rc__goods-body">
            <div class="ellipsis-2">{{ item.goodsName }}</div>
            <div v-if="item.specText" class="rc__spec ellipsis">{{ item.specText }}</div>
          </div>
        </div>
        <div class="rc__rate">
          <span class="rc__rate-label">商品评分</span>
          <van-rate v-model="rating" size="24" gutter="6" />
          <span class="rc__rate-text">{{ ratingText }}</span>
        </div>
        <van-field
          v-model="content"
          type="textarea"
          rows="5"
          autosize
          maxlength="500"
          show-word-limit
          placeholder="宝贝满足你的期待吗？说说它的优点和美中不足的地方吧"
          class="rc__field"
        />
        <div class="rc__upload">
          <ImageUploader v-model="images" :max-count="9" biz-type="review" />
        </div>
        <div class="rc__anon">
          <span>匿名评价 <small class="text-3">你的评价将以匿名形式展示</small></span>
          <van-switch v-model="anonymous" size="20" />
        </div>
      </div>
      <div class="rc__bar">
        <van-button block round class="btn-gradient" :loading="submitting" loading-text="提交中..." @click="submit">
          发布评价
        </van-button>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.rc {
  &__loading {
    padding: 60px 0;
  }
  &__goods {
    display: flex;
    gap: 10px;
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
  &__rate {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 0;
    border-bottom: 1px solid $border;
  }
  &__rate-label {
    font-size: 14px;
    font-weight: 600;
  }
  &__rate-text {
    font-size: 13px;
    color: $orange;
  }
  &__field {
    padding: 12px 0;
  }
  &__upload {
    padding: 4px 0 12px;
  }
  &__anon {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-top: 12px;
    border-top: 1px solid $border;
    font-size: 14px;
  }
  &__bar {
    padding: 24px $gap calc(24px + env(safe-area-inset-bottom));
  }
}
</style>
