<template>
  <div class="dev-dash">

    <!-- 头部 KPI 横条 -->
    <div class="dd-topbar">
      <div class="dd-topbar__left">
        <span class="dd-title">设备管理中心</span>
        <span class="dd-sub">安灯报警 · 维保闭环 · 实时状态</span>
      </div>
      <div class="dd-kpi-inline">
        <div v-for="k in kpiCards" :key="k.label" class="kpi-chip" :class="k.cls">
          <span class="kpi-chip__val">{{ k.value }}<span v-if="k.suffix" class="kpi-chip__sfx">{{ k.suffix }}</span></span>
          <span class="kpi-chip__label">{{ k.label }}</span>
        </div>
      </div>
      <div class="dd-topbar__right">
        <span class="dd-time">{{ updatedText }}</span>
        <el-button size="small" :loading="loading" @click="loadAll">刷新</el-button>
        <AvatarSetting />
      </div>
    </div>

    <!-- 3D 工厂全景（主视觉区） -->
    <FactoryScene ref="factorySceneRef" class="dd-factory" />

    <!-- 底部：图表 + 报警/维保列表 -->
    <div class="dd-bottom">

      <!-- 左侧三图 -->
      <div class="dd-charts">
        <div class="panel panel--sm">
          <div class="panel__title">设备状态分布</div>
          <BoardChart v-if="equipments.length" :option="statusDonut" class="panel__chart--sm" />
          <div v-else class="panel__empty">暂无数据</div>
        </div>
        <div class="panel panel--sm">
          <div class="panel__title">设备完好率</div>
          <BoardChart v-if="equipments.length" :option="healthGauge" class="panel__chart--sm" />
          <div v-else class="panel__empty">暂无数据</div>
          <div class="panel__foot">完好 = 空闲/运行占比（不含故障、维保、报废）</div>
        </div>
        <div class="panel panel--sm">
          <div class="panel__title">各车间设备分布</div>
          <BoardChart v-if="equipments.length" :option="workshopBar" class="panel__chart--sm" />
          <div v-else class="panel__empty">暂无数据</div>
        </div>
      </div>

      <!-- 右侧报警 + 维保列表 -->
      <div class="dd-lists">
        <div class="panel">
          <div class="panel__title">
            待处理报警
            <el-tag v-if="openAlarms.length" type="danger" size="small" round style="margin-left:8px">
              {{ openAlarms.length }}
            </el-tag>
          </div>
          <el-table v-if="openAlarms.length" :data="openAlarms" size="small" max-height="200">
            <el-table-column prop="alarmNo" label="报警号" width="120" />
            <el-table-column prop="equipmentName" label="设备" min-width="110" show-overflow-tooltip />
            <el-table-column label="级别" width="64" align="center">
              <template #default="{ row }">
                <el-tag :type="levelType(row.alarmLevel)" size="small">{{ row.alarmLevelCn }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="alarmStatusCn" label="状态" width="70" align="center" />
            <el-table-column prop="reportedAt" label="上报时间" width="148" />
          </el-table>
          <div v-else class="panel__empty panel__empty--ok">✓ 当前无未闭环报警，设备运行平稳</div>
        </div>

        <div class="panel" style="margin-top:14px">
          <div class="panel__title">最近维保记录</div>
          <el-table v-if="recentMaintenance.length" :data="recentMaintenance" size="small" max-height="200">
            <el-table-column prop="maintenanceNo" label="单号" width="120" />
            <el-table-column prop="equipmentName" label="设备" min-width="100" show-overflow-tooltip />
            <el-table-column prop="maintenanceTypeCn" label="类型" width="58" align="center" />
            <el-table-column label="结果" width="74" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.inProgress ? 'warning' : (row.maintenanceResult==='COMPLETED' ? 'success' : 'info')"
                  size="small">{{ row.maintenanceResultCn }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="downtimeMinutes" label="停机(分)" width="76" align="right" />
            <el-table-column prop="maintainerName" label="维护人" width="72" />
          </el-table>
          <div v-else class="panel__empty">暂无维保记录</div>
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
import AvatarSetting from '@/components/notification/AvatarSetting.vue'
import { fetchEquipmentKpi, fetchEquipmentViews, fetchAlarmViews, fetchMaintenanceViews } from '@/api/business'

// FactoryScene 组件 ref，用于驱动 3D 颜色更新
const factorySceneRef = ref(null)

const STATUS_META = {
  IDLE:       { cn: '空闲',   color: '#909399' },
  RUNNING:    { cn: '运行中', color: '#67c23a' },
  FAULT:      { cn: '故障',   color: '#f56c6c' },
  MAINTAINING:{ cn: '维保中', color: '#e6a23c' },
  SCRAPPED:   { cn: '已报废', color: '#c0c4cc' },
}

const loading     = ref(false)
const kpi         = ref({})
const equipments  = ref([])
const alarms      = ref([])
const maintenance = ref([])
const updatedAt   = ref(null)
let timer = null

const updatedText = computed(() => {
  if (!updatedAt.value) return '—'
  const d = updatedAt.value, p = n => String(n).padStart(2, '0')
  return `更新于 ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
})

const openAlarms       = computed(() => alarms.value.filter(a => ['OPEN','RECEIVED','PROCESSING'].includes(a.alarmStatus)))
const recentMaintenance = computed(() => maintenance.value.slice(0, 8))
const healthRate        = computed(() => {
  const total = kpi.value.total || 0
  return total ? Math.round(((kpi.value.normal || 0) / total) * 100) : 0
})

const kpiCards = computed(() => [
  { label: '设备总数',   value: kpi.value.total      || 0, cls: 'kpi--total' },
  { label: '完好率',     value: healthRate.value, suffix: '%', cls: 'kpi--rate' },
  { label: '故障设备',   value: kpi.value.fault       || 0, cls: 'kpi--fault' },
  { label: '维保中',     value: kpi.value.maintaining || 0, cls: 'kpi--maint' },
  { label: '未闭环报警', value: kpi.value.openAlarms  || 0, cls: 'kpi--alarm' },
])

const statusDistribution = computed(() => {
  const dist = {}
  for (const e of equipments.value) dist[e.status] = (dist[e.status] || 0) + 1
  return dist
})

const statusDonut = computed(() => {
  const dist = statusDistribution.value
  const data = Object.keys(STATUS_META)
    .map(k => ({ name: STATUS_META[k].cn, value: dist[k] || 0, itemStyle: { color: STATUS_META[k].color } }))
    .filter(d => d.value > 0)
  return {
    tooltip: { trigger: 'item', formatter: '{b}：{c} 台（{d}%）' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { color: '#606266', fontSize: 11 } },
    title: {
      text: String(kpi.value.total || 0), subtext: '设备总数',
      left: 'center', top: '30%',
      textStyle: { fontSize: 26, fontWeight: 700, color: '#001b3f' },
      subtextStyle: { fontSize: 11, color: '#909399' },
    },
    series: [{
      type: 'pie', radius: ['52%', '72%'], center: ['50%', '42%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
      label: { show: false }, labelLine: { show: false }, data,
    }],
  }
})

const healthGauge = computed(() => {
  const v = healthRate.value
  const color = v >= 80 ? '#67c23a' : v >= 50 ? '#e6a23c' : '#f56c6c'
  return {
    series: [{
      type: 'gauge', startAngle: 220, endAngle: -40, min: 0, max: 100,
      radius: '90%', center: ['50%', '58%'],
      progress: { show: true, width: 14, roundCap: true, itemStyle: { color } },
      axisLine: { lineStyle: { width: 14, color: [[1, '#eef1f5']] } },
      axisTick: { show: false }, splitLine: { show: false }, axisLabel: { show: false },
      pointer: { show: false }, anchor: { show: false }, title: { show: false },
      detail: { valueAnimation: true, formatter: '{value}%', offsetCenter: [0, '4%'],
        fontSize: 28, fontWeight: 700, color: '#001b3f' },
      data: [{ value: v }],
    }],
  }
})

const workshopBar = computed(() => {
  const map = {}
  for (const e of equipments.value) {
    const ws = e.workshop || '未分配'
    map[ws] = (map[ws] || 0) + 1
  }
  const names = Object.keys(map)
  return {
    grid: { left: 8, right: 16, top: 18, bottom: 6, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}：{c} 台' },
    xAxis: { type: 'category', data: names, axisLine: { lineStyle: { color: '#dcdfe6' } }, axisTick: { show: false }, axisLabel: { color: '#606266', fontSize: 11 } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f0f2f5' } }, axisLabel: { color: '#909399', fontSize: 11 } },
    series: [{
      type: 'bar', data: names.map(n => map[n]), barWidth: '44%',
      itemStyle: { color: '#3b82f6', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', color: '#606266', fontSize: 11 },
    }],
  }
})

function levelType(l) {
  return { GENERAL: 'info', IMPORTANT: 'warning', URGENT: 'danger' }[l] || 'info'
}

async function loadAll() {
  loading.value = true
  try {
    const [k, eq, al, mr] = await Promise.all([
      fetchEquipmentKpi(), fetchEquipmentViews(), fetchAlarmViews(), fetchMaintenanceViews(),
    ])
    kpi.value         = k  || {}
    equipments.value  = eq || []
    alarms.value      = al || []
    maintenance.value = mr || []
    updatedAt.value   = new Date()
  } catch (e) {
    ElMessage.error(e?.message || '加载设备数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadAll(); timer = setInterval(loadAll, 30000) })
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.dev-dash {
  padding: 14px 18px 20px;
  background: #f4f6fa;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* ── 顶部 KPI 横条 ── */
.dd-topbar {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 10px;
  padding: 10px 18px;
  box-shadow: 0 1px 4px rgba(0,27,63,.06);
  flex-wrap: wrap;
}
.dd-topbar__left { display: flex; align-items: baseline; gap: 10px; }
.dd-title { font-size: 18px; font-weight: 700; color: #001b3f; }
.dd-sub   { font-size: 12px; color: #909399; }
.dd-kpi-inline {
  display: flex;
  gap: 6px;
  flex: 1;
  flex-wrap: wrap;
  justify-content: center;
}
.kpi-chip {
  display: flex;
  align-items: baseline;
  gap: 5px;
  padding: 6px 14px;
  border-radius: 8px;
  background: #f7f9fc;
  border: 1px solid #eef1f5;
  transition: border-color .15s;
}
.kpi-chip:hover { border-color: #c8ddf0; }
.kpi-chip__val  { font-size: 22px; font-weight: 700; color: #001b3f; line-height: 1; }
.kpi-chip__sfx  { font-size: 12px; color: #909399; margin-left: 1px; }
.kpi-chip__label { font-size: 11px; color: #909399; }
.kpi--fault .kpi-chip__val  { color: #f56c6c; }
.kpi--alarm .kpi-chip__val  { color: #e6a23c; }
.dd-topbar__right { display: flex; align-items: center; gap: 10px; margin-left: auto; }
.dd-time { font-size: 11px; color: #a0a4ab; white-space: nowrap; }

/* 通知铃铛和头像在白色 topbar 上的浅色适配 */
.dd-topbar__right :deep(.nc-trigger) {
  background: #f0f4f8;
  border-color: #dce8f0;
  color: #3b5a80;
}
.dd-topbar__right :deep(.nc-trigger:hover),
.dd-topbar__right :deep(.nc-trigger.active) {
  background: #dbeafe;
  border-color: #93c5fd;
}
.dd-topbar__right :deep(.nc-trigger__icon) { filter: none; }
.dd-topbar__right :deep(.nc-badge) { border-color: #fff; }

.dd-topbar__right :deep(.av-trigger) {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border-color: #c8d6f0;
}
.dd-topbar__right :deep(.av-text) { color: #fff; }

/* ── 3D 场景 ── */
.dd-factory { flex-shrink: 0; }

/* ── 底部区域 ── */
.dd-bottom {
  display: grid;
  grid-template-columns: 1fr 1.35fr;
  gap: 14px;
  align-items: start;
}

/* 左侧三图竖排 */
.dd-charts {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 右侧列表 */
.dd-lists { display: flex; flex-direction: column; }

/* 通用面板 */
.panel {
  background: #fff;
  border-radius: 10px;
  padding: 12px 14px;
  box-shadow: 0 1px 3px rgba(0,27,63,.06);
}
.panel--sm { flex-shrink: 0; }
.panel__title {
  display: flex; align-items: center;
  font-size: 14px; font-weight: 600; color: #001b3f;
  margin-bottom: 8px;
}
.panel__chart--sm { height: 200px; }
.panel__foot { font-size: 10px; color: #a0a4ab; text-align: center; margin-top: 2px; }
.panel__empty { text-align: center; color: #a0a4ab; padding: 40px 0; font-size: 12px; }
.panel__empty--ok { color: #67c23a; }

@media (max-width: 1200px) {
  .dd-bottom { grid-template-columns: 1fr; }
  .dd-charts { flex-direction: row; flex-wrap: wrap; }
  .dd-charts .panel--sm { flex: 1 1 280px; }
}
</style>
