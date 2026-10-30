<script setup lang="ts">
import { logApi } from '@/api/system'
import type { LoginLog } from '@/types'
import { useTable } from '@/composables/useTable'
import { formatTime } from '@/utils/format'
import { USER_TYPE } from '@/utils/constants'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'

const { query, list, total, page, size, loading, load, search, reset, onPageChange, onSizeChange } = useTable(logApi.loginPage, {
  userType: '',
  username: '',
  success: '' as number | '',
})

const isOk = (v: LoginLog['success']) => v === true || v === 1

/** 从 UA 中提取浏览器与系统，便于阅读 */
function parseUA(ua = '') {
  const pick = (re: RegExp, name: string) => {
    const m = re.exec(ua)
    return m ? `${name} ${m[1].split('.')[0]}` : ''
  }
  const browser =
    pick(/Edg\/([\d.]+)/, 'Edge') ||
    pick(/Chrome\/([\d.]+)/, 'Chrome') ||
    pick(/Firefox\/([\d.]+)/, 'Firefox') ||
    pick(/Version\/([\d.]+).*Safari/, 'Safari') ||
    (/MicroMessenger/.test(ua) ? '微信' : '')
  const os = /Windows NT/.test(ua)
    ? 'Windows'
    : /iPhone|iPad/.test(ua)
      ? 'iOS'
      : /Mac OS X/.test(ua)
        ? 'macOS'
        : /Android/.test(ua)
          ? 'Android'
          : /Linux/.test(ua)
            ? 'Linux'
            : ''
  return [browser, os].filter(Boolean).join(' · ') || ua || '-'
}
</script>

<template>
  <div class="page">
    <div class="card">
      <el-form :model="query" inline class="search-bar" @submit.prevent="search">
        <el-form-item label="用户类型">
          <el-select v-model="query.userType" placeholder="全部" clearable>
            <el-option v-for="o in USER_TYPE" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="query.username" placeholder="用户名 / 手机号" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.success" placeholder="全部" clearable>
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="search">搜索</el-button>
          <el-button :icon="'Refresh'" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="toolbar__left text-secondary">连续失败 5 次将锁定账号 15 分钟</div>
        <div class="toolbar__right">
          <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="load" /></el-tooltip>
        </div>
      </div>
      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="用户类型" width="100" align="center">
          <template #default="{ row }: { row: any }"><StatusTag :value="row.userType" :options="USER_TYPE" :dot="false" /></template>
        </el-table-column>
        <el-table-column label="账号" prop="username" min-width="140" />
        <el-table-column label="IP" prop="ip" width="140" />
        <el-table-column label="终端" min-width="180">
          <template #default="{ row }: { row: any }">
            <el-tooltip :content="row.userAgent" placement="top" :disabled="!row.userAgent">
              <span>{{ parseUA(row.userAgent) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }: { row: any }">
            <StatusTag :label="isOk(row.success) ? '成功' : '失败'" :tone="isOk(row.success) ? 'success' : 'danger'" />
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="message" min-width="160" show-overflow-tooltip />
        <el-table-column label="时间" width="170">
          <template #default="{ row }: { row: any }"><span class="text-secondary">{{ formatTime(row.createdAt) }}</span></template>
        </el-table-column>
        <template #empty><EmptyState text="暂无登录日志" /></template>
      </el-table>
      <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </div>
  </div>
</template>
