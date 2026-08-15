<template>
  <div class="shop-root">
    <header class="shop-header">
      <div class="shop-header-inner">
        <div class="shop-brand" @click="router.push('/shop')">
          <div class="shop-logo">M</div>
          <span class="shop-name">OrderFlow Mall</span>
        </div>

        <nav class="shop-nav">
          <el-menu mode="horizontal" :default-active="activePath" router
                   background-color="transparent" text-color="#475569" active-text-color="#0f766e"
                   :ellipsis="false" class="shop-menu">
            <el-menu-item index="/shop">首页</el-menu-item>
            <el-menu-item index="/shop/orders">我的订单</el-menu-item>
          </el-menu>
        </nav>

        <div class="shop-actions">
          <el-badge :value="cart.count" :hidden="cart.count === 0" class="shop-cart-badge">
            <el-button circle @click="router.push('/shop/cart')">
              <el-icon :size="18"><ShoppingCart /></el-icon>
            </el-button>
          </el-badge>
          <el-dropdown @command="onCommand">
            <div class="shop-user">
              <el-avatar :size="32" class="shop-avatar">{{ initial }}</el-avatar>
              <span class="shop-username">{{ store.username }}</span>
              <el-icon color="#94a3b8"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="orders"><el-icon><Tickets /></el-icon> 我的订单</el-dropdown-item>
                <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon> 退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="shop-main">
      <router-view v-slot="{ Component }">
        <transition name="of-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCustomerStore } from '../stores/customer'
import { useCartStore } from '../stores/cart'

const store = useCustomerStore()
const cart = useCartStore()
const route = useRoute()
const router = useRouter()

const activePath = computed(() => (route.path.startsWith('/shop/orders') ? '/shop/orders' : '/shop'))
const initial = computed(() => (store.username || 'C').charAt(0).toUpperCase())

function onCommand(c: string) {
  if (c === 'logout') {
    store.logout()
    router.replace('/login?role=customer')
  } else if (c === 'orders') {
    router.push('/shop/orders')
  }
}
</script>

<style scoped>
.shop-root { min-height: 100vh; background: var(--of-bg); }
.shop-header {
  position: sticky; top: 0; z-index: 20; background: #fff; border-bottom: 1px solid var(--of-border);
}
.shop-header-inner {
  max-width: 1200px; margin: 0 auto; padding: 0 24px; height: 64px;
  display: flex; align-items: center; gap: 28px;
}
.shop-brand { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.shop-logo {
  width: 36px; height: 36px; border-radius: 10px;
  background: linear-gradient(135deg, #14b8a6, #0d9488); color: #fff; font-weight: 800; font-size: 19px;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 5px 14px rgba(20, 184, 166, 0.4);
}
.shop-name { font-weight: 700; font-size: 17px; color: #0f766e; }
.shop-nav { flex: 1; }
.shop-menu { border-bottom: none; }
.shop-menu :deep(.el-menu-item) { height: 64px; font-size: 15px; }
.shop-actions { display: flex; align-items: center; gap: 18px; }
.shop-cart-badge { cursor: pointer; }
.shop-user { display: flex; align-items: center; gap: 8px; cursor: pointer; outline: none; }
.shop-avatar { background: linear-gradient(135deg, #14b8a6, #0d9488); color: #fff; font-weight: 700; }
.shop-username { font-size: 14px; color: var(--of-text); }
.shop-main { max-width: 1200px; margin: 0 auto; padding: 24px; }
</style>
