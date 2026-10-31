<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { freightApi } from '@/api/trade'
import type { ChargeType, FreightFree, FreightRule, FreightTemplate, Switch } from '@/types'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { clone, formatTime, money } from '@/utils/format'
import { CHARGE_TYPE, ENABLE_STATUS, PROVINCES } from '@/utils/constants'
import { confirm, confirmDelete, toast } from '@/utils/message'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'
import ProvincePicker from './ProvincePicker.vue'

const user = useUserStore()
const canEdit = computed(() => user.hasPerm('trade:freight:edit'))

const { query, list, total, page, size, loading, load, search, reset, onPageChange, onSizeChange } = useTable(freightApi.page, {
  keyword: '',
  chargeType: '',
  status: '' as Switch | '',
})

// ---------------- 文案 ----------------
const unitOf = (t: ChargeType) => (t === 'BY_WEIGHT' ? 'kg' : '件')
const firstLabel = (t: ChargeType) => (t === 'BY_WEIGHT' ? '首重' : '首件')
const nextLabel = (t: ChargeType) => (t === 'BY_WEIGHT' ? '续重' : '续件')
const num = (v: number | string | null | undefined) => Number(v ?? 0)

function ruleText(r: FreightRule, t: ChargeType) {
  const u = unitOf(t)
  const first = `${firstLabel(t)} ${num(r.firstUnit)}${u} ${money(r.firstFee)}`
  if (!num(r.nextUnit)) return `${first}，超出不加收`
  return `${first}，每${nextLabel(t).slice(1)} ${num(r.nextUnit)}${u} +${money(r.nextFee)}`
}

function freeText(f: FreightFree) {
  const parts: string[] = []
  if (num(f.minCount) > 0) parts.push(`满 ${f.minCount} 件`)
  if (num(f.minAmount) > 0) parts.push(`满 ${money(f.minAmount)}`)
  return parts.join(' 或 ') + ' 包邮'
}

function regionText(provinces: string[], emptyText: string, max = 4) {
  if (!provinces?.length) return emptyText
  if (provinces.length <= max) return provinces.join('、')
  return `${provinces.slice(0, max).join('、')} 等 ${provinces.length} 个地区`
}

const defaultRule = (t: FreightTemplate) => t.rules?.find((r) => !r.provinces?.length)

// ---------------- 列表操作 ----------------
async function remove(row: FreightTemplate) {
  if (await confirmDelete(row.name, () => freightApi.remove(row.id!))) load()
}

/** 启停 / 设默认：接口为整体替换，带上完整规则提交 */
async function quickUpdate(row: FreightTemplate, patch: Partial<FreightTemplate>, okText: string) {
  const full = row.rules ? row : await freightApi.detail(row.id!)
  await freightApi.update(row.id!, toPayload({ ...clone(full), ...patch }))
  toast.success(okText)
  load()
}

async function toggleStatus(row: FreightTemplate) {
  const status = (row.status ? 0 : 1) as Switch
  if (!status && row.isDefault && !(await confirm('停用默认模板后，未指定模板的商品将按系统参数中的基础运费计算，确定停用吗？'))) return
  await quickUpdate(row, { status }, status ? '已启用' : '已停用')
}

async function setDefault(row: FreightTemplate) {
  if (!(await confirm(`设为默认后，未指定运费模板的商品都将按「${row.name}」计算运费，确定吗？`, '设为默认', 'info'))) return
  await quickUpdate(row, { isDefault: 1, status: 1 }, '已设为默认模板')
}

// ---------------- 编辑抽屉 ----------------
const drawer = ref(false)
const saving = ref(false)
const detailLoading = ref(false)
const editingId = ref<string>('')
const formRef = ref<FormInstance>()

function emptyForm(): FreightTemplate {
  return {
    name: '',
    chargeType: 'BY_COUNT',
    isDefault: 0,
    status: 1,
    rules: [{ provinces: [], firstUnit: 1, firstFee: 0, nextUnit: 1, nextFee: 0 }],
    frees: [],
  }
}
const form = reactive<FreightTemplate>(emptyForm())

