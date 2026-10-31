<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import type { AftersaleType, Order, OrderItem } from '@/types'
import { getOrder } from '@/api/order'
import { applyAftersale } from '@/api/aftersale'
import { AFTERSALE_REASONS } from '@/utils/dict'
import { fmtSpec, money } from '@/utils'
import NavBar from '@/components/NavBar.vue'
import Price from '@/components/Price.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadState from '@/components/LoadState.vue'
import { useDict } from '@/composables/useDict'
import { errorMessage } from '@/api/request'
import ImageUploader from '@/components/ImageUploader.vue'

const route = useRoute()
const router = useRouter()
const orderId = route.query.orderId as string
const itemId = route.query.itemId as string

const order = ref<Order | null>(null)
const item = ref<OrderItem | null>(null)
const loading = ref(true)
const submitting = ref(false)

const type = ref<AftersaleType>('REFUND')
const reason = ref('')
const reasonShow = ref(false)
const amount = ref('')
const description = ref('')
const images = ref<string[]>([])

const loadError = ref('')
async function load() {
  loading.value = true
  loadError.value = ''
  try {
    order.value = await getOrder(orderId)
    item.value = order.value.items.find((i) => i.id === itemId) || null
    if (item.value) amount.value = money(maxAmount.value)
  } catch (e) {
    loadError.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}
onMounted(load)

/** 待发货只能仅退款；待收货/已完成可选退货退款 */
const onlyRefund = computed(() => order.value?.status === 'PENDING_DELIVERY')
/** 最大可退：该明细实付分摊金额 */
const maxAmount = computed(() => Number(item.value?.payAmount ?? item.value?.totalAmount ?? 0))
/** 售后原因取字典 aftersale_reason，失败时用内置列表 */
const reasons = useDict('aftersale_reason', AFTERSALE_REASONS)
const reasonActions = computed(() => reasons.value.map((r) => ({ name: r.label })))

async function submit() {
  if (!item.value) return
  if (!reason.value) return showToast('请选择申请原因')
  const amt = Number(amount.value)
  if (!(amt > 0)) return showToast('请输入退款金额')
  if (amt > maxAmount.value) return showToast(`退款金额不能超过 ¥${money(maxAmount.value)}`)
  submitting.value = true
  try {
    const id = await applyAftersale({
      orderItemId: item.value.id,
      type: onlyRefund.value ? 'REFUND' : type.value,
      reason: reason.value,
      description: description.value,
      images: images.value,
      amount: Number(amt.toFixed(2)),
    })
    showSuccessToast('申请已提交')
    if (id && typeof id === 'string') router.replace({ name: 'aftersale-detail', params: { id } })
    else router.replace({ name: 'aftersale-list' })
  } catch {
    /* 已提示 */
  } finally {
    submitting.value = false
  }
}
function onReason(a: { name: string }) {
  reason.value = a.name
}
</script>

<template>
  <div class="page apply">
    <NavBar title="申请售后" />
    <LoadState v-if="!item || !order" :loading="loading" :error="loadError" @retry="load">
      <EmptyState description="订单商品不存在" />
    </LoadState>
    <template v-else>
      <div class="card apply__goods">
        <van-image :src="item.pic" width="64" height="64" radius="6" fit="cover" />
        <div class="apply__goods-body">
          <div class="ellipsis-2 apply__goods-name">{{ item.goodsName }}</div>
          <div class="apply__goods-spec ellipsis">{{ fmtSpec(item.specText) }} x{{ item.quantity }}</div>
        </div>
        <Price :value="item.payAmount" size="xs" color="inherit" />
      </div>

      <div class="card apply__types">
        <div class="apply__label">服务类型</div>
        <div class="apply__type-list">
          <div class="apply__type" :class="{ active: type === 'REFUND' || onlyRefund }" @click="type = 'REFUND'">
            <van-icon name="balance-o" />
            <div>
              <div class="apply__type-name">仅退款</div>
              <div class="apply__type-desc">{{ onlyRefund ? '商品未发货，可直接退款' : '未收到货或与商家协商一致' }}</div>
            </div>
          </div>
          <div
            v-if="!onlyRefund"
            class="apply__type"
            :class="{ active: type === 'RETURN_REFUND' }"
            @click="type = 'RETURN_REFUND'"
          >
            <van-icon name="exchange" />
            <div>
              <div class="apply__type-name">退货退款</div>
              <div class="apply__type-desc">已收到货，需要退还商品</div>
            </div>
          </div>
        </div>
      </div>

      <van-cell-group inset class="apply__form">
        <van-cell title="申请原因" is-link :value="reason || '请选择'" :value-class="reason ? '' : 'text-3'" required @click="reasonShow = true" />
        <van-field
          v-model="amount"
          label="退款金额"
          type="number"
          required
          input-align="right"
          :placeholder="`最多 ¥${money(maxAmount)}`"
        >
          <template #extra><span class="apply__yuan">元</span></template>
        </van-field>
        <div class="apply__amount-tip">最多可退 ¥{{ money(maxAmount) }}，含发货运费的以商家审核为准</div>
        <van-field
          v-model="description"
          label="问题描述"
          type="textarea"
          rows="3"
          autosize
          maxlength="200"
          show-word-limit
          placeholder="请详细描述问题，便于商家快速处理"
        />
        <div class="apply__upload">
          <div class="apply__upload-title">上传凭证 <span class="text-3">（最多 6 张）</span></div>
          <ImageUploader v-model="images" :max-count="6" biz-type="aftersale" />
        </div>
      </van-cell-group>

      <div class="apply__space" />
      <div class="apply__bar">
        <van-button block round class="btn-gradient" :loading="submitting" loading-text="提交中..." @click="submit">
          提交申请
        </van-button>
      </div>

      <van-action-sheet
        v-model:show="reasonShow"
        title="申请原因"
        :actions="reasonActions"
        cancel-text="取消"
        close-on-click-action
        @select="onReason"
      />
    </template>
  </div>
</template>

<style scoped lang="scss">
.apply {
  &__loading {
    padding: 60px 0;
  }
  &__goods {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  &__goods-body {
    flex: 1;
    min-width: 0;
  }
  &__goods-name {
    font-size: 13px;
    line-height: 18px;
  }
  &__goods-spec {
    margin-top: 4px;
    font-size: 12px;
    color: $text-3;
  }
  &__label {
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 10px;
  }
  &__type-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  &__type {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px;
    border: 1px solid $border;
    border-radius: $radius;
    transition: all 0.15s;
    .van-icon {
      font-size: 22px;
      color: $text-2;
    }
    &.active {
      border-color: $primary;
      background: $primary-light;
      .van-icon {
        color: $primary;
      }
    }
  }
  &__type-name {
    font-size: 14px;
    font-weight: 600;
  }
  &__type-desc {
    margin-top: 2px;
    font-size: 12px;
    color: $text-3;
  }
  &__form {
    margin: 10px $gap 0;
  }
  &__yuan {
    margin-left: 4px;
  }
  &__amount-tip {
    padding: 0 16px 8px;
    font-size: 11px;
    color: $text-3;
  }
  &__upload {
    padding: 10px 16px;
  }
  &__upload-title {
    font-size: 14px;
    margin-bottom: 10px;
  }
  &__space {
    height: calc(72px + env(safe-area-inset-bottom));
  }
  &__bar {
    @include fixed-bottom;
    padding: 8px $gap calc(8px + env(safe-area-inset-bottom));
  }
}
:deep(.van-cell-group--inset) {
  margin: 10px $gap 0;
}
</style>
