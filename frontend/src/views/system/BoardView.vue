<template>
  <div class="kanban">
    <header class="kanban__header">
      <div class="kanban__brand">
        <div>
          <strong>生产主管实时大屏</strong>
          <small>数据源：数据库实时快照 /mes/dashboard/snapshot</small>
        </div>
      </div>
      <h1>{{ sceneMode ? '3D 车间实景模拟' : '生产综合看板' }}</h1>
      <div class="kanban__meta">
        <span>{{ currentTime }}</span>
        <span :class="statusClass">系统 {{ snapshot.systemStatus }}</span>
        <span>{{ loading ? '同步中…' : lastRefreshTime }}</span>
        <el-button link size="small" class="kanban__btn" @click="refreshDashboard">刷新</el-button>
      </div>
    </header>

    <div v-if="apiError" class="kanban__alert">{{ apiError }}</div>

    <template v-if="sceneMode">
      <section class="scene-panel scene-panel--full">
        <div class="scene-panel__head">
          <div>
            <strong>3D 车间实景模拟</strong>
            <span>八道生产工序从左往右流水线排布，同工序内 2~3 个车间纵向并列</span>
          </div>
          <button type="button" class="back-btn" @click="sceneMode = false">返回综合看板</button>
        </div>
        <WorkshopScene3D :workshops="workshops" @select="onSelectWorkshop" />
      </section>
    </template>

    <template v-else>
      <section class="kanban__summary board-kpis">
        <div v-for="chip in summaryChips" :key="chip.key" class="kanban__chip" :class="chip.tone ? `tone-${chip.tone}` : ''">
          <strong>{{ chip.value }}</strong>
          <span>{{ chip.label }}</span>
        </div>
        <button type="button" class="scene-entry" @click="sceneMode = true">
          <strong>进入 3D 车间</strong>
          <span>查看八道生产工序 · 十九车间实景</span>
        </button>
      </section>

      <main class="dashboard-grid">
        <section class="dashboard-card dashboard-card--trend">
          <PanelTitle title="车间生产进度分析" subtitle="按数据库实时车间快照统计" />
          <BoardChart :option="progressChartOption" />
        </section>

        <section class="dashboard-card dashboard-card--attendance">
          <PanelTitle title="车间人员与设备监控" subtitle="操作员/设备/运转车间" />
          <BoardChart :option="resourceChartOption" />
        </section>

        <section class="dashboard-card dashboard-card--stage">
          <PanelTitle title="工序负荷监控" subtitle="八道生产工序车间完成量/设备量" />
          <BoardChart :option="stageChartOption" />
        </section>

        <section class="dashboard-card dashboard-card--equipment">
          <PanelTitle title="车间设备性能监控" subtitle="设备运行分布与明细" />
          <div class="equipment-layout">
            <BoardChart :option="equipmentChartOption" />
            <div class="equipment-cards">
              <div v-for="item in equipmentCards" :key="item.key" class="equipment-card">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
                <em>{{ item.desc }}</em>
              </div>
            </div>
          </div>
        </section>

        <section class="dashboard-card dashboard-card--table">
          <PanelTitle title="车间状态表" subtitle="活数据 · 点击可进入对应 3D 车间" />
          <div class="live-table">
            <div class="live-table__head">
              <span>车间</span>
              <span>工序</span>
              <span>状态</span>
              <span>批量</span>
              <span>设备</span>
            </div>
            <button
              v-for="ws in workshops"
              :key="ws.key"
              type="button"
              class="live-table__row"
              @click="openWorkshop(ws.key)"
            >
              <span>{{ ws.name }}</span>
              <span>{{ ws.parentStepName || ws.taskTitle || '-' }}</span>
              <em>{{ ws.isRunning ? (ws.batchCompletedQty > 0 ? '生产中' : '已派工') : '待机' }}</em>
              <strong>{{ ws.batchCompletedQty || 0 }}/{{ ws.batchTargetQty || batchTarget }}</strong>
              <span>{{ ws.running || 0 }}/{{ ws.total || 0 }}</span>
            </button>
          </div>
        </section>

        <section class="dashboard-card dashboard-card--alarm">
          <PanelTitle title="异常预警监控" subtitle="安灯/设备/质量异常" />
          <div class="alarm-list">
            <div v-if="!alarms.length" class="alarm-empty">当前无未关闭异常</div>
            <div v-for="alarm in alarms.slice(0, 8)" :key="alarm.alarmNo || alarm.description" class="alarm-item">
              <strong>{{ alarm.alarmType || '异常' }}</strong>
              <span>{{ alarm.description || '-' }}</span>
              <em>{{ alarm.reportedAt || alarm.status || '-' }}</em>
            </div>
          </div>
        </section>
      </main>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, defineComponent, h } from 'vue'
