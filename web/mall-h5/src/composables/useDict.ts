import { ref } from 'vue'
import type { DictItem } from '@/types'
import { getDict } from '@/api/public'

const cache = new Map<string, Promise<DictItem[]>>()

/**
 * 公开字典（GET /app/public/dict/{type}），带内置回退：接口失败或为空时使用 fallback。
 * 同一类型只请求一次。
 */
export function useDict(type: string, fallback: (string | DictItem)[]) {
  const norm = (list: (string | DictItem)[]) =>
    list.map((i) => (typeof i === 'string' ? { label: i, value: i } : i))
  const items = ref<DictItem[]>(norm(fallback))
  let p = cache.get(type)
  if (!p) {
    p = getDict(type).then((r) => (Array.isArray(r) ? r : []))
    p.catch(() => cache.delete(type))
    cache.set(type, p)
  }
  p.then((list) => {
    if (list.length) items.value = list
  }).catch(() => {
    /* 使用回退 */
  })
  return items
}
