<template>
  <div>
    <PageHeader title="退款售后" subtitle="仅退款直接处理；退货退款需经过审核、顾客寄回、商家收货和退款四步">
      <template #actions>
        <el-button type="primary" @click="openApply">+ 发起退款</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="refundNo" label="退款单号" width="180" />
        <el-table-column prop="orderNo" label="关联订单" width="180" />
        <el-table-column prop="reason" label="退款原因" show-overflow-tooltip />
        <el-table-column label="售后类型" width="120">
          <template #default="{ row }">{{ typeMeta[row.refundType]?.label || row.refundType }}</template>
        </el-table-column>
        <el-table-column label="退货物流" min-width="180">
          <template #default="{ row }">
            <span v-if="row.returnTrackingNo">{{ row.returnLogisticsCompany }} / {{ row.returnTrackingNo }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
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
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING_REVIEW'">
              <el-button link type="success" @click="approve(row)">{{ row.refundType === 'RETURN_AND_REFUND' ? '同意退货' : '同意退款' }}</el-button>
              <el-button link type="danger" @click="reject(row)">驳回</el-button>
            </template>
            <el-button v-else-if="row.status === 'WAITING_MERCHANT_RECEIPT'" link type="warning" @click="receive(row)">确认收到退货</el-button>
            <el-button v-else-if="row.status === 'REFUNDING'" link type="success" @click="complete(row)">确认退款完成</el-button>
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
import { pageRefunds, applyRefund, approveRefund, rejectRefund, confirmReturnReceived, completeRefund } from '../api/refund'
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
  PENDING_REVIEW: { label: '待审核', type: 'warning' },
  WAITING_CUSTOMER_RETURN: { label: '待顾客寄回', type: 'warning' },
  WAITING_MERCHANT_RECEIPT: { label: '待商家收货', type: 'primary' },
  REFUNDING: { label: '退款处理中', type: 'warning' },
  REJECTED: { label: '已驳回', type: 'danger' },
  REFUNDED: { label: '已退款', type: 'success' }
}

const typeMeta: Record<string, { label: string }> = {
  REFUND_ONLY: { label: '仅退款' },
  RETURN_AND_REFUND: { label: '退货退款' }
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
  const content = row.refundType === 'RETURN_AND_REFUND'
    ? `同意后顾客需要填写退货物流。确认同意售后单 ${row.refundNo} 吗？`
    : `同意后售后单将进入退款处理中。确认同意售后单 ${row.refundNo} 吗？`
  await ElMessageBox.confirm(content, '处理售后', { type: 'warning' })
  await approveRefund(row.id)
  ElMessage.success(row.refundType === 'RETURN_AND_REFUND' ? '已同意退货，等待顾客寄回' : '已进入退款处理')
  load()
}
async function receive(row: any) {
  await ElMessageBox.confirm(`确认已收到退货单 ${row.refundNo} 的商品？商品将重新入库。`, '确认收货', { type: 'warning' })
  await confirmReturnReceived(row.id)
  ElMessage.success('已确认收货，售后单进入退款处理')
  load()
}
async function complete(row: any) {
  await ElMessageBox.confirm(`确认完成退款单 ${row.refundNo}？订单将变为已退款。`, '确认退款', { type: 'warning' })
  await completeRefund(row.id)
  ElMessage.success('退款已完成')
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
</style>
