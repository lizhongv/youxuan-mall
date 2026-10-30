import type { ID, Switch } from './api'

export interface Address {
  id: ID
  name?: string
  receiverName?: string
  mobile?: string
  receiverMobile?: string
  province: string
  city: string
  district: string
  detail: string
  isDefault?: Switch
}

export interface Member {
  id: ID
  mobile: string
  nickname: string
  avatar: string
  gender: 0 | 1 | 2
  birthday: string
  levelId: ID
  levelName: string
  points: number
  growth: number
  balance: number
  status: Switch
  lastLoginAt: string
  createdAt: string
  addresses?: Address[]
}

export type AccountType = 'BALANCE' | 'POINTS' | 'GROWTH'

export interface AccountLog {
  id: ID
  memberId: ID
  accountType: AccountType
  changeValue: number
  afterValue: number
  bizType: string
  bizNo: string
  remark: string
  createdAt: string
}

export interface AdjustForm {
  accountType: 'BALANCE' | 'POINTS'
  amount: number
  remark: string
}

export interface MemberLevel {
  id: ID
  name: string
  minGrowth: number
  discount: number
  icon: string
  sort: number
}
