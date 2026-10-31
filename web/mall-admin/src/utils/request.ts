import axios, { type AxiosError, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult, TokenPair } from '@/types'
import { StorageKey, storage, tokenStore } from './storage'

/** 业务码，见 docs/mall/api.md §0 */
export const ResultCode = {
  OK: 0,
  NEED_CAPTCHA: 40002,
  UNAUTHORIZED: 40100,
  FORBIDDEN: 40300,
  MUST_CHANGE_PASSWORD: 40301,
  TOO_MANY: 42900,
  BIZ: 60000,
} as const

declare module 'axios' {
  interface AxiosRequestConfig {
    /** 为 true 时不弹出错误提示（调用方自行处理） */
    silent?: boolean
    /** 内部标记：已经重试过一次刷新令牌 */
    _retried?: boolean
  }
}

/** 业务异常：携带后端 code、msg 与 data（如 needCaptcha），供调用方区分 */
export class ApiError extends Error {
  constructor(
    public code: number,
    message: string,
    public data?: unknown,
  ) {
    super(message)
  }
}

/** 会话事件：由路由层注册，避免 request ↔ router 循环依赖 */
export const sessionEvents = {
  /** 任意接口返回 40301：跳转强制改密页 */
  onMustChangePassword: () => {},
}

const REFRESH_URL = '/admin/auth/refresh'
const LOGIN_URL = '/admin/auth/login'
const LOGIN_PATH = '/console/login'

// Nginx 会去掉 /api 前缀转发到后端
const service = axios.create({ baseURL: '/api', timeout: 20000 })

service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.access
  if (token && config.url !== REFRESH_URL) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ---------------------------------------------------------------------------
// 刷新令牌
// - 同一标签页内：并发的 40100 请求共享同一次刷新；
// - 多个标签页之间：refreshToken 一次性，必须互斥。优先用 Web Locks API，不支持时用 localStorage 锁；
//   拿到锁后先看令牌是否已被其他标签页换新，换过就直接用新令牌，不再发起刷新；
// - 只有刷新接口明确返回 40100 / HTTP 401 才判定登录失效；网络错误、超时、5xx 保留登录态，提示稍后重试。
// ---------------------------------------------------------------------------

/** 刷新被服务端明确拒绝（refreshToken 无效 / 已用过 / 已过期） */
class RefreshRejected extends ApiError {}

const LOCK_NAME = 'mall-admin-refresh-token'
const LOCK_TTL = 15000

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms))

async function withCrossTabLock<T>(fn: () => Promise<T>): Promise<T> {
  const locks = (navigator as Navigator & { locks?: LockManager }).locks
  if (locks?.request) return locks.request(LOCK_NAME, fn) as Promise<T>
  // 回退：localStorage 锁（带过期时间，防止标签页崩溃后死锁）
  const me = `${Date.now()}-${Math.random().toString(36).slice(2)}`
  const deadline = Date.now() + LOCK_TTL
  while (Date.now() < deadline) {
    const raw = storage.get(StorageKey.refreshLock)
    const [owner, at] = (raw || '').split('|')
    if (!owner || Date.now() - Number(at) > LOCK_TTL) {
      storage.set(StorageKey.refreshLock, `${me}|${Date.now()}`)
      await sleep(30)
      if ((storage.get(StorageKey.refreshLock) || '').startsWith(me)) break
    }
    await sleep(120)
  }
  try {
    return await fn()
  } finally {
    if ((storage.get(StorageKey.refreshLock) || '').startsWith(me)) storage.remove(StorageKey.refreshLock)
  }
}

let refreshing: Promise<string> | null = null

/**
 * @param staleAccess 失败请求所携带的 access 令牌：若存储中的令牌已经不同，说明别处已刷新过
 */
function refreshToken(staleAccess: string): Promise<string> {
  if (refreshing) return refreshing
  refreshing = withCrossTabLock(async () => {
    const current = tokenStore.access
    if (current && current !== staleAccess) return current
    const rt = tokenStore.refresh
    if (!rt) throw new RefreshRejected(ResultCode.UNAUTHORIZED, '登录已过期')
    let body: ApiResult<TokenPair>
    try {
      const res = await axios.post<ApiResult<TokenPair>>(`/api${REFRESH_URL}`, { refreshToken: rt }, { timeout: 15000 })
      body = res.data
    } catch (e) {
      const err = e as AxiosError<ApiResult>
      if (err.response?.status === 401 || err.response?.data?.code === ResultCode.UNAUTHORIZED) {
        throw new RefreshRejected(ResultCode.UNAUTHORIZED, '登录已过期')
      }
      throw new ApiError(err.response?.status ?? -1, '网络异常，登录状态暂时无法刷新，请稍后重试')
    }
    if (body?.code === ResultCode.OK && body.data?.accessToken) {
      tokenStore.save(body.data.accessToken, body.data.refreshToken)
      return body.data.accessToken
    }
    if (body?.code === ResultCode.UNAUTHORIZED) throw new RefreshRejected(body.code, body.msg || '登录已过期')
    throw new ApiError(body?.code ?? -1, body?.msg || '登录状态暂时无法刷新，请稍后重试')
  }).finally(() => {
    refreshing = null
  })
  return refreshing
}

