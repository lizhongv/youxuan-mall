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
  TokenResult,
  CaptchaAnswer,
  SmsCodeResult,
  UploadResult,
} from '@/types'

/** 2.3 会员中心 */
export const getProfile = (silent = false) => http.get<Profile>('/app/member/profile', undefined, { silent })

export const updateProfile = (data: ProfileForm) => http.put<void>('/app/member/profile', data)

/** 修改登录密码：其他设备登录失效，返回新令牌替换本地令牌；未设置过密码时不传 oldPassword */
export const updatePassword = (data: { oldPassword?: string; newPassword: string }) =>
  http.put<TokenResult | null>('/app/member/password', data)

/** 设置支付密码的短信验证码：发到当前登录会员自己的手机，需图形验证码 */
export const sendPayPasswordSms = (captcha: CaptchaAnswer) =>
  http.post<SmsCodeResult>('/app/member/pay-password/sms-code', captcha)

/** 设置 / 修改余额支付密码（6 位数字，首次与修改同一接口，不需要旧密码）；弱密码在消耗验证码前被拒绝 */
export const setPayPassword = (data: { code: string; payPassword: string }) =>
  http.put<void>('/app/member/pay-password', data)

/** 退出登录：作废 access 与提交的 refresh 令牌 */
export const logout = (refreshToken?: string | null) =>
  http.post<void>('/app/member/logout', { refreshToken: refreshToken || undefined }, { silent: true })

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
