import type { ID, Switch } from './api'

export interface Category {
  id: ID
  parentId: ID
  name: string
  icon: string
  sort: number
  status: Switch
  children?: Category[]
}

export interface Brand {
  id: ID
  name: string
  logo: string
  firstLetter: string
  description: string
  sort: number
  status: Switch
}

export type GoodsStatus = 'ON_SALE' | 'OFF_SALE'

export interface GoodsItem {
  id: ID
  name: string
  pic: string
  categoryId: ID
  categoryName: string
  brandName: string
  price: number
  originalPrice: number
  stock: number
  sales: number
  status: GoodsStatus
  isNew: Switch
  isHot: Switch
  isRecommend: Switch
  sort: number
  createdAt: string
}

export interface SpecItem {
  name: string
  values: string[]
}

export interface Sku {
  id?: ID | null
  specValues: string[]
  code: string
  pic: string
  price: number
  originalPrice: number
  stock: number
}

export interface GoodsForm {
  id?: ID
  categoryId: ID | ''
  brandId: ID | ''
  name: string
  subtitle: string
  pic: string
  album: string[]
  detail: string
  unit: string
  keywords: string
  virtualSales: number
  sort: number
  status: GoodsStatus
  isNew: Switch
  isHot: Switch
  isRecommend: Switch
  specItems: SpecItem[]
  skus: Sku[]
}
