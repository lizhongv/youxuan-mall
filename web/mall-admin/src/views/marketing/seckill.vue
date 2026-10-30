<script setup lang="ts">
import { computed, reactive, ref, shallowRef } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { seckillApi } from '@/api/marketing'
import { goodsApi } from '@/api/goods'
import type { GoodsItem, ID, Seckill, Sku, Switch } from '@/types'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { formatTime, money } from '@/utils/format'
import { SECKILL_STATE } from '@/utils/constants'
import { confirmDelete, toast } from '@/utils/message'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'
import GoodsPicker from '@/components/GoodsPicker.vue'

const user = useUserStore()
const canEdit = computed(() => user.hasPerm('marketing:seckill:edit'))

const { query, list, total, page, size, loading, load, search, reset, onPageChange, onSizeChange } = useTable(seckillApi.page, { keyword: '' })

async function toggleStatus(row: Seckill) {
  const status = (row.status ? 0 : 1) as Switch
  await seckillApi.update(row.id, { ...row, status })
  row.status = status
  list.value = [...list.value]
  toast.success(status ? '活动已启用' : '活动已停用')
}

async function remove(row: Seckill) {
  if (await confirmDelete(row.title, () => seckillApi.remove(row.id))) load()
}

// ---------------- 表单 ----------------
const visible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<ID>('')
const pickerVisible = ref(false)
const goods = ref<{ id: ID; name: string; pic: string } | null>(null)
const skus = shallowRef<Sku[]>([])
const skuLoading = ref(false)

const form = reactive({
  title: '',
  goodsId: '' as ID,
  skuId: '' as ID,
  seckillPrice: 0,
  stock: 1,
  limitPerMember: 1,
  range: [] as string[],
  status: 1 as Switch,
})

const currentSku = computed(() => skus.value.find((s) => s.id === form.skuId))

const rules: FormRules = {
  title: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  goodsId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  skuId: [{ required: true, message: '请选择参与秒杀的规格', trigger: 'change' }],
  seckillPrice: [
    {
      validator: (_r, v: number, cb) => {
        if (!(v > 0)) return cb(new Error('请输入秒杀价'))
        if (currentSku.value && v >= Number(currentSku.value.price)) return cb(new Error('秒杀价应低于原售价'))
        cb()
      },
      trigger: 'blur',
    },
  ],
  stock: [
    {
      validator: (_r, v: number, cb) => {
        if (!(v > 0)) return cb(new Error('请输入活动库存'))
        // 编辑时库存已从 SKU 划拨，SKU 剩余库存不再作为上限
        if (!editingId.value && currentSku.value && v > currentSku.value.stock) return cb(new Error(`不能超过 SKU 库存 ${currentSku.value.stock}`))
        cb()
      },
      trigger: 'blur',
    },
  ],
  range: [{ validator: (_r, v: string[], cb) => (v?.length === 2 ? cb() : cb(new Error('请选择活动时间'))), trigger: 'change' }],
}

async function loadSkus(goodsId: ID) {
  skuLoading.value = true
  try {
    const detail = await goodsApi.detail(goodsId)
    skus.value = (detail.skus || []).filter((s) => !!s.id)
    if (goods.value && !goods.value.pic) goods.value.pic = detail.pic
    if (skus.value.length === 1 && !form.skuId) form.skuId = skus.value[0].id as ID
  } finally {
    skuLoading.value = false
  }
}

function open(row?: Seckill) {
  editingId.value = row?.id || ''
  skus.value = []
  goods.value = row ? { id: row.goodsId, name: row.goodsName, pic: row.goodsPic } : null
  Object.assign(form, {
    title: row?.title || '',
    goodsId: row?.goodsId || '',
    skuId: row?.skuId || '',
    seckillPrice: Number(row?.seckillPrice ?? 0),
    stock: row?.stock ?? 1,
    limitPerMember: row?.limitPerMember ?? 1,
    range: row ? [row.startAt, row.endAt] : [],
    status: row?.status ?? 1,
  })
  visible.value = true
  setTimeout(() => formRef.value?.clearValidate())
  if (row) loadSkus(row.goodsId)
}

function onPick(rows: GoodsItem[]) {
  const g = rows[0]
  if (!g) return
  goods.value = { id: g.id, name: g.name, pic: g.pic }
  form.goodsId = g.id
  form.skuId = ''
  if (!form.title) form.title = `${g.name.slice(0, 20)} 限时秒杀`
  loadSkus(g.id)
  formRef.value?.validateField('goodsId')
}

function skuLabel(s: Sku) {
  return s.specValues?.length ? s.specValues.join(' / ') : '默认规格'
}

