<script setup lang="ts">
import { computed, ref } from 'vue'
import * as Icons from '@element-plus/icons-vue'

/** 图标选择器：Element Plus 图标，存储图标组件名（如 "Goods"） */
const model = defineModel<string>({ default: '' })
withDefaults(defineProps<{ placeholder?: string }>(), { placeholder: '点击选择图标' })

const visible = ref(false)
const keyword = ref('')
const all = Object.keys(Icons)
const list = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return k ? all.filter((n) => n.toLowerCase().includes(k)) : all
})

function choose(name: string) {
  model.value = name
  visible.value = false
}
</script>

<template>
  <el-popover v-model:visible="visible" placement="bottom-start" :width="460" trigger="click">
    <template #reference>
      <el-input :model-value="model" :placeholder="placeholder" readonly class="icon-select__input">
        <template #prefix>
          <el-icon v-if="model"><component :is="model" /></el-icon>
          <el-icon v-else><Search /></el-icon>
        </template>
        <template #suffix>
          <el-icon v-if="model" class="icon-select__clear" @click.stop="model = ''"><CircleClose /></el-icon>
        </template>
      </el-input>
    </template>
    <el-input v-model="keyword" placeholder="搜索图标（英文名）" clearable size="small" />
    <el-scrollbar height="260px" class="icon-select__scroll">
      <div class="icon-select__grid">
        <div
          v-for="name in list"
          :key="name"
          class="icon-select__cell"
          :class="{ 'is-active': name === model }"
          :title="name"
          @click="choose(name)"
        >
          <el-icon><component :is="name" /></el-icon>
        </div>
      </div>
    </el-scrollbar>
  </el-popover>
</template>

<style scoped lang="scss">
.icon-select {
  &__input {
    cursor: pointer;
  }
  &__clear {
    cursor: pointer;
  }
  &__scroll {
    margin-top: 8px;
  }
  &__grid {
    display: grid;
    grid-template-columns: repeat(10, 1fr);
    gap: 4px;
  }
  &__cell {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 36px;
    font-size: 18px;
    border-radius: var(--app-radius-sm);
    color: var(--app-text-regular);
    cursor: pointer;
    transition: all 0.15s;
    &:hover {
      background: var(--app-primary-soft);
      color: var(--el-color-primary);
    }
    &.is-active {
      background: var(--el-color-primary);
      color: var(--el-color-white);
    }
  }
}
</style>
