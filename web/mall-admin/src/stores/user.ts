import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import type { LoginForm, PasswordForm, TokenPair, UserInfo } from '@/types'
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
    mustChangePassword: (s) => !!s.info?.mustChangePassword,
  },
  actions: {
    applyTokens(pair: TokenPair) {
      tokenStore.save(pair.accessToken, pair.refreshToken)
      this.token = pair.accessToken
    },
    async login(form: LoginForm) {
      const res = await authApi.login(form)
      this.applyTokens(res)
      this.info = null
    },
    async fetchInfo() {
      this.info = await authApi.me()
      return this.info
    },
    /**
     * 修改密码：后端返回新令牌（旧令牌全部失效）时直接替换并刷新用户信息，无需重新登录。
     * 返回 false 表示后端未返回令牌（旧版本后端），调用方应引导重新登录。
     */
    async changePassword(form: PasswordForm): Promise<boolean> {
      const pair = await authApi.updatePassword(form)
      if (pair?.accessToken) {
        this.applyTokens(pair)
        await this.fetchInfo().catch(() => undefined)
        if (this.info) this.info.mustChangePassword = false
        return true
      }
      return false
    },
    /** 标记需要改密（任意接口返回 40301 时） */
    markMustChangePassword() {
      if (this.info) this.info.mustChangePassword = true
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
          await authApi.logout(tokenStore.refresh || undefined)
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
