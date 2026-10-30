<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import type { CartItem, GoodsItem } from '@/types'
import { checkCart, deleteCart, getCart, updateCartQuantity } from '@/api/cart'
import { getGoodsPage } from '@/api/public'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { useAppStore } from '@/stores/app'
import Price from '@/components/Price.vue'
import EmptyState from '@/components/EmptyState.vue'
import GoodsWaterfall from '@/components/GoodsWaterfall.vue'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const cartStore = useCartStore()
const app = useAppStore()

const items = ref<CartItem[]>([])
const loading = ref(false)
const loaded = ref(false)
const editing = ref(false)
/** 编辑模式下的本地选择（不影响结算勾选） */
const editSelected = ref<Set<string>>(new Set())
const guess = ref<GoodsItem[]>([])

async function load() {
  if (!user.isLogin) return
  loading.value = true
  try {
    items.value = (await getCart()) || []
    syncCount()
  } finally {
    loading.value = false
    loaded.value = true
  }
}

function syncCount() {
  cartStore.setCount(items.value.reduce((a, i) => a + i.quantity, 0))
}

onMounted(() => {
  load()
  getGoodsPage({ page: 1, size: 10, sort: 'SALES' })
    .then((r) => (guess.value = r?.records || []))
    .catch(() => {})
})

const validItems = computed(() => items.value.filter((i) => i.valid))
const invalidItems = computed(() => items.value.filter((i) => !i.valid))

const checkedItems = computed(() => validItems.value.filter((i) => i.checked))
const allChecked = computed(() =>
  editing.value
    ? items.value.length > 0 && editSelected.value.size === items.value.length
    : validItems.value.length > 0 && checkedItems.value.length === validItems.value.length,
)
const total = computed(() => checkedItems.value.reduce((a, i) => a + Number(i.price) * i.quantity, 0))
const checkedQty = computed(() => checkedItems.value.reduce((a, i) => a + i.quantity, 0))

const freightTip = computed(() => {
  const th = Number(app.config.freightFreeThreshold || 0)
  if (!th || Number(app.config.freightAmount || 0) <= 0 || !checkedItems.value.length) return ''
  const diff = th - total.value
  return diff > 0 ? `再买 ¥${diff.toFixed(2)} 可享包邮` : '已满足包邮条件'
})

/* ---------------- 勾选 ---------------- */
async function toggleItem(item: CartItem) {
  if (editing.value) {
    const s = new Set(editSelected.value)
    if (s.has(item.id)) s.delete(item.id)
    else s.add(item.id)
    editSelected.value = s
    return
  }
  const next = !item.checked
  item.checked = next
  try {
    await checkCart([item.id], next)
  } catch {
    item.checked = !next
  }
}

async function toggleAll() {
  if (editing.value) {
    editSelected.value = allChecked.value ? new Set() : new Set(items.value.map((i) => i.id))
    return
  }
  const next = !allChecked.value
  const prev = validItems.value.map((i) => i.checked)
  validItems.value.forEach((i) => (i.checked = next))
  try {
    await checkCart([], next)
  } catch {
    validItems.value.forEach((i, idx) => (i.checked = prev[idx]))
  }
}

/* ---------------- 数量 ---------------- */
const qtyTimers = new Map<string, number>()
function changeQty(item: CartItem, val: number | string) {
  const q = Number(val)
  if (!q || q < 1) return
  item.quantity = q
  syncCount()
  // 防抖：连续点击只提交最后一次
  clearTimeout(qtyTimers.get(item.id))
  qtyTimers.set(
    item.id,
    window.setTimeout(async () => {
      try {
        await updateCartQuantity(item.id, item.quantity)
      } catch {
        load()
      }
    }, 400),
  )
}

/* ---------------- 删除 ---------------- */
async function remove(ids: string[], message = '确定删除选中的商品吗？') {
  if (!ids.length) return showToast('请选择商品')
  try {
    await showConfirmDialog({ title: '删除商品', message })
  } catch {
    return
  }
  try {
    await deleteCart(ids)
    items.value = items.value.filter((i) => !ids.includes(i.id))
    editSelected.value = new Set()
    syncCount()
    showToast('已删除')
  } catch {
    /* 已提示 */
  }
}

function clearInvalid() {
  remove(
    invalidItems.value.map((i) => i.id),
    '确定清空所有失效商品吗？',
  )
}

