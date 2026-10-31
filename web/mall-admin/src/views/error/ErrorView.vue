<script setup lang="ts">
import { useRouter } from 'vue-router'

defineProps<{ code: string; title: string; desc: string }>()
const router = useRouter()

function back() {
  if (window.history.state?.back) router.back()
  else router.replace('/')
}
</script>

<template>
  <div class="error-view">
    <svg class="error-view__art" viewBox="0 0 320 200" aria-hidden="true">
      <ellipse cx="160" cy="182" rx="120" ry="10" class="ev-shadow" />
      <rect x="70" y="40" width="180" height="120" rx="14" class="ev-card" />
      <path d="M70 66h180" class="ev-line" />
      <circle cx="86" cy="53" r="4" class="ev-dot" />
      <circle cx="100" cy="53" r="4" class="ev-dot" />
      <circle cx="114" cy="53" r="4" class="ev-dot" />
      <text x="160" y="128" text-anchor="middle" class="ev-code">{{ code }}</text>
      <circle cx="252" cy="44" r="18" class="ev-badge" />
      <path d="M252 34v12M252 52v2" class="ev-mark" />
    </svg>
    <h2>{{ title }}</h2>
    <p>{{ desc }}</p>
    <div class="error-view__actions">
      <slot name="actions">
        <el-button @click="back">返回上一页</el-button>
        <el-button type="primary" @click="router.replace('/')">回到首页</el-button>
      </slot>
    </div>
  </div>
</template>

<style scoped lang="scss">
.error-view {
  min-height: 70vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 24px;

  &__art {
    width: min(360px, 90vw);
  }
  h2 {
    margin: 12px 0 8px;
    font-size: 22px;
    color: var(--app-text);
  }
  p {
    margin: 0;
    color: var(--app-text-secondary);
  }
  &__actions {
    margin-top: 24px;
    display: flex;
    gap: 12px;
  }
  .ev-shadow {
    fill: var(--app-border);
  }
  .ev-card {
    fill: var(--app-card-bg);
    stroke: var(--app-border-strong);
    stroke-width: 2;
  }
  .ev-line {
    stroke: var(--app-border-strong);
    stroke-width: 2;
  }
  .ev-dot {
    fill: var(--app-border-strong);
  }
  .ev-code {
    font-size: 54px;
    font-weight: 800;
    fill: var(--el-color-primary);
    letter-spacing: 4px;
  }
  .ev-badge {
    fill: var(--app-warning);
  }
  .ev-mark {
    stroke: var(--el-color-white);
    stroke-width: 4;
    stroke-linecap: round;
  }
}
</style>
