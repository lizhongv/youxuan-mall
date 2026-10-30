import type { ID, Switch } from './api'

export type LinkType = 'NONE' | 'GOODS' | 'CATEGORY' | 'URL'

export interface Banner {
  id: ID
  title: string
  image: string
  linkType: LinkType
  linkValue: string
  position: string
  sort: number
  status: Switch
}

export interface Notice {
  id: ID
  title: string
  content: string
  sort: number
  status: Switch
  createdAt: string
}

export type CouponType = 'FULL_REDUCTION' | 'DISCOUNT' | 'NO_THRESHOLD'
export type CouponScope = 'ALL' | 'CATEGORY' | 'GOODS'
export type CouponValidType = 'FIXED' | 'DAYS'

/** 字段同 mkt_coupon（驼峰） */
export interface Coupon {
  id: ID
  name: string
  type: CouponType
  threshold: number
  amount: number
  discount: number
  maxAmount: number
  scope: CouponScope
  scopeIds: ID[]
  total: number
  issued: number
  used: number
  perLimit: number
  validType: CouponValidType
  startAt: string
  endAt: string
  validDays: number
  status: Switch
  description: string
  createdAt?: string
}

export interface CouponRecord {
  id: ID
  memberId: ID
  memberMobile: string
  status: 'UNUSED' | 'USED' | 'EXPIRED'
  startAt: string
  endAt: string
  usedAt: string
  orderId: ID
  createdAt: string
}

export type SeckillState = 'NOT_STARTED' | 'ONGOING' | 'ENDED'

export interface Seckill {
  id: ID
  title: string
  goodsId: ID
  goodsName: string
  goodsPic: string
  skuId: ID
  specText: string
  seckillPrice: number
  stock: number
  sold: number
  limitPerMember: number
  startAt: string
  endAt: string
  status: Switch
  state: SeckillState
}
