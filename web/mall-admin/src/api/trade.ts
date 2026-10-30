import { http } from '@/utils/request'
import type { Aftersale, ID, Order, OrderDetail, OrderStatusCount, PageResult, Review, Switch } from '@/types'

export const orderApi = {
  page: (params: object) => http.get<PageResult<Order>>('/admin/trade/order/page', params),
  detail: (id: ID) => http.get<OrderDetail>(`/admin/trade/order/${id}`),
  statusCount: () => http.get<OrderStatusCount>('/admin/trade/order/status-count', undefined, { silent: true }),
  deliver: (id: ID, data: { deliveryCompany: string; deliveryNo: string }) =>
    http.put<void>(`/admin/trade/order/${id}/deliver`, data),
  adjustPrice: (id: ID, adjustAmount: number) => http.put<void>(`/admin/trade/order/${id}/price`, { adjustAmount }),
  remark: (id: ID, adminRemark: string) => http.put<void>(`/admin/trade/order/${id}/remark`, { adminRemark }),
  cancel: (id: ID, reason: string) => http.put<void>(`/admin/trade/order/${id}/cancel`, { reason }),
}

export const aftersaleApi = {
  page: (params: object) => http.get<PageResult<Aftersale>>('/admin/trade/aftersale/page', params),
  detail: (id: ID) => http.get<Aftersale>(`/admin/trade/aftersale/${id}`),
  approve: (id: ID, remark: string) => http.put<void>(`/admin/trade/aftersale/${id}/approve`, { remark }),
  reject: (id: ID, remark: string) => http.put<void>(`/admin/trade/aftersale/${id}/reject`, { remark }),
  receive: (id: ID) => http.put<void>(`/admin/trade/aftersale/${id}/receive`),
}

export const reviewApi = {
  page: (params: object) => http.get<PageResult<Review>>('/admin/trade/review/page', params),
  reply: (id: ID, reply: string) => http.put<void>(`/admin/trade/review/${id}/reply`, { reply }),
  setStatus: (id: ID, status: Switch) => http.put<void>(`/admin/trade/review/${id}/status`, undefined, { params: { status } }),
}
