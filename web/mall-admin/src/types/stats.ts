import type { ID } from './api'

export interface Overview {
  todaySales: number
  todayOrders: number
  todayMembers: number
  yesterdaySales: number
  yesterdayOrders: number
  totalMembers: number
  onSaleGoods: number
  pendingDelivery: number
  pendingAftersale: number
  pendingPay: number
}

export interface TrendPoint {
  date: string
  sales: number
  orders: number
  members: number
}

export interface TopGoods {
  goodsId: ID
  goodsName: string
  pic: string
  quantity: number
  amount: number
}

export interface OrderStatusStat {
  status: string
  statusText: string
  count: number
}
