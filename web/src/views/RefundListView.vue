<template>
  <div>
    <PageHeader title="退款售后" subtitle="处理仅退款与退货退款；退货需经审核、顾客寄回、商家收货后才能退款并回补库存">
      <template #actions>
        <el-button type="primary" @click="openApply">+ 发起退款</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="refundNo" label="退款单号" width="180" />
        <el-table-column prop="orderNo" label="关联订单" width="180" />
        <el-table-column label="售后类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.afterSaleType === 'RETURN_REFUND' ? 'warning' : 'info'">
              {{ row.afterSaleType === 'RETURN_REFUND' ? '退货退款' : '仅退款' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="退款原因" show-overflow-tooltip />
        <el-table-column label="退款金额" width="120">
          <template #default="{ row }">
            <span class="amount">¥{{ centToYuan(row.refundAmountCent) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusMeta[row.status]?.type || 'info'">{{ statusMeta[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="退货物流" min-width="150">
          <template #default="{ row }">
            <span v-if="row.returnTrackingNo">{{ row.returnTrackingNo }}</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="220">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button link type="success" @click="row.afterSaleType === 'RETURN_REFUND' ? approveReturnRequest(row) : approve(row)">
                {{ row.afterSaleType === 'RETURN_REFUND' ? '同意退货' : '确认模拟退款' }}
              </el-button>
              <el-button link type="danger" @click="reject(row)">驳回</el-button>
            </template>
            <el-button v-else-if="row.status === 'RETURNING'" link type="primary" @click="receiveReturnGoods(row)">确认收到退货</el-button>
            <el-button v-else-if="row.status === 'RETURN_RECEIVED'" link type="success" @click="completeReturnPayment(row)">确认退款并入库</el-button>
            <el-button v-else link type="primary" @click="detail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="mt" background layout="prev,pager,next,total" :total="total"
        :current-page="q.page" :page-size="q.size" @current-change="(p:number)=>load(p)" />
    </el-card>

    <el-dialog v-model="applyDialog" title="发起退款" width="440px">
      <el-form label-width="84px">
        <el-form-item label="订单ID">
          <el-input-number v-model="applyOrderId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="退款原因">
          <el-input v-model="applyReason" type="textarea" :rows="3" placeholder="如 客户申请取消订单" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageRefunds, applyRefund, approveRefund, approveReturn, completeReturnRefund, receiveReturn, rejectRefund } from '../api/refund'
import { centToYuan } from '../utils/money'
import PageHeader from '../components/PageHeader.vue'

const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const q = reactive({ page: 1, size: 10 })
const applyDialog = ref(false)
const applyOrderId = ref<number | undefined>(undefined)
const applyReason = ref('')

const statusMeta: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待审核', type: 'warning' },
  RETURN_APPROVED: { label: '待顾客寄回', type: 'warning' },
  RETURNING: { label: '顾客已寄回', type: 'primary' },
  RETURN_RECEIVED: { label: '已收货，待退款', type: 'warning' },
  REJECTED: { label: '已驳回', type: 'danger' },
  REFUNDED: { label: '已退款', type: 'success' }
}

async function load(p = q.page) {
  q.page = p
  loading.value = true
  try {
    const r = await pageRefunds(q)
    rows.value = r.list || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}
function openApply() {
  applyOrderId.value = undefined
  applyReason.value = ''
  applyDialog.value = true
}
async function doApply() {
  if (!applyOrderId.value) return ElMessage.warning('请输入订单ID')
  saving.value = true
  try {
    await applyRefund(applyOrderId.value, applyReason.value)
    ElMessage.success('退款申请已提交')
    applyDialog.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function approve(row: any) {
  await ElMessageBox.confirm(`确认完成模拟退款单 ${row.refundNo}？`, '提示', { type: 'warning' })
  await approveRefund(row.id)
  ElMessage.success('模拟退款已完成')
  load()
}
async function approveReturnRequest(row: any) {
  await ElMessageBox.confirm(`同意退货退款单 ${row.refundNo} 后，顾客将填写寄回物流单号。确认同意吗？`, '同意退货', { type: 'warning' })
  await approveReturn(row.id)
  ElMessage.success('已同意退货，等待顾客寄回商品')
  load()
}
async function receiveReturnGoods(row: any) {
  await ElMessageBox.confirm(`确认已收到退货商品？物流单号：${row.returnTrackingNo}`, '确认收货', { type: 'warning' })
  await receiveReturn(row.id)
  ElMessage.success('已确认收到退货，请继续确认退款并入库')
  load()
}
async function completeReturnPayment(row: any) {
  await ElMessageBox.confirm('确认完成模拟退款并将退回商品加回库存？', '确认退款并入库', { type: 'warning' })
  await completeReturnRefund(row.id)
  ElMessage.success('退款已完成，退回商品已入库')
  load()
}
async function reject(row: any) {
  await ElMessageBox.confirm(`确认驳回退款单 ${row.refundNo}？`, '提示', { type: 'warning' })
  await rejectRefund(row.id)
  ElMessage.success('已驳回')
  load()
}
function detail(row: any) {
  ElMessage.info(`退款单 ${row.refundNo} 当前状态：${statusMeta[row.status]?.label}`)
}
onMounted(() => load(1))
</script>

<style scoped>
.mt { margin-top: 14px; }
.amount { font-weight: 600; color: var(--of-text); }
.muted { color: var(--of-text-3); }
</style>
