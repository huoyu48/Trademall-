<template>
  <div class="login">
    <!-- 品牌展示区 -->
    <aside class="login-brand">
      <div class="brand-top">
        <div class="brand-logo">O</div>
        <span class="brand-name">OrderFlow</span>
      </div>
      <h1 class="brand-headline">多租户订单履约平台</h1>
      <p class="brand-desc">一套系统，服务多个商家：商家管订单、顾客下订单、平台治理全局。</p>
      <ul class="brand-feats">
        <li><el-icon><Lock /></el-icon> 分布式锁防超卖，高并发不丢单</li>
        <li><el-icon><Connection /></el-icon> RabbitMQ 异步通知 + 死信兜底</li>
        <li><el-icon><Grid /></el-icon> 多租户数据隔离，一套系统服务多商家</li>
      </ul>
      <div class="brand-glow" />
    </aside>

    <!-- 表单区 -->
    <main class="login-form-wrap">
      <div class="login-card of-fade-up">
        <h2 class="form-title">登录 OrderFlow</h2>
        <p class="form-sub">选择你的身份，进入对应工作台</p>

        <!-- 身份选择 -->
        <div class="role-tabs">
          <button
            v-for="r in roleList"
            :key="r.key"
            type="button"
            class="role-tab"
            :class="{ active: activeRole === r.key }"
            @click="switchRole(r.key)"
          >
            <el-icon><component :is="r.icon" /></el-icon>
            <span>{{ r.label }}</span>
          </button>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="onSubmit">
          <el-form-item prop="username">
            <el-input v-model="form.username" size="large" placeholder="用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password
                      placeholder="密码" :prefix-icon="Lock" @keyup.enter="onSubmit" />
          </el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="submit" @click="onSubmit">
            登 录
          </el-button>
        </el-form>

        <div v-if="activeRole === 'customer'" class="register-entry">
          还没有账号？<el-button link type="primary" @click="registerVisible = true">立即注册</el-button>
        </div>

        <div class="demo-hint">
          <span class="demo-label">演示账号（点击填入）</span>
          <code class="demo-click" @click="fillDemo">{{ activeRoleMeta.username }}</code>
          <span> / </span>
          <code class="demo-click" @click="fillDemo">{{ activeRoleMeta.password }}</code>
        </div>
      </div>

      <el-dialog v-model="registerVisible" title="注册顾客账号" width="400px" :close-on-click-modal="false">
        <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top"
                 @submit.prevent="onRegister">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="registerForm.username" placeholder="3-32 位字母、数字或下划线" />
          </el-form-item>
          <el-form-item label="昵称（选填）" prop="nickname">
            <el-input v-model="registerForm.nickname" placeholder="默认使用用户名" />
          </el-form-item>
          <el-form-item label="手机号（选填）" prop="phone">
            <el-input v-model="registerForm.phone" placeholder="仅支持中国大陆手机号" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="registerForm.password" type="password" show-password placeholder="至少 6 位" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="registerForm.confirmPassword" type="password" show-password @keyup.enter="onRegister" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="registerVisible = false">取消</el-button>
          <el-button type="primary" :loading="registering" @click="onRegister">注册并登录</el-button>
        </template>
      </el-dialog>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock, Shop, Monitor, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { usePlatformStore } from '../stores/platform'
import { useCustomerStore } from '../stores/customer'

type RoleKey = 'merchant' | 'platform' | 'customer'

const roleList: { key: RoleKey; label: string; icon: any; username: string; password: string }[] = [
  { key: 'merchant', label: '商家后台', icon: Shop, username: 'admin-a', password: 'admin123' },
  { key: 'platform', label: '平台管理员', icon: Monitor, username: 'platform-admin', password: 'admin123' },
  { key: 'customer', label: '顾客商城', icon: ShoppingCart, username: 'customer01', password: 'admin123' }
]

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const platformStore = usePlatformStore()
const customerStore = useCustomerStore()

const formRef = ref()
const loading = ref(false)
const form = ref({ username: '', password: '' })
const registerVisible = ref(false)
const registerFormRef = ref()
const registering = ref(false)
const registerForm = ref({ username: '', nickname: '', phone: '', password: '', confirmPassword: '' })

function resolveInitialRole(): RoleKey {
  const q = route.query.role as string
  return q === 'platform' || q === 'customer' ? q : 'merchant'
}

const activeRole = ref<RoleKey>(resolveInitialRole())

