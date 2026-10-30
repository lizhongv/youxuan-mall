import { http, request } from './request'
import type {
  AccountLog,
  AccountType,
  Address,
  AddressForm,
  GoodsItem,
  HistoryItem,
  PageResult,
  Profile,
  ProfileForm,
  UploadResult,
} from '@/types'

/** 2.3 会员中心 */
export const getProfile = (silent = false) => http.get<Profile>('/app/member/profile', undefined, { silent })

export const updateProfile = (data: ProfileForm) => http.put<void>('/app/member/profile', data)

export const updatePassword = (data: { oldPassword: string; newPassword: string }) =>
  http.put<void>('/app/member/password', data)

export const getAddressList = () => http.get<Address[]>('/app/member/address')
export const getDefaultAddress = () => http.get<Address | null>('/app/member/address/default', undefined, { silent: true })
export const createAddress = (data: AddressForm) => http.post<string>('/app/member/address', data)
export const updateAddress = (id: string, data: AddressForm) => http.put<void>(`/app/member/address/${id}`, data)
export const deleteAddress = (id: string) => http.delete<void>(`/app/member/address/${id}`)
export const setDefaultAddress = (id: string) => http.put<void>(`/app/member/address/${id}/default`)

export const getFavoritePage = (params: { page: number; size: number }) =>
  http.get<PageResult<GoodsItem>>('/app/member/favorite/page', params)
export const addFavorite = (goodsId: string) => http.post<void>(`/app/member/favorite/${goodsId}`)
export const removeFavorite = (goodsId: string) => http.delete<void>(`/app/member/favorite/${goodsId}`)

export const getHistoryPage = (params: { page: number; size: number }) =>
  http.get<PageResult<HistoryItem>>('/app/member/history/page', params)
export const clearHistory = () => http.delete<void>('/app/member/history')

export const getAccountLogPage = (params: { page: number; size: number; accountType?: AccountType }) =>
  http.get<PageResult<AccountLog>>('/app/member/account-log/page', params)

/** 上传图片（头像 avatar、评价 review、售后凭证 aftersale） */
export const upload = (file: File, bizType: string) => {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('bizType', bizType)
  return request<UploadResult>({ url: '/app/member/upload', method: 'POST', data: fd, timeout: 60000 })
}
