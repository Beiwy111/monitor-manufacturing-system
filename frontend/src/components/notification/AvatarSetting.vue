<template>
  <div class="av-wrap" v-click-outside="close">
    <!-- 触发头像 -->
    <div class="av-trigger" @click="toggle" :class="{ active: open }">
      <img v-if="store.avatar" :src="store.avatar" class="av-img" alt="头像" />
      <span v-else class="av-text">{{ initials }}</span>
    </div>

    <!-- 设置面板 -->
    <transition name="av-drop">
      <div v-if="open" class="av-panel">
        <div class="av-panel__head">
          <div class="av-preview">
            <img v-if="store.avatar" :src="store.avatar" class="av-preview__img" alt="头像" />
            <span v-else class="av-preview__text">{{ initials }}</span>
          </div>
          <div class="av-user-info">
            <div class="av-user-info__name">{{ userStore.displayName }}</div>
            <div class="av-user-info__role">{{ userStore.userInfo?.roleName || '—' }}</div>
          </div>
        </div>

        <div class="av-section-title">更换头像</div>

        <!-- 预设头像 -->
        <div class="av-presets">
          <div
            v-for="(item, i) in PRESETS" :key="i"
            class="av-preset"
            :class="{ selected: store.avatar === item.url }"
            @click="selectPreset(item.url)"
            :style="{ background: item.bg }"
          >
            <span v-if="item.emoji">{{ item.emoji }}</span>
            <img v-else-if="item.url" :src="item.url" alt="" />
          </div>
        </div>

        <!-- 上传本地图片 -->
        <div class="av-upload-row">
          <label class="av-upload-btn">
            📁 上传本地图片
            <input type="file" accept="image/*" hidden @change="onFileChange" />
          </label>
          <button v-if="store.avatar" class="av-reset-btn" @click="resetAvatar">
            ✕ 移除头像
          </button>
        </div>

        <!-- 状态提示 -->
        <div v-if="uploadError" class="av-error">{{ uploadError }}</div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useNotificationStore } from '@/stores/notification.js'
import { useUserStore } from '@/stores/user.js'

const store     = useNotificationStore()
const userStore = useUserStore()

const open        = ref(false)
const uploadError = ref('')

const initials = computed(() => userStore.displayName?.slice(0, 1) || 'U')

function toggle() { open.value = !open.value; uploadError.value = '' }
function close()  { open.value = false }

// 预设头像
const PRESETS = [
  { emoji: '🧑‍🔧', bg: '#eff6ff' },
  { emoji: '👩‍💼', bg: '#f0fdf4' },
  { emoji: '🧑‍💻', bg: '#faf5ff' },
  { emoji: '👨‍🏭', bg: '#fff7ed' },
  { emoji: '🤖', bg: '#f0f9ff' },
  { emoji: '⚙️', bg: '#fefce8' },
]

function selectPreset(url) {
  if (!url) return
  store.setAvatar(url)
}

function resetAvatar() {
  store.setAvatar(null)
}

// 上传本地图片，压缩到 128×128
function onFileChange(e) {
  uploadError.value = ''
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    uploadError.value = '请选择图片文件'; return
  }
  if (file.size > 4 * 1024 * 1024) {
    uploadError.value = '图片不能超过 4MB'; return
  }
  const reader = new FileReader()
  reader.onload = (ev) => {
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const size = 128
      canvas.width = size; canvas.height = size
      const ctx = canvas.getContext('2d')
      // 居中裁剪
      const min = Math.min(img.width, img.height)
      const sx = (img.width  - min) / 2
      const sy = (img.height - min) / 2
      ctx.drawImage(img, sx, sy, min, min, 0, 0, size, size)
      store.setAvatar(canvas.toDataURL('image/jpeg', 0.85))
    }
    img.src = ev.target.result
  }
  reader.readAsDataURL(file)
  e.target.value = ''
}

const vClickOutside = {
  mounted(el, binding) {
    el._clickOutside = (e) => { if (!el.contains(e.target)) binding.value() }
    document.addEventListener('mousedown', el._clickOutside)
  },
  unmounted(el) {
    document.removeEventListener('mousedown', el._clickOutside)
  },
}
</script>

<style scoped>
.av-wrap { position: relative; display: inline-flex; }

/* 触发头像圆圈 */
.av-trigger {
  width: 36px; height: 36px; border-radius: 50%;
  overflow: hidden; cursor: pointer;
  border: 2px solid rgba(255,255,255,.35);
  transition: border-color .15s, box-shadow .15s;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,.2);
}
.av-trigger:hover, .av-trigger.active {
  border-color: rgba(255,255,255,.7);
  box-shadow: 0 0 0 3px rgba(255,255,255,.15);
}
.av-img  { width: 100%; height: 100%; object-fit: cover; }
.av-text { font-size: 16px; font-weight: 700; color: #fff; line-height: 1; }

/* 下拉面板 */
.av-panel {
  position: absolute; top: calc(100% + 8px); right: 0;
  width: 260px;
  background: #fff; border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,27,63,.18), 0 0 0 1px rgba(0,27,63,.07);
  padding: 14px; z-index: 9999;
  display: flex; flex-direction: column; gap: 12px;
}
.av-drop-enter-active, .av-drop-leave-active {
  transition: opacity .18s ease, transform .18s ease;
}
.av-drop-enter-from, .av-drop-leave-to {
  opacity: 0; transform: translateY(-6px) scale(.98);
}

/* 用户信息行 */
.av-panel__head { display: flex; align-items: center; gap: 12px; }
.av-preview {
  width: 52px; height: 52px; border-radius: 50%; overflow: hidden;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.av-preview__img  { width: 100%; height: 100%; object-fit: cover; }
.av-preview__text { font-size: 22px; font-weight: 800; color: #fff; }
.av-user-info__name { font-size: 14px; font-weight: 700; color: #001b3f; }
.av-user-info__role { font-size: 12px; color: #8090a8; margin-top: 2px; }

/* 分区标题 */
.av-section-title {
  font-size: 12px; font-weight: 600; color: #8090a8;
  border-bottom: 1px solid #f0f4f8; padding-bottom: 6px;
}

/* 预设表情格 */
.av-presets {
  display: grid; grid-template-columns: repeat(6, 1fr); gap: 6px;
}
.av-preset {
  aspect-ratio: 1; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; cursor: pointer;
  border: 2px solid transparent;
  transition: transform .14s, border-color .14s;
}
.av-preset:hover   { transform: scale(1.1); }
.av-preset.selected { border-color: #3b82f6; box-shadow: 0 0 0 2px #bfdbfe; }

/* 上传 / 移除行 */
.av-upload-row {
  display: flex; gap: 8px; flex-wrap: wrap;
}
.av-upload-btn {
  flex: 1; text-align: center;
  padding: 6px 10px; border-radius: 8px;
  background: #f0f4f8; border: 1px dashed #c8d6e8;
  font-size: 12px; color: #3b5a80;
  cursor: pointer; transition: background .14s;
}
.av-upload-btn:hover { background: #dbeafe; border-color: #93c5fd; }
.av-reset-btn {
  padding: 6px 10px; border-radius: 8px;
  background: #fef2f2; border: 1px solid #fca5a5;
  font-size: 12px; color: #ef4444;
  cursor: pointer; transition: background .14s;
}
.av-reset-btn:hover { background: #fee2e2; }
.av-error { font-size: 11px; color: #ef4444; text-align: center; }
</style>
