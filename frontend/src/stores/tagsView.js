import { defineStore } from 'pinia'

export const useTagsViewStore = defineStore('tagsView', {
  state: () => ({
    visitedViews: [{ path: '/system/board', title: '首页', affix: true }]
  }),
  actions: {
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
