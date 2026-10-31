<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, shallowRef } from 'vue'
import type { FormInstance, FormRules, TreeInstance } from 'element-plus'
import { menuApi, roleApi } from '@/api/system'
import type { ID, Menu, Role, Switch } from '@/types'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { ENABLE_STATUS, MENU_TYPE, SUPER_ADMIN } from '@/utils/constants'
import { flattenTree } from '@/utils/format'
import { confirmDelete, toast } from '@/utils/message'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'

const user = useUserStore()
const canEdit = computed(() => user.hasPerm('system:role:edit'))

const { query, list, total, page, size, loading, load, search, onPageChange, onSizeChange } = useTable(roleApi.page, { keyword: '' })

// ---------------- 右侧权限树 ----------------
const menuTree = shallowRef<Menu[]>([])
const treeRef = ref<TreeInstance>()
const current = ref<Role | null>(null)
const permLoading = ref(false)
const permSaving = ref(false)
const expandAll = ref(true)
const treeKey = ref(0)
const isSuperRole = computed(() => current.value?.code === SUPER_ADMIN)

onMounted(async () => {
  try {
    menuTree.value = (await menuApi.tree()) || []
  } catch {
    /* 忽略 */
  }
})

const leafIds = computed(() => new Set(flattenTree(menuTree.value).filter((m) => !m.children?.length).map((m) => m.id)))

async function selectRole(row: Role | null) {
  current.value = row
  if (!row) return
  permLoading.value = true
  try {
    const ids = (await roleApi.menus(row.id)) || []
    await nextTick()
    // 只勾选叶子节点，父节点由树自动计算半选，避免父节点全选导致新增子菜单被误授权
    treeRef.value?.setCheckedKeys(ids.filter((id) => leafIds.value.has(id)))
  } finally {
    permLoading.value = false
  }
}

function checkAll(v: boolean) {
  treeRef.value?.setCheckedKeys(v ? Array.from(leafIds.value) : [])
}

function toggleExpand() {
  expandAll.value = !expandAll.value
  treeKey.value++
  if (current.value) selectRole(current.value)
}

async function savePerms() {
  if (!current.value || !treeRef.value) return
  const ids = [...treeRef.value.getCheckedKeys(false), ...treeRef.value.getHalfCheckedKeys()] as ID[]
  permSaving.value = true
  try {
    await roleApi.saveMenus(current.value.id, ids)
    toast.success(`已保存「${current.value.name}」的权限`)
  } finally {
    permSaving.value = false
  }
}

// ---------------- 角色表单 ----------------
const visible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<ID>('')
const form = reactive({ name: '', code: '', sort: 0, status: 1 as Switch, remark: '' })
const rules: FormRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]{1,63}$/, message: '小写字母开头，仅含小写字母、数字、下划线', trigger: 'blur' },
  ],
}

function open(row?: Role) {
  editingId.value = row?.id || ''
  Object.assign(form, { name: row?.name || '', code: row?.code || '', sort: row?.sort ?? 0, status: row?.status ?? 1, remark: row?.remark || '' })
  visible.value = true
  setTimeout(() => formRef.value?.clearValidate())
}

async function submit() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  saving.value = true
  try {
    if (editingId.value) await roleApi.update(editingId.value, { ...form })
    else await roleApi.create({ ...form })
    toast.success('保存成功')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Role) {
  if (await confirmDelete(row.name, () => roleApi.remove(row.id))) {
    if (current.value?.id === row.id) current.value = null
    load()
  }
}

const typeMap = Object.fromEntries(MENU_TYPE.map((t) => [t.value, t]))
</script>

