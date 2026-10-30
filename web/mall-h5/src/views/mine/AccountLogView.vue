<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { AccountLog, AccountType } from '@/types'
import { getAccountLogPage } from '@/api/member'
import { useUserStore } from '@/stores/user'
import { usePageList } from '@/composables/usePageList'
import { ACCOUNT_BIZ_TEXT } from '@/utils/dict'
import { fmtTime, money } from '@/utils'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const user = useUserStore()
const type = ref<AccountType>((route.query.type as AccountType) || 'BALANCE')

const { list, loading, finished, error, firstLoaded, load, reset } = usePageList<AccountLog>(
  (page, size) => getAccountLogPage({ page, size, accountType: type.value }),
  20,
)
onMounted(() => {
  if (!user.profile) user.fetchProfile()
  reset()
})

const summary = computed(() =>
  type.value === 'BALANCE'
    ? { label: '账户余额（元）', value: money(user.profile?.balance) }
    : { label: '可用积分', value: String(user.profile?.points ?? 0) },
)

function fmtValue(log: AccountLog) {
  const v = Number(log.changeValue)
  const s = type.value === 'BALANCE' ? money(Math.abs(v)) : String(Math.abs(v))
  return (v >= 0 ? '+' : '-') + s
}
</script>

<template>
  <div class="page alog">
    <NavBar title="账户明细" />
    <div class="alog__head">
      <div class="alog__label">{{ summary.label }}</div>
      <div class="alog__value">{{ summary.value }}</div>
    </div>
    <van-tabs v-model:active="type" sticky offset-top="46" @change="reset">
      <van-tab title="余额明细" name="BALANCE" />
      <van-tab title="积分明细" name="POINTS" />
    </van-tabs>
    <EmptyState v-if="firstLoaded && !list.length && !error" description="暂无明细记录" />
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
      <div v-for="l in list" :key="l.id" class="alog__item">
        <div class="alog__left">
          <div class="alog__biz">{{ l.remark || ACCOUNT_BIZ_TEXT[l.bizType] || l.bizType }}</div>
          <div class="alog__time">{{ fmtTime(l.createdAt, 'YYYY-MM-DD HH:mm:ss') }}</div>
        </div>
        <div class="alog__right">
          <div class="alog__change" :class="{ 'is-plus': Number(l.changeValue) >= 0 }">{{ fmtValue(l) }}</div>
          <div class="alog__after">
            {{ type === 'BALANCE' ? '余额' : '积分' }} {{ type === 'BALANCE' ? money(l.afterValue) : l.afterValue }}
          </div>
        </div>
      </div>
    </van-list>
  </div>
</template>

<style scoped lang="scss">
.alog {
  &__head {
    padding: 24px 20px;
    background: $gradient;
    color: #fff;
  }
  &__label {
    font-size: 13px;
    opacity: 0.9;
  }
  &__value {
    margin-top: 8px;
    font-size: 30px;
    font-weight: 700;
    font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  }
  &__item {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding: 14px $gap;
    background: $card;
    border-bottom: 1px solid $border;
  }
  &__left {
    min-width: 0;
  }
  &__biz {
    font-size: 14px;
    @include ellipsis(1);
  }
  &__time,
  &__after {
    margin-top: 4px;
    font-size: 12px;
    color: $text-3;
  }
  &__right {
    flex-shrink: 0;
    text-align: right;
  }
  &__change {
    font-size: 16px;
    font-weight: 600;
    color: $text;
    &.is-plus {
      color: $primary;
    }
  }
}
</style>
