<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import type { TreeInstance } from 'element-plus'
import { brandApi, categoryApi, goodsApi } from '@/api/goods'
import type { Brand, Category, GoodsItem, GoodsStatus, Switch } from '@/types'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { formatTime, money } from '@/utils/format'
import { StorageKey, storage } from '@/utils/storage'
import { confirm, confirmDelete, toast } from '@/utils/message'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'

const router = useRouter()
const user = useUserStore()
const canEdit = computed(() => user.hasPerm('goods:goods:edit'))

const { query, list, total, page, size, loading, selection, load, search, reset, onPageChange, onSizeChange, onSelectionChange } = useTable(
  goodsApi.page,
  { keyword: '', brandId: '', categoryId: '', status: '' as GoodsStatus | '' },
)

// ---------- 类目树筛选（可折叠，状态记忆） ----------
const catOpen = ref(storage.get(StorageKey.catPanel) !== '0')
function toggleCat() {
  catOpen.value = !catOpen.value
  storage.set(StorageKey.catPanel, catOpen.value ? '1' : '0')
}
const currentCategoryName = computed(() => {
  const walk = (nodes: Category[]): string => {
    for (const n of nodes) {
      if (n.id === query.categoryId) return n.name
      const c = walk(n.children || [])
      if (c) return c
    }
    return ''
  }
  return query.categoryId ? walk(categories.value) : ''
})

const categories = shallowRef<Category[]>([])
const treeRef = ref<TreeInstance>()
const treeFilter = ref('')
const brands = shallowRef<Brand[]>([])

onMounted(async () => {
  const [cats, bs] = await Promise.allSettled([categoryApi.tree(), brandApi.list()])
  if (cats.status === 'fulfilled') categories.value = cats.value || []
  if (bs.status === 'fulfilled') brands.value = bs.value || []
})

function onCategoryClick(node: Category) {
  query.categoryId = query.categoryId === node.id ? '' : node.id
  if (!query.categoryId) treeRef.value?.setCurrentKey(undefined)
  search()
}

function clearCategory() {
  query.categoryId = ''
  treeRef.value?.setCurrentKey(undefined)
  search()
}

function filterNode(value: string, data: Category) {
  return !value || data.name.includes(value)
}

function onReset() {
  treeRef.value?.setCurrentKey(undefined)
  reset()
}

// ---------- 状态标签页 ----------
const statusTab = computed({
  get: () => query.status || 'ALL',
  set: (v: string) => {
    query.status = v === 'ALL' ? '' : (v as GoodsStatus)
    search()
  },
})

// ---------- 操作 ----------
async function batchStatus(status: GoodsStatus) {
  const ids = selection.value.map((g) => g.id)
  if (!ids.length) return
  const text = status === 'ON_SALE' ? '上架' : '下架'
  if (!(await confirm(`确定${text}选中的 ${ids.length} 件商品吗？`))) return
  await goodsApi.setStatus(ids, status)
  toast.success(`已${text} ${ids.length} 件商品`)
  load()
}

const switching = ref<Record<string, boolean>>({})

async function toggleStatus(row: GoodsItem) {
  const next: GoodsStatus = row.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE'
  switching.value[row.id] = true
  try {
    await goodsApi.setStatus([row.id], next)
    row.status = next
    toast.success(next === 'ON_SALE' ? '已上架' : '已下架')
  } finally {
    switching.value[row.id] = false
  }
}

type FlagKey = 'isNew' | 'isHot' | 'isRecommend'
const FLAGS: { key: FlagKey; label: string }[] = [
  { key: 'isNew', label: '新品' },
  { key: 'isHot', label: '热卖' },
  { key: 'isRecommend', label: '推荐' },
]

async function toggleFlag(row: GoodsItem, key: FlagKey) {
  if (!canEdit.value) return
  const flags = { isNew: row.isNew, isHot: row.isHot, isRecommend: row.isRecommend }
  flags[key] = (row[key] ? 0 : 1) as Switch
  await goodsApi.setFlags(row.id, flags)
  row[key] = flags[key]
  list.value = [...list.value]
}

