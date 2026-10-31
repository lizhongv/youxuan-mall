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
/** 发起支付方式（收银台渠道 type） */
export type PayType = 'BALANCE' | 'WECHAT_JSAPI' | 'WECHAT_H5' | 'ALIPAY_WAP' | 'MOCK'

/** 收银台渠道 */
export interface PayChannel {
  type: PayType
  name: string
  /** balance / wechat / alipay / mock */
  icon: string
  enabled: boolean
  reason?: string | null
}

/** 发起支付出参 */
export interface PayLaunch {
  paid: boolean
  payType?: PayType
  payNo?: string
  /** WECHAT_JSAPI：{appId,timeStamp,nonceStr,package,signType,paySign}；WECHAT_H5：{h5Url}；ALIPAY_WAP：{payUrl} */
  payParams?: Record<string, string> | null
}

/** 支付结果轮询 */
export interface PayStatus {
  orderId: string
  status: OrderStatus
  statusText?: string
  paid: boolean
  payType?: string
  payTime?: string
}

/** 物流轨迹 */
export interface LogisticsTrace {
  time: string
  context: string
  location?: string | null
}

export interface Logistics {
  company?: string | null
  companyName?: string | null
  trackingNo?: string | null
  state: string
  stateText?: string | null
  /** 时间倒序 */
  traces: LogisticsTrace[]
  message?: string | null
}
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
  /** 订单支付方式：BALANCE / WECHAT / ALIPAY / MOCK */
  payType?: string
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
export type AftersaleStatus = 'APPLYING' | 'WAIT_RETURN' | 'RETURNING' | 'REFUNDING' | 'REFUNDED' | 'REJECTED' | 'CANCELLED'

/** 商家退货地址 */
export interface ReturnAddress {
  name: string
  mobile: string
  address: string
}

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
  /** 退货退款且状态为 WAIT_RETURN 及之后时返回 */
  returnAddress?: ReturnAddress | null
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
