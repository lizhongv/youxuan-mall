<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { CascaderOption, FormInstance, FormRules } from 'element-plus'
import { brandApi, categoryApi, goodsApi } from '@/api/goods'
import type { Brand, Category, GoodsForm, Sku, SpecItem } from '@/types'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'
import { clone } from '@/utils/format'
import { confirm, toast } from '@/utils/message'
import PageHeader from '@/components/PageHeader.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import RichEditor from '@/components/RichEditor.vue'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const tabs = useTabsStore()

const id = computed(() => (route.params.id as string) || '')
const isEdit = computed(() => !!id.value)
const readonly = computed(() => !user.hasPerm('goods:goods:edit'))

/** 每个商品最多 3 个规格维度、每个维度最多 20 个值：避免笛卡尔积生成过多 SKU */
const MAX_SPECS = 3
const MAX_SPEC_VALUES = 20

const emptySku = (): Sku => ({ id: null, specValues: [], code: '', pic: '', price: 0, originalPrice: 0, stock: 0 })

function emptyForm(): GoodsForm {
  return {
    categoryId: '',
    brandId: '',
    name: '',
    subtitle: '',
    pic: '',
    album: [],
    detail: '',
    unit: '件',
    keywords: '',
    virtualSales: 0,
    sort: 0,
    status: 'OFF_SALE',
    isNew: 0,
    isHot: 0,
    isRecommend: 0,
    specItems: [],
    skus: [emptySku()],
  }
}

const form = reactive<GoodsForm>(emptyForm())
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const specMode = ref<'single' | 'multi'>('single')
/** 单规格时的唯一 SKU */
const single = reactive<Sku>(emptySku())
const categories = shallowRef<Category[]>([])
const brands = shallowRef<Brand[]>([])

