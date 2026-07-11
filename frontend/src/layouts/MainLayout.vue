<template>
  <el-container class="layout-container">
    <el-aside width="210px" class="layout-aside">
      <div class="logo">
        <span class="logo__text">MES 制造执行</span>
      </div>
      <div class="ruoyi-sidebar-user">
        <div class="ruoyi-sidebar-user__avatar">{{ avatarText }}</div>
        <div class="ruoyi-sidebar-user__name">{{ userStore.displayName }}</div>
        <div class="ruoyi-sidebar-user__links">
          <a href="#" @click.prevent>在线</a>
          <a href="#" @click.prevent="handleLogout">注销</a>
        </div>
      </div>
      <el-scrollbar class="layout-menu-scroll">
        <el-menu
          class="layout-menu"
          :default-active="activeMenu"
          router
        >
          <el-menu-item :index="homePath">
            <el-icon><Monitor /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <template v-for="menu in userStore.menus" :key="menu.menuId">
            <el-sub-menu v-if="menu.children?.length" :index="String(menu.menuId)">
              <template #title>
                <el-icon><component :is="iconFor(menu.menuCode)" /></el-icon>
                <span>{{ menu.menuName }}</span>
              </template>
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
      </el-scrollbar>
    </el-aside>

    <el-container class="layout-right">
      <el-header class="layout-header">
        <div class="layout-header__left">
          <div class="ruoyi-breadcrumb layout-breadcrumb">
            <router-link :to="homePath" class="ruoyi-breadcrumb__link">首页</router-link>
            <span v-if="breadcrumbModule" class="ruoyi-breadcrumb__sep">/</span>
            <span v-if="breadcrumbModule">{{ breadcrumbModule }}</span>
            <span v-if="currentTitle" class="ruoyi-breadcrumb__sep">/</span>
            <span v-if="currentTitle" class="ruoyi-breadcrumb__current">{{ currentTitle }}</span>
          </div>
        </div>
        <div class="header-user">
          <el-tooltip content="全屏">
            <el-button link class="header-user__btn" @click="toggleFullscreen">
              <el-icon><FullScreen /></el-icon>
            </el-button>
          </el-tooltip>
          <el-dropdown trigger="click" @command="onUserCommand">
            <span class="header-user__trigger">
              <span class="header-user__avatar">{{ avatarText }}</span>
              {{ userStore.displayName }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="portal">返回门户</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <TagsView />
      <el-main class="layout-main ruoyi-app-main" :class="{ 'layout-main--screen': isScreenPage }">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Monitor, Setting, Box, ShoppingCart, OfficeBuilding, CircleCheck,
  Tools, Service, FullScreen, ArrowDown, User
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import TagsView from '@/components/ruoyi/TagsView.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const homePath = computed(() => userStore.dashboardPath)
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')
const isScreenPage = computed(() => route.meta.layout === 'screen')

const breadcrumbModule = computed(() => {
  const p = route.path
  if (p.startsWith('/system')) return '系统管理'
  if (p.startsWith('/order')) return '订单发货'
  if (p.startsWith('/production')) return '生产管理'
  if (p.startsWith('/quality/semi')) return '半成品质量管理'
  if (p.startsWith('/quality/fp'))   return '成品质量管理'
  if (p.startsWith('/quality'))      return '质量管理'
  if (p.startsWith('/warehouse')) return '物料库存'
  if (p.startsWith('/purchase')) return '采购管理'
  if (p.startsWith('/device')) return '设备管理'
  if (p.startsWith('/aftersale') || p.startsWith('/cost')) return '售后成本'
  if (p.startsWith('/delivery')) return '发货管理'
  return ''
})

const avatarText = computed(() => userStore.displayName?.slice(0, 1) || 'U')

const iconMap = {
  system: Setting,
  material: Box,
  order: ShoppingCart,
  production: OfficeBuilding,
  purchase: ShoppingCart,
  quality: CircleCheck,
  equipment: Tools,
  afterSales: Service
}

function iconFor(code) {
  const key = (code || '').split(':')[0]
  return iconMap[key] || User
}

function handleLogout() {
  userStore.logout()
  router.replace('/login')
}

function onUserCommand(cmd) {
  if (cmd === 'portal') router.push('/')
  if (cmd === 'logout') handleLogout()
}

function toggleFullscreen() {
  if (!document.fullscreenElement) document.documentElement.requestFullscreen?.()
  else document.exitFullscreen?.()
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  font-family: var(--layout-font, 'Inter', 'Noto Sans SC', 'PingFang SC', sans-serif);
}

.layout-aside {
  --sidebar-bg: #0a0a0a;
  --sidebar-active: #2d8a66;
  --sidebar-active-bg: rgba(45, 138, 102, 0.18);
  --sidebar-text: rgba(255, 255, 255, 0.62);
  --sidebar-text-hover: rgba(255, 255, 255, 0.92);
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
}

.logo {
  height: 52px;
  line-height: 52px;
  text-align: center;
  background: var(--sidebar-bg);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.logo__text {
  font-weight: 300;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  letter-spacing: 0.12em;
}

.layout-menu-scroll {
  flex: 1;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f8fafa;
  border-bottom: 1px solid #e8eceb;
  height: 52px;
  padding: 0 20px;
  color: #374151;
  box-shadow: 0 1px 0 rgba(15, 23, 42, 0.04);
}

.layout-header__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-user__btn {
  color: #6b7280 !important;
}

.header-user__btn:hover {
  color: #2d8a66 !important;
}

.header-user__trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 400;
  color: #374151;
}

.header-user__avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2d8a66, #1f6b4f);
  color: #fff;
  text-align: center;
  line-height: 28px;
  font-size: 12px;
  font-weight: 500;
}

