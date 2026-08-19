<template>
  <div>
    <PageHeader title="我的订单" subtitle="查看你所有的订单与当前状态" />

    <!-- 状态筛选 -->
    <div class="tabs">
      <div v-for="t in tabs" :key="t.key" class="tab" :class="{ active: activeTab === t.key }" @click="activeTab = t.key">
        {{ t.label }}
        <span class="tab-count">{{ countBy(t.key) }}</span>
      </div>
    </div>

    <div v-if="filtered.length === 0 && !loading" class="empty">
      <el-empty description="该状态下暂无订单">
        <el-button type="primary" @click="$router.push('/shop')">去逛逛</el-button>
      </el-empty>
    </div>

    <div v-else v-loading="loading" class="order-list">
      <div v-for="o in filtered" :key="o.id" class="order-card of-fade-up">
        <div class="order-head">
          <div class="oh-left">
            <span class="order-no">订单号 {{ o.orderNo }}</span>
            <span class="order-time">{{ formatTime(o.createdAt) }}</span>
            <span class="order-store"><el-icon><Shop /></el-icon>{{ o.storeName || '商家店铺' }}</span>
          </div>
          <div class="oh-right">
            <el-icon :size="18" :color="statusMap[o.status]?.color" class="oh-icon">
              <component :is="statusMap[o.status]?.icon || 'CircleCheck'" />
            </el-icon>
            <span class="oh-status" :style="{ color: statusMap[o.status]?.color }">{{ statusMap[o.status]?.label || o.status }}</span>
          </div>
        </div>

        <div class="order-items">
          <div v-for="(it, idx) in o.items" :key="idx" class="order-item" @click="$router.push(`/shop/product/${it.productId}`)">
            <span class="oi-name">{{ it.productName }}</span>
            <span class="oi-qty">x{{ it.quantity }}</span>
            <span class="oi-price">¥ {{ centToYuan(it.unitPriceCent) }}</span>
          </div>
        </div>

        <div class="order-foot">
          <span class="of-total">
            共 {{ itemCount(o) }} 件
            <template v-if="(o.discountAmountCent || 0) > 0">，{{ o.promoCode || '店铺优惠' }} 已优惠 ¥ {{ centToYuan(o.discountAmountCent) }}</template>
            ，合计 <b>¥ {{ centToYuan(o.totalAmountCent) }}</b>
          </span>
          <el-button v-if="o.status === 'PENDING_PAYMENT'" type="primary" size="small" :loading="payingOrderId === o.id"
                     @click.stop="pay(o)">模拟付款</el-button>
          <el-button v-if="o.status === 'PENDING_PAYMENT'" type="danger" plain size="small" :loading="cancellingOrderId === o.id"
                     @click.stop="cancelOrder(o)">取消订单</el-button>
          <el-button class="contact-merchant-btn" size="small" @click.stop="contactMerchant(o)">
            <el-icon><ChatDotRound /></el-icon> 联系商家
          </el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="paymentDialogVisible" title="模拟扫码付款" width="390px" align-center @closed="stopPaymentPolling">
      <div class="payment-dialog">
        <p>请用手机扫描二维码，在项目模拟收银台确认付款</p>
        <img v-if="paymentQrCodeImage" class="payment-qr-code" :src="paymentQrCodeImage" alt="模拟付款二维码" />
        <p class="payment-amount">应付 ¥ {{ paymentAmount }}</p>
        <p class="payment-hint">支付成功后，订单会自动更新为“已付款，待商家确认”</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import { cancelPendingPaymentOrder, createMockCheckout, myOrders, paymentStatus } from '../../api/customer'
import { customerChatApi } from '../../api/chat'
import { centToYuan } from '../../utils/money'
import type { Order } from '../../types'

