<template>
  <div>
    <el-form :inline="true" class="filter">
      <el-form-item label="商品编码"><el-input v-model="q.productCode" placeholder="编码" clearable /></el-form-item>
      <el-form-item label="商品名称"><el-input v-model="q.productName" placeholder="名称" clearable /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="q.status" placeholder="全部" clearable style="width:120px">
          <el-option :value="1" label="启用" /><el-option :value="0" label="停用" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="success" v-if="user.isMerchant" @click="goNew">+ 新建商品</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="productCode" label="商品编码" />
      <el-table-column prop="productName" label="商品名称" />
      <el-table-column label="单价(元)" width="110">
        <template #default="{ row }">{{ centToYuan(row.unitPriceCent) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" v-if="user.isMerchant">
        <template #default="{ row }">
          <el-button link type="primary" @click="goEdit(row.id)">编辑</el-button>
          <el-button link type="danger" v-if="row.status === 1" @click="disable(row)">停用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="mt" background layout="prev,pager,next,total" :total="total"
      :current-page="q.page" :page-size="q.size" @current-change="(p:number)=>load(p)" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageProducts, updateProduct } from '../api/product'
import { centToYuan } from '../utils/money'
import { useUserStore } from '../stores/user'

const user = useUserStore()
const router = useRouter()
const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const q = reactive({ page: 1, size: 10, productCode: '', productName: '', status: undefined as number | undefined })

async function load(p = q.page) {
  q.page = p
  loading.value = true
  try {
    const r = await pageProducts(q)
    rows.value = r.list || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}
function reset() {
  q.productCode = ''; q.productName = ''; q.status = undefined
  load(1)
}
function goNew() { router.push('/products/new') }
function goEdit(id: number) { router.push(`/products/${id}/edit`) }
async function disable(row: any) {
  await ElMessageBox.confirm(`确认停用「${row.productName}」？`, '提示', { type: 'warning' })
  await updateProduct(row.id, { status: 0 })
  ElMessage.success('已停用')
  load()
}
onMounted(() => load(1))
</script>

<style scoped>
.filter { margin-bottom: 12px; }
.mt { margin-top: 12px; }
</style>