.layout-main {
  padding: 0;
  overflow: auto;
}

.layout-main--screen {
  padding: 0;
  background: #0a1628;
}

.layout-menu {
  border-right: none;
  background: transparent !important;
}

.layout-menu :deep(.el-menu-item),
.layout-menu :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  font-size: 13px;
  font-weight: 300;
  color: var(--sidebar-text) !important;
  margin: 2px 10px;
  border-radius: 10px;
  transition: color 0.2s, background 0.2s;
}

.layout-menu :deep(.el-menu-item:hover),
.layout-menu :deep(.el-sub-menu__title:hover) {
  color: var(--sidebar-text-hover) !important;
  background: rgba(255, 255, 255, 0.04) !important;
}

.layout-menu :deep(.el-menu-item.is-active) {
  position: relative;
  background: var(--sidebar-active-bg) !important;
  color: #fff !important;
  font-weight: 400;
}

.layout-menu :deep(.el-menu-item.is-active)::after {
  content: '';
  position: absolute;
  right: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 3px 0 0 3px;
  background: var(--sidebar-active);
}

.layout-menu :deep(.el-sub-menu .el-menu-item) {
  min-width: auto;
  padding-left: 44px !important;
  background: transparent !important;
}

.layout-menu :deep(.el-sub-menu .el-menu-item.is-active) {
  background: var(--sidebar-active-bg) !important;
}

.layout-menu :deep(.el-sub-menu__title .el-sub-menu__icon-arrow) {
  color: rgba(255, 255, 255, 0.45);
}

.layout-menu :deep(.el-sub-menu .el-menu) {
  background: transparent !important;
}

.layout-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
  color: var(--sidebar-text-hover) !important;
}

.layout-breadcrumb {
  color: #6b7280;
  font-size: 13px;
  font-weight: 300;
}

.layout-breadcrumb :deep(.ruoyi-breadcrumb__link) {
  color: #6b7280;
  text-decoration: none;
}

.layout-breadcrumb :deep(.ruoyi-breadcrumb__link:hover) {
  color: #2d8a66;
}

.layout-breadcrumb :deep(.ruoyi-breadcrumb__current) {
  color: #111827;
  font-weight: 400;
}

.layout-breadcrumb :deep(.ruoyi-breadcrumb__sep) {
  color: #d1d5db;
}
</style>
