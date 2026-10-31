import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// 开发代理：默认转发到已部署环境的 Nginx（保留 /api 前缀）；
// 直连本地服务端时设 VITE_PROXY_TARGET=http://localhost:8081 VITE_PROXY_DIRECT=1，由代理去掉 /api 前缀
const target = process.env.VITE_PROXY_TARGET || 'http://localhost:8090'
const direct = process.env.VITE_PROXY_DIRECT === '1'

export default defineConfig({
  base: '/console/',
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver({ importStyle: false })],
      dts: 'src/types/auto-imports.d.ts',
    }),
    Components({
      // 仅自动注册 Element Plus 组件；业务组件显式 import，便于跳转与类型检查
      dirs: [],
      resolvers: [ElementPlusResolver({ importStyle: false })],
      dts: 'src/types/components.d.ts',
    }),
  ],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  css: {
    preprocessorOptions: {
      scss: { api: 'modern-compiler' },
    },
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': { target, changeOrigin: true, rewrite: (p) => (direct ? p.replace(/^\/api/, '') : p) },
      '/uploads': { target, changeOrigin: true },
    },
  },
  build: {
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          'element-plus': ['element-plus', '@element-plus/icons-vue'],
          echarts: ['echarts/core', 'echarts/charts', 'echarts/components', 'echarts/renderers'],
          wangeditor: ['@wangeditor/editor', '@wangeditor/editor-for-vue'],
        },
      },
    },
  },
})