<template>
  <div class="role-page">
    <div class="card role-list">
      <div class="toolbar">
        <div class="toolbar__left">
          <el-input v-model="query.keyword" placeholder="角色名称 / 编码" clearable :prefix-icon="'Search'" style="width: 220px" @keyup.enter="search" @clear="search" />
          <el-button type="primary" :icon="'Search'" @click="search">搜索</el-button>
        </div>
        <div class="toolbar__right">
          <el-button v-perm="'system:role:edit'" type="primary" :icon="'Plus'" @click="open()">新增角色</el-button>
        </div>
      </div>
      <el-table v-loading="loading" :data="list" row-key="id" highlight-current-row @current-change="selectRole">
        <el-table-column label="角色" min-width="140">
          <template #default="{ row }: { row: any }">
            <div class="role-name">{{ row.name }}</div>
            <div class="mono text-secondary sub">{{ row.code }}</div>
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="sort" width="70" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }: { row: any }"><StatusTag :value="row.status" :options="ENABLE_STATUS" /></template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="130" show-overflow-tooltip />
        <el-table-column v-if="canEdit" label="操作" width="120" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button link type="primary" @click.stop="open(row)">编辑</el-button>
            <el-button link type="danger" :disabled="row.code === SUPER_ADMIN" @click.stop="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState v-if="!loading" text="暂无角色" /><div v-else class="table-placeholder" /></template>
      </el-table>
      <Pagination :page="page" :size="size" :total="total" @page-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <div class="card perm-panel">
      <div class="perm-panel__head">
        <div>
          <div class="card-title" style="margin: 0"><span class="card-title__text">菜单权限</span></div>
          <div class="text-secondary sub">{{ current ? `当前角色：${current.name}` : '在左侧选择一个角色' }}</div>
        </div>
        <el-button v-if="current && canEdit && !isSuperRole" type="primary" :loading="permSaving" :icon="'Check'" @click="savePerms">保存权限</el-button>
      </div>
      <template v-if="current">
        <el-alert v-if="isSuperRole" type="success" :closable="false" show-icon title="超级管理员默认拥有全部权限，无需配置" />
        <template v-else>
          <div class="perm-panel__tools">
            <el-button size="small" @click="checkAll(true)">全选</el-button>
            <el-button size="small" @click="checkAll(false)">清空</el-button>
            <el-button size="small" @click="toggleExpand">{{ expandAll ? '折叠' : '展开' }}</el-button>
          </div>
          <el-scrollbar class="perm-panel__tree">
            <el-tree
              :key="treeKey"
              ref="treeRef"
              v-loading="permLoading"
              :data="menuTree"
              node-key="id"
              show-checkbox
              :default-expand-all="expandAll"
              :props="{ label: 'name', children: 'children', disabled: () => !canEdit }"
            >
              <template #default="{ data }">
                <span class="perm-node">
                  <el-icon v-if="data.icon"><component :is="data.icon" /></el-icon>
                  <span>{{ data.name }}</span>
                  <el-tag size="small" :type="data.type === 'BUTTON' ? 'info' : data.type === 'DIR' ? 'warning' : 'primary'" effect="plain">
                    {{ typeMap[data.type]?.label }}
                  </el-tag>
                  <span v-if="data.permission" class="mono text-secondary perm-code">{{ data.permission }}</span>
                </span>
              </template>
            </el-tree>
          </el-scrollbar>
        </template>
      </template>
      <EmptyState v-else text="请选择角色后配置权限" />
    </div>

    <el-dialog v-model="visible" :title="editingId ? '编辑角色' : '新增角色'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" maxlength="32" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" :disabled="!!editingId" placeholder="如 operator" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio v-for="o in ENABLE_STATUS" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.role-page {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: var(--app-gap);
  align-items: start;
}
.role-name {
  font-weight: 500;
  color: var(--app-text);
}
.sub {
  font-size: 12px;
  margin-top: 2px;
}
.perm-panel {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }
  &__tools {
    display: flex;
    gap: 4px;
  }
  &__tree {
    max-height: calc(100vh - 300px);
    border: 1px solid var(--app-border);
    border-radius: var(--app-radius);
    padding: 8px 4px;
  }
}
.perm-node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.perm-code {
  font-size: 11px;
}
@media (max-width: 1100px) {
  .role-page {
    grid-template-columns: minmax(0, 1fr);
  }
  .perm-panel {
    position: static;
  }
}
</style>
