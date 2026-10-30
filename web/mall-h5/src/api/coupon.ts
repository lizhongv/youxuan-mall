import { http } from './request'
import type { MemberCoupon, MemberCouponStatus } from '@/types'

/** 2.4 优惠券 */
export const receiveCoupon = (couponId: string) => http.post<void>(`/app/coupon/${couponId}/receive`)

export const getMyCoupons = (status: MemberCouponStatus) => http.get<MemberCoupon[]>('/app/coupon/mine', { status })
