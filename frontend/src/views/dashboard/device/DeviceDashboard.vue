<template>
  <div class="isc-page">
    <header class="isc-topbar">
      <div class="isc-topbar__left">
        <span class="isc-live" />
        <h1 class="isc-topbar__title">生产车间设备 3D 监控</h1>
        <span class="isc-topbar__sub">八道生产工序 · 19 车间 · 实时同步</span>
      </div>
      <div class="isc-topbar__right">
        <span class="isc-topbar__clock">{{ clockText }}</span>
        <span class="isc-topbar__sync">数据 {{ updatedText }}</span>
        <button class="isc-topbar__btn" :disabled="loading" @click="loadAll">
          {{ loading ? '刷新中' : '刷新' }}
        </button>
      </div>
    </header>

    <section class="isc-status-strip">
      <div class="isc-status-strip__summary">
        <span>设备运行概览</span>
        <strong>{{ statusOverview.total }}</strong>
        <small>台设备</small>
      </div>
      <div class="isc-status-strip__items">
        <div v-for="item in statusOverview.items" :key="item.key" class="isc-status-item">
          <i :style="{ backgroundColor: item.color }" />
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>台</small>
        </div>
      </div>
      <div class="isc-status-strip__progress" aria-hidden="true">
        <span
          v-for="item in statusOverview.items"
          :key="item.key"
          :style="{ backgroundColor: item.color, flexGrow: item.value || 0.08 }"
        />
      </div>
    </section>

    <main class="isc-main">
      <FactoryScene ref="factorySceneRef" />
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import FactoryScene from '@/views/device/FactoryScene.vue'
import {
  fetchEquipmentKpi, fetchEquipmentViews, fetchEquipmentHealth
} from '@/api/business'

const STATUS_META = {
  RUNNING: { label: '运行', color: '#3b82f6' },
  IDLE: { label: '空闲', color: '#52c1a2' },
  FAULT: { label: '故障', color: '#ef4444' },
  MAINTAINING: { label: '维保', color: '#f59e0b' },
  SCRAPPED: { label: '报废', color: '#6b7280' }
}

const loading = ref(false)
const factorySceneRef = ref(null)
const kpi = ref({})
const equipments = ref([])
const healthList = ref([])
const updatedAt = ref(null)
const now = ref(new Date())
let timer = null
let clockTimer = null

const updatedText = computed(() => {
  if (!updatedAt.value) return '—'
  const d = updatedAt.value
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
})

const clockText = computed(() => {
  const d = now.value
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
})


const statusOverview = computed(() => {
  const source = healthList.value.length ? healthList.value : equipments.value
  const count = (status) => source.filter((e) => e.status === status).length

  const items = [
    { key: 'RUNNING', value: count('RUNNING') },
    { key: 'IDLE', value: count('IDLE') },
    { key: 'FAULT', value: count('FAULT') || Number(kpi.value.fault || 0) },
    { key: 'MAINTAINING', value: count('MAINTAINING') || Number(kpi.value.maintaining || 0) },
    { key: 'SCRAPPED', value: count('SCRAPPED') || Number(kpi.value.scrapped || 0) }
  ]

  const listed = items.reduce((s, i) => s + i.value, 0)
  const total = Number(kpi.value.total || 0)
  if (listed < total) {
    const idleIdx = items.findIndex((i) => i.key === 'IDLE')
    items[idleIdx].value += Math.max(0, total - listed)
  }

  return {
    total: total || items.reduce((sum, item) => sum + item.value, 0),
    items: items.map(({ key, value }) => ({ key, value, ...STATUS_META[key] }))
  }
})

