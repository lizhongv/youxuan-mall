<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast } from 'vant'
import type { AddressEditInfo } from 'vant'
import { areaList } from '@vant/area-data'
import type { AddressForm } from '@/types'
import { createAddress, deleteAddress, getAddressList, updateAddress } from '@/api/member'
import NavBar from '@/components/NavBar.vue'

const route = useRoute()
const router = useRouter()
const id = (route.query.id as string) || ''
const info = ref<Partial<AddressEditInfo>>({})
const loading = ref(!!id)
const saving = ref(false)

/**
 * 后端只存省市区名称，回显时按名称反查区县编码；
 * 先匹配区县名，再用市名校验前 4 位，避免同名区县误匹配。
 */
function findAreaCode(province: string, city: string, district: string): string {
  const counties = areaList.county_list as Record<string, string>
  const cities = areaList.city_list as Record<string, string>
  const provinces = areaList.province_list as Record<string, string>
  for (const [code, name] of Object.entries(counties)) {
    if (name !== district) continue
    const cityName = cities[code.slice(0, 4) + '00']
    const provName = provinces[code.slice(0, 2) + '0000']
    if ((!city || cityName === city) && (!province || provName === province)) return code
  }
  for (const [code, name] of Object.entries(cities)) {
    if (name === city) return code
  }
  return ''
}

onMounted(async () => {
  if (!id) return
  try {
    // 契约未提供单条查询，从列表中取
    const list = await getAddressList()
    const a = list.find((i) => i.id === id)
    if (a) {
      info.value = {
        name: a.name,
        tel: a.mobile,
        province: a.province,
        city: a.city,
        county: a.district,
        addressDetail: a.detail,
        areaCode: findAreaCode(a.province, a.city, a.district),
        isDefault: !!a.isDefault,
      }
    }
  } finally {
    loading.value = false
  }
})

async function onSave(content: AddressEditInfo) {
  const form: AddressForm = {
    name: content.name,
    mobile: content.tel,
    province: content.province,
    city: content.city,
    district: content.county,
    detail: content.addressDetail,
    isDefault: content.isDefault ? 1 : 0,
  }
  saving.value = true
  try {
    if (id) await updateAddress(id, form)
    else await createAddress(form)
    showSuccessToast('保存成功')
    router.back()
  } catch {
    /* 已提示 */
  } finally {
    saving.value = false
  }
}

async function onDelete() {
  try {
    await showConfirmDialog({ title: '删除地址', message: '确定删除该收货地址吗？' })
    await deleteAddress(id)
    showSuccessToast('已删除')
    router.back()
  } catch {
    /* 取消或已提示 */
  }
}

const telValidator = (v: string) => /^1\d{10}$/.test(v)
</script>

<template>
  <div class="page">
    <NavBar :title="id ? '编辑收货地址' : '新增收货地址'" />
    <van-loading v-if="loading" class="ae__loading" vertical>加载中...</van-loading>
    <van-address-edit
      v-else
      :area-list="areaList"
      :address-info="info"
      :show-delete="!!id"
      :is-saving="saving"
      show-set-default
      :tel-validator="telValidator"
      tel-maxlength="11"
      :area-columns-placeholder="['请选择', '请选择', '请选择']"
      save-button-text="保存"
      class="ae"
      @save="onSave"
      @delete="onDelete"
    />
  </div>
</template>

<style scoped lang="scss">
.ae__loading {
  padding: 60px 0;
}
.ae {
  :deep(.van-address-edit__buttons .van-button--danger) {
    background: $gradient;
    border: none;
  }
}
</style>
