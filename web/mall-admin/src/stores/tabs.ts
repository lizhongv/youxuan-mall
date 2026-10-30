import { defineStore } from 'pinia'
import type { RouteLocationNormalized } from 'vue-router'
import { StorageKey, storage } from '@/utils/storage'

export interface TabItem {
  /** 唯一键：路径（不含查询串），同一详情页不同 ID 各占一个标签 */
  path: string
  /** 跳转用的完整路径（含查询串） */
  fullPath: string
  name: string
  title: string
  icon?: string
  affix?: boolean
}

const HOME: TabItem = { path: '/dashboard', fullPath: '/dashboard', name: 'Dashboard', title: '数据概览', icon: 'Odometer', affix: true }

export const useTabsStore = defineStore('tabs', {
  state: () => ({
    tabs: storage.getJSON<TabItem[]>(StorageKey.tabs, [HOME]),
  }),
  actions: {
    persist() {
      storage.setJSON(StorageKey.tabs, this.tabs)
    },
    ensureHome() {
      if (!this.tabs.some((t) => t.path === HOME.path)) this.tabs.unshift(HOME)
    },
    add(route: RouteLocationNormalized) {
      if (route.meta.hideTab || !route.meta.title) return
      const title = String(route.meta.tabTitle?.(route) ?? route.meta.title)
      const exist = this.tabs.find((t) => t.path === route.path)
      if (exist) {
        exist.title = title
        exist.fullPath = route.fullPath
      } else {
        this.tabs.push({
          path: route.path,
          fullPath: route.fullPath,
          name: String(route.name || ''),
          title,
          icon: route.meta.icon,
          affix: route.path === HOME.path,
        })
      }
      this.ensureHome()
      this.persist()
    },
    /** 关闭标签，返回应跳转的路径（关闭的是当前页时） */
    close(path: string, current: string): string | null {
      const idx = this.tabs.findIndex((t) => t.path === path)
      if (idx < 0 || this.tabs[idx].affix) return null
      this.tabs.splice(idx, 1)
      this.persist()
      if (path !== current) return null
      const next = this.tabs[idx] || this.tabs[idx - 1] || HOME
      return next.fullPath || next.path
    },
    closeOthers(path: string) {
      this.tabs = this.tabs.filter((t) => t.affix || t.path === path)
      this.persist()
    },
    closeLeft(path: string) {
      const idx = this.tabs.findIndex((t) => t.path === path)
      this.tabs = this.tabs.filter((t, i) => t.affix || i >= idx)
      this.persist()
    },
    closeRight(path: string) {
      const idx = this.tabs.findIndex((t) => t.path === path)
      this.tabs = this.tabs.filter((t, i) => t.affix || i <= idx)
      this.persist()
    },
    closeAll() {
      this.tabs = this.tabs.filter((t) => t.affix)
      this.ensureHome()
      this.persist()
    },
    reset() {
      this.tabs = [HOME]
      this.persist()
    },
  },
})
