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
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import { createWorkshopScene, mergeWorkshopData } from '@/composables/useWorkshopScene'

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
}

.workshop-scene__canvas {
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.workshop-scene__hover {
  position: absolute;
  right: 12px;
  top: 12px;
  width: 240px;
  padding: 10px 12px;
  background: rgba(6, 16, 31, 0.92);
  border: 1px solid rgba(0, 200, 255, 0.45);
  border-radius: 4px;
  font-size: 11px;
  color: #c8d9ef;
  pointer-events: none;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.45);
}

.workshop-scene__hover-title {
  font-size: 13px;
  font-weight: 600;
  color: #e8f4ff;
  margin-bottom: 4px;
}

.workshop-scene__hover-task {
  font-size: 12px;
  color: #4fc3f7;
  margin-bottom: 4px;
}

.workshop-scene__hover-desc {
  color: #7a9abb;
  line-height: 1.45;
  margin-bottom: 8px;
}

.workshop-scene__hover-bar,
.workshop-scene__detail-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 6px;
}

.workshop-scene__hover-bar span,
.workshop-scene__detail-bar span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #0288d1, #43a047);
  border-radius: 3px;
  transition: width 0.35s ease;
}

.workshop-scene__hover-meta {
  color: #90caf9;
  font-size: 10px;
}

.workshop-scene__detail {
  position: absolute;
  left: 12px;
  bottom: 12px;
  width: 240px;
  padding: 10px 12px;
  background: rgba(6, 16, 31, 0.88);
  border: 1px solid rgba(0, 200, 255, 0.25);
  border-radius: 4px;
  font-size: 11px;
  color: #c8d9ef;
}

.workshop-scene__detail-title {
  font-size: 13px;
  font-weight: 600;
  color: #e8f4ff;
  margin-bottom: 2px;
}

.workshop-scene__detail-task {
  font-size: 11px;
  color: #4fc3f7;
  margin-bottom: 4px;
}

.workshop-scene__detail-meta {
  color: #7a9abb;
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
  color: #90caf9;
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
  background: rgba(0, 80, 160, 0.12);
  border-left: 2px solid #607d8b;
}

.workshop-scene__machine.is-running { border-left-color: #43a047; }
.workshop-scene__machine.is-fault { border-left-color: #c62828; }
.workshop-scene__machine.is-maintenance { border-left-color: #ef6c00; }

.workshop-scene__machine em {
  font-style: normal;
  color: #7a9abb;
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
  background: rgba(3, 10, 20, 0.78);
  border: 1px solid rgba(79, 195, 247, 0.5);
  border-radius: 6px;
  text-align: center;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.38);
  backdrop-filter: blur(2px);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.workshop-3d-label.is-hovered .workshop-3d-card {
  border-color: rgba(0, 255, 200, 0.85);
  box-shadow: 0 0 18px rgba(0, 200, 255, 0.35);
}

.workshop-3d-card__name {
  font-size: 14px;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: 1px;
  margin-bottom: 2px;
  text-shadow: 0 1px 3px #000, 0 0 8px rgba(79, 195, 247, 0.45);
}

.workshop-3d-card__task {
  font-size: 10px;
  color: #8fe8ff;
  margin-bottom: 5px;
  text-shadow: 0 1px 2px #000;
}

.workshop-3d-card__bar {
  height: 5px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 3px;
}

.workshop-3d-card__bar span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #0288d1, #43a047);
  border-radius: 3px;
  width: 0;
  transition: width 0.35s ease;
}

.workshop-3d-card__meta {
  font-size: 10px;
  font-weight: 600;
  color: #c8f4ff;
  line-height: 1.3;
  text-shadow: 0 1px 2px #000;
}
</style>
