<script setup lang="ts">
import { ref } from 'vue'
import { logApi } from '@/api/system'
import type { OperLog } from '@/types'
import { useTable } from '@/composables/useTable'
import { formatTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'

const { query, list, total, page, size, loading, load, search, reset, onPageChange, onSizeChange } = useTable(
  logApi.operPage,
  { username: '', module: '', success: '' as number | '', range: [] as string[] },
  {
    beforeFetch: ({ range, ...rest }) => {
      const r = range as string[] | undefined
      return { ...rest, startTime: r?.[0], endTime: r?.[1] }
    },
  },
)

const METHOD_TONE: Record<string, 'success' | 'primary' | 'warning' | 'danger'> = { GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }
const isOk = (v: OperLog['success']) => v === true || v === 1

const drawer = ref(false)
const current = ref<OperLog | null>(null)
function show(row: OperLog) {
  current.value = row
  drawer.value = true
}

function pretty(s?: string) {
  if (!s) return '-'
  try {
    return JSON.stringify(JSON.parse(s), null, 2)
  } catch {
    return s
  }
}
</script>

<template>
  <div class="page">
    <div class="card">
      <el-form :model="query" inline class="search-bar" @submit.prevent="search">
        <el-form-item label="操作人">
          <el-input v-model="query.username" placeholder="用户名" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="模块">
          <el-input v-model="query.module" placeholder="如：商品" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.success" placeholder="全部" clearable>
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="query.range"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD HH:mm:ss"
            :default-time="[new Date(2000, 0, 1, 0, 0, 0), new Date(2000, 0, 1, 23, 59, 59)]"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="search">搜索</el-button>
          <el-button :icon="'Refresh'" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="toolbar__left text-secondary">管理端所有写操作自动记录，敏感字段已脱敏</div>
        <div class="toolbar__right">
          <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="load" /></el-tooltip>
        </div>
      </div>
      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="操作人" prop="username" width="120" />
        <el-table-column label="模块 / 操作" min-width="180">
          <template #default="{ row }: { row: any }">
            <div>{{ row.module }}</div>
            <div class="text-secondary sub">{{ row.action }}</div>
          </template>
        </el-table-column>
        <el-table-column label="请求" min-width="260">
          <template #default="{ row }: { row: any }">
            <StatusTag :label="row.method" :tone="METHOD_TONE[row.method] || 'info'" :dot="false" />
            <span class="mono uri">{{ row.uri }}</span>
          </template>
        </el-table-column>
        <el-table-column label="IP" prop="ip" width="130" />
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }: { row: any }">
            <StatusTag :label="isOk(row.success) ? '成功' : '失败'" :tone="isOk(row.success) ? 'success' : 'danger'" />
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90" align="right">
          <template #default="{ row }: { row: any }">
            <span :class="{ 'text-warning': row.costMs > 1000 }">{{ row.costMs }} ms</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }: { row: any }"><span class="text-secondary">{{ formatTime(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }: { row: any }"><el-button link type="primary" @click="show(row)">详情</el-button></template>
        </el-table-column>
        <template #empty><EmptyState text="暂无操作日志" /></template>
      </el-table>
      <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <el-drawer v-model="drawer" title="日志详情" size="560px">
      <div v-if="current" class="log-detail">
        <div class="desc-grid">
          <div class="desc-item"><span class="desc-item__label">操作人</span><span class="desc-item__value">{{ current.username }}</span></div>
          <div class="desc-item"><span class="desc-item__label">IP</span><span class="desc-item__value">{{ current.ip }}</span></div>
          <div class="desc-item"><span class="desc-item__label">模块</span><span class="desc-item__value">{{ current.module }} / {{ current.action }}</span></div>
          <div class="desc-item"><span class="desc-item__label">耗时</span><span class="desc-item__value">{{ current.costMs }} ms</span></div>
          <div class="desc-item"><span class="desc-item__label">时间</span><span class="desc-item__value">{{ formatTime(current.createdAt) }}</span></div>
          <div class="desc-item">
            <span class="desc-item__label">结果</span>
            <StatusTag :label="isOk(current.success) ? '成功' : '失败'" :tone="isOk(current.success) ? 'success' : 'danger'" />
          </div>
        </div>
        <h4>请求</h4>
        <div class="mono req">{{ current.method }} {{ current.uri }}</div>
        <h4>参数</h4>
        <pre class="code">{{ pretty(current.params) }}</pre>
        <template v-if="current.errorMsg">
          <h4>错误信息</h4>
          <pre class="code code--error">{{ current.errorMsg }}</pre>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.sub {
  font-size: 12px;
}
.uri {
  margin-left: 8px;
  font-size: 12px;
  color: var(--app-text-regular);
  word-break: break-all;
}
.log-detail {
  h4 {
    margin: 20px 0 8px;
    font-size: 14px;
    color: var(--app-text);
  }
  .desc-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
.req {
  padding: 8px 12px;
  border-radius: var(--app-radius);
  background: var(--app-card-bg-muted);
  word-break: break-all;
}
.code {
  margin: 0;
  padding: 12px;
  max-height: 360px;
  overflow: auto;
  font-family: 'JetBrains Mono', Consolas, Menlo, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  border-radius: var(--app-radius);
  color: var(--app-text-regular);
  background: var(--app-card-bg-muted);
  border: 1px solid var(--app-border);
  &--error {
    color: var(--app-danger);
    background: var(--app-danger-soft);
  }
}
</style>
