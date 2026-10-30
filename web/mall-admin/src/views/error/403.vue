<script setup lang="ts">
import ErrorView from './ErrorView.vue'
</script>

<template>
  <div class="card">
    <ErrorView code="403" title="没有访问权限" desc="当前账号未被授予该页面的查看权限，如需开通请联系超级管理员。" />
  </div>
</template>
