import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { MallConfig, OrderSubmit } from '@/types'
import { getConfig } from '@/api/public'

const CHECKOUT_KEY = 'mall_h5_checkout'

export const useAppStore = defineStore('app', () => {
  const config = ref<MallConfig>({ mallName: '商城', servicePhone: '', freightAmount: 0, freightFreeThreshold: 0 })
  let loaded: Promise<MallConfig> | null = null

  /** 商城配置只拉一次 */
  function loadConfig() {
    if (!loaded) {
      loaded = getConfig()
        .then((c) => {
          if (c) config.value = { ...config.value, ...c }
          if (c?.mallName) document.title = c.mallName
          return config.value
        })
        .catch(() => {
          loaded = null
          return config.value
        })
    }
    return loaded
  }

  /** 结算入参：用 sessionStorage 保存，刷新确认订单页不丢失 */
  const checkout = ref<OrderSubmit | null>(readCheckout())

  function setCheckout(p: OrderSubmit) {
    checkout.value = p
    selectedAddressId.value = p.addressId || ''
    try {
      sessionStorage.setItem(CHECKOUT_KEY, JSON.stringify(p))
    } catch {
      /* ignore */
    }
  }

  /** 确认订单页选择地址：地址列表（选择模式）写入，确认页读取 */
  const selectedAddressId = ref<string>('')

  return { config, loadConfig, checkout, setCheckout, selectedAddressId }
})

function readCheckout(): OrderSubmit | null {
  try {
    const raw = sessionStorage.getItem(CHECKOUT_KEY)
    return raw ? (JSON.parse(raw) as OrderSubmit) : null
  } catch {
    return null
  }
}
