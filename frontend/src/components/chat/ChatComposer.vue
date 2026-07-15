<script setup>
/**
 * 通用对话输入卡（全角色助手对话复用）。
 * variant="hero"：首页大输入框（3 行 + 底部工具条，支持 #meta 插槽）；
 * variant="slim"：对话中吸底单行输入（自动增高）。
 */
import { Microphone, Promotion } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  busy: { type: Boolean, default: false },
  recording: { type: Boolean, default: false },
  placeholder: { type: String, default: '输入消息，回车发送…' },
  variant: { type: String, default: 'slim' }   // 'hero' | 'slim'
})
const emit = defineEmits(['update:modelValue', 'send', 'mic'])

function onEnter(e) {
  if (e.shiftKey) return
  e.preventDefault()
  emit('send')
}
</script>

<template>
  <div :class="['chat-composer', `chat-composer--${variant}`]">
    <el-input
      :model-value="modelValue"
      type="textarea"
      :rows="variant === 'hero' ? 3 : 1"
      :autosize="variant === 'hero' ? false : { minRows: 1, maxRows: 5 }"
      resize="none"
      class="chat-composer__input"
      :placeholder="placeholder"
      @update:model-value="emit('update:modelValue', $event)"
      @keydown.enter="onEnter"
    />
    <div v-if="variant === 'hero'" class="chat-composer__bar">
      <span class="chat-composer__meta"><slot name="meta" /></span>
      <div class="chat-composer__btns">
        <el-button :type="recording ? 'danger' : 'default'" circle
          :icon="Microphone" :loading="busy && !recording" @click="emit('mic')" />
        <el-button class="chat-composer__send" type="primary" circle :icon="Promotion"
          :disabled="busy || !modelValue.trim()" @click="emit('send')" />
      </div>
    </div>
    <div v-else class="chat-composer__btns">
      <el-button :type="recording ? 'danger' : 'default'" circle
        :icon="Microphone" :loading="busy && !recording" @click="emit('mic')" />
      <el-button class="chat-composer__send" type="primary" circle :icon="Promotion"
        :disabled="busy || !modelValue.trim()" @click="emit('send')" />
    </div>
  </div>
</template>

<style scoped>
.chat-composer {
  width: 100%;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(61, 57, 41, 0.12);
  border-radius: 16px;
  box-shadow: 0 6px 28px rgba(61, 57, 41, 0.08);
  padding: 12px 14px 10px;
  transition: box-shadow 0.2s, border-color 0.2s;
}
.chat-composer:focus-within {
  border-color: rgba(217, 119, 87, 0.5);
  box-shadow: 0 8px 32px rgba(217, 119, 87, 0.12);
}
.chat-composer--slim {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 8px 10px;
}
.chat-composer--slim .chat-composer__input { flex: 1; }
.chat-composer__input :deep(.el-textarea__inner) {
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 4px 2px;
  font-size: 15px;
  color: #3d3929;
}
.chat-composer__input :deep(.el-textarea__inner::placeholder) { color: #a49e8c; }
.chat-composer__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
}
.chat-composer__meta { font-size: 12px; color: #a49e8c; }
.chat-composer__btns { display: flex; gap: 8px; align-items: flex-end; }
.chat-composer__send { background: #d97757; border-color: #d97757; }
.chat-composer__send:hover:not(:disabled) { background: #c5654a; border-color: #c5654a; }
</style>
