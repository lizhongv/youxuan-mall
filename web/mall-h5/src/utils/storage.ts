/** 本地存储封装：读写失败（隐私模式等）时静默降级 */
const PREFIX = 'mall_h5_'

export const storage = {
  get<T = string>(key: string, fallback: T | null = null): T | null {
    try {
      const raw = localStorage.getItem(PREFIX + key)
      return raw == null ? fallback : (JSON.parse(raw) as T)
    } catch {
      return fallback
    }
  },
  set(key: string, value: unknown) {
    try {
      localStorage.setItem(PREFIX + key, JSON.stringify(value))
    } catch {
      /* ignore */
    }
  },
  remove(key: string) {
    try {
      localStorage.removeItem(PREFIX + key)
    } catch {
      /* ignore */
    }
  },
}

export const KEYS = {
  ACCESS: 'access_token',
  REFRESH: 'refresh_token',
  SEARCH_HISTORY: 'search_history',
  CHECKOUT: 'checkout_payload',
} as const
