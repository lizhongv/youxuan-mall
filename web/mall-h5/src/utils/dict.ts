import type { AftersaleStatus, AftersaleType, OrderStatus } from '@/types'

/** 订单状态文案（后端返回 statusText 时优先用后端） */
export const ORDER_STATUS_TEXT: Record<OrderStatus, string> = {
  PENDING_PAY: '待付款',
  PENDING_DELIVERY: '待发货',
  DELIVERED: '待收货',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

/** 订单详情头部说明 */
export const ORDER_STATUS_DESC: Record<OrderStatus, string> = {
  PENDING_PAY: '请尽快完成支付，超时订单将自动取消',
  PENDING_DELIVERY: '商家正在打包商品，请耐心等待',
  DELIVERED: '商品已发出，请注意查收',
  COMPLETED: '订单已完成，感谢您的购买',
  CANCELLED: '订单已取消',
}

export const AFTERSALE_TYPE_TEXT: Record<AftersaleType, string> = {
  REFUND: '仅退款',
  RETURN_REFUND: '退货退款',
}

export const AFTERSALE_STATUS_TEXT: Record<AftersaleStatus, string> = {
  APPLYING: '待审核',
  WAIT_RETURN: '待寄回商品',
  RETURNING: '退货中',
  REFUNDED: '退款成功',
  REJECTED: '已拒绝',
  CANCELLED: '已撤销',
}

export const ITEM_AFTERSALE_TEXT: Record<string, string> = {
  APPLYING: '售后中',
  REFUNDED: '已退款',
}

/** 售后原因（固定列表） */
export const AFTERSALE_REASONS = ['不想要了', '商品与描述不符', '质量问题', '少件/漏发', '发错货', '其他']

export const PAY_TYPE_TEXT: Record<string, string> = {
  BALANCE: '余额支付',
  MOCK: '模拟支付',
  WECHAT: '微信支付',
  ALIPAY: '支付宝',
}

export const ACCOUNT_BIZ_TEXT: Record<string, string> = {
  ORDER_PAY: '订单支付',
  ORDER_REFUND: '订单退款',
  ORDER_REWARD: '购物奖励',
  ADMIN_ADJUST: '系统调整',
  RECHARGE: '充值',
  SIGN_IN: '签到',
}

export const GENDER_TEXT = ['保密', '男', '女']

/** 常用快递公司（退货物流选择） */
export const EXPRESS_COMPANIES = ['顺丰速运', '中通快递', '圆通速递', '韵达快递', '申通快递', '京东物流', 'EMS', '极兔速递', '其他']
