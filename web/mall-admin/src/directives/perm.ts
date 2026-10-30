import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 按钮级权限：v-perm="'goods:goods:edit'" 或 v-perm="['a', 'b']"（任一满足）
 * 无权限时直接移除元素，避免通过样式绕过。
 */
function check(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
  const user = useUserStore()
  if (!user.hasPerm(binding.value)) {
    el.parentNode?.removeChild(el)
  }
}

export const vPerm: Directive<HTMLElement, string | string[]> = {
  mounted: check,
}
