import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import type { LoginForm, UserInfo } from '@/types'
import { tokenStore } from '@/utils/storage'
import { SUPER_ADMIN } from '@/utils/constants'

interface State {
  info: UserInfo | null
  token: string
}

export const useUserStore = defineStore('user', {
  state: (): State => ({
    info: null,
    token: tokenStore.access,
  }),
  getters: {
    isLogin: (s) => !!s.token,
    isSuper: (s) => !!s.info?.roles?.includes(SUPER_ADMIN),
    permissionSet: (s) => new Set(s.info?.permissions || []),
    displayName: (s) => s.info?.nickname || s.info?.username || '管理员',
  },
  actions: {
    async login(form: LoginForm) {
      const res = await authApi.login(form)
      tokenStore.save(res.accessToken, res.refreshToken)
      this.token = res.accessToken
      this.info = null
    },
    async fetchInfo() {
      this.info = await authApi.me()
      return this.info
    },
    /** 是否拥有权限码；超管或未声明权限直接放行 */
    hasPerm(code?: string | string[]): boolean {
      if (!code || (Array.isArray(code) && code.length === 0)) return true
      if (this.isSuper) return true
      const codes = Array.isArray(code) ? code : [code]
      return codes.some((c) => this.permissionSet.has(c))
    },
    async logout(callApi = true) {
      if (callApi && this.token) {
        try {
          await authApi.logout()
        } catch {
          /* 忽略：本地仍然清除登录态 */
        }
      }
      tokenStore.clear()
      this.token = ''
      this.info = null
    },
  },
})
