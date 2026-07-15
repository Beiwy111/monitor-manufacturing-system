<script setup>
/**
 * 左侧聊天历史面板：对话开始后替换业务菜单栏显示。
 * 顶部固定「返回主页」「新建对话」，下方为按最近时间排序的会话列表。
 */
import { HomeFilled, Plus, ChatDotRound, Delete } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores/chat'

const chat = useChatStore()

function fmtTime(ts) {
  if (!ts) return ''
  const diff = Date.now() - ts
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return Math.floor(diff / 60_000) + ' 分钟前'
  if (diff < 86_400_000) return Math.floor(diff / 3_600_000) + ' 小时前'
  const d = new Date(ts)
  const today = new Date()
  const yesterday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1)
  if (d >= yesterday && diff < 172_800_000) return '昨天'
  return `${d.getMonth() + 1}-${d.getDate()}`
}

function onRemove(e, id) {
  e.stopPropagation()
  chat.removeSession(id)
}
</script>

<template>
  <div class="chp">
    <div class="chp__actions">
      <button class="chp__btn" @click="chat.exitToHome()">
        <el-icon><HomeFilled /></el-icon><span>返回主页</span>
      </button>
      <button class="chp__btn chp__btn--primary" @click="chat.newSession()">
        <el-icon><Plus /></el-icon><span>新建对话</span>
      </button>
    </div>

    <div class="chp__label">聊天历史</div>

    <el-scrollbar class="chp__scroll">
      <div class="chp__list">
        <div
          v-for="s in chat.sessions"
          :key="s.id"
          :class="['chp__item', { 'is-active': s.id === chat.activeId }]"
          @click="chat.setActive(s.id)"
        >
          <el-icon class="chp__item-icon"><ChatDotRound /></el-icon>
          <div class="chp__item-main">
            <div class="chp__item-title">{{ s.title }}</div>
            <div class="chp__item-time">{{ fmtTime(s.updatedAt) }}</div>
          </div>
          <el-icon class="chp__item-del" title="删除该会话" @click="onRemove($event, s.id)"><Delete /></el-icon>
        </div>
        <div v-if="!chat.sessions.length" class="chp__empty">暂无历史会话</div>
      </div>
    </el-scrollbar>
  </div>
</template>

<style scoped>
.chp {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 10px 8px;
  box-sizing: border-box;
}
.chp__actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
}
.chp__btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 9px 12px;
  border: 1px solid rgba(61, 57, 41, 0.14);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.65);
  color: #3d3929;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.chp__btn:hover { background: rgba(255, 255, 255, 0.95); }
.chp__btn--primary {
  border-color: rgba(217, 119, 87, 0.45);
  color: #c5654a;
  font-weight: 600;
}
.chp__btn--primary:hover { background: rgba(217, 119, 87, 0.1); }

.chp__label {
  margin: 14px 6px 6px;
  font-size: 12px;
  color: #8f8975;
  flex-shrink: 0;
}
.chp__scroll { flex: 1; min-height: 0; }
.chp__list { display: flex; flex-direction: column; gap: 2px; }
.chp__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.chp__item:hover { background: rgba(255, 255, 255, 0.6); }
.chp__item.is-active { background: rgba(217, 119, 87, 0.12); }
.chp__item-icon { color: #a49e8c; font-size: 14px; flex-shrink: 0; }
.chp__item.is-active .chp__item-icon { color: #d97757; }
.chp__item-main { flex: 1; min-width: 0; }
.chp__item-title {
  font-size: 13px;
  color: #3d3929;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.chp__item.is-active .chp__item-title { font-weight: 600; }
.chp__item-time { font-size: 11px; color: #a49e8c; margin-top: 2px; }
.chp__item-del {
  color: #c9c2ae;
  font-size: 13px;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.15s, color 0.15s;
}
.chp__item:hover .chp__item-del { opacity: 1; }
.chp__item-del:hover { color: #e2604c; }
.chp__empty {
  padding: 20px 10px;
  text-align: center;
  font-size: 12px;
  color: #a49e8c;
}
</style>