async function remove(row: GoodsItem) {
  if (await confirmDelete(row.name, () => goodsApi.remove(row.id))) load()
}

function edit(row?: GoodsItem) {
  router.push(row ? `/goods/edit/${row.id}` : '/goods/edit')
}
</script>

<template>
  <div class="goods-page">
    <aside v-show="catOpen" class="card cat-panel">
      <div class="cat-panel__head">
        <span class="card-title" style="margin: 0"><span class="card-title__text">商品类目</span></span>
        <div class="flex-center" style="gap: 4px">
          <el-button v-if="query.categoryId" link type="primary" size="small" @click="clearCategory">全部</el-button>
          <el-tooltip content="收起类目"><el-button link size="small" :icon="'Fold'" aria-label="收起类目" @click="toggleCat" /></el-tooltip>
        </div>
      </div>
      <el-input v-model="treeFilter" placeholder="搜索类目" clearable size="small" :prefix-icon="'Search'" @input="treeRef?.filter(treeFilter)" />
      <el-scrollbar class="cat-panel__tree">
        <el-tree
          ref="treeRef"
          :data="categories"
          node-key="id"
          :props="{ label: 'name', children: 'children' }"
          highlight-current
          default-expand-all
          :expand-on-click-node="false"
          :filter-node-method="(v: string, d: any) => filterNode(v, d as Category)"
          @node-click="(d: Category) => onCategoryClick(d)"
        >
          <template #default="{ data }">
            <span class="cat-node">
              <el-icon v-if="!data.parentId || data.parentId === '0'"><Folder /></el-icon>
              <span class="ellipsis">{{ data.name }}</span>
            </span>
          </template>
        </el-tree>
      </el-scrollbar>
    </aside>

    <section class="page goods-main">
      <div class="card">
        <el-form :model="query" inline class="search-bar" @submit.prevent="search">
          <el-form-item v-if="!catOpen">
            <el-button :icon="'Expand'" @click="toggleCat">类目筛选</el-button>
            <el-tag v-if="currentCategoryName" closable class="cat-chip" @close="clearCategory">{{ currentCategoryName }}</el-tag>
          </el-form-item>
          <el-form-item label="商品">
            <el-input v-model="query.keyword" placeholder="名称 / 关键词" clearable @keyup.enter="search" />
          </el-form-item>
          <el-form-item label="品牌">
            <el-select v-model="query.brandId" placeholder="全部品牌" clearable filterable>
              <el-option v-for="b in brands" :key="b.id" :label="b.name" :value="b.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="'Search'" @click="search">搜索</el-button>
            <el-button :icon="'Refresh'" @click="onReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="card">
        <el-tabs v-model="statusTab" class="status-tabs">
          <el-tab-pane label="全部商品" name="ALL" />
          <el-tab-pane label="在售中" name="ON_SALE" />
          <el-tab-pane label="已下架" name="OFF_SALE" />
        </el-tabs>
        <div class="toolbar">
          <div class="toolbar__left">
            <el-button v-perm="'goods:goods:edit'" type="primary" :icon="'Plus'" @click="edit()">发布商品</el-button>
            <el-button v-perm="'goods:goods:edit'" :disabled="!selection.length" @click="batchStatus('ON_SALE')">批量上架</el-button>
            <el-button v-perm="'goods:goods:edit'" :disabled="!selection.length" @click="batchStatus('OFF_SALE')">批量下架</el-button>
            <span v-if="selection.length" class="text-secondary">已选 {{ selection.length }} 项</span>
          </div>
          <div class="toolbar__right">
            <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="load" /></el-tooltip>
          </div>
        </div>

        <el-table v-loading="loading" :data="list" row-key="id" class="goods-table" @selection-change="onSelectionChange">
          <el-table-column type="selection" width="40" fixed="left" />
          <el-table-column label="商品信息" min-width="260">
            <template #default="{ row }: { row: any }">
              <div class="goods-cell">
                <el-image :src="row.pic" fit="cover" class="goods-cell__img" :preview-src-list="[row.pic]" preview-teleported />
                <div class="goods-cell__body">
                  <div class="goods-cell__name link" :title="row.name" @click="edit(row)">{{ row.name }}</div>
                  <div class="goods-cell__sub ellipsis">{{ row.categoryName || '-' }}<template v-if="row.brandName"> · {{ row.brandName }}</template></div>
                  <div class="flags">
                    <el-check-tag
                      v-for="f in FLAGS"
                      :key="f.key"
                      :checked="!!row[f.key]"
                      :type="f.key === 'isHot' ? 'danger' : f.key === 'isNew' ? 'success' : 'primary'"
                      :class="{ 'is-readonly': !canEdit }"
                      :title="canEdit ? `点击${row[f.key] ? '取消' : '设为'}${f.label}` : f.label"
                      @change="toggleFlag(row, f.key)"
                    >
                      {{ f.label }}
                    </el-check-tag>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="价格" width="104" align="right">
            <template #default="{ row }: { row: any }">
              <div class="money">{{ money(row.price) }}</div>
              <div v-if="Number(row.originalPrice) > Number(row.price)" class="price-origin">{{ money(row.originalPrice) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="库存" width="72" align="right">
            <template #default="{ row }: { row: any }">
              <span :class="{ 'text-danger': Number(row.stock) <= 10 }">{{ row.stock }}</span>
            </template>
          </el-table-column>
          <el-table-column label="销量" prop="sales" width="72" align="right" />
          <el-table-column label="上架" width="80" align="center">
            <template #default="{ row }: { row: any }">
              <el-switch
                :model-value="row.status === 'ON_SALE'"
                :loading="switching[row.id]"
                :disabled="!canEdit"
                inline-prompt
                active-text="在售"
                inactive-text="下架"
                @change="toggleStatus(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="排序 / 创建" width="108">
            <template #default="{ row }: { row: any }">
              <div class="text-secondary cell-sm">排序 {{ row.sort ?? 0 }}</div>
              <div class="text-secondary cell-sm" :title="formatTime(row.createdAt)">{{ formatTime(row.createdAt, 'YYYY-MM-DD') }}</div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="104" fixed="right">
            <template #default="{ row }: { row: any }">
              <el-button v-if="canEdit" link type="primary" @click="edit(row)">编辑</el-button>
              <el-button v-else link type="primary" @click="edit(row)">查看</el-button>
              <el-button v-perm="'goods:goods:edit'" link type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <EmptyState v-if="!loading" :text="query.keyword || query.categoryId || query.brandId ? '没有符合条件的商品' : '还没有商品'">
              <el-button v-if="!(query.keyword || query.categoryId || query.brandId)" v-perm="'goods:goods:edit'" type="primary" size="small" @click="edit()">发布第一个商品</el-button>
            </EmptyState>
            <div v-else class="table-placeholder" />
          </template>
        </el-table>
        <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.goods-page {
  display: flex;
  gap: var(--app-gap);
  align-items: flex-start;
}
.cat-panel {
  flex: none;
  width: 210px;
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  &__tree {
    max-height: calc(100vh - 260px);
    margin: 0 -6px;
  }
}
.cat-node {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  font-size: 13px;
  color: var(--app-text-regular);
}
.goods-main {
  flex: 1;
  min-width: 0;
}
.status-tabs {
  margin-top: -8px;
  :deep(.el-tabs__header) {
    margin-bottom: 14px;
  }
}
.price-origin {
  font-size: 12px;
  color: var(--app-text-placeholder);
  text-decoration: line-through;
}
.cat-chip {
  margin-left: 8px;
}
.link {
  cursor: pointer;
  &:hover {
    color: var(--el-color-primary);
  }
}
.cell-sm {
  font-size: 12px;
  line-height: 1.6;
}
.flags {
  display: flex;
  gap: 4px;
  margin-top: 4px;
  .el-check-tag {
    padding: 1px 6px;
    font-size: 11px;
    font-weight: 500;
    line-height: 18px;
  }
  .is-readonly {
    cursor: default;
  }
}

@media (max-width: 992px) {
  .goods-page {
    flex-direction: column;
    align-items: stretch;
  }
  .cat-panel {
    width: 100%;
    position: static;
    &__tree {
      max-height: 200px;
    }
  }
}
</style>
