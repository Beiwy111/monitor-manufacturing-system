<template>
  <div class="nc-wrap" v-click-outside="close">
    <!-- 触发按钮 -->
    <button class="nc-trigger" @click="toggle" :class="{ active: open }">
      <span class="nc-trigger__icon">🔔</span>
      <span v-if="store.unreadCount > 0" class="nc-badge">
        {{ store.unreadCount > 99 ? '99+' : store.unreadCount }}
      </span>
    </button>

    <!-- 下拉面板 -->
    <transition name="nc-drop">
      <div v-if="open" class="nc-panel">
        <!-- 头部 -->
        <div class="nc-head">
          <span class="nc-head__title">消息通知</span>
          <div class="nc-head__actions">
            <span v-if="store.unreadCount > 0" class="nc-action-link" @click="store.markAllRead()">
              全部已读
            </span>
            <span class="nc-action-link" @click="store.clearAll()">清空</span>
          </div>
        </div>

        <!-- 类型 tab 过滤 -->
        <div class="nc-tabs">
          <button
            v-for="tab in TABS" :key="tab.value"
            class="nc-tab" :class="{ active: activeTab === tab.value }"
            @click="activeTab = tab.value"
          >
            {{ tab.label }}
            <span v-if="tab.count > 0" class="nc-tab__cnt">{{ tab.count }}</span>
          </button>
        </div>

        <!-- 消息列表 -->
        <div class="nc-list" ref="listEl">
          <template v-if="filtered.length">
            <div
              v-for="item in filtered" :key="item.id"
              class="nc-item" :class="{ unread: !item.read }"
              @click="handleClick(item)"
            >
              <div class="nc-item__icon" :style="{ background: store.typeMeta(item.type).color + '18' }">
                <span>{{ store.typeMeta(item.type).icon }}</span>
              </div>
              <div class="nc-item__body">
                <div class="nc-item__title">{{ item.title }}</div>
                <div class="nc-item__content">{{ item.content }}</div>
                <div class="nc-item__meta">
                  <span class="nc-item__from">{{ item.from }}</span>
                  <span class="nc-item__time">{{ timeAgo(item.createdAt) }}</span>
                </div>
              </div>
              <div class="nc-item__side">
                <span v-if="!item.read" class="nc-dot" />
                <button class="nc-del" title="删除" @click.stop="store.remove(item.id)">×</button>
              </div>
            </div>
          </template>
          <div v-else class="nc-empty">
            <span>🎉</span>
            <p>暂无{{ activeTab === 'all' ? '' : currentTabLabel }}消息</p>
          </div>
        </div>

        <!-- 底部统计 -->
        <div class="nc-foot">
          共 {{ store.items.length }} 条 · 未读 {{ store.unreadCount }} 条
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore, NOTIF_TYPE } from '@/stores/notification.js'

const store = useNotificationStore()
const router = useRouter()

const open      = ref(false)
const activeTab = ref('all')
const listEl    = ref(null)

function toggle() { open.value = !open.value }
function close()  { open.value = false }

// 类型 tab 配置
const TABS = computed(() => [
  { value: 'all',                      label: '全部',   count: store.unreadCount },
  { value: NOTIF_TYPE.ALARM,           label: '报警',   count: countUnread('alarm') },
  { value: NOTIF_TYPE.MAINT,           label: '维保',   count: countUnread('maint') },
  { value: NOTIF_TYPE.PROCESS,         label: '工序',   count: countUnread('process') },
  { value: NOTIF_TYPE.QUALITY,         label: '质检',   count: countUnread('quality') },
  { value: NOTIF_TYPE.SYSTEM,          label: '系统',   count: countUnread('system') },
])

const currentTabLabel = computed(() =>
  TABS.value.find(t => t.value === activeTab.value)?.label ?? ''
)

function countUnread(type) {
  return store.items.filter(n => !n.read && n.type === type).length
}

const filtered = computed(() => {
  if (activeTab.value === 'all') return store.items
  return store.items.filter(n => n.type === activeTab.value)
})

function handleClick(item) {
  store.markRead(item.id)
  if (item.link) router.push(item.link)
}

// 相对时间
function timeAgo(ts) {
  const sec = Math.floor((Date.now() - ts) / 1000)
  if (sec < 60)   return '刚刚'
  if (sec < 3600) return `${Math.floor(sec / 60)} 分钟前`
  if (sec < 86400)return `${Math.floor(sec / 3600)} 小时前`
  return `${Math.floor(sec / 86400)} 天前`
}

// v-click-outside 指令（局部注册）
const vClickOutside = {
  mounted(el, binding) {
    el._clickOutside = (e) => {
      if (!el.contains(e.target)) binding.value()
    }
    document.addEventListener('mousedown', el._clickOutside)
  },
  unmounted(el) {
    document.removeEventListener('mousedown', el._clickOutside)
  },
}
</script>

<style scoped>
.nc-wrap { position: relative; display: inline-flex; }

