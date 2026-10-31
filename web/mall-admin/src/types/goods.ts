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
  /** 编辑页打开时详情返回的库存，保存时原样回传用于差量调整（新增 SKU 不传） */
  originStock?: number
  /** 单件重量 kg（3 位小数） */
  weight?: number
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
  /** 运费模板，空表示默认模板 */
  freightTemplateId?: ID | '' | null
  specItems: SpecItem[]
  skus: Sku[]
}
