<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { menuTree, type MenuNode } from '@/router/routes'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const logoUrl = `${import.meta.env.BASE_URL}logo.svg`
const route = useRoute()
const user = useUserStore()
const app = useAppStore()

/** 按权限过滤菜单；超级管理员（roles 含 super_admin）全部可见；空分组隐藏 */
const menus = computed<MenuNode[]>(() =>
  menuTree
    .map((n) => (n.children ? { ...n, children: n.children.filter((c) => user.hasPerm(c.permission)) } : n))
    .filter((n) => (n.children ? n.children.length > 0 : user.hasPerm(n.permission))),
)

const active = computed(() => route.meta.activeMenu || route.path)
const collapsed = computed(() => !app.isMobile && app.collapsed)
const defaultOpeneds = computed(() => menus.value.filter((m) => m.children).map((m) => m.path))

function onSelect() {
  if (app.isMobile) app.mobileMenuOpen = false
}
</script>

<template>
  <aside class="sidebar" :class="{ 'is-collapsed': collapsed }">
    <router-link to="/" class="sidebar__logo">
      <img :src="logoUrl" alt="" />
      <transition name="fade">
        <span v-show="!collapsed" class="sidebar__brand">商城管理后台</span>
      </transition>
    </router-link>
    <el-scrollbar class="sidebar__scroll">
      <el-menu
        :default-active="active"
        :default-openeds="defaultOpeneds"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        unique-opened
        class="sidebar__menu"
        @select="onSelect"
      >
        <template v-for="m in menus" :key="m.path">
          <el-sub-menu v-if="m.children" :index="m.path">
            <template #title>
              <el-icon><component :is="m.icon" /></el-icon>
              <span>{{ m.title }}</span>
            </template>
            <el-menu-item v-for="c in m.children" :key="c.path" :index="c.path">
              <el-icon><component :is="c.icon" /></el-icon>
              <template #title>{{ c.title }}</template>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="m.path">
            <el-icon><component :is="m.icon" /></el-icon>
            <template #title>{{ m.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-scrollbar>
  </aside>
</template>

<style scoped lang="scss">
.sidebar {
  display: flex;
  flex-direction: column;
  width: var(--app-sidebar-width);
  height: 100%;
  background: var(--app-sidebar-bg);
  border-right: 1px solid var(--app-border);
  transition: width 0.25s ease;
  overflow: hidden;

  &.is-collapsed {
    width: var(--app-sidebar-collapsed);
  }

  &__logo {
    display: flex;
    align-items: center;
    gap: 10px;
    height: var(--app-header-height);
    padding: 0 16px;
    flex: none;
    white-space: nowrap;
    img {
      width: 32px;
      height: 32px;
      flex: none;
    }
  }
  &__brand {
    font-size: 16px;
    font-weight: 700;
    letter-spacing: 0.5px;
    background: var(--app-gradient);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
  }
  &__scroll {
    flex: 1;
  }
  &__menu {
    --el-menu-bg-color: var(--app-sidebar-bg);
    --el-menu-text-color: var(--app-sidebar-text);
    --el-menu-hover-bg-color: var(--app-sidebar-item-hover);
    --el-menu-active-color: var(--app-sidebar-text-active);
    --el-menu-item-height: 44px;
    --el-menu-sub-item-height: 42px;
    border-right: none;
    padding: 4px 10px 16px;

    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      border-radius: var(--app-radius);
      margin: 2px 0;
    }
    :deep(.el-menu-item.is-active) {
      background: var(--app-sidebar-item-active);
      font-weight: 600;
      position: relative;
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 12px;
        bottom: 12px;
        width: 3px;
        border-radius: 0 3px 3px 0;
        background: var(--el-color-primary);
      }
    }
    :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
      color: var(--app-sidebar-text-active);
    }
    :deep(.el-sub-menu .el-menu) {
      background: transparent;
    }
    :deep(.el-sub-menu .el-menu-item) {
      padding-left: 46px !important;
    }
    &.el-menu--collapse {
      padding: 4px 8px;
      width: 100%;
      :deep(.el-menu-item),
      :deep(.el-sub-menu__title) {
        padding: 0 !important;
        justify-content: center;
      }
    }
  }
}
</style>
