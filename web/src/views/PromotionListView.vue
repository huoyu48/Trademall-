<template>
  <div>
    <PageHeader title="营销活动" subtitle="配置满减 / 优惠券，下单时自动校验门槛并减免订单金额">
      <template #actions>
        <el-button type="primary" @click="openCreate">+ 新建活动</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="promoCode" label="活动编码" />
        <el-table-column prop="promoName" label="活动名称" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.promoType === 'FULL_REDUCTION' ? 'warning' : 'success'">
              {{ row.promoType === 'FULL_REDUCTION' ? '满减' : '优惠券' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="门槛(元)" width="110">
          <template #default="{ row }">{{ centToYuan(row.thresholdCent) }}</template>
        </el-table-column>
        <el-table-column label="减免(元)" width="110">
          <template #default="{ row }">
            <span class="discount">-{{ centToYuan(row.discountAmountCent) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '进行中' : '已停用' }}</el-tag>
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

    <el-dialog v-model="dialog" :title="form.id ? '编辑活动' : '新建活动'" width="480px">
      <el-form label-width="84px">
        <el-form-item label="活动编码" v-if="!form.id">
          <el-input v-model="form.promoCode" placeholder="如 FULL200-30" />
        </el-form-item>
        <el-form-item label="活动名称">
          <el-input v-model="form.promoName" placeholder="如 满200减30" />
        </el-form-item>
        <el-form-item label="活动类型">
          <el-select v-model="form.promoType" style="width: 100%">
            <el-option value="FULL_REDUCTION" label="满减" />
            <el-option value="COUPON" label="优惠券" />
          </el-select>
        </el-form-item>
        <el-form-item label="门槛(元)">
          <el-input-number v-model="thresholdYuan" :min="0" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="减免(元)">
          <el-input-number v-model="discountYuan" :min="0" :precision="2" :step="5" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="statusOn" active-text="进行中" inactive-text="停用" inline-prompt />
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
import { pagePromotions, createPromotion, updatePromotion } from '../api/promotion'
import { centToYuan, yuanToCent } from '../utils/money'
import PageHeader from '../components/PageHeader.vue'

const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const q = reactive({ page: 1, size: 10 })
const dialog = ref(false)
const form = reactive<any>({ id: null, promoCode: '', promoName: '', promoType: 'FULL_REDUCTION', thresholdCent: 0, discountAmountCent: 0, status: 1 })
const thresholdYuan = ref(0)
const discountYuan = ref(0)
const statusOn = computed({
  get: () => form.status === 1,
  set: (v: boolean) => (form.status = v ? 1 : 0)
})

async function load(p = q.page) {
  q.page = p
  loading.value = true
  try {
    const r = await pagePromotions(q)
    rows.value = r.list || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}
function openCreate() {
  Object.assign(form, { id: null, promoCode: '', promoName: '', promoType: 'FULL_REDUCTION', thresholdCent: 0, discountAmountCent: 0, status: 1 })
  thresholdYuan.value = 0
  discountYuan.value = 0
  dialog.value = true
}
function openEdit(row: any) {
  Object.assign(form, { id: row.id, promoCode: row.promoCode, promoName: row.promoName, promoType: row.promoType, thresholdCent: row.thresholdCent, discountAmountCent: row.discountAmountCent, status: row.status })
  thresholdYuan.value = (row.thresholdCent || 0) / 100
  discountYuan.value = (row.discountAmountCent || 0) / 100
  dialog.value = true
}
async function save() {
  if (!form.promoName?.trim()) return ElMessage.warning('请输入活动名称')
  const payload = {
    promoName: form.promoName,
    promoType: form.promoType,
    thresholdCent: yuanToCent(thresholdYuan.value || 0),
    discountAmountCent: yuanToCent(discountYuan.value || 0),
    status: form.status
  }
  saving.value = true
  try {
    if (form.id) {
      await updatePromotion(form.id, payload)
    } else {
      if (!form.promoCode?.trim()) return ElMessage.warning('请输入活动编码')
      await createPromotion({ promoCode: form.promoCode, ...payload })
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
.discount { color: #f5222d; font-weight: 600; }
</style>
