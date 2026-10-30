import { ref, shallowRef } from 'vue'
import type { PageResult } from '@/types'

/**
 * 分页列表：配合 van-list（上拉加载）与 van-pull-refresh（下拉刷新）。
 * fetcher 接收页码与每页数量，返回契约分页结构。
 */
export function usePageList<T>(fetcher: (page: number, size: number) => Promise<PageResult<T>>, size = 10) {
  const list = shallowRef<T[]>([])
  const loading = ref(false)
  const finished = ref(false)
  const error = ref(false)
  const refreshing = ref(false)
  const total = ref(0)
  const firstLoaded = ref(false)
  let page = 0
  // 请求序号：切换筛选后丢弃过期响应
  let seq = 0

  async function load() {
    const my = ++seq
    loading.value = true
    try {
      const res = await fetcher(page + 1, size)
      if (my !== seq) return
      const records = res?.records || []
      page += 1
      list.value = page === 1 ? records : [...list.value, ...records]
      total.value = Number(res?.total || 0)
      const pages = Number(res?.pages || 0)
      finished.value = records.length < size || (pages > 0 && page >= pages) || list.value.length >= total.value
      error.value = false
    } catch {
      if (my !== seq) return
      error.value = true
    } finally {
      if (my === seq) {
        loading.value = false
        refreshing.value = false
        firstLoaded.value = true
      }
    }
  }

  /** 重置并重新加载第一页 */
  function reset() {
    seq++
    page = 0
    list.value = []
    finished.value = false
    error.value = false
    firstLoaded.value = false
    loading.value = true
    return load()
  }

  /** 下拉刷新：保留当前列表直到新数据返回 */
  function refresh() {
    seq++
    page = 0
    finished.value = false
    error.value = false
    return load()
  }

  return { list, loading, finished, error, refreshing, total, firstLoaded, load, reset, refresh }
}
