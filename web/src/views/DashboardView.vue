<template>
  <div v-loading="loading" class="dashboard">
    <PageHeader title="仪表盘" subtitle="实时业务概览 · 数据每分钟刷新">
      <template #actions>
        <el-button :icon="Refresh" plain @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <!-- 核心指标 -->
    <div class="stat-grid">
      <StatCard
        title="今日订单"
        :value="stats.todayCount"
        icon="Document"
        color="#4f46e5"
        tint="#eef2ff"
        :trend="trends.orders"
        footer="较昨日"
        :delay="0"
      />
      <StatCard
        title="累计销售额"
        :value="'¥' + centToYuan(stats.totalSalesCent)"
        icon="Money"
        color="#10b981"
        tint="#ecfdf5"
        :trend="trends.sales"
        footer="较昨日"
        :delay="80"
      />
      <StatCard
        title="待处理订单"
        :value="stats.pendingCount"
        icon="Loading"
        color="#f59e0b"
        tint="#fffbeb"
        footer="需尽快跟进"
        :delay="160"
        clickable
        @click="goPendingOrders"
      />
      <StatCard
        title="低库存商品"
        :value="lowStock.length"
        icon="Warning"
        color="#ef4444"
        tint="#fef2f2"
        footer="需补货"
        :delay="240"
        clickable
        @click="goLowStock"
      />
    </div>

    <!-- 图表区 -->
    <el-row :gutter="18" class="chart-row">
      <el-col :xs="24" :lg="9">
        <el-card class="panel" shadow="never">
          <template #header>
            <span class="panel-title">订单状态分布</span>
          </template>
          <div ref="pieRef" class="chart chart--mid" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="15">
        <el-card class="panel" shadow="never">
          <template #header>
            <span class="panel-title">近 7 天订单趋势</span>
          </template>
          <div ref="lineRef" class="chart chart--mid" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="18" class="chart-row">
      <el-col :xs="24" :lg="15">
        <el-card class="panel" shadow="never">
          <template #header>
            <span class="panel-title">近 7 天销售额（元）</span>
          </template>
          <div ref="barRef" class="chart chart--mid" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="9">
        <el-card class="panel" shadow="never">
          <template #header>
            <span class="panel-title">低库存预警</span>
            <el-tag type="danger" size="small" effect="light" class="panel-tag">可售量告急</el-tag>
            <el-button link type="primary" class="panel-link" @click="goLowStock">查看全部</el-button>
          </template>
          <div v-if="lowStock.length" class="low-list">
            <div v-for="p in lowStock" :key="p.productName" class="low-item">
              <div class="low-name">{{ p.productName }}</div>
              <div class="low-bar">
                <div
                  class="low-fill"
                  :class="level(p.availableQuantity)"
                  :style="{ width: ratio(p) + '%' }"
                />
              </div>
              <span class="low-num" :class="level(p.availableQuantity)">{{ p.availableQuantity }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无低库存商品" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import { getOrderStats } from '../api/order'
import { lowStockInventory } from '../api/inventory'
import { ORDER_STATUS } from '../constants/order'
import { centToYuan } from '../utils/money'
import PageHeader from '../components/PageHeader.vue'
import StatCard from '../components/StatCard.vue'

const loading = ref(false)
const router = useRouter()
const lowStock = ref<any[]>([])
const stats = reactive({
  todayCount: 0,
  totalSalesCent: 0,
  pendingCount: 0,
  statusDistribution: [] as { status: string; count: number }[],
  last7Days: [] as { date: string; count: number; amountCent: number }[]
})

const trends = reactive<{
  orders?: { dir: 'up' | 'down' | 'flat'; text: string }
  sales?: { dir: 'up' | 'down' | 'flat'; text: string }
}>({})

const STATUS_COLOR: Record<string, string> = {
  CREATED: '#3b82f6',
  SHIPPED: '#0ea5e9',
  COMPLETED: '#10b981',
  CANCELLED: '#ef4444'
}

const pieRef = ref<HTMLElement>()
const lineRef = ref<HTMLElement>()
const barRef = ref<HTMLElement>()
let pieChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

function ratio(p: any): number {
  const total = p.physicalQuantity || 0
  if (!total) return 0
  return Math.max(4, Math.min(100, Math.round((p.availableQuantity / total) * 100)))
}
function level(q: number): string {
  if (q <= 0) return 'is-danger'
  if (q <= 10) return 'is-warn'
  return 'is-ok'
}

function goPendingOrders() {
  // 仪表盘的“待处理”与统计口径一致：仅指刚创建、尚未由商家确认的订单。
  router.push({ path: '/orders', query: { status: 'CREATED' } })
}

function goLowStock() {
  router.push({ path: '/inventories', query: { lowStock: '1' } })
}

function computeTrend(today: number, yest: number) {
  if (yest === 0) return { dir: today > 0 ? ('up' as const) : ('flat' as const), text: today > 0 ? '新起步' : '—' }
  const diff = today - yest
  const pct = Math.round((diff / yest) * 100)
  return {
    dir: diff > 0 ? ('up' as const) : diff < 0 ? ('down' as const) : ('flat' as const),
    text: (pct > 0 ? '+' : '') + pct + '%'
  }
}

function resizeAll() {
  pieChart?.resize()
  lineChart?.resize()
  barChart?.resize()
}
window.addEventListener('resize', resizeAll)

onMounted(load)

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeAll)
  pieChart?.dispose()
  lineChart?.dispose()
  barChart?.dispose()
})

