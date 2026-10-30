<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'

/** 列表分页：与 useTable 的 page / size / onPageChange / onSizeChange 配合 */
defineProps<{ page: number; size: number; total: number }>()
const emit = defineEmits<{ 'page-change': [page: number]; 'size-change': [size: number] }>()

const app = useAppStore()
const layout = computed(() => (app.isMobile ? 'total, prev, pager, next' : 'total, sizes, prev, pager, next, jumper'))
</script>

<template>
  <div class="pagination">
    <el-pagination
      :current-page="page"
      :page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      :layout="layout"
      :pager-count="app.isMobile ? 5 : 7"
      background
      @current-change="emit('page-change', $event)"
      @size-change="emit('size-change', $event)"
    />
  </div>
</template>
