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

export type AftersaleStatus = 'APPLYING' | 'WAIT_RETURN' | 'RETURNING' | 'REFUNDING' | 'REFUNDED' | 'REJECTED' | 'CANCELLED'
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
  /** 商家退货地址：退货退款且状态为 WAIT_RETURN 及之后时返回 */
  returnAddress?: ReturnAddress | null
}

export interface ReturnAddress {
  name: string
  mobile: string
  address: string
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

// ---------------- 运费模板 ----------------
export type ChargeType = 'BY_COUNT' | 'BY_WEIGHT'

export interface FreightRule {
  /** 空数组 = 默认地区（全国其他地区） */
  provinces: string[]
  firstUnit: number
  firstFee: number
  nextUnit: number
  nextFee: number
}

export interface FreightFree {
  /** 空数组 = 全部地区 */
  provinces: string[]
  minCount: number
  minAmount: number
}

export interface FreightTemplate {
  id?: ID
  name: string
  chargeType: ChargeType
  isDefault: Switch
  status: Switch
  rules: FreightRule[]
  frees: FreightFree[]
  createdAt?: string
}

export interface FreightTemplateOption {
  id: ID
  name: string
  chargeType: ChargeType
  isDefault: Switch
}

// ---------------- 支付流水 ----------------
export type PayChannel = 'BALANCE' | 'WECHAT' | 'ALIPAY' | 'MOCK'
export type PaymentStatus = 'WAITING' | 'SUCCESS' | 'CLOSED' | 'REFUNDED' | 'REFUNDED_AFTER_CLOSE'

export interface Payment {
  id: ID
  payNo: string
  orderId: ID
  orderNo: string
  memberId: ID
  channel: PayChannel
  channelText: string
  tradeType: string
  amount: number
  status: PaymentStatus
  statusText: string
  channelNo: string
  paidAt: string
  remark: string
  createdAt: string
}

// ---------------- 物流 ----------------
export interface LogisticsTrace {
  time: string
  context: string
  location?: string
}

export interface Logistics {
  company: string
  companyName: string
  trackingNo: string
  state: string
  stateText: string
  traces: LogisticsTrace[]
  message: string | null
}