let redirecting = false

/** 清空登录态并跳转登录页（整页跳转以重置所有内存状态） */
export function toLogin(tip = '登录已过期，请重新登录') {
  tokenStore.clear()
  if (redirecting) return
  redirecting = true
  const current = window.location.pathname.replace(/^\/console/, '') + window.location.search
  const skip = !current || current.startsWith('/login') || current.startsWith('/change-password')
  const redirect = skip ? '' : `?redirect=${encodeURIComponent(current)}`
  if (tip) ElMessage.warning(tip)
  window.location.href = `${LOGIN_PATH}${redirect}`
}

// 其他标签页退出登录（令牌被清空）时，本页同步回到登录页
window.addEventListener('storage', (e) => {
  if (e.key === StorageKey.accessToken && !e.newValue && !window.location.pathname.startsWith(LOGIN_PATH)) {
    toLogin('已在其他页面退出登录')
  }
})

function bearerOf(config: AxiosRequestConfig): string {
  const h = config.headers as Record<string, unknown> | undefined
  const v = h?.Authorization
  return typeof v === 'string' ? v.replace(/^Bearer\s+/i, '') : ''
}

async function handleUnauthorized(config: AxiosRequestConfig, msg?: string): Promise<AxiosResponse> {
  if (config.url === LOGIN_URL) {
    if (!config.silent) ElMessage.error(msg || '用户名或密码错误')
    throw new ApiError(ResultCode.UNAUTHORIZED, msg || '用户名或密码错误')
  }
  if (config._retried || config.url === REFRESH_URL) {
    toLogin()
    throw new ApiError(ResultCode.UNAUTHORIZED, '登录已过期')
  }
  let token: string
  try {
    token = await refreshToken(bearerOf(config))
  } catch (e) {
    if (e instanceof RefreshRejected) {
      toLogin()
    } else if (!config.silent) {
      ElMessage.error((e as Error).message || '网络异常，请稍后重试')
    }
    throw e
  }
  config._retried = true
  config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
  return service.request(config)
}

let pwdTipAt = 0

service.interceptors.response.use(
  async (response) => {
    const config = response.config
    // 文件下载等非 JSON 响应直接返回
    if (config.responseType === 'blob') return response
    const body = response.data as ApiResult
    if (!body || typeof body.code !== 'number') return response
    if (body.code === ResultCode.OK) return response
    if (body.code === ResultCode.UNAUTHORIZED) {
      return handleUnauthorized(config, body.msg)
    }
    const msg = body.msg || '请求失败'
    if (body.code === ResultCode.MUST_CHANGE_PASSWORD) {
      if (Date.now() - pwdTipAt > 3000) {
        pwdTipAt = Date.now()
        ElMessage.warning(msg || '请先修改初始密码')
      }
      sessionEvents.onMustChangePassword()
      throw new ApiError(body.code, msg, body.data)
    }
    if (!config.silent) {
      ElMessage.error(body.code === ResultCode.FORBIDDEN ? msg || '没有操作权限' : msg)
    }
    throw new ApiError(body.code, msg, body.data)
  },
  async (error: AxiosError<ApiResult>) => {
    const config = error.config
    const status = error.response?.status
    if (status === 401 && config) return handleUnauthorized(config, error.response?.data?.msg)
    let msg = error.response?.data?.msg || error.message || '网络异常'
    if (error.code === 'ECONNABORTED') msg = '请求超时，请稍后重试'
    else if (!error.response) msg = '网络连接失败，请检查网络'
    else if (status === 403) msg = '没有操作权限'
    else if (status && status >= 500 && !error.response?.data?.msg) msg = '服务器开小差了，请稍后重试'
    if (!config?.silent) ElMessage.error(msg)
    throw new ApiError(error.response?.data?.code ?? status ?? -1, msg, error.response?.data?.data)
  },
)

/** 返回 data 字段的请求方法 */
async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const res = await service.request<ApiResult<T>>(config)
  return (res.data as ApiResult<T>).data
}

export const http = {
  get<T = unknown>(url: string, params?: object, config?: AxiosRequestConfig) {
    return request<T>({ ...config, url, method: 'get', params })
  },
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return request<T>({ ...config, url, method: 'post', data })
  },
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return request<T>({ ...config, url, method: 'put', data })
  },
  delete<T = unknown>(url: string, config?: AxiosRequestConfig) {
    return request<T>({ ...config, url, method: 'delete' })
  },
  upload<T = unknown>(url: string, form: FormData, config?: AxiosRequestConfig) {
    return request<T>({ ...config, url, method: 'post', data: form, timeout: 120000 })
  },
}

export default service
