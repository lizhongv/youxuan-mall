<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowRef } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { couponApi } from '@/api/marketing'
import { categoryApi } from '@/api/goods'
import type { Category, Coupon, CouponScope, CouponType, CouponValidType, GoodsItem, ID, Switch } from '@/types'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { formatTime } from '@/utils/format'
import { COUPON_SCOPE, COUPON_TYPE, COUPON_VALID_TYPE, MEMBER_COUPON_STATUS, toMap } from '@/utils/constants'
import { confirmDelete, toast } from '@/utils/message'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'
import GoodsPicker from '@/components/GoodsPicker.vue'

const user = useUserStore()
const canEdit = computed(() => user.hasPerm('marketing:coupon:edit'))
const scopeMap = toMap(COUPON_SCOPE)

const { query, list, total, page, size, loading, load, search, reset, onPageChange, onSizeChange } = useTable(couponApi.page, {
  keyword: '',
  type: '',
  status: '' as Switch | '',
})

const categories = shallowRef<Category[]>([])
onMounted(async () => {
  try {
    categories.value = (await categoryApi.tree()) || []
  } catch {
    /* 忽略 */
  }
})

/** 面额展示：满 100 减 20 / 8.5 折 / 无门槛减 5 */
function faceText(c: Pick<Coupon, 'type' | 'threshold' | 'amount' | 'discount' | 'maxAmount'>) {
  if (c.type === 'DISCOUNT') {
    const zhe = +(Number(c.discount) * 10).toFixed(2)
    const cond = Number(c.threshold) > 0 ? `满 ${c.threshold} 享` : ''
    const cap = Number(c.maxAmount) > 0 ? `，最高减 ${c.maxAmount}` : ''
    return `${cond}${zhe} 折${cap}`
  }
  if (c.type === 'NO_THRESHOLD') return `无门槛减 ${c.amount}`
  return `满 ${c.threshold} 减 ${c.amount}`
}

function validText(c: Coupon) {
  if (c.validType === 'DAYS') return `领取后 ${c.validDays} 天内有效`
  return `${formatTime(c.startAt, 'YYYY-MM-DD HH:mm')} ~ ${formatTime(c.endAt, 'YYYY-MM-DD HH:mm')}`
}

async function toggleStatus(row: Coupon) {
  const status = (row.status ? 0 : 1) as Switch
  await couponApi.setStatus(row.id, status)
  row.status = status
  list.value = [...list.value]
  toast.success(status ? '已开始发放' : '已停止发放')
}

async function remove(row: Coupon) {
  if (await confirmDelete(row.name, () => couponApi.remove(row.id))) load()
}

// ---------------- 表单 ----------------
interface CouponForm {
  name: string
  type: CouponType
  threshold: number
  amount: number
  /** 表单中以「折」录入（如 8.5），提交时换算为 0.85 */
  discountZhe: number
  maxAmount: number
  scope: CouponScope
  scopeIds: ID[]
  total: number
  perLimit: number
  validType: CouponValidType
  range: string[]
  validDays: number
  status: Switch
  description: string
}

const emptyForm = (): CouponForm => ({
  name: '',
  type: 'FULL_REDUCTION',
  threshold: 100,
  amount: 10,
  discountZhe: 9,
  maxAmount: 0,
  scope: 'ALL',
  scopeIds: [],
  total: 1000,
  perLimit: 1,
  validType: 'FIXED',
  range: [],
  validDays: 7,
  status: 1,
  description: '',
})

const visible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<ID>('')
const form = reactive<CouponForm>(emptyForm())
const goodsNames = ref<Record<string, string>>({})
const pickerVisible = ref(false)

const rules: FormRules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  threshold: [
    {
      validator: (_r, v: number, cb) => (form.type === 'FULL_REDUCTION' && !(v > 0) ? cb(new Error('满减券需设置使用门槛')) : cb()),
      trigger: 'blur',
    },
  ],
  amount: [
    {
      validator: (_r, v: number, cb) => {
        if (form.type === 'DISCOUNT') return cb()
        if (!(v > 0)) return cb(new Error('请输入减免金额'))
        if (form.type === 'FULL_REDUCTION' && v >= form.threshold) return cb(new Error('减免金额需小于使用门槛'))
        cb()
      },
      trigger: 'blur',
    },
  ],
  discountZhe: [
    {
      validator: (_r, v: number, cb) => (form.type === 'DISCOUNT' && !(v > 0 && v < 10) ? cb(new Error('折扣需在 0.1 ~ 9.9 之间')) : cb()),
      trigger: 'blur',
    },
  ],
  scopeIds: [
    {
      validator: (_r, v: ID[], cb) => (form.scope !== 'ALL' && !v.length ? cb(new Error(form.scope === 'GOODS' ? '请选择适用商品' : '请选择适用类目')) : cb()),
      trigger: 'change',
    },
  ],
  range: [
    { validator: (_r, v: string[], cb) => (form.validType === 'FIXED' && v?.length !== 2 ? cb(new Error('请选择有效期')) : cb()), trigger: 'change' },
  ],
  validDays: [{ validator: (_r, v: number, cb) => (form.validType === 'DAYS' && !(v > 0) ? cb(new Error('请输入有效天数')) : cb()), trigger: 'blur' }],
}

