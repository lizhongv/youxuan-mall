import { http } from '@/utils/request'
import type {
  Aftersale,
  FreightTemplate,
  FreightTemplateOption,
  ID,
  Logistics,
  Order,
  OrderDetail,
  OrderStatusCount,
  PageResult,
  Payment,
  Review,
  Switch,
} from '@/types'

export const orderApi = {
  page: (params: object) => http.get<PageResult<Order>>('/admin/trade/order/page', params),
  detail: (id: ID) => http.get<OrderDetail>(`/admin/trade/order/${id}`),
  statusCount: () => http.get<OrderStatusCount>('/admin/trade/order/status-count', undefined, { silent: true }),
  deliver: (id: ID, data: { deliveryCompany: string; deliveryNo: string }) =>
    http.put<void>(`/admin/trade/order/${id}/deliver`, data),
  adjustPrice: (id: ID, adjustAmount: number) => http.put<void>(`/admin/trade/order/${id}/price`, { adjustAmount }),
  remark: (id: ID, adminRemark: string) => http.put<void>(`/admin/trade/order/${id}/remark`, { adminRemark }),
  cancel: (id: ID, reason: string) => http.put<void>(`/admin/trade/order/${id}/cancel`, { reason }),
  /** 物流查询失败不弹全局错误，页面内展示 */
  logistics: (id: ID) => http.get<Logistics>(`/admin/trade/order/${id}/logistics`, undefined, { silent: true }),
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

export const freightApi = {
  page: (params: object) => http.get<PageResult<FreightTemplate>>('/admin/trade/freight-template/page', params),
  list: () => http.get<FreightTemplateOption[]>('/admin/trade/freight-template/list'),
  detail: (id: ID) => http.get<FreightTemplate>(`/admin/trade/freight-template/${id}`),
  create: (data: FreightTemplate) => http.post<ID>('/admin/trade/freight-template', data),
  update: (id: ID, data: FreightTemplate) => http.put<void>(`/admin/trade/freight-template/${id}`, data),
  remove: (id: ID) => http.delete<void>(`/admin/trade/freight-template/${id}`),
}

export const paymentApi = {
  page: (params: object) => http.get<PageResult<Payment>>('/admin/trade/payment/page', params),
  sync: (id: ID) => http.post<Payment>(`/admin/trade/payment/${id}/sync`),
}