const activeRoleMeta = computed(() => roleList.find(r => r.key === activeRole.value)!)

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{3,32}$/, message: '用户名需为 3-32 位字母、数字或下划线', trigger: 'blur' }
  ],
  nickname: [{ max: 64, message: '昵称不能超过 64 位', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '请输入正确的中国大陆手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 64, message: '密码长度需为 6-64 位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入密码', trigger: 'blur' }, {
    validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
      callback(value === registerForm.value.password ? undefined : new Error('两次输入的密码不一致'))
    }, trigger: 'blur'
  }]
}

function switchRole(key: RoleKey) {
  activeRole.value = key
}

function fillDemo() {
  form.value.username = activeRoleMeta.value.username
  form.value.password = activeRoleMeta.value.password
}

async function onSubmit() {
  await formRef.value.validate(async (ok: boolean) => {
    if (!ok) return
    loading.value = true
    try {
      const { username, password } = form.value
      if (activeRole.value === 'platform') {
        await platformStore.login(username, password)
        ElMessage.success('登录成功')
        router.replace('/platform')
      } else if (activeRole.value === 'customer') {
        await customerStore.login(username, password)
        ElMessage.success('登录成功')
        router.replace('/shop')
      } else {
        await userStore.login(username, password)
        ElMessage.success('登录成功')
        router.replace('/dashboard')
      }
    } finally {
      loading.value = false
    }
  })
}

async function onRegister() {
  await registerFormRef.value.validate(async (ok: boolean) => {
    if (!ok) return
    registering.value = true
    try {
      await customerStore.register({ ...registerForm.value })
      ElMessage.success('注册成功，已自动登录')
      registerVisible.value = false
      router.replace('/shop')
    } finally {
      registering.value = false
    }
  })
}
</script>

<style scoped>
.login {
  display: flex;
  min-height: 100vh;
}
.login-brand {
  position: relative;
  flex: 1.1;
  padding: 56px 60px;
  color: #fff;
  background: linear-gradient(150deg, #4338ca 0%, #6d28d9 55%, #7c3aed 100%);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.brand-glow {
  position: absolute;
  width: 420px; height: 420px;
  right: -120px; bottom: -140px;
  background: radial-gradient(circle, rgba(199, 210, 254, 0.45), transparent 70%);
  pointer-events: none;
}
.brand-top { display: flex; align-items: center; gap: 12px; }
.brand-logo {
  width: 42px; height: 42px; border-radius: 12px;
  background: rgba(255, 255, 255, 0.18);
  display: flex; align-items: center; justify-content: center;
  font-weight: 800; font-size: 22px;
}
.brand-name { font-size: 22px; font-weight: 700; letter-spacing: 0.5px; }
.brand-headline { font-size: 38px; font-weight: 800; margin: 48px 0 14px; line-height: 1.25; }
.brand-desc { font-size: 15px; color: #ddd6fe; max-width: 380px; line-height: 1.7; }
.brand-feats { list-style: none; padding: 0; margin: 40px 0 0; display: grid; gap: 16px; }
.brand-feats li {
  display: flex; align-items: center; gap: 12px;
  font-size: 14px; color: #ede9fe;
  background: rgba(255, 255, 255, 0.08);
  padding: 14px 16px; border-radius: 12px;
}
.brand-feats .el-icon { color: #c4b5fd; font-size: 18px; }

.login-form-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fb;
}
.login-card {
  width: 400px;
  background: #fff;
  border-radius: 18px;
  padding: 40px 36px;
  box-shadow: 0 20px 50px rgba(31, 41, 55, 0.1);
}
.form-title { margin: 0; font-size: 24px; font-weight: 700; color: #1f2937; }
.form-sub { margin: 8px 0 24px; font-size: 13px; color: #6b7280; }

.role-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  background: #f3f4f6;
  padding: 4px;
  border-radius: 10px;
}
.role-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: none;
  background: transparent;
  padding: 9px 0;
  border-radius: 8px;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.role-tab:hover { color: #4f46e5; }
.role-tab.active {
  background: #fff;
  color: #4f46e5;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.submit { width: 100%; margin-top: 6px; letter-spacing: 4px; font-weight: 600; }
.register-entry { margin-top: 14px; text-align: center; font-size: 13px; color: #6b7280; }
.demo-hint {
  margin-top: 22px;
  text-align: center;
  font-size: 13px;
  color: #6b7280;
}
.demo-label { color: #9ca3af; margin-right: 4px; }
.demo-click {
  background: #f3f4f6;
  color: #4f46e5;
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.demo-click:hover { background: #e0e7ff; }

@media (max-width: 860px) {
  .login-brand { display: none; }
}
</style>