async function submit() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  const payload: Partial<Seckill> = {
    title: form.title.trim(),
    goodsId: form.goodsId,
    skuId: form.skuId,
    seckillPrice: form.seckillPrice,
    stock: form.stock,
    limitPerMember: form.limitPerMember,
    startAt: form.range[0],
    endAt: form.range[1],
    status: form.status,
  }
  saving.value = true
  try {
    if (editingId.value) await seckillApi.update(editingId.value, payload)
    else await seckillApi.create(payload)
    toast.success('保存成功')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const disabledPast = (d: Date) => d.getTime() < Date.now() - 86400000
</script>

<template>
  <div class="page">
    <div class="card">
      <el-form :model="query" inline class="search-bar" @submit.prevent="search">
        <el-form-item label="活动">
          <el-input v-model="query.keyword" placeholder="活动名称 / 商品名称" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="search">搜索</el-button>
          <el-button :icon="'Refresh'" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="toolbar__left">
          <el-button v-perm="'marketing:seckill:edit'" type="primary" :icon="'Plus'" @click="open()">新建秒杀</el-button>
        </div>
        <div class="toolbar__right">
          <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="load" /></el-tooltip>
        </div>
      </div>
      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="活动商品" min-width="300">
          <template #default="{ row }: { row: any }">
            <div class="goods-cell">
              <el-image :src="row.goodsPic" fit="cover" class="goods-cell__img" />
              <div class="goods-cell__body">
                <div class="goods-cell__name">{{ row.title }}</div>
                <div class="goods-cell__sub">{{ row.goodsName }} · {{ row.specText || '默认规格' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="秒杀价" width="110" align="right">
          <template #default="{ row }: { row: any }"><span class="money money--danger">{{ money(row.seckillPrice) }}</span></template>
        </el-table-column>
        <el-table-column label="销售进度" min-width="160">
          <template #default="{ row }: { row: any }">
            <div class="sub">已售 {{ row.sold }} / {{ row.stock }} · 每人限 {{ row.limitPerMember }}</div>
            <el-progress :percentage="row.stock ? Math.min(100, Math.round((row.sold / row.stock) * 100)) : 0" :stroke-width="6" :show-text="false" status="exception" />
          </template>
        </el-table-column>
        <el-table-column label="活动时间" min-width="200">
          <template #default="{ row }: { row: any }">
            <div class="sub">{{ formatTime(row.startAt, 'MM-DD HH:mm') }} 开始</div>
            <div class="sub text-secondary">{{ formatTime(row.endAt, 'MM-DD HH:mm') }} 结束</div>
          </template>
        </el-table-column>
        <el-table-column label="活动状态" width="100" align="center">
          <template #default="{ row }: { row: any }"><StatusTag :value="row.state" :options="SECKILL_STATE" /></template>
        </el-table-column>
        <el-table-column label="启用" width="80" align="center">
          <template #default="{ row }: { row: any }">
            <el-switch :model-value="row.status === 1" :disabled="!canEdit" @change="toggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column v-if="canEdit" label="操作" width="120" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button link type="primary" @click="open(row)">编辑</el-button>
            <el-button link type="danger" :disabled="row.state === 'ONGOING'" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState text="暂无秒杀活动" /></template>
      </el-table>
      <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <el-dialog v-model="visible" :title="editingId ? '编辑秒杀活动' : '新建秒杀活动'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="秒杀商品" prop="goodsId">
          <div v-if="goods" class="picked-goods">
            <el-image :src="goods.pic" fit="cover" class="goods-cell__img" />
            <span class="picked-goods__name">{{ goods.name }}</span>
            <el-button link type="primary" @click="pickerVisible = true">更换</el-button>
          </div>
          <el-button v-else :icon="'Plus'" @click="pickerVisible = true">选择商品</el-button>
        </el-form-item>
        <el-form-item v-if="goods" label="参与规格" prop="skuId">
          <div v-loading="skuLoading" class="sku-options">
            <el-radio-group v-model="form.skuId" class="sku-options__group">
              <el-radio v-for="s in skus" :key="s.id!" :value="s.id!" border class="sku-option">
                <span>{{ skuLabel(s) }}</span>
                <span class="text-secondary sku-option__meta">{{ money(s.price) }} · 库存 {{ s.stock }}</span>
              </el-radio>
            </el-radio-group>
            <span v-if="!skuLoading && !skus.length" class="form-tip">该商品暂无可用 SKU</span>
          </div>
        </el-form-item>
        <el-form-item label="活动名称" prop="title">
          <el-input v-model="form.title" maxlength="64" show-word-limit />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="秒杀价" prop="seckillPrice">
              <el-input-number v-model="form.seckillPrice" :min="0.01" :precision="2" controls-position="right" />
              <div v-if="currentSku" class="form-tip">原价 {{ money(currentSku.price) }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="活动库存" prop="stock">
              <el-input-number v-model="form.stock" :min="1" :precision="0" controls-position="right" />
              <div class="form-tip">从 SKU 库存中划拨</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="每人限购">
          <el-input-number v-model="form.limitPerMember" :min="1" :max="99" :precision="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="活动时间" prop="range">
          <el-date-picker
            v-model="form.range"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled-date="disabledPast"
          />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <GoodsPicker v-model="pickerVisible" only-on-sale title="选择秒杀商品" @confirm="onPick" />
  </div>
</template>

<style scoped lang="scss">
.sub {
  font-size: 12px;
}
.picked-goods {
  display: flex;
  align-items: center;
  gap: 10px;
  &__name {
    max-width: 320px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
.sku-options {
  width: 100%;
  min-height: 40px;
  &__group {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
}
.sku-option {
  margin-right: 0 !important;
  height: auto !important;
  padding: 8px 12px !important;
  :deep(.el-radio__label) {
    display: flex;
    flex-direction: column;
    line-height: 1.4;
  }
  &__meta {
    font-size: 12px;
  }
}
</style>
