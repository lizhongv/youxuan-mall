<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowRef } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { levelApi, memberApi } from '@/api/member'
import type { Member, MemberLevel, Switch } from '@/types'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { formatTime, money } from '@/utils/format'
import { ACCOUNT_BIZ_TYPE, ACCOUNT_TYPE, GENDER, MEMBER_STATUS, toMap } from '@/utils/constants'
import { confirm, toast } from '@/utils/message'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'

const user = useUserStore()
const canEdit = computed(() => user.hasPerm('member:member:edit'))
const genderMap = toMap(GENDER)
const bizMap = toMap(ACCOUNT_BIZ_TYPE)

const { query, list, total, page, size, loading, load, search, reset, onPageChange, onSizeChange } = useTable(memberApi.page, {
  keyword: '',
  levelId: '',
  status: '' as Switch | '',
})

const levels = shallowRef<MemberLevel[]>([])
onMounted(async () => {
  try {
    levels.value = (await levelApi.list()) || []
  } catch {
    /* 忽略 */
  }
})

async function toggleStatus(row: Member) {
  const status = (row.status ? 0 : 1) as Switch
  if (!status && !(await confirm(`冻结后「${row.nickname || row.mobile}」将无法登录和下单，确定冻结吗？`))) return
  await memberApi.setStatus(row.id, status)
  row.status = status
  list.value = [...list.value]
  if (detail.value?.id === row.id) detail.value.status = status
  toast.success(status ? '已解冻' : '已冻结')
}

// ---------------- 详情抽屉 ----------------
const drawer = ref(false)
const detail = ref<Member | null>(null)
const detailLoading = ref(false)
const tab = ref('info')

const logs = useTable(memberApi.accountLogs, { memberId: '', accountType: '' }, { immediate: false, pageSize: 10 })

async function openDetail(row: Member) {
  drawer.value = true
  tab.value = 'info'
  detail.value = row
  detailLoading.value = true
  logs.query.memberId = row.id
  logs.query.accountType = ''
  logs.search()
  try {
    detail.value = await memberApi.detail(row.id)
  } finally {
    detailLoading.value = false
  }
}

// ---------------- 调账 ----------------
const adjustVisible = ref(false)
const adjusting = ref<Member | null>(null)
const adjustSaving = ref(false)
const adjustRef = ref<FormInstance>()
const adjustForm = reactive({ accountType: 'BALANCE' as 'BALANCE' | 'POINTS', direction: 1 as 1 | -1, amount: 0, remark: '' })
const adjustRules: FormRules = {
  amount: [{ validator: (_r, v: number, cb) => (v > 0 ? cb() : cb(new Error('请输入调整数值'))), trigger: 'blur' }],
  remark: [{ required: true, message: '请填写调整原因，将记录在账户流水中', trigger: 'blur' }],
}
const currentValue = computed(() => (adjusting.value ? (adjustForm.accountType === 'BALANCE' ? Number(adjusting.value.balance) : adjusting.value.points) : 0))
const afterValue = computed(() => {
  const v = currentValue.value + adjustForm.direction * (adjustForm.amount || 0)
  return adjustForm.accountType === 'BALANCE' ? Math.round(v * 100) / 100 : v
})

function openAdjust(row: Member) {
  adjusting.value = row
  Object.assign(adjustForm, { accountType: 'BALANCE', direction: 1, amount: 0, remark: '' })
  adjustVisible.value = true
  setTimeout(() => adjustRef.value?.clearValidate())
}

async function submitAdjust() {
  if (!adjusting.value) return
  if (!(await adjustRef.value?.validate().catch(() => false))) return
  if (afterValue.value < 0) return toast.warning('扣减后不能为负数')
  adjustSaving.value = true
  try {
    await memberApi.adjust(adjusting.value.id, {
      accountType: adjustForm.accountType,
      amount: adjustForm.direction * adjustForm.amount,
      remark: adjustForm.remark.trim(),
    })
    toast.success('调整成功')
    adjustVisible.value = false
    load()
    if (drawer.value && detail.value?.id === adjusting.value.id) openDetail(adjusting.value)
  } finally {
    adjustSaving.value = false
  }
}

function addrText(a: { province: string; city: string; district: string; detail: string }) {
  return [a.province, a.city, a.district, a.detail].filter(Boolean).join(' ')
}
</script>

