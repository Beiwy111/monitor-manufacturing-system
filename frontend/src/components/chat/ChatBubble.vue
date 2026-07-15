<script setup>
/**
 * 通用消息气泡（全角色助手对话复用）。
 * 用户消息：浅橙背景 + 深灰文字；助手消息：白底浅边框 + ✳ 头像。
 */
defineProps({
  role: { type: String, required: true },   // 'user' | 'assistant'
  text: { type: String, default: '' }
})
</script>

<template>
  <div :class="['chat-msg', role]">
    <div v-if="role === 'assistant'" class="chat-msg__avatar">✳</div>
    <div class="chat-msg__bubble">{{ text }}</div>
  </div>
</template>

<style scoped>
.chat-msg { display: flex; gap: 10px; align-items: flex-start; }
.chat-msg.user { justify-content: flex-end; }
.chat-msg__avatar {
  width: 30px; height: 30px;
  flex-shrink: 0;
  border-radius: 50%;
  background: rgba(217, 119, 87, 0.14);
  color: #d97757;
  display: flex; align-items: center; justify-content: center;
  font-size: 15px;
}
.chat-msg__bubble {
  max-width: 78%;
  padding: 11px 15px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}
.chat-msg.assistant .chat-msg__bubble {
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(61, 57, 41, 0.1);
  color: #3d3929;
  border-top-left-radius: 4px;
}
.chat-msg.user .chat-msg__bubble {
  background: #fdeacf;
  border: 1px solid rgba(217, 119, 87, 0.22);
  color: #4a4437;
  border-bottom-right-radius: 4px;
}
</style>
