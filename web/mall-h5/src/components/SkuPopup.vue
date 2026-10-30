<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { showImagePreview, showToast } from 'vant'
import type { GoodsDetail, Sku, SkuAction } from '@/types'
import Price from './Price.vue'

/**
 * SKU 选择弹窗。
 * - 规格按钮组：某值与已选其它规格组合后无任何有库存 SKU 时置灰不可选。
 * - 秒杀：选中的是秒杀 SKU 且为「立即购买」时，按秒杀价展示，数量受限购与剩余名额限制。
 * - action：all 显示加购+购买两个按钮；cart / buy 只显示「确定」。
 */

const props = withDefaults(
  defineProps<{ goods: GoodsDetail; action?: 'all' | SkuAction; seckillOnly?: boolean }>(),
  { action: 'all', seckillOnly: false },
)
const show = defineModel<boolean>('show', { default: false })
const skuId = defineModel<string>('skuId', { default: '' })
const quantity = defineModel<number>('quantity', { default: 1 })

const emit = defineEmits<{
  confirm: [payload: { sku: Sku; quantity: number; action: SkuAction; seckillId?: string }]
}>()

const specs = computed(() => props.goods.specItems || [])
const skus = computed(() => props.goods.skus || [])
const selected = ref<(string | undefined)[]>([])

const ongoingSeckill = computed(() => (props.goods.seckill?.state === 'ONGOING' ? props.goods.seckill : null))

/** 根据已有 skuId 或默认规则初始化选中项 */
function init() {
  const current = skus.value.find((s) => s.id === skuId.value)
  if (current) {
    selected.value = [...current.specValues]
    return
  }
  const sk = ongoingSeckill.value && skus.value.find((s) => s.id === ongoingSeckill.value!.skuId)
  if (sk) {
    selected.value = [...sk.specValues]
    return
  }
  const inStock = skus.value.filter((s) => s.stock > 0)
  if (inStock.length === 1) {
    selected.value = [...inStock[0].specValues]
    return
  }
  // 只有一个可选值的规格默认选中
  selected.value = specs.value.map((s) => (s.values.length === 1 ? s.values[0] : undefined))
}

watch(
  () => [props.goods.id, show.value],
  () => {
    if (show.value) init()
  },
  { immediate: true },
)

function matches(sku: Sku, sel: (string | undefined)[]) {
  return sel.every((v, i) => v === undefined || sku.specValues[i] === v)
}

function isDisabled(specIndex: number, value: string) {
  const sel = [...selected.value]
  sel[specIndex] = value
  return !skus.value.some((s) => s.stock > 0 && matches(s, sel))
}

function toggle(specIndex: number, value: string) {
  if (isDisabled(specIndex, value)) return
  const sel = [...selected.value]
  sel[specIndex] = sel[specIndex] === value ? undefined : value
  selected.value = sel
}

const selectedSku = computed<Sku | null>(() => {
  if (!specs.value.length) return skus.value[0] || null
  if (selected.value.length < specs.value.length || selected.value.some((v) => v === undefined)) return null
  return skus.value.find((s) => matches(s, selected.value)) || null
})

watch(selectedSku, (s) => {
  skuId.value = s?.id || ''
  if (quantity.value > maxQty.value && maxQty.value > 0) quantity.value = maxQty.value
})

const isSeckillSku = computed(() => !!ongoingSeckill.value && selectedSku.value?.id === ongoingSeckill.value.skuId)

/** 秒杀剩余名额 */
const seckillRemain = computed(() => {
  const s = ongoingSeckill.value
  return s ? Math.max(0, s.stock - s.sold) : 0
})

const maxQty = computed(() => {
  const sku = selectedSku.value
  if (!sku) return 99
  if (isSeckillSku.value && props.action !== 'cart') {
    const limit = ongoingSeckill.value!.limitPerMember || 99
    return Math.max(1, Math.min(limit, seckillRemain.value, sku.stock))
  }
  return Math.max(1, Math.min(sku.stock, 999))
})

const displayPrice = computed(() => {
  if (isSeckillSku.value && props.action !== 'cart') return ongoingSeckill.value!.seckillPrice
  return selectedSku.value?.price ?? props.goods.price
})

const displayStock = computed(() => {
  if (isSeckillSku.value && props.action !== 'cart') return seckillRemain.value
  return selectedSku.value ? selectedSku.value.stock : props.goods.stock ?? skus.value.reduce((a, s) => a + s.stock, 0)
})

const displayImg = computed(() => selectedSku.value?.pic || props.goods.pic)

const tipText = computed(() => {
  const missing = specs.value.filter((_, i) => !selected.value[i]).map((s) => s.name)
  if (missing.length) return `请选择 ${missing.join(' ')}`
  return `已选：${selected.value.join('，') || '默认'}`
})

function previewImg() {
  showImagePreview({ images: [displayImg.value], closeable: true })
}

