import { defineStore } from 'pinia'
import { findMockUser } from '@/mock/users'
import { getMenusByRoleKey } from '@/mock/menus'
import { login as loginApi, getUserInfo, getMenus } from '@/api/auth'
import { MES_LIVE_MODE } from '@/config/mes'
import { useMesStore } from '@/stores/mes'
import { normalizeMenus, getHomePath, getHomeTitle, stripManagerOnlyFromMenus, sanitizeMenus } from '@/utils/menuRoutes'
import { useTagsViewStore } from '@/stores/tagsView'

/** 是否使用 Mock 登录（后端接口就绪后改为 false） */
const USE_MOCK_AUTH = false

function mapRoleCodeToKey(roleCode) {
  const map = {
    ADMIN: 'admin',
    ORDER: 'order',
    PLANNER: 'planner',
    MANAGER: 'manager',
    OPERATOR: 'operator',
    QC: 'quality',
    PURCHASER: 'purchase',
    WAREHOUSE: 'warehouse',
    DEVICE: 'device',
    SERVICE: 'aftersale',
    COST: 'cost',
    CUSTOMER: 'customer'
  }
  return map[(roleCode || '').toUpperCase()] || 'admin'
}

function buildUserInfo(session) {
  const roleKey = mapRoleCodeToKey(session.roleCode)
  return {
    userId: session.userId,
    username: session.username,
    realName: session.realName,
    roleId: session.roleId,
    roleCode: session.roleCode,
    roleName: session.roleName,
    roleKey,
    customerName: session.customerName,
    shippingAddress: session.shippingAddress,
    phone: session.phone,
    email: session.email,
    dashboardPath: getHomePath(roleKey)
  }
}

const _MENU_VERSION = '21'

/**
 * 登录态存 sessionStorage（按浏览器标签页隔离）：
 * 可以同时开多个标签页登录不同账号，互不顶号；刷新当前标签页登录态仍在。
 */
const authStorage = window.sessionStorage

// 兼容旧版本：把遗留在 localStorage 的登录态迁移进当前标签页，然后清掉全局副本
for (const k of ['token', 'userInfo', 'menus', 'menuVersion']) {
  const legacy = localStorage.getItem(k)
  if (legacy !== null) {
    if (authStorage.getItem(k) === null) authStorage.setItem(k, legacy)
    localStorage.removeItem(k)
  }
}

if (authStorage.getItem('menuVersion') !== _MENU_VERSION) {
  authStorage.removeItem('menus')
  authStorage.setItem('menuVersion', _MENU_VERSION)
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: authStorage.getItem('token') || '',
    userInfo: JSON.parse(authStorage.getItem('userInfo') || 'null'),
    menus: sanitizeMenus(JSON.parse(authStorage.getItem('menus') || '[]'))
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    displayName: (state) => state.userInfo?.realName || state.userInfo?.username || '用户',
    roleKey: (state) => state.userInfo?.roleKey || '',
    dashboardPath: (state) => getHomePath(state.userInfo?.roleKey) || state.userInfo?.dashboardPath
  },
  actions: {
    async login(form) {
      if (USE_MOCK_AUTH) {
        const user = findMockUser(form.username?.trim(), form.password)
        if (!user) {
          throw new Error('用户名或密码错误')
        }
        const data = {
          token: `mock-token-${user.roleKey}-${Date.now()}`,
          userId: 0,
          username: user.username,
          realName: user.realName,
          roleKey: user.roleKey,
          roleCode: user.roleCode,
          roleName: user.roleName,
          dashboardPath: getHomePath(user.roleKey)
        }
        this.token = data.token
        this.userInfo = data
        authStorage.setItem('token', data.token)
        authStorage.setItem('userInfo', JSON.stringify(data))
        await this.loadMenus()
        this.resetTagsHome()
        return data
      }

      const data = await loginApi(form)
      this.token = data.token
      if (!this.token) {
        throw new Error('登录失败：未返回 token')
      }
      authStorage.setItem('token', this.token)
      authStorage.removeItem('menus')
      await this.refreshUserInfo()
      await this.loadMenus()
      this.resetTagsHome()
      if (MES_LIVE_MODE) {
        useMesStore().hydrateForPage().catch((e) => {
          console.warn('MES 数据加载失败，登录仍有效', e)
        })
      }
      return this.userInfo
    },
    async loadMenus(force = false) {
      const MENU_VERSION = _MENU_VERSION
      if (authStorage.getItem('menuVersion') !== MENU_VERSION) {
        authStorage.removeItem('menus')
        authStorage.setItem('menuVersion', MENU_VERSION)
      }
      if (!force && this.menus.length && authStorage.getItem('menus')) {
        return this.menus
      }
      const fallback = getMenusByRoleKey(this.roleKey)
      try {
        const apiMenus = await getMenus()
        this.menus = normalizeMenus(apiMenus, this.roleKey, fallback)
      } catch {
        this.menus = normalizeMenus([], this.roleKey, fallback)
      }
      if (!this.menus.some((m) => m.children?.length) && fallback.length) {
        this.menus = normalizeMenus(fallback, this.roleKey, null)
      }
      this.menus = sanitizeMenus(stripManagerOnlyFromMenus(this.menus, this.roleKey))
      authStorage.setItem('menus', JSON.stringify(this.menus))
      return this.menus
    },
    resetTagsHome() {
      const path = this.dashboardPath
      const title = getHomeTitle(this.roleKey)
      useTagsViewStore().visitedViews = [{ path, title, affix: true }]
    },
    async refreshUserInfo() {
      if (USE_MOCK_AUTH) return this.userInfo
      const session = await getUserInfo()
      this.userInfo = buildUserInfo(session)
      authStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      return this.userInfo
    },
    logout() {
      const token = this.token
      this.token = ''
      this.userInfo = null
      this.menus = []
      authStorage.removeItem('token')
      authStorage.removeItem('userInfo')
      authStorage.removeItem('menus')
      useTagsViewStore().visitedViews = []

      if (!USE_MOCK_AUTH && token) {
        import('@/api/auth').then(({ logout }) => {
          logout(token).catch(() => {})
        })
      }
    }
  }
})
