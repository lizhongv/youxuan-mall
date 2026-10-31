import { ref, shallowRef } from 'vue'
import { errorMessage } from '@/api/request'

/**
 * 页面数据加载状态：区分「加载中 / 加载失败 / 已加载」，避免请求失败被当成空数据。
 * 配合 <LoadState> 组件展示加载、错误重试与空状态。
 *
 * - run() 返回本次结果；失败时返回 undefined 并记录 error（错误 Toast 由请求层负责）
 * - 再次 run 时保留已有 data，只有首次加载显示整页 loading
 */
export function useAsync<T>(fn: () => Promise<T>, opts: { immediate?: boolean } = {}) {
  const data = shallowRef<T>()
  const loading = ref(false)
  const loaded = ref(false)
  const error = ref<string>('')
  let seq = 0

  async function run(): Promise<T | undefined> {
    const my = ++seq
    loading.value = true
    error.value = ''
    try {
      const res = await fn()
      if (my !== seq) return undefined
      data.value = res
      loaded.value = true
      return res
    } catch (e) {
      if (my === seq) error.value = errorMessage(e)
      return undefined
    } finally {
      if (my === seq) loading.value = false
    }
  }

  if (opts.immediate) run()

  return { data, loading, loaded, error, run }
}
