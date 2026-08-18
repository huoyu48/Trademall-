import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  // ===== 统一登录页（三端入口：商家 / 平台 / 顾客）=====
  { path: '/login', name: 'login', component: () => import('../views/UnifiedLoginView.vue'), meta: { public: true } },
  // 旧独立登录页地址，重定向到统一页并选中对应身份
  { path: '/platform/login', redirect: { path: '/login', query: { role: 'platform' } } },
  { path: '/shop/login', redirect: { path: '/login', query: { role: 'customer' } } },

  // ===== 商家后台 =====
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { title: '首页仪表盘' } },
      { path: 'products', name: 'products', component: () => import('../views/ProductListView.vue'), meta: { title: '商品管理' } },
      { path: 'products/new', name: 'product-new', component: () => import('../views/ProductFormView.vue'), meta: { title: '新建商品' } },
      { path: 'products/:id/edit', name: 'product-edit', component: () => import('../views/ProductFormView.vue'), meta: { title: '编辑商品' } },
      { path: 'inventories', name: 'inventories', component: () => import('../views/InventoryView.vue'), meta: { title: '库存管理' } },
      { path: 'orders', name: 'orders', component: () => import('../views/OrderListView.vue'), meta: { title: '订单管理' } },
      { path: 'orders/new', name: 'order-new', component: () => import('../views/OrderCreateView.vue'), meta: { title: '创建订单' } },
      { path: 'orders/:id', name: 'order-detail', component: () => import('../views/OrderDetailView.vue'), meta: { title: '订单详情' } },
      { path: 'categories', name: 'categories', component: () => import('../views/CategoryListView.vue'), meta: { title: '商品分类' } },
      { path: 'stores', name: 'stores', component: () => import('../views/StoreListView.vue'), meta: { title: '门店管理' } },
      { path: 'promotions', name: 'promotions', component: () => import('../views/PromotionListView.vue'), meta: { title: '营销活动' } },
      { path: 'refunds', name: 'refunds', component: () => import('../views/RefundListView.vue'), meta: { title: '退款售后' } }
      ,{ path: 'chats', name: 'merchant-chats', component: () => import('../views/ChatListView.vue'), meta: { title: '客户咨询' } }
    ]
  },

  // ===== 平台后台 =====
  {
    path: '/platform',
    component: () => import('../layout/PlatformLayout.vue'),
    redirect: '/platform/overview',
    children: [
      { path: 'overview', name: 'platform-overview', component: () => import('../views/platform/PlatformOverview.vue'), meta: { title: '平台概览' } },
      { path: 'tenants', name: 'platform-tenants', component: () => import('../views/platform/TenantListView.vue'), meta: { title: '租户管理' } }
    ]
  },

  // ===== 顾客商城 =====
  {
    path: '/shop',
    component: () => import('../layout/ShopLayout.vue'),
    redirect: '/shop/home',
    children: [
      { path: 'home', name: 'shop-home', component: () => import('../views/shop/ShopHome.vue'), meta: { title: '商城首页' } },
      { path: 'product/:id', name: 'shop-product', component: () => import('../views/shop/ShopProductDetail.vue'), meta: { title: '商品详情' } },
      { path: 'cart', name: 'shop-cart', component: () => import('../views/shop/ShopCart.vue'), meta: { title: '购物车' } },
      { path: 'orders', name: 'shop-orders', component: () => import('../views/shop/ShopMyOrders.vue'), meta: { title: '我的订单' } }
      ,{ path: 'chat', name: 'shop-chat', component: () => import('../views/shop/ShopChatView.vue'), meta: { title: '咨询消息' } }
    ]
  },

  { path: '/:pathMatch(.*)*', name: 'notfound', component: () => import('../views/NotFoundView.vue') }
]

const router = createRouter({ history: createWebHistory(), routes })

/**
 * 三角色门控：商家 / 平台 / 顾客各持独立 token key，
 * 未登录（或 token 缺失）时跳回对应登录页。
 */
router.beforeEach((to) => {
  if (to.meta.public) return true
  if (to.path.startsWith('/platform')) {
    if (!localStorage.getItem('of_platform_token')) return { path: '/login', query: { role: 'platform' } }
  } else if (to.path.startsWith('/shop')) {
    if (!localStorage.getItem('of_customer_token')) return { path: '/login', query: { role: 'customer' } }
  } else {
    if (!localStorage.getItem('of_token')) return { path: '/login' }
  }
  return true
})

export default router
