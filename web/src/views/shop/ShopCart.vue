<template>
  <div>
    <PageHeader title="购物车" subtitle="不同商家的商品将拆分为独立订单结算" />

    <div v-if="cart.items.length === 0" class="empty">
      <el-empty description="购物车还是空的，去挑点好物吧">
        <el-button type="primary" @click="$router.push('/shop')">去逛逛</el-button>
      </el-empty>
    </div>

    <div v-else class="cart-wrap">
      <div class="cart-left">
        <div class="cart-toolbar">
          <el-checkbox :model-value="cart.allChecked" @change="(v: boolean) => cart.checkAll(v)">全选</el-checkbox>
          <el-button link type="danger" :disabled="cart.checkedCount === 0" @click="batchRemove">
            <el-icon><Delete /></el-icon> 删除选中
          </el-button>
        </div>

        <!-- 按商家分组 -->
        <div v-for="g in allGroups" :key="g.storeId" class="store-group" :class="{ dim: !groupChecked(g) }">
          <div class="group-head">
            <el-checkbox :model-value="groupChecked(g)" @change="(v: boolean) => cart.toggleStore(g.storeId, v)" />
            <el-icon color="#d97706" :size="16"><Shop /></el-icon>
            <span class="group-name">{{ g.storeName }}</span>
            <el-tag size="small" type="warning" effect="plain">
              {{ pricingOf(g)?.discountAmountCent ? `${pricingOf(g)?.promoCode || '本店满减'} -¥${centToYuan(pricingOf(g)?.discountAmountCent)}` : '暂无可用满减' }}
            </el-tag>
            <span class="group-sub">
              小计 ¥ {{ centToYuan(g.totalCent) }}
              <template v-if="pricingOf(g)?.discountAmountCent">，优惠 -¥ {{ centToYuan(pricingOf(g)?.discountAmountCent) }}，应付 ¥ {{ centToYuan(pricingOf(g)?.payableAmountCent) }}</template>
            </span>
            <el-button link type="danger" size="small" @click="cart.removeStore(g.storeId)">删除整组</el-button>
          </div>

          <div v-for="it in g.items" :key="it.productId" class="cart-item" :class="{ dim: !it.checked }">
            <el-checkbox :model-value="it.checked" @change="() => cart.toggle(it.productId)" />
            <div class="ci-cover" @click="$router.push(`/shop/product/${it.productId}`)">{{ it.productName.charAt(0) }}</div>
            <div class="ci-name" @click="$router.push(`/shop/product/${it.productId}`)">{{ it.productName }}</div>
            <div class="ci-price">¥ {{ centToYuan(it.unitPriceCent) }}</div>
            <el-input-number :model-value="it.quantity" :min="1" :max="99" size="small"
                             @update:model-value="(v: number) => cart.setQty(it.productId, v)" />
            <div class="ci-line">¥ {{ centToYuan(it.unitPriceCent * it.quantity) }}</div>
            <el-button link type="danger" @click="cart.remove(it.productId)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 结算栏 -->
      <div class="cart-summary">
        <div class="sum-row"><span>已选件数</span><b>{{ cart.checkedCount }}</b></div>
        <div class="sum-row"><span>涉及商家</span><b>{{ cart.checkedGroups.length }} 家</b></div>
        <div class="sum-row"><span>商品合计</span><b>¥ {{ centToYuan(cart.checkedTotalCent) }}</b></div>
        <div class="sum-row" v-if="selectedDiscountCent > 0"><span>店铺优惠</span><b class="sum-discount">- ¥ {{ centToYuan(selectedDiscountCent) }}</b></div>
        <div class="sum-row sum-total"><span>应付金额</span><b class="sum-price">¥ {{ centToYuan(selectedPayableCent) }}</b></div>
        <el-button type="primary" size="large" class="submit-btn" :loading="submitting"
                   :disabled="cart.checkedCount === 0" @click="checkout">
          支付宝付款{{ cart.checkedGroups.length > 1 ? `（拆 ${cart.checkedGroups.length} 单，依次支付）` : '' }}
        </el-button>
        <p class="sum-tip">点击后将创建订单并立即展示支付宝沙箱付款码</p>
      </div>
    </div>

    <el-dialog v-model="paymentDialogVisible" title="支付宝沙箱付款" width="390px" align-center
               :close-on-click-modal="false" @closed="stopPaymentPolling">
      <div class="payment-dialog">
        <p v-if="paymentOrderIds.length > 1" class="payment-progress">
          第 {{ currentPaymentIndex + 1 }} / {{ paymentOrderIds.length }} 笔订单
        </p>
        <p>请使用支付宝沙箱版 App 扫描二维码完成模拟付款</p>
        <img v-if="paymentQrCodeImage" class="payment-qr-code" :src="paymentQrCodeImage" alt="支付宝付款二维码" />
        <p class="payment-amount">应付 ¥ {{ paymentAmount }}</p>
        <p class="payment-hint">支付成功后会自动继续下一笔订单；关闭后也可在“我的订单”中继续付款。</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import { useCartStore, type StoreGroup } from '../../stores/cart'
