<script setup lang="ts">
import { onBeforeUnmount, shallowRef } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import '@wangeditor/editor/dist/css/style.css'
import { ElMessage } from 'element-plus'
import { fileApi } from '@/api/system'

/** 富文本编辑器（wangEditor 5），图片走统一上传接口 */
const props = withDefaults(defineProps<{ height?: number; placeholder?: string; bizType?: string; maxSize?: number }>(), {
  height: 420,
  placeholder: '请输入内容…',
  bizType: 'goods',
  maxSize: 5,
})
const html = defineModel<string>({ default: '' })

const editorRef = shallowRef<IDomEditor>()

const toolbarConfig: Partial<IToolbarConfig> = {
  excludeKeys: ['group-video', 'insertVideo', 'uploadVideo', 'fullScreen', 'codeBlock', 'emotion'],
}

type InsertFn = (url: string, alt: string, href: string) => void

const editorConfig: Partial<IEditorConfig> = {
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      async customUpload(file: File, insertFn: InsertFn) {
        if (!file.type.startsWith('image/')) {
          ElMessage.warning('只能上传图片')
          return
        }
        if (file.size > props.maxSize * 1024 * 1024) {
          ElMessage.warning(`图片不能超过 ${props.maxSize}MB`)
          return
        }
        const res = await fileApi.upload(file, props.bizType)
        insertFn(res.url, res.originalName || '', '')
      },
    },
  },
}

function onCreated(editor: IDomEditor) {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  editorRef.value?.destroy()
})
</script>

<template>
  <div class="rich-editor">
    <Toolbar class="rich-editor__toolbar" :editor="editorRef" :default-config="toolbarConfig" mode="default" />
    <Editor
      v-model="html"
      class="rich-editor__body"
      :style="{ height: `${height}px` }"
      :default-config="editorConfig"
      mode="default"
      @on-created="onCreated"
    />
  </div>
</template>

<style lang="scss">
.rich-editor {
  width: 100%;
  border: 1px solid var(--app-border-strong);
  border-radius: var(--app-radius);
  overflow: hidden;
  // wangEditor 自带 CSS 变量，映射到主题以支持深色模式
  --w-e-textarea-bg-color: var(--app-card-bg);
  --w-e-textarea-color: var(--app-text);
  --w-e-textarea-border-color: var(--app-border);
  --w-e-textarea-slight-border-color: var(--app-border);
  --w-e-textarea-slight-color: var(--app-text-placeholder);
  --w-e-textarea-slight-bg-color: var(--app-card-bg-muted);
  --w-e-textarea-selected-border-color: var(--el-color-primary);
  --w-e-textarea-handler-bg-color: var(--el-color-primary);
  --w-e-toolbar-color: var(--app-text-regular);
  --w-e-toolbar-bg-color: var(--app-card-bg-muted);
  --w-e-toolbar-active-color: var(--app-text);
  --w-e-toolbar-active-bg-color: var(--app-border);
  --w-e-toolbar-disabled-color: var(--app-text-placeholder);
  --w-e-toolbar-border-color: var(--app-border);
  --w-e-modal-button-bg-color: var(--app-card-bg-muted);
  --w-e-modal-button-border-color: var(--app-border-strong);

  &__toolbar {
    border-bottom: 1px solid var(--app-border);
  }
  &__body {
    overflow-y: hidden;
  }
}
</style>
