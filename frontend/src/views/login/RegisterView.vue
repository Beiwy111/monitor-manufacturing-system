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
        <p class="auth-brand__slogan">注册后由系统管理员分配角色与权限，分配完成即可登录使用</p>
        <div class="auth-brand__tags">
          <span v-for="tag in tags" :key="tag">{{ tag }}</span>
        </div>
      </aside>

      <div class="auth-panel auth-panel--wide">
        <div class="auth-panel__label">NEW ACCOUNT</div>
        <h2 class="auth-panel__heading">注册账号</h2>

        <form class="auth-form" @submit.prevent="handleRegister">
          <div class="auth-field">
            <el-icon class="auth-field__icon"><User /></el-icon>
            <input
              v-model="form.username"
              type="text"
              class="auth-field__input"
              placeholder="登录用户名（4-20 位字母数字下划线）"
              autocomplete="username"
            />
          </div>
          <div class="auth-field">
            <el-icon class="auth-field__icon"><Lock /></el-icon>
            <input
              v-model="form.password"
              type="password"
              class="auth-field__input"
              placeholder="登录密码（至少 6 位）"
              autocomplete="new-password"
            />
          </div>
          <div class="auth-field">
            <el-icon class="auth-field__icon"><Lock /></el-icon>
            <input
              v-model="form.confirmPassword"
              type="password"
              class="auth-field__input"
              placeholder="确认密码"
              autocomplete="new-password"
            />
          </div>
          <div class="auth-field">
            <el-icon class="auth-field__icon"><Postcard /></el-icon>
            <input
              v-model="form.realName"
              type="text"
              class="auth-field__input"
              placeholder="姓名"
            />
          </div>
          <div class="auth-field">
            <el-icon class="auth-field__icon"><Phone /></el-icon>
            <input
              v-model="form.phone"
              type="text"
              class="auth-field__input"
              placeholder="手机号（选填）"
            />
          </div>
          <div class="auth-field">
            <el-icon class="auth-field__icon"><OfficeBuilding /></el-icon>
            <input
              v-model="form.department"
              type="text"
              class="auth-field__input"
              placeholder="部门（选填）"
            />
          </div>
          <button type="submit" class="auth-submit" :disabled="loading">
            {{ loading ? '提交中...' : '提交注册' }}
          </button>
        </form>

        <p class="auth-panel__welcome">注册成功后请等待管理员在「用户管理」中分配角色并启用账号</p>
        <div class="auth-panel__links">
          <a @click.prevent="$router.push('/login')">已有账号，去登录</a>
          <span class="auth-panel__sep">|</span>
          <a @click.prevent="$router.push('/')">返回首页</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Postcard, Phone, OfficeBuilding } from '@element-plus/icons-vue'
import { register } from '@/api/auth'
import { homeImages } from '@/config/homeImages'

const heroVideo = homeImages.heroVideo
const heroBg = homeImages.heroBg
const tags = ['生产管理', '质量追溯', '设备监控', '库存仓储']

const router = useRouter()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  department: ''
})

async function handleRegister() {
  if (!form.username || !form.password || !form.realName) {
    ElMessage.warning('请填写用户名、密码和姓名')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    const res = await register({
      username: form.username.trim(),
      password: form.password,
      realName: form.realName.trim(),
      phone: form.phone.trim() || undefined,
      department: form.department.trim() || undefined
    })
    ElMessage.success(res?.message || '注册成功，请等待管理员分配角色后登录')
    router.push('/login')
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '注册失败'
    ElMessage.error(msg)
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
  grid-template-columns: minmax(280px, 1fr) minmax(320px, 460px);
  align-items: center;
  gap: 48px;
}

.auth-brand {
  color: #fff;
  padding-right: 24px;
}

.auth-brand__kicker {
  font-size: 13px;
  letter-spacing: 0.28em;
  color: #5eead4;
  margin-bottom: 18px;
}

.auth-brand__title {
  margin: 0;
  line-height: 1.15;
  font-weight: 700;
}

.auth-brand__title-cn {
  display: block;
  font-size: clamp(36px, 5vw, 56px);
  color: #fff;
}

.auth-brand__title-en {
  display: block;
  font-size: clamp(42px, 6vw, 64px);
  color: #2dd4bf;
  letter-spacing: 0.06em;
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

.auth-panel--wide {
  max-width: 460px;
}

.auth-panel__label {
  text-align: right;
  font-size: 11px;
  letter-spacing: 0.22em;
  color: #5eead4;
  margin-bottom: 8px;
}

.auth-panel__heading {
  margin: 0 0 24px;
  text-align: center;
  font-size: 28px;
  font-weight: 600;
  color: #fff;
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
  padding-bottom: 8px;
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
  font-size: 14px;
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
  border-radius: 2px;
  background: #fff;
  color: #0f4c75;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s;
}

.auth-submit:hover:not(:disabled) {
  transform: translateY(-1px);
}

.auth-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.auth-panel__welcome {
  margin: 20px 0 12px;
  text-align: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.65);
  line-height: 1.5;
}

.auth-panel__links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.auth-panel__links a {
  color: #5eead4;
  cursor: pointer;
  text-decoration: none;
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
