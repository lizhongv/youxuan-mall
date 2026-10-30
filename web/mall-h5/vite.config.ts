import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { VantResolver } from '@vant/auto-import-resolver'
import { fileURLToPath, URL } from 'node:url'

// 开发代理：默认转发到已部署环境的 Nginx（保留 /api 前缀）；
// 直连本地服务端时设 VITE_PROXY_TARGET=http://localhost:8081 VITE_PROXY_DIRECT=1，由代理去掉 /api 前缀
const target = process.env.VITE_PROXY_TARGET || 'http://192.168.0.100:8090'
const direct = process.env.VITE_PROXY_DIRECT === '1'

export default defineConfig({
  base: '/',
  plugins: [
    vue(),
    Components({ resolvers: [VantResolver()], dts: 'src/components.d.ts' }),
  ],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  css: {
    preprocessorOptions: {
      scss: { api: 'modern-compiler', additionalData: `@use "@/styles/variables.scss" as *;\n` },
    },
  },
  server: {
    host: true,
    port: 5174,
    proxy: {
      '/api': { target, changeOrigin: true, rewrite: (p) => (direct ? p.replace(/^\/api/, '') : p) },
      '/uploads': { target, changeOrigin: true },
    },
  },
  build: {
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          vant: ['vant'],
        },
      },
    },
  },
})
