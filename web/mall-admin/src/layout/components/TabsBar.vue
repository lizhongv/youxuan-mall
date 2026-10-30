<script setup lang="ts">
import { nextTick, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTabsStore, type TabItem } from '@/stores/tabs'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const tabs = useTabsStore()
const app = useAppStore()

const scrollRef = ref<HTMLElement>()
const menu = reactive({ visible: false, x: 0, y: 0, tab: null as TabItem | null })

function go(t: TabItem) {
  if (t.path !== route.path) router.push(t.fullPath || t.path)
}

function close(t: TabItem) {
  const next = tabs.close(t.path, route.path)
  if (next) router.push(next)
}

function openMenu(e: MouseEvent, t: TabItem) {
  menu.visible = true
  menu.x = e.clientX
  menu.y = e.clientY
  menu.tab = t
  const hide = () => {
    menu.visible = false
    document.removeEventListener('click', hide)
  }
  setTimeout(() => document.addEventListener('click', hide))
}

function onCommand(cmd: 'reload' | 'close' | 'others' | 'left' | 'right' | 'all') {
  const t = menu.tab
  menu.visible = false
  if (!t) return
  switch (cmd) {
    case 'reload':
      if (t.path !== route.path) router.push(t.fullPath)
      else app.reload()
      break
    case 'close':
      close(t)
      break
    case 'others':
      tabs.closeOthers(t.path)
      go(t)
      break
    case 'left':
      tabs.closeLeft(t.path)
      if (!tabs.tabs.some((x) => x.path === route.path)) go(t)
      break
    case 'right':
      tabs.closeRight(t.path)
      if (!tabs.tabs.some((x) => x.path === route.path)) go(t)
      break
    case 'all':
      tabs.closeAll()
      router.push(tabs.tabs[tabs.tabs.length - 1]?.fullPath || '/')
      break
  }
}

function onWheel(e: WheelEvent) {
  if (scrollRef.value) scrollRef.value.scrollLeft += e.deltaY
}

// 激活标签滚入可视区
watch(
  () => route.path,
  () =>
    nextTick(() => {
      scrollRef.value?.querySelector('.tabs-bar__item.is-active')?.scrollIntoView({ block: 'nearest', inline: 'nearest', behavior: 'smooth' })
    }),
  { immediate: true },
)
</script>

<template>
  <div class="tabs-bar">
    <div ref="scrollRef" class="tabs-bar__scroll" @wheel.prevent="onWheel">
      <div
        v-for="t in tabs.tabs"
        :key="t.path"
        class="tabs-bar__item"
        :class="{ 'is-active': t.path === route.path }"
        @click="go(t)"
        @click.middle="close(t)"
        @contextmenu.prevent="openMenu($event, t)"
      >
        <el-icon v-if="t.icon" class="tabs-bar__icon"><component :is="t.icon" /></el-icon>
        <span>{{ t.title }}</span>
        <el-icon v-if="!t.affix" class="tabs-bar__close" @click.stop="close(t)"><Close /></el-icon>
      </div>
    </div>
    <el-dropdown trigger="click" @command="onCommand" @visible-change="(v: boolean) => v && (menu.tab = tabs.tabs.find((x) => x.path === route.path) || null)">
      <button class="tabs-bar__more" title="标签操作"><el-icon><ArrowDown /></el-icon></button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="reload" :icon="'RefreshRight'">刷新当前</el-dropdown-item>
          <el-dropdown-item command="close" :icon="'Close'">关闭当前</el-dropdown-item>
          <el-dropdown-item command="others" :icon="'CircleClose'">关闭其他</el-dropdown-item>
          <el-dropdown-item command="all" :icon="'FolderDelete'">关闭全部</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <teleport to="body">
      <ul v-show="menu.visible" class="tabs-ctx" :style="{ left: `${menu.x}px`, top: `${menu.y}px` }">
        <li @click="onCommand('reload')"><el-icon><RefreshRight /></el-icon>刷新</li>
        <li v-if="!menu.tab?.affix" @click="onCommand('close')"><el-icon><Close /></el-icon>关闭</li>
        <li @click="onCommand('others')"><el-icon><CircleClose /></el-icon>关闭其他</li>
        <li @click="onCommand('left')"><el-icon><Back /></el-icon>关闭左侧</li>
        <li @click="onCommand('right')"><el-icon><Right /></el-icon>关闭右侧</li>
        <li @click="onCommand('all')"><el-icon><FolderDelete /></el-icon>关闭全部</li>
      </ul>
    </teleport>
  </div>
</template>

<style scoped lang="scss">
.tabs-bar {
  display: flex;
  align-items: center;
  height: var(--app-tabs-height);
  padding: 0 8px 0 12px;
  background: var(--app-tabs-bg);
  border-bottom: 1px solid var(--app-border);
  gap: 6px;

  &__scroll {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 6px;
    overflow-x: auto;
    overflow-y: hidden;
    scrollbar-width: none;
    &::-webkit-scrollbar {
      display: none;
    }
  }
  &__item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    flex: none;
    height: 28px;
    padding: 0 10px;
    border-radius: var(--app-radius-sm);
    font-size: 13px;
    color: var(--app-text-regular);
    background: var(--app-card-bg-muted);
    border: 1px solid var(--app-border);
    cursor: pointer;
    user-select: none;
    transition: all 0.2s;

    &:hover {
      color: var(--el-color-primary);
    }
    &.is-active {
      color: var(--el-color-primary);
      background: var(--app-primary-soft);
      border-color: var(--el-color-primary-light-7);
      font-weight: 500;
    }
  }
  &__icon {
    font-size: 13px;
  }
  &__close {
    font-size: 12px;
    border-radius: 50%;
    padding: 1px;
    transition: all 0.15s;
    &:hover {
      background: var(--el-color-primary);
      color: var(--el-color-white);
    }
  }
  &__more {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border: 1px solid var(--app-border);
    border-radius: var(--app-radius-sm);
    background: var(--app-card-bg-muted);
    color: var(--app-text-regular);
    cursor: pointer;
  }
}
</style>

<style lang="scss">
.tabs-ctx {
  position: fixed;
  z-index: 3000;
  margin: 0;
  padding: 6px;
  list-style: none;
  min-width: 128px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  box-shadow: var(--app-shadow-pop);

  li {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 7px 10px;
    font-size: 13px;
    color: var(--app-text-regular);
    border-radius: var(--app-radius-sm);
    cursor: pointer;
    &:hover {
      background: var(--app-primary-soft);
      color: var(--el-color-primary);
    }
  }
}
</style>
