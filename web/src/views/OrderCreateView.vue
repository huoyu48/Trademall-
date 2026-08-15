<template>
  <el-card>
    <template #header>创建订单</template>
    <el-form label-width="100px" style="max-width:820px">
      <el-form-item label="客户名称" :error="errors.customerName">
        <el-input v-model="customerName" placeholder="客户名称" />
      </el-form-item>

      <el-form-item label="促销码(可选)">
        <el-input v-model="promoCode" placeholder="如 FULL200-30，订单满足门槛自动减免" style="max-width:360px" />
      </el-form-item>

      <el-form-item label="商品明细">
        <el-button @click="addRow">+ 添加商品</el-button>
      </el-form-item>

      <el-table :data="items" border>
        <el-table-column label="商品">
          <template #default="{ row, $index }">
            <el-select v-model="row.productId" filterable placeholder="请选择商品" @change="onSelect">
              <el-option v-for="p in products" :key="p.id" :value="p.id"
                :label="`${p.productCode} - ${p.productName}（可售 ${stockOf(p.id)}）`"
                :disabled="usedIds.includes(p.id) && p.id !== row.productId" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="单价(元)" width="110">
          <template #default="{ row }">{{ centToYuan(priceOf(row.productId)) }}</template>
        </el-table-column>
        <el-table-column label="可售库存" width="110">
          <template #default="{ row }">
            <span :class="row.quantity > stockOf(row.productId) ? 'danger' : ''">{{ stockOf(row.productId) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="140">
          <template #default="{ row, $index }">
            <el-input-number v-model="row.quantity" :min="1" @change="()=>{}" />
          </template>
        </el-table-column>
        <el-table-column label="小计(元)" width="110">
          <template #default="{ row }">{{ centToYuan(priceOf(row.productId) * (row.quantity || 0)) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row, $index }">
            <el-button link type="danger" @click="removeRow($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-form-item label="订单总金额">
        <span class="total">¥{{ centToYuan(totalCent) }}</span>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" :disabled="hasOverStock" @click="submit">提交订单</el-button>
        <el-button @click="router.back()">取消</el-button>
        <el-tag v-if="hasOverStock" type="danger" class="ml">存在商品数量超过可售库存</el-tag>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pageProducts } from '../api/product'
import { listInventory } from '../api/inventory'
import { createOrder } from '../api/order'
import { centToYuan, yuanToCent } from '../utils/money'

const router = useRouter()
const submitting = ref(false)
const customerName = ref('')
const promoCode = ref('')
const errors = reactive({ customerName: '' })
const products = ref<any[]>([])
const stockMap = ref<Record<number, number>>({})
const items = ref<{ productId: number | null; quantity: number }[]>([])
const idempotencyKey = (crypto as any).randomUUID()

const usedIds = computed(() => items.value.map((i) => i.productId).filter((x) => x != null) as number[])
const hasOverStock = computed(() =>
  items.value.some((i) => i.productId != null && (i.quantity || 0) > (stockMap.value[i.productId] ?? 0))
)
const totalCent = computed(() =>
  items.value.reduce((s, i) => s + (priceOf(i.productId) * (i.quantity || 0)), 0)
)

function priceOf(pid: number | null): number {
  const p = products.value.find((x) => x.id === pid)
  return p ? p.unitPriceCent : 0
}
function stockOf(pid: number): number {
  return stockMap.value[pid] ?? 0
}
function addRow() {
  items.value.push({ productId: null, quantity: 1 })
}
function removeRow(i: number) {
  items.value.splice(i, 1)
}
function onSelect() {
  // 同一商品不可重复添加：若已选则提示（由 disabled 选项拦截）
  const ids = items.value.map((i) => i.productId)
  const dup = ids.filter((x) => x != null)
  if (new Set(dup).size !== dup.length) {
    ElMessage.warning('该商品已在明细中')
  }
}

async function submit() {
  errors.customerName = ''
  if (!customerName.value.trim()) { errors.customerName = '请输入客户名称'; return }
  if (items.value.length === 0 || items.value.some((i) => !i.productId || !i.quantity)) {
    return ElMessage.warning('请完善商品明细')
  }
  submitting.value = true
  try {
    if (hasOverStock.value) {
      ElMessage.warning('存在商品数量超过可售库存，请调整')
      return
    }
    const data = {
      customerName: customerName.value,
      promoCode: promoCode.value || undefined,
      items: items.value.map((i) => ({ productId: i.productId as number, quantity: i.quantity }))
    }
    const order = await createOrder(data, idempotencyKey)
    ElMessage.success(`订单创建成功：${order.orderNo}`)
    router.push(`/orders/${order.id}`)
  } finally { submitting.value = false }
}

onMounted(async () => {
  const [ps, inv] = await Promise.all([pageProducts({ page: 1, size: 200, status: 1 }), listInventory()])
  products.value = ps.list || []
  const m: Record<number, number> = {}
  ;(inv || []).forEach((i: any) => (m[i.productId] = i.availableQuantity))
  stockMap.value = m
})
</script>

<style scoped>
.total { font-size: 18px; font-weight: 700; color: #f56c6c; }
.danger { color: #f5222d; font-weight: 700; }
.ml { margin-left: 12px; }
</style>
