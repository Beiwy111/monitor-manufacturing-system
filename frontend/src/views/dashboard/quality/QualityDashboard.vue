<template>
  <div class="ruoyi-page quality-dashboard-page">
    <div class="ruoyi-stats">
      <span
        v-for="m in metrics"
        :key="m.key"
        class="ruoyi-stats__item ruoyi-stats__item--filter"
        :class="{
          'is-active': false,
          'ruoyi-stats__item--warn': m.cls === 'warn',
          'ruoyi-stats__item--danger': m.cls === 'danger'
        }"
        @click="m.to && go(m.to)"
      >
        {{ m.label }}：<em>{{ m.num }}{{ m.suffix || '' }}</em>
      </span>
      <span class="ruoyi-toolbar__right" style="margin-left: auto; display: flex; align-items: center; gap: 10px;">
        <span style="font-size: 12px; color: #909399;">更新 {{ updatedText }}</span>
        <el-button size="small" :loading="loading" @click="loadAll">刷新</el-button>
      </span>
    </div>

    <div class="qc-dashboard-grid qc-dashboard-grid--2">
      <section class="qc-dashboard-section">
        <header class="qc-dashboard-section__hd">
          缺陷帕累托分析
          <span class="qc-dashboard-section__note">按缺陷类型频次排列，虚线为 80% 控制线</span>
        </header>
        <BoardChart v-if="defectEntries.length" :option="paretoOption" class="chart chart--h" />
        <p v-else class="qc-empty">暂无不良品数据</p>
      </section>

      <section class="qc-dashboard-section">
        <header class="qc-dashboard-section__hd">质检结论分布</header>
        <div v-if="inspections.length" class="conc">
          <BoardChart :option="donutOption" class="conc__ring" />
          <ul class="conc__legend">
            <li v-for="s in statusLegend" :key="s.key">
              <span class="dot" :style="{ background: s.color }" />
              <span class="conc__name">{{ s.name }}</span>
              <span class="conc__val">{{ s.value }}</span>
              <span class="conc__pct">{{ pct(s.value, inspections.length) }}%</span>
            </li>
          </ul>
        </div>
        <p v-else class="qc-empty">暂无质检数据</p>
      </section>
    </div>

    <div class="qc-dashboard-grid qc-dashboard-grid--2b">
      <section class="qc-dashboard-section">
        <header class="qc-dashboard-section__hd">不良品处置闭环</header>
        <template v-if="nc.length">
          <div class="flow">
            <div
              v-for="seg in dispositionSegs"
              :key="seg.key"
              class="flow__seg"
              :style="{ flexGrow: seg.value || 0.001, background: seg.color }"
              :title="`${seg.name} ${seg.value}`"
            >
              <span v-if="seg.value">{{ seg.value }}</span>
            </div>
          </div>
          <div class="flow__legend">
            <span v-for="seg in dispositionSegs" :key="seg.key">
              <i :style="{ background: seg.color }" />{{ seg.name }} {{ seg.value }}
            </span>
          </div>
          <div class="method">
            <div class="method__title">处置方式构成</div>
            <div v-for="m in methodRows" :key="m.key" class="method__row">
              <span class="method__name">{{ m.name }}</span>
              <div class="method__track">
                <div class="method__fill" :style="{ width: pct(m.value, nc.length) + '%', background: m.color }" />
              </div>
              <span class="method__val">{{ m.value }}</span>
            </div>
          </div>
        </template>
        <p v-else class="qc-empty">暂无不良品数据</p>
      </section>

      <section class="qc-dashboard-section">
        <header class="qc-dashboard-section__hd">
          待检 / 复检任务
          <span class="qc-link" @click="go('/quality/fp/inspection')">去检验 ›</span>
        </header>
        <div class="ruoyi-table-wrap" style="padding: 0;">
          <el-table
            v-if="pendingTasks.length"
            :data="pendingTasks"
            size="small"
            max-height="252"
            class="clickable"
            @row-click="(row) => go(taskPath(row))"
          >
            <el-table-column prop="inspectionNo" label="质检单号" width="140" />
            <el-table-column prop="batchNo" label="批次" min-width="150" show-overflow-tooltip />
            <el-table-column label="品类" width="76" align="center">
              <template #default="{ row }">{{ categoryCn(row.inspectionCategory) }}</template>
            </el-table-column>
            <el-table-column label="类型" width="76" align="center">
              <template #default="{ row }">{{ typeCn(row.inspectionType) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="82" align="center">
              <template #default="{ row }">
                <el-tag :type="row.inspectionStatus === 'RECHECK_REQUIRED' ? 'warning' : 'info'" size="small">
                  {{ statusCn(row.inspectionStatus) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <p v-else class="qc-empty qc-empty--ok">✓ 无待检任务，检验进度已清零</p>
        </div>
      </section>
    </div>

    <section class="qc-dashboard-section qc-dashboard-section--full">
      <header class="qc-dashboard-section__hd">
        最近质检完成记录
        <span class="qc-link" @click="go('/quality/fp/records')">查看全部 ›</span>
      </header>
      <div class="ruoyi-table-wrap" style="padding: 0;">
        <el-table
          v-if="recentCompleted.length"
          :data="recentCompleted"
          size="small"
          max-height="280"
          class="clickable"
          @row-click="(row) => go('/quality/fp/records')"
        >
          <el-table-column prop="inspectionNo" label="质检单号" width="140" />
          <el-table-column prop="materialName" label="产品/物料" min-width="150" show-overflow-tooltip />
          <el-table-column prop="batchNo" label="批次" min-width="130" show-overflow-tooltip />
          <el-table-column label="品类" width="76" align="center">
            <template #default="{ row }">{{ categoryCn(row.inspectionCategory) }}</template>
          </el-table-column>
          <el-table-column prop="sampleQuantity" label="抽检台数" width="88" align="right" />
          <el-table-column prop="qualifiedQuantity" label="合格台数" width="88" align="right" />
          <el-table-column prop="unqualifiedQuantity" label="不合格台数" width="96" align="right" />
          <el-table-column label="结论" width="88" align="center">
            <template #default="{ row }">
              <el-tag :type="row.inspectionStatus === 'PASSED' ? 'success' : 'danger'" size="small">
                {{ statusCn(row.inspectionStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="inspectedAt" label="完成时间" width="160" />
        </el-table>
        <p v-else class="qc-empty">暂无已完成质检记录，完成检验后将在此展示</p>
      </div>
    </section>

    <section class="qc-dashboard-section qc-dashboard-section--full">
      <header class="qc-dashboard-section__hd">
        不良品台账
        <span class="qc-link" @click="go('/quality/fp/defect')">去处置 ›</span>
      </header>
      <div class="ruoyi-table-wrap" style="padding: 0;">
        <el-table
          v-if="nc.length"
          :data="ncSorted"
          size="small"
          max-height="300"
          class="clickable"
          @row-click="() => go('/quality/fp/defect')"
        >
          <el-table-column prop="nonconformingNo" label="不良单号" width="140" />
          <el-table-column prop="defectType" label="缺陷类型" min-width="120" />
          <el-table-column prop="defectDescription" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column label="严重度" width="82" align="center">
            <template #default="{ row }">
              <el-tag :type="sevType(row.severity)" size="small">{{ sevCn(row.severity) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="66" align="right" />
          <el-table-column label="处置方式" width="96" align="center">
            <template #default="{ row }">{{ methodCn(row.handleMethod) }}</template>
          </el-table-column>
          <el-table-column label="处置状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="handleType(row.handleStatus)" size="small">{{ handleCn(row.handleStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="registeredAt" label="登记时间" width="160" />
        </el-table>
        <p v-else class="qc-empty">暂无不良品记录</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BoardChart from '@/components/board/BoardChart.vue'
import { fetchInspectionViews, fetchNonconformingViews, fetchQualityKpi } from '@/api/quality'

const router = useRouter()
const go = (path) => router.push(path)

const loading = ref(false)
const inspections = ref([])
const nc = ref([])
const kpi = ref({})
const updatedAt = ref(null)
let timer = null

const STATUS_CN = { PENDING: '待检', PASSED: '通过', FAILED: '不通过', RECHECK_REQUIRED: '需复检', CLOSED: '已关闭' }
const CATEGORY_CN = { RAW_MATERIAL: '物料', SEMI_FINISHED: '半成品', FINISHED_PRODUCT: '成品' }
const TYPE_CN = { INCOMING: '来料检', PROCESS: '过程检', FINAL: '终检', RECHECK: '复检',
  PANEL_INSPECTION: '面板检', BACKLIGHT_INSPECTION: '背光检', PCB_INSPECTION: 'PCB检', ASSEMBLY_INSPECTION: '组装检' }
const SEV_CN = { MINOR: '轻微', GENERAL: '一般', MAJOR: '严重', CRITICAL: '致命' }
const METHOD_CN = { REWORK: '返工', SCRAP: '报废', CONCESSION_ACCEPT: '让步接收', RETURNED: '退回', PENDING: '待处置' }
const HANDLE_CN = { PENDING: '待处理', PROCESSING: '处理中', DONE: '已处置' }
const statusCn = (s) => STATUS_CN[s] || s || '—'
const categoryCn = (s) => CATEGORY_CN[s] || s || '—'
const typeCn = (s) => TYPE_CN[s] || s || '—'
const sevCn = (s) => SEV_CN[s] || s || '—'
const methodCn = (s) => METHOD_CN[s] || s || '—'
const normHandleStatus = (s) => (s === 'COMPLETED' ? 'DONE' : s)
const normHandleMethod = (s) => (s === 'CONCESSION' ? 'CONCESSION_ACCEPT' : s)
const handleCn = (s) => HANDLE_CN[normHandleMethod(s)] || s || '—'
const sevType = (s) => ({ MINOR: 'info', GENERAL: '', MAJOR: 'warning', CRITICAL: 'danger' }[s] || 'info')
const handleType = (s) => ({ PENDING: 'warning', PROCESSING: 'primary', DONE: 'success' }[normHandleStatus(s)] || 'info')
const pct = (v, t) => (t ? Math.round((v / t) * 100) : 0)

const updatedText = computed(() => {
  if (!updatedAt.value) return '—'
  const d = updatedAt.value
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}`
})

const counts = computed(() => {
  const c = { pending: 0, passed: 0, failed: 0, recheck: 0, closed: 0 }
  for (const i of inspections.value) {
    if (i.inspectionStatus === 'PENDING') c.pending++
    else if (i.inspectionStatus === 'PASSED') c.passed++
    else if (i.inspectionStatus === 'FAILED') c.failed++
    else if (i.inspectionStatus === 'RECHECK_REQUIRED') c.recheck++
    else if (i.inspectionStatus === 'CLOSED') c.closed++
  }
  return c
})

const passRate = computed(() => {
  const { sample, qual } = unitStats.value
  if (sample > 0) return Math.round((qual / sample) * 100)
  const judged = inspections.value.filter((i) => i.inspectionResult)
  if (!judged.length) return 0
  const ok = judged.filter((i) => i.inspectionResult === 'QUALIFIED').length
  return Math.round((ok / judged.length) * 100)
})

const unitStats = computed(() => {
  const judged = inspections.value.filter((i) => ['PASSED', 'FAILED', 'CLOSED'].includes(i.inspectionStatus))
  let sample = 0
  let qual = 0
  let unqual = 0
  for (const i of judged) {
    sample += Number(i.sampleQuantity) || 0
    qual += Number(i.qualifiedQuantity) || 0
    unqual += Number(i.unqualifiedQuantity) || 0
  }
  return { sample, qual, unqual }
})

const recentCompleted = computed(() =>
  [...inspections.value]
    .filter((i) => ['PASSED', 'FAILED', 'CLOSED'].includes(i.inspectionStatus))
    .sort((a, b) => String(b.inspectedAt || b.updatedAt || '').localeCompare(String(a.inspectedAt || a.updatedAt || '')))
    .slice(0, 12)
)

const ncOpen = computed(() => nc.value.filter((n) => normHandleStatus(n.handleStatus) !== 'DONE').length)
const ncClosed = computed(() => nc.value.filter((n) => normHandleStatus(n.handleStatus) === 'DONE').length)

const metrics = computed(() => [
  { key: 'pending', num: counts.value.pending, label: '待检任务', to: '/quality/fp/inspection' },
  { key: 'sample', num: unitStats.value.sample, label: '累计抽检台数', to: '/quality/fp/records' },
  { key: 'qual', num: unitStats.value.qual, label: '累计合格台数', to: '/quality/fp/records' },
  { key: 'rate', num: passRate.value, suffix: '%', label: '台数合格率', to: '/quality/fp/records' },
  { key: 'recheck', num: counts.value.recheck, label: '需复检', cls: counts.value.recheck ? 'warn' : '', to: '/quality/fp/reinspect' },
  { key: 'ncOpen', num: ncOpen.value, label: '不良品待处置', cls: ncOpen.value ? 'danger' : '', to: '/quality/fp/defect' },
  { key: 'ncClosed', num: `${ncClosed.value} / ${nc.value.length}`, label: '不良品已闭环', to: '/quality/fp/defect' }
])

function taskPath(row) {
  if (row.inspectionStatus === 'RECHECK_REQUIRED') {
    return row.inspectionCategory === 'RAW_MATERIAL' ? '/quality/material/reinspect' : '/quality/fp/reinspect'
  }
  if (row.inspectionCategory === 'RAW_MATERIAL') return '/quality/material/inspection'
  if (row.inspectionCategory === 'FINISHED_PRODUCT') return '/quality/fp/inspection'
  return '/quality/inspection'
}

const pendingTasks = computed(() =>
  inspections.value.filter((i) => ['PENDING', 'RECHECK_REQUIRED'].includes(i.inspectionStatus))
)
const ncSorted = computed(() =>
  [...nc.value].sort((a, b) => String(b.registeredAt || '').localeCompare(String(a.registeredAt || '')))
)

const defectEntries = computed(() => {
  const map = {}
  for (const n of nc.value) map[n.defectType || '未分类'] = (map[n.defectType || '未分类'] || 0) + 1
  return Object.entries(map).sort((a, b) => b[1] - a[1])
})

const paretoOption = computed(() => {
  const names = defectEntries.value.map((e) => e[0])
  const vals = defectEntries.value.map((e) => e[1])
  const total = vals.reduce((a, b) => a + b, 0) || 1
  let cum = 0
  const cumVals = vals.map((v) => (cum += v))
  const cumPct = cumVals.map((v) => Math.round((v / total) * 100))
  return {
    grid: { left: 6, right: 14, top: 30, bottom: 6, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' },
      formatter: (ps) => { const i = ps[0].dataIndex; return `${names[i]}<br/>缺陷数：${vals[i]} 件<br/>累计占比：${cumPct[i]}%` } },
    legend: { data: ['缺陷数', '累计'], top: 2, right: 2, icon: 'roundRect', itemWidth: 12, itemHeight: 8, textStyle: { color: '#606266', fontSize: 12 } },
    xAxis: { type: 'category', data: names, axisTick: { show: false }, axisLine: { lineStyle: { color: '#dcdfe6' } }, axisLabel: { color: '#606266', fontSize: 12, interval: 0 } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f0f2f5' } }, axisLabel: { color: '#909399' } },
    series: [
      { name: '缺陷数', type: 'bar', data: vals, barWidth: '40%', itemStyle: { color: '#5b8def', borderRadius: [3, 3, 0, 0] },
        label: { show: true, position: 'top', color: '#606266', fontSize: 12 },
        markLine: { silent: true, symbol: 'none', lineStyle: { color: '#e6a23c', type: 'dashed', width: 1 },
          data: [{ yAxis: +(total * 0.8).toFixed(2), label: { formatter: '80%', color: '#e6a23c', fontSize: 11, position: 'insideEndTop' } }] } },
      { name: '累计', type: 'line', data: cumVals, symbol: 'circle', symbolSize: 6, smooth: false,
        lineStyle: { color: '#e6a23c', width: 2 }, itemStyle: { color: '#e6a23c' },
        label: { show: true, formatter: (p) => cumPct[p.dataIndex] + '%', color: '#e6a23c', fontSize: 11, position: 'top' } }
    ]
  }
})

const STATUS_ORDER = [
  { key: 'PASSED', name: '通过', color: '#67c23a' },
  { key: 'PENDING', name: '待检', color: '#909399' },
  { key: 'RECHECK_REQUIRED', name: '需复检', color: '#e6a23c' },
  { key: 'CLOSED', name: '已关闭', color: '#7c8ba1' },
  { key: 'FAILED', name: '不通过', color: '#f56c6c' }
]
const statusLegend = computed(() => {
  const c = counts.value
  const m = { PASSED: c.passed, PENDING: c.pending, RECHECK_REQUIRED: c.recheck, CLOSED: c.closed, FAILED: c.failed }
  return STATUS_ORDER.map((s) => ({ ...s, value: m[s.key] || 0 }))
})

const donutOption = computed(() => {
  const data = statusLegend.value.filter((s) => s.value > 0).map((s) => ({ name: s.name, value: s.value, itemStyle: { color: s.color } }))
  return {
    tooltip: { trigger: 'item', formatter: '{b}：{c} 单（{d}%）' },
    title: { text: passRate.value + '%', subtext: '合格率', left: 'center', top: '38%',
      textStyle: { fontSize: 26, fontWeight: 700, color: '#001b3f' }, subtextStyle: { fontSize: 12, color: '#909399' } },
    series: [{ type: 'pie', radius: ['58%', '80%'], center: ['50%', '50%'], avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2 }, label: { show: false }, labelLine: { show: false }, data }]
  }
})

const dispositionSegs = computed(() => {
  const g = { PENDING: 0, PROCESSING: 0, DONE: 0 }
  for (const n of nc.value) {
    const st = normHandleStatus(n.handleStatus)
    g[st] = (g[st] || 0) + 1
  }
  return [
    { key: 'PENDING', name: '待处置', value: g.PENDING || 0, color: '#e6a23c' },
    { key: 'PROCESSING', name: '处理中', value: g.PROCESSING || 0, color: '#409eff' },
    { key: 'DONE', name: '已处置', value: g.DONE || 0, color: '#67c23a' }
  ]
})

const methodRows = computed(() => {
  const g = {}
  for (const n of nc.value) {
    const m = normHandleMethod(n.handleMethod)
    g[m] = (g[m] || 0) + 1
  }
  const order = [
    { key: 'REWORK', color: '#409eff' }, { key: 'CONCESSION_ACCEPT', color: '#e6a23c' },
    { key: 'SCRAP', color: '#f56c6c' }, { key: 'RETURNED', color: '#7c8ba1' }, { key: 'PENDING', color: '#c0c4cc' }
  ]
  return order.map((o) => ({ ...o, name: methodCn(o.key), value: g[o.key] || 0 })).filter((r) => r.value > 0)
})

async function loadAll() {
  loading.value = true
  try {
    const [ins, ncList, kpiRes] = await Promise.all([
      fetchInspectionViews().catch(() => null),
      fetchNonconformingViews().catch(() => null),
      fetchQualityKpi().catch(() => null)
    ])
    inspections.value = ins || []
    nc.value = ncList || []
    if (kpiRes) kpi.value = kpiRes
    updatedAt.value = new Date()
  } catch (e) {
    ElMessage.error(e?.message || '加载质检数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadAll(); timer = setInterval(loadAll, 30000) })
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.quality-dashboard-page {
  min-height: calc(100vh - 120px);
}

.clickable :deep(.el-table__row) {
  cursor: pointer;
}

.chart--h {
  height: 264px;
}

.conc {
  display: flex;
  align-items: center;
  gap: 8px;
}

.conc__ring {
  width: 190px;
  height: 200px;
  flex: 0 0 190px;
}

.conc__legend {
  flex: 1;
  list-style: none;
  margin: 0;
  padding: 0;
}

.conc__legend li {
  display: flex;
  align-items: center;
  padding: 7px 4px;
  border-bottom: 1px dashed #eef1f5;
  font-size: 13px;
}

.conc__legend li:last-child {
  border-bottom: none;
}

.dot {
  width: 9px;
  height: 9px;
  border-radius: 2px;
  margin-right: 8px;
  flex: 0 0 9px;
}

.conc__name {
  color: #4a5568;
}

.conc__val {
  margin-left: auto;
  font-weight: 700;
  color: #1a2434;
}

.conc__pct {
  width: 44px;
  text-align: right;
  color: #a0a6b0;
  font-size: 12px;
}

.flow {
  display: flex;
  height: 30px;
  border-radius: 4px;
  overflow: hidden;
  background: #f0f2f5;
}

.flow__seg {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  min-width: 0;
}

.flow__legend {
  display: flex;
  gap: 16px;
  margin: 8px 0 4px;
  font-size: 12px;
  color: #6b7280;
}

.flow__legend span {
  display: flex;
  align-items: center;
}

.flow__legend i {
  width: 9px;
  height: 9px;
  border-radius: 2px;
  margin-right: 5px;
}

.method {
  margin-top: 12px;
  border-top: 1px solid #eef1f5;
  padding-top: 10px;
}

.method__title {
  font-size: 12px;
  color: #8a94a6;
  margin-bottom: 8px;
}

.method__row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
}

.method__name {
  width: 62px;
  color: #4a5568;
}

.method__track {
  flex: 1;
  height: 8px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
  margin: 0 8px;
}

.method__fill {
  height: 100%;
  border-radius: 4px;
}

.method__val {
  width: 20px;
  text-align: right;
  font-weight: 600;
  color: #1a2434;
}
</style>
