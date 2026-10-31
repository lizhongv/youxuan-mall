import { http } from '@/utils/request'
import { createCrud } from './crud'
import type { Admin, Dict, ID, LoginLog, Menu, OperLog, PageResult, Role, Switch, SysConfig, UploadResult } from '@/types'

export const adminApi = {
  ...createCrud<Admin>('system/admin'),
  setStatus: (id: ID, status: Switch) => http.put<void>(`/admin/system/admin/${id}/status`, undefined, { params: { status } }),
  resetPassword: (id: ID, password: string) => http.put<void>(`/admin/system/admin/${id}/password`, { password }),
}

export const roleApi = {
  ...createCrud<Role>('system/role'),
  list: () => http.get<Role[]>('/admin/system/role/list'),
  menus: (id: ID) => http.get<ID[]>(`/admin/system/role/${id}/menus`),
  saveMenus: (id: ID, menuIds: ID[]) => http.put<void>(`/admin/system/role/${id}/menus`, { menuIds }),
}

export const menuApi = {
  tree: () => http.get<Menu[]>('/admin/system/menu/tree'),
  create: (data: Partial<Menu>) => http.post<ID>('/admin/system/menu', data),
  update: (id: ID, data: Partial<Menu>) => http.put<void>(`/admin/system/menu/${id}`, data),
  remove: (id: ID) => http.delete<void>(`/admin/system/menu/${id}`),
}

export const dictApi = {
  ...createCrud<Dict>('system/dict'),
  types: () => http.get<string[]>('/admin/system/dict/types'),
}

export const configApi = {
  list: () => http.get<SysConfig[]>('/admin/system/config/list'),
  batch: (items: { configKey: string; configValue: string }[]) => http.put<void>('/admin/system/config/batch', { items }),
}

export const infraApi = {
  smsTest: (mobile: string) => http.post<{ provider: string }>('/admin/system/sms/test', { mobile }, { timeout: 30000 }),
  storageTest: () =>
    http.post<{ provider: string; sampleUrl?: string; elapsedMs?: number; message?: string }>('/admin/system/storage/test', undefined, {
      timeout: 30000,
    }),
}

export const logApi = {
  operPage: (params: object) => http.get<PageResult<OperLog>>('/admin/system/oper-log/page', params),
  loginPage: (params: object) => http.get<PageResult<LoginLog>>('/admin/system/login-log/page', params),
}

export const fileApi = {
  upload(file: File, bizType = 'common') {
    const form = new FormData()
    form.append('file', file)
    form.append('bizType', bizType)
    return http.upload<UploadResult>('/admin/system/file/upload', form)
  },
}
