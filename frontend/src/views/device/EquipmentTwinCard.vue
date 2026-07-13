<template>
  <div class="twin-card" :class="`level-${level}`" @click.stop="$emit('click', data)">

    <!-- 设备图标（CSS 3D） -->
    <div class="twin-icon">
      <div class="twin-icon__body">
        <div class="twin-face twin-face--front">{{ typeIcon }}</div>
        <div class="twin-face twin-face--side" />
        <div class="twin-face twin-face--top" />
      </div>
      <div class="twin-glow" :style="{ background: levelColor }" />
    </div>

    <!-- SVG 健康环 -->
    <svg viewBox="0 0 44 44" width="52" height="52" style="flex-shrink:0">
      <circle cx="22" cy="22" r="18" fill="none" stroke="#eef1f5" stroke-width="4" />
      <circle cx="22" cy="22" r="18" fill="none"
        :stroke="levelColor" stroke-width="4" stroke-linecap="round"
        :stroke-dasharray="`${(113.1 * score / 100).toFixed(1)} 113.1`"
        stroke-dashoffset="28.3"
        style="transition: stroke-dasharray 0.9s ease" />
      <text x="22" y="26" text-anchor="middle" font-size="10" font-weight="700"
        :fill="levelColor">{{ score }}</text>
    </svg>

    <!-- 设备信息 -->
    <div class="twin-info">
      <div class="twin-code">{{ data.equipmentCode }}</div>
      <div class="twin-name" :title="data.equipmentName">{{ data.equipmentName }}</div>
      <div class="twin-status">
        <span class="twin-dot" :style="{ background: statusColor }" />
        {{ data.statusCn }}
      </div>
    </div>

    <!-- 健康等级标签 -->
    <div class="twin-level-tag"
      :style="{ background: levelColor + '22', color: levelColor, borderColor: levelColor + '55' }">
      {{ levelLabel }}
    </div>

    <!-- 3D 查看按钮 -->
    <button class="twin-3d-btn" @click.stop="viewerOpen = true">
      ⬡ 3D查看
    </button>

    <!-- 报警角标 -->
    <div v-if="data.alarm7d > 0" class="twin-badge">{{ data.alarm7d }}</div>

    <!-- 全屏 3D 查看器 -->
    <Equipment3DViewer v-model="viewerOpen" :data="data" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import Equipment3DViewer from './Equipment3DViewer.vue'

const props = defineProps({ data: { type: Object, required: true } })
defineEmits(['click'])

const viewerOpen = ref(false)

const score = computed(() => props.data?.healthScore ?? 100)
const level = computed(() => (props.data?.healthLevel ?? 'GOOD').toLowerCase())

const LEVEL_COLOR  = { good: '#67c23a', warn: '#e6a23c', alert: '#f56c6c', danger: '#c0392b' }
const LEVEL_LABEL  = { good: '优良',    warn: '注意',    alert: '警告',    danger: '危险'    }
const STATUS_COLOR = {
  RUNNING: '#67c23a', IDLE: '#909399', FAULT: '#f56c6c',
  MAINTAINING: '#e6a23c', SCRAPPED: '#c0c4cc'
}
const TYPE_ICON = {
  '流水': '🏭', '组装': '⚙️', '贴附': '🔧', '调校': '🎛️',
  '老化': '🌡️', '测试': '🔬', '包装': '📦'
}

const levelColor  = computed(() => LEVEL_COLOR[level.value]  ?? '#909399')
const levelLabel  = computed(() => LEVEL_LABEL[level.value]  ?? '—')
const statusColor = computed(() => STATUS_COLOR[props.data?.status] ?? '#909399')
const typeIcon    = computed(() => {
  const t = props.data?.equipmentType ?? ''
  for (const [k, v] of Object.entries(TYPE_ICON)) { if (t.includes(k)) return v }
  return '🏭'
})
</script>