import BoardChart from '@/components/board/BoardChart.vue'
import WorkshopScene3D from '@/components/board/WorkshopScene3D.vue'
import { useManagerDashboard } from '@/composables/useManagerDashboard'
import { mergeWorkshopData } from '@/composables/useWorkshopScene'

/** 大屏图表浅色配色 */
const BC = {
  axis: '#64748b',
  axisLine: '#e8eaf0',
  split: '#f0f2f5',
  primary: '#506784',
  primaryLight: '#7c9ab8',
  success: '#5a9a7a',
  warn: '#c9a227',
  danger: '#c45c5c',
  info: '#607d9b'
}

const PanelTitle = defineComponent({
  props: {
    title: { type: String, required: true },
    subtitle: { type: String, default: '' }
  },
  setup(props) {
    return () => h('div', { class: 'panel-title' }, [
      h('strong', props.title),
      props.subtitle ? h('span', props.subtitle) : null
    ])
  }
})

const { loading, apiError, lastRefreshTime, snapshot, refreshDashboard } = useManagerDashboard()

const selectedKey = ref('')
const sceneMode = ref(false)
const currentTime = ref('')
let clockTimer = null

const overview = computed(() => snapshot.value.productionOverview || {})
const summary = computed(() => overview.value.summary || {})
const workshops = computed(() => mergeWorkshopData(snapshot.value.workshops3d || overview.value.workshops || []))
const activeCount = computed(() => summary.value.activeWorkshops || workshops.value.filter((w) => w.isRunning).length)
const batchTarget = computed(() => overview.value.batchTargetQty || summary.value.batchTargetQty || 20)
const alarms = computed(() => Array.isArray(snapshot.value.alarms) ? snapshot.value.alarms : [])
const equipment = computed(() => Array.isArray(snapshot.value.equipment) ? snapshot.value.equipment : [])

const stageRows = computed(() => {
  const map = new Map()
  workshops.value.forEach((ws) => {
    const key = ws.parentStepKey || ws.parentStepName || ws.taskTitle || ws.key
    const row = map.get(key) || {
      key,
      name: ws.parentStepName || ws.taskTitle || ws.name,
      completed: 0,
      planned: 0,
      running: 0,
      total: 0,
      workshops: 0
    }
    row.completed += ws.completedQty || ws.batchCompletedQty || 0
    row.planned += ws.plannedQty || ws.batchTargetQty || batchTarget.value
    row.running += ws.running || 0
    row.total += ws.total || 0
    row.workshops += 1
    map.set(key, row)
  })
  return [...map.values()]
})

const summaryChips = computed(() => [
  { key: 'stages', label: '生产工序', value: `${summary.value.productionStageCount || 8}道` },
  { key: 'workshops', label: '车间', value: `${summary.value.workshopCount || workshops.value.length}个` },
  { key: 'active', label: '运转车间', value: `${activeCount.value}个`, tone: activeCount.value > 0 ? 'ok' : '' },
  { key: 'equipment', label: '设备', value: `${summary.value.equipmentTotal || 0}台` },
  { key: 'running', label: '运行设备', value: `${summary.value.running || 0}台`, tone: summary.value.running > 0 ? 'ok' : '' },
  { key: 'operator', label: '操作员', value: `${summary.value.availableOperators || 0}人` },
  { key: 'fault', label: '故障', value: `${summary.value.fault || 0}台`, tone: summary.value.fault > 0 ? 'warn' : '' }
])

const equipmentCards = computed(() => [
  { key: 'total', label: '设备总数', value: summary.value.equipmentTotal || equipment.value.length || 0, desc: '数据库设备台账' },
  { key: 'running', label: '运行中', value: summary.value.running || 0, desc: 'RUNNING 状态设备' },
  { key: 'idle', label: '待机', value: summary.value.idle || 0, desc: 'IDLE 状态设备' },
  { key: 'fault', label: '故障', value: summary.value.fault || 0, desc: 'FAULT 状态设备' }
])

const progressChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { right: 8, top: 0, textStyle: { color: BC.axis, fontSize: 10 } },
  grid: { left: 42, right: 18, top: 38, bottom: 34 },
  xAxis: {
    type: 'category',
    data: workshops.value.map((w) => w.name.replace('车间', '')),
    axisLabel: { color: BC.axis, fontSize: 10 },
    axisLine: { lineStyle: { color: BC.axisLine } }
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: BC.axis, fontSize: 10 },
    splitLine: { lineStyle: { color: BC.split } }
  },
  series: [
    {
      name: '计划/批量',
      type: 'bar',
      barWidth: 12,
      data: workshops.value.map((w) => w.batchTargetQty || batchTarget.value),
      itemStyle: { borderRadius: [6, 6, 0, 0], color: BC.primaryLight }
    },
    {
      name: '完成',
      type: 'bar',
      barWidth: 12,
      data: workshops.value.map((w) => w.batchCompletedQty || 0),
      itemStyle: { borderRadius: [6, 6, 0, 0], color: BC.success }
    },
    {
      name: '进度',
      type: 'line',
      yAxisIndex: 0,
      smooth: true,
      data: workshops.value.map((w) => w.progress || 0),
      symbolSize: 6,
      itemStyle: { color: BC.warn },
      lineStyle: { color: BC.warn, width: 2 }
    }
  ]
}))

const equipmentChartOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: {
    bottom: 0,
    textStyle: { color: BC.axis, fontSize: 10 }
  },
  series: [{
    type: 'pie',
    radius: ['46%', '70%'],
    center: ['50%', '43%'],
    label: { color: BC.axis, formatter: '{b} {c}' },
    data: [
      { name: '运行', value: summary.value.running || 0, itemStyle: { color: BC.success } },
      { name: '待机', value: summary.value.idle || 0, itemStyle: { color: BC.info } },
      { name: '故障', value: summary.value.fault || 0, itemStyle: { color: BC.danger } }
    ]
  }]
}))

const resourceChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0, textStyle: { color: BC.axis, fontSize: 10 } },
  grid: { left: 42, right: 18, top: 38, bottom: 30 },
  xAxis: {
    type: 'category',
    data: ['操作员', '车间', '设备'],
    axisLabel: { color: BC.axis },
    axisLine: { lineStyle: { color: BC.axisLine } }
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: BC.axis },
    splitLine: { lineStyle: { color: BC.split } }
  },
  series: [
    {
      name: '总量',
      type: 'bar',
      data: [summary.value.availableOperators || 0, summary.value.workshopCount || workshops.value.length, summary.value.equipmentTotal || 0],
      itemStyle: { color: BC.primary, borderRadius: [6, 6, 0, 0] }
    },
    {
      name: '运行/激活',
      type: 'bar',
      data: [0, activeCount.value, summary.value.running || 0],
      itemStyle: { color: BC.primaryLight, borderRadius: [6, 6, 0, 0] }
    }
  ]
}))

const stageChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0, textStyle: { color: BC.axis, fontSize: 10 } },
  grid: { left: 42, right: 18, top: 38, bottom: 32 },
  xAxis: {
    type: 'category',
    data: stageRows.value.map((s) => s.name),
    axisLabel: { color: BC.axis },
    axisLine: { lineStyle: { color: BC.axisLine } }
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: BC.axis },
    splitLine: { lineStyle: { color: BC.split } }
  },
  series: [
    {
      name: '完成量',
      type: 'bar',
      data: stageRows.value.map((s) => s.completed),
      itemStyle: { color: BC.primary, borderRadius: [6, 6, 0, 0] }
    },
    {
      name: '设备数',
      type: 'line',
      smooth: true,
      data: stageRows.value.map((s) => s.total),
      itemStyle: { color: BC.success },
      lineStyle: { color: BC.success, width: 2 }
    }
  ]
}))

const statusClass = computed(() => {
  const s = snapshot.value.systemStatus
  if (s === '预警') return 'kanban__status--warn'
  if (s === '关注') return 'kanban__status--watch'
  return 'kanban__status--ok'
})

function onSelectWorkshop(key) {
  selectedKey.value = key || ''
}

function openWorkshop(key) {
  selectedKey.value = key || ''
  sceneMode.value = true
}

onMounted(() => {
  const tick = () => { currentTime.value = new Date().toLocaleString('zh-CN', { hour12: false }) }
  tick()
  clockTimer = setInterval(tick, 1000)
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped src="@/styles/board-light.css"></style>
