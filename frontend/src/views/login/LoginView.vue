<template>
  <div class="login-page">
    <div class="login-bg" :style="{ backgroundImage: `url(${loginBg})` }"></div>
    <div class="login-scrim"></div>

    <div class="login-container">
      <!-- 品牌区 -->
      <div class="brand-block">
        <div class="brand-icon">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="24" cy="24" r="20" stroke="currentColor" stroke-width="2" />
            <path d="M16 24h16M24 16v16" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
          </svg>
        </div>
        <div class="brand-text">
          <div class="brand-name">电脑显示器制造 MES</div>
          <div class="brand-tag">display manufacturing execution system</div>
        </div>
      </div>

      <!-- 登录卡片 -->
      <div class="login-card">
        <div class="card-header">登 录</div>
        <div class="card-divider"></div>

        <form class="login-form" @submit.prevent="handleLogin">
          <div class="field-row">
            <el-icon class="field-icon"><User /></el-icon>
            <input
              v-model="form.username"
              type="text"
              class="field-input"
              placeholder="用户名"
              autocomplete="username"
            />
          </div>
          <div class="field-row">
            <el-icon class="field-icon"><Lock /></el-icon>
            <input
              v-model="form.password"
              type="password"
              class="field-input"
              placeholder="密码"
              autocomplete="current-password"
            />
          </div>
          <button type="submit" class="submit-btn" :disabled="loading">
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </form>

        <div class="card-welcome">欢迎使用电脑显示器制造 MES 平台</div>
        <div class="card-links">
          <a @click.prevent="$router.push('/')">返回首页</a>
          <span class="link-sep">|</span>
          <a @click.prevent="showHint = !showHint">演示账号</a>
        </div>

        <div v-if="showHint" class="demo-accounts">
          <p class="demo-tip">密码均为 123456，点击账号可快速填入</p>
          <div class="demo-list">
            <a
              v-for="item in accountHints"
              :key="item.username"
              class="demo-item"
              @click.prevent="fillAccount(item.username)"
            >
              {{ item.username }} · {{ item.roleName }}
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { mockUsers } from '@/mock/users'
import loginBg from '@picture/login-bg.png'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const showHint = ref(false)
const form = reactive({
  username: '',
  password: '123456'
})

const accountHints = mockUsers.map(({ username, roleName }) => ({ username, roleName }))

onMounted(() => {
  if (route.query.username) {
    form.username = route.query.username
    showHint.value = true
  }
})

function fillAccount(username) {
  form.username = username
  form.password = '123456'
}

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await userStore.login(form)
    ElMessage.success('登录成功')
    router.push(data.dashboardPath || userStore.dashboardPath)
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.login-scrim {
  position: absolute;
  inset: 0;
  background: rgba(0, 20, 40, 0.35);
}

.login-container {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 440px;
  padding: 24px;
}

/* 品牌 */
.brand-block {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
}
.brand-icon {
  width: 52px;
  height: 52px;
  color: #fff;
  flex-shrink: 0;
}
.brand-icon svg {
  width: 100%;
  height: 100%;
}
.brand-name {
  font-size: var(--fs-page-title);
  font-weight: var(--heading-weight);
  color: #fff;
  line-height: var(--lh-heading);
}
.brand-tag {
  font-size: var(--fs-caption);
  font-weight: var(--body-weight-medium);
  color: rgba(255, 255, 255, 0.82);
  margin-top: 4px;
  letter-spacing: var(--ls-card-label);
  text-transform: lowercase;
}

.login-card {
  width: 100%;
  background: rgba(45, 106, 159, 0.88);
  border-radius: 4px;
  padding: 0 0 24px;
  box-shadow: 0 4px 24px rgba(0, 40, 80, 0.25);
}

.card-header {
  padding: 18px 32px 14px;
  text-align: center;
  font-size: var(--fs-body-sm);
  font-weight: var(--body-weight-medium);
  color: #fff;
  letter-spacing: var(--ls-login-header);
}

.card-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.35);
  margin: 0 24px 28px;
}

.login-form {
  padding: 0 32px;
}

.field-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.45);
  padding-bottom: 8px;
}

.field-icon {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.85);
  flex-shrink: 0;
}

.field-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: var(--fs-base);
  font-weight: var(--body-weight);
  font-family: var(--font-sans);
  color: #fff;
  padding: 6px 0;
}
.field-input::placeholder {
  color: rgba(255, 255, 255, 0.55);
}

.submit-btn {
  display: block;
  width: 100%;
  margin-top: 8px;
  padding: 14px 24px;
  background: #fff;
  color: #2d6a9f;
  border: none;
  border-radius: 999px;
  font-size: var(--fs-btn);
  font-weight: var(--btn-weight);
  font-family: var(--font-sans);
  letter-spacing: var(--ls-btn-en);
  cursor: pointer;
  transition: background 0.2s, opacity 0.2s;
}
.submit-btn:hover:not(:disabled) {
  background: #f0f4f8;
}
.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.card-welcome {
  margin-top: 24px;
  text-align: center;
  font-size: var(--fs-caption);
  font-weight: var(--body-weight);
  color: rgba(255, 255, 255, 0.75);
}

.card-links {
  margin-top: 10px;
  text-align: center;
  font-size: var(--fs-caption);
  font-weight: var(--body-weight-medium);
}
.card-links a {
  color: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 2px;
}
.card-links a:hover {
  color: #fff;
}
.link-sep {
  margin: 0 10px;
  color: rgba(255, 255, 255, 0.4);
}

.demo-accounts {
  margin: 16px 24px 0;
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}
.demo-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
}
.demo-tip {
  margin: 0 0 10px;
  font-size: var(--fs-caption);
  font-weight: var(--body-weight);
  color: rgba(255, 255, 255, 0.6);
  text-align: center;
}
.demo-item {
  font-size: var(--fs-caption);
  font-weight: var(--body-weight);
  color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  padding: 3px 8px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 3px;
  text-decoration: none;
}
.demo-item:hover {
  background: rgba(255, 255, 255, 0.12);
}

@media (max-width: 480px) {
  .login-form {
    padding: 0 24px;
  }
  .card-divider {
    margin: 0 20px 24px;
  }
}
</style>
