<script setup lang="ts">
import { onMounted } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useAppStore } from '@/stores/app'

const app = useAppStore()

onMounted(() => {
  app.applyTheme()
  const mq = window.matchMedia('(max-width: 992px)')
  const update = () => app.setMobile(mq.matches)
  update()
  mq.addEventListener('change', update)
})
</script>

<template>
  <el-config-provider :locale="zhCn" :button="{ autoInsertSpace: false }">
    <router-view />
  </el-config-provider>
</template>