const formRules: FormRules = {
  name: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
    { max: 64, message: '最多 64 个字', trigger: 'blur' },
  ],
}

/** 默认地区规则固定在第一行 */
function normalize(t: FreightTemplate): FreightTemplate {
  const rules = (t.rules || []).map((r) => ({
    provinces: [...(r.provinces || [])],
    firstUnit: num(r.firstUnit),
    firstFee: num(r.firstFee),
    nextUnit: num(r.nextUnit),
    nextFee: num(r.nextFee),
  }))
  const def = rules.find((r) => !r.provinces.length)
  const others = rules.filter((r) => r !== def)
  return {
    ...t,
    isDefault: (num(t.isDefault) ? 1 : 0) as Switch,
    status: (num(t.status) ? 1 : 0) as Switch,
    rules: [def || { provinces: [], firstUnit: 1, firstFee: 0, nextUnit: 1, nextFee: 0 }, ...others],
    frees: (t.frees || []).map((f) => ({ provinces: [...(f.provinces || [])], minCount: num(f.minCount), minAmount: num(f.minAmount) })),
  }
}

async function openEdit(row?: FreightTemplate) {
  editingId.value = row?.id || ''
  Object.assign(form, emptyForm())
  drawer.value = true
  setTimeout(() => formRef.value?.clearValidate())
  if (!row) return
  detailLoading.value = true
  try {
    Object.assign(form, normalize(await freightApi.detail(row.id!)))
  } catch {
    drawer.value = false
  } finally {
    detailLoading.value = false
  }
}

const isWeight = computed(() => form.chargeType === 'BY_WEIGHT')
const unitPrecision = computed(() => (isWeight.value ? 3 : 0))
const unitMin = computed(() => (isWeight.value ? 0.001 : 1))

function onChargeTypeChange() {
  // 切换计费方式时把单位取整到合法值
  form.rules.forEach((r) => {
    if (!isWeight.value) {
      r.firstUnit = Math.max(1, Math.round(r.firstUnit))
      r.nextUnit = Math.max(0, Math.round(r.nextUnit))
    }
  })
}

// 地区选择
const picker = reactive({ visible: false, kind: 'rule' as 'rule' | 'free', index: -1, selected: [] as string[], disabled: [] as string[] })

function usedByRules(except: number) {
  return form.rules.flatMap((r, i) => (i === except ? [] : r.provinces))
}

function pickRegion(kind: 'rule' | 'free', index: number) {
  const list = kind === 'rule' ? form.rules : form.frees
  picker.kind = kind
  picker.index = index
  picker.selected = index >= 0 ? [...list[index].provinces] : []
  picker.disabled = kind === 'rule' ? usedByRules(index) : []
  picker.visible = true
}

function onPicked(provinces: string[]) {
  if (picker.kind === 'rule') {
    if (picker.index >= 0) form.rules[picker.index].provinces = provinces
    else {
      const d = form.rules[0]
      form.rules.push({ provinces, firstUnit: d.firstUnit, firstFee: d.firstFee, nextUnit: d.nextUnit, nextFee: d.nextFee })
    }
  } else if (picker.index >= 0) form.frees[picker.index].provinces = provinces
  else form.frees.push({ provinces, minCount: 0, minAmount: 0 })
}

const remainingProvinces = computed(() => PROVINCES.length - usedByRules(-1).length)

function addFreeAll() {
  if (form.frees.some((f) => !f.provinces.length)) return toast.warning('已有「全部地区」的包邮条件')
  form.frees.push({ provinces: [], minCount: 0, minAmount: 99 })
}

