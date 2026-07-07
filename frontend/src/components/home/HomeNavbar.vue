<template>
  <header class="home-navbar">
    <div class="navbar-top">
      <div class="navbar-inner">
        <div class="navbar-brand">电脑显示器制造 MES</div>
        <nav class="navbar-links">
          <a href="#hero" @click.prevent="scrollTo('hero')">首页</a>
          <a href="#products" @click.prevent="scrollTo('products')">核心能力</a>
          <a href="#process" @click.prevent="scrollTo('process')">业务流程</a>
          <a href="#modules" @click.prevent="scrollTo('modules')">功能模块</a>
          <a href="#roles" @click.prevent="scrollTo('roles')">角色工作台</a>
        </nav>
        <a class="login-link" @click="goLogin">登录</a>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function scrollTo(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
}

function goLogin() {
  router.push('/login')
}
</script>

<style scoped>
.home-navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 200;
}
.navbar-top {
  background: #001b3f;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: var(--nav-height);
  display: flex;
  align-items: center;
  padding: 0 32px;
}
.navbar-brand {
  font-size: var(--fs-nav-brand);
  font-weight: var(--brand-weight);
  color: #fff;
  margin-right: 56px;
  white-space: nowrap;
  letter-spacing: -0.2px;
}
.navbar-links {
  flex: 1;
  display: flex;
  gap: 32px;
}
.navbar-links a {
  color: rgba(255, 255, 255, 0.78);
  text-decoration: none;
  font-size: var(--fs-nav);
  font-weight: var(--nav-weight);
  cursor: pointer;
  transition: color 0.15s;
}
.navbar-links a:hover {
  color: #fff;
}
.login-link {
  color: #fff;
  font-size: var(--fs-nav);
  font-weight: var(--nav-weight);
  cursor: pointer;
  text-decoration: none;
  transition: color 0.15s;
}
.login-link:hover {
  color: var(--accent-bright);
}
@media (max-width: 768px) {
  .navbar-inner { padding: 0 20px; }
  .navbar-links { display: none; }
  .navbar-brand { margin-right: 0; }
}
</style>