const loading = ref(false)
const orders = ref<Order[]>([])
const activeTab = ref('all')
const payingOrderId = ref<number | null>(null)
const cancellingOrderId = ref<number | null>(null)
const paymentDialogVisible = ref(false)
const paymentQrCodeImage = ref('')
const paymentAmount = ref('0.00')
const payingDialogOrderId = ref<number | null>(null)
const router = useRouter()
let paymentPollingTimer: ReturnType<typeof setInterval> | null = null

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'progress', label: '进行中' },
  { key: 'completed', label: '已完成' },
  { key: 'refund', label: '退款/售后' },
  { key: 'cancelled', label: '已取消' }
]

const statusMap: Record<string, { label: string; color: string; icon: string }> = {
  PENDING_PAYMENT: { label: '待付款', color: '#f59e0b', icon: 'Wallet' },
  PAID: { label: '已付款，待商家确认', color: '#6366f1', icon: 'CircleCheck' },
  CREATED: { label: '已创建', color: '#3b82f6', icon: 'Document' },
  CONFIRMED: { label: '已确认', color: '#f59e0b', icon: 'CircleCheck' },
  SHIPPED: { label: '已发货', color: '#8b5cf6', icon: 'Van' },
  COMPLETED: { label: '已完成', color: '#10b981', icon: 'CircleCheck' },
  CANCELLED: { label: '已取消', color: '#9ca3af', icon: 'CircleClose' },
  REFUNDING: { label: '退款中', color: '#f59e0b', icon: 'Refresh' },
  REFUNDED: { label: '已退款', color: '#6b7280', icon: 'Money' }
}

const GROUP: Record<string, string> = {
  PENDING_PAYMENT: 'progress', PAID: 'progress', CREATED: 'progress', CONFIRMED: 'progress', SHIPPED: 'progress',
  COMPLETED: 'completed',
  REFUNDING: 'refund', REFUNDED: 'refund',
  CANCELLED: 'cancelled'
}

const filtered = computed(() => {
  if (activeTab.value === 'all') return orders.value
  return orders.value.filter((o) => GROUP[o.status] === activeTab.value)
})

function countBy(key: string) {
  if (key === 'all') return orders.value.length
  return orders.value.filter((o) => GROUP[o.status] === key).length
}

function itemCount(o: Order) {
  return o.items.reduce((n, it) => n + it.quantity, 0)
}

function formatTime(t?: string) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 19)
}

async function contactMerchant(order: Order) {
  const productId = order.items?.[0]?.productId
  if (!productId) {
    ElMessage.warning('订单中没有可咨询的商品')
    return
  }
  const conversation = await customerChatApi.open(productId)
  router.push({ path: '/shop/chat', query: { conversationId: String(conversation.id) } })
}

async function pay(order: Order) {
  payingOrderId.value = order.id
  try {
    const checkout = await createMockCheckout(order.id)
    paymentQrCodeImage.value = checkout.qrCodeImage
    paymentAmount.value = centToYuan(checkout.amountCent)
    payingDialogOrderId.value = order.id
    paymentDialogVisible.value = true
    startPaymentPolling()
  } finally {
    payingOrderId.value = null
  }
}

async function cancelOrder(order: Order) {
  try {
    await ElMessageBox.confirm('取消后商品库存会立即释放，已生成的付款码也将失效。确认取消该订单吗？', '取消订单', {
      confirmButtonText: '确认取消',
      cancelButtonText: '暂不取消',
      type: 'warning'
    })
  } catch {
    return
  }

  cancellingOrderId.value = order.id
  try {
    await cancelPendingPaymentOrder(order.id)
    ElMessage.success('订单已取消，商品库存已释放')
    await loadOrders()
  } finally {
    cancellingOrderId.value = null
  }
}

function stopPaymentPolling() {
  if (paymentPollingTimer) {
    clearInterval(paymentPollingTimer)
    paymentPollingTimer = null
  }
}

