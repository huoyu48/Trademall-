<template>
  <div class="pdp" v-loading="loading">
    <div class="pdp-breadcrumb">
      <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
      <span class="sep">/</span>
      <span>商品详情</span>
    </div>

    <div v-if="p" class="pdp-main">
      <!-- 左：封面 -->
      <div class="pdp-cover" :style="{ background: categoryStyle(p.categoryName).gradient }">
        <div class="cover-top">
          <el-tag size="small" class="cover-store" effect="dark" type="warning">
            <el-icon style="margin-right: 2px"><Shop /></el-icon>{{ p.storeName || '官方直营' }}
          </el-tag>
          <span v-if="(p.sales || 0) >= 5000" class="badge-hot">🔥 热卖</span>
        </div>
        <span class="pdp-emoji">{{ categoryStyle(p.categoryName).emoji }}</span>
      </div>

      <!-- 右：信息 -->
      <div class="pdp-info">
        <div class="pdp-tags">
          <el-tag size="small" effect="plain" type="info">{{ p.categoryName || '通用' }}</el-tag>
          <span class="pdp-store"><el-icon><OfficeBuilding /></el-icon> {{ p.storeName || '官方直营' }}</span>
          <span class="pdp-sales">已售 {{ formatSales(p.sales) }}</span>
        </div>
        <h1 class="pdp-name">{{ p.productName }}</h1>
        <div class="pdp-code">商品编码 {{ p.productCode }}</div>

        <div class="pdp-price-box">
          <span class="pdp-price-label">售价</span>
          <span class="pdp-price"><i>¥</i>{{ centToYuan(p.unitPriceCent) }}</span>
        </div>

        <div class="pdp-promo">
          <el-icon color="#0f766e"><Discount /></el-icon>
          <span>{{ p.storeName || '该商家' }}专享：满 200 减 30 · 满 500 减 80 · 新人首单立减 20</span>
        </div>

        <div class="pdp-actions">
          <el-button size="large" class="btn-cart" @click="addToCart">
            <el-icon style="margin-right: 6px"><ShoppingCart /></el-icon>加入购物车
          </el-button>
          <el-button size="large" class="btn-chat" @click="contactMerchant">
            <el-icon style="margin-right: 6px"><ChatDotRound /></el-icon>联系商家
          </el-button>
          <el-button size="large" type="primary" class="btn-buy" @click="buyNow">立即购买</el-button>
        </div>

        <div class="pdp-specs">
          <div class="spec-row"><span>商品分类</span><b>{{ p.categoryName || '通用' }}</b></div>
          <div class="spec-row"><span>所属门店</span><b class="store-highlight">{{ p.storeName || '官方直营' }}</b></div>
          <div class="spec-row"><span>累计销量</span><b>{{ formatSales(p.sales) }} 件</b></div>
          <div class="spec-row"><span>上架时间</span><b>{{ formatTime(p.createdAt) }}</b></div>
        </div>
      </div>
    </div>

    <!-- 相关推荐 -->
    <div v-if="related.length" class="pdp-related">
      <h3 class="related-title">相关推荐</h3>
      <div class="related-grid">
        <div v-for="r in related" :key="r.id" class="rel-card" @click="goToProduct(r.id)">
          <div class="rel-cover" :style="{ background: categoryStyle(r.categoryName).gradient }">
            <span class="rel-emoji">{{ categoryStyle(r.categoryName).emoji }}</span>
            <span class="rel-store">{{ r.storeName || '官方直营' }}</span>
          </div>
          <div class="rel-name">{{ r.productName }}</div>
          <div class="rel-price">¥ {{ centToYuan(r.unitPriceCent) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productDetail, products as fetchProducts } from '../../api/customer'
import { customerChatApi } from '../../api/chat'
import { useCartStore } from '../../stores/cart'
import { centToYuan } from '../../utils/money'
import { categoryStyle, formatSales } from '../../utils/product'
import type { Product } from '../../types'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const loading = ref(false)
const p = ref<Product | null>(null)
const related = ref<Product[]>([])

function formatTime(t?: string) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 10)
}

function addToCart() {
  if (!p.value) return
  cart.add(p.value, 1)
  ElMessage.success('已加入购物车')
}

function buyNow() {
  if (!p.value) return
  cart.add(p.value, 1)
  router.push('/shop/cart')
}

async function contactMerchant() {
  if (!p.value) return
  const conversation = await customerChatApi.open(p.value.id)
  router.push({ path: '/shop/chat', query: { conversationId: String(conversation.id) } })
}

function goToProduct(id: number) {
  // 即使目标商品 id 相同，也走 router.push 让组件实例复用时 watcher 重新触发 load
  router.push(`/shop/product/${id}`).catch(() => {
    /* 防止相同路由重复跳转抛错 */
  })
}

