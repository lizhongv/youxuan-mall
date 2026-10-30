<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { configApi } from '@/api/system'
import type { SysConfig } from '@/types'
import { useUserStore } from '@/stores/user'
import { toast } from '@/utils/message'
import EmptyState from '@/components/EmptyState.vue'

const user = useUserStore()
const canEdit = computed(() => user.hasPerm('system:config:edit'))

const list = ref<SysConfig[]>([])
const values = ref<Record<string, string>>({})
const original = ref<Record<string, string>>({})
const loading = ref(false)
const saving = ref(false)
const activeGroup = ref('')

/**
 * 契约未定义分组字段：按 configKey 的第一段前缀分组（如 mall.name → mall、order.pay-timeout → order），
 * 已知前缀给出中文名，其余直接显示前缀。
 */
const GROUP_LABELS: Record<string, { label: string; icon: string }> = {
  mall: { label: '商城信息', icon: 'Shop' },
  order: { label: '交易设置', icon: 'Tickets' },
  trade: { label: '交易设置', icon: 'Tickets' },
  freight: { label: '运费设置', icon: 'Van' },
  member: { label: '会员设置', icon: 'User' },
  points: { label: '积分成长', icon: 'Coin' },
  growth: { label: '积分成长', icon: 'Coin' },
  aftersale: { label: '售后设置', icon: 'RefreshLeft' },
  upload: { label: '上传设置', icon: 'Upload' },
}

const groupOf = (key: string) => key.split(/[._]/)[0] || 'other'

const groups = computed(() => {
  const map = new Map<string, SysConfig[]>()
  list.value.forEach((c) => {
    const g = GROUP_LABELS[groupOf(c.configKey)]?.label || groupOf(c.configKey)
    if (!map.has(g)) map.set(g, [])
    map.get(g)!.push(c)
  })
  return Array.from(map.entries()).map(([label, items]) => ({
    label,
    icon: Object.values(GROUP_LABELS).find((x) => x.label === label)?.icon || 'Setting',
    items,
  }))
})

const dirtyKeys = computed(() => Object.keys(values.value).filter((k) => values.value[k] !== original.value[k]))

async function load() {
  loading.value = true
  try {
    list.value = (await configApi.list()) || []
    const v: Record<string, string> = {}
    list.value.forEach((c) => (v[c.configKey] = c.configValue ?? ''))
    values.value = { ...v }
    original.value = { ...v }
    if (!groups.value.some((g) => g.label === activeGroup.value)) activeGroup.value = groups.value[0]?.label || ''
  } finally {
    loading.value = false
  }
}
onMounted(load)

/** 布尔类参数用开关展示 */
const isBool = (c: SysConfig) => ['true', 'false'].includes(String(original.value[c.configKey]).toLowerCase())
const isNumber = (c: SysConfig) => /^-?\d+(\.\d+)?$/.test(String(original.value[c.configKey])) && !isBool(c)
const isLong = (c: SysConfig) => String(original.value[c.configKey] || '').length > 60

async function save() {
  if (!dirtyKeys.value.length) return toast.warning('没有修改')
  saving.value = true
  try {
    await configApi.batch(dirtyKeys.value.map((k) => ({ configKey: k, configValue: values.value[k] })))
    toast.success(`已保存 ${dirtyKeys.value.length} 项参数`)
    original.value = { ...values.value }
  } finally {
    saving.value = false
  }
}

function resetChanges() {
  values.value = { ...original.value }
}
</script>

<template>
  <div v-loading="loading" class="page">
    <div class="card config">
      <el-tabs v-if="groups.length" v-model="activeGroup" tab-position="left" class="config__tabs">
        <el-tab-pane v-for="g in groups" :key="g.label" :name="g.label">
          <template #label>
            <span class="config__tab"><el-icon><component :is="g.icon" /></el-icon>{{ g.label }}</span>
          </template>
          <div class="card-title"><span class="card-title__text">{{ g.label }}</span></div>
          <el-form label-position="top" :disabled="!canEdit" class="config__form">
            <el-form-item v-for="c in g.items" :key="c.configKey">
              <template #label>
                <span class="config__label">
                  {{ c.name || c.configKey }}
                  <el-tag v-if="values[c.configKey] !== original[c.configKey]" size="small" type="warning" effect="plain">已修改</el-tag>
                </span>
              </template>
              <el-switch
                v-if="isBool(c)"
                :model-value="values[c.configKey] === 'true'"
                @change="(v: string | number | boolean) => (values[c.configKey] = v ? 'true' : 'false')"
              />
              <el-input-number
                v-else-if="isNumber(c)"
                :model-value="Number(values[c.configKey])"
                controls-position="right"
                style="width: 240px"
                @change="(v: number | undefined) => (values[c.configKey] = String(v ?? 0))"
              />
              <el-input v-else-if="isLong(c)" v-model="values[c.configKey]" type="textarea" :rows="3" />
              <el-input v-else v-model="values[c.configKey]" style="max-width: 480px" />
              <div class="form-tip">
                <span class="mono">{{ c.configKey }}</span><template v-if="c.remark"> · {{ c.remark }}</template>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <EmptyState v-else-if="!loading" text="暂无系统参数" />
    </div>

    <div v-if="canEdit && groups.length" class="card footer-bar">
      <span class="text-secondary">{{ dirtyKeys.length ? `${dirtyKeys.length} 项参数待保存` : '修改后点击保存，所有分组一起提交' }}</span>
      <div>
        <el-button :disabled="!dirtyKeys.length" @click="resetChanges">撤销修改</el-button>
        <el-button type="primary" :loading="saving" :disabled="!dirtyKeys.length" :icon="'Check'" @click="save">保存全部</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.config {
  &__tabs {
    min-height: 420px;
    :deep(.el-tabs__header.is-left) {
      margin-right: 24px;
    }
    :deep(.el-tabs__item) {
      justify-content: flex-start;
      min-width: 150px;
    }
  }
  &__tab {
    display: inline-flex;
    align-items: center;
    gap: 8px;
  }
  &__form {
    max-width: 720px;
  }
  &__label {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;
  }
}
.footer-bar {
  position: sticky;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 20px;
  box-shadow: var(--app-shadow-pop);
}
@media (max-width: 768px) {
  .config__tabs {
    :deep(.el-tabs__header.is-left) {
      float: none;
      margin-right: 0;
    }
  }
}
</style>
