<template>
  <div class="workshop-scene">
    <div ref="containerRef" class="workshop-scene__canvas" />
    <div v-if="hoverWorkshop" class="workshop-scene__hover">
      <div class="workshop-scene__hover-title">{{ hoverWorkshop.name }}</div>
      <div class="workshop-scene__hover-task">{{ hoverWorkshop.taskTitle }}</div>
      <div class="workshop-scene__hover-desc">{{ hoverWorkshop.taskDescription }}</div>
      <div class="workshop-scene__hover-bar">
        <span :style="{ width: `${hoverWorkshop.progress || 0}%` }" />
      </div>
      <div class="workshop-scene__hover-meta">
        {{ hoverWorkshop.progressLabel || `进度 ${hoverWorkshop.progress || 0}%` }}
        <template v-if="hoverWorkshop.workOrderNo"> · 工单 {{ hoverWorkshop.workOrderNo }}</template>
      </div>
    </div>
    <div v-if="selectedWorkshop" class="workshop-scene__detail">
      <div class="workshop-scene__detail-title">{{ selectedWorkshop.name }}</div>
      <div class="workshop-scene__detail-task">{{ selectedWorkshop.taskTitle }}</div>
      <div class="workshop-scene__detail-meta">
        {{ selectedWorkshop.department }} · 运行 {{ selectedWorkshop.running }}/{{ selectedWorkshop.total }}
      </div>
      <div class="workshop-scene__detail-progress">
        <div class="workshop-scene__detail-bar">
          <span :style="{ width: `${selectedWorkshop.progress || 0}%` }" />
        </div>
        <em>{{ selectedWorkshop.progressLabel || `${selectedWorkshop.progress || 0}%` }}</em>
      </div>
      <div class="workshop-scene__machines">
        <div v-for="m in selectedWorkshop.machines" :key="m.code" class="workshop-scene__machine" :class="`is-${(m.status || '').toLowerCase()}`">
          <span>{{ m.name }}</span>
          <em>{{ m.statusLabel }}</em>
        </div>
      </div>
      <div v-if="selectedOperators.length" class="workshop-scene__operators">
        <div class="workshop-scene__operators-title">现场操作员</div>
        <div v-for="op in selectedOperators" :key="op.username" class="workshop-scene__operator">
          <span class="workshop-scene__operator-dot" />
          <span>{{ op.displayName }}</span>
          <em>{{ operatorStatusLabel(op.username) }}</em>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import { createWorkshopScene, mergeWorkshopData } from '@/composables/useWorkshopScene'
import { operatorsForWorkshop } from '@/utils/operatorWorkshop'

const emit = defineEmits(['select'])

const props = defineProps({
  workshops: { type: Array, default: () => [] }
})

const containerRef = ref(null)
const selectedKey = ref('')
const hoverWorkshop = ref(null)
let sceneApi = null

const selectedWorkshop = computed(() =>
  props.workshops.find((w) => w.key === selectedKey.value) || null
)

const selectedOperators = computed(() => {
  if (!selectedKey.value) return []
  return operatorsForWorkshop(selectedKey.value)
})

function operatorStatusLabel(username) {
  const ws = selectedWorkshop.value
  if (!ws) return '待命'
  const running = (ws.machines || []).some(m => m.status === 'RUNNING')
  if (ws.status === 'running' && running) return '作业中'
  if (ws.status === 'running') return '巡检中'
  return '待命'
}

const displayWorkshops = computed(() => mergeWorkshopData(props.workshops))

function syncScene() {
  if (!sceneApi) return
  sceneApi.updateWorkshops(displayWorkshops.value, selectedKey.value)
}

watch(displayWorkshops, syncScene, { deep: true })

onMounted(() => {
  if (!containerRef.value) return
  sceneApi = createWorkshopScene(containerRef.value, {
    onSelectWorkshop(key) {
      selectedKey.value = selectedKey.value === key ? '' : key
      emit('select', selectedKey.value)
      syncScene()
    },
    onHoverWorkshop(data) {
      hoverWorkshop.value = data
    }
  })
  syncScene()
})

onUnmounted(() => {
  sceneApi?.dispose()
  sceneApi = null
})
</script>

<style scoped>
.workshop-scene {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 380px;
  background: #f4f6fa;
  border-radius: 8px;
}