async function load() {
  loading.value = true
  try {
    const [s, low] = await Promise.all([getOrderStats(), lowStockInventory()])
    lowStock.value = low || []

    stats.todayCount = s.todayCount
    stats.totalSalesCent = s.totalSalesCent
    stats.pendingCount = s.pendingCount
    stats.statusDistribution = s.statusDistribution || []
    stats.last7Days = s.last7Days || []

    const days = stats.last7Days
    const tOrders = days[days.length - 1]?.count ?? 0
    const yOrders = days[days.length - 2]?.count ?? 0
    const tSales = days[days.length - 1]?.amountCent ?? 0
    const ySales = days[days.length - 2]?.amountCent ?? 0
    trends.orders = computeTrend(tOrders, yOrders)
    trends.sales = computeTrend(tSales, ySales)

    await nextTick()
    renderPie(stats.statusDistribution)
    renderLine(days)
    renderBar(days)
  } finally {
    loading.value = false
  }
}

function statusLabel(s: string): string {
  return ORDER_STATUS[s]?.label || s
}

function renderPie(dist: { status: string; count: number }[]) {
  if (!pieRef.value) return
  pieChart = echarts.init(pieRef.value)
  const total = dist.reduce((a, b) => a + b.count, 0)
  pieChart.setOption({
    color: dist.map((d) => STATUS_COLOR[d.status] || '#94a3b8'),
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, icon: 'circle', textStyle: { color: '#6b7280' } },
    title: {
      text: String(total),
      subtext: '订单总数',
      left: 'center',
      top: '42%',
      textStyle: { fontSize: 24, fontWeight: 700, color: '#1f2937' },
      subtextStyle: { fontSize: 12, color: '#9ca3af' }
    },
    series: [{
      type: 'pie',
      radius: ['52%', '74%'],
      center: ['50%', '46%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 6 },
      label: { show: false },
      data: dist.map((d) => ({ name: statusLabel(d.status), value: d.count }))
    }]
  })
}

function renderLine(days: { date: string; count: number }[]) {
  if (!lineRef.value) return
  lineChart = echarts.init(lineRef.value)
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 20, top: 24, bottom: 28 },
    xAxis: { type: 'category', boundaryGap: false, data: days.map((d) => (d.date || '').slice(5)), axisLine: { lineStyle: { color: '#e5e7eb' } }, axisLabel: { color: '#9ca3af' } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f1f3f7' } }, axisLabel: { color: '#9ca3af' } },
    series: [{
      name: '订单数',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      data: days.map((d) => d.count),
      lineStyle: { width: 3, color: '#4f46e5' },
      itemStyle: { color: '#4f46e5', borderWidth: 2, borderColor: '#fff' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(79,70,229,0.32)' },
          { offset: 1, color: 'rgba(79,70,229,0.02)' }
        ])
      }
    }]
  })
}

function renderBar(days: { date: string; count: number; amountCent: number }[]) {
  if (!barRef.value) return
  barChart = echarts.init(barRef.value)
  barChart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v: number) => '¥' + centToYuan(v) },
    grid: { left: 56, right: 20, top: 24, bottom: 28 },
    xAxis: { type: 'category', data: days.map((d) => (d.date || '').slice(5)), axisLine: { lineStyle: { color: '#e5e7eb' } }, axisLabel: { color: '#9ca3af' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f3f7' } }, axisLabel: { color: '#9ca3af' } },
    series: [{
      name: '销售额',
      type: 'bar',
      barWidth: '46%',
      data: days.map((d) => d.amountCent),
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#34d399' },
          { offset: 1, color: '#059669' }
        ])
      }
    }]
  })
}
</script>

<style scoped>
.dashboard { padding: 2px; }
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
.chart-row { margin-top: 18px; }
.panel { border-radius: 14px; }
.panel-title { font-weight: 600; font-size: 15px; }
.panel-tag { margin-left: 8px; }
.panel-link { float: right; padding: 0; }
.chart { width: 100%; height: 300px; }
.chart--mid { height: 280px; }

.low-list { display: grid; gap: 14px; }
.low-item { display: grid; grid-template-columns: 1fr 90px 34px; align-items: center; gap: 12px; }
.low-name { font-size: 14px; color: #374151; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.low-bar { height: 8px; border-radius: 8px; background: #f1f3f7; overflow: hidden; }
.low-fill { height: 100%; border-radius: 8px; transition: width 0.5s ease; }
.low-fill.is-ok { background: linear-gradient(90deg, #34d399, #10b981); }
.low-fill.is-warn { background: linear-gradient(90deg, #fbbf24, #f59e0b); }
.low-fill.is-danger { background: linear-gradient(90deg, #f87171, #ef4444); }
.low-num { font-weight: 700; font-size: 14px; text-align: right; }
.low-num.is-ok { color: #10b981; }
.low-num.is-warn { color: #f59e0b; }
.low-num.is-danger { color: #ef4444; }

@media (max-width: 1100px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 560px) {
  .stat-grid { grid-template-columns: 1fr; }
}
</style>
