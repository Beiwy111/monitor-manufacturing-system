import { defineStore } from 'pinia'

export const useTagsViewStore = defineStore('tagsView', {
  state: () => ({
    visitedViews: [{ path: '/system/board', title: '首页', affix: true }]
  }),
  actions: {
    /** 将固定的“首页”标签校准到当前角色首页，并去掉与之重复的普通标签 */
    syncHome(path, title = '首页') {
      if (!path) return
      const home = this.visitedViews.find((v) => v.affix)
      if (home) {
        home.path = path
        home.title = title
      } else {
        this.visitedViews.unshift({ path, title, affix: true })
      }
      this.visitedViews = this.visitedViews.filter((v) => v.affix || v.path !== path)
    },
    addView(route) {
      if (!route?.path || route.meta?.public) return
      const title = route.meta?.title || route.name || route.path
      if (this.visitedViews.some((v) => v.path === route.path)) return
      this.visitedViews.push({ path: route.path, title, affix: false })
    },
    removeView(path) {
      const view = this.visitedViews.find((v) => v.path === path)
      if (view?.affix) return
      this.visitedViews = this.visitedViews.filter((v) => v.path !== path)
    },
    closeOthers(path) {
      this.visitedViews = this.visitedViews.filter((v) => v.affix || v.path === path)
    }
  }
})
