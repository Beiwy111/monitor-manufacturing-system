<script setup>
/**
 * 通用对话输入（全角色助手对话复用）。
 * 单一圆角容器：文本区 + 语音/发送按钮同层，无内层白框。
 * variant="hero"：首页大输入（约 100px 起，可增高至上限）；
 * variant="slim"：对话底栏单行输入（自动增高）。
 */
import { ref, watch, onMounted, nextTick } from 'vue'
import { Microphone, Promotion } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  busy: { type: Boolean, default: false },
  recording: { type: Boolean, default: false },
  placeholder: { type: String, default: '输入消息，回车发送…' },
  variant: { type: String, default: 'slim' }
})
const emit = defineEmits(['update:modelValue', 'send', 'mic'])

const fieldRef = ref(null)

const MIN_HERO = 100
const MAX_HERO = 220
const MAX_SLIM = 160

function resizeField() {
  const el = fieldRef.value
  if (!el) return
  el.style.height = 'auto'
  const max = props.variant === 'hero' ? MAX_HERO : MAX_SLIM
  const min = props.variant === 'hero' ? MIN_HERO : 44
  el.style.height = `${Math.min(Math.max(el.scrollHeight, min), max)}px`
}

function onInput(e) {
  emit('update:modelValue', e.target.value)
  nextTick(resizeField)
}

function onEnter(e) {
  if (e.shiftKey) return
  e.preventDefault()
  emit('send')
}

watch(() => props.modelValue, () => nextTick(resizeField))

onMounted(() => nextTick(resizeField))
</script>

<template>
  <div :class="['chat-composer', `chat-composer--${variant}`]">
    <textarea
      ref="fieldRef"
      class="chat-composer__field"
      :value="modelValue"
      :placeholder="placeholder"
      rows="1"
      @input="onInput"
      @keydown.enter="onEnter"
    />
    <div v-if="variant === 'hero'" class="chat-composer__footer">
      <span class="chat-composer__meta"><slot name="meta" /></span>
      <div class="chat-composer__btns">
        <button
          type="button"
          class="chat-composer__icon-btn"
          :class="{ 'is-recording': recording }"
          :disabled="busy && !recording"
          :title="recording ? '停止录音' : '语音输入'"
          @click="emit('mic')"
        >
          <el-icon><Microphone /></el-icon>
        </button>
        <button
          type="button"
          class="chat-composer__icon-btn chat-composer__icon-btn--send"
          :disabled="busy || !modelValue.trim()"
          title="发送"
          @click="emit('send')"
        >
          <el-icon><Promotion /></el-icon>
        </button>
      </div>
    </div>
    <div v-else class="chat-composer__btns chat-composer__btns--inline">
      <button
        type="button"
        class="chat-composer__icon-btn"
        :class="{ 'is-recording': recording }"
        :disabled="busy && !recording"
        :title="recording ? '停止录音' : '语音输入'"
        @click="emit('mic')"
      >
        <el-icon><Microphone /></el-icon>
      </button>
      <button
        type="button"
        class="chat-composer__icon-btn chat-composer__icon-btn--send"
        :disabled="busy || !modelValue.trim()"
        title="发送"
        @click="emit('send')"
      >
        <el-icon><Promotion /></el-icon>
      </button>
    </div>
  </div>
</template>

<style scoped>
.chat-composer {
  width: 100%;
  font-family: Inter, "PingFang SC", "Noto Sans SC", "Microsoft YaHei", system-ui, sans-serif;
  background: #fff;
  border: 1px solid #dce3e8;
  border-radius: 16px;
  padding: 12px 14px;
  box-sizing: border-box;
  transition: border-color 0.15s;
}
.chat-composer:focus-within {
  border-color: #b8c9cc;
}

.chat-composer--hero {
  max-width: 860px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-composer--slim {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.chat-composer__field {
  flex: 1;
  width: 100%;
  min-height: 44px;
  max-height: 160px;
  margin: 0;
  padding: 0;
  border: none;
  outline: none;
  resize: none;
  background: transparent;
  box-shadow: none;
  font-family: inherit;
  font-size: 15px;
  font-weight: 400;
  line-height: 1.7;
  color: #1f2937;
  overflow-y: auto;
}
.chat-composer--hero .chat-composer__field {
  min-height: 100px;
  max-height: 220px;
}
.chat-composer__field::placeholder {
  color: #6b7280;
}

.chat-composer__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 4px;
}

.chat-composer__meta {
  font-size: 13px;
  font-weight: 400;
  color: #6b7280;
  line-height: 1.5;
}

.chat-composer__btns {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-shrink: 0;
}
.chat-composer__btns--inline {
  padding-bottom: 2px;
}

.chat-composer__icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: 1px solid #dce3e8;
  border-radius: 10px;
  background: #fff;
  color: #667085;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.chat-composer__icon-btn:hover:not(:disabled) {
  background: #f8fafb;
  border-color: #c5d0d8;
  color: #1f2937;
}
.chat-composer__icon-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.chat-composer__icon-btn.is-recording {
  background: #fef2f2;
  border-color: #fecaca;
  color: #dc2626;
}
.chat-composer__icon-btn--send {
  background: #ddeff0;
  border-color: #c5e0e2;
  color: #1f2937;
}
.chat-composer__icon-btn--send:hover:not(:disabled) {
  background: #cfe8ea;
  border-color: #b8d8db;
}
</style>
