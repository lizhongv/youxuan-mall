import { ref } from 'vue'
import { http } from '@/utils/request'
import type { EnumOption } from '@/utils/constants'

/**
 * 字典缓存：同一类型只请求一次。
 * 使用只需登录的 `/admin/system/dict/data/{type}`（运营角色没有字典管理权限也要能选快递公司）；
 * 请求失败时回退到内置种子数据，保证发货等操作可用。
 */
interface DictItem {
  label: string
  value: string
}

const cache = new Map<string, Promise<EnumOption[]>>()

/** 回退数据：[label, value]，value 省略时与 label 相同；快递公司 value 为快递100 编码 */
const FALLBACK: Record<string, (string | [string, string])[]> = {
  express_company: [
    ['顺丰速运', 'shunfeng'],
    ['中通快递', 'zhongtong'],
    ['圆通速递', 'yuantong'],
    ['韵达快递', 'yunda'],
    ['申通快递', 'shentong'],
    ['京东物流', 'jd'],
    ['EMS', 'ems'],
    ['极兔速递', 'jtexpress'],
  ],
  aftersale_reason: ['不想要了', '商品与描述不符', '质量问题', '少件/漏发', '发错货', '其他'],
  cancel_reason: ['不想买了', '信息填写错误', '重复下单', '其他'],
}

const fallback = (type: string): EnumOption[] =>
  (FALLBACK[type] || []).map((v) => (Array.isArray(v) ? { label: v[0], value: v[1] } : { label: v, value: v }))

function load(type: string): Promise<EnumOption[]> {
  if (!cache.has(type)) {
    // silent：失败时不弹提示，直接走回退数据
    const p = http
      .get<DictItem[]>(`/admin/system/dict/data/${encodeURIComponent(type)}`, undefined, { silent: true })
      .then((items) => (items || []).map((d) => ({ value: d.value, label: d.label })))
      .then((list) => (list.length ? list : fallback(type)))
      .catch(() => {
        cache.delete(type)
        return fallback(type)
      })
    cache.set(type, p)
  }
  return cache.get(type)!
}

export function useDict(type: string) {
  const options = ref<EnumOption[]>([])
  const loading = ref(true)
  load(type).then((list) => {
    options.value = list
    loading.value = false
  })
  return { options, loading }
}

export function clearDictCache(type?: string) {
  if (type) cache.delete(type)
  else cache.clear()
}
