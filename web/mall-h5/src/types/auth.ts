export type SmsScene = 'REGISTER' | 'LOGIN' | 'RESET' | 'PAY_PASSWORD'

export interface TokenResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface SmsCodeResult {
  devCode?: string | null
}

/** 图形验证码：image 为 data URI；devCode 仅 demo 模式返回 */
export interface Captcha {
  captchaId: string
  image: string
  devCode?: string | null
}

/** 提交时携带的图形验证码 */
export interface CaptchaAnswer {
  captchaId: string
  captchaCode: string
}

/** 微信授权地址 */
export interface WechatAuthorizeUrl {
  url: string
  state: string
}

/** 微信登录：已绑定直接返回令牌；未绑定返回 bindToken */
export interface WechatLoginResult extends Partial<TokenResult> {
  needBind: boolean
  bindToken?: string
}
