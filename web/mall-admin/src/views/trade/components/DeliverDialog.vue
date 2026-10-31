<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { orderApi } from '@/api/trade'
import type { Order } from '@/types'
import { useDict } from '@/composables/useDict'
import { fullAddress } from '@/utils/format'
import { toast } from '@/utils/message'

/** 发货：快递公司取字典 express_company，提交 value（快递100 编码，后端也兼容 label），手输的公司名原样提交 */
const props = defineProps<{ order: Pick<Order, 'id' | 'orderNo' | 'receiverName' | 'receiverMobile' | 'receiverProvince' | 'receiverCity' | 'receiverDistrict' | 'receiverDetail'> | null }>()
const visible = defineModel<boolean>({ default: false })
const emit = defineEmits<{ success: [] }>()

const { options: companies, loading: dictLoading } = useDict('express_company')
const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive({ deliveryCompany: '', deliveryNo: '' })
const rules: FormRules = {
  deliveryCompany: [{ required: true, message: '请选择快递公司', trigger: 'change' }],
  deliveryNo: [
    { required: true, message: '请输入快递单号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9-]{6,40}$/, message: '单号格式不正确', trigger: 'blur' },
  ],
}

watch(visible, (v) => {
  if (v) {
    form.deliveryCompany = ''
    form.deliveryNo = ''
    formRef.value?.clearValidate()
  }
})

async function submit() {
  if (!props.order) return
  if (!(await formRef.value?.validate().catch(() => false))) return
  saving.value = true
  try {
    await orderApi.deliver(props.order.id, { deliveryCompany: form.deliveryCompany, deliveryNo: form.deliveryNo.trim() })
    toast.success('发货成功')
    visible.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="订单发货" width="500px" append-to-body>
    <div v-if="order" class="deliver-info">
      <div><span class="text-secondary">订单号：</span><span class="mono">{{ order.orderNo }}</span></div>
      <div><span class="text-secondary">收货人：</span>{{ order.receiverName }} {{ order.receiverMobile }}</div>
      <div><span class="text-secondary">地址：</span>{{ fullAddress(order) }}</div>
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" @submit.prevent>
      <el-form-item label="快递公司" prop="deliveryCompany">
        <el-select v-model="form.deliveryCompany" :loading="dictLoading" placeholder="请选择" filterable allow-create class="w-full">
          <el-option v-for="c in companies" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="快递单号" prop="deliveryNo">
        <el-input v-model="form.deliveryNo" placeholder="请输入运单号" clearable @keyup.enter="submit" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">确认发货</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.deliver-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  margin-bottom: 18px;
  font-size: 13px;
  line-height: 1.5;
  border-radius: var(--app-radius);
  background: var(--app-card-bg-muted);
  color: var(--app-text);
}
</style>
