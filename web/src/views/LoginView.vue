<template>
  <div class="login">
    <!-- 品牌展示区 -->
    <aside class="login-brand">
      <div class="brand-top">
        <div class="brand-logo">O</div>
        <span class="brand-name">OrderFlow</span>
      </div>
      <h1 class="brand-headline">多租户订单履约平台</h1>
      <p class="brand-desc">从下单、锁库存到异步通知，全程可靠可追溯。</p>
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
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-sub">登录以管理你的订单与库存</p>

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

        <div class="demo-hint">
          <span class="demo-label">演示账号</span>
          <code>admin-a</code> / <code>admin123</code>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const user = useUserStore()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = ref({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate(async (ok: boolean) => {
    if (!ok) return
    loading.value = true
    try {
      await user.login(form.value.username, form.value.password)
      ElMessage.success('登录成功')
      router.replace('/dashboard')
    } finally {
      loading.value = false
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
  width: 380px;
  background: #fff;
  border-radius: 18px;
  padding: 40px 36px;
  box-shadow: 0 20px 50px rgba(31, 41, 55, 0.1);
}
.form-title { margin: 0; font-size: 24px; font-weight: 700; color: #1f2937; }
.form-sub { margin: 8px 0 28px; font-size: 13px; color: #6b7280; }
.submit { width: 100%; margin-top: 6px; letter-spacing: 4px; font-weight: 600; }
.demo-hint {
  margin-top: 22px;
  text-align: center;
  font-size: 13px;
  color: #6b7280;
}
.demo-label { color: #9ca3af; margin-right: 4px; }
.demo-hint code {
  background: #f3f4f6;
  color: #4f46e5;
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 600;
}

@media (max-width: 860px) {
  .login-brand { display: none; }
}
</style>
