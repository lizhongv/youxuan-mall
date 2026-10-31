import { useRouter } from 'vue-router'
import { showConfirmDialog, showLoadingToast, showSuccessToast, closeToast, showToast } from 'vant'
import type { Order, OrderItem } from '@/types'
import { cancelOrder, deleteOrder, receiveOrder } from '@/api/order'
import { addCart } from '@/api/cart'
import { useCartStore } from '@/stores/cart'

export type OrderAction = 'cancel' | 'pay' | 'receive' | 'review' | 'aftersale' | 'delete' | 'rebuy' | 'logistics'

/** 每个状态下可见的操作按钮（顺序即展示顺序，最后一个为主按钮） */
export function orderButtons(order: Order): OrderAction[] {
  switch (order.status) {
    case 'PENDING_PAY':
      return ['cancel', 'pay']
    case 'PENDING_DELIVERY':
      return ['aftersale', 'rebuy']
    case 'DELIVERED':
      return ['aftersale', 'logistics', 'receive']
    case 'COMPLETED':
      return order.reviewed ? ['delete', 'aftersale', 'rebuy'] : ['delete', 'aftersale', 'rebuy', 'review']
    case 'CANCELLED':
      return ['delete', 'rebuy']
    default:
      return []
  }
}

export const ACTION_TEXT: Record<OrderAction, string> = {
  cancel: '取消订单',
  pay: '去支付',
  receive: '确认收货',
  review: '评价',
  aftersale: '申请售后',
  delete: '删除订单',
  rebuy: '再次购买',
  logistics: '查看物流',
}


/** 可申请售后的明细：未在售后中、未退款 */
export function canApplyAftersale(order: Order, item: OrderItem) {
  if (!['PENDING_DELIVERY', 'DELIVERED', 'COMPLETED'].includes(order.status)) return false
  return !item.aftersaleStatus || item.aftersaleStatus === 'NONE'
}

/** 订单通用操作；onChanged 在状态变更后回调（刷新列表/详情） */
export function useOrderActions(onChanged: (type: OrderAction, order: Order) => void) {
  const router = useRouter()
  const cart = useCartStore()

  async function doCancel(order: Order, reason: string) {
    showLoadingToast({ message: '取消中...', forbidClick: true })
    try {
      await cancelOrder(order.id, reason)
      showSuccessToast('订单已取消')
      onChanged('cancel', order)
    } catch {
      closeToast()
    }
  }

  async function handle(type: OrderAction, order: Order, cancelPicker?: (order: Order) => void) {
    switch (type) {
      case 'pay':
        router.push({ name: 'cashier', params: { orderId: order.id } })
        return
      case 'cancel':
        if (cancelPicker) cancelPicker(order)
        else {
          await showConfirmDialog({ title: '取消订单', message: '确定取消该订单吗？' })
          await doCancel(order, '不想买了')
        }
        return
      case 'receive':
        await showConfirmDialog({ title: '确认收货', message: '请确认已收到商品，确认后订单将完成' })
        await receiveOrder(order.id)
        showSuccessToast('已确认收货')
        onChanged('receive', order)
        return
      case 'delete':
        await showConfirmDialog({ title: '删除订单', message: '删除后订单不可恢复，确定删除吗？' })
        await deleteOrder(order.id)
        showSuccessToast('已删除')
        onChanged('delete', order)
        return
      case 'review': {
        const item = order.items.find((i) => !i.reviewed)
        if (!item) return showToast('该订单已全部评价')
        router.push({ name: 'review-create', query: { orderId: order.id, itemId: item.id } })
        return
      }
      case 'aftersale': {
        const items = order.items.filter((i) => canApplyAftersale(order, i))
        if (!items.length) {
          router.push({ name: 'aftersale-list' })
          return
        }
        if (items.length === 1) router.push({ name: 'aftersale-apply', query: { orderId: order.id, itemId: items[0].id } })
        else router.push({ name: 'order-detail', params: { id: order.id } })
        return
      }
      case 'logistics':
        router.push({ name: 'logistics', params: { id: order.id } })
        return
      case 'rebuy':
        showLoadingToast({ message: '加入购物车...', forbidClick: true })
        try {
          for (const it of order.items) await addCart(it.skuId, it.quantity)
          closeToast()
          await cart.refreshCount()
          router.push({ name: 'cart' })
        } catch {
          closeToast()
        }
        return
    }
  }

  return {
    handle: (type: OrderAction, order: Order, cancelPicker?: (order: Order) => void) =>
      handle(type, order, cancelPicker).catch(() => {
        /* 用户取消确认框或请求失败（已提示） */
      }),
    doCancel,
  }
}
