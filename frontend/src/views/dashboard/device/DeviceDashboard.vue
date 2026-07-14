<template>
  <div class="dev-dash">
    <!-- 头部 -->
    <div class="dd-head">
      <div class="dd-head__left">
        <span class="dd-title">设备管理中心</span>
        <span class="dd-sub">八道生产工序 · 19 个生产车间 · 与生产主管大屏数据同步</span>
      </div>
      <div class="dd-head__right">
        <span class="dd-time">更新于 {{ updatedText }}</span>
        <el-button size="small" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <!-- KPI 卡片 -->
    <div class="dd-kpi">
      <div class="kpi" v-for="k in kpiCards" :key="k.label" :class="k.cls">
        <div class="kpi__bar" />
        <div class="kpi__body">
          <div class="kpi__value">{{ k.value }}<span v-if="k.suffix" class="kpi__suffix">{{ k.suffix }}</span></div>
          <div class="kpi__label">{{ k.label }}</div>
        </div>
      </div>
    </div>

    <!-- 3D 车间实景大屏 -->
    <div class="panel panel--scene">
      <div class="panel__title">
        生产车间 3D 实景
        <el-tag size="small" type="info" style="margin-left:8px">点击设备查看健康详情</el-tag>
      </div>
      <FactoryScene ref="factorySceneRef" class="dd-factory" />
    </div>

    <!-- 生产车间总览（8 工序 × 19 车间，活数据） -->
    <div class="panel panel--workshops">
      <div class="panel__title">
        生产车间状态
        <el-tag size="small" type="info" style="margin-left:8px">{{ workshops.length }} 个车间</el-tag>
        <el-tag size="small" type="success" style="margin-left:6px">{{ stageRows.length }} 道工序</el-tag>
      </div>
      <div class="stage-row">
        <div v-for="s in stageRows" :key="s.key" class="stage-card">
          <div class="stage-card__name">{{ s.name }}</div>
          <div class="stage-card__meta">
            <span>{{ s.workshops }} 车间</span>
            <span>{{ s.equipment }} 台设备</span>
            <span :class="s.fault > 0 ? 'text-danger' : 'text-ok'">{{ s.running }} 运行 / {{ s.fault }} 故障</span>
          </div>
        </div>
      </div>
      <el-table :data="workshops" border stripe size="small" max-height="280" style="margin-top:10px">
        <el-table-column prop="parentStepName" label="工序" width="100" />
        <el-table-column prop="name" label="车间" min-width="140" />
        <el-table-column label="设备" width="64" align="center">
          <template #default="{ row }">{{ row.total || (row.machines?.length || 0) }}</template>
        </el-table-column>
        <el-table-column label="运行" width="64" align="center">
          <template #default="{ row }"><span class="text-ok">{{ row.running || row.active || 0 }}</span></template>
        </el-table-column>
        <el-table-column label="故障" width="64" align="center">
          <template #default="{ row }">
            <span :class="Number(row.fault || row.abnormal) > 0 ? 'text-danger' : ''">{{ row.fault || row.abnormal || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="88" align="center">
          <template #default="{ row }">
            <el-tag :type="wsStatusType(row.status)" size="small">{{ wsStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="在制工单" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.workOrderNo || '—' }}</template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 图表区 -->
    <div class="dd-row dd-row--charts">
      <div class="panel">
        <div class="panel__title">设备状态分布</div>
        <BoardChart v-if="productionEquipments.length" :option="statusDonut" class="panel__chart" />
        <div v-else class="panel__empty">暂无数据</div>
      </div>
      <div class="panel">
        <div class="panel__title">设备完好率</div>
        <BoardChart v-if="equipments.length" :option="healthGauge" class="panel__chart" />
        <div v-else class="panel__empty">暂无数据</div>
        <div class="panel__foot">完好 = 空闲/运行设备占比（不含故障、维保、报废）</div>
      </div>
      <div class="panel">
        <div class="panel__title">各车间设备分布（19 个生产车间）</div>
        <BoardChart v-if="workshops.length" :option="workshopBar" class="panel__chart" />
        <div v-else class="panel__empty">暂无数据</div>
      </div>
    </div>

    <!-- 列表区 -->
    <div class="dd-row dd-row--lists">
      <div class="panel">
        <div class="panel__title">
          待处理报警
          <el-tag v-if="openAlarms.length" type="danger" size="small" round style="margin-left:8px">{{ openAlarms.length }}</el-tag>
        </div>
        <el-table v-if="openAlarms.length" :data="openAlarms" size="small" :show-header="true" max-height="260">
          <el-table-column prop="alarmNo" label="报警号" width="128" />
          <el-table-column prop="equipmentName" label="设备" min-width="120" show-overflow-tooltip />
          <el-table-column label="级别" width="66" align="center">
            <template #default="{ row }"><el-tag :type="levelType(row.alarmLevel)" size="small">{{ row.alarmLevelCn }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="alarmStatusCn" label="状态" width="72" align="center" />
          <el-table-column prop="reportedAt" label="上报时间" width="150" />
        </el-table>
        <div v-else class="panel__empty panel__empty--ok">✓ 当前无未闭环报警，设备运行平稳</div>
      </div>

      <div class="panel">
        <div class="panel__title">最近维保记录</div>
        <el-table v-if="recentMaintenance.length" :data="recentMaintenance" size="small" max-height="260">
          <el-table-column prop="maintenanceNo" label="单号" width="128" />
          <el-table-column prop="equipmentName" label="设备" min-width="110" show-overflow-tooltip />
          <el-table-column prop="maintenanceTypeCn" label="类型" width="60" align="center" />
          <el-table-column label="结果" width="76" align="center">
            <template #default="{ row }">
              <el-tag :type="row.inProgress ? 'warning' : (row.maintenanceResult==='COMPLETED' ? 'success' : 'info')" size="small">
                {{ row.maintenanceResultCn }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="downtimeMinutes" label="停机(分)" width="80" align="right" />
          <el-table-column prop="maintainerName" label="维护人" width="76" />
        </el-table>
        <div v-else class="panel__empty">暂无维保记录</div>
      </div>
    </div>

    <!-- 设备状态墙 -->
    <div class="panel">
      <div class="panel__title">生产设备状态墙（八道生产工序范围内）</div>
      <div v-if="productionEquipments.length === 0" class="panel__empty">暂无生产车间设备数据</div>
      <div v-else class="wall">
        <div v-for="e in productionEquipments" :key="e.equipmentId" class="wall__chip" :class="'is-'+e.status.toLowerCase()">
          <span class="wall__dot" />
          <div class="wall__info">
            <div class="wall__code">{{ e.equipmentCode }}</div>
            <div class="wall__name">{{ e.equipmentName }}</div>
            <div class="wall__ws">{{ e.parentStepName }} · {{ e.workshop }}</div>
          </div>
          <span class="wall__state">{{ e.statusCn }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import BoardChart from '@/components/board/BoardChart.vue'
import FactoryScene from '@/views/device/FactoryScene.vue'
import {
  fetchEquipmentKpi, fetchEquipmentViews, fetchAlarmViews, fetchMaintenanceViews,
  fetchEquipmentWorkshopOverview, fetchEquipmentHealth
} from '@/api/business'
import { mergeWorkshopData } from '@/composables/useWorkshopScene'
import { PRODUCTION_STAGES } from '@/utils/productionProgress'

// 状态语义色（与全局 Element 主题一致，红黄绿灰）
const STATUS_META = {
  IDLE: { cn: '空闲', color: '#909399' },
  RUNNING: { cn: '运行中', color: '#67c23a' },
  FAULT: { cn: '故障', color: '#f56c6c' },
  MAINTAINING: { cn: '维保中', color: '#e6a23c' },
  SCRAPPED: { cn: '已报废', color: '#c0c4cc' }
}

const loading = ref(false)
const factorySceneRef = ref(null)
const kpi = ref({})
const equipments = ref([])
const workshops = ref([])
const alarms = ref([])
const maintenance = ref([])
const updatedAt = ref(null)
let timer = null

const updatedText = computed(() => {
  if (!updatedAt.value) return '—'
  const d = updatedAt.value
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
})

const openAlarms = computed(() =>
  alarms.value.filter((a) => ['OPEN', 'RECEIVED', 'PROCESSING'].includes(a.alarmStatus))
)
const productionEquipments = computed(() =>
  equipments.value.filter((e) => e.isProductionWorkshop !== false)
)
const recentMaintenance = computed(() => maintenance.value.slice(0, 8))

const stageRows = computed(() => {
  const rows = PRODUCTION_STAGES.map((stage) => ({
    key: stage.stepKey,
    name: stage.stepName,
    workshops: 0,
    equipment: 0,
    running: 0,
    fault: 0
  }))
  for (const ws of workshops.value) {
    const row = rows.find((r) => r.key === ws.parentStepKey)
    if (!row) continue
    row.workshops += 1
    row.equipment += Number(ws.total || ws.machines?.length || 0)
    row.running += Number(ws.running || ws.active || 0)
    row.fault += Number(ws.fault || ws.abnormal || 0)
  }
  return rows
})

const healthRate = computed(() => {
  const total = kpi.value.total || 0
  if (!total) return 0
  return Math.round(((kpi.value.normal || 0) / total) * 100)
})

const kpiCards = computed(() => [
  { label: '生产车间设备', value: kpi.value.total || 0, cls: 'kpi--total' },
  { label: '完好率', value: healthRate.value, suffix: '%', cls: 'kpi--rate' },
  { label: '故障设备', value: kpi.value.fault || 0, cls: 'kpi--fault' },
  { label: '维保中', value: kpi.value.maintaining || 0, cls: 'kpi--maint' },
  { label: '未闭环报警', value: kpi.value.openAlarms || 0, cls: 'kpi--alarm' }
])

// 设备状态分布（从真实设备清单聚合）
const statusDistribution = computed(() => {
  const dist = {}
  for (const e of productionEquipments.value) dist[e.status] = (dist[e.status] || 0) + 1
  return dist
})

const statusDonut = computed(() => {
  const dist = statusDistribution.value
  const data = Object.keys(STATUS_META)
    .map((k) => ({ name: STATUS_META[k].cn, value: dist[k] || 0, itemStyle: { color: STATUS_META[k].color } }))
    .filter((d) => d.value > 0)
  return {
    tooltip: { trigger: 'item', formatter: '{b}：{c} 台（{d}%）' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { color: '#606266', fontSize: 12 } },
    title: {
      text: String(kpi.value.total || 0), subtext: '设备总数',
      left: 'center', top: '34%',
      textStyle: { fontSize: 30, fontWeight: 700, color: '#001b3f' },
      subtextStyle: { fontSize: 12, color: '#909399' }
    },
    series: [{
      type: 'pie', radius: ['54%', '74%'], center: ['50%', '44%'], avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
      label: { show: false }, labelLine: { show: false },
      data
    }]
  }
})

const healthGauge = computed(() => {
  const v = healthRate.value
  const color = v >= 80 ? '#67c23a' : v >= 50 ? '#e6a23c' : '#f56c6c'
  return {
    series: [{
      type: 'gauge', startAngle: 220, endAngle: -40, min: 0, max: 100, radius: '92%', center: ['50%', '56%'],
      progress: { show: true, width: 16, roundCap: true, itemStyle: { color } },
      axisLine: { lineStyle: { width: 16, color: [[1, '#eef1f5']] } },
      axisTick: { show: false }, splitLine: { show: false }, axisLabel: { show: false }, pointer: { show: false },
      anchor: { show: false }, title: { show: false },
      detail: { valueAnimation: true, formatter: '{value}%', offsetCenter: [0, '2%'],
        fontSize: 32, fontWeight: 700, color: '#001b3f' },
      data: [{ value: v }]
    }]
  }
})

const workshopBar = computed(() => {
  const names = workshops.value.map((w) => (w.name || '').replace('车间', ''))
  const counts = workshops.value.map((w) => Number(w.total || w.machines?.length || 0))
  return {
    grid: { left: 8, right: 18, top: 20, bottom: 6, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}：{c} 台' },
    xAxis: { type: 'category', data: names, axisLine: { lineStyle: { color: '#dcdfe6' } }, axisTick: { show: false }, axisLabel: { color: '#606266', fontSize: 11, rotate: 30 } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f0f2f5' } }, axisLabel: { color: '#909399' } },
    series: [{
      type: 'bar', data: counts, barWidth: '46%',
      itemStyle: { color: '#3b82f6', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', color: '#606266', fontSize: 12 }
    }]
  }
})

function wsStatusLabel(status) {
  return { running: '运行中', abnormal: '异常', pending: '待机', idle: '空闲' }[status] || status || '—'
}
function wsStatusType(status) {
  return { running: 'success', abnormal: 'danger', pending: 'info', idle: 'info' }[status] || 'info'
}

function levelType(l) {
  return { GENERAL: 'info', IMPORTANT: 'warning', URGENT: 'danger' }[l] || 'info'
}

async function loadAll() {
  loading.value = true
  try {
    const [kRes, eqRes, alRes, mrRes, ovRes] = await Promise.allSettled([
      fetchEquipmentKpi(), fetchEquipmentViews(), fetchAlarmViews(), fetchMaintenanceViews(),
      fetchEquipmentWorkshopOverview()
    ])
    const pick = (res, fallback) => (res.status === 'fulfilled' ? (res.value ?? fallback) : fallback)
    kpi.value = pick(kRes, {})
    equipments.value = pick(eqRes, [])
    alarms.value = pick(alRes, [])
    maintenance.value = pick(mrRes, [])
    workshops.value = mergeWorkshopData(pick(ovRes, {})?.workshops || [])
    updatedAt.value = new Date()
    try {
      const healthList = await fetchEquipmentHealth({ silent: true })
      factorySceneRef.value?.syncEquipments(Array.isArray(healthList) ? healthList : [])
    } catch { /* 3D 场景自行轮询 */ }
    const failed = [kRes, eqRes, alRes, mrRes, ovRes].filter((r) => r.status === 'rejected')
    if (failed.length === failed.length) {
      ElMessage.error(failed[0].reason?.message || '加载设备数据失败')
    } else if (ovRes.status === 'rejected') {
      console.warn('[DeviceDashboard] 车间总览加载失败，已使用本地车间模板', ovRes.reason)
    }
  } catch (e) {
    ElMessage.error(e?.message || '加载设备数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAll()
  timer = setInterval(loadAll, 30000) // 30s 自动刷新，贴近实时
})
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.dev-dash { padding: 16px 20px; background: #f4f6fa; min-height: 100%; }

.dd-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.dd-title { font-size: 20px; font-weight: 700; color: #001b3f; }
.dd-sub { margin-left: 12px; font-size: 13px; color: #909399; }
.dd-head__right { display: flex; align-items: center; gap: 12px; }
.dd-time { font-size: 12px; color: #a0a4ab; }

/* KPI */
.dd-kpi { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; margin-bottom: 14px; }
.kpi { position: relative; display: flex; align-items: stretch; background: #fff; border-radius: 10px; overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 27, 63, .06); }
.kpi__bar { width: 4px; flex: 0 0 4px; }
.kpi__body { padding: 16px 18px; }
.kpi__value { font-size: 30px; font-weight: 700; color: #001b3f; line-height: 1.1; }
.kpi__suffix { font-size: 15px; color: #909399; margin-left: 2px; }
.kpi__label { margin-top: 6px; font-size: 13px; color: #909399; }
.kpi--total .kpi__bar { background: #3b82f6; }
.kpi--rate .kpi__bar { background: #67c23a; }
.kpi--fault .kpi__bar { background: #f56c6c; }
.kpi--fault .kpi__value { color: #f56c6c; }
.kpi--maint .kpi__bar { background: #e6a23c; }
.kpi--alarm .kpi__bar { background: #f56c6c; }
.kpi--alarm .kpi__value { color: #e6a23c; }

/* 面板通用 */
.panel { background: #fff; border-radius: 10px; padding: 14px 16px; margin-bottom: 14px; box-shadow: 0 1px 3px rgba(0, 27, 63, .06); }
.panel__title { display: flex; align-items: center; font-size: 15px; font-weight: 600; color: #001b3f; margin-bottom: 10px; }
.panel__chart { height: 260px; }
.panel__foot { font-size: 11px; color: #a0a4ab; text-align: center; margin-top: 2px; }
.panel__empty { text-align: center; color: #a0a4ab; padding: 60px 0; font-size: 13px; }
.panel__empty--ok { color: #67c23a; }

.dd-row { display: grid; gap: 14px; }
.dd-row--charts { grid-template-columns: 1.1fr 1fr 1.2fr; }
.dd-row--lists { grid-template-columns: 1fr 1fr; }

.panel--scene { padding-bottom: 10px; }
.dd-factory { width: 100%; }

/* 设备状态墙 */
.panel--workshops { margin-bottom: 14px; }
.stage-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.stage-card { background: #f7f9fc; border: 1px solid #eef1f5; border-radius: 8px; padding: 10px 12px; }
.stage-card__name { font-size: 14px; font-weight: 600; color: #001b3f; }
.stage-card__meta { margin-top: 6px; display: flex; flex-wrap: wrap; gap: 8px; font-size: 12px; color: #909399; }
.text-danger { color: #f56c6c; font-weight: 600; }
.text-ok { color: #67c23a; font-weight: 600; }
.wall { display: grid; grid-template-columns: repeat(auto-fill, minmax(210px, 1fr)); gap: 10px; }
.wall__chip { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 8px; background: #f7f9fc; border: 1px solid #eef1f5; }
.wall__dot { width: 10px; height: 10px; border-radius: 50%; flex: 0 0 10px; background: #909399; }
.wall__info { min-width: 0; flex: 1; }
.wall__code { font-size: 12px; color: #909399; }
.wall__name { font-size: 13px; font-weight: 600; color: #001b3f; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.wall__state { font-size: 12px; color: #606266; }
.wall__chip.is-running { background: #f0f9eb; border-color: #e1f3d8; }
.wall__chip.is-running .wall__dot { background: #67c23a; }
.wall__chip.is-fault { background: #fef0f0; border-color: #fde2e2; }
.wall__chip.is-fault .wall__dot { background: #f56c6c; }
.wall__chip.is-fault .wall__state { color: #f56c6c; font-weight: 600; }
.wall__chip.is-maintaining { background: #fdf6ec; border-color: #faecd8; }
.wall__chip.is-maintaining .wall__dot { background: #e6a23c; }
.wall__chip.is-maintaining .wall__state { color: #e6a23c; font-weight: 600; }
.wall__chip.is-scrapped { opacity: .7; }
.wall__chip.is-scrapped .wall__dot { background: #c0c4cc; }

@media (max-width: 1200px) {
  .dd-kpi { grid-template-columns: repeat(2, 1fr); }
  .dd-row--charts, .dd-row--lists { grid-template-columns: 1fr; }
}
</style>