import { alipayPaymentStatus, createAlipayCheckout, createOrder, previewOrder } from '../../api/customer'
import { centToYuan } from '../../utils/money'
import type { OrderPricing } from '../../types'

const cart = useCartStore()
const submitting = ref(false)
const pricingByStore = ref<Record<number, OrderPricing>>({})
const paymentDialogVisible = ref(false)
const paymentQrCodeImage = ref('')
const paymentAmount = ref('0.00')
const paymentOrderIds = ref<number[]>([])
const currentPaymentIndex = ref(0)
// 同一次结算失败后再次点击会沿用同一 Key；成功后才清除，避免网络重试重复创建订单。
const checkoutKeys = ref<Record<number, string>>({})
let paymentPollingTimer: ReturnType<typeof setInterval> | null = null
let paymentChecking = false

/** 展示所有分组（含未勾选的，便于整组展示；结算只看 checkedGroups） */
const allGroups = computed<StoreGroup[]>(() => {
  const map = new Map<number, StoreGroup>()
  for (const i of cart.items) {
    const key = i.storeId || 0
    if (!map.has(key)) {
      map.set(key, { storeId: key, storeName: i.storeName || '官方直营', items: [], totalCent: 0, count: 0 })
    }
    const g = map.get(key)!
    g.items.push(i)
    g.totalCent += i.unitPriceCent * i.quantity
    g.count += i.quantity
  }
  return Array.from(map.values())
})

const selectedDiscountCent = computed(() => cart.checkedGroups.reduce(
  (sum, group) => sum + (pricingByStore.value[group.storeId]?.discountAmountCent || 0), 0
))
const selectedPayableCent = computed(() => Math.max(0, cart.checkedTotalCent - selectedDiscountCent.value))

function pricingOf(group: StoreGroup) {
  return pricingByStore.value[group.storeId]
}

async function refreshPricing() {
  const groups = cart.checkedGroups
  const next: Record<number, OrderPricing> = {}
  await Promise.all(groups.map(async (group) => {
    const items = group.items.map((item) => ({ productId: item.productId, quantity: item.quantity }))
    next[group.storeId] = await previewOrder(items)
  }))
  pricingByStore.value = next
}

watch(
  () => cart.items.map((item) => `${item.productId}:${item.quantity}:${item.checked}`).join('|'),
  () => { void refreshPricing() },
  { immediate: true }
)

function groupChecked(g: StoreGroup) {
  return g.items.length > 0 && g.items.every((i) => i.checked)
}

async function checkout() {
  const groups = cart.checkedGroups
  if (groups.length === 0) return
  submitting.value = true
  try {
    const createdOrderIds: number[] = []
    // 按商家逐个下单（拆单）
    for (const g of groups) {
      const items = g.items.map((i) => ({ productId: i.productId, quantity: i.quantity }))
      const key = checkoutKeys.value[g.storeId] ||= crypto.randomUUID()
      const order = await createOrder(items, undefined, key)
      createdOrderIds.push(order.id)
      g.items.forEach((i) => cart.remove(i.productId))
      delete checkoutKeys.value[g.storeId]
    }
    paymentOrderIds.value = createdOrderIds
    currentPaymentIndex.value = 0
    await showCurrentPaymentQrCode()
  } finally {
    submitting.value = false
  }
}

function currentPaymentOrderId() {
  return paymentOrderIds.value[currentPaymentIndex.value]
}

async function showCurrentPaymentQrCode() {
  const orderId = currentPaymentOrderId()
  if (!orderId) return
  const checkout = await createAlipayCheckout(orderId)
  paymentQrCodeImage.value = checkout.qrCodeImage
  paymentAmount.value = centToYuan(checkout.amountCent)
  paymentDialogVisible.value = true
  startPaymentPolling()
}

function stopPaymentPolling() {
  if (paymentPollingTimer) {
    clearInterval(paymentPollingTimer)
    paymentPollingTimer = null
  }
  paymentChecking = false
}

