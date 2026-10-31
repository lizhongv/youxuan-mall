<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { wechatLogin } from '@/api/auth'
import { errorMessage } from '@/api/request'
import { useAuthRedirect } from '@/composables/useAuthRedirect'
import type { TokenResult } from '@/types'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

/** 微信授权回跳页：用 code + state 登录；未绑定手机号时进入绑定页 */
const route = useRoute()
const router = useRouter()
const { onLogin, redirectTarget } = useAuthRedirect()
const error = ref('')

/** 与 BindMobileView 共用的 sessionStorage 键 */
const BIND_TOKEN_KEY = 'mall_h5_wechat_bind'

onMounted(async () => {
  const code = route.query.code as string
  const state = route.query.state as string
  if (!code || !state) {
    error.value = '微信授权已取消或参数缺失'
    return
  }
  try {
    const res = await wechatLogin({ code, state })
    if (res.needBind && res.bindToken) {
      try {
        sessionStorage.setItem(BIND_TOKEN_KEY, res.bindToken)
      } catch {
        /* ignore */
      }
      router.replace({ name: 'wechat-bind', query: { redirect: redirectTarget() } })
    } else if (res.accessToken) {
      await onLogin(res as TokenResult)
    } else {
      error.value = '微信登录失败，请重试'
    }
  } catch (e) {
    error.value = errorMessage(e, '微信登录失败，请重试')
  }
})
</script>

<template>
  <div class="page">
    <NavBar title="微信登录" :on-back="() => router.replace({ name: 'login', query: { redirect: redirectTarget() } })" />
    <van-loading v-if="!error" class="wx-cb__loading" vertical>正在登录...</van-loading>
    <EmptyState
      v-else
      image="error"
      :description="error"
      button-text="返回登录"
      @click="router.replace({ name: 'login', query: { redirect: redirectTarget() } })"
    />
  </div>
</template>

<style scoped lang="scss">
.wx-cb__loading {
  padding: 80px 0;
}
</style>
