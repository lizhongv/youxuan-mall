// @wangeditor/editor-for-vue 的 package.json exports 未暴露类型声明，这里补充最小声明
declare module '@wangeditor/editor-for-vue' {
  import type { DefineComponent } from 'vue'
  export const Editor: DefineComponent<Record<string, unknown>>
  export const Toolbar: DefineComponent<Record<string, unknown>>
}
