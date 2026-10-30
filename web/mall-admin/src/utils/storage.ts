/** 本地存储键集中定义，避免散落魔法字符串 */
const PREFIX = 'mall-admin-'

export const StorageKey = {
  accessToken: `${PREFIX}access-token`,
  refreshToken: `${PREFIX}refresh-token`,
  rememberUser: `${PREFIX}remember-username`,
  theme: `${PREFIX}theme`,
  collapsed: `${PREFIX}sidebar-collapsed`,
  tabs: `${PREFIX}tabs`,
} as const

function safe<T>(fn: () => T, fallback: T): T {
  try {
    return fn()
  } catch {
    return fallback
  }
}

export const storage = {
  get(key: string): string | null {
    return safe(() => localStorage.getItem(key), null)
  },
  set(key: string, value: string) {
    safe(() => localStorage.setItem(key, value), undefined)
  },
  remove(key: string) {
    safe(() => localStorage.removeItem(key), undefined)
  },
  getJSON<T>(key: string, fallback: T): T {
    const raw = storage.get(key)
    if (!raw) return fallback
    return safe(() => JSON.parse(raw) as T, fallback)
  },
  setJSON(key: string, value: unknown) {
    storage.set(key, JSON.stringify(value))
  },
}

export const tokenStore = {
  get access() {
    return storage.get(StorageKey.accessToken) || ''
  },
  get refresh() {
    return storage.get(StorageKey.refreshToken) || ''
  },
  save(access: string, refresh: string) {
    storage.set(StorageKey.accessToken, access)
    storage.set(StorageKey.refreshToken, refresh)
  },
  clear() {
    storage.remove(StorageKey.accessToken)
    storage.remove(StorageKey.refreshToken)
  },
}
