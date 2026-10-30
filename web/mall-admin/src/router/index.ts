import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { routes, menuTree, type MenuNode } from './routes'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'

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
    } catch {
      await user.logout(false)
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }
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
  if (!to.meta.public) useTabsStore().add(to)
})

router.onError(() => NProgress.done())

export default router
