import { http } from '@/utils/request'
import { createCrud } from './crud'
import type { AccountLog, AdjustForm, ID, Member, MemberLevel, PageResult, Switch } from '@/types'

export const memberApi = {
  page: (params: object) => http.get<PageResult<Member>>('/admin/member/member/page', params),
  detail: (id: ID) => http.get<Member>(`/admin/member/member/${id}`),
  setStatus: (id: ID, status: Switch) => http.put<void>(`/admin/member/member/${id}/status`, undefined, { params: { status } }),
  adjust: (id: ID, data: AdjustForm) => http.post<void>(`/admin/member/member/${id}/adjust`, data),
  accountLogs: (params: object) => http.get<PageResult<AccountLog>>('/admin/member/account-log/page', params),
}

export const levelApi = {
  ...createCrud<MemberLevel>('member/level'),
  list: () => http.get<MemberLevel[]>('/admin/member/level/list'),
}
