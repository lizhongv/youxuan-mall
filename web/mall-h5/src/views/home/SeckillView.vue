<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { Seckill } from '@/types'
import { getSeckillList } from '@/api/public'
import NavBar from '@/components/NavBar.vue'
import Price from '@/components/Price.vue'
import CountDown from '@/components/CountDown.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const list = ref<Seckill[]>([])
const loading = ref(true)
const tab = ref<'ONGOING' | 'NOT_STARTED'>('ONGOING')

async function load() {
  loading.value = true
  try {
    list.value = (await getSeckillList()) || []
  } finally {
    loading.value = false
  }
}
onMounted(load)

const filtered = computed(() => list.value.filter((s) => s.state === tab.value))
const percent = (s: Seckill) => (s.stock ? Math.min(100, Math.round((s.sold / s.stock) * 100)) : 100)
</script>

<template>
  <div class="page seckill">
    <NavBar title="限时秒杀" />
    <div class="seckill__banner">
      <div class="seckill__title">限时秒杀</div>
      <div class="seckill__desc">好物低价 · 限量抢购</div>
    </div>
    <van-tabs v-model:active="tab" sticky offset-top="46" class="seckill__tabs">
      <van-tab title="正在疯抢" name="ONGOING" />
      <van-tab title="即将开抢" name="NOT_STARTED" />
    </van-tabs>

    <van-loading v-if="loading" class="seckill__loading" vertical>加载中...</van-loading>
    <EmptyState v-else-if="!filtered.length" description="暂无秒杀活动，稍后再来看看" />
    <div v-else class="seckill__list">
      <div
        v-for="s in filtered"
        :key="s.id"
        class="sk-row"
        @click="router.push({ name: 'goods', params: { id: s.goodsId } })"
      >
        <van-image :src="s.goodsPic" width="110" height="110" radius="8" fit="cover" lazy-load />
        <div class="sk-row__body">
          <div class="sk-row__name ellipsis-2">{{ s.goodsName }}</div>
          <div v-if="s.specText" class="sk-row__spec ellipsis">{{ s.specText }}</div>
          <div class="sk-row__time">
            {{ s.state === 'ONGOING' ? '距结束' : '距开始' }}
            <CountDown :end="s.state === 'ONGOING' ? s.endAt : s.startAt" @finish="load" />
          </div>
          <div class="sk-row__foot">
            <div>
              <Price :value="s.seckillPrice" size="lg" />
              <div class="sk-row__progress">
                <div class="sk-row__bar" :style="{ width: percent(s) + '%' }" />
              </div>
              <div class="sk-row__percent">已抢{{ percent(s) }}%</div>
            </div>
            <van-button size="small" round class="btn-gradient sk-row__btn">
              {{ s.state === 'ONGOING' ? (percent(s) >= 100 ? '已抢光' : '马上抢') : '去看看' }}
            </van-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.seckill {
  &__banner {
    padding: 20px $gap 24px;
    background: $gradient;
    color: #fff;
  }
  &__title {
    font-size: 24px;
    font-weight: 800;
    font-style: italic;
  }
  &__desc {
    margin-top: 6px;
    font-size: 13px;
    opacity: 0.9;
  }
  &__loading {
    padding: 40px 0;
  }
  &__list {
    padding-bottom: 20px;
  }
}
.sk-row {
  @include card;
  display: flex;
  gap: 10px;
  margin: 10px $gap 0;
  padding: 10px;
  &__body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }
  &__name {
    font-size: 14px;
    line-height: 19px;
  }
  &__spec {
    margin-top: 4px;
    font-size: 12px;
    color: $text-3;
  }
  &__time {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-top: 6px;
    font-size: 12px;
    color: $text-2;
  }
  &__foot {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    margin-top: auto;
    padding-top: 6px;
  }
  &__progress {
    width: 100px;
    height: 6px;
    margin-top: 6px;
    background: $primary-light;
    border-radius: 3px;
    overflow: hidden;
  }
  &__bar {
    height: 100%;
    background: $gradient;
  }
  &__percent {
    margin-top: 2px;
    font-size: 11px;
    color: $text-3;
  }
  &__btn {
    flex-shrink: 0;
    padding: 0 14px;
  }
}
</style>
