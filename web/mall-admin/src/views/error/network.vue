<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import ErrorView from './ErrorView.vue'

/** 网络异常时加载用户信息失败：保留登录态，提供重试，不把用户踢回登录页 */
const route = useRoute()
const user = useUserStore()
const target = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/') ? route.query.redirect : '/'

function retry() {
  window.location.href = `${import.meta.env.BASE_URL.replace(/\/$/, '')}${target}`
}

async function relogin() {
  await user.logout()
  window.location.href = `${import.meta.env.BASE_URL}login`
}
</script>

<template>
  <div class="network-error">
    <ErrorView code="!" title="网络连接异常" desc="暂时无法连接服务器，登录状态已保留，请检查网络后重试。">
      <template #actions>
        <el-button @click="relogin">重新登录</el-button>
        <el-button type="primary" :icon="'Refresh'" @click="retry">重试</el-button>
      </template>
    </ErrorView>
  </div>
</template>

<style scoped>
.network-error {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-bg);
}
</style>
