<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fileApi } from '@/api/system'

/**
 * 图片上传：单图（v-model 为 string）或多图（v-model 为 string[]）。
 * 支持预览、删除、拖拽排序（原生 HTML5 拖放，免第三方依赖）、大小 / 类型校验。
 */
const props = withDefaults(
  defineProps<{
    modelValue?: string | string[] | null
    multiple?: boolean
    limit?: number
    /** 单张大小上限（MB） */
    maxSize?: number
    accept?: string
    bizType?: string
    /** 缩略图尺寸（px） */
    size?: number
    tip?: string
    disabled?: boolean
  }>(),
  {
    multiple: false,
    limit: 9,
    maxSize: 5,
    accept: 'image/jpeg,image/png,image/gif,image/webp',
    bizType: 'common',
    size: 96,
  },
)

const emit = defineEmits<{ 'update:modelValue': [value: string | string[]]; change: [value: string | string[]] }>()

const inputRef = ref<HTMLInputElement>()
const uploading = ref(0)
const previewIndex = ref(-1)
const dragFrom = ref(-1)
const dragOver = ref(-1)

const urls = computed<string[]>(() => {
  const v = props.modelValue
  if (!v) return []
  return Array.isArray(v) ? v.filter(Boolean) : [v]
})

const max = computed(() => (props.multiple ? props.limit : 1))
const canAdd = computed(() => !props.disabled && urls.value.length + uploading.value < max.value)
const boxStyle = computed(() => ({ width: `${props.size}px`, height: `${props.size}px` }))

function emitValue(list: string[]) {
  const v = props.multiple ? list : list[0] || ''
  emit('update:modelValue', v)
  emit('change', v)
}

function pick() {
  if (!canAdd.value) return
  inputRef.value?.click()
}

function validate(file: File): boolean {
  const types = props.accept.split(',').map((s) => s.trim())
  if (!types.includes(file.type)) {
    ElMessage.warning(`「${file.name}」格式不支持，请上传 ${types.map((t) => t.replace('image/', '')).join(' / ')} 图片`)
    return false
  }
  if (file.size > props.maxSize * 1024 * 1024) {
    ElMessage.warning(`「${file.name}」超过 ${props.maxSize}MB`)
    return false
  }
  return true
}

async function onFiles(e: Event) {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  const room = max.value - urls.value.length
  if (files.length > room) ElMessage.warning(`最多上传 ${max.value} 张，已忽略超出部分`)
  const valid = files.slice(0, Math.max(room, 0)).filter(validate)
  if (!valid.length) return
  uploading.value += valid.length
  // 逐张上传并按选择顺序追加，单张失败不影响其余
  const results: string[] = []
  await Promise.all(
    valid.map(async (f, i) => {
      try {
        const res = await fileApi.upload(f, props.bizType)
        results[i] = res.url
      } catch {
        /* 请求层已提示 */
      } finally {
        uploading.value--
      }
    }),
  )
  const added = results.filter(Boolean)
  if (!added.length) return
  emitValue(props.multiple ? [...urls.value, ...added] : [added[0]])
}

function remove(i: number) {
  const list = [...urls.value]
  list.splice(i, 1)
  emitValue(list)
}

function onDragStart(i: number, e: DragEvent) {
  dragFrom.value = i
  e.dataTransfer?.setData('text/plain', String(i))
  if (e.dataTransfer) e.dataTransfer.effectAllowed = 'move'
}

function onDrop(i: number) {
  const from = dragFrom.value
  dragFrom.value = -1
  dragOver.value = -1
  if (from < 0 || from === i) return
  const list = [...urls.value]
  const [item] = list.splice(from, 1)
  list.splice(i, 0, item)
  emitValue(list)
}
</script>

