<template>
  <div>
    <PageHeader title="商品分类" subtitle="管理商品分类，支撑商品的多维归类与前端筛选">
      <template #actions>
        <el-button type="primary" @click="openCreate">+ 新建分类</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="categoryCode" label="分类编码" />
        <el-table-column prop="categoryName" label="分类名称" />
        <el-table-column prop="sort" label="排序" width="80" />
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

    <el-dialog v-model="dialog" :title="form.id ? '编辑分类' : '新建分类'" width="460px">
      <el-form label-width="92px">
        <el-form-item label="分类编码" v-if="!form.id">
          <el-input v-model="form.categoryCode" placeholder="如 CAT-DIGITAL" />
        </el-form-item>
        <el-form-item label="分类名称">
          <el-input v-model="form.categoryName" placeholder="如 数码电子" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
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
import { pageCategories, createCategory, updateCategory } from '../api/category'
import PageHeader from '../components/PageHeader.vue'

const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const q = reactive({ page: 1, size: 50 })
const dialog = ref(false)
const form = reactive<any>({ id: null, categoryCode: '', categoryName: '', sort: 0, status: 1 })
const statusOn = computed({
  get: () => form.status === 1,
  set: (v: boolean) => (form.status = v ? 1 : 0)
})

async function load(p = q.page) {
  q.page = p
  loading.value = true
  try {
    const r = await pageCategories(q)
    rows.value = r.list || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}
function openCreate() {
  Object.assign(form, { id: null, categoryCode: '', categoryName: '', sort: 0, status: 1 })
  dialog.value = true
}
function openEdit(row: any) {
  Object.assign(form, { id: row.id, categoryCode: row.categoryCode, categoryName: row.categoryName, sort: row.sort, status: row.status })
  dialog.value = true
}
async function save() {
  if (!form.categoryName?.trim()) return ElMessage.warning('请输入分类名称')
  saving.value = true
  try {
    if (form.id) {
      await updateCategory(form.id, { categoryName: form.categoryName, sort: form.sort, status: form.status })
    } else {
      if (!form.categoryCode?.trim()) return ElMessage.warning('请输入分类编码')
      await createCategory({ categoryCode: form.categoryCode, categoryName: form.categoryName, sort: form.sort, status: form.status })
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
