import { defineStore } from 'pinia'

/** 通知类型枚举 */
export const NOTIF_TYPE = {
  ALARM:    'alarm',    // 设备报警
  MAINT:    'maint',    // 维保任务
  PROCESS:  'process',  // 工序流转
  QUALITY:  'quality',  // 质检异常
  SYSTEM:   'system',   // 系统消息
}

const TYPE_META = {
  alarm:   { label: '设备报警', icon: '🔴', color: '#ef4444' },
  maint:   { label: '维保任务', icon: '🔧', color: '#f59e0b' },
  process: { label: '工序流转', icon: '🔄', color: '#3b82f6' },
  quality: { label: '质检异常', icon: '⚠️', color: '#f59e0b' },
  system:  { label: '系统消息', icon: '📢', color: '#8b5cf6' },
}

function getTypeMeta(type) {
  return TYPE_META[type] ?? { label: '通知', icon: '📌', color: '#6b7a90' }
}

let _idCounter = Date.now()
function genId() { return ++_idCounter }

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    items: [],
    audienceKey: '',
    readKeys: [],
    // 头像：base64 字符串 or null（用文字头像兜底）
    avatar: localStorage.getItem('userAvatar') || null,
  }),

  getters: {
    unread: (state) => state.items.filter(n => !n.read),
    unreadCount: (state) => state.items.filter(n => !n.read).length,
    typeMeta: () => getTypeMeta,
  },

  actions: {
    configureAudience(user) {
      const key = user?.username || user?.userId || 'anonymous'
      if (this.audienceKey === key) return
      this.audienceKey = String(key)
      this.readKeys = _loadReadKeys(this.audienceKey)
      this.items = []
    },
    sync(inbox) {
      const read = new Set(this.readKeys)
      this.items = (inbox || []).map(item => ({
        ...item,
        id: item.sourceKey,
        read: read.has(item.sourceKey)
      })).sort((a, b) => b.createdAt - a.createdAt)
    },
    /**
     * 推送一条通知
     * @param {{ type, title, content, from?, link? }} notif
     */
    push(notif) {
      if (notif.sourceKey && this.items.some(item => item.sourceKey === notif.sourceKey)) return
      const item = {
        id:        genId(),
        type:      notif.type ?? NOTIF_TYPE.SYSTEM,
        title:     notif.title ?? '新消息',
        content:   notif.content ?? '',
        from:      notif.from ?? '',
        link:      notif.link ?? null,
        sourceKey: notif.sourceKey ?? null,
        read:      false,
        createdAt: Date.now(),
      }
      this.items.unshift(item)
      // 最多保留 200 条
      if (this.items.length > 200) this.items = this.items.slice(0, 200)
      _saveToStorage(this.items)
    },

    markRead(id) {
      const item = this.items.find(n => n.id === id)
      if (item) {
        item.read = true
        if (!this.readKeys.includes(item.sourceKey)) this.readKeys.push(item.sourceKey)
        _saveReadKeys(this.audienceKey, this.readKeys)
      }
    },

    markAllRead() {
      this.items.forEach(n => { n.read = true })
      this.readKeys = [...new Set([...this.readKeys, ...this.items.map(item => item.sourceKey)])]
      _saveReadKeys(this.audienceKey, this.readKeys)
    },

    remove(id) {
      this.items = this.items.filter(n => n.id !== id)
      _saveToStorage(this.items)
    },

    clearAll() {
      this.items = []
      _saveToStorage(this.items)
    },

    setAvatar(base64OrNull) {
      this.avatar = base64OrNull
      if (base64OrNull) localStorage.setItem('userAvatar', base64OrNull)
      else localStorage.removeItem('userAvatar')
    },

    /** SSE / 轮询接入口：外部调用此方法注入新消息即可 */
    injectFromApi(rawList) {
      if (!Array.isArray(rawList)) return
      rawList.forEach(r => this.push({
        type:    r.type,
        title:   r.title,
        content: r.content,
        from:    r.from,
        link:    r.link,
      }))
    },
  },
})

// ── localStorage 持久化 ──────────────────────────────────────────
let _activeAudience = 'anonymous'

function _storageKey(audience = _activeAudience) { return `mes_notifications_v2_${audience}` }

function _loadFromStorage(audience) {
  try {
    _activeAudience = audience || 'anonymous'
    const raw = localStorage.getItem(_storageKey())
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function _saveToStorage(items) {
  try { localStorage.setItem(_storageKey(), JSON.stringify(items)) } catch { /**/ }
}

function _readStorageKey(audience) { return `mes_notification_reads_v1_${audience}` }
function _loadReadKeys(audience) {
  try { return JSON.parse(localStorage.getItem(_readStorageKey(audience)) || '[]') } catch { return [] }
}
function _saveReadKeys(audience, keys) {
  try { localStorage.setItem(_readStorageKey(audience), JSON.stringify(keys)) } catch { /**/ }
}
