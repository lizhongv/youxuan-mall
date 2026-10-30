<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import NavBar from '@/components/NavBar.vue'

const app = useAppStore()
const version = '1.0.0'
</script>

<template>
  <div class="page about">
    <NavBar title="关于我们" />
    <div class="about__logo">
      <img src="/favicon.svg" alt="logo" width="72" height="72" />
      <div class="about__name">{{ app.config.mallName || '商城' }}</div>
      <div class="about__ver">版本 {{ version }}</div>
    </div>
    <van-cell-group inset>
      <van-cell title="客服电话" :value="app.config.servicePhone || '暂无'" />
      <van-cell title="正品保障" value="官方直营 · 假一赔十" />
      <van-cell title="售后服务" value="支持退款 / 退货退款" />
    </van-cell-group>
    <div class="about__copy">© {{ new Date().getFullYear() }} {{ app.config.mallName || '商城' }} 版权所有</div>
  </div>
</template>

<style scoped lang="scss">
.about {
  &__logo {
    padding: 40px 0 28px;
    text-align: center;
    img {
      border-radius: 16px;
    }
  }
  &__name {
    margin-top: 12px;
    font-size: 18px;
    font-weight: 700;
  }
  &__ver {
    margin-top: 4px;
    font-size: 12px;
    color: $text-3;
  }
  &__copy {
    padding: 40px 0;
    text-align: center;
    font-size: 12px;
    color: $text-3;
  }
}
</style>
