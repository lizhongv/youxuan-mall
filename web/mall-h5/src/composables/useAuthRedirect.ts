import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import type { TokenResult } from '@/types'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'

/** 登录/注册成功：保存令牌、拉取资料、回跳 redirect（只允许站内路径） */
export function useAuthRedirect() {
  const route = useRoute()
  const router = useRouter()
  const user = useUserStore()
  const cart = useCartStore()

  function redirectTarget() {
    const r = route.query.redirect
    return typeof r === 'string' && r.startsWith('/') && !r.startsWith('//') ? r : '/mine'
  }

  async function onLogin(token: TokenResult, msg = '登录成功') {
    user.setToken(token)
    showSuccessToast(msg)
    user.fetchProfile()
    cart.refreshCount()
    router.replace(redirectTarget())
  }

  return { onLogin, redirectTarget }
}
