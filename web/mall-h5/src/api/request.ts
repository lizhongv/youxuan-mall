import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { showToast } from 'vant'
import type { ApiResult, TokenResult } from '@/types'
import { storage, KEYS } from '@/utils/storage'
import router from '@/router'

/** 业务码 */
export const CODE = { OK: 0, UNAUTHORIZED: 40100, FORBIDDEN: 40300, BIZ: 60000 } as const

/** 扩展配置：silent 不弹出错误提示；_retried 内部标记已重试 */
export interface ReqConfig extends AxiosRequestConfig {
  silent?: boolean
  _retried?: boolean
}

/** 业务异常：携带 code，便于页面按需处理 */
export class ApiError extends Error {
  code: number
  constructor(code: number, msg: string) {
    super(msg)
    this.code = code
  }
}

const service = axios.create({ baseURL: '/api', timeout: 15000 })


/* ---------------- 刷新令牌：并发请求排队，只刷新一次 ---------------- */
let refreshing: Promise<boolean> | null = null

function refreshToken(): Promise<boolean> {
  if (refreshing) return refreshing
  const rt = storage.get<string>(KEYS.REFRESH)
  if (!rt) return Promise.resolve(false)
  refreshing = axios
    .post<ApiResult<TokenResult>>('/api/app/auth/refresh', { refreshToken: rt })
    .then((res) => {
      if (res.data.code === CODE.OK && res.data.data?.accessToken) {
        storage.set(KEYS.ACCESS, res.data.data.accessToken)
        storage.set(KEYS.REFRESH, res.data.data.refreshToken)
        return true
      }
      return false
    })
    .catch(() => false)
    .finally(() => {
      // 稍后释放，让同一批排队请求共享结果
      setTimeout(() => (refreshing = null), 0)
    })
  return refreshing
}

let redirecting = false
function toLogin() {
  storage.remove(KEYS.ACCESS)
  storage.remove(KEYS.REFRESH)
  // 通知 store 清理（避免循环依赖，用事件）
  window.dispatchEvent(new Event('mall:logout'))
  if (redirecting) return
  const current = router.currentRoute.value
  if (current.name === 'login') return
  redirecting = true
  router.replace({ name: 'login', query: { redirect: current.fullPath } }).finally(() => {
    redirecting = false
  })
}

async function handleUnauthorized<T>(config: ReqConfig): Promise<T> {
  const current = storage.get<string>(KEYS.ACCESS)
  const used = String((config.headers as Record<string, unknown> | undefined)?.Authorization || '').replace('Bearer ', '')
  // 令牌已被其它并发请求刷新过：直接用新令牌重试
  if (!config._retried && current && used && current !== used) {
    return request<T>({ ...config, _retried: true })
  }
  if (!config._retried && current) {
    const ok = await refreshToken()
    if (ok) return request<T>({ ...config, _retried: true })
  }
  if (config.silent) {
    // 后台静默请求（角标、资料预取）只清理登录态，不打断用户当前浏览
    storage.remove(KEYS.ACCESS)
    storage.remove(KEYS.REFRESH)
    window.dispatchEvent(new Event('mall:logout'))
  } else {
    toLogin()
    showToast('请先登录')
  }
  throw new ApiError(CODE.UNAUTHORIZED, '请先登录')
}

/** 核心请求：解包 {code,msg,data}，返回 data */
export async function request<T = unknown>(rawConfig: ReqConfig): Promise<T> {
  // 每次请求时读取最新令牌（刷新后的重试也会拿到新令牌），并记录本次使用的令牌
  const token = storage.get<string>(KEYS.ACCESS)
  const config: ReqConfig = {
    ...rawConfig,
    headers: { ...(rawConfig.headers as Record<string, string>), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
  }
  let body: ApiResult<T>
  try {
    const res = await service.request<ApiResult<T>>(config)
    body = res.data
  } catch (e) {
    const err = e as AxiosError<ApiResult<T>>
    if (err.response?.status === 401 || err.response?.data?.code === CODE.UNAUTHORIZED) {
      return handleUnauthorized<T>(config)
    }
    const msg =
      err.response?.data?.msg ||
      (err.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : err.response ? `服务异常（${err.response.status}）` : '网络连接失败')
    if (!config.silent) showToast(msg)
    throw new ApiError(err.response?.data?.code ?? -1, msg)
  }
  if (body && typeof body === 'object' && 'code' in body) {
    if (body.code === CODE.OK) return body.data
    if (body.code === CODE.UNAUTHORIZED) return handleUnauthorized<T>(config)
    if (!config.silent) showToast(body.msg || '操作失败')
    throw new ApiError(body.code, body.msg)
  }
  return body as unknown as T
}

export const http = {
  get<T>(url: string, params?: object, cfg?: ReqConfig) {
    return request<T>({ ...cfg, url, method: 'GET', params })
  },
  post<T>(url: string, data?: unknown, cfg?: ReqConfig) {
    return request<T>({ ...cfg, url, method: 'POST', data })
  },
  put<T>(url: string, data?: unknown, cfg?: ReqConfig) {
    return request<T>({ ...cfg, url, method: 'PUT', data })
  },
  delete<T>(url: string, data?: unknown, cfg?: ReqConfig) {
    return request<T>({ ...cfg, url, method: 'DELETE', data })
  },
}
