import { http } from '@/utils/request'
import type { ID, PageResult } from '@/types'

/**
 * 通用 CRUD 工厂：对应契约中「CRUD」五个接口
 * GET /admin/{res}/page、GET /{id}、POST、PUT /{id}、DELETE /{id}
 */
export function createCrud<T, F = Partial<T>>(res: string) {
  const base = `/admin/${res}`
  return {
    page: (params: object) => http.get<PageResult<T>>(`${base}/page`, params),
    detail: (id: ID) => http.get<T>(`${base}/${id}`),
    create: (data: F) => http.post<ID>(base, data),
    update: (id: ID, data: F) => http.put<void>(`${base}/${id}`, data),
    remove: (id: ID) => http.delete<void>(`${base}/${id}`),
  }
}
