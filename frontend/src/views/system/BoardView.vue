<template>
  <div class="kanban">
    <header class="kanban__header">
      <div class="kanban__brand">
        <span class="kanban__brand-mark">MES</span>
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
            <span>四道工序从左往右流水线排布，同工序内 2~3 个车间纵向并列</span>
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
          <span>查看四道工序 · 十一车间实景</span>
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
          <PanelTitle title="工序负荷监控" subtitle="四道工序车间完成量/设备量" />
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
  legend: { right: 8, top: 0, textStyle: { color: '#8fb4d8', fontSize: 10 } },
  grid: { left: 42, right: 18, top: 38, bottom: 34 },
  xAxis: {
    type: 'category',
    data: workshops.value.map((w) => w.name.replace('车间', '')),
    axisLabel: { color: '#8fb4d8', fontSize: 10 },
    axisLine: { lineStyle: { color: 'rgba(143, 180, 216, 0.25)' } }
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#8fb4d8', fontSize: 10 },
    splitLine: { lineStyle: { color: 'rgba(143, 180, 216, 0.12)' } }
  },
  series: [
    {
      name: '计划/批量',
      type: 'bar',
      barWidth: 12,
      data: workshops.value.map((w) => w.batchTargetQty || batchTarget.value),
      itemStyle: { borderRadius: [6, 6, 0, 0], color: '#0f7cff' }
    },
    {
      name: '完成',
      type: 'bar',
      barWidth: 12,
      data: workshops.value.map((w) => w.batchCompletedQty || 0),
      itemStyle: { borderRadius: [6, 6, 0, 0], color: '#16e6a4' }
    },
    {
      name: '进度',
      type: 'line',
      yAxisIndex: 0,
      smooth: true,
      data: workshops.value.map((w) => w.progress || 0),
      symbolSize: 6,
      itemStyle: { color: '#ffd84d' },
      lineStyle: { color: '#ffd84d', width: 2 }
    }
  ]
}))

const equipmentChartOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: {
    bottom: 0,
    textStyle: { color: '#8fb4d8', fontSize: 10 }
  },
  series: [{
    type: 'pie',
    radius: ['46%', '70%'],
    center: ['50%', '43%'],
    label: { color: '#d8e8f8', formatter: '{b} {c}' },
    data: [
      { name: '运行', value: summary.value.running || 0, itemStyle: { color: '#3dd598' } },
      { name: '待机', value: summary.value.idle || 0, itemStyle: { color: '#607d8b' } },
      { name: '故障', value: summary.value.fault || 0, itemStyle: { color: '#ff6b6b' } }
    ]
  }]
}))

const resourceChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0, textStyle: { color: '#8fb4d8', fontSize: 10 } },
  grid: { left: 42, right: 18, top: 38, bottom: 30 },
  xAxis: {
    type: 'category',
    data: ['操作员', '车间', '设备'],
    axisLabel: { color: '#8fb4d8' },
    axisLine: { lineStyle: { color: 'rgba(143, 180, 216, 0.25)' } }
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#8fb4d8' },
    splitLine: { lineStyle: { color: 'rgba(143, 180, 216, 0.12)' } }
  },
  series: [
    {
      name: '总量',
      type: 'bar',
      data: [summary.value.availableOperators || 0, summary.value.workshopCount || workshops.value.length, summary.value.equipmentTotal || 0],
      itemStyle: { color: '#1e9bff', borderRadius: [6, 6, 0, 0] }
    },
    {
      name: '运行/激活',
      type: 'bar',
      data: [0, activeCount.value, summary.value.running || 0],
      itemStyle: { color: '#ffc84d', borderRadius: [6, 6, 0, 0] }
    }
  ]
}))

const stageChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0, textStyle: { color: '#8fb4d8', fontSize: 10 } },
  grid: { left: 42, right: 18, top: 38, bottom: 32 },
  xAxis: {
    type: 'category',
    data: stageRows.value.map((s) => s.name),
    axisLabel: { color: '#8fb4d8' },
    axisLine: { lineStyle: { color: 'rgba(143, 180, 216, 0.25)' } }
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#8fb4d8' },
    splitLine: { lineStyle: { color: 'rgba(143, 180, 216, 0.12)' } }
  },
  series: [
    {
      name: '完成量',
      type: 'bar',
      data: stageRows.value.map((s) => s.completed),
      itemStyle: { color: '#00c8ff', borderRadius: [6, 6, 0, 0] }
    },
    {
      name: '设备数',
      type: 'line',
      smooth: true,
      data: stageRows.value.map((s) => s.total),
      itemStyle: { color: '#67f58b' },
      lineStyle: { color: '#67f58b', width: 2 }
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

<style scoped>
.kanban {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 100px);
  min-height: 720px;
  padding: 10px 14px 14px;
  gap: 12px;
  overflow: hidden;
  color: #d8e8f8;
  background: radial-gradient(circle at 50% 0%, #10294c 0%, #061327 52%, #030912 100%);
  font-size: 12px;
}

.kanban__header {
  display: grid;
  grid-template-columns: 300px 1fr 360px;
  align-items: center;
  flex-shrink: 0;
}

.kanban__brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.kanban__brand-mark {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 6px;
  background: linear-gradient(135deg, #0088ff, #00d4ff);
  color: #fff;
  font-size: 11px;
  font-weight: 800;
}

.kanban__brand strong,
.kanban__brand small {
  display: block;
}

.kanban__brand strong {
  color: #fff;
  font-size: 13px;
}

.kanban__brand small {
  color: #75a0c7;
  font-size: 10px;
}

.kanban__header h1 {
  margin: 0;
  text-align: center;
  font-size: 22px;
  letter-spacing: 4px;
  color: #f0f8ff;
}

.kanban__meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  color: #83a7c8;
  font-size: 11px;
}

.kanban__status--ok { color: #3dd598; }
.kanban__status--watch { color: #ffd166; }
.kanban__status--warn { color: #ff6b6b; }
.kanban__btn :deep(span) { color: #00c8ff !important; }

.kanban__alert {
  flex-shrink: 0;
  padding: 6px 10px;
  color: #ffb4a8;
  background: rgba(180, 50, 40, 0.22);
  border: 1px solid rgba(255, 100, 80, 0.4);
}

.kanban__summary {
  display: flex;
  align-items: stretch;
  flex-shrink: 0;
  gap: 10px;
}

.kanban__chip {
  min-width: 106px;
  padding: 9px 12px;
  text-align: center;
  background: rgba(7, 30, 58, 0.86);
  border: 1px solid rgba(0, 180, 255, 0.2);
  border-radius: 6px;
}

.kanban__chip strong {
  display: block;
  color: #fff;
  font-size: 20px;
  line-height: 1.1;
}

.kanban__chip span {
  color: #7d9dba;
  font-size: 10px;
}

.kanban__chip.tone-ok { border-color: rgba(61, 213, 152, 0.5); }
.kanban__chip.tone-warn { border-color: rgba(255, 107, 107, 0.65); }

.kanban__hint {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 0 14px;
  color: #9fd6ff;
  background: rgba(0, 80, 140, 0.22);
  border-left: 3px solid #00aaff;
  border-radius: 4px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.workshop-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  grid-auto-rows: minmax(220px, 1fr);
  gap: 12px;
  overflow: auto;
}

.workshop-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
  padding: 14px;
  color: inherit;
  text-align: left;
  cursor: pointer;
  background: linear-gradient(180deg, rgba(8, 34, 65, 0.96), rgba(5, 19, 38, 0.96));
  border: 1px solid rgba(0, 180, 255, 0.22);
  border-radius: 10px;
  box-shadow: inset 0 0 28px rgba(0, 150, 255, 0.06);
}

.workshop-card:hover {
  border-color: rgba(0, 220, 255, 0.7);
}

.workshop-card.is-running {
  border-color: rgba(61, 213, 152, 0.6);
  box-shadow: 0 0 20px rgba(61, 213, 152, 0.12), inset 0 0 28px rgba(61, 213, 152, 0.06);
}

.workshop-card.st-abnormal {
  border-color: rgba(255, 107, 107, 0.7);
}

.workshop-card__top,
.workshop-card__numbers,
.workshop-card__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.workshop-card__top strong {
  display: block;
  color: #fff;
  font-size: 16px;
}

.workshop-card__top span,
.workshop-card__numbers,
.workshop-card__bottom span {
  color: #82a4c4;
  font-size: 10px;
}

.workshop-card__top em {
  flex-shrink: 0;
  padding: 4px 8px;
  color: #8ea6b8;
  font-style: normal;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 999px;
}

.workshop-card.is-running .workshop-card__top em {
  color: #3dd598;
  background: rgba(61, 213, 152, 0.12);
}

.workshop-card__numbers {
  align-items: flex-start;
  padding: 8px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.workshop-card__numbers span {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.workshop-card__bottom strong {
  color: #e8f4ff;
  font-size: 14px;
}

.flow-line {
  position: relative;
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 6px;
  align-items: center;
  min-height: 74px;
  padding: 0 8px;
  background:
    linear-gradient(90deg, transparent 0%, rgba(0, 180, 255, 0.22) 50%, transparent 100%) center / 100% 4px no-repeat,
    rgba(0, 20, 40, 0.45);
  border-radius: 8px;
  overflow: hidden;
}

.flow-line::before {
  content: "";
  position: absolute;
  left: -30%;
  top: 50%;
  width: 30%;
  height: 4px;
  background: linear-gradient(90deg, transparent, #3dd598, transparent);
  transform: translateY(-50%);
  opacity: 0;
}

.flow-line.is-moving::before {
  opacity: 1;
  animation: flowMove 5s linear infinite;
}

.flow-line__station {
  z-index: 1;
  height: 34px;
  border: 1px solid rgba(120, 160, 190, 0.5);
  border-radius: 5px;
  background: rgba(12, 42, 72, 0.95);
}

.flow-line__station.is-done {
  border-color: rgba(61, 213, 152, 0.8);
  background: rgba(61, 213, 152, 0.18);
}

.back-btn {
  align-self: flex-start;
  padding: 6px 12px;
  color: #4fc3f7;
  cursor: pointer;
  background: transparent;
  border: 1px solid rgba(0, 200, 255, 0.35);
  border-radius: 4px;
}

.workshop-detail {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.detail-hero {
  flex-shrink: 0;
  padding: 16px;
  background: linear-gradient(180deg, rgba(8, 34, 65, 0.96), rgba(5, 19, 38, 0.96));
  border: 1px solid rgba(0, 180, 255, 0.24);
  border-radius: 10px;
}

.detail-hero.is-running {
  border-color: rgba(61, 213, 152, 0.6);
}

.detail-hero__head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
}

.detail-hero h2 {
  margin: 0 0 6px;
  color: #fff;
  font-size: 22px;
}

.detail-hero p {
  margin: 0;
  color: #86a8c7;
}

.detail-hero__state {
  text-align: right;
}

.detail-hero__state strong,
.detail-hero__state span {
  display: block;
}

.detail-hero__state strong {
  color: #3dd598;
  font-size: 20px;
}

.detail-hero__state span {
  color: #8aa8c2;
  margin-top: 4px;
}

.detail-flow__track {
  display: grid;
  grid-template-columns: repeat(20, 1fr);
  gap: 5px;
  min-height: 84px;
  padding: 14px;
  background:
    linear-gradient(90deg, transparent 0%, rgba(0, 180, 255, 0.2) 50%, transparent 100%) center / 100% 5px no-repeat,
    rgba(0, 20, 40, 0.45);
  border-radius: 8px;
}

.detail-flow__track span {
  border: 1px solid rgba(120, 160, 190, 0.45);
  border-radius: 4px;
  background: rgba(12, 42, 72, 0.95);
}

.detail-flow__track span.is-done {
  border-color: rgba(61, 213, 152, 0.8);
  background: rgba(61, 213, 152, 0.2);
}

.detail-flow__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  color: #9fd6ff;
}

.detail-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 0.8fr 1.2fr;
  gap: 12px;
}

.detail-panel {
  min-height: 0;
  padding: 12px;
  overflow: auto;
  background: rgba(5, 20, 40, 0.82);
  border: 1px solid rgba(0, 180, 255, 0.18);
  border-radius: 8px;
}

.detail-panel h3 {
  margin: 0 0 10px;
  color: #fff;
  font-size: 14px;
}

.line-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 10px;
  align-items: center;
  padding: 10px;
  margin-bottom: 8px;
  background: rgba(0, 40, 80, 0.32);
  border-left: 3px solid #607d8b;
  border-radius: 4px;
}

.line-row.st-running { border-left-color: #3dd598; }
.line-row.st-abnormal { border-left-color: #ff6b6b; }
.line-row strong { color: #e8f4ff; }
.line-row span { color: #86a8c7; }
.line-row em { color: #8aa8c2; font-style: normal; }
.line-row.st-running em { color: #3dd598; }

.device-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.device-card {
  display: grid;
  gap: 4px;
  padding: 10px;
  background: rgba(0, 40, 80, 0.32);
  border-left: 3px solid #607d8b;
  border-radius: 4px;
}

.device-card.is-running { border-left-color: #3dd598; }
.device-card.is-fault { border-left-color: #ff6b6b; }
.device-card.is-maintenance { border-left-color: #ef6c00; }
.device-card strong { color: #e8f4ff; }
.device-card span { color: #7e9fbd; font-size: 10px; }
.device-card em { color: #8aa8c2; font-style: normal; }
.device-card.is-running em { color: #3dd598; }

.empty {
  color: #6e8aa4;
  text-align: center;
  padding: 24px;
}

@keyframes flowMove {
  from { left: -30%; }
  to { left: 100%; }
}

.board-main {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-rows: minmax(520px, 1.7fr) minmax(150px, 0.45fr);
  gap: 12px;
  overflow: hidden;
}

.scene-panel {
  position: relative;
  min-height: 520px;
  padding: 6px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(8, 34, 65, 0.72), rgba(5, 19, 38, 0.92)),
    radial-gradient(circle at 50% 20%, rgba(0, 180, 255, 0.16), transparent 55%);
  border: 1px solid rgba(0, 180, 255, 0.24);
  border-radius: 10px;
  box-shadow: inset 0 0 38px rgba(0, 180, 255, 0.08);
}

.scene-panel__head {
  position: absolute;
  z-index: 4;
  top: 12px;
  left: 14px;
  right: 14px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  pointer-events: none;
}

.scene-panel__head strong,
.scene-panel__head span {
  display: block;
}

.scene-panel__head strong {
  color: #fff;
  font-size: 14px;
}

.scene-panel__head span {
  margin-top: 3px;
  color: #83a7c8;
  font-size: 10px;
}

.scene-panel__head .back-btn {
  pointer-events: auto;
}

.scene-panel :deep(.workshop-scene) {
  height: 100%;
  min-height: 0;
}

.scene-panel :deep(.workshop-scene__detail) {
  display: none;
}

.scene-panel--full {
  flex: 1;
  min-height: 0;
}

.board-kpis {
  align-items: stretch;
}

.scene-entry {
  min-width: 190px;
  padding: 10px 16px;
  color: #e8f7ff;
  text-align: left;
  cursor: pointer;
  background:
    linear-gradient(135deg, rgba(0, 170, 255, 0.28), rgba(0, 255, 200, 0.12)),
    rgba(3, 18, 40, 0.76);
  border: 1px solid rgba(0, 216, 255, 0.5);
  border-radius: 6px;
  box-shadow: inset 0 0 22px rgba(0, 200, 255, 0.12), 0 0 18px rgba(0, 160, 255, 0.16);
}

.scene-entry strong,
.scene-entry span {
  display: block;
}

.scene-entry strong {
  font-size: 15px;
  letter-spacing: 1px;
}

.scene-entry span {
  margin-top: 3px;
  color: #8fdcff;
  font-size: 11px;
}

.dashboard-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 1.25fr 1fr 1.1fr;
  grid-template-rows: minmax(250px, 1fr) minmax(250px, 1fr);
  gap: 12px;
  overflow: hidden;
}

.dashboard-card {
  position: relative;
  min-height: 0;
  padding: 42px 12px 12px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(8, 34, 70, 0.86), rgba(4, 16, 36, 0.96)),
    radial-gradient(circle at 70% 0%, rgba(0, 170, 255, 0.16), transparent 55%);
  border: 1px solid rgba(58, 178, 255, 0.38);
  border-radius: 8px;
  box-shadow: inset 0 0 30px rgba(0, 126, 255, 0.12), 0 0 18px rgba(0, 50, 120, 0.24);
}

.dashboard-card::before,
.dashboard-card::after {
  content: '';
  position: absolute;
  width: 28px;
  height: 18px;
  border-color: rgba(118, 223, 255, 0.75);
  pointer-events: none;
}

.dashboard-card::before {
  left: 0;
  top: 0;
  border-left: 2px solid;
  border-top: 2px solid;
}

.dashboard-card::after {
  right: 0;
  bottom: 0;
  border-right: 2px solid;
  border-bottom: 2px solid;
}

.dashboard-card--equipment {
  grid-column: span 2;
}

.panel-title {
  position: absolute;
  top: 10px;
  left: 12px;
  right: 12px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(84, 194, 255, 0.16);
}

.panel-title strong {
  color: #eaf7ff;
  font-size: 15px;
  letter-spacing: 1px;
}

.panel-title span {
  color: #79a9cf;
  font-size: 10px;
}

.equipment-layout {
  height: 100%;
  display: grid;
  grid-template-columns: minmax(260px, 0.9fr) 1.1fr;
  gap: 12px;
}

.equipment-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  align-content: center;
}

.equipment-card {
  min-height: 84px;
  padding: 12px;
  background: rgba(0, 70, 150, 0.22);
  border: 1px solid rgba(76, 183, 255, 0.35);
  border-radius: 8px;
  box-shadow: inset 0 0 22px rgba(0, 150, 255, 0.08);
}

.equipment-card span,
.equipment-card em {
  display: block;
  color: #85a9c6;
  font-style: normal;
  font-size: 11px;
}

.equipment-card strong {
  display: block;
  margin: 6px 0;
  color: #37e8b0;
  font-size: 26px;
}

.live-table {
  height: 100%;
  overflow: auto;
  padding-right: 4px;
}

.live-table__head,
.live-table__row {
  display: grid;
  grid-template-columns: 1.4fr 0.9fr 0.7fr 0.8fr 0.7fr;
  gap: 8px;
  align-items: center;
}

.live-table__head {
  position: sticky;
  top: 0;
  z-index: 2;
  padding: 7px 10px;
  color: #6fb6e4;
  font-size: 11px;
  background: rgba(4, 18, 38, 0.96);
  border-bottom: 1px solid rgba(76, 183, 255, 0.2);
}

.live-table__row {
  width: 100%;
  margin-top: 6px;
  padding: 8px 10px;
  color: #dbefff;
  text-align: left;
  cursor: pointer;
  background: rgba(0, 60, 130, 0.16);
  border: 1px solid rgba(65, 168, 238, 0.18);
  border-radius: 5px;
}

.live-table__row:hover {
  border-color: rgba(0, 225, 255, 0.55);
  background: rgba(0, 126, 255, 0.22);
}

.live-table__row em {
  color: #35e6a8;
  font-style: normal;
}

.live-table__row strong {
  color: #f9d65c;
}

.alarm-list {
  height: 100%;
  overflow: auto;
}

.alarm-empty {
  height: 100%;
  display: grid;
  place-items: center;
  color: #7fa4c5;
}

.alarm-item {
  display: grid;
  grid-template-columns: 74px 1fr auto;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
  padding: 9px 10px;
  background: rgba(255, 128, 0, 0.08);
  border: 1px solid rgba(255, 190, 88, 0.2);
  border-radius: 6px;
}

.alarm-item strong {
  color: #ffc45d;
}

.alarm-item span {
  min-width: 0;
  color: #dbefff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alarm-item em {
  color: #8aa8c2;
  font-style: normal;
  font-size: 10px;
}

.scene-detail {
  position: absolute;
  z-index: 5;
  right: 14px;
  bottom: 14px;
  width: 360px;
  padding: 12px;
  background: rgba(4, 12, 24, 0.92);
  border: 1px solid rgba(0, 200, 255, 0.32);
  border-radius: 8px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.42);
}

.scene-detail.is-running {
  border-color: rgba(61, 213, 152, 0.62);
}

.scene-detail strong,
.scene-detail span {
  display: block;
}

.scene-detail strong {
  color: #fff;
  font-size: 15px;
}

.scene-detail span {
  margin-top: 3px;
  color: #8fb4d8;
  font-size: 11px;
}

.scene-detail__progress {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.scene-detail__progress em {
  color: #3dd598;
  font-style: normal;
  font-weight: 700;
  white-space: nowrap;
}

.scene-detail__progress i {
  flex: 1;
  height: 7px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 999px;
}

.scene-detail__progress b {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #0288d1, #3dd598);
  border-radius: 999px;
}

.scene-detail__meta {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px 10px;
  margin-top: 10px;
}

.scene-detail__meta span {
  margin: 0;
  color: #9fd6ff;
  font-size: 10px;
}

.analytics-grid {
  min-height: 0;
  display: grid;
  grid-template-columns: 1.2fr 0.8fr 1fr;
  gap: 12px;
}

.chart-card,
.status-table {
  min-height: 0;
  padding: 10px 12px;
  overflow: hidden;
  background: rgba(5, 20, 40, 0.86);
  border: 1px solid rgba(0, 180, 255, 0.18);
  border-radius: 8px;
}

.chart-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 24px;
  margin-bottom: 6px;
}

.chart-card__head strong {
  color: #fff;
  font-size: 13px;
}

.chart-card__head span {
  color: #7d9dba;
  font-size: 10px;
}

.chart-card :deep(.board-chart) {
  height: calc(100% - 30px);
  min-height: 132px;
}

.status-table {
  display: flex;
  flex-direction: column;
}

.status-table__head,
.status-table__row {
  display: grid;
  grid-template-columns: 1.2fr 0.7fr 0.7fr 0.7fr;
  gap: 8px;
  align-items: center;
}

.status-table__head {
  padding: 5px 8px;
  color: #6f8ca8;
  font-size: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.status-table__row {
  width: 100%;
  padding: 7px 8px;
  color: #d8e8f8;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.status-table__row:hover,
.status-table__row.is-active {
  background: rgba(0, 180, 255, 0.1);
}

.status-table__row em {
  color: #8aa8c2;
  font-style: normal;
}

.status-table__row strong {
  color: #3dd598;
}
</style>
