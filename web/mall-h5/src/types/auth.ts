export type SmsScene = 'REGISTER' | 'LOGIN' | 'RESET'

export interface TokenResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface SmsCodeResult {
  devCode?: string | null
}
