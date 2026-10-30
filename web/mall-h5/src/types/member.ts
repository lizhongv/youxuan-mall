import type { GoodsItem } from './goods'

export interface OrderCount {
  PENDING_PAY: number
  PENDING_DELIVERY: number
  DELIVERED: number
  TO_REVIEW: number
  AFTERSALE: number
}

export interface Profile {
  id: string
  mobile: string
  nickname: string
  avatar?: string
  gender?: number
  birthday?: string
  levelName?: string
  discount?: number
  points: number
  growth: number
  balance: number
  couponCount: number
  favoriteCount: number
  orderCount: OrderCount
}

export interface ProfileForm {
  nickname: string
  avatar?: string
  gender?: number
  birthday?: string
}

export interface Address {
  id: string
  name: string
  mobile: string
  province: string
  city: string
  district: string
  detail: string
  areaCode?: string
  isDefault: number | boolean
}

export type AddressForm = Omit<Address, 'id'> & { id?: string }

/** 足迹（商品列表项 + 浏览时间） */
export interface HistoryItem extends GoodsItem {
  goodsId?: string
  browsedAt?: string
  createdAt?: string
  updatedAt?: string
}

export type AccountType = 'BALANCE' | 'POINTS'

export interface AccountLog {
  id: string
  accountType: AccountType
  changeValue: number
  afterValue: number
  bizType: string
  bizNo?: string
  remark?: string
  createdAt: string
}
