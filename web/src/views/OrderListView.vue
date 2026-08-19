<template>
  <div>
    <el-form :inline="true" class="filter">
      <el-form-item label="订单号"><el-input v-model="q.orderNo" placeholder="订单号" clearable /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="q.status" placeholder="全部" clearable style="width:130px">
          <el-option v-for="(v,k) in statusMap" :key="k" :value="k" :label="v.label" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="success" @click="router.push('/orders/new')">+ 创建订单</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column prop="customerName" label="客户名称" />
      <el-table-column label="总金额" width="120">
        <template #default="{ row }">¥{{ centToYuan(row.totalAmountCent) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type || 'info'">{{ statusMap[row.status]?.label || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付信息" min-width="200">
        <template #default="{ row }">
          <template v-if="row.payment">
            <el-tag size="small" :type="paymentStatusType(row.payment.status)">{{ paymentStatusLabel(row.payment.status) }}</el-tag>
            <span v-if="row.payment.paidAt" class="payment-time">{{ row.payment.paidAt }}</span>
            <span v-else class="payment-time">{{ paymentProviderLabel(row.payment.provider) }}</span>
          </template>
          <span v-else class="payment-time">未发起付款</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/orders/${row.id}`)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="mt" background layout="prev,pager,next,total" :total="total"
      :current-page="q.page" :page-size="q.size" @current-change="(p:number)=>load(p)" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { pageOrders } from '../api/order'
import { centToYuan } from '../utils/money'
import { ORDER_STATUS as statusMap } from '../constants/order'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const q = reactive({ page: 1, size: 10, orderNo: '', status: '' as string })

function paymentStatusLabel(status?: string) {
  return ({ PENDING: '待付款', SUCCESS: '已付款', CLOSED: '已关闭', REFUNDED: '已退款' } as Record<string, string>)[status || ''] || status || '未知'
}
function paymentStatusType(status?: string) {
  return ({ PENDING: 'warning', SUCCESS: 'success', CLOSED: 'info', REFUNDED: 'success' } as Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'>)[status || ''] || 'info'
}
function paymentProviderLabel(provider?: string) {
  return provider === 'MOCK' ? '模拟付款（演示）' : provider || '—'
}

async function load(p = q.page) {
  q.page = p
  loading.value = true
  try {
    const r = await pageOrders(q)
    rows.value = r.list || []
    total.value = r.total || 0
  } finally { loading.value = false }
}
function reset() {
  q.orderNo = ''; q.status = ''
  router.replace({ query: {} })
  load(1)
}
watch(
  () => route.query.status,
  (status) => {
    q.status = typeof status === 'string' ? status : ''
    load(1)
  },
  { immediate: true }
)
</script>

<style scoped>
.filter { margin-bottom: 12px; }
.mt { margin-top: 12px; }
.payment-time { margin-left: 8px; color: #909399; font-size: 12px; }
</style>
