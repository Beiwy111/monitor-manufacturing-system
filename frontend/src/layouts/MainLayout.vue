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
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#ffffff"
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
          <span class="layout-header__brand">若依风格 MES</span>
          <div class="ruoyi-breadcrumb">
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
  font-family: 'Microsoft YaHei', 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
}
.layout-aside {
  background: #304156;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 50px;
  line-height: 50px;
  text-align: center;
  background: #2b3a4d;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.logo__text {
  font-weight: 700;
  font-size: 15px;
  color: #fff;
  letter-spacing: 1px;
}
.layout-menu-scroll {
  flex: 1;
}
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(90deg, #3c8dbc 0%, #367fa9 100%);
  height: 50px;
  padding: 0 16px;
  color: #fff;
}
.layout-header__left {
  display: flex;
  align-items: center;
  gap: 20px;
}
.layout-header__brand {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}
.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
}
.header-user__btn {
  color: #fff !important;
}
.header-user__trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #fff;
}
.header-user__avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  text-align: center;
  line-height: 28px;
  font-size: 13px;
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
}
.layout-menu :deep(.el-menu-item),
.layout-menu :deep(.el-sub-menu__title) {
  height: 46px;
  line-height: 46px;
  font-size: 14px;
}
.layout-menu :deep(.el-menu-item.is-active) {
  background: #409eff !important;
  color: #fff !important;
}
.layout-menu :deep(.el-sub-menu .el-menu-item) {
  min-width: auto;
  padding-left: 48px !important;
  background: #1f2d3d !important;
}
.layout-menu :deep(.el-sub-menu .el-menu-item.is-active) {
  background: #409eff !important;
}
:deep(.ruoyi-breadcrumb) {
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
}
:deep(.ruoyi-breadcrumb__link) {
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none;
}
:deep(.ruoyi-breadcrumb__link:hover) {
  color: #fff;
}
:deep(.ruoyi-breadcrumb__current) {
  color: #fff;
}
:deep(.ruoyi-breadcrumb__sep) {
  color: rgba(255, 255, 255, 0.5);
}
</style>
