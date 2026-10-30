<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { levelApi } from '@/api/member'
import type { ID, MemberLevel } from '@/types'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { confirmDelete, toast } from '@/utils/message'
import ImageUpload from '@/components/ImageUpload.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'

const user = useUserStore()
const canEdit = computed(() => user.hasPerm('member:level:edit'))

const { list, total, page, size, loading, load, onPageChange, onSizeChange } = useTable(levelApi.page, {}, { pageSize: 50 })

/** 按成长值升序展示，形成等级阶梯 */
const sorted = computed(() => [...list.value].sort((a, b) => a.minGrowth - b.minGrowth))

const visible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<ID>('')
const form = reactive({ name: '', minGrowth: 0, discountZhe: 10, icon: '', sort: 0 })
const rules: FormRules = {
  name: [{ required: true, message: '请输入等级名称', trigger: 'blur' }],
  discountZhe: [{ validator: (_r, v: number, cb) => (v > 0 && v <= 10 ? cb() : cb(new Error('折扣需在 0.1 ~ 10 之间'))), trigger: 'blur' }],
}

function open(row?: MemberLevel) {
  editingId.value = row?.id || ''
  Object.assign(form, {
    name: row?.name || '',
    minGrowth: row?.minGrowth ?? 0,
    discountZhe: row ? +(Number(row.discount) * 10).toFixed(2) : 10,
    icon: row?.icon || '',
    sort: row?.sort ?? 0,
  })
  visible.value = true
}

async function submit() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  const payload = { name: form.name.trim(), minGrowth: form.minGrowth, discount: +(form.discountZhe / 10).toFixed(2), icon: form.icon, sort: form.sort }
  saving.value = true
  try {
    if (editingId.value) await levelApi.update(editingId.value, payload)
    else await levelApi.create(payload)
    toast.success('保存成功')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: MemberLevel) {
  if (await confirmDelete(row.name, () => levelApi.remove(row.id))) load()
}

const zhe = (d: number) => (Number(d) >= 1 ? '无折扣' : `${+(Number(d) * 10).toFixed(2)} 折`)
</script>

<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <div class="toolbar__left">
          <el-button v-perm="'member:level:edit'" type="primary" :icon="'Plus'" @click="open()">新增等级</el-button>
          <span class="text-secondary tip">会员成长值达到门槛后自动升级，下单享受对应折扣</span>
        </div>
        <div class="toolbar__right">
          <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="load" /></el-tooltip>
        </div>
      </div>

      <div v-loading="loading" class="levels">
        <div v-for="(l, i) in sorted" :key="l.id" class="level" :style="{ '--i': i }">
          <div class="level__icon">
            <el-image v-if="l.icon" :src="l.icon" fit="contain" />
            <el-icon v-else><Medal /></el-icon>
          </div>
          <div class="level__name">{{ l.name }}</div>
          <div class="level__growth">成长值 ≥ {{ l.minGrowth }}</div>
          <div class="level__discount">{{ zhe(l.discount) }}</div>
          <div v-if="canEdit" class="level__actions">
            <el-button link type="primary" @click="open(l)">编辑</el-button>
            <el-button link type="danger" @click="remove(l)">删除</el-button>
          </div>
        </div>
        <EmptyState v-if="!loading && !list.length" class="levels__empty" text="还没有会员等级" />
      </div>
      <Pagination v-if="total > size" :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <el-dialog v-model="visible" :title="editingId ? '编辑等级' : '新增等级'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="等级名称" prop="name">
          <el-input v-model="form.name" maxlength="16" show-word-limit placeholder="如：黄金会员" />
        </el-form-item>
        <el-form-item label="成长值门槛">
          <el-input-number v-model="form.minGrowth" :min="0" :precision="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="会员折扣" prop="discountZhe">
          <el-input-number v-model="form.discountZhe" :min="0.1" :max="10" :step="0.5" :precision="1" controls-position="right" />
          <span class="form-tip" style="margin-left: 8px">折（10 表示不打折）</span>
        </el-form-item>
        <el-form-item label="等级图标">
          <ImageUpload v-model="form.icon" biz-type="level" :size="72" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.tip {
  font-size: 12px;
}
.levels {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
  min-height: 160px;
  &__empty {
    grid-column: 1 / -1;
  }
}
.level {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 22px 16px 14px;
  border-radius: var(--app-radius-lg);
  border: 1px solid var(--app-border);
  background: linear-gradient(180deg, var(--app-warning-soft), var(--app-card-bg) 60%);
  transition:
    transform 0.2s,
    box-shadow 0.2s;
  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--app-shadow-hover);
  }
  &__icon {
    width: 52px;
    height: 52px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 26px;
    color: var(--app-warning);
    background: var(--app-card-bg);
    box-shadow: var(--app-shadow);
    .el-image {
      width: 36px;
      height: 36px;
    }
  }
  &__name {
    margin-top: 4px;
    font-size: 16px;
    font-weight: 700;
    color: var(--app-text);
  }
  &__growth {
    font-size: 12px;
    color: var(--app-text-secondary);
  }
  &__discount {
    padding: 2px 10px;
    border-radius: 10px;
    font-size: 12px;
    font-weight: 600;
    color: var(--app-danger);
    background: var(--app-danger-soft);
  }
  &__actions {
    margin-top: 4px;
  }
}
</style>
