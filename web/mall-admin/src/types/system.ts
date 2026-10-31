import type { ID, Switch } from './api'

export interface LoginForm {
  username: string
  password: string
  captchaId?: string
  captchaCode?: string
}

/** 图形验证码：image 为 data URI；devCode 仅 demo 模式返回 */
export interface Captcha {
  captchaId: string
  image: string
  devCode?: string
}

export interface TokenPair {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export type MenuType = 'DIR' | 'MENU' | 'BUTTON'

export interface MenuTree {
  id: ID
  parentId: ID
  type: MenuType
  name: string
  path: string
  component: string
  icon: string
  sort: number
  visible: Switch
  children?: MenuTree[]
}

export interface Menu extends MenuTree {
  permission: string
  status: Switch
  children?: Menu[]
}

export interface UserInfo {
  id: ID
  username: string
  nickname: string
  avatar: string
  mobile?: string
  email?: string
  roles: string[]
  permissions: string[]
  menus: MenuTree[]
  /** true 时必须先修改初始密码，其余接口返回 40301 */
  mustChangePassword?: boolean
}

export interface ProfileForm {
  nickname: string
  avatar: string
  mobile: string
  email: string
}

export interface PasswordForm {
  oldPassword: string
  newPassword: string
}

export interface Admin {
  id: ID
  username: string
  nickname: string
  avatar: string
  mobile: string
  email: string
  status: Switch
  roleIds: ID[]
  roleNames: string[]
  lastLoginAt: string
  remark: string
  createdAt: string
  password?: string
}

export interface Role {
  id: ID
  name: string
  code: string
  sort: number
  status: Switch
  remark: string
  createdAt: string
}

export interface Dict {
  id: ID
  dictType: string
  label: string
  value: string
  sort: number
  status: Switch
  remark: string
}

export interface SysConfig {
  id: ID
  configKey: string
  configValue: string
  name: string
  remark: string
  /** 敏感参数：已设置时 configValue 为 ******，未设置为空串；提交 ****** 表示不修改 */
  sensitive?: boolean
}

export interface OperLog {
  id: ID
  username: string
  module: string
  action: string
  method: string
  uri: string
  params: string
  ip: string
  success: Switch | boolean
  errorMsg: string
  costMs: number
  createdAt: string
}

export interface LoginLog {
  id: ID
  userType: 'ADMIN' | 'MEMBER'
  username: string
  ip: string
  userAgent: string
  success: Switch | boolean
  message: string
  createdAt: string
}

export interface UploadResult {
  id: ID
  url: string
  originalName: string
  size: number
}
