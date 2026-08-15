<template>
  <div>
    <PageHeader title="平台概览" subtitle="跨租户汇总全平台经营情况" />

    <div class="stat-grid">
      <StatCard title="入驻租户" :value="overview.tenants" icon="OfficeBuilding" color="#2563eb" tint="rgba(37,99,235,0.12)" />
      <StatCard title="全平台订单" :value="overview.orders" icon="List" color="#7c3aed" tint="rgba(124,58,237,0.12)" />
      <StatCard title="全平台 GMV" :value="'¥ ' + centToYuan(overview.gmvCent)" icon="Money" color="#0d9488" tint="rgba(13,148,136,0.12)" />
    </div>

    <div class="panel">
      <div class="panel-head">
        <h3 class="panel-title">各租户经营概况</h3>
      </div>
      <el-table :data="tenantList" v-loading="loading" stripe>
        <el-table-column prop="tenantCode" label="租户代码" width="140" />
        <el-table-column prop="tenantName" label="租户名称" min-width="160" />
        <el-table-column label="状态" width="110">
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
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageHeader from '../../components/PageHeader.vue'
import StatCard from '../../components/StatCard.vue'
import { overview as fetchOverview, tenants as fetchTenants, type PlatformOverview, type TenantStat } from '../../api/platform'
import { centToYuan } from '../../utils/money'

const loading = ref(false)
const overview = ref<PlatformOverview>({ tenants: 0, orders: 0, gmvCent: 0 })
const tenantList = ref<TenantStat[]>([])

onMounted(async () => {
  loading.value = true
  try {
    const [o, t] = await Promise.all([fetchOverview(), fetchTenants()])
    overview.value = o
    tenantList.value = t
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}
.panel {
  background: var(--of-surface);
  border-radius: var(--of-radius);
  box-shadow: var(--of-shadow);
  padding: 20px;
}
.panel-head { margin-bottom: 16px; }
.panel-title { margin: 0; font-size: 16px; font-weight: 700; color: var(--of-text); }
.money { font-weight: 700; color: #0d9488; }
</style>
