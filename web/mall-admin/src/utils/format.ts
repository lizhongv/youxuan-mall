import dayjs from 'dayjs'

/** 金额：保留两位小数并加千分位 */
export function money(v: number | string | null | undefined, prefix = '¥'): string {
  const n = Number(v ?? 0)
  if (Number.isNaN(n)) return `${prefix}0.00`
  return prefix + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 大数字缩写：12345 → 1.23万 */
export function compactNumber(v: number | null | undefined): string {
  const n = Number(v ?? 0)
  if (Math.abs(n) >= 1e8) return `${(n / 1e8).toFixed(2)}亿`
  if (Math.abs(n) >= 1e4) return `${(n / 1e4).toFixed(2)}万`
  return n.toLocaleString('zh-CN')
}

export function formatTime(v?: string | null, fmt = 'YYYY-MM-DD HH:mm:ss'): string {
  if (!v) return '-'
  const d = dayjs(v)
  return d.isValid() ? d.format(fmt) : v
}

/** 后端时间格式 yyyy-MM-dd HH:mm:ss */
export const DATE_TIME_FMT = 'YYYY-MM-DD HH:mm:ss'

/** 收货地址拼接 */
export function fullAddress(o: { receiverProvince?: string; receiverCity?: string; receiverDistrict?: string; receiverDetail?: string }) {
  return [o.receiverProvince, o.receiverCity, o.receiverDistrict, o.receiverDetail].filter(Boolean).join(' ')
}

/** 环比百分比；昨日为 0 时返回 null（无法比较） */
export function percentChange(today: number, yesterday: number): number | null {
  if (!yesterday) return today ? null : 0
  return ((today - yesterday) / yesterday) * 100
}

/** 读取 CSS 自定义属性（图表等需要运行时颜色的场景） */
export function cssVar(name: string, el: Element = document.documentElement): string {
  return getComputedStyle(el).getPropertyValue(name).trim()
}

/** 去掉对象中的空字符串 / null / undefined，用于查询参数 */
export function cleanParams<T extends object>(obj: T): Partial<T> {
  const out: Record<string, unknown> = {}
  Object.entries(obj).forEach(([k, v]) => {
    if (v === '' || v === null || v === undefined) return
    if (Array.isArray(v) && v.length === 0) return
    out[k] = v
  })
  return out as Partial<T>
}

/** 深拷贝（仅 JSON 安全数据） */
export function clone<T>(v: T): T {
  return JSON.parse(JSON.stringify(v)) as T
}

/** 扁平化树 */
export function flattenTree<T extends { children?: T[] }>(list: T[] = []): T[] {
  const out: T[] = []
  const walk = (arr: T[]) =>
    arr.forEach((n) => {
      out.push(n)
      if (n.children?.length) walk(n.children)
    })
  walk(list)
  return out
}
