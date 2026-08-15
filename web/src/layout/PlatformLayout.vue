<template>
  <el-container class="pf-root">
    <el-aside :width="collapse ? '76px' : 'var(--of-sidebar-w)'" class="pf-aside">
      <div class="pf-brand" :class="{ compact: collapse }">
        <div class="pf-logo">P</div>
        <div v-show="!collapse" class="pf-brand-text">
          <span class="pf-brand-name">OrderFlow</span>
          <span class="pf-brand-sub">平台运营中心</span>
        </div>
      </div>

      <el-menu
        :default-active="activePath"
        router
        :collapse="collapse"
        class="pf-menu"
        background-color="transparent"
        text-color="#bfdbfe"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <template #title>{{ m.title }}</template>
        </el-menu-item>
      </el-menu>

      <div v-show="!collapse" class="pf-aside-foot">
        <span class="dot" /> 平台服务正常
      </div>
    </el-aside>

    <el-container>
      <el-header class="pf-header">
        <div class="pf-header-left">
          <el-button text class="pf-toggle" @click="collapse = !collapse">
            <el-icon :size="20"><Expand v-if="collapse" /><Fold v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/platform' }">平台中心</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="pf-header-right">
          <el-tag effect="plain" type="info" class="pf-role">
            <el-icon style="margin-right: 4px"><Key /></el-icon> PLATFORM_ADMIN
          </el-tag>
          <el-dropdown @command="onCommand">
            <div class="pf-user">
              <el-avatar :size="32" class="pf-avatar">{{ initial }}</el-avatar>
              <span class="pf-username">{{ store.username }}</span>
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

      <el-main class="pf-main">
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
import { usePlatformStore } from '../stores/platform'

const store = usePlatformStore()
const route = useRoute()
const router = useRouter()
const collapse = ref(false)

const menus = [
  { path: '/platform/overview', title: '平台概览', icon: 'DataAnalysis' },
  { path: '/platform/tenants', title: '租户管理', icon: 'OfficeBuilding' }
]

const activePath = computed(() => route.path)
const currentTitle = computed(() => (route.meta.title as string) || '平台概览')
const initial = computed(() => (store.username || 'P').charAt(0).toUpperCase())

function onCommand(c: string) {
  if (c === 'logout') {
    store.logout()
    router.replace('/platform/login')
  }
}
</script>

<style scoped>
.pf-root { height: 100vh; }
.pf-aside {
  background: linear-gradient(180deg, #1e3a8a 0%, #172554 100%);
  display: flex; flex-direction: column; transition: width 0.2s ease; overflow: hidden;
}
.pf-brand { display: flex; align-items: center; gap: 12px; padding: 20px 18px; height: 72px; }
.pf-brand.compact { justify-content: center; padding: 20px 0; }
.pf-logo {
  flex: none; width: 38px; height: 38px; border-radius: 11px;
  background: linear-gradient(135deg, #60a5fa, #818cf8); color: #fff; font-weight: 800; font-size: 20px;
  display: flex; align-items: center; justify-content: center; box-shadow: 0 6px 16px rgba(96, 165, 250, 0.5);
}
.pf-brand-text { display: flex; flex-direction: column; line-height: 1.2; }
.pf-brand-name { color: #fff; font-weight: 700; font-size: 17px; }
.pf-brand-sub { color: #93c5fd; font-size: 11px; margin-top: 2px; }

.pf-menu { border-right: none; flex: 1; padding-top: 8px; }
.pf-menu :deep(.el-menu-item) {
  position: relative; height: 48px; margin: 6px 12px; border-radius: 11px; color: #bfdbfe;
}
.pf-menu :deep(.el-menu-item:hover) { background: rgba(255, 255, 255, 0.08); color: #fff; }
.pf-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.95), rgba(79, 70, 229, 0.95)); color: #fff;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.45);
}
.pf-menu :deep(.el-menu-item.is-active)::before {
  content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%);
  width: 3px; height: 20px; border-radius: 3px; background: #fff;
}
.pf-aside-foot {
  display: flex; align-items: center; gap: 8px; padding: 14px 20px; color: #93c5fd; font-size: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}
.pf-aside-foot .dot { width: 7px; height: 7px; border-radius: 50%; background: #34d399; box-shadow: 0 0 8px #34d399; }

.pf-header {
  height: 60px; display: flex; align-items: center; justify-content: space-between; padding: 0 20px;
  background: #fff; border-bottom: 1px solid var(--of-border);
}
.pf-header-left { display: flex; align-items: center; gap: 12px; }
.pf-toggle { color: var(--of-text-2); }
.pf-header-right { display: flex; align-items: center; gap: 18px; }
.pf-role { border-color: var(--of-border); color: var(--of-text-2); background: #f8fafc; }
.pf-user { display: flex; align-items: center; gap: 8px; cursor: pointer; outline: none; }
.pf-avatar { background: linear-gradient(135deg, #2563eb, #4f46e5); color: #fff; font-weight: 700; }
.pf-username { font-size: 14px; color: var(--of-text); }
.pf-main { background: var(--of-bg); padding: 22px; min-height: calc(100vh - 60px); }
</style>
