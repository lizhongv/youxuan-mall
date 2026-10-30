<script setup lang="ts">
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { useAppStore } from '@/stores/app'
import NavBar from '@/components/NavBar.vue'

const router = useRouter()
const user = useUserStore()
const cart = useCartStore()
const app = useAppStore()

function logout() {
  showConfirmDialog({ title: '退出登录', message: '确定退出当前账号吗？' })
    .then(() => {
      user.clear()
      cart.setCount(0)
      showToast('已退出登录')
      router.replace('/mine')
    })
    .catch(() => {})
}

function clearCache() {
  try {
    sessionStorage.clear()
  } catch {
    /* ignore */
  }
  showToast('缓存已清除')
}
</script>

<template>
  <div class="page">
    <NavBar title="设置" />
    <van-cell-group inset class="settings__group">
      <van-cell v-if="user.isLogin" title="个人资料" is-link to="/profile" />
      <van-cell v-if="user.isLogin" title="收货地址" is-link to="/address" />
      <van-cell v-if="user.isLogin" title="修改密码" is-link to="/password" />
    </van-cell-group>
    <van-cell-group inset class="settings__group">
      <van-cell title="清除缓存" is-link @click="clearCache" />
      <van-cell
        v-if="app.config.servicePhone"
        title="联系客服"
        is-link
        :value="app.config.servicePhone"
        :url="`tel:${app.config.servicePhone}`"
      />
      <van-cell title="关于我们" is-link to="/about" />
    </van-cell-group>
    <div class="settings__logout">
      <van-button v-if="user.isLogin" block round @click="logout">退出登录</van-button>
      <van-button v-else block round class="btn-gradient" @click="router.push({ name: 'login' })">登录</van-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.settings__group {
  margin-top: 12px;
}
.settings__logout {
  padding: 32px $gap;
  :deep(.van-button:not(.btn-gradient)) {
    color: $primary;
  }
}
</style>
