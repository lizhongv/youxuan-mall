<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import type { TableInstance } from 'element-plus'
import { goodsApi } from '@/api/goods'
import type { GoodsItem, ID } from '@/types'
import { useTable } from '@/composables/useTable'
import { money } from '@/utils/format'
import { GOODS_STATUS } from '@/utils/constants'
import StatusTag from './StatusTag.vue'
import EmptyState from './EmptyState.vue'

/** 商品选择对话框：单选（秒杀 / 轮播链接）或多选（优惠券适用商品） */
const props = withDefaults(defineProps<{ multiple?: boolean; selectedIds?: ID[]; title?: string; onlyOnSale?: boolean }>(), {
  multiple: false,
  selectedIds: () => [],
  title: '选择商品',
  onlyOnSale: false,
})
const visible = defineModel<boolean>({ default: false })
const emit = defineEmits<{ confirm: [rows: GoodsItem[]] }>()

const tableRef = ref<TableInstance>()
const picked = ref<Map<ID, GoodsItem>>(new Map())
const current = ref<GoodsItem | null>(null)

const { query, list, total, page, size, loading, load, search, onPageChange, onSizeChange } = useTable(
  goodsApi.page,
  { keyword: '', status: props.onlyOnSale ? 'ON_SALE' : '' },
  { immediate: false, pageSize: 10 },
)

watch(visible, async (v) => {
  if (!v) return
  picked.value = new Map()
  current.value = null
  await search()
  syncSelection()
})

watch(list, () => nextTick(syncSelection))

/** 跨页保留勾选：翻页后根据已选集合回显 */
function syncSelection() {
  if (!props.multiple || !tableRef.value) return
  list.value.forEach((row) => {
    const on = picked.value.has(row.id) || props.selectedIds.includes(row.id)
    if (on) picked.value.set(row.id, row)
    tableRef.value!.toggleRowSelection(row, on, false)
  })
}

function onSelect(rows: GoodsItem[], row: GoodsItem) {
  if (rows.includes(row)) picked.value.set(row.id, row)
  else picked.value.delete(row.id)
}

function onSelectAll(rows: GoodsItem[]) {
  const on = rows.length > 0
  list.value.forEach((r) => (on ? picked.value.set(r.id, r) : picked.value.delete(r.id)))
}

function confirm() {
  const rows = props.multiple ? Array.from(picked.value.values()) : current.value ? [current.value] : []
  emit('confirm', rows)
  visible.value = false
}

defineExpose({ load })
</script>

<template>
  <el-dialog v-model="visible" :title="title" width="820px" append-to-body destroy-on-close>
    <div class="toolbar">
      <div class="toolbar__left">
        <el-input v-model="query.keyword" placeholder="搜索商品名称" clearable style="width: 260px" @keyup.enter="search" @clear="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="search">搜索</el-button>
      </div>
      <div v-if="multiple" class="toolbar__right text-secondary">已选 {{ picked.size }} 件</div>
    </div>
    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="list"
      row-key="id"
      height="400"
      :highlight-current-row="!multiple"
      @select="onSelect"
      @select-all="onSelectAll"
      @current-change="(r: GoodsItem | null) => (current = r)"
    >
      <el-table-column v-if="multiple" type="selection" width="48" />
      <el-table-column label="商品" min-width="280">
        <template #default="{ row }">
          <div class="goods-cell">
            <el-image :src="row.pic" fit="cover" class="goods-cell__img" />
            <div class="goods-cell__body">
              <div class="goods-cell__name">{{ row.name }}</div>
              <div class="goods-cell__sub">{{ row.categoryName }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="价格" width="110">
        <template #default="{ row }"><span class="money">{{ money(row.price) }}</span></template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column label="状态" width="96">
        <template #default="{ row }"><StatusTag :value="row.status" :options="GOODS_STATUS" /></template>
      </el-table-column>
      <el-table-column v-if="!multiple" width="56" align="center">
        <template #default="{ row }">
          <el-icon v-if="current?.id === row.id" class="text-primary" :size="18"><CircleCheckFilled /></el-icon>
        </template>
      </el-table-column>
      <template #empty><EmptyState text="没有找到商品" /></template>
    </el-table>
    <div class="pagination">
      <el-pagination
        :current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        size="small"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="multiple ? picked.size === 0 : !current" @click="confirm">确定</el-button>
    </template>
  </el-dialog>
</template>