.workshop-scene__canvas {
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.workshop-scene__hover {
  position: absolute;
  right: 12px;
  top: 52px;
  width: 240px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #e8eaf0;
  border-radius: 8px;
  font-size: 11px;
  color: #475569;
  pointer-events: none;
  box-shadow: 0 4px 20px rgba(31, 41, 55, 0.08);
}

.workshop-scene__hover-title {
  font-size: 13px;
  font-weight: 600;
  color: #172033;
  margin-bottom: 4px;
}

.workshop-scene__hover-task {
  font-size: 12px;
  color: #506784;
  margin-bottom: 4px;
}

.workshop-scene__hover-desc {
  color: #64748b;
  line-height: 1.45;
  margin-bottom: 8px;
}

.workshop-scene__hover-bar,
.workshop-scene__detail-bar {
  height: 6px;
  background: #f0f2f5;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 6px;
}

.workshop-scene__hover-bar span,
.workshop-scene__detail-bar span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #506784, #5a9a7a);
  border-radius: 3px;
  transition: width 0.35s ease;
}

.workshop-scene__hover-meta {
  color: #64748b;
  font-size: 10px;
}

.workshop-scene__detail {
  position: absolute;
  left: 12px;
  bottom: 12px;
  width: 240px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #e8eaf0;
  border-radius: 8px;
  font-size: 11px;
  color: #475569;
  box-shadow: 0 4px 20px rgba(31, 41, 55, 0.08);
}

.workshop-scene__detail-title {
  font-size: 13px;
  font-weight: 600;
  color: #172033;
  margin-bottom: 2px;
}

.workshop-scene__detail-task {
  font-size: 11px;
  color: #506784;
  margin-bottom: 4px;
}

.workshop-scene__detail-meta {
  color: #64748b;
  margin-bottom: 6px;
}

.workshop-scene__detail-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.workshop-scene__detail-progress .workshop-scene__detail-bar {
  flex: 1;
  margin-bottom: 0;
}

.workshop-scene__detail-progress em {
  font-style: normal;
  color: #506784;
  font-size: 10px;
  white-space: nowrap;
}

.workshop-scene__machines {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 120px;
  overflow: auto;
}

.workshop-scene__machine {
  display: flex;
  justify-content: space-between;
  padding: 4px 6px;
  background: #f8fafc;
  border-left: 2px solid #94a3b8;
}

.workshop-scene__machine.is-running { border-left-color: #5a9a7a; }
.workshop-scene__machine.is-fault { border-left-color: #c45c5c; }
.workshop-scene__machine.is-maintenance { border-left-color: #c9a227; }

.workshop-scene__machine em {
  font-style: normal;
  color: #94a3b8;
}

.workshop-scene__operators {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f2f5;
}

.workshop-scene__operators-title {
  font-size: 10px;
  font-weight: 600;
  color: #506784;
  margin-bottom: 4px;
}

.workshop-scene__operator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 0;
  font-size: 10px;
}

.workshop-scene__operator-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #5a9a7a;
  flex-shrink: 0;
}

.workshop-scene__operator em {
  margin-left: auto;
  font-style: normal;
  color: #94a3b8;
}
</style>

<style>
.workshop-3d-label {
  min-width: 132px;
  padding: 0;
  pointer-events: none;
}

.workshop-3d-card {
  padding: 6px 8px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid #e8eaf0;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 4px 16px rgba(31, 41, 55, 0.1);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.workshop-3d-label.is-hovered .workshop-3d-card {
  border-color: #506784;
  box-shadow: 0 4px 20px rgba(31, 41, 55, 0.14);
}

.workshop-3d-card__name {
  font-size: 13px;
  font-weight: 700;
  color: #172033;
  letter-spacing: 0;
  margin-bottom: 2px;
}

.workshop-3d-card__task {
  font-size: 10px;
  color: #506784;
  margin-bottom: 5px;
}

.workshop-3d-card__bar {
  height: 5px;
  background: #f0f2f5;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 3px;
}

.workshop-3d-card__bar span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #506784, #5a9a7a);
  border-radius: 3px;
  width: 0;
  transition: width 0.35s ease;
}

.workshop-3d-card__meta {
  font-size: 10px;
  font-weight: 500;
  color: #64748b;
  line-height: 1.3;
}

.operator-3d-label {
  padding: 2px 5px;
  font-size: 9px;
  font-weight: 600;
  color: #334155;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  white-space: nowrap;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.08);
  transform-origin: center bottom;
  pointer-events: none;
  user-select: none;
}

.operator-3d-label.is-working {
  color: #2e6b52;
  border-color: #a8d5c2;
  background: rgba(240, 252, 247, 0.95);
}

.operator-3d-label.is-chatting {
  color: #506784;
  border-color: #c5d0e0;
}

.operator-3d-chat {
  padding: 1px 5px;
  font-size: 9px;
  color: #506784;
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.1);
  transform-origin: center bottom;
  pointer-events: none;
  user-select: none;
}
</style>
