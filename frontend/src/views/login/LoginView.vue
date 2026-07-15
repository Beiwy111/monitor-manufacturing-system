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
        <div class="auth-brand__head">
          <BrandLogo :size="76" :show-text="false" variant="default" class="auth-brand__logo" />
          <div class="auth-brand__titles">
            <h1 class="auth-display-title">
              <span class="auth-display-title__brand">{{ BRAND_NAME }}</span><span class="auth-display-title__mes">MES</span>
            </h1>
            <p class="auth-brand__slogan">智造每一行屏幕</p>
          </div>
        </div>
        <div class="auth-brand__kicker">JINGCHENG MES PLATFORM</div>
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

        <p class="auth-panel__welcome">欢迎使用 {{ APP_TITLE }}</p>
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getHomePath } from '@/utils/menuRoutes'
import { homeImages } from '@/config/homeImages'
import BrandLogo from '@/components/brand/BrandLogo.vue'
import { APP_TITLE, BRAND_NAME } from '@/constants/brand'

const heroVideo = homeImages.loginVideo
const heroBg = homeImages.heroBg
const tags = ['生产管理', '质量追溯', '设备监控', '库存仓储']

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

function fillDemo() {
  form.username = 'li_manager'
  form.password = '123456'
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
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    router.push(redirect || userStore.dashboardPath || getHomePath(userStore.roleKey))
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
  font-family: Inter, "HarmonyOS Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif;
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
    rgba(226, 232, 240, 0.72) 0%,
    rgba(219, 228, 240, 0.58) 42%,
    rgba(210, 220, 235, 0.42) 68%,
    rgba(200, 212, 228, 0.28) 100%
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
  color: #172033;
  padding-right: 24px;
}

.auth-brand__head {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.auth-brand__logo :deep(.brand-logo__img) {
  background: transparent;
  width: 76px;
  max-height: 76px;
}

.auth-brand__titles {
  min-width: 0;
}

.auth-display-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 300;
  font-size: clamp(36px, 4.5vw, 52px);
  line-height: 1.15;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.auth-display-title__brand {
  color: #172033;
  font-weight: 400;
}

.auth-display-title__mes {
  font-style: italic;
  font-weight: 500;
  color: #0284c7;
  letter-spacing: 0.05em;
}

.auth-brand__kicker {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: #64748b;
  margin-bottom: 20px;
  font-weight: 400;
}

.auth-brand__slogan {
  margin: 10px 0 0;
  font-family: var(--font-display);
  font-size: clamp(17px, 2vw, 20px);
  font-weight: 300;
  color: #475569;
  line-height: 1.35;
  letter-spacing: 0.08em;
}

.auth-brand__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.auth-brand__tags span {
  padding: 8px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 400;
  color: #64748b;
  background: rgba(255, 255, 255, 0.72);
}

.auth-panel {
  justify-self: end;
  width: 100%;
  max-width: 420px;
  padding: 36px 36px 28px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.06);
}

.auth-panel__label {
  text-align: right;
  font-size: 11px;
  letter-spacing: 0.06em;
  color: #94a3b8;
  margin-bottom: 8px;
  font-weight: 400;
}

.auth-panel__heading {
  margin: 0 0 28px;
  text-align: center;
  font-size: 26px;
  font-weight: 600;
  color: #172033;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.auth-field {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.auth-field:focus-within {
  border-color: #3b5b92;
  box-shadow: 0 0 0 3px rgba(59, 91, 146, 0.12);
  background: #ffffff;
}

.auth-field__icon {
  font-size: 18px;
  color: #94a3b8;
}

.auth-field__input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  color: #172033;
  font-size: 15px;
  font-weight: 400;
  padding: 4px 0;
}

.auth-field__input::placeholder {
  color: #94a3b8;
}

.auth-submit {
  margin-top: 8px;
  width: 100%;
  padding: 14px 24px;
  border: none;
  border-radius: 8px;
  background: #3b5b92;
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.auth-submit:hover:not(:disabled) {
  background: #2f4a78;
}

.auth-submit:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.auth-panel__welcome {
  margin: 24px 0 12px;
  text-align: center;
  font-size: 13px;
  font-weight: 400;
  color: #64748b;
}

.auth-panel__links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.auth-panel__links a {
  color: #71717a;
  cursor: pointer;
  text-decoration: none;
  font-weight: 400;
  transition: color 0.2s;
}

.auth-panel__links a:hover {
  color: #3b5b92;
  text-decoration: none;
}

.auth-panel__sep {
  color: #d4d4d8;
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

  .auth-brand__head {
    flex-direction: column;
    align-items: center;
    gap: 16px;
  }

  .auth-brand__titles {
    text-align: center;
  }

  .auth-brand__tags {
    justify-content: center;
  }

  .auth-panel {
    justify-self: center;
  }
}
</style>
