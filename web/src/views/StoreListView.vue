<template>
  <div>
    <PageHeader title="门店管理" subtitle="管理线下门店，订单与库存可按门店维度归集">
      <template #actions>
        <el-button type="primary" @click="openCreate">+ 新建门店</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="storeCode" label="门店编码" />
        <el-table-column prop="storeName" label="门店名称" />
        <el-table-column prop="province" label="省份" />
        <el-table-column prop="city" label="城市" />
        <el-table-column prop="address" label="地址" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="mt" background layout="prev,pager,next,total" :total="total"
        :current-page="q.page" :page-size="q.size" @current-change="(p:number)=>load(p)" />
    </el-card>

    <el-dialog v-model="dialog" :title="form.id ? '编辑门店' : '新建门店'" width="480px">
      <el-form label-width="80px">
        <el-form-item label="门店编码" v-if="!form.id">
          <el-input v-model="form.storeCode" placeholder="如 ST-BJ" />
        </el-form-item>
        <el-form-item label="门店名称">
          <el-input v-model="form.storeName" placeholder="如 北京旗舰店" />
        </el-form-item>
        <el-form-item label="省份"><el-input v-model="form.province" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="form.city" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="statusOn" active-text="启用" inactive-text="停用" inline-prompt />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageStores, createStore, updateStore } from '../api/store'
import PageHeader from '../components/PageHeader.vue'

const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const q = reactive({ page: 1, size: 50 })
const dialog = ref(false)
const form = reactive<any>({ id: null, storeCode: '', storeName: '', province: '', city: '', address: '', status: 1 })
const statusOn = computed({
  get: () => form.status === 1,
  set: (v: boolean) => (form.status = v ? 1 : 0)
})

async function load(p = q.page) {
  q.page = p
  loading.value = true
  try {
    const r = await pageStores(q)
    rows.value = r.list || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}
function openCreate() {
  Object.assign(form, { id: null, storeCode: '', storeName: '', province: '', city: '', address: '', status: 1 })
  dialog.value = true
}
function openEdit(row: any) {
  Object.assign(form, { id: row.id, storeCode: row.storeCode, storeName: row.storeName, province: row.province, city: row.city, address: row.address, status: row.status })
  dialog.value = true
}
async function save() {
  if (!form.storeName?.trim()) return ElMessage.warning('请输入门店名称')
  saving.value = true
  try {
    if (form.id) {
      await updateStore(form.id, { storeName: form.storeName, province: form.province, city: form.city, address: form.address, status: form.status })
    } else {
      if (!form.storeCode?.trim()) return ElMessage.warning('请输入门店编码')
      await createStore({ storeCode: form.storeCode, storeName: form.storeName, province: form.province, city: form.city, address: form.address, status: form.status })
    }
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}
onMounted(() => load(1))
</script>

<style scoped>
.mt { margin-top: 14px; }
</style>
