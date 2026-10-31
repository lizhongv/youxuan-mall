<script setup lang="ts">
import { ref, watch } from 'vue'
import { showToast, type UploaderFileListItem } from 'vant'
import { upload } from '@/api/member'
import { compressImage } from '@/utils'

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

/**
 * 读取前压缩：长边 1600px、JPEG 质量 0.8（手机原图常超 5MB）；压缩后仍超限再提示。
 * 体积校验放在压缩之后，所以 van-uploader 不再设置 max-size。
 */
async function beforeRead(file: File | File[]): Promise<File | File[]> {
  const list = Array.isArray(file) ? file : [file]
  const out: File[] = []
  for (const f of list) {
    const c = await compressImage(f, 1600, 0.8)
    if (c.size > props.maxSizeMb * 1024 * 1024) {
      showToast(`图片不能超过 ${props.maxSizeMb}MB`)
      continue
    }
    out.push(c)
  }
  if (!out.length) return Promise.reject(new Error('oversize'))
  return Array.isArray(file) ? out : out[0]
}
</script>

<template>
  <van-uploader
    v-model="fileList"
    :max-count="maxCount"
    accept="image/*"
    multiple
    :before-read="beforeRead"
    :after-read="afterRead"
    preview-size="72"
    @delete="onDelete"
  />
</template>
