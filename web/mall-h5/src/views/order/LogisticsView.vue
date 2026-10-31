<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { getLogistics, getOrder } from '@/api/order'
import { useAsync } from '@/composables/useAsync'
import { copyText, fmtTime } from '@/utils'
import NavBar from '@/components/NavBar.vue'
import LoadState from '@/components/LoadState.vue'

/** 物流详情：时间线展示轨迹（最新在上并高亮），无轨迹时展示服务端 message */
const route = useRoute()
const id = route.params.id as string

const { data, loading, error, run } = useAsync(async () => {
  const [logi, order] = await Promise.all([getLogistics(id), getOrder(id).catch(() => null)])
  return { logi, order }
})
run()

const logi = computed(() => data.value?.logi)
const order = computed(() => data.value?.order)
const firstItem = computed(() => order.value?.items?.[0])
const companyName = computed(() => logi.value?.companyName || order.value?.deliveryCompany || '快递')
const trackingNo = computed(() => logi.value?.trackingNo || order.value?.deliveryNo || '')
const traces = computed(() => logi.value?.traces || [])

const STATE_ICON: Record<string, string> = {
  SIGNED: 'passed',
  DELIVERING: 'logistics',
  IN_TRANSIT: 'logistics',
  COLLECTED: 'gift-o',
  PROBLEM: 'warning-o',
  REJECTED: 'warning-o',
  RETURNING: 'revoke',
  RETURN_SIGNED: 'revoke',
}
const stateIcon = computed(() => STATE_ICON[logi.value?.state || ''] || 'logistics')
const stateText = computed(() => logi.value?.stateText || (traces.value.length ? '运输中' : '暂无物流信息'))
</script>

<template>
  <div class="page logi">
    <NavBar title="物流详情" />
    <LoadState :loading="loading" :error="error" :ready="!!data" @retry="run">
      <template v-if="logi">
        <div class="logi__head">
          <van-image v-if="firstItem" :src="firstItem.pic" width="64" height="64" radius="6" fit="cover" />
          <div class="logi__head-body">
            <div class="logi__state">
              <van-icon :name="stateIcon" />
              {{ stateText }}
            </div>
            <div class="logi__company">{{ companyName }}</div>
            <div v-if="trackingNo" class="logi__no">
              运单号 {{ trackingNo }}
              <span class="logi__copy" @click="copyText(trackingNo)">复制</span>
            </div>
          </div>
        </div>

        <div v-if="order" class="card logi__addr">
          <span class="logi__dot logi__dot--addr">收</span>
          <div class="logi__addr-body">
            <div>
              {{ order.receiverProvince }}{{ order.receiverCity }}{{ order.receiverDistrict }} {{ order.receiverDetail }}
            </div>
            <div class="logi__addr-user">{{ order.receiverName }} {{ order.receiverMobile }}</div>
          </div>
        </div>

        <div class="card logi__traces">
          <div v-if="!traces.length" class="logi__empty">
            <van-icon name="info-o" />
            <span>{{ logi.message || '暂无物流轨迹，请稍后再查' }}</span>
          </div>
          <van-steps v-else direction="vertical" :active="0" class="logi__steps">
            <van-step v-for="(t, i) in traces" :key="i + t.time">
              <div class="logi__ctx" :class="{ 'is-latest': i === 0 }">{{ t.context }}</div>
              <div class="logi__time">
                {{ fmtTime(t.time, 'YYYY-MM-DD HH:mm') }}<template v-if="t.location"> · {{ t.location }}</template>
              </div>
            </van-step>
          </van-steps>
        </div>
        <div class="logi__foot">物流信息由快递公司提供，如有疑问请联系快递公司或商城客服</div>
      </template>
    </LoadState>
  </div>
</template>

<style scoped lang="scss">
.logi {
  padding-bottom: calc(20px + env(safe-area-inset-bottom));
  &__head {
    display: flex;
    gap: 12px;
    padding: 16px $gap;
    background: $card;
  }
  &__head-body {
    flex: 1;
    min-width: 0;
  }
  &__state {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 17px;
    font-weight: 700;
    color: $primary;
  }
  &__company {
    margin-top: 6px;
    font-size: 13px;
    color: $text-2;
  }
  &__no {
    margin-top: 4px;
    font-size: 12px;
    color: $text-3;
  }
  &__copy {
    margin-left: 6px;
    padding: 0 6px;
    border: 1px solid $border-strong;
    border-radius: 999px;
    color: $text-2;
  }
  &__addr {
    display: flex;
    gap: 10px;
    font-size: 13px;
    line-height: 19px;
  }
  &__dot {
    flex-shrink: 0;
    width: 22px;
    height: 22px;
    line-height: 22px;
    text-align: center;
    border-radius: 50%;
    font-size: 11px;
    color: #fff;
    background: $text-3;
  }
  &__addr-body {
    flex: 1;
    min-width: 0;
  }
  &__addr-user {
    margin-top: 2px;
    color: $text-3;
  }
  &__traces {
    padding: 4px 4px 4px 0;
  }
  &__empty {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 20px 12px;
    font-size: 13px;
    color: $text-3;
  }
  &__steps {
    :deep(.van-step__title) {
      color: $text-3;
    }
    :deep(.van-step--process .van-step__title) {
      color: $text;
    }
  }
  &__ctx {
    font-size: 13px;
    line-height: 19px;
    &.is-latest {
      font-weight: 600;
      color: $primary;
    }
  }
  &__time {
    margin-top: 4px;
    font-size: 12px;
    color: $text-3;
  }
  &__foot {
    padding: 14px $gap;
    text-align: center;
    font-size: 11px;
    color: $text-3;
  }
}
</style>