function open(row?: Coupon) {
  editingId.value = row?.id || ''
  goodsNames.value = {}
  Object.assign(
    form,
    row
      ? {
          name: row.name,
          type: row.type,
          threshold: Number(row.threshold),
          amount: Number(row.amount),
          discountZhe: +(Number(row.discount || 1) * 10).toFixed(2),
          maxAmount: Number(row.maxAmount),
          scope: row.scope,
          scopeIds: [...(row.scopeIds || [])].map(String),
          total: row.total,
          perLimit: row.perLimit,
          validType: row.validType,
          range: row.startAt && row.endAt ? [row.startAt, row.endAt] : [],
          validDays: row.validDays,
          status: row.status,
          description: row.description,
        }
      : emptyForm(),
  )
  visible.value = true
  setTimeout(() => formRef.value?.clearValidate())
}

function onScopeChange() {
  form.scopeIds = []
}

function onPickGoods(rows: GoodsItem[]) {
  rows.forEach((g) => (goodsNames.value[g.id] = g.name))
  form.scopeIds = rows.map((g) => g.id)
  formRef.value?.validateField('scopeIds')
}

function removeGoods(id: ID) {
  form.scopeIds = form.scopeIds.filter((x) => x !== id)
}

const preview = computed(() =>
  faceText({ type: form.type, threshold: form.threshold, amount: form.amount, discount: form.discountZhe / 10, maxAmount: form.maxAmount }),
)

