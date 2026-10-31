<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import type { Address } from '@/types'
import { deleteAddress, getAddressList, setDefaultAddress } from '@/api/member'
import { useAppStore } from '@/stores/app'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { errorMessage } from '@/api/request'
import LoadState from '@/components/LoadState.vue'

const route = useRoute()
const router = useRouter()
const app = useAppStore()
/** 选择模式：从确认订单页进入，点击地址后回传 */
const selectMode = route.query.select === '1'
const currentId = (route.query.id as string) || ''

const list = ref<Address[]>([])
const loading = ref(true)
const loadError = ref('')

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    list.value = (await getAddressList()) || []
  } catch (e) {
    loadError.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}
onMounted(load)

function pick(a: Address) {
  if (!selectMode) return
  app.selectedAddressId = a.id
  router.back()
}

async function makeDefault(a: Address) {
  if (a.isDefault) return
  try {
    await setDefaultAddress(a.id)
    list.value.forEach((i) => (i.isDefault = i.id === a.id ? 1 : 0))
    showToast('已设为默认')
  } catch {
    /* 已提示 */
  }
}

async function remove(a: Address) {
  try {
    await showConfirmDialog({ title: '删除地址', message: '确定删除该收货地址吗？' })
    await deleteAddress(a.id)
    list.value = list.value.filter((i) => i.id !== a.id)
    if (app.selectedAddressId === a.id) app.selectedAddressId = ''
    showToast('已删除')
  } catch {
    /* 取消或已提示 */
  }
}

function edit(a?: Address) {
  // 选择模式下新增地址：保存后直接选中并返回确认订单页
  router.push({ name: 'address-edit', query: a ? { id: a.id } : selectMode ? { select: '1' } : {} })
}
</script>

<template>
  <div class="page addr">
    <NavBar :title="selectMode ? '选择收货地址' : '收货地址'" />
    <van-loading v-if="loading" class="addr__loading" vertical>加载中...</van-loading>
    <LoadState v-else-if="loadError" :error="loadError" @retry="load" />
    <EmptyState v-else-if="!list.length" description="还没有收货地址" />
    <div v-else class="addr__list">
      <van-swipe-cell v-for="a in list" :key="a.id" class="addr__cell">
        <div class="addr-item" :class="{ 'is-current': selectMode && a.id === currentId }" @click="pick(a)">
          <div class="addr-item__avatar">{{ a.name.slice(0, 1) }}</div>
          <div class="addr-item__body">
            <div class="addr-item__user">
              <span class="ellipsis">{{ a.name }}</span>
              <span class="addr-item__mobile">{{ a.mobile }}</span>
              <span v-if="a.isDefault" class="addr-item__tag">默认</span>
            </div>
            <div class="addr-item__detail ellipsis-2">{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detail }}</div>
          </div>
          <van-icon v-if="selectMode && a.id === currentId" name="success" class="addr-item__check" />
          <van-icon name="edit" class="addr-item__edit" @click.stop="edit(a)" />
        </div>
        <template #right>
          <div class="addr__actions">
            <van-button square class="addr__btn" :disabled="!!a.isDefault" @click="makeDefault(a)">设为默认</van-button>
            <van-button square type="danger" class="addr__btn" @click="remove(a)">删除</van-button>
          </div>
        </template>
      </van-swipe-cell>
      <div class="addr__tip">左滑可设为默认或删除</div>
    </div>
    <div class="addr__space" />
    <div class="addr__bar">
      <van-button round block class="btn-gradient" icon="plus" @click="edit()">新增收货地址</van-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.addr {
  &__loading {
    padding: 60px 0;
  }
  &__list {
    padding-top: 10px;
  }
  &__cell {
    margin: 0 $gap 10px;
    border-radius: $radius;
    overflow: hidden;
  }
  &__actions {
    display: flex;
    height: 100%;
  }
  &__btn {
    height: 100%;
  }
  &__tip {
    text-align: center;
    font-size: 12px;
    color: $text-3;
  }
  &__space {
    height: calc(70px + env(safe-area-inset-bottom));
  }
  &__bar {
    @include fixed-bottom;
    padding: 8px $gap calc(8px + env(safe-area-inset-bottom));
  }
}
.addr-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 12px;
  background: $card;
  &.is-current {
    box-shadow: inset 0 0 0 1px $primary;
    border-radius: $radius;
  }
  &__avatar {
    flex-shrink: 0;
    width: 36px;
    height: 36px;
    line-height: 36px;
    text-align: center;
    border-radius: 50%;
    background: $primary-light;
    color: $primary;
    font-weight: 600;
  }
  &__body {
    flex: 1;
    min-width: 0;
  }
  &__user {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 600;
  }
  &__mobile {
    flex-shrink: 0;
    font-size: 13px;
    font-weight: normal;
    color: $text-2;
  }
  &__tag {
    flex-shrink: 0;
    padding: 0 4px;
    font-size: 10px;
    line-height: 16px;
    font-weight: normal;
    color: #fff;
    background: $primary;
    border-radius: 3px;
  }
  &__detail {
    margin-top: 4px;
    font-size: 13px;
    color: $text-2;
    line-height: 18px;
  }
  &__check {
    color: $primary;
    font-size: 18px;
  }
  &__edit {
    flex-shrink: 0;
    padding: 4px;
    font-size: 18px;
    color: $text-3;
  }
}
</style>
