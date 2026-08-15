<template>
  <div>
    <PageHeader title="租户管理" subtitle="管理入驻商家的启停状态，停用后该租户无法再登录使用">
      <template #actions>
        <el-button type="primary" @click="openDialog">
          <el-icon style="margin-right: 4px"><Plus /></el-icon>新增租户
        </el-button>
      </template>
    </PageHeader>

    <div class="panel">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="tenantCode" label="租户代码" width="150" />
        <el-table-column prop="tenantName" label="租户名称" min-width="180" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderCount" label="订单数" width="120" align="right" />
        <el-table-column label="GMV" width="160" align="right">
          <template #default="{ row }">
            <span class="money">¥ {{ centToYuan(row.gmvCent) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" size="small" type="danger" plain @click="toggle(row, 0)">
              停用
            </el-button>
            <el-button v-else size="small" type="success" plain @click="toggle(row, 1)">
              启用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增租户弹窗 -->
    <el-dialog v-model="dialogVisible" title="新增租户（商家进驻）" width="480px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="96px">
        <el-form-item label="租户代码" prop="tenantCode">
          <el-input v-model="form.tenantCode" placeholder="如 t-c，用于登录与数据隔离" />
        </el-form-item>
        <el-form-item label="租户名称" prop="tenantName">
          <el-input v-model="form.tenantName" placeholder="如 深圳旗舰店" />
        </el-form-item>
        <el-form-item label="管理员账号">
          <el-input v-model="form.adminUsername" placeholder="留空则自动 admin-{租户代码}" />
        </el-form-item>
        <el-form-item label="初始密码">
          <el-input v-model="form.adminPassword" placeholder="留空则默认 admin123" />
        </el-form-item>
      </el-form>
      <div class="dialog-tip">
        <el-icon><InfoFilled /></el-icon>
        创建后该商家即可用管理员账号登录商家后台，上传自己的商品。
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import { tenants, createTenant, setTenantStatus, type TenantStat } from '../../api/platform'
import { centToYuan } from '../../utils/money'

const loading = ref(false)
const list = ref<TenantStat[]>([])
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  tenantCode: '',
  tenantName: '',
  adminUsername: '',
  adminPassword: ''
})
const rules = {
  tenantCode: [{ required: true, message: '请输入租户代码', trigger: 'blur' }],
  tenantName: [{ required: true, message: '请输入租户名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    list.value = await tenants()
  } finally {
    loading.value = false
  }
}

function openDialog() {
  form.tenantCode = ''
  form.tenantName = ''
  form.adminUsername = ''
  form.adminPassword = ''
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate(async (ok: boolean) => {
    if (!ok) return
    submitting.value = true
    try {
      const t = await createTenant({
        tenantCode: form.tenantCode,
        tenantName: form.tenantName,
        adminUsername: form.adminUsername || undefined,
        adminPassword: form.adminPassword || undefined
      })
      ElMessage.success(`租户「${t.tenantName}」已创建，管理员账号：${form.adminUsername || 'admin-' + form.tenantCode}`)
      dialogVisible.value = false
      load()
    } finally {
      submitting.value = false
    }
  })
}

async function toggle(row: TenantStat, target: number) {
  const action = target === 0 ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}租户「${row.tenantName}」？${target === 0 ? '停用后该租户将无法登录。' : ''}`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await setTenantStatus(row.id, target)
  ElMessage.success(`${action}成功`)
  load()
}

onMounted(load)
</script>

<style scoped>
.panel {
  background: var(--of-surface);
  border-radius: var(--of-radius);
  box-shadow: var(--of-shadow);
  padding: 20px;
}
.money { font-weight: 700; color: #0d9488; }
.dialog-tip {
  display: flex; align-items: center; gap: 6px; font-size: 13px; color: #0f766e;
  background: #f0fdfa; padding: 10px 12px; border-radius: 8px; margin-top: 4px;
}
</style>
