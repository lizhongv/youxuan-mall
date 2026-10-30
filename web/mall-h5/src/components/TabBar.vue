<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useCartStore } from '@/stores/cart'

/** 底部标签栏：首页 / 分类 / 购物车（数量徽标）/ 我的 */
const route = useRoute()
const cart = useCartStore()
const active = computed(() => route.name as string)
const badge = computed(() => (cart.count > 0 ? (cart.count > 99 ? '99+' : cart.count) : undefined))
</script>

<template>
  <van-tabbar :model-value="active" route fixed placeholder safe-area-inset-bottom z-index="100">
    <van-tabbar-item name="home" to="/" icon="wap-home-o">首页</van-tabbar-item>
    <van-tabbar-item name="category" to="/category" icon="apps-o">分类</van-tabbar-item>
    <van-tabbar-item name="cart" to="/cart" icon="shopping-cart-o" :badge="badge">购物车</van-tabbar-item>
    <van-tabbar-item name="mine" to="/mine" icon="user-o">我的</van-tabbar-item>
  </van-tabbar>
</template>
