<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { toast } from '@/utils/message'
import ImageUpload from '@/components/ImageUpload.vue'

const router = useRouter()
const user = useUserStore()
const tab = ref('profile')

// ---------- 资料 ----------
const profileRef = ref<FormInstance>()
const profileSaving = ref(false)
const profile = reactive({ nickname: '', avatar: '', mobile: '', email: '' })
const profileRules: FormRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  mobile: [{ pattern: /^$|^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

function fill() {
  const i = user.info
  Object.assign(profile, { nickname: i?.nickname || '', avatar: i?.avatar || '', mobile: i?.mobile || '', email: i?.email || '' })
}
onMounted(fill)

async function saveProfile() {
  if (!(await profileRef.value?.validate().catch(() => false))) return
  profileSaving.value = true
  try {
    await authApi.updateProfile({ ...profile })
    await user.fetchInfo()
    fill()
    toast.success('资料已更新')
  } finally {
    profileSaving.value = false
  }
}

// ---------- 密码 ----------
const pwdRef = ref<FormInstance>()
const pwdSaving = ref(false)
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{6,32}$/, message: '6~32 位，需包含字母和数字', trigger: 'blur' },
    { validator: (_r, v: string, cb) => (v && v === pwd.oldPassword ? cb(new Error('新密码不能与当前密码相同')) : cb()), trigger: 'blur' },
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: (_r, v: string, cb) => (v === pwd.newPassword ? cb() : cb(new Error('两次输入不一致'))), trigger: 'blur' },
  ],
}

/** 密码强度：长度 + 字符类别 */
function strength(p: string) {
  let s = 0
  if (p.length >= 8) s++
  if (p.length >= 12) s++
  if (/[a-z]/.test(p) && /[A-Z]/.test(p)) s++
  if (/\d/.test(p)) s++
  if (/[^A-Za-z0-9]/.test(p)) s++
  return Math.min(3, Math.ceil(s / 1.7))
}

async function savePwd() {
  if (!(await pwdRef.value?.validate().catch(() => false))) return
  pwdSaving.value = true
  try {
    await authApi.updatePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    toast.success('密码已修改，请重新登录')
    await user.logout(false)
    router.replace('/login')
  } finally {
    pwdSaving.value = false
  }
}
</script>

<template>
  <div class="profile-page">
    <aside class="card profile-card">
      <div class="profile-card__bg" />
      <el-avatar :size="84" :src="user.info?.avatar || undefined" class="profile-card__avatar">{{ user.displayName.slice(0, 1) }}</el-avatar>
      <div class="profile-card__name">{{ user.displayName }}</div>
      <div class="text-secondary">@{{ user.info?.username }}</div>
      <div class="profile-card__roles">
        <el-tag v-for="r in user.info?.roles || []" :key="r" size="small" effect="plain">{{ r }}</el-tag>
      </div>
      <ul class="profile-card__meta">
        <li><el-icon><Iphone /></el-icon>{{ user.info?.mobile || '未绑定手机' }}</li>
        <li><el-icon><Message /></el-icon>{{ user.info?.email || '未绑定邮箱' }}</li>
        <li><el-icon><Key /></el-icon>{{ user.isSuper ? '全部权限' : `${user.info?.permissions?.length || 0} 项权限` }}</li>
      </ul>
    </aside>

    <section class="card profile-main">
      <el-tabs v-model="tab">
        <el-tab-pane label="基本资料" name="profile">
          <el-form ref="profileRef" :model="profile" :rules="profileRules" label-width="80px" class="profile-form">
            <el-form-item label="头像">
              <ImageUpload v-model="profile.avatar" biz-type="avatar" :size="88" tip="支持 JPG / PNG，≤ 2MB" :max-size="2" />
            </el-form-item>
            <el-form-item label="用户名">
              <el-input :model-value="user.info?.username" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="profile.nickname" maxlength="32" />
            </el-form-item>
            <el-form-item label="手机号" prop="mobile">
              <el-input v-model="profile.mobile" maxlength="11" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profile.email" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存资料</el-button>
              <el-button @click="fill">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="修改密码" name="password">
          <el-form ref="pwdRef" :model="pwd" :rules="pwdRules" label-width="90px" class="profile-form" @submit.prevent>
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="pwd.oldPassword" type="password" show-password autocomplete="current-password" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwd.newPassword" type="password" show-password autocomplete="new-password" />
              <div v-if="pwd.newPassword" class="strength" :class="`is-${strength(pwd.newPassword)}`">
                <i /><i /><i />
                <span>{{ ['弱', '弱', '中', '强'][strength(pwd.newPassword)] }}</span>
              </div>
            </el-form-item>
            <el-form-item label="确认密码" prop="confirm">
              <el-input v-model="pwd.confirm" type="password" show-password autocomplete="new-password" @keyup.enter="savePwd" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdSaving" @click="savePwd">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<style scoped lang="scss">
.profile-page {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: var(--app-gap);
  align-items: start;
}
.profile-card {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding-top: 56px;
  &__bg {
    position: absolute;
    inset: 0 0 auto;
    height: 96px;
    background: var(--app-gradient);
  }
  &__avatar {
    position: relative;
    border: 4px solid var(--app-card-bg);
    font-size: 30px;
    background: var(--el-color-primary);
    color: var(--el-color-white);
  }
  &__name {
    margin-top: 10px;
    font-size: 18px;
    font-weight: 600;
    color: var(--app-text);
  }
  &__roles {
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
    justify-content: center;
    margin-top: 10px;
  }
  &__meta {
    list-style: none;
    margin: 20px 0 0;
    padding: 16px 0 0;
    width: 100%;
    border-top: 1px solid var(--app-border);
    text-align: left;
    li {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 6px 4px;
      font-size: 13px;
      color: var(--app-text-regular);
    }
  }
}
.profile-form {
  max-width: 520px;
  padding-top: 8px;
}
.strength {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
  margin-top: 6px;
  font-size: 12px;
  i {
    flex: 1;
    max-width: 60px;
    height: 4px;
    border-radius: 2px;
    background: var(--app-border);
  }
  span {
    margin-left: 6px;
    color: var(--app-text-secondary);
  }
  &.is-0 i:nth-child(-n + 1),
  &.is-1 i:nth-child(-n + 1) {
    background: var(--app-danger);
  }
  &.is-2 i:nth-child(-n + 2) {
    background: var(--app-warning);
  }
  &.is-3 i {
    background: var(--app-success);
  }
}
@media (max-width: 900px) {
  .profile-page {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
