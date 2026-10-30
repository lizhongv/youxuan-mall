import type { Address } from './member'
import type { CouponOption } from './marketing'

export interface CartItem {
  id: string
  goodsId: string
  skuId: string
  goodsName: string
  pic: string
  specText?: string
  price: number
  quantity: number
  checked: boolean
  stock: number
  valid: boolean
}

export type OrderSource = 'CART' | 'BUY_NOW'

export interface OrderSubmit {
  source: OrderSource
  cartIds?: string[]
  items?: { skuId: string; quantity: number }[]
  seckillId?: string
  addressId?: string
  /** 不传自动选最优券，传 '0' 表示不用券 */
  memberCouponId?: string
  remark?: string
}

export interface ConfirmItem {
  goodsId: string
  skuId: string
  goodsName: string
  pic: string
  specText?: string
  price: number
  quantity: number
  totalAmount: number
}

export interface ConfirmResult {
  items: ConfirmItem[]
  address: Address | null
  goodsAmount: number
  freightAmount: number
  couponAmount: number
  discountAmount: number
  payAmount: number
  memberCouponId?: string | null
  coupons: CouponOption[]
  balance: number
}

export interface SubmitResult {
  orderId: string
  orderNo: string
  payAmount: number
  payExpireAt: string
}

export type OrderStatus = 'PENDING_PAY' | 'PENDING_DELIVERY' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED'
/** 订单列表筛选：额外支持 TO_REVIEW（已完成未评价） */
export type OrderQueryStatus = OrderStatus | 'TO_REVIEW' | ''
export type PayType = 'BALANCE' | 'MOCK'
export type ItemAftersaleStatus = 'NONE' | 'APPLYING' | 'REFUNDED'

export interface OrderItem {
  id: string
  goodsId: string
  skuId: string
  goodsName: string
  pic: string
  specText?: string
  price: number
  quantity: number
  totalAmount: number
  payAmount: number
  aftersaleStatus?: ItemAftersaleStatus | string
  /** 契约未列出 aftersaleId；若后端返回则可直接跳转售后详情 */
  aftersaleId?: string
  reviewed?: number | boolean
}

export interface OrderLog {
  id?: string
  action?: string
  content?: string
  remark?: string
  createdAt: string
}

export interface Order {
  id: string
  orderNo: string
  orderType?: string
  status: OrderStatus
  statusText?: string
  goodsAmount: number
  freightAmount: number
  couponAmount: number
  discountAmount: number
  adjustAmount?: number
  payAmount: number
  payType?: PayType
  payTime?: string
  payExpireAt?: string
  receiverName: string
  receiverMobile: string
  receiverProvince: string
  receiverCity: string
  receiverDistrict: string
  receiverDetail: string
  remark?: string
  deliveryCompany?: string
  deliveryNo?: string
  deliveryTime?: string
  receiveTime?: string
  cancelTime?: string
  cancelReason?: string
  reviewed?: number | boolean
  createdAt: string
  items: OrderItem[]
  logs?: OrderLog[]
}

export type AftersaleType = 'REFUND' | 'RETURN_REFUND'
export type AftersaleStatus = 'APPLYING' | 'WAIT_RETURN' | 'RETURNING' | 'REFUNDED' | 'REJECTED' | 'CANCELLED'

export interface Aftersale {
  id: string
  aftersaleNo: string
  orderId: string
  orderNo: string
  orderItemId: string
  type: AftersaleType
  status: AftersaleStatus
  statusText?: string
  reason: string
  description?: string
  images: string[]
  amount: number
  quantity?: number
  auditRemark?: string
  auditTime?: string
  returnCompany?: string
  returnNo?: string
  refundTime?: string
  createdAt: string
  item?: OrderItem
}

export interface AftersaleForm {
  orderItemId: string
  type: AftersaleType
  reason: string
  description?: string
  images: string[]
  amount: number
}

export interface ReviewForm {
  orderItemId: string
  rating: number
  content: string
  images: string[]
  anonymous: boolean
}
