/** 支付渠道调起辅助 */

interface WeixinJSBridgeLike {
  invoke(api: string, params: Record<string, string>, cb: (res: { err_msg?: string }) => void): void
}

declare global {
  interface Window {
    WeixinJSBridge?: WeixinJSBridgeLike
  }
}

/**
 * 微信内 JSAPI 支付：WeixinJSBridge.invoke('getBrandWCPayRequest')。
 * 返回 ok / cancel / fail；结果最终以服务端 pay-status 为准。
 */
export function invokeWechatPay(params: Record<string, string>): Promise<'ok' | 'cancel' | 'fail'> {
  return new Promise((resolve) => {
    const call = () => {
      const bridge = window.WeixinJSBridge
      if (!bridge) return resolve('fail')
      bridge.invoke('getBrandWCPayRequest', params, (res) => {
        const msg = res?.err_msg || ''
        if (msg === 'get_brand_wcpay_request:ok') resolve('ok')
        else if (msg === 'get_brand_wcpay_request:cancel') resolve('cancel')
        else resolve('fail')
      })
    }
    if (window.WeixinJSBridge) call()
    else document.addEventListener('WeixinJSBridgeReady', call, { once: true })
  })
}

const PAYING_KEY = 'mall_h5_paying'

/** 跳转外部渠道前记录订单（支付宝 return_url 等回跳时可能不带 orderId） */
export function rememberPayingOrder(orderId: string, amount: string, type: string) {
  try {
    sessionStorage.setItem(PAYING_KEY, JSON.stringify({ orderId, amount, type, at: Date.now() }))
  } catch {
    /* ignore */
  }
}

/** 读取最近一次跳出支付的订单（30 分钟内有效） */
export function readPayingOrder(): { orderId: string; amount: string; type: string } | null {
  try {
    const raw = sessionStorage.getItem(PAYING_KEY)
    if (!raw) return null
    const v = JSON.parse(raw) as { orderId: string; amount: string; type: string; at: number }
    return Date.now() - v.at < 30 * 60 * 1000 ? v : null
  } catch {
    return null
  }
}

export function clearPayingOrder() {
  try {
    sessionStorage.removeItem(PAYING_KEY)
  } catch {
    /* ignore */
  }
}
