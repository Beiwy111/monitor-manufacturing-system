<template>
  <div class="auth-page">
    <video
      class="auth-bg"
      :src="heroVideo"
      :poster="heroBg"
      autoplay
      loop
      muted
      playsinline
      disablePictureInPicture
    />
    <div class="auth-overlay"></div>

    <div class="auth-shell">
      <aside class="auth-brand">
        <div class="auth-brand__kicker">DISPLAY MES PLATFORM</div>
        <h1 class="auth-brand__title">
          <span class="auth-brand__title-cn">电脑显示器</span>
          <span class="auth-brand__title-en">MES</span>
        </h1>
        <p class="auth-brand__slogan">智造每一块屏幕 · 连接订单到交付的全流程</p>
        <div class="auth-brand__tags">
          <span v-for="tag in tags" :key="tag">{{ tag }}</span>
        </div>
      </aside>

      <div class="auth-panel">
        <div class="auth-panel__label">OPERATOR LOGIN</div>
        <h2 class="auth-panel__heading">登录</h2>

        <form class="auth-form" @submit.prevent="handleLogin">
          <div class="auth-field">
            <el-icon class="auth-field__icon"><User /></el-icon>
            <input
              v-model="form.username"
              type="text"
              class="auth-field__input"
              placeholder="用户名"
              autocomplete="username"
            />
          </div>
          <div class="auth-field">
            <el-icon class="auth-field__icon"><Lock /></el-icon>
            <input
              v-model="form.password"
              type="password"
              class="auth-field__input"
              placeholder="密码"
              autocomplete="current-password"
            />
          </div>
          <button type="submit" class="auth-submit" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </form>

        <p class="auth-panel__welcome">欢迎使用电脑显示器制造 MES 平台</p>
        <div class="auth-panel__links">
          <a @click.prevent="$router.push('/')">返回首页</a>
          <span class="auth-panel__sep">|</span>
          <a @click.prevent="$router.push('/register')">注册账号</a>
          <span class="auth-panel__sep">|</span>
          <a @click.prevent="fillDemo">演示账号</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { homeImages } from '@/config/homeImages'

const heroVideo = homeImages.heroVideo
const heroBg = homeImages.heroBg
const tags = ['生产管理', '质量追溯', '设备监控', '库存仓储']

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

function fillDemo() {
  form.username = 'li_manager'
  form.password = 'Mes@2026'
  ElMessage.info('已填入演示账号 li_manager，点击登录即可')
}

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push(userStore.dashboardPath || '/system/board')
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '登录失败'
    if (msg.includes('Network Error') || msg.includes('ECONNREFUSED')) {
      ElMessage.error('无法连接后端，请先启动 backend（8088）和 Redis')
    } else {
      ElMessage.error(msg)
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
}

.auth-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 84% 32%;
  pointer-events: none;
}

.auth-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    105deg,
    rgba(0, 18, 42, 0.92) 0%,
    rgba(0, 24, 52, 0.78) 38%,
    rgba(0, 32, 64, 0.45) 62%,
    rgba(0, 32, 64, 0.28) 100%
  );
}

.auth-shell {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  max-width: 1280px;
  margin: 0 auto;
  padding: 48px 40px;
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(320px, 420px);
  align-items: center;
  gap: 48px;
}

.auth-brand {
  color: #fff;
  padding-right: 24px;
}

.auth-brand__kicker {
  font-size: 12px;
  letter-spacing: 0.28em;
  color: #4ade80;
  margin-bottom: 18px;
  font-weight: 300;
}

.auth-brand__title {
  margin: 0;
  line-height: 1.15;
  font-weight: 400;
}

.auth-brand__title-cn {
  display: block;
  font-size: clamp(36px, 5vw, 56px);
  color: #fff;
}

.auth-brand__title-en {
  display: block;
  font-size: clamp(42px, 6vw, 64px);
  color: #2d8a66;
  letter-spacing: 0.06em;
  font-weight: 300;
}

.auth-brand__slogan {
  margin: 20px 0 28px;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.6;
}

.auth-brand__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.auth-brand__tags span {
  padding: 8px 18px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 999px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(4px);
}

.auth-panel {
  justify-self: end;
  width: 100%;
  max-width: 420px;
  padding: 36px 36px 28px;
  background: rgba(8, 36, 72, 0.72);
  border: 1px solid rgba(94, 234, 212, 0.18);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(12px);
}

.auth-panel__label {
  text-align: right;
  font-size: 11px;
  letter-spacing: 0.22em;
  color: #5eead4;
  margin-bottom: 8px;
}

.auth-panel__heading {
  margin: 0 0 28px;
  text-align: center;
  font-size: 26px;
  font-weight: 400;
  color: #fff;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.auth-field {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.35);
}

.auth-field__icon {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.85);
}

.auth-field__input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  color: #fff;
  font-size: 15px;
  padding: 6px 0;
}

.auth-field__input::placeholder {
  color: rgba(255, 255, 255, 0.45);
}

.auth-submit {
  margin-top: 8px;
  width: 100%;
  padding: 14px 24px;
  border: none;
  border-radius: 10px;
  background: #2d8a66;
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s, background 0.2s;
}

.auth-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  background: #256f52;
}

.auth-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.auth-panel__welcome {
  margin: 24px 0 12px;
  text-align: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.65);
}

.auth-panel__links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.auth-panel__links a {
  color: #4ade80;
  cursor: pointer;
  text-decoration: none;
  font-weight: 300;
}

.auth-panel__links a:hover {
  text-decoration: underline;
}

.auth-panel__sep {
  color: rgba(255, 255, 255, 0.35);
}

@media (max-width: 900px) {
  .auth-shell {
    grid-template-columns: 1fr;
    padding: 32px 20px 48px;
    align-content: center;
  }

  .auth-brand {
    text-align: center;
    padding-right: 0;
  }

  .auth-brand__tags {
    justify-content: center;
  }

  .auth-panel {
    justify-self: center;
  }
}
</style>
