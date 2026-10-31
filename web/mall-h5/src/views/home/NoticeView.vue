<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { Notice } from '@/types'
import { getNotice } from '@/api/public'
import { fmtTime, sanitizeHtml } from '@/utils'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { errorMessage } from '@/api/request'
import LoadState from '@/components/LoadState.vue'

const route = useRoute()
const notice = ref<Notice | null>(null)
const loading = ref(true)
const loadError = ref('')

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    notice.value = await getNotice(route.params.id as string)
  } catch (e) {
    loadError.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}
onMounted(load)

/** 公告内容可能是纯文本或富文本：纯文本保留换行 */
const html = computed(() => {
  const c = notice.value?.content || ''
  if (/<[a-z][\s\S]*>/i.test(c)) return sanitizeHtml(c)
  return c.replace(/[&<>"']/g, (m) => `&#${m.charCodeAt(0)};`).replace(/\n/g, '<br/>')
})
</script>

<template>
  <div class="page notice">
    <NavBar title="公告详情" />
    <LoadState v-if="!notice" :loading="loading" :error="loadError" @retry="load">
      <EmptyState description="公告不存在或已下线" />
    </LoadState>
    <article v-else class="notice__card">
      <h1 class="notice__title">{{ notice.title }}</h1>
      <div class="notice__time">{{ fmtTime(notice.createdAt) }}</div>
      <div class="rich-text" v-html="html" />
    </article>
  </div>
</template>

<style scoped lang="scss">
.notice {
  background: $card;
  &__loading {
    padding: 60px 0;
  }
  &__card {
    padding: 16px;
  }
  &__title {
    margin: 0;
    font-size: 19px;
    line-height: 1.4;
  }
  &__time {
    margin: 8px 0 16px;
    font-size: 12px;
    color: $text-3;
  }
}
</style>
