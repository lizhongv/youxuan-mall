import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCartCount } from '@/api/cart'
import { useUserStore } from './user'

/** 购物车角标数量（标签栏、详情页共用） */
export const useCartStore = defineStore('cart', () => {
  const count = ref(0)

  async function refreshCount() {
    const user = useUserStore()
    if (!user.isLogin) {
      count.value = 0
      return
    }
    try {
      count.value = Number(await getCartCount()) || 0
    } catch {
      /* 静默 */
    }
  }

  function setCount(n: number) {
    count.value = n
  }

  return { count, refreshCount, setCount }
})
