import type { ID, Switch } from './api'

export type OrderStatus = 'PENDING_PAY' | 'PENDING_DELIVERY' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED'
export type OrderType = 'NORMAL' | 'SECKILL'

export interface OrderItem {
  id: ID
  goodsId: ID
  skuId: ID
  goodsName: string
  pic: string
  specText: string
  price: number
  quantity: number
  totalAmount: number
  payAmount: number
  aftersaleStatus: 'NONE' | 'APPLYING' | 'REFUNDED'
  reviewed: Switch
}

export interface OrderLog {
  id?: ID
  fromStatus?: string
  toStatus?: string
  operator?: string
  remark?: string
  createdAt: string
}

export interface Order {
  id: ID
  orderNo: string
  memberId: ID
  memberNickname: string
  orderType: OrderType
  status: OrderStatus
  statusText: string
  goodsAmount: number
  freightAmount: number
  couponAmount: number
  discountAmount: number
  adjustAmount: number
  payAmount: number
  payType: string
  payTime: string
  payExpireAt: string
  receiverName: string
  receiverMobile: string
  receiverProvince: string
  receiverCity: string
  receiverDistrict: string
  receiverDetail: string
  remark: string
  adminRemark: string
  deliveryCompany: string
  deliveryNo: string
  deliveryTime: string
  receiveTime: string
  cancelTime: string
  cancelReason: string
  reviewed: Switch
  createdAt: string
  items: OrderItem[]
}

export interface OrderDetail extends Order {
  logs: OrderLog[]
  member?: { id: ID; nickname: string; mobile: string }
}

export type OrderStatusCount = Partial<Record<OrderStatus, number>>

export type AftersaleStatus = 'APPLYING' | 'WAIT_RETURN' | 'RETURNING' | 'REFUNDED' | 'REJECTED' | 'CANCELLED'
export type AftersaleType = 'REFUND' | 'RETURN_REFUND'

export interface Aftersale {
  id: ID
  aftersaleNo: string
  orderId: ID
  orderNo: string
  orderItemId: ID
  memberId: ID
  memberNickname: string
  type: AftersaleType
  status: AftersaleStatus
  statusText: string
  reason: string
  description: string
  images: string[]
  amount: number
  quantity: number
  auditRemark: string
  auditTime: string
  returnCompany: string
  returnNo: string
  refundTime: string
  createdAt: string
  item?: OrderItem
}

export interface Review {
  id: ID
  orderId: ID
  goodsId: ID
  goodsName: string
  goodsPic: string
  memberId: ID
  nickname: string
  avatar: string
  specText: string
  rating: number
  content: string
  images: string[]
  anonymous: Switch
  reply: string
  replyTime: string
  status: Switch
  createdAt: string
}
