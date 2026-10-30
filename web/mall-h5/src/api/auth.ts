import { http } from './request'
import type { SmsCodeResult, SmsScene, TokenResult } from '@/types'

/** 2.1 认证（公开） */
export const sendSmsCode = (mobile: string, scene: SmsScene) =>
  http.post<SmsCodeResult>('/app/auth/sms-code', { mobile, scene })

export const register = (data: { mobile: string; code: string; password: string; nickname?: string }) =>
  http.post<TokenResult>('/app/auth/register', data)

export const login = (data: { mobile: string; password: string }) => http.post<TokenResult>('/app/auth/login', data)

export const loginBySms = (data: { mobile: string; code: string }) =>
  http.post<TokenResult>('/app/auth/login-sms', data)

export const resetPassword = (data: { mobile: string; code: string; password: string }) =>
  http.post<void>('/app/auth/reset-password', data)
