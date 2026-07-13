import { defineStore } from 'pinia'
import { findMockUser } from '@/mock/users'
import { getMenusByRoleKey } from '@/mock/menus'
import { login as loginApi, getUserInfo, getMenus } from '@/api/auth'
import { MES_LIVE_MODE } from '@/config/mes'
import { useMesStore } from '@/stores/mes'
import { normalizeMenus, getHomePath, BOARD_PATH, stripManagerOnlyFromMenus, sanitizeMenus } from '@/utils/menuRoutes'
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

const _MENU_VERSION = '8'
if (localStorage.getItem('menuVersion') !== _MENU_VERSION) {
  localStorage.removeItem('menus')
  localStorage.setItem('menuVersion', _MENU_VERSION)
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
    menus: sanitizeMenus(JSON.parse(localStorage.getItem('menus') || '[]'))
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
        localStorage.setItem('token', data.token)
        localStorage.setItem('userInfo', JSON.stringify(data))
        await this.loadMenus()
        this.resetTagsHome()
        return data
      }

      const data = await loginApi(form)
      this.token = data.token
      if (!this.token) {
        throw new Error('登录失败：未返回 token')
      }
      localStorage.setItem('token', this.token)
      localStorage.removeItem('menus')
      await this.refreshUserInfo()
      await this.loadMenus()
      this.resetTagsHome()
      if (MES_LIVE_MODE) {
        try {
          await useMesStore().hydrateFromApi()
        } catch (e) {
          console.warn('MES 数据加载失败，登录仍有效', e)
        }
      }
      return this.userInfo
    },
    async loadMenus() {
      // 菜单结构版本号：改动菜单后递增，强制清除旧缓存
      const MENU_VERSION = '6'
      if (localStorage.getItem('menuVersion') !== MENU_VERSION) {
        localStorage.removeItem('menus')
        localStorage.setItem('menuVersion', MENU_VERSION)
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
      localStorage.setItem('menus', JSON.stringify(this.menus))
      return this.menus
    },
    resetTagsHome() {
      const path = this.dashboardPath
      const title = path === BOARD_PATH ? '生产调度大屏' : '首页'
      useTagsViewStore().visitedViews = [{ path, title, affix: true }]
    },
    async refreshUserInfo() {
      if (USE_MOCK_AUTH) return this.userInfo
      const session = await getUserInfo()
      this.userInfo = buildUserInfo(session)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      return this.userInfo
    },
    logout() {
      const token = this.token
      this.token = ''
      this.userInfo = null
      this.menus = []
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('menus')
      useTagsViewStore().visitedViews = []

      if (!USE_MOCK_AUTH && token) {
        import('@/api/auth').then(({ logout }) => {
          logout(token).catch(() => {})
        })
      }
    }
  }
})