<template>
  <div class="page">
    <div class="card">
      <el-form :model="query" inline class="search-bar" @submit.prevent="search">
        <el-form-item label="会员">
          <el-input v-model="query.keyword" placeholder="手机号 / 昵称" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="query.levelId" placeholder="全部等级" clearable>
            <el-option v-for="l in levels" :key="l.id" :label="l.name" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option v-for="o in MEMBER_STATUS" :key="o.value" :label="o.label" :value="o.value" />
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
        <div class="toolbar__left text-secondary">共 {{ total }} 位会员</div>
        <div class="toolbar__right">
          <el-tooltip content="刷新"><el-button circle :icon="'Refresh'" @click="load" /></el-tooltip>
        </div>
      </div>
      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="会员" min-width="200">
          <template #default="{ row }: { row: any }">
            <div class="member-cell" @click="openDetail(row)">
              <el-avatar :size="38" :src="row.avatar || undefined">{{ (row.nickname || '会').slice(0, 1) }}</el-avatar>
              <div>
                <div class="member-cell__name">{{ row.nickname || '未设置昵称' }}</div>
                <div class="text-secondary sub">{{ genderMap[row.gender]?.label || '未知' }} · ID {{ row.id }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="手机号" prop="mobile" width="130" />
        <el-table-column label="等级" width="110">
          <template #default="{ row }: { row: any }">
            <span class="level-tag"><el-icon><Medal /></el-icon>{{ row.levelName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="110" align="right">
          <template #default="{ row }: { row: any }"><span class="money">{{ money(row.balance) }}</span></template>
        </el-table-column>
        <el-table-column label="积分" prop="points" width="90" align="right" />
        <el-table-column label="成长值" prop="growth" width="90" align="right" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }: { row: any }">
            <el-switch
              :model-value="row.status === 1"
              :disabled="!canEdit"
              inline-prompt
              active-text="正常"
              inactive-text="冻结"
              @change="toggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="160">
          <template #default="{ row }: { row: any }"><span class="text-secondary">{{ formatTime(row.lastLoginAt) }}</span></template>
        </el-table-column>
        <el-table-column label="注册时间" width="160">
          <template #default="{ row }: { row: any }"><span class="text-secondary">{{ formatTime(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="canEdit" link type="primary" @click="openAdjust(row)">调账</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState text="暂无会员" /></template>
      </el-table>
      <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <!-- 详情 -->
    <el-drawer v-model="drawer" title="会员详情" size="640px">
      <div v-if="detail" v-loading="detailLoading" class="member-detail">
        <div class="profile-head">
          <el-avatar :size="60" :src="detail.avatar || undefined">{{ (detail.nickname || '会').slice(0, 1) }}</el-avatar>
          <div class="profile-head__main">
            <div class="profile-head__name">
              {{ detail.nickname || '未设置昵称' }}
              <span class="level-tag"><el-icon><Medal /></el-icon>{{ detail.levelName || '-' }}</span>
              <StatusTag :value="detail.status" :options="MEMBER_STATUS" />
            </div>
            <div class="text-secondary sub">{{ detail.mobile }} · 注册于 {{ formatTime(detail.createdAt, 'YYYY-MM-DD') }}</div>
          </div>
          <el-button v-if="canEdit" type="primary" plain size="small" @click="openAdjust(detail)">调账</el-button>
        </div>
        <div class="assets">
          <div class="asset"><span>余额</span><b>{{ money(detail.balance) }}</b></div>
          <div class="asset"><span>积分</span><b>{{ detail.points }}</b></div>
          <div class="asset"><span>成长值</span><b>{{ detail.growth }}</b></div>
        </div>

        <el-tabs v-model="tab">
          <el-tab-pane label="基本信息" name="info">
            <div class="desc-grid">
              <div class="desc-item"><span class="desc-item__label">会员 ID</span><span class="desc-item__value mono">{{ detail.id }}</span></div>
              <div class="desc-item"><span class="desc-item__label">手机号</span><span class="desc-item__value">{{ detail.mobile }}</span></div>
              <div class="desc-item"><span class="desc-item__label">性别</span><span class="desc-item__value">{{ genderMap[detail.gender]?.label || '未知' }}</span></div>
              <div class="desc-item"><span class="desc-item__label">生日</span><span class="desc-item__value">{{ detail.birthday || '-' }}</span></div>
              <div class="desc-item"><span class="desc-item__label">最近登录</span><span class="desc-item__value">{{ formatTime(detail.lastLoginAt) }}</span></div>
              <div class="desc-item"><span class="desc-item__label">注册时间</span><span class="desc-item__value">{{ formatTime(detail.createdAt) }}</span></div>
            </div>
          </el-tab-pane>
          <el-tab-pane :label="`收货地址（${detail.addresses?.length || 0}）`" name="address">
            <div v-if="detail.addresses?.length" class="addresses">
              <div v-for="a in detail.addresses" :key="a.id" class="address">
                <div class="address__head">
                  <b>{{ a.receiverName || a.name }}</b>
                  <span class="mono">{{ a.receiverMobile || a.mobile }}</span>
                  <el-tag v-if="a.isDefault" size="small" type="danger" effect="plain">默认</el-tag>
                </div>
                <div class="address__text">{{ addrText(a) }}</div>
              </div>
            </div>
            <EmptyState v-else text="暂无收货地址" />
          </el-tab-pane>
          <el-tab-pane label="账户流水" name="logs">
            <el-radio-group v-model="logs.query.accountType" size="small" style="margin-bottom: 12px" @change="logs.search">
              <el-radio-button value="">全部</el-radio-button>
              <el-radio-button v-for="o in ACCOUNT_TYPE" :key="o.value" :value="o.value">{{ o.label }}</el-radio-button>
            </el-radio-group>
            <el-table v-loading="logs.loading.value" :data="logs.list.value" row-key="id" size="small">
              <el-table-column label="类型" width="80">
                <template #default="{ row }: { row: any }"><StatusTag :value="row.accountType" :options="ACCOUNT_TYPE" :dot="false" /></template>
              </el-table-column>
              <el-table-column label="变动" width="100" align="right">
                <template #default="{ row }: { row: any }">
                  <span :class="row.changeValue >= 0 ? 'text-success' : 'text-danger'">{{ row.changeValue > 0 ? '+' : '' }}{{ row.changeValue }}</span>
                </template>
              </el-table-column>
              <el-table-column label="变动后" prop="afterValue" width="90" align="right" />
              <el-table-column label="业务" min-width="120">
                <template #default="{ row }: { row: any }">
                  <div>{{ bizMap[row.bizType]?.label || row.bizType }}</div>
                  <div v-if="row.remark" class="text-secondary sub">{{ row.remark }}</div>
                </template>
              </el-table-column>
              <el-table-column label="时间" width="150">
                <template #default="{ row }: { row: any }"><span class="sub">{{ formatTime(row.createdAt, 'YYYY-MM-DD HH:mm') }}</span></template>
              </el-table-column>
              <template #empty><EmptyState text="暂无流水" :size="90" /></template>
            </el-table>
            <Pagination
              :page="logs.page.value"
              :size="logs.size.value"
              :total="logs.total.value"
              @page-change="logs.onPageChange"
              @size-change="logs.onSizeChange"
            />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <!-- 调账 -->
    <el-dialog v-model="adjustVisible" title="账户调整" width="480px" append-to-body>
      <div v-if="adjusting" class="adjust-target">
        <el-avatar :size="32" :src="adjusting.avatar || undefined">{{ (adjusting.nickname || '会').slice(0, 1) }}</el-avatar>
        <span>{{ adjusting.nickname || '未设置昵称' }}</span>
        <span class="text-secondary">{{ adjusting.mobile }}</span>
      </div>
      <el-form ref="adjustRef" :model="adjustForm" :rules="adjustRules" label-width="90px" @submit.prevent>
        <el-form-item label="调整账户">
          <el-radio-group v-model="adjustForm.accountType">
            <el-radio-button value="BALANCE">余额</el-radio-button>
            <el-radio-button value="POINTS">积分</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="调整方式">
          <el-radio-group v-model="adjustForm.direction">
            <el-radio :value="1">增加</el-radio>
            <el-radio :value="-1">扣减</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="调整数值" prop="amount">
          <el-input-number
            v-model="adjustForm.amount"
            :min="0"
            :precision="adjustForm.accountType === 'BALANCE' ? 2 : 0"
            controls-position="right"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="调整后">
          <span>
            {{ adjustForm.accountType === 'BALANCE' ? money(currentValue) : currentValue }}
            <el-icon style="margin: 0 6px; vertical-align: -2px"><Right /></el-icon>
            <b :class="afterValue < 0 ? 'text-danger' : 'text-primary'">{{ adjustForm.accountType === 'BALANCE' ? money(afterValue) : afterValue }}</b>
          </span>
        </el-form-item>
        <el-form-item label="调整原因" prop="remark">
          <el-input v-model="adjustForm.remark" type="textarea" :rows="2" maxlength="100" show-word-limit placeholder="如：客诉补偿、活动奖励" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjustSaving" @click="submitAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.sub {
  font-size: 12px;
  margin-top: 2px;
}
.member-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  &__name {
    color: var(--app-text);
    font-weight: 500;
  }
  &:hover .member-cell__name {
    color: var(--el-color-primary);
  }
}
.level-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
  color: var(--app-warning);
  background: var(--app-warning-soft);
}
.member-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.profile-head {
  display: flex;
  align-items: center;
  gap: 14px;
  &__main {
    flex: 1;
    min-width: 0;
  }
  &__name {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    font-size: 17px;
    font-weight: 600;
    color: var(--app-text);
  }
}
.assets {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.asset {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border-radius: var(--app-radius);
  background: var(--app-card-bg-muted);
  span {
    font-size: 12px;
    color: var(--app-text-secondary);
  }
  b {
    font-size: 18px;
    color: var(--app-text);
    font-variant-numeric: tabular-nums;
  }
}
.addresses {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.address {
  padding: 12px 14px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  &__head {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--app-text);
  }
  &__text {
    margin-top: 6px;
    font-size: 13px;
    color: var(--app-text-regular);
  }
}
.adjust-target {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 16px;
  border-radius: var(--app-radius);
  background: var(--app-card-bg-muted);
}
</style>
