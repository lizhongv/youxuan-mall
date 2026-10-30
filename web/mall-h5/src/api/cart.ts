import { http } from './request'
import type { CartItem } from '@/types'

/** 2.5 购物车 */
export const getCart = () => http.get<CartItem[]>('/app/cart')

export const addCart = (skuId: string, quantity: number) => http.post<void>('/app/cart', { skuId, quantity })

export const updateCartQuantity = (id: string, quantity: number) => http.put<void>(`/app/cart/${id}`, { quantity })

/** ids 为空表示全部 */
export const checkCart = (ids: string[], checked: boolean) => http.put<void>('/app/cart/check', { ids, checked })

export const deleteCart = (ids: string[]) => http.delete<void>('/app/cart', { ids })

export const getCartCount = () => http.get<number>('/app/cart/count', undefined, { silent: true })