const rules: FormRules = {
  categoryId: [{ required: true, message: '请选择商品类目', trigger: 'change' }],
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { max: 100, message: '不超过 100 个字符', trigger: 'blur' },
  ],
  subtitle: [{ max: 200, message: '不超过 200 个字符', trigger: 'blur' }],
  pic: [{ required: true, message: '请上传商品主图', trigger: 'change' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
}

// ---------------- 规格与 SKU ----------------
const newSpecName = ref('')
const valueInputs = reactive<Record<number, string>>({})

/** 已填写的 SKU 值按规格组合缓存，规格增删时保留已填值 */
const skuCache = new Map<string, Sku>()
const keyOf = (vals: string[]) => vals.join('\u0001')

const validSpecs = computed(() => form.specItems.filter((s) => s.name.trim() && s.values.length))

function cartesian(arrs: string[][]): string[][] {
  return arrs.reduce<string[][]>((acc, vals) => acc.flatMap((a) => vals.map((v) => [...a, v])), [[]])
}

function rebuildSkus() {
  form.skus.forEach((s) => skuCache.set(keyOf(s.specValues), s))
  const combos = validSpecs.value.length ? cartesian(validSpecs.value.map((s) => s.values)) : []
  form.skus = combos.map((vals) => {
    const cached = skuCache.get(keyOf(vals))
    return cached ? { ...cached, specValues: vals } : { ...emptySku(), specValues: vals }
  })
}

watch(
  () => validSpecs.value.map((s) => s.values.join('|')).join('||'),
  () => {
    if (specMode.value === 'multi') rebuildSkus()
  },
)

function addSpec() {
  const name = newSpecName.value.trim()
  if (!name) return
  if (form.specItems.some((s) => s.name === name)) return toast.warning('规格名已存在')
  if (form.specItems.length >= MAX_SPECS) return toast.warning(`最多添加 ${MAX_SPECS} 个规格`)
  form.specItems.push({ name, values: [] })
  newSpecName.value = ''
}

async function removeSpec(i: number) {
  const s = form.specItems[i]
  if (s.values.length && !(await confirm(`删除规格「${s.name}」将重新生成 SKU 列表，确定吗？`))) return
  form.specItems.splice(i, 1)
}

function addValue(i: number) {
  const v = (valueInputs[i] || '').trim()
  if (!v) return
  const spec = form.specItems[i]
  if (spec.values.includes(v)) return toast.warning('规格值重复')
  if (spec.values.length >= MAX_SPEC_VALUES) return toast.warning(`每个规格最多 ${MAX_SPEC_VALUES} 个值`)
  spec.values.push(v)
  valueInputs[i] = ''
}

function removeValue(i: number, j: number) {
  form.specItems[i].values.splice(j, 1)
}

function switchMode(mode: string | number | boolean | undefined) {
  if (mode === 'multi') {
    if (!form.specItems.length) form.specItems.push({ name: '颜色', values: [] })
    rebuildSkus()
  }
}

// 批量设置
const batch = reactive<{ price?: number; originalPrice?: number; stock?: number }>({})
function applyBatch() {
  if (batch.price === undefined && batch.originalPrice === undefined && batch.stock === undefined) {
    return toast.warning('请先填写要批量设置的值')
  }
  form.skus.forEach((s) => {
    if (batch.price !== undefined) s.price = batch.price
    if (batch.originalPrice !== undefined) s.originalPrice = batch.originalPrice
    if (batch.stock !== undefined) s.stock = batch.stock
  })
  toast.success(`已应用到 ${form.skus.length} 个 SKU`)
}

/** 第一列规格值合并单元格，便于阅读 */
function spanMethod({ rowIndex, columnIndex }: { rowIndex: number; columnIndex: number }) {
  if (columnIndex !== 0 || validSpecs.value.length < 2) return { rowspan: 1, colspan: 1 }
  const group = validSpecs.value.slice(1).reduce((n, s) => n * s.values.length, 1)
  return rowIndex % group === 0 ? { rowspan: group, colspan: 1 } : { rowspan: 0, colspan: 0 }
}

const skuSummary = computed(() => {
  const list = specMode.value === 'multi' ? form.skus : [single]
  const prices = list.map((s) => Number(s.price) || 0)
  return {
    count: list.length,
    stock: list.reduce((n, s) => n + (Number(s.stock) || 0), 0),
    min: prices.length ? Math.min(...prices) : 0,
    max: prices.length ? Math.max(...prices) : 0,
  }
})

// ---------------- 锚点导航 ----------------
const sections = [
  { key: 'base', label: '基本信息' },
  { key: 'images', label: '商品图片' },
  { key: 'spec', label: '规格库存' },
  { key: 'detail', label: '商品详情' },
  { key: 'sale', label: '销售设置' },
]
const activeSection = ref('base')
function scrollTo(key: string) {
  activeSection.value = key
  document.getElementById(`sec-${key}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// ---------------- 加载与保存 ----------------
onMounted(async () => {
  loading.value = true
  try {
    const [cats, bs] = await Promise.allSettled([categoryApi.tree(), brandApi.list()])
    if (cats.status === 'fulfilled') categories.value = cats.value || []
    if (bs.status === 'fulfilled') brands.value = bs.value || []
    if (isEdit.value) {
      const data = await goodsApi.detail(id.value)
      Object.assign(form, emptyForm(), data, {
        album: data.album || [],
        specItems: data.specItems || [],
        skus: data.skus?.length ? data.skus : [emptySku()],
      })
      if (form.specItems.length) {
        specMode.value = 'multi'
        form.skus.forEach((s) => skuCache.set(keyOf(s.specValues), s))
      } else {
        specMode.value = 'single'
        Object.assign(single, form.skus[0] || emptySku())
      }
    }
  } finally {
    loading.value = false
  }
})

function validateSkus(): string | null {
  if (specMode.value === 'single') {
    if (!(single.price > 0)) return '请填写销售价'
    return null
  }
  if (!validSpecs.value.length) return '请至少添加一个规格及规格值'
  const emptyNamed = form.specItems.find((s) => s.name.trim() && !s.values.length)
  if (emptyNamed) return `规格「${emptyNamed.name}」还没有规格值`
  const bad = form.skus.findIndex((s) => !(Number(s.price) > 0))
  if (bad >= 0) return `第 ${bad + 1} 个 SKU（${form.skus[bad].specValues.join(' / ')}）未填写销售价`
  return null
}

function buildPayload(): GoodsForm {
  const data = clone(form)
  if (specMode.value === 'single') {
    data.specItems = []
    data.skus = [{ ...clone(single), specValues: [] }]
  } else {
    data.specItems = validSpecs.value.map((s: SpecItem) => ({ name: s.name.trim(), values: [...s.values] }))
  }
  data.skus = data.skus.map((s) => ({
    ...s,
    price: Number(s.price) || 0,
    originalPrice: Number(s.originalPrice) || 0,
    stock: Math.max(0, Math.floor(Number(s.stock) || 0)),
  }))
  return data
}

async function save() {
  const ok = await formRef.value?.validate().catch(() => false)
  if (!ok) {
    scrollTo('base')
    return
  }
  const err = validateSkus()
  if (err) {
    scrollTo('spec')
    return toast.warning(err)
  }
  saving.value = true
  try {
    const payload = buildPayload()
    if (isEdit.value) await goodsApi.update(id.value, payload)
    else await goodsApi.create(payload)
    toast.success(isEdit.value ? '商品已保存' : '商品已发布')
    closeAndBack()
  } finally {
    saving.value = false
  }
}

function closeAndBack() {
  tabs.close(route.path, '')
  router.push('/goods/list')
}
</script>

<template>
  <div v-loading="loading" class="page goods-edit">
    <PageHeader :title="isEdit ? (readonly ? '商品详情' : '编辑商品') : '发布商品'" :subtitle="isEdit ? form.name : '填写商品信息并设置规格库存'" back="/goods/list">
      <template #extra>
        <div class="anchors">
          <button v-for="s in sections" :key="s.key" :class="{ 'is-active': activeSection === s.key }" @click="scrollTo(s.key)">{{ s.label }}</button>
        </div>
      </template>
    </PageHeader>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" :disabled="readonly" class="goods-form" @submit.prevent>
      <!-- 基本信息 -->
      <section id="sec-base" class="card">
        <h3 class="card-title"><span class="card-title__text">基本信息</span></h3>
        <el-row :gutter="24">
          <el-col :xs="24" :md="12">
            <el-form-item label="商品类目" prop="categoryId">
              <el-cascader
                v-model="form.categoryId"
                :options="(categories as unknown as CascaderOption[])"
                :props="{ value: 'id', label: 'name', children: 'children', emitPath: false }"
                placeholder="请选择类目"
                filterable
                clearable
                class="w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="品牌" prop="brandId">
              <el-select v-model="form.brandId" placeholder="请选择品牌（可选）" filterable clearable class="w-full">
                <el-option v-for="b in brands" :key="b.id" :label="b.name" :value="b.id">
                  <div class="flex-center">
                    <el-image v-if="b.logo" :src="b.logo" fit="contain" style="width: 20px; height: 20px" />
                    {{ b.name }}
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" maxlength="100" show-word-limit placeholder="建议包含品牌、品名、核心卖点" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="副标题" prop="subtitle">
              <el-input v-model="form.subtitle" maxlength="200" show-word-limit placeholder="一句话卖点，展示在商品名称下方" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="搜索关键词" prop="keywords">
              <el-input v-model="form.keywords" placeholder="多个关键词以空格分隔" />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :md="6">
            <el-form-item label="单位" prop="unit">
              <el-select v-model="form.unit" filterable allow-create default-first-option class="w-full">
                <el-option v-for="u in ['件', '个', '台', '套', '盒', '箱', '瓶', '袋', '双', '斤']" :key="u" :label="u" :value="u" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="12" :md="6">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="form.sort" :min="0" :max="99999" controls-position="right" class="w-full" />
            </el-form-item>
          </el-col>
        </el-row>
      </section>

      <!-- 图片 -->
      <section id="sec-images" class="card">
        <h3 class="card-title"><span class="card-title__text">商品图片</span></h3>
        <el-form-item label="商品主图" prop="pic">
          <ImageUpload v-model="form.pic" biz-type="goods" :size="110" tip="用于列表与分享，建议 800×800 正方形，≤ 5MB" />
        </el-form-item>
        <el-form-item label="轮播图">
          <ImageUpload v-model="form.album" multiple :limit="9" biz-type="goods" :size="110" tip="商品详情页顶部轮播，最多 9 张，建议 800×800。" />
        </el-form-item>
      </section>

      <!-- 规格库存 -->
      <section id="sec-spec" class="card">
        <h3 class="card-title">
          <span class="card-title__text">规格与库存</span>
          <span class="spec-summary">
            共 <b>{{ skuSummary.count }}</b> 个 SKU · 总库存 <b>{{ skuSummary.stock }}</b> · 价格
            <b>¥{{ skuSummary.min.toFixed(2) }}<template v-if="skuSummary.max !== skuSummary.min"> ~ ¥{{ skuSummary.max.toFixed(2) }}</template></b>
          </span>
        </h3>
        <el-form-item label="规格类型">
          <el-radio-group v-model="specMode" @change="switchMode">
            <el-radio-button value="single">统一规格</el-radio-button>
            <el-radio-button value="multi">多规格</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <template v-if="specMode === 'single'">
          <el-row :gutter="24">
            <el-col :xs="24" :sm="12" :lg="6">
              <el-form-item label="销售价" required>
                <el-input-number v-model="single.price" :min="0" :precision="2" :step="1" controls-position="right" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <el-form-item label="划线价">
                <el-input-number v-model="single.originalPrice" :min="0" :precision="2" controls-position="right" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <el-form-item label="库存" required>
                <el-input-number v-model="single.stock" :min="0" :precision="0" controls-position="right" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <el-form-item label="商品编码">
                <el-input v-model="single.code" placeholder="SKU 编码（可选）" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <template v-else>
          <el-form-item label="商品规格">
            <div class="spec-editor">
              <div v-for="(spec, i) in form.specItems" :key="i" class="spec-group">
                <div class="spec-group__head">
                  <el-input v-model="spec.name" placeholder="规格名" maxlength="20" class="spec-group__name" />
                  <el-button v-if="!readonly" link type="danger" :icon="'Delete'" @click="removeSpec(i)">删除规格</el-button>
                </div>
                <div class="spec-group__values">
                  <el-tag v-for="(v, j) in spec.values" :key="v" :closable="!readonly" size="large" effect="plain" @close="removeValue(i, j)">{{ v }}</el-tag>
                  <el-input
                    v-if="!readonly"
                    v-model="valueInputs[i]"
                    size="default"
                    placeholder="输入规格值后回车"
                    class="spec-group__input"
                    @keyup.enter="addValue(i)"
                    @blur="addValue(i)"
                  />
                </div>
              </div>
              <div v-if="!readonly && form.specItems.length < MAX_SPECS" class="spec-add">
                <el-input v-model="newSpecName" placeholder="新规格名，如：尺码" maxlength="20" style="width: 220px" @keyup.enter="addSpec" />
                <el-button :icon="'Plus'" @click="addSpec">添加规格</el-button>
                <span class="form-tip">最多 {{ MAX_SPECS }} 个规格，修改规格后 SKU 自动生成并保留已填写的值</span>
              </div>
            </div>
          </el-form-item>

          <el-form-item v-if="form.skus.length" label="SKU 列表">
            <div class="sku-wrap">
              <div v-if="!readonly" class="sku-batch">
                <span class="sku-batch__label">批量设置</span>
                <el-input-number v-model="batch.price" :min="0" :precision="2" placeholder="销售价" :controls="false" />
                <el-input-number v-model="batch.originalPrice" :min="0" :precision="2" placeholder="划线价" :controls="false" />
                <el-input-number v-model="batch.stock" :min="0" :precision="0" placeholder="库存" :controls="false" />
                <el-button type="primary" plain @click="applyBatch">应用到全部</el-button>
              </div>
              <el-table :data="form.skus" border :span-method="spanMethod" class="sku-table" max-height="520">
                <el-table-column v-for="(s, si) in validSpecs" :key="s.name" :label="s.name" min-width="100">
                  <template #default="{ row }"><b>{{ row.specValues[si] }}</b></template>
                </el-table-column>
                <el-table-column label="图片" width="86" align="center">
                  <template #default="{ row }">
                    <ImageUpload v-model="row.pic" biz-type="goods" :size="52" :disabled="readonly" class="sku-pic" />
                  </template>
                </el-table-column>
                <el-table-column label="销售价" min-width="140">
                  <template #header><span class="required">销售价</span></template>
                  <template #default="{ row }">
                    <el-input-number v-model="row.price" :min="0" :precision="2" :controls="false" class="w-full" />
                  </template>
                </el-table-column>
                <el-table-column label="划线价" min-width="140">
                  <template #default="{ row }">
                    <el-input-number v-model="row.originalPrice" :min="0" :precision="2" :controls="false" class="w-full" />
                  </template>
                </el-table-column>
                <el-table-column label="库存" min-width="120">
                  <template #header><span class="required">库存</span></template>
                  <template #default="{ row }">
                    <el-input-number v-model="row.stock" :min="0" :precision="0" :controls="false" class="w-full" />
                  </template>
                </el-table-column>
                <el-table-column label="编码" min-width="150">
                  <template #default="{ row }"><el-input v-model="row.code" placeholder="可选" /></template>
                </el-table-column>
              </el-table>
            </div>
          </el-form-item>
        </template>
      </section>

      <!-- 详情 -->
      <section id="sec-detail" class="card">
        <h3 class="card-title"><span class="card-title__text">商品详情</span></h3>
        <div v-if="readonly" class="detail-preview" v-html="form.detail" />
        <RichEditor v-else-if="!loading" v-model="form.detail" :height="460" placeholder="图文详情：支持插入图片、表格、标题等" />
      </section>

      <!-- 销售设置 -->
      <section id="sec-sale" class="card">
        <h3 class="card-title"><span class="card-title__text">销售设置</span></h3>
        <el-row :gutter="24">
          <el-col :xs="24" :md="12">
            <el-form-item label="上架状态">
              <el-radio-group v-model="form.status">
                <el-radio value="ON_SALE">立即上架</el-radio>
                <el-radio value="OFF_SALE">放入仓库</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="虚拟销量">
              <el-input-number v-model="form.virtualSales" :min="0" :precision="0" controls-position="right" />
              <span class="form-tip" style="margin-left: 10px">商城展示销量 = 实际销量 + 虚拟销量</span>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品标签">
              <div class="flag-cards">
                <label class="flag-card" :class="{ 'is-on': form.isNew }">
                  <el-checkbox v-model="form.isNew" :true-value="1" :false-value="0" />
                  <div><b>新品</b><span>出现在首页「新品上市」</span></div>
                </label>
                <label class="flag-card" :class="{ 'is-on': form.isHot }">
                  <el-checkbox v-model="form.isHot" :true-value="1" :false-value="0" />
                  <div><b>热卖</b><span>出现在首页「热卖推荐」</span></div>
                </label>
                <label class="flag-card" :class="{ 'is-on': form.isRecommend }">
                  <el-checkbox v-model="form.isRecommend" :true-value="1" :false-value="0" />
                  <div><b>推荐</b><span>出现在首页「为你推荐」</span></div>
                </label>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </section>
    </el-form>

    <div class="footer-bar card">
      <el-button @click="closeAndBack">{{ readonly ? '返回' : '取消' }}</el-button>
      <el-button v-if="!readonly" type="primary" :loading="saving" :icon="'Check'" @click="save">{{ isEdit ? '保存修改' : '发布商品' }}</el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.goods-edit {
  padding-bottom: 8px;
}
.goods-form {
  display: flex;
  flex-direction: column;
  gap: var(--app-gap);
  section {
    scroll-margin-top: 12px;
  }
}
.anchors {
  display: flex;
  gap: 4px;
  padding: 4px;
  border-radius: var(--app-radius);
  background: var(--app-card-bg-muted);
  flex-wrap: wrap;
  button {
    border: none;
    background: transparent;
    padding: 6px 12px;
    border-radius: var(--app-radius-sm);
    font-size: 13px;
    color: var(--app-text-regular);
    cursor: pointer;
    &:hover {
      color: var(--el-color-primary);
    }
    &.is-active {
      background: var(--app-card-bg);
      color: var(--el-color-primary);
      box-shadow: var(--app-shadow);
      font-weight: 600;
    }
  }
}
.spec-summary {
  font-size: 12px;
  font-weight: 400;
  color: var(--app-text-secondary);
  b {
    color: var(--app-text);
  }
}
.spec-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.spec-group {
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  overflow: hidden;
  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 10px 14px;
    background: var(--app-card-bg-muted);
    border-bottom: 1px solid var(--app-border);
  }
  &__name {
    width: 200px;
  }
  &__values {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    padding: 12px 14px;
  }
  &__input {
    width: 170px;
  }
}
.spec-add {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  .form-tip {
    margin: 0;
  }
}
.sku-wrap {
  width: 100%;
  min-width: 0;
}
.sku-batch {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 12px;
  margin-bottom: 10px;
  border-radius: var(--app-radius);
  background: var(--app-primary-soft);
  &__label {
    font-size: 13px;
    font-weight: 600;
    color: var(--el-color-primary);
    margin-right: 4px;
  }
  .el-input-number {
    width: 120px;
  }
}
.sku-table {
  :deep(.el-input-number .el-input__inner) {
    text-align: left;
  }
  .required::before {
    content: '*';
    color: var(--app-danger);
    margin-right: 4px;
  }
}
.sku-pic {
  display: flex;
  justify-content: center;
  :deep(.form-tip) {
    display: none;
  }
}
.detail-preview {
  padding: 12px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  min-height: 120px;
  :deep(img) {
    max-width: 100%;
  }
}
.flag-cards {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.flag-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 220px;
  padding: 12px 14px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1.5;
  div {
    display: flex;
    flex-direction: column;
  }
  b {
    color: var(--app-text);
  }
  span {
    font-size: 12px;
    color: var(--app-text-secondary);
  }
  &.is-on {
    border-color: var(--el-color-primary);
    background: var(--app-primary-soft);
  }
}
.footer-bar {
  position: sticky;
  bottom: 0;
  z-index: 5;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 20px;
  box-shadow: var(--app-shadow-pop);
}

@media (max-width: 768px) {
  .goods-form :deep(.el-form-item) {
    flex-direction: column;
    align-items: stretch;
    .el-form-item__label {
      justify-content: flex-start;
    }
  }
}
</style>
