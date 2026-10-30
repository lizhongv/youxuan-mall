import { useRouter, useRoute } from 'vue-router'
import { showSuccessToast } from 'vant'
import type { Coupon } from '@/types'
import { receiveCoupon } from '@/api/coupon'
import { useUserStore } from '@/stores/user'

/** 领券：未登录先去登录；成功后本地累加已领数量 */
export function useReceiveCoupon() {
  const router = useRouter()
  const route = useRoute()
  const user = useUserStore()

  /** 是否还能领取（发行量与每人限领） */
  function canReceive(c: Coupon) {
    const soldOut = c.total > 0 && c.issued >= c.total
    const reachedLimit = c.perLimit > 0 && (c.received || 0) >= c.perLimit
    return !soldOut && !reachedLimit
  }

  function statusText(c: Coupon) {
    if (c.perLimit > 0 && (c.received || 0) >= c.perLimit) return '已领取'
    if (c.total > 0 && c.issued >= c.total) return '已抢光'
    return (c.received || 0) > 0 ? '再领一张' : '立即领取'
  }

  async function receive(c: Coupon) {
    if (!user.isLogin) {
      router.push({ name: 'login', query: { redirect: route.fullPath } })
      return false
    }
    if (!canReceive(c)) return false
    try {
      await receiveCoupon(c.id)
      c.received = (c.received || 0) + 1
      c.issued = (c.issued || 0) + 1
      showSuccessToast('领取成功')
      return true
    } catch {
      return false
    }
  }

  return { receive, canReceive, statusText }
}
