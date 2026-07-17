<script setup>
/**
 * 左侧聊天历史面板：对话开始后替换业务菜单栏显示。
 * 顶部固定「返回主页」「新建对话」，下方为按最近时间排序的会话列表。
 */
import { HomeFilled, Plus, Delete } from '@element-plus/icons-vue'
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
      <button type="button" class="chp__btn" @click="chat.exitToHome()">
        <el-icon><HomeFilled /></el-icon>
        <span>返回主页</span>
      </button>
      <button type="button" class="chp__btn" @click="chat.newSession()">
        <el-icon><Plus /></el-icon>
        <span>新建对话</span>
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
          <div class="chp__item-main">
            <div class="chp__item-title" :title="s.title">{{ s.title }}</div>
            <div class="chp__item-time">{{ fmtTime(s.updatedAt) }}</div>
          </div>
          <button
            type="button"
            class="chp__item-del"
            title="删除该会话"
            @click="onRemove($event, s.id)"
          >
            <el-icon><Delete /></el-icon>
          </button>
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
  font-family: Inter, "PingFang SC", "Noto Sans SC", "Microsoft YaHei", system-ui, sans-serif;
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
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #dce3e8;
  border-radius: 8px;
  background: #fff;
  color: #1f2937;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.chp__btn:hover {
  background: #f8fafb;
  border-color: #c5d0d8;
}

.chp__label {
  margin: 14px 6px 6px;
  font-size: 13px;
  font-weight: 400;
  color: #6b7280;
  flex-shrink: 0;
}
.chp__scroll {
  flex: 1;
  min-height: 0;
}
.chp__list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.chp__item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px 8px 12px;
  border-radius: 6px;
  border-left: 2px solid transparent;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.chp__item:hover {
  background: #f8fafb;
}
.chp__item.is-active {
  background: #ddeff0;
  border-left-color: #7eb8bd;
}
.chp__item-main {
  flex: 1;
  min-width: 0;
}
.chp__item-title {
  font-size: 14px;
  font-weight: 400;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}
.chp__item.is-active .chp__item-title {
  font-weight: 500;
}
.chp__item-time {
  font-size: 12px;
  font-weight: 400;
  color: #667085;
  margin-top: 2px;
}
.chp__item-del {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #667085;
  font-size: 14px;
  flex-shrink: 0;
  opacity: 0;
  cursor: pointer;
  transition: opacity 0.15s, color 0.15s, background 0.15s;
}
.chp__item:hover .chp__item-del {
  opacity: 1;
}
.chp__item-del:hover {
  color: #1f2937;
  background: rgba(0, 0, 0, 0.04);
}
.chp__empty {
  padding: 20px 10px;
  text-align: center;
  font-size: 13px;
  font-weight: 400;
  color: #6b7280;
}
</style>