/* 触发按钮 */
.nc-trigger {
  position: relative;
  width: 36px; height: 36px;
  border-radius: 9px;
  background: rgba(255,255,255,.12);
  border: 1px solid rgba(255,255,255,.18);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: background .15s;
  color: inherit;
}
.nc-trigger:hover, .nc-trigger.active {
  background: rgba(255,255,255,.22);
}
.nc-trigger__icon { font-size: 17px; line-height: 1; }
.nc-badge {
  position: absolute; top: -4px; right: -4px;
  background: #ef4444; color: #fff;
  font-size: 10px; font-weight: 700;
  min-width: 17px; height: 17px;
  border-radius: 9px; padding: 0 3px;
  display: flex; align-items: center; justify-content: center;
  border: 2px solid #fff;
  box-shadow: 0 1px 4px rgba(239,68,68,.4);
  animation: badge-pop .25s ease;
}
@keyframes badge-pop {
  0%   { transform: scale(0); }
  70%  { transform: scale(1.15); }
  100% { transform: scale(1); }
}

/* 下拉面板 */
.nc-panel {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 360px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,27,63,.18), 0 0 0 1px rgba(0,27,63,.08);
  z-index: 9999;
  display: flex; flex-direction: column;
  overflow: hidden;
}
.nc-drop-enter-active, .nc-drop-leave-active {
  transition: opacity .18s ease, transform .18s ease;
}
.nc-drop-enter-from, .nc-drop-leave-to {
  opacity: 0; transform: translateY(-6px) scale(.98);
}

/* 头部 */
.nc-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px 10px;
  border-bottom: 1px solid #f0f4f8;
}
.nc-head__title { font-size: 14px; font-weight: 700; color: #001b3f; }
.nc-head__actions { display: flex; gap: 12px; }
.nc-action-link {
  font-size: 12px; color: #3b82f6; cursor: pointer;
  transition: color .12s;
}
.nc-action-link:hover { color: #1d4ed8; }

/* Tabs */
.nc-tabs {
  display: flex; gap: 2px; padding: 8px 10px 6px;
  border-bottom: 1px solid #f0f4f8;
  overflow-x: auto; scrollbar-width: none;
}
.nc-tabs::-webkit-scrollbar { display: none; }
.nc-tab {
  flex-shrink: 0;
  padding: 3px 10px; border-radius: 8px;
  background: transparent; border: none;
  font-size: 12px; color: #6b7a90; cursor: pointer;
  display: flex; align-items: center; gap: 4px;
  transition: background .14s, color .14s;
}
.nc-tab:hover  { background: #f0f4f8; }
.nc-tab.active { background: #eff6ff; color: #3b82f6; font-weight: 600; }
.nc-tab__cnt {
  background: #ef4444; color: #fff;
  font-size: 10px; min-width: 16px; height: 16px;
  border-radius: 8px; padding: 0 3px;
  display: inline-flex; align-items: center; justify-content: center;
  font-weight: 700;
}

/* 列表 */
.nc-list {
  max-height: 380px; overflow-y: auto;
  padding: 4px 0;
}
.nc-list::-webkit-scrollbar { width: 4px; }
.nc-list::-webkit-scrollbar-thumb { background: #d1daea; border-radius: 2px; }

/* 单条消息 */
.nc-item {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 10px 14px;
  cursor: pointer;
  transition: background .12s;
  position: relative;
}
.nc-item:hover { background: #f7faff; }
.nc-item.unread { background: #f0f6ff; }
.nc-item.unread:hover { background: #e8f0ff; }

.nc-item__icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; flex-shrink: 0;
}
.nc-item__body { flex: 1; min-width: 0; }
.nc-item__title {
  font-size: 13px; font-weight: 600; color: #001b3f;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.nc-item.unread .nc-item__title { color: #1d3a6e; }
.nc-item__content {
  font-size: 12px; color: #6b7a90; margin-top: 2px;
  display: -webkit-box; -webkit-line-clamp: 2;
  -webkit-box-orient: vertical; overflow: hidden;
}
.nc-item__meta {
  display: flex; justify-content: space-between;
  margin-top: 5px;
}
.nc-item__from { font-size: 11px; color: #9aa5b4; }
.nc-item__time { font-size: 11px; color: #b0bec5; }

.nc-item__side {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  flex-shrink: 0;
}
.nc-dot {
  width: 7px; height: 7px; border-radius: 50%; background: #3b82f6;
  flex-shrink: 0;
}
.nc-del {
  width: 18px; height: 18px; border-radius: 5px;
  background: transparent; border: none;
  color: #c0c4cc; font-size: 15px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: background .12s, color .12s;
  opacity: 0;
}
.nc-item:hover .nc-del { opacity: 1; }
.nc-del:hover { background: #fee2e2; color: #ef4444; }

/* 空态 */
.nc-empty {
  padding: 40px 0; text-align: center;
  color: #9aa5b4; font-size: 13px;
}
.nc-empty span { font-size: 28px; display: block; margin-bottom: 8px; }

/* 底部 */
.nc-foot {
  border-top: 1px solid #f0f4f8;
  padding: 8px 14px;
  font-size: 11px; color: #9aa5b4;
  text-align: center;
}
</style>
