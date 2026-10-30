import type { Category, GoodsItem, Seckill } from './goods'

export type BannerLinkType = 'NONE' | 'GOODS' | 'CATEGORY' | 'URL'

export interface Banner {
  id: string
  title: string
  image: string
  linkType: BannerLinkType
  linkValue?: string
}

export interface Notice {
  id: string
  title: string
  content?: string
  createdAt?: string
}

export interface HomeData {
  banners: Banner[]
  notices: Notice[]
  categories: Category[]
  seckills: Seckill[]
  newGoods: GoodsItem[]
  hotGoods: GoodsItem[]
  recommendGoods: GoodsItem[]
}

export interface MallConfig {
  mallName: string
  servicePhone: string
  freightAmount: number
  freightFreeThreshold: number
}

export type CouponType = 'FULL_REDUCTION' | 'DISCOUNT' | 'NO_THRESHOLD'
export type CouponScope = 'ALL' | 'CATEGORY' | 'GOODS'

/** 可领取的券（mkt_coupon 驼峰） */
export interface Coupon {
  id: string
  name: string
  type: CouponType
  threshold: number
  amount: number
  discount: number
  maxAmount: number
  scope: CouponScope
  total: number
  issued: number
  perLimit: number
  validType: 'FIXED' | 'DAYS'
  startAt?: string
  endAt?: string
  validDays?: number
  description?: string
  /** 已领数量（已登录时） */
  received?: number
}

export type MemberCouponStatus = 'UNUSED' | 'USED' | 'EXPIRED'

export interface MemberCoupon {
  id: string
  couponId: string
  name: string
  type: CouponType
  threshold: number
  amount: number
  discount: number
  maxAmount: number
  scope: CouponScope
  startAt: string
  endAt: string
  status: MemberCouponStatus
  description?: string
}

/** 结算页券选项 */
export interface CouponOption {
  memberCouponId: string
  name: string
  type: CouponType
  threshold: number
  amount: number
  discount: number
  endAt?: string
  usable: boolean
  discountAmount: number
  unusableReason?: string
}

/** 票券卡片通用展示字段 */
export interface CouponLike {
  name: string
  type: CouponType
  threshold: number
  amount: number
  discount: number
  maxAmount?: number
  startAt?: string
  endAt?: string
  validType?: 'FIXED' | 'DAYS'
  validDays?: number
  description?: string
}