function confirm(action: SkuAction) {
  const sku = selectedSku.value
  if (!sku) {
    showToast(tipText.value)
    return
  }
  if (sku.stock <= 0) {
    showToast('该规格已售罄')
    return
  }
  const useSeckill = isSeckillSku.value && action === 'buy'
  if (useSeckill && seckillRemain.value <= 0) {
    showToast('秒杀名额已抢光')
    return
  }
  emit('confirm', {
    sku,
    quantity: quantity.value,
    action,
    seckillId: useSeckill ? ongoingSeckill.value!.id : undefined,
  })
}
</script>

<template>
  <van-popup v-model:show="show" position="bottom" round closeable safe-area-inset-bottom class="sku-popup">
    <div class="sku">
      <div class="sku__head">
        <div class="sku__img" @click="previewImg">
          <van-image :src="displayImg" fit="cover" width="100%" height="100%" radius="8" />
        </div>
        <div class="sku__info">
          <div class="sku__price">
            <span v-if="isSeckillSku && action !== 'cart'" class="sku__seckill-tag">秒杀价</span>
            <Price :value="displayPrice" size="lg" />
            <Price
              v-if="isSeckillSku && action !== 'cart' && selectedSku"
              :value="selectedSku.price"
              strike
              class="sku__origin"
            />
          </div>
          <div class="sku__stock">
            库存 {{ displayStock }} 件
            <template v-if="isSeckillSku && action !== 'cart' && ongoingSeckill?.limitPerMember">
              · 每人限购 {{ ongoingSeckill.limitPerMember }} 件
            </template>
          </div>
          <div class="sku__tip ellipsis">{{ tipText }}</div>
        </div>
      </div>

      <div class="sku__body">
        <div v-for="(spec, si) in specs" :key="spec.name" class="sku__group">
          <div class="sku__group-title">{{ spec.name }}</div>
          <div class="sku__values">
            <span
              v-for="v in spec.values"
              :key="v"
              class="sku__value"
              :class="{ 'is-active': selected[si] === v, 'is-disabled': isDisabled(si, v) }"
              @click="toggle(si, v)"
            >
              {{ v }}
            </span>
          </div>
        </div>
        <div class="sku__qty">
          <span>购买数量</span>
          <van-stepper v-model="quantity" :min="1" :max="maxQty" integer button-size="26" input-width="40" />
        </div>
      </div>

      <div class="sku__actions">
        <template v-if="action === 'all' && !seckillOnly">
          <van-button class="sku__btn sku__btn--cart" round @click="confirm('cart')">加入购物车</van-button>
          <van-button class="sku__btn btn-gradient" round @click="confirm('buy')">
            {{ isSeckillSku ? '立即抢购' : '立即购买' }}
          </van-button>
        </template>
        <van-button
          v-else
          class="sku__btn btn-gradient"
          round
          block
          @click="confirm(action === 'cart' ? 'cart' : 'buy')"
        >
          确定
        </van-button>
      </div>
    </div>
  </van-popup>
</template>

<style scoped lang="scss">
.sku {
  display: flex;
  flex-direction: column;
  max-height: 80vh;
  &__head {
    display: flex;
    gap: 12px;
    padding: 16px 40px 12px 16px;
  }
  &__img {
    flex-shrink: 0;
    width: 96px;
    height: 96px;
    border-radius: 8px;
    background: $bg;
    overflow: hidden;
  }
  &__info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
  }
  &__price {
    display: flex;
    align-items: baseline;
    gap: 6px;
    flex-wrap: wrap;
  }
  &__seckill-tag {
    padding: 1px 4px;
    font-size: 10px;
    color: #fff;
    background: $gradient;
    border-radius: 3px;
    align-self: center;
  }
  &__stock,
  &__tip {
    margin-top: 6px;
    font-size: 12px;
    color: $text-3;
  }
  &__tip {
    color: $text-2;
  }
  &__body {
    flex: 1;
    overflow-y: auto;
    padding: 0 16px;
    -webkit-overflow-scrolling: touch;
  }
  &__group {
    padding: 12px 0;
    border-top: 1px solid $border;
  }
  &__group-title {
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 10px;
  }
  &__values {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }
  &__value {
    max-width: 100%;
    padding: 6px 14px;
    font-size: 13px;
    line-height: 18px;
    border-radius: 999px;
    background: $bg;
    color: $text;
    border: 1px solid transparent;
    transition: all 0.15s;
    word-break: break-all;
    &.is-active {
      color: $primary;
      background: $primary-light;
      border-color: $primary;
    }
    &.is-disabled {
      color: $disabled;
      background: $bg;
      text-decoration: line-through;
    }
  }
  &__qty {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 0 20px;
    border-top: 1px solid $border;
    font-size: 14px;
    font-weight: 600;
  }
  &__actions {
    display: flex;
    gap: 10px;
    padding: 8px 16px;
  }
  &__btn {
    flex: 1;
    height: 42px;
    font-size: 15px;
    &--cart {
      background: $orange-gradient;
      color: #fff;
      border: none;
    }
  }
}
</style>
