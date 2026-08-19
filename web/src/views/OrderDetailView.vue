<template>
  <div v-loading="loading">
    <el-page-header @back="router.back()" title="返回">
      <template #content><span>订单详情 {{ order?.orderNo }}</span></template>
    </el-page-header>

    <el-card class="mt" v-if="order">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[order.status]?.type || 'info'">{{ statusMap[order.status]?.label }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ order.customerName }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ centToYuan(order.totalAmountCent) }}</el-descriptions-item>
        <el-descriptions-item label="优惠活动" v-if="order.promoCode">
          <el-tag type="warning" effect="plain">{{ order.promoCode }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="优惠减免" v-if="order.discountAmountCent">
          <span style="color:#f5222d;font-weight:600">-¥{{ centToYuan(order.discountAmountCent) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ order.createdAt }}</el-descriptions-item>
      </el-descriptions>

      <!-- 状态机步骤条 -->
      <el-steps :active="stepActive" :process-status="stepProcessStatus" finish-status="success" class="mt">
        <el-step v-for="s in steps" :key="s.status" :title="s.label" />
      </el-steps>
      <el-alert v-if="order.status === 'CANCELLED'" class="mt" type="error" :closable="false"
        title="该订单已取消，预占库存已释放" />

      <el-table :data="order.items || []" border class="mt">
        <el-table-column prop="productCode" label="商品编码" />
        <el-table-column prop="productName" label="商品名称" />
        <el-table-column label="单价(元)" width="110">
          <template #default="{ row }">{{ centToYuan(row.unitPriceCent) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column label="小计(元)" width="110">
          <template #default="{ row }">{{ centToYuan(row.lineAmountCent) }}</template>
        </el-table-column>
      </el-table>

      <div class="actions" v-if="actions.length">
        <el-button v-for="a in actions" :key="a" :type="a === 'cancel' ? 'danger' : 'primary'"
          @click="doTransition(a)">{{ TRANSITION_LABEL[a] }}</el-button>
      </div>
      <el-alert v-else class="mt" title="当前状态无可用操作" type="info" :closable="false" />
    </el-card>

    <!-- 状态流转历史 -->
    <el-card class="mt" shadow="never" v-if="history.length">
      <template #header>状态流转历史</template>
      <el-timeline>
        <el-timeline-item v-for="(h, i) in history" :key="i" :timestamp="h.createdAt" placement="top">
          <span class="hist">
            <el-tag size="small" :type="statusMap[h.fromStatus]?.type || 'info'" v-if="h.fromStatus">
              {{ statusMap[h.fromStatus]?.label }}
            </el-tag>
            <el-icon v-if="h.fromStatus"><Right /></el-icon>
            <el-tag size="small" :type="statusMap[h.toStatus]?.type || 'info'">{{ statusMap[h.toStatus]?.label }}</el-tag>
            <span class="remark">（{{ h.remark }}）</span>
          </span>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrder, transitionOrder, getOrderHistory } from '../api/order'
import { centToYuan } from '../utils/money'
import { ORDER_STATUS as statusMap, TRANSITIONS, TRANSITION_LABEL } from '../constants/order'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const order = ref<any>(null)
const history = ref<any[]>([])

const steps = computed(() => [
  { status: 'PENDING_PAYMENT', label: '待付款' },
  { status: 'PAID', label: '已付款' },
  { status: 'CONFIRMED', label: '已确认' },
  { status: 'SHIPPED', label: '已发货' },
  { status: 'COMPLETED', label: '已完成' }
])
const stepActive = computed(() => {
  if (!order.value) return 0
  if (order.value.status === 'CANCELLED') return -1
  return Math.max(0, steps.value.findIndex(s => s.status === order.value.status))
})
const stepProcessStatus = computed(() => (order.value?.status === 'CANCELLED' ? 'error' : 'process'))
const actions = computed(() => (order.value ? TRANSITIONS[order.value.status] || [] : []))

async function load() {
  loading.value = true
  try {
    const [o, h] = await Promise.all([getOrder(Number(route.params.id)), getOrderHistory(Number(route.params.id))])
    order.value = o
    history.value = h || []
  } finally { loading.value = false }
}
async function doTransition(action: 'confirm' | 'ship' | 'complete' | 'cancel') {
  await ElMessageBox.confirm(`确认对订单 ${order.value.orderNo} 执行「${TRANSITION_LABEL[action]}」？`, '提示', { type: 'warning' })
  try {
    await transitionOrder(order.value.id, action)
    ElMessage.success('操作成功')
    load()
  } catch { /* 后端已拦截非法流转并提示 */ }
}
onMounted(load)
</script>

<style scoped>
.mt { margin-top: 16px; }
.actions { margin-top: 16px; display: flex; gap: 12px; }
.hist { display: inline-flex; align-items: center; gap: 6px; }
.remark { color: #888; margin-left: 4px; }
</style>
