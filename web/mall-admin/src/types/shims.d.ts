import type { vPerm } from '@/directives/perm'

declare module 'vue' {
  interface ComponentCustomProperties {
    vPerm: typeof vPerm
  }
}

export {}
