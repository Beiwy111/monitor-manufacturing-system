<template>
  <el-container class="layout-container">
    <el-aside width="168px" class="layout-aside">
      <div class="logo">
        <router-link :to="homePath" class="logo__link" :title="APP_TITLE">
          <BrandLogo :size="32" variant="sidebar" :text="BRAND_NAME" />
        </router-link>
      </div>
      <!-- 对话进行中：左侧切换为聊天历史（返回主页可恢复业务菜单） -->
      <ChatHistoryPanel v-if="showChatHistory" class="layout-chat-history" />
      <el-scrollbar v-else class="layout-menu-scroll">
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
      <div class="layout-aside__chat">
        <router-link to="/chat" class="layout-aside__chat-link" :class="{ 'is-active': route.path === '/chat' }">
          <el-icon><ChatDotRound /></el-icon>
          <span>智能对话</span>
        </router-link>
      </div>
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
          <NotificationCenter />
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
      <el-main class="layout-main ruoyi-app-main" :class="{ 'layout-main--screen': isScreenPage }">
        <router-view />
      </el-main>
      <AssistantWidget v-if="route.path !== '/chat'" />
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Monitor, Setting, Box, ShoppingCart, OfficeBuilding, CircleCheck,
  Tools, Service, FullScreen, ArrowDown, User, ChatDotRound
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import NotificationCenter from '@/components/notification/NotificationCenter.vue'
import AssistantWidget from '@/components/assistant/AssistantWidget.vue'
import ChatHistoryPanel from '@/components/chat/ChatHistoryPanel.vue'
import BrandLogo from '@/components/brand/BrandLogo.vue'
import { APP_TITLE, BRAND_NAME } from '@/constants/brand'
import { useGlobalBusinessMonitor } from '@/composables/useGlobalBusinessMonitor'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()
useGlobalBusinessMonitor()

/** 助手页且已进入对话：侧栏切换为聊天历史；其余场景保持原业务菜单 */
const showChatHistory = computed(() => route.path === '/chat' && chatStore.inChat)

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
  if (p.startsWith('/customer')) return '客户门户'
  if (p.startsWith('/chat')) return '智能对话'
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
  overflow: hidden;
  font-family: var(--layout-font);
  background: transparent;
}

.layout-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: transparent;
}

