<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { showLoadingToast, closeToast, showSuccessToast, showToast } from 'vant'
import type { UploaderFileListItem } from 'vant'
import dayjs from 'dayjs'
import type { ProfileForm } from '@/types'
import { updateProfile, upload } from '@/api/member'
import { useUserStore } from '@/stores/user'
import { GENDER_TEXT } from '@/utils/dict'
import NavBar from '@/components/NavBar.vue'

const user = useUserStore()
const form = ref<ProfileForm>({ nickname: '', avatar: '', gender: 0, birthday: '' })

onMounted(async () => {
  const p = user.profile || (await user.fetchProfile())
  if (p) form.value = { nickname: p.nickname, avatar: p.avatar, gender: p.gender ?? 0, birthday: p.birthday || '' }
})

/* ---------------- 头像 ---------------- */
async function onAvatar(file: UploaderFileListItem | UploaderFileListItem[]) {
  const f = Array.isArray(file) ? file[0] : file
  if (!f.file) return
  showLoadingToast({ message: '上传中...', forbidClick: true })
  try {
    const res = await upload(f.file, 'avatar')
    form.value.avatar = res.url
    await save(false)
    closeToast()
    showSuccessToast('头像已更新')
  } catch {
    closeToast()
  }
}

/* ---------------- 昵称 ---------------- */
const nickShow = ref(false)
const nickInput = ref('')
function editNick() {
  nickInput.value = form.value.nickname
  nickShow.value = true
}
async function saveNick() {
  const v = nickInput.value.trim()
  if (!v) {
    showToast('昵称不能为空')
    return false
  }
  form.value.nickname = v
  await save()
  return true
}

/* ---------------- 性别 ---------------- */
const genderShow = ref(false)
const genderActions = GENDER_TEXT.map((name, value) => ({ name, value }))
async function onGender(a: { name: string; value: number }) {
  form.value.gender = a.value
  await save()
}

/* ---------------- 生日 ---------------- */
const birthShow = ref(false)
const birthValue = ref<string[]>([])
const minDate = new Date(1920, 0, 1)
const maxDate = new Date()
function openBirth() {
  const d = form.value.birthday ? dayjs(form.value.birthday) : dayjs('1995-01-01')
  birthValue.value = [d.format('YYYY'), d.format('MM'), d.format('DD')]
  birthShow.value = true
}
async function onBirth({ selectedValues }: { selectedValues: string[] }) {
  form.value.birthday = selectedValues.join('-')
  birthShow.value = false
  await save()
}

async function save(toast = true) {
  try {
    await updateProfile(form.value)
    await user.fetchProfile()
    if (toast) showSuccessToast('已保存')
  } catch {
    /* 已提示 */
  }
}

const genderText = computed(() => GENDER_TEXT[form.value.gender ?? 0] || '保密')
</script>

<template>
  <div class="page">
    <NavBar title="个人资料" />
    <van-cell-group inset class="profile__group">
      <van-cell title="头像" is-link center class="profile__avatar-cell">
        <template #value>
          <van-uploader :after-read="onAvatar" accept="image/*" :max-size="5 * 1024 * 1024" :preview-image="false">
            <van-image :src="form.avatar" round width="52" height="52" fit="cover">
              <template #error><van-icon name="user-o" size="24" /></template>
              <template #loading><van-icon name="user-o" size="24" /></template>
            </van-image>
          </van-uploader>
        </template>
      </van-cell>
      <van-cell title="昵称" is-link :value="form.nickname || '未设置'" @click="editNick" />
      <van-cell title="手机号" :value="user.profile?.mobile" />
      <van-cell title="性别" is-link :value="genderText" @click="genderShow = true" />
      <van-cell title="生日" is-link :value="form.birthday || '未设置'" @click="openBirth" />
      <van-cell title="会员等级" :value="user.profile?.levelName || '普通会员'" />
    </van-cell-group>

    <van-dialog
      v-model:show="nickShow"
      title="修改昵称"
      show-cancel-button
      :before-close="(action: string) => (action === 'confirm' ? saveNick() : true)"
    >
      <van-field v-model="nickInput" placeholder="请输入昵称" maxlength="20" clearable class="profile__nick" />
    </van-dialog>
    <van-action-sheet
      v-model:show="genderShow"
      :actions="genderActions"
      cancel-text="取消"
      close-on-click-action
      @select="onGender"
    />
    <van-popup v-model:show="birthShow" position="bottom" round>
      <van-date-picker
        v-model="birthValue"
        title="选择生日"
        :min-date="minDate"
        :max-date="maxDate"
        @confirm="onBirth"
        @cancel="birthShow = false"
      />
    </van-popup>
  </div>
</template>

<style scoped lang="scss">
.profile__group {
  margin-top: 12px;
}
.profile__avatar-cell {
  :deep(.van-cell__value) {
    display: flex;
    justify-content: flex-end;
  }
  :deep(.van-image) {
    background: $bg;
  }
}
.profile__nick {
  margin: 12px 0;
}
</style>
