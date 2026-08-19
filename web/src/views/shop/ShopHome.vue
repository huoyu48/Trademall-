<template>
  <div class="shop-home">
    <!-- 轮播 Banner -->
    <el-carousel height="260px" class="banner" :interval="4500" arrow="hover">
      <el-carousel-item v-for="b in banners" :key="b.title">
        <div class="banner-slide" :style="{ background: b.gradient }">
          <div class="banner-text">
            <span class="banner-tag">{{ b.tag }}</span>
            <h2 class="banner-title">{{ b.title }}</h2>
            <p class="banner-sub">{{ b.sub }}</p>
            <el-button class="banner-cta" round @click="scrollToGrid">{{ b.cta }}</el-button>
          </div>
          <div class="banner-emoji">{{ b.emoji }}</div>
        </div>
      </el-carousel-item>
    </el-carousel>

    <!-- 搜索框 -->
    <div class="search-bar">
      <el-input v-model="keyword" size="large" placeholder="搜索商品名称或编码…" clearable
                :prefix-icon="Search" @keyup.enter="onSearch" class="search-input" />
      <el-button type="primary" size="large" class="search-btn" @click="onSearch">搜索</el-button>
    </div>

    <!-- 分类导航 -->
    <div class="cat-nav">
      <div class="cat-item" :class="{ active: activeCat === 0 }" @click="selectCat(0)">
        <span class="cat-emoji">🛍️</span>
        <span>全部</span>
      </div>
      <div v-for="c in cats" :key="c.id" class="cat-item" :class="{ active: activeCat === c.id }"
           @click="selectCat(c.id)">
        <span class="cat-emoji">{{ catEmoji(c.categoryName) }}</span>
        <span>{{ c.categoryName }}</span>
      </div>
    </div>

    <!-- 商品网格 -->
    <div class="grid" v-loading="loading">
      <div v-for="(p, idx) in products" :key="p.id" class="card of-fade-up"
           :style="{ animationDelay: idx * 40 + 'ms' }" @click="goDetail(p.id)">
        <div class="card-cover" :style="{ background: categoryStyle(p.categoryName).gradient }">
          <span class="cover-emoji">{{ categoryStyle(p.categoryName).emoji }}</span>
          <span v-if="(p.sales || 0) >= 5000" class="badge badge-hot">热卖</span>
          <span v-else-if="idx < 2" class="badge badge-new">新品</span>
        </div>
        <div class="card-body">
          <div class="card-name">{{ p.productName }}</div>
          <div class="card-store"><el-icon style="margin-right: 3px"><Shop /></el-icon>{{ p.storeName || '官方直营' }}</div>
          <div v-if="p.storePromotionTexts?.length" class="card-promotions">
            <span v-for="text in p.storePromotionTexts.slice(0, 2)" :key="text" class="card-promotion">{{ text }}</span>
          </div>
          <div class="card-meta">
            <el-tag size="small" effect="plain" type="info">{{ p.categoryName || '通用' }}</el-tag>
            <span class="card-sales">已售 {{ formatSales(p.sales) }}</span>
          </div>
          <div class="card-foot">
            <span class="card-price"><i>¥</i>{{ centToYuan(p.unitPriceCent) }}</span>
            <el-button circle type="primary" size="small" @click.stop="addToCart(p)">
              <el-icon><ShoppingCart /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && products.length === 0" description="没有找到相关商品" />

    <div class="pager" v-if="total > size">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="size"
                     :current-page="page" @current-change="onPage" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { products as fetchProducts, categories as fetchCategories } from '../../api/customer'
import { useCustomerStore } from '../../stores/customer'
import { useCartStore } from '../../stores/cart'
import { centToYuan } from '../../utils/money'
import { categoryStyle, formatSales, CATEGORY_STYLE } from '../../utils/product'
import type { Product, Category } from '../../types'

const store = useCustomerStore()
const cart = useCartStore()
const router = useRouter()
const loading = ref(false)
const products = ref<Product[]>([])
const cats = ref<Category[]>([])
const activeCat = ref(0)
const keyword = ref('')
const page = ref(1)
const size = 12
const total = ref(0)

const banners = [
  { tag: '限时特惠', title: '数码焕新季', sub: '全场满 200 减 30，爆款直降', cta: '立即抢购', emoji: '💻', gradient: 'linear-gradient(120deg,#6366f1,#8b5cf6)' },
  { tag: '智能生活', title: '智能家居特惠', sub: '满 500 减 80，让家更聪明', cta: '去逛逛', emoji: '🏠', gradient: 'linear-gradient(120deg,#0ea5e9,#06b6d4)' },
  { tag: '新人专享', title: '首单立减 20 元', sub: '注册即享，全场通用', cta: '领券下单', emoji: '🎁', gradient: 'linear-gradient(120deg,#f59e0b,#f97316)' }
]

const CAT_EMOJI: Record<string, string> = {
  '数码电子': '💻', '手机配件': '📱', '办公设备': '🖨️', '智能家居': '🏠', '影音娱乐': '🎧'
}
function catEmoji(name?: string) {
  return CAT_EMOJI[name || ''] || '🛒'
}