async function submit() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  const isDiscount = form.type === 'DISCOUNT'
  const payload: Partial<Coupon> = {
    name: form.name.trim(),
    type: form.type,
    threshold: form.type === 'NO_THRESHOLD' ? 0 : form.threshold,
    amount: isDiscount ? 0 : form.amount,
    discount: isDiscount ? +(form.discountZhe / 10).toFixed(2) : 1,
    maxAmount: isDiscount ? form.maxAmount : 0,
    scope: form.scope,
    scopeIds: form.scope === 'ALL' ? [] : form.scopeIds,
    total: form.total,
    perLimit: form.perLimit,
    validType: form.validType,
    startAt: form.validType === 'FIXED' ? form.range[0] : undefined,
    endAt: form.validType === 'FIXED' ? form.range[1] : undefined,
    validDays: form.validType === 'DAYS' ? form.validDays : 0,
    status: form.status,
    description: form.description,
  }
  saving.value = true
  try {
    if (editingId.value) await couponApi.update(editingId.value, payload)
    else await couponApi.create(payload)
    toast.success('保存成功')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

// ---------------- 领取记录 ----------------
const recordVisible = ref(false)
const recordCoupon = ref<Coupon | null>(null)
const records = useTable((p) => couponApi.records(recordCoupon.value!.id, p), { status: '' }, { immediate: false, pageSize: 10 })

function openRecords(row: Coupon) {
  recordCoupon.value = row
  recordVisible.value = true
  records.reset()
}
</script>

<template>
  <div class="page">
    <div class="card">
      <el-form :model="query" inline class="search-bar" @submit.prevent="search">
        <el-form-item label="名称">
          <el-input v-model="query.keyword" placeholder="优惠券名称" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable>
            <el-option v-for="o in COUPON_TYPE" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option label="发放中" :value="1" />
            <el-option label="已停发" :value="0" />
          </el-select>
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
          <el-button v-perm="'marketing:coupon:edit'" type="primary" :icon="'Plus'" @click="open()">新建优惠券</el-button>
        </div>
        <div class="toolbar__right">
          <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="load" /></el-tooltip>
        </div>
      </div>
      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="优惠券" min-width="280">
          <template #default="{ row }: { row: any }">
            <div class="coupon-cell">
              <div class="coupon-cell__face" :class="`is-${row.type}`">
                <b>{{ row.type === 'DISCOUNT' ? +(row.discount * 10).toFixed(2) : row.amount }}</b>
                <small>{{ row.type === 'DISCOUNT' ? '折' : '元' }}</small>
              </div>
              <div class="coupon-cell__body">
                <div class="coupon-cell__name">{{ row.name }}</div>
                <div class="text-secondary sub">{{ faceText(row) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }: { row: any }"><StatusTag :value="row.type" :options="COUPON_TYPE" :dot="false" /></template>
        </el-table-column>
        <el-table-column label="适用范围" width="110">
          <template #default="{ row }: { row: any }">
            {{ scopeMap[row.scope]?.label }}<span v-if="row.scope !== 'ALL'" class="text-secondary">（{{ row.scopeIds?.length || 0 }}）</span>
          </template>
        </el-table-column>
        <el-table-column label="领取 / 使用" min-width="160">
          <template #default="{ row }: { row: any }">
            <div class="sub">已领 {{ row.issued }} / {{ row.total || '不限' }} · 已用 {{ row.used }}</div>
            <el-progress v-if="row.total" :percentage="Math.min(100, Math.round((row.issued / row.total) * 100))" :stroke-width="6" :show-text="false" />
          </template>
        </el-table-column>
        <el-table-column label="有效期" min-width="230">
          <template #default="{ row }: { row: any }"><span class="sub">{{ validText(row) }}</span></template>
        </el-table-column>
        <el-table-column label="发放" width="80" align="center">
          <template #default="{ row }: { row: any }">
            <el-switch :model-value="row.status === 1" :disabled="!canEdit" @change="toggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button link type="primary" @click="openRecords(row)">领取记录</el-button>
            <el-button v-if="canEdit" link type="primary" @click="open(row)">编辑</el-button>
            <el-button v-if="canEdit" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState text="还没有优惠券" /></template>
      </el-table>
      <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="visible" :title="editingId ? '编辑优惠券' : '新建优惠券'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="32" show-word-limit placeholder="如：新人专享满 100 减 10" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio-button v-for="o in COUPON_TYPE" :key="o.value" :value="o.value">{{ o.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.type !== 'NO_THRESHOLD'" label="使用门槛" prop="threshold">
          <span class="inline-text">订单满</span>
          <el-input-number v-model="form.threshold" :min="0" :precision="2" controls-position="right" />
          <span class="inline-text">元可用</span>
          <span v-if="form.type === 'DISCOUNT'" class="form-tip">（0 表示不限）</span>
        </el-form-item>
        <el-form-item v-if="form.type !== 'DISCOUNT'" label="减免金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" controls-position="right" />
          <span class="inline-text">元</span>
        </el-form-item>
        <template v-else>
          <el-form-item label="折扣" prop="discountZhe">
            <el-input-number v-model="form.discountZhe" :min="0.1" :max="9.9" :step="0.5" :precision="1" controls-position="right" />
            <span class="inline-text">折</span>
          </el-form-item>
          <el-form-item label="最高减免">
            <el-input-number v-model="form.maxAmount" :min="0" :precision="2" controls-position="right" />
            <span class="inline-text">元</span>
            <span class="form-tip">（0 表示不限）</span>
          </el-form-item>
        </template>
        <el-form-item label="券面预览">
          <div class="coupon-preview">
            <span class="coupon-preview__face">{{ preview }}</span>
            <span class="coupon-preview__name">{{ form.name || '优惠券名称' }}</span>
          </div>
        </el-form-item>

        <el-form-item label="适用范围">
          <el-radio-group v-model="form.scope" @change="onScopeChange">
            <el-radio v-for="o in COUPON_SCOPE" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.scope === 'CATEGORY'" label="适用类目" prop="scopeIds">
          <el-tree-select
            v-model="form.scopeIds"
            :data="categories"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            multiple
            show-checkbox
            check-strictly
            collapse-tags
            collapse-tags-tooltip
            :max-collapse-tags="4"
            placeholder="选择一级或二级类目"
            class="w-full"
          />
        </el-form-item>
        <el-form-item v-if="form.scope === 'GOODS'" label="适用商品" prop="scopeIds">
          <div class="w-full">
            <el-button :icon="'Plus'" @click="pickerVisible = true">选择商品</el-button>
            <span class="form-tip" style="margin-left: 8px">已选 {{ form.scopeIds.length }} 件</span>
            <div v-if="form.scopeIds.length" class="picked">
              <el-tag v-for="gid in form.scopeIds" :key="gid" closable @close="removeGoods(gid)">{{ goodsNames[gid] || `商品 #${gid}` }}</el-tag>
            </div>
          </div>
        </el-form-item>

        <el-row>
          <el-col :span="12">
            <el-form-item label="发行量">
              <el-input-number v-model="form.total" :min="0" :precision="0" controls-position="right" />
              <div class="form-tip">0 表示不限量</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="每人限领">
              <el-input-number v-model="form.perLimit" :min="1" :max="99" :precision="0" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="有效期类型">
          <el-radio-group v-model="form.validType">
            <el-radio v-for="o in COUPON_VALID_TYPE" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.validType === 'FIXED'" label="有效期" prop="range">
          <el-date-picker
            v-model="form.range"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            :default-time="[new Date(2000, 0, 1, 0, 0, 0), new Date(2000, 0, 1, 23, 59, 59)]"
          />
        </el-form-item>
        <el-form-item v-else label="有效天数" prop="validDays">
          <span class="inline-text">领取后</span>
          <el-input-number v-model="form.validDays" :min="1" :max="365" :precision="0" controls-position="right" />
          <span class="inline-text">天内有效</span>
        </el-form-item>
        <el-form-item label="使用说明">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="立即发放">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <GoodsPicker v-model="pickerVisible" multiple :selected-ids="form.scopeIds" title="选择适用商品" @confirm="onPickGoods" />

    <!-- 领取记录 -->
    <el-drawer v-model="recordVisible" :title="`领取记录 · ${recordCoupon?.name || ''}`" size="760px">
      <div class="toolbar">
        <div class="toolbar__left">
          <el-radio-group v-model="records.query.status" size="small" @change="records.search">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button v-for="o in MEMBER_COUPON_STATUS" :key="o.value" :value="o.value">{{ o.label }}</el-radio-button>
          </el-radio-group>
        </div>
        <div class="toolbar__right text-secondary">共 {{ records.total.value }} 条</div>
      </div>
      <el-table v-loading="records.loading.value" :data="records.list.value" row-key="id">
        <el-table-column label="会员手机号" prop="memberMobile" min-width="130" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }: { row: any }"><StatusTag :value="row.status" :options="MEMBER_COUPON_STATUS" /></template>
        </el-table-column>
        <el-table-column label="有效期" min-width="200">
          <template #default="{ row }: { row: any }"><span class="sub">{{ formatTime(row.startAt, 'YYYY-MM-DD') }} ~ {{ formatTime(row.endAt, 'YYYY-MM-DD') }}</span></template>
        </el-table-column>
        <el-table-column label="使用时间" width="160">
          <template #default="{ row }: { row: any }">{{ formatTime(row.usedAt) }}</template>
        </el-table-column>
        <el-table-column label="领取时间" width="160">
          <template #default="{ row }: { row: any }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <template #empty><EmptyState text="暂无领取记录" /></template>
      </el-table>
      <Pagination
        :page="records.page.value"
        :size="records.size.value"
        :total="records.total.value"
        @page-change="records.onPageChange"
        @size-change="records.onSizeChange"
      />
      <div v-if="recordCoupon" class="text-secondary record-tip">面额：{{ faceText(recordCoupon) }} · 已领 {{ recordCoupon.issued }} · 已用 {{ recordCoupon.used }}</div>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.sub {
  font-size: 12px;
}
.inline-text {
  margin: 0 8px;
  color: var(--app-text-regular);
  font-size: 13px;
}
.coupon-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  &__face {
    --c: var(--app-danger);
    --c-soft: var(--app-danger-soft);
    position: relative;
    flex: none;
    width: 64px;
    height: 48px;
    display: flex;
    align-items: baseline;
    justify-content: center;
    padding-top: 10px;
    border-radius: var(--app-radius-sm);
    color: var(--c);
    background: var(--c-soft);
    border-left: 3px dashed var(--c);
    b {
      font-size: 20px;
    }
    small {
      font-size: 11px;
      margin-left: 1px;
    }
    &.is-DISCOUNT {
      --c: var(--app-warning);
      --c-soft: var(--app-warning-soft);
    }
    &.is-NO_THRESHOLD {
      --c: var(--app-purple);
      --c-soft: var(--app-purple-soft);
    }
  }
  &__name {
    font-weight: 500;
    color: var(--app-text);
  }
}
.coupon-preview {
  display: inline-flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 18px;
  border-radius: var(--app-radius);
  color: var(--el-color-white);
  background: linear-gradient(135deg, var(--app-danger), var(--app-warning));
  &__face {
    font-size: 16px;
    font-weight: 700;
  }
  &__name {
    font-size: 12px;
    opacity: 0.9;
  }
}
.picked {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
  max-height: 120px;
  overflow: auto;
}
.record-tip {
  margin-top: 12px;
  font-size: 12px;
}
</style>
