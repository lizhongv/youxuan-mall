<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { themeVars } from '@/styles/theme'
import { useAppStore } from '@/stores/app'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import TabBar from '@/components/TabBar.vue'

const route = useRoute()
const app = useAppStore()
const cart = useCartStore()
const user = useUserStore()

onMounted(() => {
  app.loadConfig()
  cart.refreshCount()
})

// 登录状态变化时刷新购物车角标
watch(
  () => user.isLogin,
  () => cart.refreshCount(),
)
</script>

<template>
  <van-config-provider :theme-vars="themeVars" theme-vars-scope="global">
    <router-view v-slot="{ Component, route: r }">
      <keep-alive :max="5">
        <component :is="Component" v-if="r.meta.keepAlive" :key="r.name" />
      </keep-alive>
      <component :is="Component" v-if="!r.meta.keepAlive" :key="r.path" />
    </router-view>
    <TabBar v-if="route.meta.tabbar" />
  </van-config-provider>
</template>
