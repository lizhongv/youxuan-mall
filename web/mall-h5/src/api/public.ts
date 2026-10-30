import { http } from './request'
import type {
  Category,
  Coupon,
  GoodsDetail,
  GoodsItem,
  GoodsQuery,
  HomeData,
  MallConfig,
  Notice,
  PageResult,
  Review,
  ReviewStat,
  ReviewType,
  Seckill,
} from '@/types'

/** 2.2 公开浏览 */
export const getHome = () => http.get<HomeData>('/app/public/home')

export const getCategoryTree = () => http.get<Category[]>('/app/public/category/tree')

export const getGoodsPage = (params: GoodsQuery) => http.get<PageResult<GoodsItem>>('/app/public/goods/page', params)

export const getGoodsDetail = (id: string) => http.get<GoodsDetail>(`/app/public/goods/${id}`)

export const getGoodsReviews = (id: string, params: { page: number; size: number; type: ReviewType }) =>
  http.get<PageResult<Review>>(`/app/public/goods/${id}/reviews`, params)

export const getReviewStat = (id: string) => http.get<ReviewStat>(`/app/public/goods/${id}/review-stat`)

export const getAvailableCoupons = () => http.get<Coupon[]>('/app/public/coupon/available')

export const getSeckillList = () => http.get<Seckill[]>('/app/public/seckill/list')

export const getConfig = () => http.get<MallConfig>('/app/public/config', undefined, { silent: true })

export const getNotice = (id: string) => http.get<Notice>(`/app/public/notice/${id}`)
