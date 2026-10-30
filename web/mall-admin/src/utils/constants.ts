/**
 * 枚举与展示映射。tone 对应 StatusTag 的色调，集中维护以保证全站状态颜色一致。
 */
export type Tone = 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'purple' | 'cyan'

export interface EnumOption<V extends string | number = string> {
  value: V
  label: string
  tone?: Tone
}

export function toMap<V extends string | number>(opts: EnumOption<V>[]) {
  return Object.fromEntries(opts.map((o) => [String(o.value), o])) as Record<string, EnumOption<V>>
}

export const ORDER_STATUS: EnumOption[] = [
  { value: 'PENDING_PAY', label: '待付款', tone: 'warning' },
  { value: 'PENDING_DELIVERY', label: '待发货', tone: 'primary' },
  { value: 'DELIVERED', label: '待收货', tone: 'cyan' },
  { value: 'COMPLETED', label: '已完成', tone: 'success' },
  { value: 'CANCELLED', label: '已取消', tone: 'info' },
]

export const ORDER_TYPE: EnumOption[] = [
  { value: 'NORMAL', label: '普通订单', tone: 'info' },
  { value: 'SECKILL', label: '秒杀订单', tone: 'danger' },
]

export const PAY_TYPE: EnumOption[] = [
  { value: 'BALANCE', label: '余额支付' },
  { value: 'WECHAT', label: '微信支付' },
  { value: 'ALIPAY', label: '支付宝' },
  { value: 'MOCK', label: '模拟支付' },
]

export const AFTERSALE_STATUS: EnumOption[] = [
  { value: 'APPLYING', label: '待审核', tone: 'warning' },
  { value: 'WAIT_RETURN', label: '待买家退货', tone: 'primary' },
  { value: 'RETURNING', label: '退货中', tone: 'cyan' },
  { value: 'REFUNDED', label: '已退款', tone: 'success' },
  { value: 'REJECTED', label: '已拒绝', tone: 'danger' },
  { value: 'CANCELLED', label: '已撤销', tone: 'info' },
]

export const AFTERSALE_TYPE: EnumOption[] = [
  { value: 'REFUND', label: '仅退款', tone: 'primary' },
  { value: 'RETURN_REFUND', label: '退货退款', tone: 'purple' },
]

export const ITEM_AFTERSALE_STATUS: EnumOption[] = [
  { value: 'NONE', label: '' },
  { value: 'APPLYING', label: '售后中', tone: 'warning' },
  { value: 'REFUNDED', label: '已退款', tone: 'info' },
]

export const GOODS_STATUS: EnumOption[] = [
  { value: 'ON_SALE', label: '在售', tone: 'success' },
  { value: 'OFF_SALE', label: '已下架', tone: 'info' },
]

export const COUPON_TYPE: EnumOption[] = [
  { value: 'FULL_REDUCTION', label: '满减券', tone: 'danger' },
  { value: 'DISCOUNT', label: '折扣券', tone: 'warning' },
  { value: 'NO_THRESHOLD', label: '无门槛券', tone: 'purple' },
]

export const COUPON_SCOPE: EnumOption[] = [
  { value: 'ALL', label: '全场通用' },
  { value: 'CATEGORY', label: '指定类目' },
  { value: 'GOODS', label: '指定商品' },
]

export const COUPON_VALID_TYPE: EnumOption[] = [
  { value: 'FIXED', label: '固定时间段' },
  { value: 'DAYS', label: '领取后 N 天有效' },
]

export const MEMBER_COUPON_STATUS: EnumOption[] = [
  { value: 'UNUSED', label: '未使用', tone: 'primary' },
  { value: 'USED', label: '已使用', tone: 'success' },
  { value: 'EXPIRED', label: '已过期', tone: 'info' },
]

export const SECKILL_STATE: EnumOption[] = [
  { value: 'NOT_STARTED', label: '未开始', tone: 'warning' },
  { value: 'ONGOING', label: '进行中', tone: 'success' },
  { value: 'ENDED', label: '已结束', tone: 'info' },
]

export const LINK_TYPE: EnumOption[] = [
  { value: 'NONE', label: '无链接' },
  { value: 'GOODS', label: '商品' },
  { value: 'CATEGORY', label: '类目' },
  { value: 'URL', label: '网页链接' },
]

export const BANNER_POSITION: EnumOption[] = [{ value: 'HOME', label: '首页轮播' }]

export const MENU_TYPE: EnumOption[] = [
  { value: 'DIR', label: '目录', tone: 'warning' },
  { value: 'MENU', label: '菜单', tone: 'primary' },
  { value: 'BUTTON', label: '按钮', tone: 'info' },
]

export const ACCOUNT_TYPE: EnumOption[] = [
  { value: 'BALANCE', label: '余额', tone: 'primary' },
  { value: 'POINTS', label: '积分', tone: 'warning' },
  { value: 'GROWTH', label: '成长值', tone: 'success' },
]

export const ACCOUNT_BIZ_TYPE: EnumOption[] = [
  { value: 'ORDER_PAY', label: '订单支付' },
  { value: 'ORDER_REFUND', label: '订单退款' },
  { value: 'ORDER_REWARD', label: '订单奖励' },
  { value: 'ADMIN_ADJUST', label: '后台调整' },
  { value: 'RECHARGE', label: '充值' },
  { value: 'SIGN_IN', label: '签到' },
]

export const GENDER: EnumOption<number>[] = [
  { value: 0, label: '未知' },
  { value: 1, label: '男' },
  { value: 2, label: '女' },
]

export const ENABLE_STATUS: EnumOption<number>[] = [
  { value: 1, label: '启用', tone: 'success' },
  { value: 0, label: '停用', tone: 'info' },
]

export const MEMBER_STATUS: EnumOption<number>[] = [
  { value: 1, label: '正常', tone: 'success' },
  { value: 0, label: '冻结', tone: 'danger' },
]

export const USER_TYPE: EnumOption[] = [
  { value: 'ADMIN', label: '管理员', tone: 'primary' },
  { value: 'MEMBER', label: '会员', tone: 'success' },
]

/** 超级管理员角色编码：拥有全部权限 */
export const SUPER_ADMIN = 'super_admin'
