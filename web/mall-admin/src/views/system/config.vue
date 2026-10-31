<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox, type FormInstance } from 'element-plus'
import { configApi, infraApi } from '@/api/system'
import type { SysConfig } from '@/types'
import { useUserStore } from '@/stores/user'
import { toast } from '@/utils/message'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const user = useUserStore()
const canEdit = computed(() => user.hasPerm('system:config:edit'))

const MASK = '******'

const list = ref<SysConfig[]>([])
/** 待提交的值（敏感项未修改时保持掩码 ******） */
const values = ref<Record<string, string>>({})
const original = ref<Record<string, string>>({})
/** 敏感项输入框内容：空 = 不修改 */
const secretInputs = reactive<Record<string, string>>({})
const loading = ref(true)
const saving = ref(false)

// ---------------------------------------------------------------------------
// 分组：按键前缀固定分为 8 个标签页（契约 §1.2），未知前缀归入「其他」
// ---------------------------------------------------------------------------
interface GroupDef {
  key: string
  label: string
  icon: string
  desc: string
}
const GROUPS: GroupDef[] = [
  { key: 'mall', label: '基础', icon: 'Shop', desc: '商城名称、客服电话等基础信息，商城端即时生效。' },
  { key: 'trade', label: '交易', icon: 'Tickets', desc: '运费、支付超时、自动收货、售后期限与商家退货地址。商品使用的运费模板优先于这里的基础运费。' },
  { key: 'member', label: '会员', icon: 'User', desc: '积分、成长值与注册赠送规则。' },
  { key: 'pay', label: '支付', icon: 'Wallet', desc: '微信支付 / 支付宝渠道参数，保存后立即生效，无需重启。密钥类参数只显示是否已设置。' },
  { key: 'wechat', label: '微信', icon: 'ChatDotRound', desc: '公众号网页授权登录（微信内一键登录）。' },
  { key: 'sms', label: '短信', icon: 'Message', desc: '验证码与通知短信渠道。生产环境请选择阿里云或腾讯云，保存后可「测试发送」验证。' },
  { key: 'storage', label: '存储', icon: 'Files', desc: '上传文件的存储位置。切换只影响之后的新上传，保存后可「测试连接」验证。' },
  { key: 'logistics', label: '物流', icon: 'Van', desc: '物流轨迹查询服务，开通后订单详情与商城端可查看物流。' },
]
const OTHER: GroupDef = { key: 'other', label: '其他', icon: 'Setting', desc: '' }

const groupOf = (key: string) => {
  const p = key.split('.')[0]
  return GROUPS.some((g) => g.key === p) ? p : 'other'
}

/** 组内小节：按数组顺序展示，键归入第一个匹配的小节 */
const SECTIONS: Record<string, { match: (k: string) => boolean; title: string }[]> = {
  trade: [
    { match: (k) => !k.startsWith('trade.return_'), title: '交易规则' },
    { match: (k) => k.startsWith('trade.return_'), title: '商家退货地址（退货退款审核通过后展示给买家）' },
  ],
  pay: [
    { match: (k) => !k.startsWith('pay.wechat.') && !k.startsWith('pay.alipay.'), title: '通用' },
    { match: (k) => k.startsWith('pay.wechat.'), title: '微信支付' },
    { match: (k) => k.startsWith('pay.alipay.'), title: '支付宝' },
  ],
  sms: [
    { match: (k) => !k.startsWith('sms.template.'), title: '服务商' },
    { match: (k) => k.startsWith('sms.template.'), title: '短信模板' },
  ],
  storage: [
    { match: (k) => k === 'storage.provider', title: '存储方式' },
    { match: () => true, title: 'S3 对象存储（OSS / COS / MinIO / 七牛）' },
  ],
}

