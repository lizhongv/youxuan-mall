import axios, { type AxiosError, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult, TokenPair } from '@/types'
import { tokenStore } from './storage'

/** 业务码，见 docs/mall/api.md §0 */
export const ResultCode = {
  OK: 0,
  UNAUTHORIZED: 40100,
  FORBIDDEN: 40300,
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

/** 业务异常：携带后端 code 与 msg，供调用方区分 */
export class ApiError extends Error {
  constructor(
    public code: number,
    message: string,
  ) {
    super(message)
  }
}

const REFRESH_URL = '/admin/auth/refresh'
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

/**
 * 刷新令牌：同一时刻只发起一次刷新，其余 40100 请求等待同一个 Promise，
 * 避免并发请求各自刷新导致旧 refreshToken 失效。
 */
let refreshing: Promise<string> | null = null

function refreshToken(): Promise<string> {
  if (refreshing) return refreshing
  const rt = tokenStore.refresh
  if (!rt) return Promise.reject(new ApiError(ResultCode.UNAUTHORIZED, '登录已过期'))
  refreshing = axios
    .post<ApiResult<TokenPair>>(`/api${REFRESH_URL}`, { refreshToken: rt })
    .then((res) => {
      const body = res.data
      if (body.code !== ResultCode.OK || !body.data?.accessToken) {
        throw new ApiError(body.code, body.msg || '登录已过期')
      }
      tokenStore.save(body.data.accessToken, body.data.refreshToken)
      return body.data.accessToken
    })
    .finally(() => {
      refreshing = null
    })
  return refreshing
}

let redirecting = false

/** 清空登录态并跳转登录页（整页跳转以重置所有内存状态） */
export function toLogin() {
  tokenStore.clear()
  if (redirecting) return
  redirecting = true
  const current = window.location.pathname.replace(/^\/console/, '') + window.location.search
  const redirect = current && !current.startsWith('/login') ? `?redirect=${encodeURIComponent(current)}` : ''
  ElMessage.warning('登录已过期，请重新登录')
  window.location.href = `${LOGIN_PATH}${redirect}`
}

async function handleUnauthorized(config: AxiosRequestConfig): Promise<AxiosResponse> {
  if (config._retried || config.url === REFRESH_URL || config.url === '/admin/auth/login') {
    toLogin()
    throw new ApiError(ResultCode.UNAUTHORIZED, '登录已过期')
  }
  try {
    const token = await refreshToken()
    config._retried = true
    config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
    return service.request(config)
  } catch (e) {
    toLogin()
    throw e
  }
}

service.interceptors.response.use(
  async (response) => {
    const config = response.config
    // 文件下载等非 JSON 响应直接返回
    if (config.responseType === 'blob') return response
    const body = response.data as ApiResult
    if (!body || typeof body.code !== 'number') return response
    if (body.code === ResultCode.OK) return response
    if (body.code === ResultCode.UNAUTHORIZED) {
      return handleUnauthorized(config)
    }
    const msg = body.msg || '请求失败'
    if (!config.silent) {
      ElMessage.error(body.code === ResultCode.FORBIDDEN ? msg || '没有操作权限' : msg)
    }
    throw new ApiError(body.code, msg)
  },
  async (error: AxiosError<ApiResult>) => {
    const config = error.config
    const status = error.response?.status
    if (status === 401 && config) return handleUnauthorized(config)
    let msg = error.response?.data?.msg || error.message || '网络异常'
    if (error.code === 'ECONNABORTED') msg = '请求超时，请稍后重试'
    else if (!error.response) msg = '网络连接失败，请检查网络'
    else if (status === 403) msg = '没有操作权限'
    else if (status && status >= 500 && !error.response?.data?.msg) msg = '服务器开小差了，请稍后重试'
    if (!config?.silent) ElMessage.error(msg)
    throw new ApiError(error.response?.data?.code ?? status ?? -1, msg)
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
