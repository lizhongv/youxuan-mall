<script setup lang="ts">
import { computed } from 'vue'
import { CANCEL_REASONS } from '@/utils/dict'
import { useDict } from '@/composables/useDict'

/** 取消订单原因选择 */
const show = defineModel<boolean>('show', { default: false })
const emit = defineEmits<{ select: [reason: string] }>()
/** 取消原因取字典 cancel_reason，失败时用内置列表 */
const reasons = useDict('cancel_reason', CANCEL_REASONS)
const actions = computed(() => reasons.value.map((r) => ({ name: r.label })))
function onSelect(a: { name: string }) {
  emit('select', a.name)
}
</script>

<template>
  <van-action-sheet
    v-model:show="show"
    title="请选择取消原因"
    :actions="actions"
    cancel-text="暂不取消"
    close-on-click-action
    @select="onSelect"
  />
</template>
