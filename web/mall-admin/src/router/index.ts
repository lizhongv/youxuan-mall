import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { routes, menuTree, type MenuNode } from './routes'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'
import { ApiError, ResultCode, sessionEvents } from '@/utils/request'
import { tokenStore } from '@/utils/storage'

NProgress.configure({ showSpinner: false, trickleSpeed: 120 })

const router = createRouter({
  history: createWebHistory('/console/'),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

const APP_NAME = '商城管理后台'

/** 第一个有权限的菜单页，用于登录后默认落地 */
export function firstAccessiblePath(): string {
  const user = useUserStore()
  const walk = (nodes: MenuNode[]): string | null => {
    for (const n of nodes) {
      if (n.children) {
        const p = walk(n.children)
        if (p) return p
      } else if (user.hasPerm(n.permission)) return n.path
    }
    return null
  }
  return walk(menuTree) || '/profile'
}

router.beforeEach(async (to) => {
  NProgress.start()
  const user = useUserStore()

  if (to.meta.public) {
    if (to.name === 'Login' && user.isLogin) return { path: '/' }
    return true
  }
  if (!user.isLogin) {
    return { path: '/login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : {} }
  }
  if (!user.info) {
    try {
      await user.fetchInfo()
    } catch (e) {
      // 只有明确的未登录（刷新令牌被拒，请求层已跳登录页）才清登录态；网络异常保留令牌并提供重试
      if (!tokenStore.access || (e instanceof ApiError && e.code === ResultCode.UNAUTHORIZED)) {
        await user.logout(false)
        return { path: '/login', query: { redirect: to.fullPath } }
      }
      return { name: 'NetworkError', query: { redirect: to.fullPath } }
    }
  }
  // 强制修改初始密码：除改密页外一律拦截
  if (user.mustChangePassword) {
    return to.name === 'ChangePassword' ? true : { name: 'ChangePassword', replace: true }
  }
  if (to.name === 'ChangePassword') return { path: '/', replace: true }
  // 根路径 / 默认首页：无数据概览权限时落到第一个可访问菜单
  if (to.path === '/dashboard' && !user.hasPerm(to.meta.permission) && to.redirectedFrom?.path === '/') {
    return { path: firstAccessiblePath(), replace: true }
  }
  if (to.meta.permission && !user.hasPerm(to.meta.permission)) {
    return { path: '/403', query: { from: to.fullPath } }
  }
  return true
})

router.afterEach((to) => {
  NProgress.done()
  document.title = to.meta.title ? `${to.meta.title} · ${APP_NAME}` : APP_NAME
  if (!to.meta.public && to.name !== 'ChangePassword') useTabsStore().add(to)
})

router.onError(() => NProgress.done())

// 任意接口返回 40301（请先修改初始密码）：标记并跳到改密页
sessionEvents.onMustChangePassword = () => {
  const user = useUserStore()
  user.markMustChangePassword()
  if (!user.info) return
  if (router.currentRoute.value.name !== 'ChangePassword') router.replace({ name: 'ChangePassword' })
}

export default router
