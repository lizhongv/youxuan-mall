<script setup lang="ts">
import ErrorView from './ErrorView.vue'
</script>

<template>
  <div class="not-found">
    <ErrorView code="404" title="页面不存在" desc="你访问的页面可能已被移除，或链接地址有误。" />
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-bg);
}
</style>