<template>
  <div class="image-upload" :class="{ 'is-disabled': disabled }">
    <div class="image-upload__list">
      <div
        v-for="(url, i) in urls"
        :key="url + i"
        class="image-upload__item"
        :class="{ 'is-drag-over': dragOver === i && dragFrom !== i, 'is-dragging': dragFrom === i }"
        :style="boxStyle"
        :draggable="multiple && !disabled && urls.length > 1"
        @dragstart="onDragStart(i, $event)"
        @dragover.prevent="dragOver = i"
        @dragleave="dragOver = -1"
        @drop.prevent="onDrop(i)"
        @dragend="dragFrom = -1; dragOver = -1"
      >
        <el-image :src="url" fit="cover" class="image-upload__img" />
        <span v-if="multiple && i === 0" class="image-upload__badge">首图</span>
        <div class="image-upload__mask">
          <el-icon title="预览" @click="previewIndex = i"><ZoomIn /></el-icon>
          <el-icon v-if="!disabled" title="删除" @click="remove(i)"><Delete /></el-icon>
        </div>
      </div>
      <div v-for="n in uploading" :key="'u' + n" class="image-upload__item image-upload__loading" :style="boxStyle">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>上传中</span>
      </div>
      <div v-if="canAdd" class="image-upload__trigger" :style="boxStyle" @click="pick">
        <el-icon><Plus /></el-icon>
        <span v-if="multiple">{{ urls.length }}/{{ limit }}</span>
        <span v-else>上传图片</span>
      </div>
    </div>
    <div v-if="tip || multiple" class="form-tip">
      {{ tip }}
      <template v-if="multiple && urls.length > 1"> 拖动图片可调整顺序。</template>
    </div>
    <input ref="inputRef" type="file" hidden :accept="accept" :multiple="multiple" @change="onFiles" />
    <el-image-viewer
      v-if="previewIndex >= 0"
      :url-list="urls"
      :initial-index="previewIndex"
      teleported
      hide-on-click-modal
      @close="previewIndex = -1"
    />
  </div>
</template>

<style scoped lang="scss">
.image-upload {
  &__list {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }
  &__item,
  &__trigger {
    position: relative;
    border-radius: var(--app-radius);
    overflow: hidden;
    flex: none;
  }
  &__item {
    border: 1px solid var(--app-border);
    background: var(--app-card-bg-muted);
    transition:
      transform 0.15s,
      box-shadow 0.15s;
    &[draggable='true'] {
      cursor: grab;
    }
    &.is-dragging {
      opacity: 0.4;
    }
    &.is-drag-over {
      box-shadow: 0 0 0 2px var(--el-color-primary);
      transform: scale(1.03);
    }
    &:hover .image-upload__mask {
      opacity: 1;
    }
  }
  &__img {
    width: 100%;
    height: 100%;
    display: block;
  }
  &__badge {
    position: absolute;
    left: 0;
    top: 0;
    padding: 2px 6px;
    font-size: 11px;
    color: var(--el-color-white);
    background: var(--el-color-primary);
    border-bottom-right-radius: var(--app-radius-sm);
  }
  &__mask {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 14px;
    font-size: 18px;
    color: var(--el-color-white);
    background: var(--app-mask);
    opacity: 0;
    transition: opacity 0.2s;
    .el-icon {
      cursor: pointer;
      &:hover {
        transform: scale(1.15);
      }
    }
  }
  &__loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 6px;
    font-size: 12px;
    color: var(--app-text-secondary);
    .el-icon {
      font-size: 20px;
      color: var(--el-color-primary);
    }
  }
  &__trigger {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 6px;
    border: 1px dashed var(--app-border-strong);
    background: var(--app-card-bg-muted);
    color: var(--app-text-secondary);
    font-size: 12px;
    cursor: pointer;
    transition:
      border-color 0.2s,
      color 0.2s;
    .el-icon {
      font-size: 22px;
    }
    &:hover {
      border-color: var(--el-color-primary);
      color: var(--el-color-primary);
    }
  }
  &.is-disabled .image-upload__trigger {
    cursor: not-allowed;
  }
}
</style>
