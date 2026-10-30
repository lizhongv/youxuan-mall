import { http } from './request'
import type { PageResult, Review, ReviewForm } from '@/types'

/** 2.7 评价 */
export const createReview = (data: ReviewForm) => http.post<string>('/app/review', data)

export const getMyReviews = (params: { page: number; size: number }) =>
  http.get<PageResult<Review>>('/app/review/mine', params)
