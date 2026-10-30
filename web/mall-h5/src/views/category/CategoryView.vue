<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { Category } from '@/types'
import { getCategoryTree } from '@/api/public'
import EmptyState from '@/components/EmptyState.vue'

defineOptions({ name: 'CategoryView' })

const router = useRouter()
const tree = ref<Category[]>([])
const active = ref(0)
const loading = ref(true)
const error = ref(false)

async function load() {
  loading.value = true
  error.value = false
  try {
    tree.value = (await getCategoryTree()) || []
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}
onMounted(load)

const current = computed(() => tree.value[active.value])
const children = computed(() => current.value?.children || [])

function openCategory(c: Category) {
  router.push({ name: 'goods-list', query: { categoryId: c.id, title: c.name } })
}
</script>

<template>
  <div class="cat page">
    <div class="cat__search">
      <div class="cat__search-box" @click="router.push({ name: 'search' })">
        <van-icon name="search" size="16" />
        <span>搜索商品</span>
      </div>
    </div>

    <van-loading v-if="loading" class="cat__loading" vertical>加载中...</van-loading>
    <EmptyState v-else-if="error" image="network" description="加载失败" button-text="重试" @click="load" />
    <EmptyState v-else-if="!tree.length" description="暂无分类" />
    <div v-else class="cat__body">
      <van-sidebar v-model="active" class="cat__side">
        <van-sidebar-item v-for="c in tree" :key="c.id" :title="c.name" />
      </van-sidebar>
      <div class="cat__main">
        <div v-if="current" class="cat__hero" @click="openCategory(current)">
          <van-image v-if="current.icon" :src="current.icon" fit="cover" width="56" height="56" radius="8" />
          <div class="cat__hero-text">
            <div class="cat__hero-name">{{ current.name }}</div>
            <div class="cat__hero-sub">查看全部商品 <van-icon name="arrow" /></div>
          </div>
        </div>
        <div v-if="children.length" class="cat__grid">
          <div v-for="c in children" :key="c.id" class="cat__item" @click="openCategory(c)">
            <div class="cat__item-img">
              <van-image v-if="c.icon" :src="c.icon" fit="cover" width="100%" height="100%" lazy-load radius="8" />
              <span v-else>{{ c.name.slice(0, 2) }}</span>
            </div>
            <span class="cat__item-name ellipsis">{{ c.name }}</span>
          </div>
        </div>
        <EmptyState v-else description="该类目下暂无子类目" button-text="查看商品" @click="current && openCategory(current)" />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.cat {
  background: $card;
  &__search {
    position: sticky;
    top: 0;
    z-index: 10;
    padding: 8px $gap;
    padding-top: calc(8px + env(safe-area-inset-top));
    background: $card;
    border-bottom: 1px solid $border;
  }
  &__search-box {
    height: 34px;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0 12px;
    border-radius: 999px;
    background: $bg;
    color: $text-3;
    font-size: 13px;
  }
  &__loading {
    padding: 60px 0;
  }
  &__body {
    display: flex;
    height: calc(100vh - 51px - 50px - env(safe-area-inset-top) - env(safe-area-inset-bottom));
  }
  &__side {
    flex: 0 0 88px;
    width: 88px;
    height: 100%;
    overflow-y: auto;
    background: $bg;
    :deep(.van-sidebar-item) {
      padding: 16px 8px;
      font-size: 13px;
      text-align: center;
      background: $bg;
    }
    :deep(.van-sidebar-item--select) {
      background: $card;
      color: $primary;
      font-weight: 600;
    }
  }
  &__main {
    flex: 1;
    min-width: 0;
    height: 100%;
    overflow-y: auto;
    padding: 12px;
  }
  &__hero {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px;
    border-radius: $radius;
    background: linear-gradient(135deg, $primary-light, $card);
  }
  &__hero-name {
    font-size: 16px;
    font-weight: 700;
  }
  &__hero-sub {
    margin-top: 4px;
    font-size: 12px;
    color: $text-3;
  }
  &__grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px 8px;
    margin-top: 16px;
  }
  &__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    min-width: 0;
  }
  &__item-img {
    width: 100%;
    max-width: 72px;
    aspect-ratio: 1 / 1;
    border-radius: 8px;
    background: $bg;
    color: $text-2;
    font-size: 13px;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
  }
  &__item-name {
    max-width: 100%;
    font-size: 12px;
  }
}
</style>
