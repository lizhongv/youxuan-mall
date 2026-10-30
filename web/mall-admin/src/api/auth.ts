import { http } from '@/utils/request'
import type { LoginForm, PasswordForm, ProfileForm, TokenPair, UserInfo } from '@/types'

export const authApi = {
  login: (data: LoginForm) => http.post<TokenPair>('/admin/auth/login', data),
  logout: () => http.post<void>('/admin/auth/logout', undefined, { silent: true }),
  me: () => http.get<UserInfo>('/admin/auth/me'),
  updateProfile: (data: ProfileForm) => http.put<void>('/admin/auth/profile', data),
  updatePassword: (data: PasswordForm) => http.put<void>('/admin/auth/password', data),
}
