<template>
  <main class="payment-page">
    <section class="payment-card" v-loading="loading">
      <template v-if="payment">
        <div class="brand">OrderFlow Mall</div>
        <div class="badge">模拟收银台</div>
        <template v-if="payment.paid">
          <div class="result success">✓</div>
          <h1>模拟付款成功</h1>
          <p class="muted">订单已更新为“已付款，待商家确认”</p>
        </template>
        <template v-else-if="payment.status === 'PENDING'">
          <div class="result wallet">¥</div>
          <h1>确认模拟付款</h1>
          <p class="muted">这是项目演示付款，不会从支付宝或银行卡扣款。</p>
          <div class="amount">¥ {{ centToYuan(payment.amountCent) }}</div>
          <p class="order-no">订单号 {{ payment.orderNo }}</p>
          <el-button type="primary" size="large" class="confirm-btn" :loading="confirming" @click="confirm">
            确认模拟付款
          </el-button>
          <p v-if="payment.expiresAt" class="expiry">付款码有效至 {{ formatTime(payment.expiresAt) }}</p>
        </template>
        <template v-else>
          <div class="result failed">!</div>
          <h1>付款码已失效</h1>
          <p class="muted">请回到电脑端的“我的订单”，重新发起模拟付款。</p>
        </template>
      </template>
      <el-result v-else-if="loadError" icon="error" title="无法打开模拟收银台" :sub-title="loadError" />
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { confirmMockPayment, mockPaymentPage, type MockPaymentPage } from '../api/customer'
import { centToYuan } from '../utils/money'

const route = useRoute()
const token = String(route.query.token || '')
const payment = ref<MockPaymentPage | null>(null)
const loading = ref(true)
const confirming = ref(false)
const loadError = ref('')

function formatTime(value: string) {
  return value.replace('T', ' ').slice(0, 19)
}

async function load() {
  if (!token) {
    loadError.value = '付款码缺失，请返回商城重新扫码。'
    loading.value = false
    return
  }
  try {
    payment.value = await mockPaymentPage(token)
  } catch {
    loadError.value = '付款码不存在、已更新或已失效，请返回商城重新扫码。'
  } finally {
    loading.value = false
  }
}

async function confirm() {
  confirming.value = true
  try {
    payment.value = await confirmMockPayment(token)
    ElMessage.success('模拟付款成功')
  } finally {
    confirming.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.payment-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; background: linear-gradient(145deg, #effaf8, #eef2ff); }
.payment-card { width: min(100%, 400px); min-height: 460px; box-sizing: border-box; padding: 38px 30px; text-align: center; border-radius: 24px; background: #fff; box-shadow: 0 20px 50px rgba(15, 118, 110, 0.16); }
.brand { color: #0f766e; font-weight: 800; font-size: 20px; letter-spacing: .2px; }
.badge { display: inline-block; margin: 14px 0 20px; padding: 5px 12px; border-radius: 999px; color: #0f766e; background: #ccfbf1; font-size: 13px; font-weight: 700; }
.result { width: 74px; height: 74px; margin: 6px auto 18px; display: grid; place-items: center; border-radius: 50%; color: #fff; font-size: 38px; font-weight: 800; }
.success { background: #10b981; }.wallet { background: #6366f1; }.failed { background: #f59e0b; }
h1 { margin: 0 0 10px; color: #1e293b; font-size: 24px; }.muted { color: #64748b; line-height: 1.7; font-size: 14px; }.amount { margin: 24px 0 8px; color: #ef4444; font-size: 36px; font-weight: 800; }.order-no, .expiry { color: #94a3b8; font-size: 12px; }.confirm-btn { width: 100%; height: 48px; margin-top: 22px; font-size: 16px; font-weight: 700; }
</style>
