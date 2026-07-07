import { defineStore } from 'pinia'
import { findMockUser } from '@/mock/users'
import { getMenusByRoleKey } from '@/mock/menus'
import { login as loginApi, getUserInfo, getMenus } from '@/api/auth'

/** 是否使用 Mock 登录（后端接口就绪后改为 false） */
const USE_MOCK_AUTH = true

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
    menus: JSON.parse(localStorage.getItem('menus') || '[]')
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    displayName: (state) => state.userInfo?.realName || state.userInfo?.username || '用户',
    roleKey: (state) => state.userInfo?.roleKey || '',
    dashboardPath: (state) => state.userInfo?.dashboardPath || '/dashboard/admin'
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
          dashboardPath: user.dashboardPath
        }
        this.token = data.token
        this.userInfo = data
        localStorage.setItem('token', data.token)
        localStorage.setItem('userInfo', JSON.stringify(data))
        await this.loadMenus()
        return data
      }

      const data = await loginApi(form)
      this.token = data.token
      this.userInfo = data
      localStorage.setItem('token', data.token)
      localStorage.setItem('userInfo', JSON.stringify(data))
      await this.loadMenus()
      return data
    },
    async loadMenus() {
      if (USE_MOCK_AUTH) {
        this.menus = getMenusByRoleKey(this.roleKey)
        localStorage.setItem('menus', JSON.stringify(this.menus))
        return this.menus
      }
      const menus = await getMenus()
      this.menus = menus || []
      localStorage.setItem('menus', JSON.stringify(this.menus))
      return this.menus
    },
    async refreshUserInfo() {
      if (USE_MOCK_AUTH) return this.userInfo
      const user = await getUserInfo()
      this.userInfo = { ...this.userInfo, ...user }
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      return this.userInfo
    },
    logout() {
      this.token = ''
      this.userInfo = null
      this.menus = []
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('menus')
    }
  }
})
