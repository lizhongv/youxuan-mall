import { http } from '@/utils/request'
import type { OrderStatusStat, Overview, TopGoods, TrendPoint } from '@/types'

export const statsApi = {
  overview: () => http.get<Overview>('/admin/stats/overview'),
  trend: (days: 7 | 30) => http.get<TrendPoint[]>('/admin/stats/trend', { days }),
  topGoods: (days = 30, limit = 10) => http.get<TopGoods[]>('/admin/stats/top-goods', { days, limit }),
  orderStatus: () => http.get<OrderStatusStat[]>('/admin/stats/order-status'),
}
