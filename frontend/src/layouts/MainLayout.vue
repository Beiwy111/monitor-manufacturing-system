<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="layout-aside">
      <div class="logo">电脑显示器制造 MES</div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1a2332"
        text-color="#c0c4cc"
        active-text-color="#1677ff"
      >
        <el-menu-item :index="userStore.dashboardPath">
          <span>工作台</span>
        </el-menu-item>
        <template v-for="menu in userStore.menus" :key="menu.menuId">
          <el-sub-menu v-if="menu.children && menu.children.length" :index="String(menu.menuId)">
            <template #title>{{ menu.menuName }}</template>
            <el-menu-item
              v-for="child in menu.children"
              :key="child.menuId"
              :index="child.path"
            >
              {{ child.menuName }}
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="header-title">{{ currentTitle }}</div>
        <div class="header-user">
          <span>{{ userStore.displayName }}（{{ userStore.userInfo?.roleName || '未分配角色' }}）</span>
          <el-button link type="primary" @click="goHome">返回首页</el-button>
          <el-button link type="primary" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '工作台')

function goHome() {
  router.push('/')
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.layout-aside {
  background: #1a2332;
  border-right: 1px solid #2d3748;
}
.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  font-weight: var(--nav-weight);
  font-size: var(--fs-body-sm);
  color: #fff;
  border-bottom: 1px solid #2d3748;
}
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  height: 56px;
  padding: 0 20px;
}
.header-title {
  font-size: var(--fs-page-title);
  font-weight: var(--heading-weight);
  line-height: var(--lh-heading);
  color: var(--heading-color);
}
.header-user {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: var(--fs-body-sm);
  font-weight: var(--body-weight);
  color: var(--text-subtle);
}
.layout-main {
  background: #eef1f5;
  padding: 16px;
  overflow: auto;
}
</style>
