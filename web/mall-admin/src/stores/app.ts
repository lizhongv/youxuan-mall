import { defineStore } from 'pinia'
import { StorageKey, storage } from '@/utils/storage'

export type Theme = 'light' | 'dark'

function initialTheme(): Theme {
  const saved = storage.get(StorageKey.theme)
  if (saved === 'light' || saved === 'dark') return saved
  try {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  } catch {
    return 'light'
  }
}

export const useAppStore = defineStore('app', {
  state: () => ({
    collapsed: storage.get(StorageKey.collapsed) === '1',
    theme: initialTheme(),
    /** 移动端：侧栏以抽屉形式出现 */
    isMobile: false,
    mobileMenuOpen: false,
    /** 刷新当前页时递增，用于 router-view key */
    reloadKey: 0,
  }),
  actions: {
    toggleCollapse() {
      if (this.isMobile) {
        this.mobileMenuOpen = !this.mobileMenuOpen
        return
      }
      this.collapsed = !this.collapsed
      storage.set(StorageKey.collapsed, this.collapsed ? '1' : '0')
    },
    applyTheme() {
      document.documentElement.classList.toggle('dark', this.theme === 'dark')
    },
    setTheme(theme: Theme) {
      this.theme = theme
      storage.set(StorageKey.theme, theme)
      this.applyTheme()
    },
    setMobile(v: boolean) {
      this.isMobile = v
      if (!v) this.mobileMenuOpen = false
    },
    reload() {
      this.reloadKey++
    },
  },
})
