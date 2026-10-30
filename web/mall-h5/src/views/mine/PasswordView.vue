<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { updatePassword } from '@/api/member'
import { useUserStore } from '@/stores/user'
import NavBar from '@/components/NavBar.vue'

const router = useRouter()
const user = useUserStore()
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)

const rules = {
  pwd: [{ pattern: /^.{6,20}$/, message: '密码长度 6-20 位' }],
  confirm: [{ validator: (v: string) => v === newPassword.value, message: '两次输入的密码不一致' }],
}

async function submit() {
  loading.value = true
  try {
    await updatePassword({ oldPassword: oldPassword.value, newPassword: newPassword.value })
    showSuccessToast('密码已修改，请重新登录')
    user.clear()
    router.replace({ name: 'login' })
  } catch {
    /* 已提示 */
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <NavBar title="修改密码" />
    <van-form class="pwd__form" @submit="submit">
      <van-cell-group inset>
        <van-field
          v-model="oldPassword"
          type="password"
          label="原密码"
          placeholder="请输入原密码"
          :rules="[{ required: true, message: '请输入原密码' }]"
        />
        <van-field v-model="newPassword" type="password" label="新密码" placeholder="6-20 位新密码" :rules="rules.pwd" />
        <van-field
          v-model="confirmPassword"
          type="password"
          label="确认密码"
          placeholder="再次输入新密码"
          :rules="rules.confirm"
        />
      </van-cell-group>
      <div class="pwd__btn">
        <van-button block round native-type="submit" class="btn-gradient" :loading="loading">确认修改</van-button>
      </div>
      <div class="pwd__forgot" @click="router.push({ name: 'forgot' })">忘记原密码？</div>
    </van-form>
  </div>
</template>

<style scoped lang="scss">
.pwd__form {
  padding-top: 12px;
}
.pwd__btn {
  padding: 24px $gap 12px;
}
.pwd__forgot {
  text-align: center;
  font-size: 13px;
  color: $text-3;
}
</style>