function toggleEdit() {
  editing.value = !editing.value
  editSelected.value = new Set()
}

function settle() {
  if (!checkedItems.value.length) return showToast('请选择要结算的商品')
  app.setCheckout({ source: 'CART', cartIds: checkedItems.value.map((i) => i.id) })
  router.push({ name: 'order-confirm' })
}

function toLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

function isSelected(item: CartItem) {
  return editing.value ? editSelected.value.has(item.id) : item.checked
}
</script>

<template>
  <div class="page cart" :class="{ 'has-bar': user.isLogin && items.length }">
    <div class="cart__header">
      <span class="cart__title">购物车<small v-if="items.length">({{ items.length }})</small></span>
      <span v-if="items.length" class="cart__edit" @click="toggleEdit">{{ editing ? '完成' : '编辑' }}</span>
    </div>

    <!-- 未登录 -->
    <EmptyState
      v-if="!user.isLogin"
      description="登录后可同步购物车中的商品"
      button-text="去登录"
      @click="toLogin"
    />
    <van-loading v-else-if="loading && !loaded" class="cart__loading" vertical>加载中...</van-loading>
    <EmptyState
      v-else-if="loaded && !items.length"
      description="购物车还是空的，快去挑选好物吧"
      button-text="去逛逛"
      @click="router.push('/')"
    />

    <template v-else>
      <div v-if="freightTip && !editing" class="cart__tip">
        <van-icon name="logistics" /> {{ freightTip }}
      </div>

      <!-- 有效商品 -->
      <div v-if="validItems.length" class="cart__group">
        <van-swipe-cell v-for="item in validItems" :key="item.id" class="cart__cell">
          <div class="cart-item">
            <van-checkbox :model-value="isSelected(item)" class="cart-item__check" @click="toggleItem(item)" />
            <van-image
              :src="item.pic"
              width="88"
              height="88"
              radius="8"
              fit="cover"
              lazy-load
              @click="router.push({ name: 'goods', params: { id: item.goodsId } })"
            />
            <div class="cart-item__body">
              <div class="cart-item__name ellipsis-2" @click="router.push({ name: 'goods', params: { id: item.goodsId } })">
                {{ item.goodsName }}
              </div>
              <div v-if="item.specText" class="cart-item__spec ellipsis">{{ item.specText }}</div>
              <div class="cart-item__foot">
                <Price :value="item.price" size="sm" />
                <van-stepper
                  :model-value="item.quantity"
                  :min="1"
                  :max="Math.max(1, item.stock)"
                  integer
                  button-size="22"
                  input-width="32"
                  @change="(v: number | string) => changeQty(item, v)"
                />
              </div>
              <div v-if="item.stock > 0 && item.stock < 10" class="cart-item__low">仅剩 {{ item.stock }} 件</div>
            </div>
          </div>
          <template #right>
            <van-button square type="danger" text="删除" class="cart__swipe-btn" @click="remove([item.id])" />
          </template>
        </van-swipe-cell>
      </div>

      <!-- 失效商品 -->
      <div v-if="invalidItems.length" class="cart__group cart__group--invalid">
        <div class="cart__invalid-head">
          <span>失效宝贝 {{ invalidItems.length }} 件</span>
          <span class="text-primary" @click="clearInvalid">清空失效商品</span>
        </div>
        <van-swipe-cell v-for="item in invalidItems" :key="item.id" class="cart__cell">
          <div class="cart-item is-invalid">
            <van-checkbox
              v-if="editing"
              :model-value="isSelected(item)"
              class="cart-item__check"
              @click="toggleItem(item)"
            />
            <span v-else class="cart-item__invalid-tag">失效</span>
            <van-image :src="item.pic" width="88" height="88" radius="8" fit="cover" lazy-load />
            <div class="cart-item__body">
              <div class="cart-item__name ellipsis-2">{{ item.goodsName }}</div>
              <div v-if="item.specText" class="cart-item__spec ellipsis">{{ item.specText }}</div>
              <div class="cart-item__reason">{{ item.stock <= 0 ? '商品已售罄' : '商品已下架' }}</div>
            </div>
          </div>
          <template #right>
            <van-button square type="danger" text="删除" class="cart__swipe-btn" @click="remove([item.id])" />
          </template>
        </van-swipe-cell>
      </div>
    </template>

    <!-- 猜你喜欢 -->
    <template v-if="guess.length && (!user.isLogin || loaded)">
      <div class="cart__guess-title">— 猜你喜欢 —</div>
      <GoodsWaterfall :list="guess" />
    </template>

    <!-- 结算栏 -->
    <div v-if="user.isLogin && items.length" class="cart__bar">
      <van-checkbox :model-value="allChecked" @click="toggleAll">全选</van-checkbox>
      <template v-if="!editing">
        <div class="cart__sum">
          <div>
            合计：<Price :value="total" size="lg" />
          </div>
          <div class="cart__sum-tip">不含运费</div>
        </div>
        <van-button round class="btn-gradient cart__settle" @click="settle">
          结算<template v-if="checkedQty">({{ checkedQty }})</template>
        </van-button>
      </template>
      <template v-else>
        <div class="cart__sum" />
        <van-button round plain type="danger" class="cart__settle" @click="remove([...editSelected])">
          删除<template v-if="editSelected.size">({{ editSelected.size }})</template>
        </van-button>
      </template>
    </div>
  </div>