function startPaymentPolling() {
  stopPaymentPolling()
  paymentPollingTimer = setInterval(async () => {
    const orderId = currentPaymentOrderId()
    if (!orderId || paymentChecking) return
    paymentChecking = true
    try {
      const status = await alipayPaymentStatus(orderId)
      if (!status.paid) return
      stopPaymentPolling()
      currentPaymentIndex.value += 1
      if (currentPaymentOrderId()) {
        ElMessage.success('本笔订单支付成功，请继续支付下一笔')
        await showCurrentPaymentQrCode()
      } else {
        paymentDialogVisible.value = false
        ElMessage.success('支付成功，订单已进入待商家确认状态')
      }
    } catch {
      // 网络瞬时失败不打断顾客扫码，下一轮轮询会继续确认状态。
    } finally {
      paymentChecking = false
    }
  }, 2000)
}

async function batchRemove() {
  try {
    await ElMessageBox.confirm('确认删除选中的商品？', '提示', { type: 'warning' })
  } catch {
    return
  }
  cart.removeChecked()
  ElMessage.success('已删除')
}

onUnmounted(stopPaymentPolling)
</script>

<style scoped>
.empty { padding: 60px 0; }
.cart-wrap { display: grid; grid-template-columns: 1fr 320px; gap: 20px; align-items: start; }

.cart-left { display: flex; flex-direction: column; gap: 16px; }
.cart-toolbar {
  display: flex; align-items: center; justify-content: space-between; padding: 12px 20px;
  background: var(--of-surface); border-radius: 12px; box-shadow: var(--of-shadow);
}

.store-group {
  background: var(--of-surface); border-radius: 14px; box-shadow: var(--of-shadow); padding: 8px 20px 16px;
  transition: opacity 0.2s;
}
.store-group.dim { opacity: 0.55; }
.group-head {
  display: flex; align-items: center; gap: 8px; padding: 12px 0;
  border-bottom: 1px solid var(--of-border);
}
.group-name { font-weight: 700; font-size: 15px; color: var(--of-text); }
.group-sub { margin-left: auto; font-size: 13px; color: #d97706; font-weight: 600; }

.cart-item {
  display: grid; grid-template-columns: 20px 48px 1fr 100px 130px 120px 40px;
  align-items: center; gap: 12px; padding: 14px 0; border-bottom: 1px solid var(--of-border);
  transition: opacity 0.2s;
}
.cart-item:last-child { border-bottom: none; }
.cart-item.dim { opacity: 0.45; }
.ci-cover {
  width: 44px; height: 44px; border-radius: 10px; color: #fff; font-weight: 700; font-size: 20px;
  background: linear-gradient(135deg, #14b8a6, #0d9488); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
}
.ci-name { font-size: 14px; font-weight: 600; color: var(--of-text); cursor: pointer; }
.ci-price { font-size: 14px; color: var(--of-text-2); }
.ci-line { font-size: 15px; font-weight: 700; color: #ef4444; text-align: right; }

.cart-summary {
  background: var(--of-surface); border-radius: 14px; box-shadow: var(--of-shadow); padding: 22px;
  position: sticky; top: 84px;
}
.sum-row { display: flex; justify-content: space-between; margin-bottom: 14px; font-size: 14px; color: var(--of-text-2); }
.sum-row b { color: var(--of-text); }
.sum-discount { color: #16a34a !important; }
.sum-total { border-top: 1px dashed var(--of-border); padding-top: 14px; font-size: 15px; }
.sum-price { color: #ef4444; font-size: 24px; }
.submit-btn { width: 100%; margin-top: 6px; font-weight: 600; }
.sum-tip { margin: 12px 0 0; font-size: 12px; color: var(--of-text-3); text-align: center; }
.payment-dialog { text-align: center; color: var(--of-text-2); }
.payment-dialog > p { margin: 8px 0; font-size: 14px; }
.payment-progress { color: #0f766e; font-weight: 700; }
.payment-qr-code { display: block; width: 280px; height: 280px; margin: 14px auto; border: 8px solid #fff; border-radius: 10px; box-shadow: 0 3px 16px rgba(15, 23, 42, 0.12); }
.payment-amount { color: #ef4444; font-size: 20px !important; font-weight: 700; }
.payment-hint { color: var(--of-text-3); font-size: 12px !important; line-height: 1.6; }

@media (max-width: 860px) { .cart-wrap { grid-template-columns: 1fr; } }
</style>
