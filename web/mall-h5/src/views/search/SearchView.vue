<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import { getGoodsPage } from '@/api/public'
import { storage, KEYS } from '@/utils/storage'

const route = useRoute()
const router = useRouter()
const keyword = ref((route.query.keyword as string) || '')
const history = ref<string[]>(storage.get<string[]>(KEYS.SEARCH_HISTORY, []) || [])
/** 后端暂无热词接口：取热卖商品名，失败时用固定词 */
const FALLBACK_HOT = ['手机', '耳机', '运动鞋', '零食', 'T恤', '面膜', '洗发水', '保温杯']
const hot = ref<string[]>([])
const MAX_HISTORY = 15

onMounted(async () => {
  try {
    const res = await getGoodsPage({ page: 1, size: 8, sort: 'SALES' })
    // 热搜词保留完整商品名，过长时由标签样式省略显示
    const names = (res?.records || []).map((g) => g.name)
    hot.value = names.length ? names : FALLBACK_HOT
  } catch {
    hot.value = FALLBACK_HOT
  }
})

function search(word?: string) {
  const w = (word ?? keyword.value).trim()
  if (!w) return
  history.value = [w, ...history.value.filter((h) => h !== w)].slice(0, MAX_HISTORY)
  storage.set(KEYS.SEARCH_HISTORY, history.value)
  router.replace({ name: 'goods-list', query: { keyword: w } })
}

function clearHistory() {
  showConfirmDialog({ message: '确定清空搜索历史吗？' })
    .then(() => {
      history.value = []
      storage.remove(KEYS.SEARCH_HISTORY)
    })
    .catch(() => {})
}

function back() {
  if (window.history.state?.back) router.back()
  else router.replace('/')
}
</script>

<template>
  <div class="page search">
    <form action="/" class="search__bar" @submit.prevent="search()">
      <van-icon name="arrow-left" size="20" class="search__back" @click="back" />
      <van-search
        v-model="keyword"
        class="search__input"
        shape="round"
        placeholder="搜索商品"
        autofocus
        clearable
        @search="search()"
      />
      <span class="search__btn" @click="search()">搜索</span>
    </form>

    <section v-if="history.length" class="search__section">
      <div class="search__head">
        <span>搜索历史</span>
        <van-icon name="delete-o" size="16" class="text-3" @click="clearHistory" />
      </div>
      <div class="search__tags">
        <span v-for="h in history" :key="h" class="search__tag ellipsis" @click="search(h)">{{ h }}</span>
      </div>
    </section>

    <section class="search__section">
      <div class="search__head"><span>热门搜索</span></div>
      <div class="search__tags">
        <span
          v-for="(h, i) in hot"
          :key="h + i"
          class="search__tag ellipsis"
          :class="{ 'is-hot': i < 3 }"
          @click="search(h)"
        >
          <van-icon v-if="i < 3" name="fire" />{{ h }}
        </span>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.search {
  background: $card;
  &__bar {
    position: sticky;
    top: 0;
    z-index: 10;
    display: flex;
    align-items: center;
    padding: 0 $gap 0 8px;
    padding-top: env(safe-area-inset-top);
    background: $card;
  }
  &__back {
    padding: 4px;
    color: $text;
  }
  /*
   * van-search 内部的 field 是 van-cell，样式加载顺序可能让 cell 的白底盖住搜索框灰底，
   * 只露出左侧圆角一截（背景错位）：统一由 content 提供灰底，field 透明。
   */
  &__input {
    flex: 1;
    min-width: 0;
    padding: 8px 6px;
    background: transparent;
    :deep(.van-search__content) {
      height: 34px;
      align-items: center;
      background: $bg;
    }
    :deep(.van-search__field),
    :deep(.van-cell) {
      height: 100%;
      padding-top: 0;
      padding-bottom: 0;
      background: transparent;
    }
  }
  &__btn {
    flex-shrink: 0;
    font-size: 15px;
    color: $primary;
    font-weight: 600;
  }
  &__section {
    padding: 12px $gap 4px;
  }
  &__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 15px;
    font-weight: 600;
    margin-bottom: 12px;
  }
  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }
  &__tag {
    max-width: 100%;
    padding: 6px 12px;
    font-size: 13px;
    background: $bg;
    border-radius: 999px;
    color: $text;
    &.is-hot {
      color: $primary;
      background: $primary-light;
    }
  }
}
</style>
