<template>
  <!-- 页面最外层容器，用于统一控制上下间距。 -->
  <div class="favorites-page">
    <!-- 页面标题区：展示总数，并提供返回商城的入口。 -->
    <div class="page-title">
      <div>
        <h2>我的收藏</h2>
        <p>共收藏 {{ total }} 件在售商品</p>
      </div>
      <!-- router.push 是前端路由跳转，不会整页刷新。 -->
      <el-button @click="router.push('/shop')">继续逛商城</el-button>
    </div>

    <!--
      v-loading 是 Element Plus 指令：loading=true 时显示加载遮罩。
      v-for 遍历后端返回的 list，每个 item 渲染一张商品卡片。
      :key 帮助 Vue 精确识别哪条数据被新增、删除或移动。
    -->
    <div class="favorite-grid" v-loading="loading">
      <article
        v-for="item in list"
        :key="item.favoriteId"
        class="favorite-card"
      >
        <!-- :style 动态绑定分类对应的渐变背景，@click 处理跳转详情。 -->
        <div
          class="card-cover"
          :style="{ background: categoryStyle(item.categoryName).gradient }"
          @click="goDetail(item.productId)"
        >
          <span class="cover-emoji">
            {{ categoryStyle(item.categoryName).emoji }}
          </span>
          <span class="favorite-time">
            收藏于 {{ formatDate(item.favoritedAt) }}
          </span>
        </div>

        <div class="card-body">
          <h3 @click="goDetail(item.productId)">
            {{ item.productName }}
          </h3>

          <div class="store-name">
            <el-icon><Shop /></el-icon>
            {{ item.storeName || '官方直营' }}
          </div>

          <div class="card-meta">
            <span class="price">
              <i>¥</i>{{ centToYuan(item.unitPriceCent) }}
            </span>
            <span class="sales">已售 {{ formatSales(item.sales) }}</span>
          </div>

          <!-- 卡片操作区：一个按钮操作购物车，一个按钮操作收藏表。 -->
          <div class="card-actions">
            <el-button type="primary" @click="addToCart(item)">
              <el-icon><ShoppingCart /></el-icon>加入购物车
            </el-button>

            <el-button
              :loading="removingId === item.productId"
              @click="cancelFavorite(item)"
            >
              <el-icon><Delete /></el-icon>取消收藏
            </el-button>
          </div>
        </div>
      </article>
    </div>

    <!-- 请求完成且数据为空时，显示空状态，不与 loading 同时出现。 -->
    <el-empty
      v-if="!loading && list.length === 0"
      description="还没有收藏商品"
    >
      <el-button type="primary" @click="router.push('/shop')">
        去逛逛
      </el-button>
    </el-empty>

    <!-- 总数超过一页容量时才显示分页器。 -->
    <div v-if="total > size" class="pager">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="changePage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
// onMounted：组件挂载到页面后执行初始化逻辑。
// ref：创建能够驱动页面更新的响应式状态。
import { onMounted, ref } from 'vue'

// useRouter 返回路由实例，用于在代码中跳转页面。
import { useRouter } from 'vue-router'

// ElMessage 用来在操作成功后弹出短消息。
import { ElMessage } from 'element-plus'
import {
  favoriteProducts,
  removeFavorite,
  type FavoriteProduct
} from '../../api/favorite'
import { useCartStore } from '../../stores/cart'
import { centToYuan } from '../../utils/money'
import { categoryStyle, formatSales } from '../../utils/product'
import type { Product } from '../../types'

const router = useRouter()

// Pinia 购物车 Store，加入商品时会修改全局购物车状态。
const cart = useCartStore()

// 是否正在获取收藏列表。
const loading = ref(false)

// 正在取消收藏的商品 ID，用于只让对应按钮显示 loading。
const removingId = ref<number | null>(null)

// 当前页的收藏商品数组。
const list = ref<FavoriteProduct[]>([])

// 收藏总数，由后端 PageResult.total 返回。
const total = ref(0)

// 当前页码。
const page = ref(1)

// 每页固定显示 12 条，它不需要响应式，因此不使用 ref。
const size = 12