.layout-aside {
  background: var(--layout-content-gradient, var(--layout-bg-gradient));
  background-attachment: fixed;
  background-repeat: no-repeat;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--layout-border, #e8e8e3);
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 56px;
  padding: 10px 8px;
  background: transparent;
  border-bottom: 1px solid var(--layout-border, #e8e8e3);
}

.logo__link {
  display: flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  width: 100%;
}

.layout-header__left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.layout-menu-scroll {
  flex: 1;
  min-height: 0;
}

.layout-aside__chat {
  flex-shrink: 0;
  padding: 10px 12px 14px;
  border-top: 1px solid var(--layout-border, #e8e8e3);
}

.layout-aside__chat-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--sidebar-text, #4f5560);
  text-decoration: none;
  border-radius: 4px;
  transition: color 0.15s, background 0.15s;
}

.layout-aside__chat-link:hover {
  color: var(--sidebar-text-hover, #25272a);
  background: rgba(0, 0, 0, 0.03);
}

.layout-aside__chat-link.is-active {
  color: var(--sidebar-active-text, #25272a);
  font-weight: 700;
}

.layout-chat-history {
  flex: 1;
  min-height: 0;
}

.layout-menu-scroll :deep(.el-scrollbar__view),
.layout-menu-scroll :deep(.el-scrollbar__wrap) {
  background: transparent;
}

.layout-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: transparent;
  border-bottom: 1px solid var(--layout-border, #e8e8e3);
  height: 52px;
  padding: 0 24px;
  color: var(--layout-text-body, #25272a);
  box-shadow: none;
}

.layout-header__left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-user__btn {
  color: var(--layout-text-caption, #7e838b) !important;
}

.header-user__btn:hover {
  color: var(--layout-text-title, #25272a) !important;
}

.header-user__trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  color: var(--layout-text-body, #25272a);
}

.header-user__avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--sidebar-avatar-bg, #eef1ea);
  color: var(--layout-text-title, #25272a);
  text-align: center;
  line-height: 28px;
  font-size: 12px;
  font-weight: 600;
}

.layout-main {
  flex: 1;
  min-height: 0;
  padding: 0;
  box-sizing: border-box;
  overflow: auto;
  display: flex;
  flex-direction: column;
  background: var(--layout-content-gradient, var(--layout-bg-gradient));
  background-attachment: fixed;
  background-repeat: no-repeat;
}

.layout-main--screen {
  padding: 0;
  background: transparent !important;
}

.layout-menu {
  border-right: none;
  background: transparent !important;
  --el-menu-active-color: var(--layout-text-title, #25272a);
  --el-menu-hover-bg-color: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: var(--sidebar-text, #7e838b);
}

.layout-menu :deep(.el-menu-item),
.layout-menu :deep(.el-sub-menu__title) {
  position: relative;
  height: 44px;
  line-height: 44px;
  font-size: 14px;
  font-weight: 600;
  color: var(--sidebar-text, #4f5560) !important;
  margin: 0;
  padding-left: 20px !important;
  border-radius: 0;
  background: transparent !important;
  transition: color 0.15s, font-weight 0.15s;
}

.layout-menu :deep(.el-menu-item .el-icon),
.layout-menu :deep(.el-sub-menu__title .el-icon) {
  color: var(--sidebar-text, #4f5560) !important;
  font-size: 16px;
  transition: color 0.15s;
}

.layout-menu :deep(.el-menu-item:hover),
.layout-menu :deep(.el-sub-menu__title:hover) {
  color: var(--sidebar-text-hover, #25272a) !important;
  background: transparent !important;
}

.layout-menu :deep(.el-menu-item:hover .el-icon),
.layout-menu :deep(.el-sub-menu__title:hover .el-icon) {
  color: var(--sidebar-text-hover, #25272a) !important;
}

.layout-menu :deep(.el-menu-item.is-active) {
  background: transparent !important;
  color: var(--sidebar-active-text, #25272a) !important;
  font-weight: 700;
}

.layout-menu :deep(.el-menu-item.is-active .el-icon) {
  color: var(--sidebar-active-text, #25272a) !important;
}

.layout-menu :deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 0 2px 2px 0;
  background: var(--sidebar-accent, #8fad94);
}

.layout-menu :deep(.el-menu-item.is-active)::after {
  display: none;
}

.layout-menu :deep(.el-sub-menu .el-menu-item) {
  position: relative;
  min-width: auto;
  padding-left: 44px !important;
  background: transparent !important;
  font-size: 13px;
  font-weight: 600;
  color: var(--sidebar-text, #4f5560) !important;
}

.layout-menu :deep(.el-sub-menu .el-menu-item .el-icon) {
  display: none;
}

.layout-menu :deep(.el-sub-menu .el-menu-item:hover) {
  color: var(--sidebar-text-hover, #25272a) !important;
  background: transparent !important;
}

.layout-menu :deep(.el-sub-menu .el-menu-item.is-active) {
  background: transparent !important;
  color: var(--sidebar-active-text, #25272a) !important;
  font-weight: 700;
}

.layout-menu :deep(.el-sub-menu .el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 12px;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 0 2px 2px 0;
  background: var(--sidebar-accent, #8fad94);
}

.layout-menu :deep(.el-sub-menu__title .el-sub-menu__icon-arrow) {
  color: #b8bdb5;
}

.layout-menu :deep(.el-sub-menu .el-menu) {
  background: transparent !important;
}

.layout-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
  color: var(--layout-text-body, #25272a) !important;
  font-weight: 700;
}

.layout-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title .el-icon) {
  color: var(--layout-text-body, #25272a) !important;
}

.layout-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: var(--sidebar-active-text, #25272a) !important;
  font-weight: 700;
}

.layout-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title .el-icon) {
  color: var(--sidebar-active-text, #25272a) !important;
}

.layout-breadcrumb {
  color: var(--layout-text-caption, #7e838b);
  font-size: 14px;
  font-weight: 500;
}

.layout-breadcrumb :deep(.ruoyi-breadcrumb__link) {
  color: var(--layout-text-caption, #7e838b);
  text-decoration: none;
}

.layout-breadcrumb :deep(.ruoyi-breadcrumb__link:hover) {
  color: var(--layout-text-title, #25272a);
}

.layout-breadcrumb :deep(.ruoyi-breadcrumb__current) {
  color: var(--layout-text-title, #25272a);
  font-weight: 700;
}

.layout-breadcrumb :deep(.ruoyi-breadcrumb__sep) {
  color: var(--layout-border, #e8e8e3);
}
</style>