function startPaymentPolling() {
  stopPaymentPolling()
  paymentPollingTimer = setInterval(async () => {
    const orderId = payingDialogOrderId.value
    if (!orderId) return
    try {
      const status = await paymentStatus(orderId)
      if (!status.paid) return
      stopPaymentPolling()
      paymentDialogVisible.value = false
      await loadOrders()
      ElMessage.success('支付成功，订单已进入待商家确认状态')
    } catch {
      // 网络瞬时失败不打断顾客扫码，下一轮轮询会继续确认状态。
    }
  }, 2000)
}

async function loadOrders() {
  loading.value = true
  try {
    orders.value = await myOrders()
  } finally {
    loading.value = false
  }
}

onMounted(loadOrders)
onUnmounted(stopPaymentPolling)
</script>

<style scoped>
.empty { padding: 60px 0; }

.tabs { display: flex; gap: 8px; margin-bottom: 18px; }
.tab {
  padding: 9px 18px; border-radius: 22px; cursor: pointer; font-size: 14px; color: var(--of-text-2);
  background: var(--of-surface); box-shadow: var(--of-shadow); transition: all 0.2s ease;
  display: flex; align-items: center; gap: 6px;
}
.tab:hover { color: var(--of-text); }
.tab.active {
  background: linear-gradient(120deg, #0f766e, #14b8a6); color: #fff; font-weight: 600;
  box-shadow: 0 8px 18px rgba(13, 148, 136, 0.3);
}
.tab-count { font-size: 12px; opacity: 0.75; }

.order-list { display: grid; gap: 16px; }
.order-card {
  background: var(--of-surface); border-radius: 14px; box-shadow: var(--of-shadow); padding: 18px 22px;
}
.order-head {
  display: flex; align-items: center; justify-content: space-between; padding-bottom: 12px;
  border-bottom: 1px solid var(--of-border);
}
.oh-left { display: flex; flex-direction: column; gap: 4px; }
.order-no { font-size: 14px; font-weight: 600; color: var(--of-text); }
.order-time { font-size: 12px; color: var(--of-text-3); }
.order-store { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #b45309; }
.oh-right { display: flex; align-items: center; gap: 6px; }
.oh-status { font-size: 14px; font-weight: 600; }

.order-items { padding: 12px 0; display: grid; gap: 8px; }
.order-item { display: flex; align-items: center; gap: 16px; font-size: 14px; cursor: pointer; border-radius: 6px; padding: 2px 0; }
.order-item:hover .oi-name { color: #0f766e; }
.oi-name { flex: 1; color: var(--of-text); transition: color 0.15s; }
.oi-qty { color: var(--of-text-2); }
.oi-price { color: var(--of-text-2); width: 90px; text-align: right; }

.order-foot { display: flex; justify-content: flex-end; align-items: center; gap: 14px; padding-top: 10px; border-top: 1px dashed var(--of-border); }
.of-total { font-size: 14px; color: var(--of-text-2); }
.of-total b { color: #ef4444; font-size: 18px; margin-left: 4px; }
.contact-merchant-btn {
  min-width: 112px; color: #fff !important; font-weight: 700;
  background: #2563eb !important; border-color: #2563eb !important;
  box-shadow: 0 4px 10px rgba(37, 99, 235, 0.24);
}
.contact-merchant-btn:hover, .contact-merchant-btn:focus {
  color: #fff !important; background: #1d4ed8 !important; border-color: #1d4ed8 !important;
}
.contact-merchant-btn :deep(.el-icon) { color: #fff; margin-right: 4px; }
.payment-dialog { text-align: center; color: var(--of-text-2); }
.payment-dialog > p:first-child { margin-top: 0; font-size: 14px; }
.payment-qr-code { display: block; width: 280px; height: 280px; margin: 14px auto; border: 8px solid #fff; border-radius: 10px; box-shadow: 0 3px 16px rgba(15, 23, 42, 0.12); }
.payment-amount { color: #ef4444; font-size: 20px; font-weight: 700; margin: 8px 0; }
.payment-hint { color: var(--of-text-3); font-size: 12px; line-height: 1.6; }
</style>
