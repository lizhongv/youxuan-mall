import { http } from './request'
import type { Aftersale, AftersaleForm, PageResult } from '@/types'

/** 2.7 售后 */
export const applyAftersale = (data: AftersaleForm) => http.post<string>('/app/aftersale', data)

export const getAftersalePage = (params: { page: number; size: number }) =>
  http.get<PageResult<Aftersale>>('/app/aftersale/page', params)

export const getAftersale = (id: string) => http.get<Aftersale>(`/app/aftersale/${id}`)

export const cancelAftersale = (id: string) => http.put<void>(`/app/aftersale/${id}/cancel`)

export const fillReturn = (id: string, data: { returnCompany: string; returnNo: string }) =>
  http.put<void>(`/app/aftersale/${id}/return`, data)
