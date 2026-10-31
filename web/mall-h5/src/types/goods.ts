/** 类目（最多两级） */
export interface Category {
  id: string
  parentId?: string
  name: string
  icon?: string
  sort?: number
  children?: Category[]
}

/** 商品列表项 */
export interface GoodsItem {
  id: string
  name: string
  subtitle?: string
  pic: string
  price: number
  originalPrice?: number
  sales: number
  isNew?: number
  isHot?: number
}

export interface SpecItem {
  name: string
  values: string[]
}

export interface Sku {
  id: string
  specValues: string[]
  code?: string
  pic?: string
  price: number
  originalPrice?: number
  stock: number
}

export type SeckillState = 'NOT_STARTED' | 'ONGOING' | 'ENDED'

export interface Seckill {
  id: string
  title: string
  goodsId: string
  goodsName: string
  goodsPic: string
  skuId: string
  specText?: string
  seckillPrice: number
  /** SKU 当前售价（划线对比用） */
  price?: number
  /** 兼容字段：旧版本可能返回 originalPrice */
  originalPrice?: number
  stock: number
  sold: number
  limitPerMember: number
  startAt: string
  endAt: string
  status?: number
  state: SeckillState
}

export interface Review {
  id: string
  orderId?: string
  goodsId: string
  goodsName?: string
  goodsPic?: string
  memberId?: string
  nickname: string
  avatar?: string
  specText?: string
  rating: number
  content: string
  images: string[]
  anonymous: number | boolean
  reply?: string
  replyTime?: string
  createdAt: string
}

export type ReviewType = 'ALL' | 'GOOD' | 'MEDIUM' | 'BAD' | 'IMAGE'

export interface ReviewStat {
  total: number
  good: number
  medium: number
  bad: number
  image: number
  goodRate: number
}

export interface GoodsDetail {
  id: string
  categoryId?: string
  brandId?: string
  brandName?: string
  name: string
  subtitle?: string
  pic: string
  album: string[]
  detail: string
  unit?: string
  keywords?: string
  price: number
  originalPrice?: number
  stock?: number
  sales?: number
  virtualSales?: number
  status?: string
  specItems: SpecItem[]
  skus: Sku[]
  reviewCount: number
  goodRate: number
  latestReviews: Review[]
  favorited?: boolean
  seckill?: Seckill | null
}

export interface GoodsQuery {
  page?: number
  size?: number
  keyword?: string
  categoryId?: string
  brandId?: string
  sort?: 'DEFAULT' | 'SALES' | 'PRICE_ASC' | 'PRICE_DESC' | 'NEW'
}

/** SKU 弹窗动作 */
export type SkuAction = 'cart' | 'buy'