/** 下拉类参数 */
const SELECTS: Record<string, { value: string; label: string }[]> = {
  'sms.provider': [
    { value: 'LOG', label: 'LOG（仅开发 / 演示，不真实发送）' },
    { value: 'ALIYUN', label: '阿里云短信' },
    { value: 'TENCENT', label: '腾讯云短信' },
  ],
  'storage.provider': [
    { value: 'LOCAL', label: 'LOCAL 本地磁盘' },
    { value: 'S3', label: 'S3 对象存储' },
  ],
  'logistics.provider': [
    { value: 'NONE', label: 'NONE 未开通' },
    { value: 'KUAIDI100', label: '快递100' },
  ],
}

/** 依赖服务商的参数：未选用对应服务商时隐藏，减少干扰 */
function visible(c: SysConfig): boolean {
  const k = c.configKey
  const v = (key: string) => values.value[key]
  if (k.startsWith('storage.') && k !== 'storage.provider') return v('storage.provider') === 'S3'
  if (k.startsWith('logistics.') && k !== 'logistics.provider') return v('logistics.provider') !== 'NONE'
  if (k === 'sms.tencent.sdk_app_id') return v('sms.provider') === 'TENCENT'
  if (k.startsWith('sms.') && k !== 'sms.provider' && !k.startsWith('sms.template.')) return v('sms.provider') !== 'LOG'
  return true
}

const isBool = (c: SysConfig) => ['true', 'false'].includes(String(original.value[c.configKey]).toLowerCase())
const isSelect = (c: SysConfig) => !!SELECTS[c.configKey]
/** 私钥 / 公钥全文用多行文本框 */
const isMultiline = (c: SysConfig) => /(private_key|public_key)$/.test(c.configKey)
const isNumber = (c: SysConfig) =>
  /(amount|threshold|minutes|days|per_yuan|balance)/.test(c.configKey) && /^-?\d+(\.\d+)?$/.test(String(original.value[c.configKey]))
const isMoney = (c: SysConfig) => /(amount|threshold|balance)/.test(c.configKey)
const secretSet = (c: SysConfig) => original.value[c.configKey] === MASK

const groups = computed(() => {
  const map = new Map<string, SysConfig[]>()
  list.value.forEach((c) => {
    const g = groupOf(c.configKey)
    if (!map.has(g)) map.set(g, [])
    map.get(g)!.push(c)
  })
  return [...GROUPS, OTHER]
    .filter((g) => map.has(g.key))
    .map((g) => {
      const items = map.get(g.key)!
      const defs = SECTIONS[g.key]
      if (!defs) return { ...g, sections: [{ title: '', items }] }
      const buckets = defs.map((d) => ({ title: d.title, items: [] as SysConfig[] }))
      items.forEach((c) => buckets[defs.findIndex((d) => d.match(c.configKey))].items.push(c))
      return { ...g, sections: buckets.filter((b) => b.items.length) }
    })
})

const hasReturnKeys = computed(() => list.value.some((c) => c.configKey.startsWith('trade.return_')))

const activeGroup = ref(typeof route.query.group === 'string' ? route.query.group : '')

const dirtyKeys = computed(() => Object.keys(values.value).filter((k) => values.value[k] !== original.value[k]))
const dirtyIn = (g: string) => dirtyKeys.value.filter((k) => groupOf(k) === g).length

async function load() {
  loading.value = true
  try {
    list.value = (await configApi.list()) || []
    const v: Record<string, string> = {}
    list.value.forEach((c) => (v[c.configKey] = c.configValue ?? ''))
    values.value = { ...v }
    original.value = { ...v }
    Object.keys(secretInputs).forEach((k) => delete secretInputs[k])
    if (!groups.value.some((g) => g.key === activeGroup.value)) activeGroup.value = groups.value[0]?.key || ''
  } finally {
    loading.value = false
  }
}
onMounted(load)

function onSecretInput(c: SysConfig, v: string) {
  secretInputs[c.configKey] = v
  // 留空 = 不修改：已设置的提交掩码（即不提交），未设置的保持空串
  values.value[c.configKey] = v ? v : original.value[c.configKey]
}

