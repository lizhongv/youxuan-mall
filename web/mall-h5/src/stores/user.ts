import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { Profile, TokenResult } from '@/types'
import { storage, KEYS } from '@/utils/storage'
import { getProfile, logout as logoutApi } from '@/api/member'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref<string | null>(storage.get<string>(KEYS.ACCESS))
  const profile = ref<Profile | null>(null)

  const isLogin = computed(() => !!accessToken.value)

  function setToken(t: TokenResult) {
    accessToken.value = t.accessToken
    storage.set(KEYS.ACCESS, t.accessToken)
    storage.set(KEYS.REFRESH, t.refreshToken)
  }

  /** 拉取会员信息；未登录时直接返回 */
  async function fetchProfile() {
    if (!isLogin.value) return null
    try {
      profile.value = await getProfile(true)
    } catch {
      /* 401 已由请求层处理 */
    }
    return profile.value
  }

  /** 退出登录：通知服务端作废 access 与 refresh 令牌（失败不影响本地退出） */
  async function logout() {
    const rt = storage.get<string>(KEYS.REFRESH)
    try {
      if (accessToken.value) await logoutApi(rt)
    } catch {
      /* 令牌可能已失效，忽略 */
    }
    clear()
  }

  function clear() {
    accessToken.value = null
    profile.value = null
    storage.remove(KEYS.ACCESS)
    storage.remove(KEYS.REFRESH)
  }

  // 请求层刷新失败时派发事件，保持 store 与本地存储一致
  window.addEventListener('mall:logout', () => {
    accessToken.value = null
    profile.value = null
  })

  return { accessToken, profile, isLogin, setToken, fetchProfile, logout, clear }
})
