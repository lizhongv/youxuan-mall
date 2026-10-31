<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import type { MemberCoupon, MemberCouponStatus } from '@/types'
import { getMyCoupons } from '@/api/coupon'
import NavBar from '@/components/NavBar.vue'
import CouponCard from '@/components/CouponCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { errorMessage } from '@/api/request'
import LoadState from '@/components/LoadState.vue'

const router = useRouter()
const status = ref<MemberCouponStatus>('UNUSED')
const list = ref<MemberCoupon[]>([])
const loading = ref(true)
const loadError = ref('')

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    list.value = (await getMyCoupons(status.value)) || []
  } catch (e) {
    list.value = []
    loadError.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}
onMounted(load)

/**
 * 去使用：全场券回首页；类目 / 商品券跳对应商品列表。
 * 契约的「我的优惠券」未返回 scopeIds，存在时按其跳转，否则进入分类 / 全部商品并提示适用范围。
 */
function useCoupon(c: MemberCoupon & { scopeIds?: (string | number)[] }) {
  const ids = (c.scopeIds || []).map(String)
  if (c.scope === 'CATEGORY') {
    if (ids.length === 1) return router.push({ name: 'goods-list', query: { categoryId: ids[0] } })
    showToast('该券仅限指定类目商品使用')
    return router.push({ name: 'category' })
  }
  if (c.scope === 'GOODS') {
    if (ids.length === 1) return router.push({ name: 'goods', params: { id: ids[0] } })
    showToast('该券仅限指定商品使用')
    return router.push({ name: 'goods-list' })
  }
  router.push('/')
}

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
    <LoadState v-else-if="loadError" :error="loadError" @retry="load" />
    <EmptyState
      v-else-if="!list.length && !loadError"
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
        <van-button v-if="status === 'UNUSED'" size="mini" round class="btn-gradient" @click="useCoupon(c)">
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
