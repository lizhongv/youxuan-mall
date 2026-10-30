import dayjs from 'dayjs'
import { showToast } from 'vant'

/** 生成幂等键（优先使用原生 randomUUID，非安全上下文下降级） */
export function uuid(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    try {
      return crypto.randomUUID()
    } catch {
      /* 非 https 环境可能抛错，降级 */
    }
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16)
  })
}

/** 金额两位小数 */
export function money(v: number | string | null | undefined): string {
  const n = Number(v ?? 0)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

/** 时间格式化 */
export function fmtTime(v?: string | null, pattern = 'YYYY-MM-DD HH:mm'): string {
  if (!v) return ''
  const d = dayjs(v)
  return d.isValid() ? d.format(pattern) : v
}

/** 解析契约时间字符串 yyyy-MM-dd HH:mm:ss 为毫秒（兼容 iOS 不认横杠） */
export function parseTime(v?: string | null): number {
  if (!v) return 0
  const d = dayjs(v.replace(/-/g, '/'))
  return d.isValid() ? d.valueOf() : dayjs(v).valueOf()
}

/** 销量展示：1.2万+ */
export function fmtSales(n?: number): string {
  const v = Number(n || 0)
  if (v >= 10000) return (v / 10000).toFixed(1).replace(/\.0$/, '') + '万+'
  return String(v)
}

/** 复制文本 */
export async function copyText(text: string) {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
    } else {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
    }
    showToast('已复制')
  } catch {
    showToast('复制失败，请长按手动复制')
  }
}

/**
 * 富文本基本清洗：移除 script/style/iframe 等危险标签、on* 事件属性、javascript: 协议。
 * 基于 DOMParser，不执行脚本。
 */
export function sanitizeHtml(html?: string): string {
  if (!html) return ''
  const doc = new DOMParser().parseFromString(`<div>${html}</div>`, 'text/html')
  const root = doc.body.firstElementChild as HTMLElement | null
  if (!root) return ''
  root.querySelectorAll('script,style,iframe,object,embed,link,meta,form,frame,frameset,base').forEach((el) => el.remove())
  root.querySelectorAll('*').forEach((el) => {
    Array.from(el.attributes).forEach((attr) => {
      const name = attr.name.toLowerCase()
      const val = attr.value.trim().toLowerCase()
      if (name.startsWith('on')) el.removeAttribute(attr.name)
      else if ((name === 'href' || name === 'src' || name === 'xlink:href') && /^(javascript|vbscript|data:text)/.test(val))
        el.removeAttribute(attr.name)
    })
    if (el.tagName === 'IMG') {
      el.removeAttribute('width')
      el.removeAttribute('height')
      el.setAttribute('loading', 'lazy')
    }
  })
  return root.innerHTML
}

/** 手机号校验 */
export const MOBILE_RE = /^1\d{10}$/

export function isMobile(v: string) {
  return MOBILE_RE.test(v)
}

/** 星级 → 好/中/差 */
export function ratingText(r: number) {
  return r >= 4 ? '好评' : r === 3 ? '中评' : '差评'
}

/** 按日期分组（足迹） */
export function groupByDate<T>(list: T[], getTime: (t: T) => string | undefined) {
  const today = dayjs().format('YYYY-MM-DD')
  const yesterday = dayjs().subtract(1, 'day').format('YYYY-MM-DD')
  const groups: { label: string; items: T[] }[] = []
  const map = new Map<string, { label: string; items: T[] }>()
  list.forEach((it) => {
    const raw = getTime(it)
    const day = raw ? dayjs(raw.replace(/-/g, '/')).format('YYYY-MM-DD') : '更早'
    let label = day
    if (day === today) label = '今天'
    else if (day === yesterday) label = '昨天'
    else if (day !== '更早') label = dayjs(day).format('M月D日')
    let g = map.get(day)
    if (!g) {
      g = { label, items: [] }
      map.set(day, g)
      groups.push(g)
    }
    g.items.push(it)
  })
  return groups
}

/** 好评率：契约未说明是 0-1 还是 0-100，两种都兼容 */
export function ratePercent(v?: number | null): number {
  const n = Number(v || 0)
  return Math.round(n <= 1 ? n * 100 : n)
}
