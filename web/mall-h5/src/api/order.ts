import { http } from './request'
import type {
  ConfirmResult,
  Logistics,
  Order,
  OrderQueryStatus,
  OrderSubmit,
  PageResult,
  PayChannel,
  PayLaunch,
  PayStatus,
  PayType,
  SubmitResult,
} from '@/types'

/** 2.6 订单。结算预览错误（如不在配送范围）由页面展示，故静默 */
export const confirmOrder = (data: OrderSubmit) => http.post<ConfirmResult>('/app/order/confirm', data, { silent: true })

/** 提交订单：幂等键由调用方生成，同一次提交重试时复用 */
export const submitOrder = (data: OrderSubmit, idempotentKey: string) =>
  http.post<SubmitResult>('/app/order', data, { headers: { 'X-Idempotent-Key': idempotentKey } })

export const getOrderPage = (params: { page: number; size: number; status?: OrderQueryStatus }) =>
  http.get<PageResult<Order>>('/app/order/page', { ...params, status: params.status || undefined })

export const getOrder = (id: string) => http.get<Order>(`/app/order/${id}`)

/** 发起支付（需幂等头）；余额支付必须带 payPassword。错误码由收银台按需处理，故静默 */
export const payOrder = (id: string, payType: PayType, idempotentKey: string, payPassword?: string) =>
  http.post<PayLaunch>(
    `/app/order/${id}/pay`,
    { payType, payPassword },
    { headers: { 'X-Idempotent-Key': idempotentKey }, silent: true },
  )

/** 收银台可选支付方式；inWechat 按 UA 是否含 MicroMessenger */
export const getPayChannels = (orderId: string, inWechat: boolean) =>
  http.get<PayChannel[]>('/app/pay/channels', { orderId, inWechat })

/** 支付结果轮询（静默，失败由轮询逻辑处理） */
export const getPayStatus = (id: string) => http.get<PayStatus>(`/app/order/${id}/pay-status`, undefined, { silent: true })

/** 物流轨迹 */
export const getLogistics = (id: string) => http.get<Logistics>(`/app/order/${id}/logistics`)

export const cancelOrder = (id: string, reason: string) => http.put<void>(`/app/order/${id}/cancel`, { reason })

export const receiveOrder = (id: string) => http.put<void>(`/app/order/${id}/receive`)

export const deleteOrder = (id: string) => http.delete<void>(`/app/order/${id}`)
