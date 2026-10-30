<script setup lang="ts">
import { useRouter } from 'vue-router'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
</script>

<template>
  <div class="page">
    <NavBar title="页面不存在" />
    <EmptyState image="error" description="页面走丢了~" button-text="回到首页" @click="router.replace('/')" />
  </div>
</template>