async function loadAll() {
  loading.value = true
  try {
    const [kRes, eqRes, hRes] = await Promise.allSettled([
      fetchEquipmentKpi(), fetchEquipmentViews(), fetchEquipmentHealth({ silent: true })
    ])
    const pick = (res, fallback) => (res.status === 'fulfilled' ? (res.value ?? fallback) : fallback)
    kpi.value = pick(kRes, {})
    equipments.value = pick(eqRes, [])
    healthList.value = pick(hRes, [])
    updatedAt.value = new Date()

    const merged = healthList.value.length
      ? healthList.value.map((h) => {
          const view = equipments.value.find((e) => e.equipmentId === h.equipmentId) || {}
          return { ...view, ...h }
        })
      : equipments.value

    factorySceneRef.value?.syncEquipments(merged)

    const results = [kRes, eqRes, hRes]
    const failed = results.filter((r) => r.status === 'rejected')
    if (failed.length === results.length) {
      ElMessage.error(failed[0]?.reason?.message || '加载设备数据失败')
    }
  } catch (e) {
    ElMessage.error(e?.message || '加载设备数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAll()
  timer = setInterval(loadAll, 30000)
  clockTimer = setInterval(() => { now.value = new Date() }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped>
.isc-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 52px);
  min-height: 640px;
  margin: -12px;
  background: #fff;
  color: #4a5568;
  font-size: 12px;
  overflow: hidden;
}

.isc-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.isc-topbar__left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.isc-live {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #67c23a;
  box-shadow: 0 0 6px rgba(103, 194, 58, 0.5);
  animation: isc-pulse 2s infinite;
}

@keyframes isc-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.isc-topbar__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #001b3f;
  letter-spacing: 0.5px;
}

.isc-topbar__sub {
  font-size: 11px;
  color: #909399;
}

.isc-topbar__right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.isc-topbar__clock {
  font-family: Consolas, monospace;
  color: #606266;
  font-size: 12px;
}

.isc-topbar__sync {
  color: #909399;
  font-size: 11px;
}

.isc-topbar__btn {
  padding: 4px 14px;
  background: #fff;
  border: 1px solid #d8dee8;
  color: #606266;
  font-size: 11px;
  cursor: pointer;
}

.isc-topbar__btn:hover:not(:disabled) {
  border-color: #3b82f6;
  color: #1d4ed8;
  background: #f0f7ff;
}

.isc-main {
  flex: 1;
  min-height: 0;
  position: relative;
  background: #f5f7fa;
}

.isc-status-strip {
  position: relative;
  display: flex;
  align-items: center;
  gap: 22px;
  min-height: 56px;
  padding: 7px 18px 9px;
  border-bottom: 1px solid #e4e7ed;
  background: rgba(255, 255, 255, 0.98);
  flex-shrink: 0;
}

.isc-status-strip__summary {
  display: flex;
  align-items: baseline;
  gap: 5px;
  min-width: 166px;
  padding-right: 20px;
  border-right: 1px solid #e8edf4;
}

.isc-status-strip__summary span {
  margin-right: 5px;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}

.isc-status-strip__summary strong {
  color: #0f2747;
  font-size: 22px;
  line-height: 1;
}

.isc-status-strip__summary small,
.isc-status-item small {
  color: #94a3b8;
  font-size: 10px;
}

.isc-status-strip__items {
  display: flex;
  align-items: center;
  gap: clamp(18px, 4vw, 54px);
  flex: 1;
}

.isc-status-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
  white-space: nowrap;
}

.isc-status-item i {
  align-self: center;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgba(148, 163, 184, 0.12);
}

.isc-status-item span {
  color: #64748b;
  font-size: 11px;
}

.isc-status-item strong {
  color: #0f2747;
  font-size: 16px;
}

.isc-status-strip__progress {
  position: absolute;
  right: 18px;
  bottom: 4px;
  left: 204px;
  display: flex;
  height: 2px;
  overflow: hidden;
  border-radius: 2px;
  opacity: 0.72;
}

@media (max-width: 900px) {
  .isc-status-strip {
    gap: 12px;
    padding-inline: 12px;
  }

  .isc-status-strip__summary {
    min-width: 140px;
    padding-right: 10px;
  }

  .isc-status-strip__summary span {
    display: none;
  }

  .isc-status-strip__items {
    justify-content: space-between;
    gap: 8px;
  }

  .isc-status-strip__progress {
    right: 12px;
    left: 164px;
  }
}
</style>
