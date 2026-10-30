<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { MemberCoupon, MemberCouponStatus } from '@/types'
import { getMyCoupons } from '@/api/coupon'
import NavBar from '@/components/NavBar.vue'
import CouponCard from '@/components/CouponCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const status = ref<MemberCouponStatus>('UNUSED')
const list = ref<MemberCoupon[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    list.value = (await getMyCoupons(status.value)) || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}
onMounted(load)

const STAMP: Record<MemberCouponStatus, string> = { UNUSED: '', USED: '已使用', EXPIRED: '已过期' }
</script>

<template>
  <div class="page mc">
    <NavBar title="我的优惠券">
      <template #right><span class="mc__center" @click="router.push({ name: 'coupon-center' })">领券中心</span></template>
    </NavBar>
    <van-tabs v-model:active="status" sticky offset-top="46" @change="load">
      <van-tab title="未使用" name="UNUSED" />
      <van-tab title="已使用" name="USED" />
      <van-tab title="已过期" name="EXPIRED" />
    </van-tabs>
    <van-loading v-if="loading" class="mc__loading" vertical>加载中...</van-loading>
    <EmptyState
      v-else-if="!list.length"
      description="暂无优惠券"
      :button-text="status === 'UNUSED' ? '去领券' : ''"
      @click="router.push({ name: 'coupon-center' })"
    />
    <div v-else class="mc__list">
      <CouponCard
        v-for="c in list"
        :key="c.id"
        :coupon="c"
        :disabled="status !== 'UNUSED'"
        :stamp="STAMP[status]"
        class="mc__item"
      >
        <van-button v-if="status === 'UNUSED'" size="mini" round class="btn-gradient" @click="router.push('/')">
          去使用
        </van-button>
      </CouponCard>
    </div>
  </div>
</template>

<style scoped lang="scss">
.mc {
  &__center {
    font-size: 14px;
    color: $primary;
  }
  &__loading {
    padding: 60px 0;
  }
  &__list {
    padding: 0 $gap 20px;
  }
  &__item {
    margin-top: 10px;
  }
}
</style>
