import { http } from '@/utils/request'
import { createCrud } from './crud'
import type { Banner, Coupon, CouponRecord, ID, Notice, PageResult, Seckill, Switch } from '@/types'

export const bannerApi = createCrud<Banner>('marketing/banner')
export const noticeApi = createCrud<Notice>('marketing/notice')
export const seckillApi = createCrud<Seckill>('marketing/seckill')

export const couponApi = {
  ...createCrud<Coupon>('marketing/coupon'),
  setStatus: (id: ID, status: Switch) => http.put<void>(`/admin/marketing/coupon/${id}/status`, undefined, { params: { status } }),
  records: (id: ID, params: object) => http.get<PageResult<CouponRecord>>(`/admin/marketing/coupon/${id}/records`, params),
}
