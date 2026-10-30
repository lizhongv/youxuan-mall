import { http } from '@/utils/request'
import { createCrud } from './crud'
import type { Brand, Category, GoodsForm, GoodsItem, GoodsStatus, ID, Switch } from '@/types'

export const categoryApi = {
  tree: () => http.get<Category[]>('/admin/goods/category/tree'),
  create: (data: Partial<Category>) => http.post<ID>('/admin/goods/category', data),
  update: (id: ID, data: Partial<Category>) => http.put<void>(`/admin/goods/category/${id}`, data),
  remove: (id: ID) => http.delete<void>(`/admin/goods/category/${id}`),
}

export const brandApi = {
  ...createCrud<Brand>('goods/brand'),
  list: () => http.get<Brand[]>('/admin/goods/brand/list'),
}

const goodsCrud = createCrud<GoodsItem, GoodsForm>('goods/goods')

export const goodsApi = {
  page: goodsCrud.page,
  detail: (id: ID) => http.get<GoodsForm>(`/admin/goods/goods/${id}`),
  create: goodsCrud.create,
  update: goodsCrud.update,
  remove: goodsCrud.remove,
  setStatus: (ids: ID[], status: GoodsStatus) => http.put<void>('/admin/goods/goods/status', { ids, status }),
  setFlags: (id: ID, flags: { isNew: Switch; isHot: Switch; isRecommend: Switch }) =>
    http.put<void>(`/admin/goods/goods/${id}/flags`, flags),
}
