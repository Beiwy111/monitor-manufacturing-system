import { defineStore } from 'pinia'
import { useUserStore } from '@/stores/user'

/**
 * 智能助手对话会话（全角色通用）。
 * - 会话按登录用户名分开持久化到 localStorage，刷新后历史仍在；
 * - activeId 不持久化：初次进入助手页永远是首页 + 业务菜单栏；
 * - 每个会话有独立的后端 sessionId，切换会话不会串上下文。
 */
function storageKey(username) {
  return `chat-sessions-${username || 'anon'}`
}

let seq = 0
function newId() {
  return `${Date.now()}-${++seq}`
}

export const useChatStore = defineStore('chat', {
  state: () => ({
    sessions: [],
    activeId: null,
    loadedFor: ''
  }),
  getters: {
    activeSession: (s) => s.sessions.find((x) => x.id === s.activeId) || null,
    /** 是否处于对话中（决定左侧栏显示聊天历史还是业务菜单） */
    inChat: (s) => !!s.activeId
  },
  actions: {
    ensureLoaded() {
      const username = useUserStore().userInfo?.username || 'anon'
      if (this.loadedFor === username) return
      this.loadedFor = username
      this.activeId = null
      try {
        this.sessions = JSON.parse(localStorage.getItem(storageKey(username)) || '[]')
      } catch {
        this.sessions = []
      }
    },
    persist() {
      try {
        localStorage.setItem(storageKey(this.loadedFor), JSON.stringify(this.sessions.slice(0, 50)))
      } catch { /* 存储满时放弃持久化，不影响会话 */ }
    },
    newSession() {
      const username = useUserStore().userInfo?.username || 'anon'
      const s = {
        id: newId(),
        apiSessionId: `chat-${username}-${Date.now()}-${seq}`,
        title: '新对话',
        updatedAt: Date.now(),
        messages: []
      }
      this.sessions.unshift(s)
      this.activeId = s.id
      this.persist()
      return s
    },
    setActive(id) {
      if (this.sessions.some((s) => s.id === id)) this.activeId = id
    },
    /** 返回主页：退出对话视图，恢复业务菜单栏与助手首页 */
    exitToHome() {
      this.activeId = null
      // 清掉从未开聊的空会话
      this.sessions = this.sessions.filter((s) => s.messages.length > 0)
      this.persist()
    },
    pushMessage(role, text) {
      let s = this.activeSession
      if (!s) s = this.newSession()
      s.messages.push({ role, text })
      if (role === 'user' && (s.title === '新对话' || !s.title)) {
        s.title = text.length > 18 ? text.slice(0, 18) + '…' : text
      }
      s.updatedAt = Date.now()
      // 最近会话置顶
      const idx = this.sessions.indexOf(s)
      if (idx > 0) {
        this.sessions.splice(idx, 1)
        this.sessions.unshift(s)
      }
      this.persist()
      return s
    },
    removeSession(id) {
      this.sessions = this.sessions.filter((s) => s.id !== id)
      if (this.activeId === id) this.activeId = null
      this.persist()
    }
  }
})
