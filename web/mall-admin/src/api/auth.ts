import { http } from '@/utils/request'
import type { Captcha, LoginForm, PasswordForm, ProfileForm, TokenPair, UserInfo } from '@/types'

export const authApi = {
  captcha: () => http.get<Captcha>('/admin/auth/captcha'),
  login: (data: LoginForm) => http.post<TokenPair>('/admin/auth/login', data),
  logout: (refreshToken?: string) => http.post<void>('/admin/auth/logout', refreshToken ? { refreshToken } : {}, { silent: true }),
  me: () => http.get<UserInfo>('/admin/auth/me'),
  updateProfile: (data: ProfileForm) => http.put<void>('/admin/auth/profile', data),
  /** 改密成功返回新令牌：该账号所有旧令牌已失效，必须替换本地令牌 */
  updatePassword: (data: PasswordForm) => http.put<TokenPair | null>('/admin/auth/password', data),
}
