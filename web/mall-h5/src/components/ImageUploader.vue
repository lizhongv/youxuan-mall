<script setup lang="ts">
import { ref, watch } from 'vue'
import { showToast, type UploaderFileListItem } from 'vant'
import { upload } from '@/api/member'

/** 图片上传：选择后立即上传到 /app/member/upload，v-model 为图片 URL 数组 */
const props = withDefaults(defineProps<{ maxCount?: number; bizType?: string; maxSizeMb?: number }>(), {
  maxCount: 6,
  bizType: 'review',
  maxSizeMb: 5,
})
const urls = defineModel<string[]>({ default: () => [] })

const fileList = ref<UploaderFileListItem[]>(urls.value.map((u) => ({ url: u, status: 'done' })))

// 外部重置时同步
watch(urls, (v) => {
  const done = fileList.value.filter((f) => f.status === 'done').map((f) => f.url)
  if (v.join() !== done.join()) fileList.value = v.map((u) => ({ url: u, status: 'done' }))
})

function sync() {
  urls.value = fileList.value.filter((f) => f.status === 'done' && f.url).map((f) => f.url as string)
}

async function afterRead(items: UploaderFileListItem | UploaderFileListItem[]) {
  const arr = Array.isArray(items) ? items : [items]
  await Promise.all(
    arr.map(async (item) => {
      if (!item.file) return
      item.status = 'uploading'
      item.message = '上传中'
      try {
        const res = await upload(item.file, props.bizType)
        item.url = res.url
        item.status = 'done'
        item.message = ''
      } catch {
        item.status = 'failed'
        item.message = '上传失败'
      }
    }),
  )
  sync()
}

function onDelete() {
  sync()
}

function onOversize() {
  showToast(`图片不能超过 ${props.maxSizeMb}MB`)
}
</script>

<template>
  <van-uploader
    v-model="fileList"
    :max-count="maxCount"
    :max-size="maxSizeMb * 1024 * 1024"
    accept="image/*"
    multiple
    :after-read="afterRead"
    preview-size="72"
    @delete="onDelete"
    @oversize="onOversize"
  />
</template>
