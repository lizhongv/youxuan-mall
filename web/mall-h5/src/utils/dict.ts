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
  REFUNDING: '退款中',
  REFUNDED: '退款成功',
  REJECTED: '已拒绝',
  CANCELLED: '已撤销',
}

export const ITEM_AFTERSALE_TEXT: Record<string, string> = {
  APPLYING: '售后中',
  REFUNDED: '已退款',
}

/** 售后原因（字典 aftersale_reason 的内置回退） */
export const AFTERSALE_REASONS = ['不想要了', '商品与描述不符', '质量问题', '少件/漏发', '发错货', '其他']

/** 取消原因（字典 cancel_reason 的内置回退） */
export const CANCEL_REASONS = ['不想买了', '信息填写错误', '重复下单', '其他']

export const PAY_TYPE_TEXT: Record<string, string> = {
  BALANCE: '余额支付',
  MOCK: '模拟支付',
  WECHAT: '微信支付',
  WECHAT_JSAPI: '微信支付',
  WECHAT_H5: '微信支付',
  ALIPAY: '支付宝',
  ALIPAY_WAP: '支付宝',
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

/** 常用快递公司（字典 express_company 的内置回退；value 为快递100 编码） */
export const EXPRESS_COMPANIES: { label: string; value: string }[] = [
  { label: '顺丰速运', value: 'shunfeng' },
  { label: '中通快递', value: 'zhongtong' },
  { label: '圆通速递', value: 'yuantong' },
  { label: '韵达快递', value: 'yunda' },
  { label: '申通快递', value: 'shentong' },
  { label: '京东物流', value: 'jd' },
  { label: 'EMS', value: 'ems' },
  { label: '极兔速递', value: 'jtexpress' },
]
