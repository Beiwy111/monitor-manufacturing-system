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

    <main class="isc-main">
      <FactoryScene ref="factorySceneRef" />

      <div class="isc-chart">
        <div class="isc-chart__head">
          <span class="isc-chart__title">设备运行情况</span>
          <span class="isc-chart__total">共 {{ kpi.total || 0 }} 台</span>
        </div>
        <BoardChart :option="statusChartOption" class="isc-chart__body" />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import FactoryScene from '@/views/device/FactoryScene.vue'
import BoardChart from '@/components/board/BoardChart.vue'
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


const statusChartOption = computed(() => {
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

  const data = items
    .map(({ key, value }) => ({
      name: STATUS_META[key].label,
      value,
      itemStyle: { color: STATUS_META[key].color }
    }))
    .filter((d) => d.value > 0)

  if (!data.length && total > 0) {
    data.push({ name: '设备', value: total, itemStyle: { color: '#3b82f6' } })
  }

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}：{c} 台 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 8,
      top: 'middle',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { fontSize: 12, color: '#606266' },
      formatter: (name) => {
        const hit = data.find((d) => d.name === name)
        return hit ? `${name}  ${hit.value}台` : name
      }
    },
    series: [{
      type: 'pie',
      radius: ['46%', '72%'],
      center: ['34%', '50%'],
      avoidLabelOverlap: true,
      label: {
        show: true,
        formatter: '{b}\n{c}台',
        fontSize: 12,
        color: '#4a5568',
        lineHeight: 16
      },
      labelLine: {
        show: true,
        length: 10,
        length2: 8
      },
      emphasis: {
        label: { show: true, fontSize: 13, fontWeight: 600 }
      },
      data
    }]
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

.isc-chart {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 4;
  width: 340px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 10px rgba(0, 27, 63, 0.08);
  pointer-events: auto;
}

.isc-chart__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px 4px;
  border-bottom: 1px solid #eef1f5;
}

.isc-chart__title {
  font-size: 12px;
  font-weight: 600;
  color: #001b3f;
}

.isc-chart__total {
  font-size: 11px;
  color: #909399;
}

.isc-chart__body {
  height: 220px;
}

.isc-chart__body :deep(.board-chart) {
  min-height: 0;
}

@media (max-width: 768px) {
  .isc-chart {
    width: 280px;
  }
  .isc-chart__body {
    height: 180px;
  }
}
</style>
