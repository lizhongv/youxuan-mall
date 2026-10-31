/**
 * 分片（懒加载路由 / CSS）加载失败处理：发版后旧页面引用的分片已被删除、或弱网加载失败时，
 * 先自动整页刷新一次（sessionStorage 标记防止循环），刷新后仍失败则显示「页面加载失败，点击重试」。
 */
const KEY = 'mall_h5_chunk_reload'
const CHUNK_RE =
  /Failed to fetch dynamically imported module|error loading dynamically imported module|Importing a module script failed|Unable to preload CSS|Loading (CSS )?chunk [\w-]+ failed/i

export function isChunkError(err: unknown): boolean {
  const msg = err instanceof Error ? err.message : String(err ?? '')
  return CHUNK_RE.test(msg)
}

function showFailure(target?: string) {
  if (document.getElementById('chunk-error')) return
  const el = document.createElement('div')
  el.id = 'chunk-error'
  el.className = 'chunk-error'
  el.innerHTML =
    '<div class="chunk-error__icon">!</div>' +
    '<div class="chunk-error__title">页面加载失败</div>' +
    '<div class="chunk-error__desc">网络不稳定或页面已更新</div>' +
    '<button type="button" class="chunk-error__btn">点击重试</button>'
  el.querySelector('button')!.addEventListener('click', () => {
    try {
      sessionStorage.removeItem(KEY)
    } catch {
      /* ignore */
    }
    if (target) window.location.assign(target)
    else window.location.reload()
  })
  document.body.appendChild(el)
}

/** 处理一次分片加载失败；target 为要进入的页面（路由跳转失败时） */
export function handleChunkError(target?: string) {
  let reloaded = false
  try {
    reloaded = !!sessionStorage.getItem(KEY)
    if (!reloaded) sessionStorage.setItem(KEY, String(Date.now()))
  } catch {
    /* 隐私模式下 sessionStorage 不可用：直接显示重试，避免无限刷新 */
    reloaded = true
  }
  if (reloaded) {
    showFailure(target)
    return
  }
  if (target) window.location.assign(target)
  else window.location.reload()
}

/** 页面成功渲染后清除标记，后续再发生分片失败仍可自动刷新一次 */
export function clearChunkReloadFlag() {
  try {
    sessionStorage.removeItem(KEY)
  } catch {
    /* ignore */
  }
}

/** 最近一次 Vite 预加载失败（调试用） */
export let lastPreloadError: unknown = null

/** 全局监听：Vite 预加载失败事件 + 未捕获的动态 import 失败 */
export function setupChunkErrorHandler() {
  // Vite 预加载失败（含 Unable to preload CSS）：不 preventDefault，让错误继续抛给动态 import，
  // 路由懒加载由 router.onError 带着目标地址处理，其余情况落到下面的 unhandledrejection
  window.addEventListener('vite:preloadError', (e) => {
    lastPreloadError = (e as Event & { payload?: unknown }).payload
  })
  window.addEventListener('unhandledrejection', (e) => {
    if (isChunkError(e.reason)) {
      e.preventDefault()
      handleChunkError()
    }
  })
}