function addToCart(p: Product) {
  cart.add(p, 1)
  ElMessage.success(`已加入购物车：${p.productName}`)
}

function goDetail(id: number) {
  router.push(`/shop/product/${id}`)
}

function scrollToGrid() {
  document.querySelector('.cat-nav')?.scrollIntoView({ behavior: 'smooth' })
}

async function load() {
  loading.value = true
  try {
    const d = await fetchProducts(page.value, size, activeCat.value === 0 ? undefined : activeCat.value, keyword.value || undefined)
    products.value = d.list
    total.value = d.total
  } finally {
    loading.value = false
  }
}

function selectCat(id: number) {
  activeCat.value = id
  page.value = 1
  load()
}

function onSearch() {
  page.value = 1
  load()
}

function onPage(p: number) {
  page.value = p
  load()
}

onMounted(async () => {
  cats.value = await fetchCategories()
  load()
})
</script>

<style scoped>
.shop-home { display: flex; flex-direction: column; gap: 20px; }

.banner { border-radius: 16px; overflow: hidden; }
.banner-slide {
  height: 100%; display: flex; align-items: center; justify-content: space-between;
  padding: 0 60px; position: relative; overflow: hidden;
}
.banner-text { position: relative; z-index: 1; color: #fff; }
.banner-tag {
  display: inline-block; padding: 3px 12px; border-radius: 20px; font-size: 12px; font-weight: 600;
  background: rgba(255, 255, 255, 0.22); margin-bottom: 14px;
}
.banner-title { margin: 0 0 8px; font-size: 34px; font-weight: 800; }
.banner-sub { margin: 0 0 20px; font-size: 15px; color: rgba(255, 255, 255, 0.85); }
.banner-cta { font-weight: 600; }
.banner-emoji {
  font-size: 120px; position: relative; z-index: 1; opacity: 0.9;
  filter: drop-shadow(0 12px 24px rgba(0, 0, 0, 0.25));
}

.search-bar { display: flex; gap: 12px; }
.search-input { flex: 1; }
.search-input :deep(.el-input__wrapper) { border-radius: 24px; padding-left: 16px; }
.search-btn { width: 110px; border-radius: 24px; font-weight: 600; }

.cat-nav {
  display: flex; gap: 12px; overflow-x: auto; padding: 4px 2px; scrollbar-width: none;
}
.cat-nav::-webkit-scrollbar { display: none; }
.cat-item {
  flex: none; display: flex; align-items: center; gap: 8px; padding: 10px 18px; border-radius: 24px;
  background: var(--of-surface); box-shadow: var(--of-shadow); cursor: pointer; font-size: 14px;
  color: var(--of-text-2); transition: all 0.2s ease; white-space: nowrap;
}
.cat-item:hover { color: var(--of-text); transform: translateY(-2px); }
.cat-item.active {
  background: linear-gradient(120deg, #0f766e, #14b8a6); color: #fff; font-weight: 600;
  box-shadow: 0 8px 18px rgba(13, 148, 136, 0.35);
}
.cat-emoji { font-size: 18px; }

.grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); gap: 20px;
}
.card {
  background: var(--of-surface); border-radius: 14px; overflow: hidden;
  box-shadow: var(--of-shadow); cursor: pointer; transition: transform 0.22s ease, box-shadow 0.22s ease;
}
.card:hover { transform: translateY(-6px); box-shadow: var(--of-shadow-lg); }
.card-cover {
  height: 150px; position: relative; display: flex; align-items: center; justify-content: center;
}
.cover-emoji { font-size: 64px; filter: drop-shadow(0 8px 16px rgba(0, 0, 0, 0.2)); }
.badge {
  position: absolute; top: 10px; left: 10px; font-size: 11px; font-weight: 700; color: #fff;
  padding: 3px 10px; border-radius: 20px;
}
.badge-hot { background: linear-gradient(120deg, #ef4444, #f97316); }
.badge-new { background: linear-gradient(120deg, #0ea5e9, #06b6d4); }

.card-body { padding: 14px 16px 16px; }
.card-name {
  font-size: 15px; font-weight: 600; color: var(--of-text); line-height: 1.4;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; min-height: 42px;
}
.card-store {
  display: inline-flex; align-items: center; font-size: 12px; color: #d97706;
  background: #fffbeb; padding: 2px 8px; border-radius: 4px; margin: 6px 0 8px;
}
.card-promotions { display: flex; flex-wrap: wrap; gap: 5px; margin: -2px 0 8px; }
.card-promotion {
  color: #dc2626; background: #fef2f2; border: 1px solid #fecaca; border-radius: 4px;
  padding: 2px 6px; font-size: 11px; font-weight: 600;
}
.card-meta { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.card-sales { font-size: 12px; color: var(--of-text-3); }
.card-foot { display: flex; align-items: center; justify-content: space-between; }
.card-price { font-size: 20px; font-weight: 800; color: #ef4444; }
.card-price i { font-style: normal; font-size: 13px; margin-right: 1px; }

.pager { display: flex; justify-content: center; margin-top: 8px; }
</style>
