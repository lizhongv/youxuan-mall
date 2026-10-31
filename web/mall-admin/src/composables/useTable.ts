import { onMounted, reactive, ref, shallowRef, type UnwrapRef } from 'vue'
import type { PageResult } from '@/types'
import { cleanParams, clone } from '@/utils/format'

interface Options<T> {
  /** 是否在挂载时自动加载，默认 true */
  immediate?: boolean
  pageSize?: number
  /** 查询前对参数做转换（如日期范围拆分） */
  beforeFetch?: (params: Record<string, unknown>) => Record<string, unknown>
  afterFetch?: (records: T[]) => T[]
}

/**
 * 列表页通用逻辑：分页、搜索、重置、loading、多选。
 * query 只放筛选字段；page / size 单独维护，重置时回到第一页。
 */
export function useTable<T, Q extends object = Record<string, unknown>>(
  fetcher: (params: Record<string, unknown>) => Promise<PageResult<T>>,
  initialQuery: Q = {} as Q,
  options: Options<T> = {},
) {
  const { immediate = true, pageSize = 20 } = options
  const query = reactive(clone(initialQuery)) as UnwrapRef<Q>
  const list = shallowRef<T[]>([])
  const total = ref(0)
  const page = ref(1)
  const size = ref(pageSize)
  // 自动加载时初始即为 loading，避免首帧闪现空状态
  const loading = ref(immediate)
  const selection = shallowRef<T[]>([])
  let seq = 0

  async function load() {
    const current = ++seq
    loading.value = true
    try {
      let params: Record<string, unknown> = { ...cleanParams(query as object), page: page.value, size: size.value }
      if (options.beforeFetch) params = cleanParams(options.beforeFetch(params))
      const res = await fetcher(params)
      // 忽略过期响应：快速切换筛选时只保留最后一次请求的结果
      if (current !== seq) return
      const records = res?.records || []
      list.value = options.afterFetch ? options.afterFetch(records) : records
      total.value = Number(res?.total || 0)
      // 删除最后一页最后一条后回退一页
      if (list.value.length === 0 && page.value > 1 && total.value > 0) {
        page.value = Math.max(1, Math.ceil(total.value / size.value))
        await load()
      }
    } catch {
      if (current === seq) list.value = []
    } finally {
      if (current === seq) loading.value = false
    }
  }

  function search() {
    page.value = 1
    return load()
  }

  function reset() {
    const init = clone(initialQuery) as Record<string, unknown>
    const q = query as Record<string, unknown>
    Object.keys(q).forEach((k) => {
      q[k] = k in init ? init[k] : undefined
    })
    return search()
  }

  function onSizeChange(s: number) {
    size.value = s
    page.value = 1
    load()
  }

  function onPageChange(p: number) {
    page.value = p
    load()
  }

  function onSelectionChange(rows: T[]) {
    selection.value = rows
  }

  if (immediate) onMounted(load)

  return { query, list, total, page, size, loading, selection, load, search, reset, onSizeChange, onPageChange, onSelectionChange }
}
