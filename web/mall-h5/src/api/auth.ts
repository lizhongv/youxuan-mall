import { http } from './request'
import type {
  Captcha,
  CaptchaAnswer,
  SmsCodeResult,
  SmsScene,
  TokenResult,
  WechatAuthorizeUrl,
  WechatLoginResult,
} from '@/types'

/** 2.1 认证（公开） */

/** 图形验证码（一次性；发送短信、登录失败多次后需要） */
export const getCaptcha = () => http.get<Captcha>('/app/auth/captcha', undefined, { silent: true })

/** 发送短信验证码：必须带图形验证码；对已注册 / 未注册手机号一律返回成功 */
export const sendSmsCode = (mobile: string, scene: SmsScene, captcha: CaptchaAnswer) =>
  http.post<SmsCodeResult>('/app/auth/sms-code', { mobile, scene, ...captcha })

export const register = (data: { mobile: string; code: string; password: string; nickname?: string }) =>
  http.post<TokenResult>('/app/auth/register', data)

/** 密码登录：失败多次后需要图形验证码（40002 / needCaptcha），由页面自行提示，故静默 */
export const login = (data: { mobile: string; password: string } & Partial<CaptchaAnswer>) =>
  http.post<TokenResult>('/app/auth/login', data, { silent: true })

export const loginBySms = (data: { mobile: string; code: string }) =>
  http.post<TokenResult>('/app/auth/login-sms', data)

export const resetPassword = (data: { mobile: string; code: string; password: string }) =>
  http.post<void>('/app/auth/reset-password', data)

/** 微信公众号网页授权（仅微信内置浏览器） */
export const getWechatAuthorizeUrl = (redirect: string) =>
  http.get<WechatAuthorizeUrl>('/app/auth/wechat/authorize-url', { redirect })

export const wechatLogin = (data: { code: string; state: string }) =>
  http.post<WechatLoginResult>('/app/auth/wechat/login', data)

export const wechatBind = (data: { bindToken: string; mobile: string; code: string }) =>
  http.post<TokenResult>('/app/auth/wechat/bind', data)
