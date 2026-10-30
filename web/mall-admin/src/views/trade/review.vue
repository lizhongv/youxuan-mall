<script setup lang="ts">
import { computed, ref } from 'vue'
import { reviewApi } from '@/api/trade'
import type { Review, Switch } from '@/types'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { formatTime } from '@/utils/format'
import { toast } from '@/utils/message'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'
import GoodsPicker from '@/components/GoodsPicker.vue'
import type { GoodsItem } from '@/types'

const user = useUserStore()
const canEdit = computed(() => user.hasPerm('trade:review:edit'))

const { query, list, total, page, size, loading, load, search, reset, onPageChange, onSizeChange } = useTable(reviewApi.page, {
  goodsId: '',
  rating: '' as number | '',
  status: '' as Switch | '',
  hasImage: '' as Switch | '',
})

// 商品筛选通过选择器
const pickerVisible = ref(false)
const goodsName = ref('')
function onPickGoods(rows: GoodsItem[]) {
  query.goodsId = rows[0]?.id || ''
  goodsName.value = rows[0]?.name || ''
  search()
}
function clearGoods() {
  query.goodsId = ''
  goodsName.value = ''
  search()
}
function onReset() {
  goodsName.value = ''
  reset()
}

async function toggleStatus(row: Review) {
  const status = (row.status ? 0 : 1) as Switch
  await reviewApi.setStatus(row.id, status)
  row.status = status
  list.value = [...list.value]
  toast.success(status ? '评价已显示' : '评价已隐藏')
}

// 回复
const replyVisible = ref(false)
const replying = ref<Review | null>(null)
const replyText = ref('')
const saving = ref(false)
function openReply(row: Review) {
  replying.value = row
  replyText.value = row.reply || ''
  replyVisible.value = true
}
async function saveReply() {
  if (!replying.value) return
  if (!replyText.value.trim()) return toast.warning('请输入回复内容')
  saving.value = true
  try {
    await reviewApi.reply(replying.value.id, replyText.value.trim())
    toast.success('回复成功')
    replyVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const QUICK_REPLIES = ['感谢您的支持与好评，期待您再次光临！', '非常抱歉给您带来不好的体验，客服会尽快联系您处理。', '感谢您的反馈，我们会持续改进商品与服务。']
</script>

<template>
  <div class="page">
    <div class="card">
      <el-form :model="query" inline class="search-bar" @submit.prevent="search">
        <el-form-item label="商品">
          <el-input :model-value="goodsName" placeholder="点击选择商品" readonly @click="pickerVisible = true">
            <template #suffix>
              <el-icon v-if="query.goodsId" style="cursor: pointer" @click.stop="clearGoods"><CircleClose /></el-icon>
              <el-icon v-else><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="评分">
          <el-select v-model="query.rating" placeholder="全部" clearable>
            <el-option v-for="n in [5, 4, 3, 2, 1]" :key="n" :label="`${n} 星`" :value="n" />
          </el-select>
        </el-form-item>
        <el-form-item label="有图">
          <el-select v-model="query.hasImage" placeholder="全部" clearable>
            <el-option label="有图" :value="1" />
            <el-option label="无图" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option label="显示中" :value="1" />
            <el-option label="已隐藏" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="search">搜索</el-button>
          <el-button :icon="'Refresh'" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="买家" width="160">
          <template #default="{ row }: { row: any }">
            <div class="flex-center">
              <el-avatar :size="32" :src="row.anonymous ? undefined : row.avatar || undefined">{{ (row.nickname || '匿').slice(0, 1) }}</el-avatar>
              <div>
                <div>{{ row.nickname }}</div>
                <el-tag v-if="row.anonymous" size="small" type="info">匿名</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="360">
          <template #default="{ row }: { row: any }">
            <div class="review">
              <div class="review__head">
                <el-rate :model-value="row.rating" disabled size="small" />
                <span class="text-secondary">{{ formatTime(row.createdAt, 'YYYY-MM-DD HH:mm') }}</span>
              </div>
              <div class="review__content">{{ row.content || '买家未填写文字评价' }}</div>
              <div v-if="row.images?.length" class="review__images">
                <el-image
                  v-for="(img, i) in row.images"
                  :key="img"
                  :src="img"
                  fit="cover"
                  :preview-src-list="row.images"
                  :initial-index="i"
                  preview-teleported
                  class="review__img"
                />
              </div>
              <div v-if="row.reply" class="review__reply"><b>商家回复：</b>{{ row.reply }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="220">
          <template #default="{ row }: { row: any }">
            <div class="goods-cell">
              <el-image :src="row.goodsPic" fit="cover" class="goods-cell__img" />
              <div class="goods-cell__body">
                <div class="goods-cell__name">{{ row.goodsName }}</div>
                <div class="goods-cell__sub">{{ row.specText }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="显示" width="90" align="center">
          <template #default="{ row }: { row: any }">
            <el-switch :model-value="row.status === 1" :disabled="!canEdit" @change="toggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column v-if="canEdit" label="操作" width="100" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button link type="primary" @click="openReply(row)">{{ row.reply ? '修改回复' : '回复' }}</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState text="暂无评价" /></template>
      </el-table>
      <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <GoodsPicker v-model="pickerVisible" title="按商品筛选" @confirm="onPickGoods" />

    <el-dialog v-model="replyVisible" title="回复评价" width="520px">
      <div v-if="replying" class="reply-origin">
        <el-rate :model-value="replying.rating" disabled size="small" />
        <p>{{ replying.content || '买家未填写文字评价' }}</p>
      </div>
      <el-input v-model="replyText" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="回复内容将展示在商品评价下方" />
      <div class="quick">
        <span class="text-secondary">快捷回复：</span>
        <el-check-tag v-for="q in QUICK_REPLIES" :key="q" :checked="replyText === q" @change="replyText = q">{{ q.slice(0, 10) }}…</el-check-tag>
      </div>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveReply">发送回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.review {
  display: flex;
  flex-direction: column;
  gap: 6px;
  &__head {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 12px;
  }
  &__content {
    color: var(--app-text);
    line-height: 1.6;
  }
  &__images {
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
  }
  &__img {
    width: 56px;
    height: 56px;
    border-radius: var(--app-radius-sm);
    border: 1px solid var(--app-border);
  }
  &__reply {
    padding: 8px 10px;
    font-size: 12px;
    line-height: 1.6;
    border-radius: var(--app-radius-sm);
    color: var(--app-text-regular);
    background: var(--app-primary-soft);
  }
}
.reply-origin {
  padding: 10px 12px;
  margin-bottom: 12px;
  border-radius: var(--app-radius);
  background: var(--app-card-bg-muted);
  p {
    margin: 6px 0 0;
    font-size: 13px;
    color: var(--app-text-regular);
  }
}
.quick {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  font-size: 12px;
  .el-check-tag {
    font-size: 12px;
    padding: 4px 8px;
  }
}
</style>
