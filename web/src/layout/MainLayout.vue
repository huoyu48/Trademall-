<template>
  <el-container class="of-root">
    <el-aside :width="collapse ? '76px' : 'var(--of-sidebar-w)'" class="of-aside">
      <div class="of-brand" :class="{ compact: collapse }">
        <div class="of-logo">O</div>
        <div v-show="!collapse" class="of-brand-text">
          <span class="of-brand-name">OrderFlow</span>
          <span class="of-brand-sub">订单履约平台</span>
        </div>
      </div>

      <el-menu
        :default-active="activePath"
        router
        :collapse="collapse"
        class="of-menu"
        background-color="transparent"
        text-color="#c7d2fe"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <template #title>{{ m.title }}</template>
        </el-menu-item>
      </el-menu>

      <div v-show="!collapse" class="of-aside-foot">
        <span class="dot" /> 系统运行正常
      </div>
    </el-aside>

    <el-container>
      <el-header class="of-header">
        <div class="of-header-left">
          <el-button text class="of-toggle" @click="collapse = !collapse">
            <el-icon :size="20"><Expand v-if="collapse" /><Fold v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">工作台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="of-header-right">
          <el-tag class="of-tenant" effect="plain" type="info">
            <el-icon style="margin-right: 4px"><OfficeBuilding /></el-icon>
            {{ user.tenantName || '—' }}
          </el-tag>
          <el-badge is-dot class="of-bell" type="danger">
            <el-icon :size="18" color="#6b7280"><Bell /></el-icon>
          </el-badge>
          <el-dropdown @command="onCommand">
            <div class="of-user">
              <el-avatar :size="32" class="of-avatar">{{ initial }}</el-avatar>
              <span class="of-username">{{ user.username }}</span>
              <el-icon color="#9ca3af"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="of-main">
        <router-view v-slot="{ Component }">
          <transition name="of-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const user = useUserStore()
const route = useRoute()
const router = useRouter()
const collapse = ref(false)

const menus = [
  { path: '/dashboard', title: '仪表盘', icon: 'Odometer' },
  { path: '/products', title: '商品管理', icon: 'Goods' },
  { path: '/inventories', title: '库存管理', icon: 'Box' },
  { path: '/categories', title: '商品分类', icon: 'Menu' },
  { path: '/stores', title: '门店管理', icon: 'Location' },
  { path: '/promotions', title: '营销活动', icon: 'Tickets' },
  { path: '/orders', title: '订单管理', icon: 'List' },
  { path: '/refunds', title: '退款售后', icon: 'Wallet' }
]

const activePath = computed(() => '/' + (route.path.split('/')[1] || 'dashboard'))
const currentTitle = computed(() => (route.meta.title as string) || '仪表盘')
const initial = computed(() => (user.username || 'U').charAt(0).toUpperCase())

function onCommand(c: string) {
  if (c === 'logout') {
    user.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.of-root { height: 100vh; }

/* 侧边栏 —— 深靛蓝渐变 */
.of-aside {
  background: linear-gradient(180deg, #312e81 0%, #1e1b4b 100%);
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
  overflow: hidden;
}
.of-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 18px;
  height: 72px;
}
.of-brand.compact { justify-content: center; padding: 20px 0; }
.of-logo {
  flex: none;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: linear-gradient(135deg, #818cf8, #c084fc);
  color: #fff;
  font-weight: 800;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(129, 140, 248, 0.5);
}
.of-brand-text { display: flex; flex-direction: column; line-height: 1.2; }
.of-brand-name { color: #fff; font-weight: 700; font-size: 17px; }
.of-brand-sub { color: #a5b4fc; font-size: 11px; margin-top: 2px; }

.of-menu { border-right: none; flex: 1; padding-top: 8px; }
.of-menu :deep(.el-menu-item) {
  position: relative;
  height: 48px;
  margin: 6px 12px;
  border-radius: 11px;
  color: #c7d2fe;
}
.of-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}
.of-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.95), rgba(124, 58, 237, 0.95));
  color: #fff;
  box-shadow: 0 8px 18px rgba(79, 70, 229, 0.45);
}
.of-menu :deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  border-radius: 3px;
  background: #fff;
}

.of-aside-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  color: #a5b4fc;
  font-size: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}
.of-aside-foot .dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: #34d399; box-shadow: 0 0 8px #34d399;
}

/* 顶栏 */
.of-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid var(--of-border);
}
.of-header-left { display: flex; align-items: center; gap: 12px; }
.of-toggle { color: var(--of-text-2); }
.of-header-right { display: flex; align-items: center; gap: 18px; }
.of-tenant {
  border-color: var(--of-border);
  color: var(--of-text-2);
  background: #f8fafc;
}
.of-bell { cursor: pointer; }
.of-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}
.of-avatar {
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  font-weight: 700;
}
.of-username { font-size: 14px; color: var(--of-text); }

.of-main {
  background: var(--of-bg);
  padding: 22px;
  min-height: calc(100vh - 60px);
}
</style>
