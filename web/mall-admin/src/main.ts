import { createApp } from 'vue'
import { createPinia } from 'pinia'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// 组件按需注册（unplugin-vue-components），样式整体引入以覆盖 ElMessage 等命令式组件
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import '@/styles/index.scss'

import App from './App.vue'
import router from './router'
import { vPerm } from './directives/perm'

dayjs.locale('zh-cn')

const app = createApp(App)

// 菜单与图标选择器按名称渲染图标，因此全局注册
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.directive('perm', vPerm)
app.mount('#app')
