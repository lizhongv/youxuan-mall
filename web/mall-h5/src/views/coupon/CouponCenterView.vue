<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { Coupon } from '@/types'
import { getAvailableCoupons } from '@/api/public'
import { useReceiveCoupon } from '@/composables/useCoupon'
import NavBar from '@/components/NavBar.vue'
import CouponCard from '@/components/CouponCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { errorMessage } from '@/api/request'
import LoadState from '@/components/LoadState.vue'

const list = ref<Coupon[]>([])
const loading = ref(true)
const loadError = ref('')
const { receive, canReceive, statusText } = useReceiveCoupon()

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    list.value = (await getAvailableCoupons()) || []
  } catch (e) {
    loadError.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}
onMounted(load)

/** 已领进度（有发行量限制时显示） */
function percent(c: Coupon) {
  return c.total > 0 ? Math.min(100, Math.round((c.issued / c.total) * 100)) : 0
}
</script>

<template>
  <div class="page cc">
    <NavBar title="领券中心" />
    <div class="cc__banner">
      <div class="cc__title">领券中心</div>
      <div class="cc__desc">好券天天领 · 下单更优惠</div>
    </div>
    <van-loading v-if="loading" class="cc__loading" vertical>加载中...</van-loading>
    <LoadState v-else-if="loadError" :error="loadError" @retry="load" />
    <EmptyState v-else-if="!list.length" description="暂无可领取的优惠券" />
    <div v-else class="cc__list">
      <CouponCard v-for="c in list" :key="c.id" :coupon="c" :disabled="!canReceive(c)" class="cc__item">
        <div class="cc__action">
          <span v-if="c.total > 0" class="cc__progress">已抢{{ percent(c) }}%</span>
          <van-button size="mini" round class="btn-gradient" :disabled="!canReceive(c)" @click="receive(c)">
            {{ statusText(c) }}
          </van-button>
        </div>
      </CouponCard>
    </div>
  </div>
</template>

<style scoped lang="scss">
.cc {
  &__banner {
    padding: 20px $gap 36px;
    background: $gradient;
    color: #fff;
  }
  &__title {
    font-size: 22px;
    font-weight: 800;
  }
  &__desc {
    margin-top: 6px;
    font-size: 13px;
    opacity: 0.9;
  }
  &__loading {
    padding: 60px 0;
  }
  &__list {
    margin-top: -24px;
    padding: 0 $gap 20px;
  }
  &__item {
    margin-bottom: 10px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  }
  &__action {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 4px;
  }
  &__progress {
    font-size: 10px;
    color: $primary;
  }
}
</style>