<style scoped>
.twin-card {
  position: relative;
  background: #fff;
  border: 1.5px solid #eef1f5;
  border-radius: 12px;
  padding: 12px 10px 10px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  transition: box-shadow 0.22s, border-color 0.22s, transform 0.22s;
  user-select: none;
}
.twin-card:hover { transform: translateY(-4px); box-shadow: 0 10px 28px rgba(0,27,63,.13); }
.twin-card:hover .twin-3d-btn { opacity: 1; transform: translateY(0); }

.twin-card.level-good  { border-color: #e1f3d8; }
.twin-card.level-warn  { border-color: #faecd8; background: #fffbf5; }
.twin-card.level-alert { border-color: #fde2e2; background: #fff8f8; }
.twin-card.level-danger {
  border-color: #f5b7b1; background: #fff5f5;
  animation: pulse-danger 1.6s ease-in-out infinite;
}
@keyframes pulse-danger {
  0%,100% { box-shadow: 0 0 0 0 rgba(192,57,43,.3); }
  50%      { box-shadow: 0 0 0 8px rgba(192,57,43,0); }
}

/* CSS 3D 图标 */
.twin-icon { position: relative; perspective: 140px; width: 60px; height: 54px; }
.twin-icon__body {
  width: 42px; height: 38px; position: relative;
  transform-style: preserve-3d;
  transform: rotateX(14deg) rotateY(-22deg);
}
.twin-face { position: absolute; border-radius: 4px; }
.twin-face--front {
  width: 42px; height: 38px; transform: translateZ(8px);
  background: linear-gradient(135deg, #e8edf5, #c4d0e8);
  display: flex; align-items: center; justify-content: center; font-size: 22px;
  box-shadow: inset 0 1px 3px rgba(255,255,255,.6);
}
.twin-face--side {
  width: 16px; height: 38px; left: 42px; top: 0;
  background: linear-gradient(180deg, #a8b8d0, #7890b0);
  transform: rotateY(90deg) translateZ(-8px);
}
.twin-face--top {
  width: 42px; height: 16px; top: -16px;
  background: linear-gradient(90deg, #ccd8e8, #b0c0d8);
  transform: rotateX(90deg) translateZ(-8px);
}
.twin-glow {
  position: absolute; bottom: -4px; left: 50%; transform: translateX(-50%);
  width: 36px; height: 7px; border-radius: 50%; opacity: .4; filter: blur(5px);
}

/* 文字信息 */
.twin-info { text-align: center; width: 100%; }
.twin-code { font-size: 10px; color: #909399; }
.twin-name {
  font-size: 12px; font-weight: 600; color: #001b3f;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  max-width: 130px; margin: 1px auto 0;
}
.twin-status {
  display: flex; align-items: center; justify-content: center;
  gap: 4px; font-size: 11px; color: #606266; margin-top: 2px;
}
.twin-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }

/* 等级标签 */
.twin-level-tag {
  font-size: 10px; font-weight: 600;
  padding: 1px 8px; border-radius: 10px; border: 1px solid transparent;
}

/* 3D 查看按钮 */
.twin-3d-btn {
  font-size: 11px;
  color: #4a7aaa;
  background: #eef4fb;
  border: 1px solid #c8ddf0;
  border-radius: 8px;
  padding: 3px 10px;
  cursor: pointer;
  transition: background 0.15s, opacity 0.2s, transform 0.2s;
  opacity: 0;
  transform: translateY(4px);
  width: 100%;
}
.twin-3d-btn:hover { background: #d8eaf8; color: #1a5a9a; }

/* 报警角标 */
.twin-badge {
  position: absolute; top: 8px; right: 8px;
  background: #f56c6c; color: #fff;
  font-size: 10px; font-weight: 700;
  min-width: 18px; height: 18px; border-radius: 9px; padding: 0 4px;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 1px 4px rgba(245,108,108,.5);
  pointer-events: none;
}
</style>
