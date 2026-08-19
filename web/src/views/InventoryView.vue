<template>
  <div>
    <el-alert v-if="lowStockOnly" title="当前仅显示低库存商品" type="warning" :closable="false" class="low-filter">
      <template #default><el-button link type="primary" @click="showAll">查看全部库存</el-button></template>
    </el-alert>
    <el-table :data="displayRows" v-loading="loading" border>
      <el-table-column prop="productName" label="商品" />
      <el-table-column prop="physicalQuantity" label="实物库存" />
      <el-table-column prop="reservedQuantity" label="已预占" />
      <el-table-column prop="availableQuantity" label="可售库存" />
      <el-table-column label="库存状态" width="110">
        <template #default="{ row }">
          <el-tag v-if="lowSet.has(row.productId)" type="danger">紧张</el-tag>
          <el-tag v-else type="success">正常</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="openAdjust(row)">调整库存</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="调整库存" width="440px">
      <template #header>
        <span>调整库存：{{ current?.productName }}</span>
      </template>
      <el-form :model="adj" label-width="100px">
        <el-form-item label="当前可售">
          <span>{{ current?.availableQuantity }}</span>
        </el-form-item>
        <el-form-item label="调整方式">
          <el-radio-group v-model="adj.type">
            <el-radio value="add">增加</el-radio>
            <el-radio value="sub">减少</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="调整数量">
          <el-input v-model.number="adj.qty" type="number" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { listInventory, adjustInventory, lowStockInventory } from '../api/inventory'

const loading = ref(false)
const route = useRoute()
const router = useRouter()
const rows = ref<any[]>([])
const lowSet = ref<Set<number>>(new Set())
const visible = ref(false)
const submitting = ref(false)
const current = ref<any>(null)
const adj = ref({ type: 'add', qty: 1 })
const lowStockOnly = computed(() => route.query.lowStock === '1')
const displayRows = computed(() => lowStockOnly.value
  ? rows.value.filter((row) => lowSet.value.has(row.productId))
  : rows.value)

async function load() {
  loading.value = true
  try {
    const [inv, low] = await Promise.all([listInventory(), lowStockInventory()])
    rows.value = inv
    lowSet.value = new Set((low || []).map((i: any) => i.productId))
  } finally { loading.value = false }
}
function openAdjust(row: any) {
  current.value = row
  adj.value = { type: 'add', qty: 1 }
  visible.value = true
}
async function submit() {
  if (!adj.value.qty || adj.value.qty < 1) return ElMessage.warning('数量必须为正整数')
  submitting.value = true
  try {
    const change = adj.value.type === 'add' ? adj.value.qty : -adj.value.qty
    await adjustInventory({ productId: current.value.productId, changeQuantity: change })
    ElMessage.success('调整成功')
    visible.value = false
    load()
  } finally {
    submitting.value = false
  }
}
function showAll() {
  router.replace({ path: '/inventories' })
}

watch(() => route.query.lowStock, load)
onMounted(load)
</script>
