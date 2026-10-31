<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { PROVINCE_GROUPS } from '@/utils/constants'

/**
 * 省份多选弹窗：按大区分组，支持大区全选；disabled 中的省份已被其他规则占用，不可选。
 */
const props = defineProps<{ selected: string[]; disabled?: string[]; title?: string }>()
const visible = defineModel<boolean>({ default: false })
const emit = defineEmits<{ confirm: [provinces: string[]] }>()

const checked = ref<string[]>([])
const disabledSet = computed(() => new Set(props.disabled || []))

watch(visible, (v) => {
  if (v) checked.value = [...props.selected]
})

function groupState(items: string[]) {
  const avail = items.filter((p) => !disabledSet.value.has(p))
  const n = avail.filter((p) => checked.value.includes(p)).length
  return { all: avail.length > 0 && n === avail.length, some: n > 0 && n < avail.length, empty: avail.length === 0 }
}

function toggleGroup(items: string[], on: boolean) {
  const avail = items.filter((p) => !disabledSet.value.has(p))
  const set = new Set(checked.value)
  avail.forEach((p) => (on ? set.add(p) : set.delete(p)))
  checked.value = Array.from(set)
}

function confirm() {
  // 保持全国列表顺序，便于阅读
  const order = PROVINCE_GROUPS.flatMap((g) => g.items)
  emit(
    'confirm',
    order.filter((p) => checked.value.includes(p)),
  )
  visible.value = false
}
</script>

<template>
  <el-dialog v-model="visible" :title="title || '选择地区'" width="680px" append-to-body>
    <div class="picker">
      <div v-for="g in PROVINCE_GROUPS" :key="g.region" class="picker__group">
        <el-checkbox
          class="picker__region"
          :model-value="groupState(g.items).all"
          :indeterminate="groupState(g.items).some"
          :disabled="groupState(g.items).empty"
          @change="(v: any) => toggleGroup(g.items, !!v)"
        >
          {{ g.region }}
        </el-checkbox>
        <el-checkbox-group v-model="checked" class="picker__items">
          <el-tooltip v-for="p in g.items" :key="p" :disabled="!disabledSet.has(p)" content="已在其他规则中设置" placement="top">
            <el-checkbox :value="p" :disabled="disabledSet.has(p)">{{ p }}</el-checkbox>
          </el-tooltip>
        </el-checkbox-group>
      </div>
    </div>
    <template #footer>
      <span class="picker__count text-secondary">已选 {{ checked.length }} 个地区</span>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="!checked.length" @click="confirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.picker {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  &__group {
    display: flex;
    gap: 12px;
    padding: 10px 14px;
    & + & {
      border-top: 1px solid var(--app-border);
    }
  }
  &__region {
    flex: none;
    width: 80px;
    font-weight: 600;
  }
  &__items {
    flex: 1;
    display: flex;
    flex-wrap: wrap;
    column-gap: 4px;
    .el-checkbox {
      margin-right: 12px;
      min-width: 118px;
    }
  }
  &__count {
    float: left;
    line-height: 32px;
    font-size: 13px;
  }
}
</style>
