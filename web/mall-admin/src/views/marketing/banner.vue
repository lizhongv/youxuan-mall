<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowRef } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { bannerApi } from '@/api/marketing'
import { categoryApi } from '@/api/goods'
import type { Banner, Category, GoodsItem, ID, LinkType, Switch } from '@/types'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { BANNER_POSITION, LINK_TYPE, toMap } from '@/utils/constants'
import { confirmDelete, toast } from '@/utils/message'
import ImageUpload from '@/components/ImageUpload.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'
import GoodsPicker from '@/components/GoodsPicker.vue'

const user = useUserStore()
const canEdit = computed(() => user.hasPerm('marketing:banner:edit'))
const linkMap = toMap(LINK_TYPE)
const positionMap = toMap(BANNER_POSITION)

const { query, list, total, page, size, loading, load, search, reset, onPageChange, onSizeChange } = useTable(bannerApi.page, {
  keyword: '',
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

async function toggleStatus(row: Banner) {
  const status = (row.status ? 0 : 1) as Switch
  await bannerApi.update(row.id, { ...row, status })
  row.status = status
  list.value = [...list.value]
  toast.success(status ? '已上线' : '已下线')
}

async function remove(row: Banner) {
  if (await confirmDelete(row.title, () => bannerApi.remove(row.id))) load()
}

// ---------------- 表单 ----------------
const visible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<ID>('')
const pickerVisible = ref(false)
const linkGoodsName = ref('')
const form = reactive({ title: '', image: '', linkType: 'NONE' as LinkType, linkValue: '', position: 'HOME', sort: 0, status: 1 as Switch })

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  image: [{ required: true, message: '请上传轮播图', trigger: 'change' }],
  linkValue: [
    {
      validator: (_r, v: string, cb) => {
        if (form.linkType === 'NONE') return cb()
        if (!v) return cb(new Error('请设置跳转目标'))
        if (form.linkType === 'URL' && !/^(https?:\/\/|\/)/.test(v)) return cb(new Error('请输入以 http(s):// 或 / 开头的地址'))
        cb()
      },
      trigger: 'change',
    },
  ],
}

function open(row?: Banner) {
  editingId.value = row?.id || ''
  linkGoodsName.value = ''
  Object.assign(form, {
    title: row?.title || '',
    image: row?.image || '',
    linkType: row?.linkType || 'NONE',
    linkValue: row?.linkValue || '',
    position: row?.position || 'HOME',
    sort: row?.sort ?? 0,
    status: row?.status ?? 1,
  })
  visible.value = true
  setTimeout(() => formRef.value?.clearValidate())
}

function onLinkTypeChange() {
  form.linkValue = ''
  linkGoodsName.value = ''
}

function onPick(rows: GoodsItem[]) {
  form.linkValue = rows[0]?.id || ''
  linkGoodsName.value = rows[0]?.name || ''
  formRef.value?.validateField('linkValue')
}

function linkText(b: Banner) {
  if (b.linkType === 'NONE') return '-'
  return `${linkMap[b.linkType]?.label}：${b.linkValue}`
}

async function submit() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  saving.value = true
  try {
    const payload = { ...form, linkValue: form.linkType === 'NONE' ? '' : form.linkValue }
    if (editingId.value) await bannerApi.update(editingId.value, payload)
    else await bannerApi.create(payload)
    toast.success('保存成功')
    visible.value = false
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
        <el-form-item label="标题">
          <el-input v-model="query.keyword" placeholder="轮播标题" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option label="上线" :value="1" />
            <el-option label="下线" :value="0" />
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
          <el-button v-perm="'marketing:banner:edit'" type="primary" :icon="'Plus'" @click="open()">新增轮播</el-button>
        </div>
        <div class="toolbar__right">
          <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="load" /></el-tooltip>
        </div>
      </div>
      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="图片" width="200">
          <template #default="{ row }: { row: any }">
            <el-image :src="row.image" fit="cover" class="banner-img" :preview-src-list="[row.image]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column label="位置" width="110">
          <template #default="{ row }: { row: any }">{{ positionMap[row.position]?.label || row.position }}</template>
        </el-table-column>
        <el-table-column label="跳转" min-width="200" show-overflow-tooltip>
          <template #default="{ row }: { row: any }"><span class="text-secondary">{{ linkText(row) }}</span></template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="上线" width="80" align="center">
          <template #default="{ row }: { row: any }">
            <el-switch :model-value="row.status === 1" :disabled="!canEdit" @change="toggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column v-if="canEdit" label="操作" width="120" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button link type="primary" @click="open(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState text="暂无轮播广告" /></template>
      </el-table>
      <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <el-dialog v-model="visible" :title="editingId ? '编辑轮播' : '新增轮播'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="轮播图" prop="image">
          <div>
            <div v-if="form.image" class="banner-preview">
              <el-image :src="form.image" fit="cover" />
              <el-button class="banner-preview__del" circle size="small" :icon="'Delete'" @click="form.image = ''" />
            </div>
            <ImageUpload v-else v-model="form.image" biz-type="banner" :size="120" />
            <div class="form-tip">建议尺寸 750×340 像素，JPG / PNG，≤ 5MB；重要内容请居中放置</div>
          </div>
        </el-form-item>
        <el-form-item label="展示位置">
          <el-select v-model="form.position" style="width: 200px">
            <el-option v-for="p in BANNER_POSITION" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="链接类型">
          <el-radio-group v-model="form.linkType" @change="onLinkTypeChange">
            <el-radio-button v-for="o in LINK_TYPE" :key="o.value" :value="o.value">{{ o.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.linkType === 'GOODS'" label="跳转商品" prop="linkValue">
          <el-input :model-value="linkGoodsName || (form.linkValue ? `商品 #${form.linkValue}` : '')" readonly placeholder="点击选择商品" @click="pickerVisible = true">
            <template #append><el-button @click="pickerVisible = true">选择</el-button></template>
          </el-input>
        </el-form-item>
        <el-form-item v-else-if="form.linkType === 'CATEGORY'" label="跳转类目" prop="linkValue">
          <el-cascader
            v-model="form.linkValue"
            :options="(categories as any)"
            :props="{ value: 'id', label: 'name', children: 'children', emitPath: false, checkStrictly: true }"
            placeholder="选择类目"
            filterable
            class="w-full"
          />
        </el-form-item>
        <el-form-item v-else-if="form.linkType === 'URL'" label="链接地址" prop="linkValue">
          <el-input v-model="form.linkValue" placeholder="https://… 或站内路径 /pages/…" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
          <span class="form-tip" style="margin-left: 10px">数字越小越靠前</span>
        </el-form-item>
        <el-form-item label="上线">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <GoodsPicker v-model="pickerVisible" title="选择跳转商品" @confirm="onPick" />
  </div>
</template>

<style scoped lang="scss">
.banner-img {
  width: 165px;
  height: 75px;
  border-radius: var(--app-radius-sm);
  border: 1px solid var(--app-border);
}
.banner-preview {
  position: relative;
  width: 375px;
  max-width: 100%;
  aspect-ratio: 750 / 340;
  border-radius: var(--app-radius);
  overflow: hidden;
  border: 1px solid var(--app-border);
  .el-image {
    width: 100%;
    height: 100%;
  }
  &__del {
    position: absolute;
    top: 8px;
    right: 8px;
  }
}
</style>
