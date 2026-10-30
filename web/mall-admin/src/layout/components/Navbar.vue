<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { breadcrumbMap } from '@/router/routes'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'
import { confirm } from '@/utils/message'
import PasswordDialog from './PasswordDialog.vue'

const route = useRoute()
const router = useRouter()
const app = useAppStore()
const user = useUserStore()
const tabs = useTabsStore()

const crumbs = computed(() => {
  const list = breadcrumbMap[String(route.name)] || [route.meta.title || '']
  const last = route.meta.tabTitle ? route.meta.tabTitle(route) : list[list.length - 1]
  return [...list.slice(0, -1), last].filter(Boolean)
})

const isFull = ref(false)
function syncFull() {
  isFull.value = !!document.fullscreenElement
}
function toggleFull() {
  if (document.fullscreenElement) document.exitFullscreen()
  else document.documentElement.requestFullscreen?.()
}
onMounted(() => document.addEventListener('fullscreenchange', syncFull))
onBeforeUnmount(() => document.removeEventListener('fullscreenchange', syncFull))

/** 主题切换：支持 View Transition 时以按钮为圆心扩散过渡 */
function toggleTheme(e: MouseEvent) {
  const next = app.theme === 'dark' ? 'light' : 'dark'
  const doc = document as Document & { startViewTransition?: (cb: () => void) => { ready: Promise<void> } }
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (!doc.startViewTransition || reduce) {
    app.setTheme(next)
    return
  }
  const x = e.clientX
  const y = e.clientY
  const r = Math.hypot(Math.max(x, innerWidth - x), Math.max(y, innerHeight - y))
  const t = doc.startViewTransition(() => app.setTheme(next))
  t.ready.then(() => {
    document.documentElement.animate(
      { clipPath: [`circle(0px at ${x}px ${y}px)`, `circle(${r}px at ${x}px ${y}px)`] },
      { duration: 420, easing: 'ease-out', pseudoElement: '::view-transition-new(root)' },
    )
  })
}

const pwdVisible = ref(false)

async function onCommand(cmd: string) {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'password') pwdVisible.value = true
  else if (cmd === 'logout') {
    if (!(await confirm('确定退出登录吗？', '退出登录', 'info'))) return
    await user.logout()
    tabs.reset()
    router.replace('/login')
  }
}
</script>

<template>
  <header class="navbar">
    <div class="navbar__left">
      <button class="navbar__btn" :title="app.collapsed ? '展开菜单' : '收起菜单'" @click="app.toggleCollapse()">
        <el-icon :size="18"><Expand v-if="app.collapsed || app.isMobile" /><Fold v-else /></el-icon>
      </button>
      <el-breadcrumb separator="/" class="navbar__crumb">
        <el-breadcrumb-item v-for="(c, i) in crumbs" :key="i">{{ c }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="navbar__right">
      <el-tooltip content="刷新页面" placement="bottom">
        <button class="navbar__btn" @click="app.reload()">
          <el-icon :size="17"><RefreshRight /></el-icon>
        </button>
      </el-tooltip>
      <el-tooltip :content="isFull ? '退出全屏' : '全屏'" placement="bottom">
        <button class="navbar__btn hide-mobile" @click="toggleFull">
          <el-icon :size="17"><Aim v-if="isFull" /><FullScreen v-else /></el-icon>
        </button>
      </el-tooltip>
      <el-tooltip :content="app.theme === 'dark' ? '切换浅色' : '切换深色'" placement="bottom">
        <button class="navbar__btn" @click="toggleTheme">
          <el-icon :size="17"><Sunny v-if="app.theme === 'dark'" /><Moon v-else /></el-icon>
        </button>
      </el-tooltip>
      <el-dropdown trigger="click" @command="onCommand">
        <div class="navbar__user">
          <el-avatar :size="30" :src="user.info?.avatar || undefined" class="navbar__avatar">
            {{ user.displayName.slice(0, 1) }}
          </el-avatar>
          <span class="navbar__name hide-mobile">{{ user.displayName }}</span>
          <el-icon class="hide-mobile"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile" :icon="'User'">个人资料</el-dropdown-item>
            <el-dropdown-item command="password" :icon="'Lock'">修改密码</el-dropdown-item>
            <el-dropdown-item command="logout" :icon="'SwitchButton'" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    <PasswordDialog v-model="pwdVisible" />
  </header>
</template>

<style scoped lang="scss">
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--app-header-height);
  padding: 0 12px 0 8px;
  background: var(--app-header-bg);
  backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid var(--app-border);

  &__left,
  &__right {
    display: flex;
    align-items: center;
    gap: 4px;
    min-width: 0;
  }
  &__btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border: none;
    border-radius: var(--app-radius);
    background: transparent;
    color: var(--app-text-regular);
    cursor: pointer;
    transition: background 0.2s;
    &:hover {
      background: var(--app-sidebar-item-hover);
      color: var(--app-text);
    }
  }
  &__crumb {
    margin-left: 8px;
    white-space: nowrap;
    overflow: hidden;
  }
  &__user {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 4px 8px;
    margin-left: 4px;
    border-radius: var(--app-radius);
    cursor: pointer;
    color: var(--app-text);
    outline: none;
    &:hover {
      background: var(--app-sidebar-item-hover);
    }
  }
  &__avatar {
    background: var(--app-gradient);
    color: var(--el-color-white);
    font-weight: 600;
  }
  &__name {
    max-width: 120px;
    font-size: 14px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

@media (max-width: 768px) {
  .hide-mobile {
    display: none !important;
  }
  .navbar__crumb {
    display: none;
  }
}
</style>
