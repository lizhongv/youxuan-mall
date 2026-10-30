import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { Lazyload } from 'vant'
// 函数式组件样式需手动引入（组件样式由 unplugin-vue-components 按需引入）
import 'vant/es/toast/style'
import 'vant/es/dialog/style'
import 'vant/es/notify/style'
import 'vant/es/image-preview/style'
import '@/styles/global.scss'
import { applyCssVars } from '@/styles/theme'
import App from './App.vue'
import router from './router'

applyCssVars()

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(Lazyload, { lazyComponent: true, loading: '', error: '' })
app.mount('#app')
