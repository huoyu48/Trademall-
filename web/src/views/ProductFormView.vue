<template>
  <el-card>
    <template #header>{{ isEdit ? '编辑商品' : '新建商品' }}</template>
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" style="max-width:520px">
      <el-form-item label="商品编码" prop="productCode">
        <el-input v-model="form.productCode" :disabled="isEdit" placeholder="如 P1001" />
      </el-form-item>
      <el-form-item label="商品名称" prop="productName">
        <el-input v-model="form.productName" placeholder="如 精品咖啡豆" />
      </el-form-item>
      <el-form-item label="单价(元)" prop="priceYuan">
        <el-input v-model="form.priceYuan" placeholder="如 39.90" />
      </el-form-item>
      <el-form-item label="商品分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:260px">
          <el-option v-for="category in categories" :key="category.id" :value="category.id" :label="category.categoryName" />
        </el-select>
      </el-form-item>
      <el-form-item label="所属店铺" prop="storeId">
        <el-select v-model="form.storeId" placeholder="请选择店铺" style="width:260px">
          <el-option v-for="store in stores" :key="store.id" :value="store.id" :label="store.storeName" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status" style="width:160px">
          <el-option :value="1" label="启用" /><el-option :value="0" label="停用" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">保存</el-button>
        <el-button @click="router.back()">返回</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createProduct, updateProduct, getProduct } from '../api/product'
import { listAllCategories } from '../api/category'
import { listAllStores } from '../api/store'
import { yuanToCent } from '../utils/money'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const id = route.params.id ? Number(route.params.id) : null
const isEdit = computed(() => id !== null)
const form = reactive({ productCode: '', productName: '', priceYuan: '', categoryId: undefined as number | undefined, storeId: undefined as number | undefined, status: 1, remark: '' })
const categories = ref<any[]>([])
const stores = ref<any[]>([])
const rules = {
  productCode: [{ required: true, message: '请输入商品编码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  priceYuan: [{ required: true, message: '请输入单价', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  storeId: [{ required: true, message: '请选择所属店铺', trigger: 'change' }],
  status: [{ required: true }]
}

onMounted(async () => {
  const [categoryList, storeList] = await Promise.all([listAllCategories(), listAllStores()])
  categories.value = categoryList.filter((category: any) => category.status === 1)
  stores.value = storeList.filter((store: any) => store.status === 1)
  if (isEdit.value) {
    const p = await getProduct(id!)
    form.productCode = p.productCode
    form.productName = p.productName
    form.priceYuan = (p.unitPriceCent / 100).toFixed(2)
    form.categoryId = p.categoryId
    form.storeId = p.storeId
    form.status = p.status
  }
})

async function submit() {
  await formRef.value.validate(async (ok: boolean) => {
    if (!ok) return
    loading.value = true
    const data = {
      productCode: form.productCode,
      productName: form.productName,
      unitPriceCent: yuanToCent(form.priceYuan),
      categoryId: form.categoryId,
      storeId: form.storeId,
      status: form.status,
      remark: form.remark
    }
    try {
      if (isEdit.value) await updateProduct(id!, data)
      else await createProduct(data)
      ElMessage.success('保存成功')
      router.push('/products')
    } finally {
      loading.value = false
    }
  })
}
</script>
