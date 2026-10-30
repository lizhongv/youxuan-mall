/** 统一响应 */
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

/** 分页入参 */
export interface PageQuery {
  page?: number
  size?: number
  keyword?: string
}

/** 上传结果（同管理端 1.2） */
export interface UploadResult {
  id: string
  url: string
  originalName: string
  size: number
}