/** 调用后端分页接口，把数据保存到响应式变量。 */
async function load() {
  // 在发起请求前显示页面加载状态。
  loading.value = true
  try {
    // await 等待后端返回 PageResult<FavoriteProduct>。
    const result = await favoriteProducts(page.value, size)

    // 把返回的当前页数据和总数写入 ref，Vue 会自动重新渲染。
    list.value = result.list
    total.value = result.total
  } finally {
    // 即使接口抛出异常，也必须关闭 loading。
    loading.value = false
  }
}

/** 点击卡片后，携带商品 ID 跳转到详情路由。 */
function goDetail(productId: number) {
  router.push(`/shop/product/${productId}`)
}

/** 把收藏 DTO 转换为购物车需要的 Product 对象。 */
function addToCart(item: FavoriteProduct) {
  // FavoriteProduct 与购物车 Product 结构不完全一致，因此显式完成字段转换。
  const product: Product = {
    // 收藏 DTO 使用 productId，通用 Product 使用 id。
    id: item.productId,
    productCode: item.productCode,
    productName: item.productName,
    unitPriceCent: item.unitPriceCent,
    // 收藏 SQL 已经过滤 p.status=1，因此这里可以填入上架状态。
    status: 1,
    categoryId: item.categoryId,
    categoryName: item.categoryName,
    storeId: item.storeId,
    storeName: item.storeName,
    sales: item.sales
  }

  // 第二个参数 1 表示本次加入 1 件。
  cart.add(product, 1)
  ElMessage.success('已加入购物车')
}

/** 取消成功后重新查询，保证列表和总数一致。 */
async function cancelFavorite(item: FavoriteProduct) {
  // 记录当前正在删除哪一条，避免所有卡片同时显示 loading。
  removingId.value = item.productId

  try {
    // 调用 DELETE 接口，后端会根据 JWT 限制只删除当前顾客的记录。
    await removeFavorite(item.productId)
    ElMessage.success('已取消收藏')

    // 删除当前页最后一条时，回到上一页。
    if (list.value.length === 1 && page.value > 1) {
      page.value -= 1
    }

    // 重新从服务器加载，确保前端列表和 MySQL 数据一致。
    await load()
  } finally {
    removingId.value = null
  }
}

function changePage(nextPage: number) {
  // Element Plus 分页器会把用户选择的页码传入这个函数。
  page.value = nextPage

  // 页码变化后立即查询对应页数据。
  load()
}

/** 把后端时间字符串格式化为 YYYY-MM-DD HH:mm，空值显示横线。 */
function formatDate(value: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—'
}

// 页面首次挂载完成后自动执行 load，用户不需要手动点击刷新。
onMounted(load)
</script>

<style scoped>
/* scoped 表示这些样式只影响当前组件，不污染其他页面。 */
.favorites-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title h2 { margin: 0 0 6px; color: var(--of-text); }
.page-title p { margin: 0; color: var(--of-text-3); font-size: 14px; }

/* CSS Grid 在宽屏上每行显示 3 张卡片。 */
.favorite-grid {
  min-height: 160px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.favorite-card {
  overflow: hidden;
  background: var(--of-surface);
  border-radius: 14px;
  box-shadow: var(--of-shadow);
}

.card-cover {
  height: 150px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.cover-emoji { font-size: 68px; }

.favorite-time {
  position: absolute;
  right: 10px;
  bottom: 10px;
  padding: 3px 9px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 11px;
}

.card-body { padding: 15px; }
.card-body h3 { margin: 0 0 8px; cursor: pointer; }

.store-name {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #d97706;
  font-size: 12px;
}

.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16px 0;
}

.price { color: #ef4444; font-size: 22px; font-weight: 800; }
.price i { font-size: 13px; font-style: normal; }
.sales { color: var(--of-text-3); font-size: 12px; }

.card-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.card-actions .el-button { margin: 0; }
.pager { display: flex; justify-content: center; }

/* 响应式布局：平板屏幕改为每行 2 张。 */
@media (max-width: 900px) {
  .favorite-grid { grid-template-columns: repeat(2, 1fr); }
}

/* 手机屏幕改为单列。 */
@media (max-width: 620px) {
  .favorite-grid { grid-template-columns: 1fr; }
}
</style>