</template>

<style scoped lang="scss">
.cart {
  padding-bottom: 12px;
  &.has-bar {
    padding-bottom: 64px;
  }
  &__header {
    position: sticky;
    top: 0;
    z-index: 20;
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 46px;
    padding: 0 $gap;
    padding-top: env(safe-area-inset-top);
    box-sizing: content-box;
    background: $card;
  }
  &__title {
    font-size: 17px;
    font-weight: 700;
    small {
      margin-left: 2px;
      font-size: 13px;
      font-weight: normal;
    }
  }
  &__edit {
    font-size: 14px;
    color: $text-2;
  }
  &__loading {
    padding: 60px 0;
  }
  &__tip {
    margin: 10px $gap 0;
    padding: 8px 12px;
    font-size: 12px;
    color: $primary;
    background: $primary-light;
    border-radius: $radius;
  }
  &__group {
    margin: 10px $gap 0;
    background: $card;
    border-radius: $radius;
    overflow: hidden;
  }
  &__invalid-head {
    display: flex;
    justify-content: space-between;
    padding: 12px 12px 0;
    font-size: 13px;
  }
  &__swipe-btn {
    height: 100%;
  }
  &__guess-title {
    padding: 20px 0 12px;
    text-align: center;
    font-size: 14px;
    color: $text-2;
  }
  &__bar {
    position: fixed;
    left: 50%;
    transform: translateX(-50%);
    bottom: calc(50px + env(safe-area-inset-bottom));
    z-index: 99;
    width: 100%;
    max-width: $page-max;
    height: 54px;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 0 $gap;
    background: $card;
    border-top: 1px solid $border;
  }
  &__sum {
    flex: 1;
    min-width: 0;
    text-align: right;
    font-size: 13px;
  }
  &__sum-tip {
    font-size: 11px;
    color: $text-3;
  }
  &__settle {
    flex-shrink: 0;
    min-width: 96px;
    height: 40px;
  }
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: $card;
  &__check {
    flex-shrink: 0;
  }
  :deep(.van-image) {
    flex-shrink: 0;
  }
  &__body {
    flex: 1;
    min-width: 0;
    align-self: stretch;
    display: flex;
    flex-direction: column;
  }
  &__name {
    font-size: 13px;
    line-height: 18px;
  }
  &__spec {
    align-self: flex-start;
    max-width: 100%;
    margin-top: 4px;
    padding: 1px 6px;
    font-size: 11px;
    color: $text-3;
    background: $bg;
    border-radius: 3px;
  }
  &__foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: auto;
    padding-top: 6px;
  }
  &__low {
    margin-top: 2px;
    font-size: 11px;
    color: $primary;
  }
  &__invalid-tag {
    flex-shrink: 0;
    padding: 1px 4px;
    font-size: 10px;
    color: #fff;
    background: $disabled;
    border-radius: 3px;
  }
  &__reason {
    margin-top: auto;
    font-size: 12px;
    color: $text-3;
  }
  &.is-invalid {
    .cart-item__name {
      color: $text-3;
    }
    :deep(.van-image) {
      opacity: 0.5;
    }
  }
}
</style>
