import { http } from './request'
import type { ConfirmResult, Order, OrderQueryStatus, OrderSubmit, PageResult, PayType, SubmitResult } from '@/types'

/** 2.6 订单 */
export const confirmOrder = (data: OrderSubmit) => http.post<ConfirmResult>('/app/order/confirm', data)

/** 提交订单：幂等键由调用方生成，同一次提交重试时复用 */
export const submitOrder = (data: OrderSubmit, idempotentKey: string) =>
  http.post<SubmitResult>('/app/order', data, { headers: { 'X-Idempotent-Key': idempotentKey } })

export const getOrderPage = (params: { page: number; size: number; status?: OrderQueryStatus }) =>
  http.get<PageResult<Order>>('/app/order/page', { ...params, status: params.status || undefined })

export const getOrder = (id: string) => http.get<Order>(`/app/order/${id}`)

export const payOrder = (id: string, payType: PayType, idempotentKey: string) =>
  http.post<{ paid: boolean }>(`/app/order/${id}/pay`, { payType }, { headers: { 'X-Idempotent-Key': idempotentKey } })

export const cancelOrder = (id: string, reason: string) => http.put<void>(`/app/order/${id}/cancel`, { reason })

export const receiveOrder = (id: string) => http.put<void>(`/app/order/${id}/receive`)

export const deleteOrder = (id: string) => http.delete<void>(`/app/order/${id}`)