function clearSecret(c: SysConfig) {
  secretInputs[c.configKey] = ''
  values.value[c.configKey] = ''
}

function validate(): string | null {
  const changed = (k: string) => values.value[k] && values.value[k] !== original.value[k]
  if (changed('pay.notify_base_url') && !/^https:\/\//i.test(values.value['pay.notify_base_url'])) return '支付回调根地址必须以 https:// 开头'
  if (changed('trade.return_receiver_mobile') && !/^[\d\-+ ]{7,20}$/.test(values.value['trade.return_receiver_mobile'])) {
    return '退货联系电话格式不正确'
  }
  return null
}

async function save() {
  if (!dirtyKeys.value.length) return toast.warning('没有修改')
  const err = validate()
  if (err) return toast.warning(err)
  saving.value = true
  try {
    const n = dirtyKeys.value.length
    await configApi.batch(dirtyKeys.value.map((k) => ({ configKey: k, configValue: values.value[k] })))
    toast.success(`已保存 ${n} 项参数，即时生效`)
    await load()
  } finally {
    saving.value = false
  }
}

function resetChanges() {
  values.value = { ...original.value }
  Object.keys(secretInputs).forEach((k) => delete secretInputs[k])
}

// ---------------------------------------------------------------------------
// 测试：短信发送、存储连接（服务端使用「已保存」的配置）
// ---------------------------------------------------------------------------
const smsDialog = ref(false)
const smsForm = reactive({ mobile: '' })
const smsFormRef = ref<FormInstance>()
const testing = ref(false)

function needSaveFirst(group: string) {
  if (!dirtyIn(group)) return false
  toast.warning('测试使用已保存的配置，请先保存本组的修改')
  return true
}

function openSmsTest() {
  if (needSaveFirst('sms')) return
  smsDialog.value = true
}

async function sendSmsTest() {
  if (!(await smsFormRef.value?.validate().catch(() => false))) return
  testing.value = true
  try {
    const res = await infraApi.smsTest(smsForm.mobile.trim())
    smsDialog.value = false
    toast.success(`测试短信已通过 ${res?.provider || values.value['sms.provider']} 发出，请留意手机`)
  } finally {
    testing.value = false
  }
}

const escapeHtml = (s: string) => s.replace(/[&<>"']/g, (ch) => `&#${ch.charCodeAt(0)};`)

async function testStorage() {
  if (needSaveFirst('storage')) return
  testing.value = true
  try {
    const res = await infraApi.storageTest()
    const lines = [
      `服务商：${res?.provider || '-'}`,
      res?.elapsedMs != null ? `耗时：${res.elapsedMs} ms` : '',
      res?.sampleUrl ? `探测文件：${res.sampleUrl}` : '',
      res?.message || '',
    ].filter(Boolean)
    ElMessageBox.alert(lines.map(escapeHtml).join('<br>'), '存储连接正常', {
      type: 'success',
      dangerouslyUseHTMLString: true,
      confirmButtonText: '知道了',
    })
  } finally {
    testing.value = false
  }
}
</script>

<template>
  <div v-loading="loading" class="page">
    <div class="card config">
      <el-tabs v-if="groups.length" v-model="activeGroup" tab-position="left" class="config__tabs">
        <el-tab-pane v-for="g in groups" :key="g.key" :name="g.key">
          <template #label>
            <span class="config__tab">
              <el-icon><component :is="g.icon" /></el-icon>{{ g.label }}
              <i v-if="dirtyIn(g.key)" class="config__dirty" title="有未保存的修改" />
            </span>
          </template>
          <div class="config__head">
            <div>
              <div class="card-title" style="margin-bottom: 6px"><span class="card-title__text">{{ g.label }}设置</span></div>
              <p v-if="g.desc" class="config__desc">{{ g.desc }}</p>
            </div>
            <div class="config__actions">
              <el-button v-if="g.key === 'sms'" v-perm="'system:config:edit'" :icon="'Message'" @click="openSmsTest">测试发送</el-button>
              <el-button v-if="g.key === 'storage'" v-perm="'system:config:edit'" :icon="'Connection'" :loading="testing" @click="testStorage">
                测试连接
              </el-button>
            </div>
          </div>

          <el-form label-position="top" :disabled="!canEdit" class="config__form" @submit.prevent>
            <el-alert
              v-if="g.key === 'trade' && !hasReturnKeys"
              type="info"
              :closable="false"
              show-icon
              class="config__notice"
              title="当前服务端尚未提供退货地址参数（trade.return_*），升级后会显示在这里。"
            />
            <template v-for="sec in g.sections" :key="sec.title">
              <div v-if="sec.title && g.sections.length > 1" class="config__section">{{ sec.title }}</div>
              <template v-for="c in sec.items" :key="c.configKey">
                <el-form-item v-if="visible(c)" class="config__item">
                  <template #label>
                    <span class="config__label">
                      {{ c.name || c.configKey }}
                      <template v-if="c.sensitive">
                        <el-tag v-if="values[c.configKey] === MASK" size="small" type="success" effect="plain">已设置</el-tag>
                        <el-tag v-else-if="!values[c.configKey]" size="small" type="info" effect="plain">未设置</el-tag>
                      </template>
                      <el-tag v-if="values[c.configKey] !== original[c.configKey]" size="small" type="warning" effect="plain">已修改</el-tag>
                    </span>
                  </template>

                  <!-- 敏感参数：不回显明文，留空表示不修改 -->
                  <div v-if="c.sensitive" class="config__secret">
                    <el-input
                      :model-value="secretInputs[c.configKey] || ''"
                      :type="isMultiline(c) ? 'textarea' : 'password'"
                      :rows="isMultiline(c) ? 5 : undefined"
                      :show-password="!isMultiline(c)"
                      :placeholder="secretSet(c) ? '已设置，留空则保持不变；输入新值将覆盖' : '未设置，请输入'"
                      autocomplete="new-password"
                      :class="isMultiline(c) ? 'mono-input' : 'config__input'"
                      @update:model-value="(v: string) => onSecretInput(c, v)"
                    />
                    <el-button v-if="secretSet(c) && values[c.configKey] !== ''" link type="danger" @click="clearSecret(c)">清空</el-button>
                  </div>
                  <el-switch
                    v-else-if="isBool(c)"
                    :model-value="values[c.configKey] === 'true'"
                    inline-prompt
                    active-text="开"
                    inactive-text="关"
                    @change="(v: string | number | boolean) => (values[c.configKey] = v ? 'true' : 'false')"
                  />
                  <el-select v-else-if="isSelect(c)" v-model="values[c.configKey]" class="config__input">
                    <el-option v-for="o in SELECTS[c.configKey]" :key="o.value" :label="o.label" :value="o.value" />
                  </el-select>
                  <el-input-number
                    v-else-if="isNumber(c)"
                    :model-value="Number(values[c.configKey])"
                    :min="0"
                    :precision="isMoney(c) ? 2 : 0"
                    controls-position="right"
                    style="width: 240px"
                    @change="(v: number | undefined) => (values[c.configKey] = isMoney(c) ? Number(v ?? 0).toFixed(2) : String(v ?? 0))"
                  />
                  <el-input v-else-if="isMultiline(c)" v-model="values[c.configKey]" type="textarea" :rows="5" class="mono-input" />
                  <el-input
                    v-else-if="c.configKey === 'trade.return_address'"
                    v-model="values[c.configKey]"
                    type="textarea"
                    :rows="2"
                    maxlength="200"
                    show-word-limit
                    placeholder="省 市 区 详细地址"
                    class="config__input"
                  />
                  <el-input v-else v-model="values[c.configKey]" class="config__input" />
                  <div class="form-tip config__tip">
                    <template v-if="c.remark">{{ c.remark }}</template>
                    <span class="config__key mono">{{ c.configKey }}</span>
                  </div>
                </el-form-item>
              </template>
            </template>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <EmptyState v-else-if="!loading" text="暂无系统参数" />
    </div>

    <div v-if="canEdit && groups.length" class="card footer-bar">
      <span class="text-secondary">{{ dirtyKeys.length ? `${dirtyKeys.length} 项参数待保存` : '修改后点击保存，所有分组一起提交，保存后即时生效' }}</span>
      <div>
        <el-button :disabled="!dirtyKeys.length" @click="resetChanges">撤销修改</el-button>
        <el-button type="primary" :loading="saving" :disabled="!dirtyKeys.length" :icon="'Check'" @click="save">保存全部</el-button>
      </div>
    </div>

    <el-dialog v-model="smsDialog" title="测试发送短信" width="440px" append-to-body>
      <el-alert type="info" :closable="false" show-icon title="使用已保存的配置与验证码模板发送一条随机验证码（不可用于登录）" style="margin-bottom: 16px" />
      <el-form ref="smsFormRef" :model="smsForm" label-width="70px" @submit.prevent>
        <el-form-item
          label="手机号"
          prop="mobile"
          :rules="[
            { required: true, message: '请输入接收手机号', trigger: 'blur' },
            { pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' },
          ]"
        >
          <el-input v-model="smsForm.mobile" maxlength="11" placeholder="接收测试短信的手机号" @keyup.enter="sendSmsTest" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="smsDialog = false">取消</el-button>
        <el-button type="primary" :loading="testing" @click="sendSmsTest">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.config {
  &__tabs {
    min-height: 420px;
    :deep(.el-tabs__header.is-left) {
      margin-right: 24px;
    }
    :deep(.el-tabs__item) {
      justify-content: flex-start;
      min-width: 120px;
    }
  }
  &__tab {
    display: inline-flex;
    align-items: center;
    gap: 8px;
  }
  &__dirty {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--app-warning);
  }
  &__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    max-width: 760px;
    margin-bottom: 8px;
  }
  &__desc {
    margin: 0;
    font-size: 13px;
    line-height: 1.6;
    color: var(--app-text-secondary);
  }
  &__actions {
    flex: none;
    display: flex;
    gap: 8px;
  }
  &__form {
    max-width: 760px;
  }
  &__notice {
    margin: 8px 0 4px;
  }
  &__section {
    margin: 20px 0 14px;
    padding-bottom: 8px;
    font-size: 13px;
    font-weight: 600;
    color: var(--app-text);
    border-bottom: 1px solid var(--app-border);
  }
  &__item {
    margin-bottom: 20px;
    // 输入控件与说明上下排列：说明统一在输入框下方
    :deep(.el-form-item__content) {
      flex-direction: column;
      align-items: flex-start;
    }
  }
  &__input {
    width: 480px;
    max-width: 100%;
  }
  &__secret {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    width: 100%;
  }
  &__tip {
    width: 100%;
  }
  &__key {
    margin-left: 8px;
    font-size: 11px;
    color: var(--app-text-placeholder);
  }
  &__label {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;
  }
}
.mono-input {
  width: 100%;
  :deep(textarea) {
    font-family: 'JetBrains Mono', Consolas, Menlo, monospace;
    font-size: 12px;
  }
}
.footer-bar {
  position: sticky;
  bottom: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 20px;
  box-shadow: var(--app-shadow-pop);
}
@media (max-width: 768px) {
  .config__tabs {
    :deep(.el-tabs__header.is-left) {
      float: none;
      margin-right: 0;
    }
  }
  .config__head {
    flex-direction: column;
  }
}
</style>
