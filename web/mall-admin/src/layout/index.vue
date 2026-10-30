<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import TabsBar from './components/TabsBar.vue'

const app = useAppStore()
</script>

<template>
  <div class="layout" :class="{ 'is-mobile': app.isMobile }">
    <div v-if="app.isMobile && app.mobileMenuOpen" class="layout__mask" @click="app.mobileMenuOpen = false" />
    <Sidebar class="layout__sidebar" :class="{ 'is-open': app.mobileMenuOpen }" />
    <div class="layout__main">
      <div class="layout__header">
        <Navbar />
        <TabsBar />
      </div>
      <main class="layout__content">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-slide" mode="out-in" appear>
            <div :key="route.path + app.reloadKey" class="layout__page">
              <component :is="Component" />
            </div>
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped lang="scss">
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;

  &__sidebar {
    flex: none;
    z-index: 20;
  }
  &__main {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }
  &__header {
    flex: none;
    z-index: 10;
  }
  &__content {
    flex: 1;
    overflow: auto;
    padding: var(--app-gap);
  }
  &__page {
    min-height: 100%;
  }

  &.is-mobile {
    .layout__sidebar {
      position: fixed;
      left: 0;
      top: 0;
      bottom: 0;
      transform: translateX(-100%);
      transition: transform 0.25s ease;
      box-shadow: var(--app-shadow-pop);
      &.is-open {
        transform: translateX(0);
      }
    }
    .layout__content {
      padding: 10px;
    }
  }
  &__mask {
    position: fixed;
    inset: 0;
    z-index: 19;
    background: var(--app-mask);
  }
}
</style>