function validateBody(): string | null {
  for (let i = 0; i < form.rules.length; i++) {
    const r = form.rules[i]
    const where = i === 0 ? '默认地区' : `第 ${i + 1} 条规则`
    if (i > 0 && !r.provinces.length) return `${where}还没有选择地区`
    if (!(num(r.firstUnit) > 0)) return `${where}的${firstLabel(form.chargeType)}必须大于 0`
    if (num(r.firstFee) < 0 || num(r.nextFee) < 0 || num(r.nextUnit) < 0) return `${where}的费用不能为负数`
  }
  for (let i = 0; i < form.frees.length; i++) {
    const f = form.frees[i]
    if (!(num(f.minCount) > 0) && !(num(f.minAmount) > 0)) return `第 ${i + 1} 条包邮条件需设置「满 N 件」或「满 X 元」至少一项`
  }
  return null
}

function toPayload(t: FreightTemplate): FreightTemplate {
  const n = normalize(t)
  return {
    name: n.name.trim(),
    chargeType: n.chargeType,
    isDefault: n.isDefault,
    status: n.status,
    rules: n.rules,
    frees: n.frees.map((f) => ({ ...f, minCount: Math.max(0, Math.floor(f.minCount)) })),
  }
}

async function save() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  const err = validateBody()
  if (err) return toast.warning(err)
  saving.value = true
  try {
    const payload = toPayload(form)
    if (editingId.value) await freightApi.update(editingId.value, payload)
    else await freightApi.create(payload)
    toast.success(editingId.value ? '运费模板已保存' : '运费模板已创建')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="card">
      <el-form :model="query" inline class="search-bar" @submit.prevent="search">
        <el-form-item label="模板名称">
          <el-input v-model="query.keyword" placeholder="请输入" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="计费方式">
          <el-select v-model="query.chargeType" placeholder="全部" clearable>
            <el-option v-for="o in CHARGE_TYPE" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option v-for="o in ENABLE_STATUS" :key="o.value" :label="o.label" :value="o.value" />
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
          <el-button v-perm="'trade:freight:edit'" type="primary" :icon="'Plus'" @click="openEdit()">新建模板</el-button>
          <span class="text-secondary tip">商品未指定模板时使用「默认模板」；没有默认模板时按系统参数的基础运费 + 满额包邮计算。</span>
        </div>
        <div class="toolbar__right">
          <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="load" /></el-tooltip>
        </div>
      </div>
      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="模板名称" min-width="170">
          <template #default="{ row }: { row: any }">
            <div class="tpl-name">
              <span class="link" @click="openEdit(row)">{{ row.name }}</span>
              <el-tag v-if="row.isDefault" size="small" type="primary" effect="dark" round>默认</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="计费方式" width="100">
          <template #default="{ row }: { row: any }"><StatusTag :value="row.chargeType" :options="CHARGE_TYPE" :dot="false" /></template>
        </el-table-column>
        <el-table-column label="运费规则" min-width="240">
          <template #default="{ row }: { row: any }">
            <div v-if="defaultRule(row)" class="sub">默认：{{ ruleText(defaultRule(row)!, row.chargeType) }}</div>
            <div v-if="(row.rules?.length || 0) > 1" class="text-secondary sub">另有 {{ row.rules.length - 1 }} 条指定地区规则</div>
          </template>
        </el-table-column>
        <el-table-column label="包邮条件" min-width="180">
          <template #default="{ row }: { row: any }">
            <template v-if="row.frees?.length">
              <div v-for="(f, i) in row.frees.slice(0, 2)" :key="i" class="sub">{{ regionText(f.provinces, '全部地区', 2) }}：{{ freeText(f) }}</div>
              <div v-if="row.frees.length > 2" class="text-secondary sub">等 {{ row.frees.length }} 条</div>
            </template>
            <span v-else class="text-secondary">不包邮</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }: { row: any }">
            <el-switch :model-value="row.status === 1" :disabled="!canEdit" inline-prompt active-text="启用" inactive-text="停用" @change="toggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="110">
          <template #default="{ row }: { row: any }"><span class="text-secondary sub" :title="formatTime(row.createdAt)">{{ formatTime(row.createdAt, 'YYYY-MM-DD') }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button link type="primary" @click="openEdit(row)">{{ canEdit ? '编辑' : '查看' }}</el-button>
            <el-button v-if="canEdit && !row.isDefault" link type="primary" @click="setDefault(row)">设为默认</el-button>
            <el-button v-perm="'trade:freight:edit'" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState v-if="!loading" text="还没有运费模板">
            <el-button v-perm="'trade:freight:edit'" type="primary" size="small" @click="openEdit()">新建运费模板</el-button>
          </EmptyState>
          <div v-else class="table-placeholder" />
        </template>
      </el-table>
      <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <el-drawer v-model="drawer" :title="editingId ? (canEdit ? '编辑运费模板' : '运费模板') : '新建运费模板'" size="min(900px, 100%)" class="freight-drawer">
      <div v-loading="detailLoading">
        <el-form ref="formRef" :model="form" :rules="formRules" label-width="96px" :disabled="!canEdit" @submit.prevent>
          <el-form-item label="模板名称" prop="name">
            <el-input v-model="form.name" maxlength="64" show-word-limit placeholder="如：普通快递、大件物流" style="max-width: 420px" />
          </el-form-item>
          <el-form-item label="计费方式">
            <el-radio-group v-model="form.chargeType" @change="onChargeTypeChange">
              <el-radio-button value="BY_COUNT">按件数</el-radio-button>
              <el-radio-button value="BY_WEIGHT">按重量</el-radio-button>
            </el-radio-group>
            <div class="form-tip block">{{ isWeight ? '按订单商品总重量（SKU 重量 × 件数，kg）计费，请在商品 SKU 中填写重量。' : '按订单商品总件数计费。' }}</div>
          </el-form-item>
          <el-form-item label="模板设置">
            <el-checkbox v-model="form.isDefault" :true-value="1" :false-value="0">设为默认模板</el-checkbox>
            <el-checkbox v-model="form.status" :true-value="1" :false-value="0">启用</el-checkbox>
            <div class="form-tip block">默认模板只能有一个，设置后其他模板自动取消默认；停用的模板视同不存在，引用它的商品回退到默认模板。</div>
          </el-form-item>

          <div class="section-title">
            <span>运费规则</span>
            <span class="text-secondary">「默认地区」适用于未单独设置的所有地区；同一省份只能出现在一条规则中</span>
          </div>
          <el-table :data="form.rules" border class="rule-table">
            <el-table-column label="配送地区" min-width="220">
              <template #default="{ row, $index }: { row: any; $index: number }">
                <div v-if="$index === 0" class="region">
                  <b>默认地区</b>
                  <span class="text-secondary sub">全国其他地区</span>
                </div>
                <div v-else class="region">
                  <span class="region__text" :title="row.provinces.join('、')">{{ regionText(row.provinces, '未选择地区', 6) }}</span>
                  <el-button v-if="canEdit" link type="primary" size="small" @click="pickRegion('rule', $index)">编辑</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="`${firstLabel(form.chargeType)}（${unitOf(form.chargeType)}）`" width="128">
              <template #default="{ row }: { row: any }">
                <el-input-number v-model="row.firstUnit" :min="unitMin" :precision="unitPrecision" :controls="false" class="w-full" />
              </template>
            </el-table-column>
            <el-table-column label="首费（元）" width="118">
              <template #default="{ row }: { row: any }">
                <el-input-number v-model="row.firstFee" :min="0" :precision="2" :controls="false" class="w-full" />
              </template>
            </el-table-column>
            <el-table-column :label="`${nextLabel(form.chargeType)}（${unitOf(form.chargeType)}）`" width="128">
              <template #default="{ row }: { row: any }">
                <el-input-number v-model="row.nextUnit" :min="0" :precision="unitPrecision" :controls="false" class="w-full" />
              </template>
            </el-table-column>
            <el-table-column label="续费（元）" width="118">
              <template #default="{ row }: { row: any }">
                <el-input-number v-model="row.nextFee" :min="0" :precision="2" :controls="false" :disabled="!row.nextUnit" class="w-full" />
              </template>
            </el-table-column>
            <el-table-column v-if="canEdit" label="操作" width="70" align="center">
              <template #default="{ $index }: { $index: number }">
                <el-button v-if="$index > 0" link type="danger" @click="form.rules.splice($index, 1)">删除</el-button>
                <span v-else class="text-secondary">-</span>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="canEdit" class="table-actions">
            <el-button :icon="'Plus'" :disabled="remainingProvinces <= 0" @click="pickRegion('rule', -1)">为指定地区设置运费</el-button>
            <span class="form-tip">{{ nextLabel(form.chargeType) }}填 0 表示超出{{ firstLabel(form.chargeType) }}后不再加收</span>
          </div>

          <div class="section-title">
            <span>指定条件包邮</span>
            <span class="text-secondary">满足任一条件即包邮；金额按会员折扣后、用券前计算</span>
          </div>
          <el-table v-if="form.frees.length" :data="form.frees" border class="rule-table">
            <el-table-column label="包邮地区" min-width="220">
              <template #default="{ row, $index }: { row: any; $index: number }">
                <div class="region">
                  <span class="region__text" :title="row.provinces.join('、')">{{ regionText(row.provinces, '全部地区', 6) }}</span>
                  <el-button v-if="canEdit" link type="primary" size="small" @click="pickRegion('free', $index)">编辑</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="包邮条件" min-width="330">
              <template #default="{ row }: { row: any }">
                <div class="free-cond">
                  <span>满</span>
                  <el-input-number v-model="row.minCount" :min="0" :precision="0" :controls="false" class="free-cond__num" />
                  <span>件</span>
                  <span class="text-secondary">或</span>
                  <span>满</span>
                  <el-input-number v-model="row.minAmount" :min="0" :precision="2" :controls="false" class="free-cond__num" />
                  <span>元</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column v-if="canEdit" label="操作" width="70" align="center">
              <template #default="{ $index }: { $index: number }">
                <el-button link type="danger" @click="form.frees.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="canEdit" class="table-actions">
            <el-button :icon="'Plus'" @click="addFreeAll">全部地区包邮条件</el-button>
            <el-button :icon="'Plus'" @click="pickRegion('free', -1)">指定地区包邮条件</el-button>
            <span class="form-tip">两项都填 0 视为未设置；不需要包邮时留空即可</span>
          </div>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="drawer = false">{{ canEdit ? '取消' : '关闭' }}</el-button>
        <el-button v-if="canEdit" type="primary" :loading="saving" :icon="'Check'" @click="save">保存</el-button>
      </template>
    </el-drawer>

    <ProvincePicker
      v-model="picker.visible"
      :selected="picker.selected"
      :disabled="picker.disabled"
      :title="picker.kind === 'rule' ? '选择配送地区' : '选择包邮地区'"
      @confirm="onPicked"
    />
  </div>
</template>

<style scoped lang="scss">
.tip {
  font-size: 12px;
}
.sub {
  font-size: 12px;
  line-height: 1.7;
}
.link {
  cursor: pointer;
  color: var(--app-text);
  font-weight: 500;
  &:hover {
    color: var(--el-color-primary);
  }
}
.tpl-name {
  display: flex;
  align-items: center;
  gap: 8px;
}
.block {
  width: 100%;
}
.section-title {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 10px;
  margin: 24px 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text);
  .text-secondary {
    font-size: 12px;
    font-weight: 400;
  }
}
.rule-table {
  :deep(.el-input-number .el-input__inner) {
    text-align: left;
  }
}
.region {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  &__text {
    flex: 1;
    min-width: 0;
    line-height: 1.5;
    color: var(--app-text);
  }
}
.table-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
  .form-tip {
    margin: 0;
  }
}
.free-cond {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  &__num {
    width: 96px;
  }
}
</style>
