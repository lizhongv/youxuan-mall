/** 统一响应体，见 docs/mall/api.md §0 */
export interface ApiResult<T = unknown> {
  code: number
  msg: string
  data: T
  ts?: number
  traceId?: string
}

/** 分页出参 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}

/** 分页入参基础字段 */
export interface PageQuery {
  page: number
  size: number
  keyword?: string
  [key: string]: unknown
}

/** 所有 ID 都是字符串（后端 Long → String） */
export type ID = string

/** 1 启用 / 0 停用 */
export type Switch = 0 | 1