async function load(id: number) {
  loading.value = true
  try {
    p.value = await productDetail(id)
    const d = await fetchProducts(1, 6, p.value.categoryId)
    related.value = d.list.filter((x) => x.id !== id).slice(0, 4)
  } finally {
    loading.value = false
  }
}

// 修复点：URL 参数变化时（同组件复用场景）重新加载数据
watch(
  () => Number(route.params.id),
  (id) => {
    if (!Number.isNaN(id)) load(id)
  },
  { immediate: true }
)
</script>

<style scoped>
.pdp { display: flex; flex-direction: column; gap: 20px; }
.pdp-breadcrumb { display: flex; align-items: center; gap: 8px; color: var(--of-text-3); font-size: 13px; }
.pdp-breadcrumb .sep { color: #d1d5db; }

.pdp-main {
  display: grid; grid-template-columns: 440px 1fr; gap: 40px;
  background: var(--of-surface); border-radius: 16px; box-shadow: var(--of-shadow); padding: 28px;
}
.pdp-cover {
  height: 360px; border-radius: 14px; position: relative;
  display: flex; align-items: center; justify-content: center; overflow: hidden;
}
.cover-top {
  position: absolute; top: 14px; left: 14px; right: 14px;
  display: flex; align-items: center; justify-content: space-between;
}
.cover-store { background: rgba(0, 0, 0, 0.55); border: none; backdrop-filter: blur(4px); }
.badge-hot {
  background: linear-gradient(120deg, #ef4444, #f97316); color: #fff; font-size: 12px; font-weight: 700;
  padding: 4px 12px; border-radius: 20px;
}
.pdp-emoji { font-size: 150px; filter: drop-shadow(0 16px 30px rgba(0, 0, 0, 0.25)); }

.pdp-info { display: flex; flex-direction: column; }
.pdp-tags { display: flex; align-items: center; gap: 12px; }
.pdp-store {
  display: inline-flex; align-items: center; gap: 4px; font-size: 13px;
  color: #d97706; background: #fffbeb; padding: 4px 10px; border-radius: 6px;
}
.pdp-store .el-icon { font-size: 14px; }
.pdp-sales { font-size: 13px; color: var(--of-text-3); }
.pdp-name { margin: 14px 0 8px; font-size: 26px; font-weight: 800; color: var(--of-text); line-height: 1.35; }
.pdp-code { font-size: 13px; color: var(--of-text-3); }

.pdp-price-box {
  margin: 20px 0; padding: 18px 20px; border-radius: 12px;
  background: linear-gradient(120deg, #fef2f2, #fff7ed); display: flex; align-items: baseline; gap: 12px;
}
.pdp-price-label { font-size: 14px; color: var(--of-text-2); }
.pdp-price { font-size: 36px; font-weight: 800; color: #ef4444; }
.pdp-price i { font-style: normal; font-size: 20px; margin-right: 2px; }

.pdp-promo {
  display: flex; align-items: center; gap: 8px; font-size: 13px; color: #0f766e;
  background: #f0fdfa; padding: 10px 14px; border-radius: 10px; margin-bottom: 20px;
}
.pdp-actions { display: flex; gap: 14px; }
.btn-cart { flex: 1; border-color: #0f766e; color: #0f766e; }
.btn-chat { flex: 1; color: #4f46e5; border-color: #a5b4fc; }
.btn-buy { flex: 1; font-weight: 600; }

.pdp-specs {
  margin-top: 26px; border-top: 1px dashed var(--of-border); padding-top: 18px; display: grid; gap: 12px;
}
.spec-row { display: flex; justify-content: space-between; font-size: 13px; color: var(--of-text-2); }
.spec-row b { color: var(--of-text); font-weight: 600; }
.store-highlight { color: #d97706 !important; font-weight: 700 !important; }

.pdp-related { margin-top: 8px; }
.related-title { margin: 0 0 16px; font-size: 18px; font-weight: 700; color: var(--of-text); }
.related-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.rel-card {
  background: var(--of-surface); border-radius: 12px; overflow: hidden; cursor: pointer;
  box-shadow: var(--of-shadow); transition: transform 0.2s ease, box-shadow 0.2s ease;
  user-select: none;
}
.rel-card:hover { transform: translateY(-4px); box-shadow: var(--of-shadow-lg); }
.rel-card:active { transform: translateY(-2px); }
.rel-cover {
  height: 100px; display: flex; align-items: center; justify-content: center; position: relative;
}
.rel-emoji { font-size: 44px; }
.rel-store {
  position: absolute; bottom: 6px; right: 6px; font-size: 11px; color: #fff;
  background: rgba(0, 0, 0, 0.45); padding: 2px 8px; border-radius: 10px;
}
.rel-name {
  padding: 10px 12px 4px; font-size: 13px; font-weight: 600; color: var(--of-text);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.rel-price { padding: 0 12px 12px; font-size: 15px; font-weight: 700; color: #ef4444; }

@media (max-width: 860px) {
  .pdp-main { grid-template-columns: 1fr; }
  .related-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
