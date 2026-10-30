<script setup lang="ts">
import { computed } from 'vue'
import { CANCEL_REASONS } from '@/composables/useOrderActions'

/** 取消订单原因选择 */
const show = defineModel<boolean>('show', { default: false })
const emit = defineEmits<{ select: [reason: string] }>()
const actions = computed(() => CANCEL_REASONS.map((name) => ({ name })))
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
