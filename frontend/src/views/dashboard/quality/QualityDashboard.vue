<template>
  <div class="qc-wb">
    <!-- 顶部指标带 -->
    <header class="qc-wb__metrics">
      <div
        v-for="m in metricBands"
        :key="m.key"
        class="qc-metric"
        :class="{ 'qc-metric--clickable': m.to }"
        @click="m.to && go(m.to)"
      >
        <div class="qc-metric__label">{{ m.label }}</div>
        <div class="qc-metric__main">
          <span class="qc-metric__num" :class="m.numClass">{{ m.display }}</span>
          <span class="qc-metric__delta" :class="`qc-metric__delta--${m.delta.cls}`">{{ m.delta.text }}</span>
    </div>
        <div class="qc-metric__sub">{{ m.compareLabel }}</div>
        <div v-if="m.sparkPoints?.length" class="qc-metric__trend-wrap">
          <span class="qc-metric__trend-label">近7日趋势</span>
          <BoardChart :option="m.sparkOption" class="qc-metric__spark" />
        </div>
      </div>
      <div class="qc-wb__refresh">
        <span class="qc-wb__time">更新 {{ updatedText }}</span>
        <el-button size="small" text :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </header>

    <!-- 中部图表 -->
    <div class="qc-wb__charts">
      <section class="qc-panel">
        <header class="qc-panel__hd">
          <span>{{ showPareto ? '缺陷帕累托分析' : '缺陷类型排行' }}</span>
          <div class="qc-panel__tools">
            <el-radio-group v-model="paretoRange" size="small">
              <el-radio-button label="1">今日</el-radio-button>
              <el-radio-button label="7">近7天</el-radio-button>
              <el-radio-button label="30">近30天</el-radio-button>
            </el-radio-group>
            <el-select v-model="paretoProcess" size="small" clearable placeholder="工序筛选" class="qc-panel__select">
              <el-option v-for="p in processOptions" :key="p" :label="p" :value="p" />
            </el-select>
            <button v-if="selectedDefect" type="button" class="qc-filter-chip" @click="clearDefectFilter">
              已选 {{ selectedDefect }} ×
            </button>
          </div>
        </header>
        <BoardChart
          v-if="paretoEntries.length && showPareto"
          :option="paretoOption"
          class="qc-chart qc-chart--pareto"
          @chart-click="onParetoClick"
        />
        <div v-else-if="paretoEntries.length" class="qc-rank">
          <p class="qc-rank__hint">当前样本较少，暂未形成明显主要缺陷</p>
          <ul class="qc-rank__list">
            <li
              v-for="(item, idx) in paretoEntries"
              :key="item.name"
              class="qc-rank__item"
              :class="{ 'qc-rank__item--active': selectedDefect === item.name }"
              @click="toggleDefectFilter(item.name)"
            >
              <span class="qc-rank__idx">{{ idx + 1 }}</span>
              <span class="qc-rank__name">{{ item.name }}</span>
              <span class="qc-rank__bar-wrap">
                <span class="qc-rank__bar" :style="{ width: pct(item.count, paretoMaxCount) + '%' }" />
              </span>
              <span class="qc-rank__val">{{ item.count }}</span>
            </li>
          </ul>
        </div>
        <p v-else class="qc-empty">暂无不良品数据</p>
      </section>

      <section class="qc-panel">
        <header class="qc-panel__hd">
          <span>质检结论分布</span>
        </header>
        <div v-if="inspections.length" class="qc-pass-layout">
          <BoardChart :option="donutOption" class="qc-chart qc-chart--donut" />
          <div class="qc-pass-side">
            <ul class="qc-pass-legend">
            <li v-for="s in statusLegend" :key="s.key">
                <i :style="{ background: s.color }" />
                <span>{{ s.name }}</span>
                <em>{{ s.value }}</em>
                <b>{{ pct(s.value, donutTotal) }}%</b>
            </li>
          </ul>
            <div class="qc-pass-trend">
              <div class="qc-pass-trend__label">近7日抽检与合格率</div>
              <BoardChart :option="sampleRateOption" class="qc-chart qc-chart--combo" />
            </div>
          </div>
        </div>
        <p v-else class="qc-empty">暂无质检数据</p>
      </section>
    </div>

    <!-- 下方：闭环 + 任务 -->
    <div class="qc-wb__bottom">
      <section class="qc-panel">
        <header class="qc-panel__hd">
          <span>不良品处置闭环</span>
          <span class="qc-panel__meta">
            检验已关闭 {{ counts.closed }} 单 · 平均 {{ closureStats.avgHours }}h · 超时 {{ closureStats.overdue }} 单
          </span>
        </header>
        <template v-if="nc.length">
          <div class="qc-flow">
            <div v-for="(step, idx) in closureSteps" :key="step.key" class="qc-flow__item">
              <div class="qc-flow__node" :class="{ 'qc-flow__node--active': step.value > 0 }">
                <span class="qc-flow__count">{{ step.value }}</span>
                <span class="qc-flow__name">{{ step.name }}</span>
            </div>
              <div v-if="idx < closureSteps.length - 1" class="qc-flow__arrow" />
          </div>
          </div>
          <div class="qc-method">
            <div v-for="m in methodRows" :key="m.key" class="qc-method__row">
              <span class="qc-method__name">{{ m.name }}</span>
              <div class="qc-method__track">
                <div
                  class="qc-method__fill"
                  :style="{ width: pct(m.value, ncDoneOrAll) + '%', background: m.color }"
                />
              </div>
              <span class="qc-method__val">{{ m.value }}</span>
            </div>
          </div>
        </template>
        <p v-else class="qc-empty">暂无不良品数据</p>
      </section>

      <section class="qc-panel qc-panel--tasks">
        <header class="qc-panel__hd">
          <span>待检 / 复检任务</span>
          <div class="qc-panel__tools">
            <button
              v-for="tab in taskTabs"
              :key="tab.key"
              type="button"
              class="qc-tab"
              :class="{ 'qc-tab--active': taskTab === tab.key }"
              @click="taskTab = tab.key"
            >
              {{ tab.label }}
              <em v-if="tab.count">{{ tab.count }}</em>
            </button>
          <span class="qc-link" @click="go('/quality/fp/inspection')">去检验 ›</span>
          </div>
        </header>

        <div class="qc-table-wrap">
          <el-table
            v-if="taskTab === 'pending' && displayPendingTasks.length"
            :data="displayPendingTasks"
            size="small"
            max-height="240"
            :row-class-name="taskRowClass"
          >
            <el-table-column label="优先级" width="72" align="center">
              <template #default="{ row }">
                <span class="qc-priority" :class="priorityClass(row)">{{ row._priority.level }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="inspectionNo" label="质检单号" width="132" />
            <el-table-column prop="batchNo" label="批次" min-width="120" show-overflow-tooltip />
            <el-table-column label="工序" width="88" align="center">
              <template #default="{ row }">{{ processLabel(row) }}</template>
            </el-table-column>
            <el-table-column label="等待" width="88" align="center">
              <template #default="{ row }">
                <span :class="timeClass(row)">{{ row._waitText }}</span>
              </template>
            </el-table-column>
            <el-table-column label="截止" width="108" align="center">
              <template #default="{ row }">
                <span :class="timeClass(row)">{{ row._deadline }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="88" align="center">
              <template #default="{ row }">
                <span class="qc-status" :class="`qc-status--${row._statusKey}`">{{ statusCn(row.inspectionStatus) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click.stop="go(taskPath(row))">去检验</el-button>
              </template>
            </el-table-column>
          </el-table>

        <el-table
            v-else-if="taskTab === 'records' && displayRecent.length"
            :data="displayRecent"
          size="small"
            max-height="240"
          class="clickable"
            @row-click="() => go('/quality/fp/records')"
          >
            <el-table-column prop="inspectionNo" label="质检单号" width="132" />
            <el-table-column prop="materialName" label="产品/物料" min-width="120" show-overflow-tooltip />
            <el-table-column prop="batchNo" label="批次" min-width="110" show-overflow-tooltip />
            <el-table-column label="工序" width="80" align="center">
              <template #default="{ row }">{{ processLabel(row) }}</template>
          </el-table-column>
            <el-table-column prop="sampleQuantity" label="抽检" width="64" align="right" />
            <el-table-column label="结论" width="80" align="center">
            <template #default="{ row }">
                <span class="qc-status" :class="row.inspectionStatus === 'PASSED' ? 'qc-status--pass' : 'qc-status--fail'">
                {{ statusCn(row.inspectionStatus) }}
                </span>
            </template>
          </el-table-column>
            <el-table-column prop="inspectedAt" label="完成时间" width="140" />
        </el-table>

        <el-table
            v-else-if="taskTab === 'nc' && displayNc.length"
            :data="displayNc"
          size="small"
            max-height="240"
          class="clickable"
          @row-click="() => go('/quality/fp/defect')"
        >
            <el-table-column prop="nonconformingNo" label="不良单号" width="132" />
            <el-table-column prop="defectType" label="缺陷类型" min-width="100" />
            <el-table-column prop="defectDescription" label="描述" min-width="140" show-overflow-tooltip />
            <el-table-column label="严重度" width="72" align="center">
            <template #default="{ row }">
                <span class="qc-sev" :class="`qc-sev--${row.severity}`">{{ sevCn(row.severity) }}</span>
            </template>
          </el-table-column>
            <el-table-column label="处置" width="80" align="center">
            <template #default="{ row }">{{ methodCn(row.handleMethod) }}</template>
          </el-table-column>
            <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
                <span class="qc-status" :class="`qc-status--${handleStatusKey(row.handleStatus)}`">
                  {{ handleCn(row.handleStatus) }}
                </span>
            </template>
          </el-table-column>
        </el-table>

          <p v-else class="qc-empty" :class="{ 'qc-empty--ok': taskTab === 'pending' && !selectedDefect }">
            {{ emptyTaskText || (taskTab === 'pending' ? '✓ 无待检任务，检验进度已清零' : '暂无数据') }}
          </p>
      </div>
    </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BoardChart from '@/components/board/BoardChart.vue'
import { fetchInspectionViews, fetchNonconformingViews, fetchQualityKpi } from '@/api/quality'
import {
  QC_COLORS,
  STATUS_CN,
  parseQcDate,
  startOfDay,
  addDays,
  dayKey,
  isToday,
  isYesterday,
  inDateRange,
  formatWaitDuration,
  normHandleStatus,
  normHandleMethod,
  processLabel,
  buildDailySeriesDetailed,
  deltaChange,
  weekNewDelta,
  pctChange,
  passRateOfRows,
  derivePriority,
  deriveDeadline,
  isTaskOverdue,
  isTaskNearOverdue,
  buildParetoData,
  canShowPareto,
  sparklineOption,
  buildParetoOption,
  buildDonutOption,
  buildSampleRateComboOption,
  buildDailyPassPoints
} from '@/composables/useQualityDashboard'

const router = useRouter()
const go = (path) => router.push(path)

const loading = ref(false)
const inspections = ref([])
const nc = ref([])
const kpi = ref({})
const updatedAt = ref(null)
const paretoRange = ref('30')
const paretoProcess = ref('')
const selectedDefect = ref('')
const taskTab = ref('pending')
let timer = null

const SEV_CN = { MINOR: '轻微', GENERAL: '一般', MAJOR: '严重', CRITICAL: '致命' }
const METHOD_CN = { REWORK: '返工', SCRAP: '报废', CONCESSION_ACCEPT: '让步接收', RETURNED: '退回', PENDING: '待处置' }
const HANDLE_CN = { PENDING: '待处置', PROCESSING: '处理中', DONE: '已闭环' }

const statusCn = (s) => STATUS_CN[s] || s || '—'
const sevCn = (s) => SEV_CN[s] || s || '—'
const methodCn = (s) => METHOD_CN[normHandleMethod(s)] || s || '—'
const handleCn = (s) => HANDLE_CN[normHandleStatus(s)] || s || '—'
const handleStatusKey = (s) => {
  const n = normHandleStatus(s)
  if (n === 'DONE') return 'pass'
  if (n === 'PROCESSING') return 'recheck'
  return 'pending'
}
const pct = (v, t) => (t ? Math.round((v / t) * 100) : 0)

const updatedText = computed(() => {
  if (!updatedAt.value) return '—'
  const d = updatedAt.value
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
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

const judgedRows = computed(() =>
  inspections.value.filter((i) => ['PASSED', 'FAILED', 'CLOSED'].includes(i.inspectionStatus))
)

const unitStats = computed(() => {
  let sample = 0
  let qual = 0
  let unqual = 0
  for (const i of judgedRows.value) {
    sample += Number(i.sampleQuantity) || 0
    qual += Number(i.qualifiedQuantity) || 0
    unqual += Number(i.unqualifiedQuantity) || 0
  }
  return { sample, qual, unqual }
})

const passRate = computed(() => passRateOfRows(judgedRows.value) ?? 0)

const ncOpen = computed(() => nc.value.filter((n) => normHandleStatus(n.handleStatus) !== 'DONE').length)
const ncClosed = computed(() => nc.value.filter((n) => normHandleStatus(n.handleStatus) === 'DONE').length)

const todayPending = computed(() =>
  inspections.value.filter((i) => i.inspectionStatus === 'PENDING' && isToday(i.createdAt)).length
)
const ydayPending = computed(() =>
  inspections.value.filter((i) => i.inspectionStatus === 'PENDING' && isYesterday(i.createdAt)).length
)

const weekPassRate = computed(() => {
  const rows = judgedRows.value.filter((i) => inDateRange(i.inspectedAt || i.updatedAt, 7))
  return passRateOfRows(rows) ?? 0
})

const lastWeekPassRate = computed(() => {
  const end = addDays(startOfDay(), -7)
  const start = addDays(end, -6)
  const rows = judgedRows.value.filter((i) => {
    const d = parseQcDate(i.inspectedAt || i.updatedAt)
    return d && d >= start && d < startOfDay()
  })
  return passRateOfRows(rows) ?? 0
})

const weekPassDelta = computed(() => weekPassRate.value - (lastWeekPassRate.value ?? 0))

const weekNewSample = computed(() =>
  judgedRows.value
    .filter((i) => inDateRange(i.inspectedAt || i.updatedAt, 7))
    .reduce((s, i) => s + (Number(i.sampleQuantity) || 0), 0)
)

const weekNewQual = computed(() =>
  judgedRows.value
    .filter((i) => inDateRange(i.inspectedAt || i.updatedAt, 7))
    .reduce((s, i) => s + (Number(i.qualifiedQuantity) || 0), 0)
)

const weekClosedNc = computed(() =>
  nc.value.filter((n) => normHandleStatus(n.handleStatus) === 'DONE' && inDateRange(n.handledAt || n.registeredAt, 7)).length
)

const metricBands = computed(() => {
  const pendingSpark = buildDailySeriesDetailed(
    inspections.value.filter((i) => ['PENDING', 'RECHECK_REQUIRED'].includes(i.inspectionStatus)),
    (i) => i.createdAt,
    () => 1
  )
  const sampleSpark = buildDailySeriesDetailed(
    judgedRows.value,
    (i) => i.inspectedAt || i.updatedAt,
    (i) => Number(i.sampleQuantity) || 0
  )
  const qualSpark = buildDailySeriesDetailed(
    judgedRows.value,
    (i) => i.inspectedAt || i.updatedAt,
    (i) => Number(i.qualifiedQuantity) || 0
  )
  const rateSpark = buildDailySeriesDetailed(judgedRows.value, (i) => i.inspectedAt || i.updatedAt, (i) => {
    const s = Number(i.sampleQuantity) || 0
    const q = Number(i.qualifiedQuantity) || 0
    if (s > 0) return Math.round((q / s) * 100)
    if (!i.inspectionResult) return 0
    return i.inspectionResult === 'QUALIFIED' ? 100 : 0
  })

  const todayNewNc = nc.value.filter((n) => isToday(n.registeredAt)).length
  const ydayNewNc = nc.value.filter((n) => isYesterday(n.registeredAt)).length

  return [
    {
      key: 'pending',
      label: '待检任务',
      display: String(counts.value.pending),
      compareLabel: '对比口径：较昨日变化',
      delta: deltaChange(todayPending.value, ydayPending.value),
      sparkPoints: pendingSpark,
      sparkOption: sparklineOption(pendingSpark, QC_COLORS.primary),
      to: '/quality/fp/inspection'
    },
    {
      key: 'sample',
      label: '累计抽检台数',
      display: String(unitStats.value.sample),
      compareLabel: '对比口径：本周新增',
      delta: weekNewDelta(weekNewSample.value, '台'),
      sparkPoints: sampleSpark,
      sparkOption: sparklineOption(sampleSpark, QC_COLORS.bar, '台'),
      to: '/quality/fp/records'
    },
    {
      key: 'qual',
      label: '累计合格台数',
      display: String(unitStats.value.qual),
      compareLabel: '对比口径：本周新增',
      delta: weekNewDelta(weekNewQual.value, '台'),
      sparkPoints: qualSpark,
      sparkOption: sparklineOption(qualSpark, QC_COLORS.pass, '台'),
      to: '/quality/fp/records'
    },
    {
      key: 'rate',
      label: '台数合格率',
      display: `${passRate.value}%`,
      numClass: 'qc-metric__num--rate',
      compareLabel: '对比口径：较上周',
      delta: {
        text: pctChange(weekPassRate.value, lastWeekPassRate.value),
        cls: weekPassDelta.value >= 0 ? 'up' : 'neutral'
      },
      sparkPoints: rateSpark,
      sparkOption: sparklineOption(rateSpark, QC_COLORS.pass, '%'),
      to: '/quality/fp/records'
    },
    {
      key: 'recheck',
      label: '需复检',
      display: String(counts.value.recheck),
      compareLabel: '对比口径：较昨日变化',
      delta: deltaChange(
        inspections.value.filter((i) => i.inspectionStatus === 'RECHECK_REQUIRED' && isToday(i.updatedAt)).length,
        inspections.value.filter((i) => i.inspectionStatus === 'RECHECK_REQUIRED' && isYesterday(i.updatedAt)).length
      ),
      sparkPoints: buildDailySeriesDetailed(
        inspections.value.filter((i) => i.inspectionStatus === 'RECHECK_REQUIRED'),
        (i) => i.updatedAt || i.createdAt,
        () => 1
      ),
      sparkOption: sparklineOption(
        buildDailySeriesDetailed(
          inspections.value.filter((i) => i.inspectionStatus === 'RECHECK_REQUIRED'),
          (i) => i.updatedAt || i.createdAt,
          () => 1
        ),
        QC_COLORS.recheck
      ),
      to: '/quality/fp/reinspect'
    },
    {
      key: 'ncOpen',
      label: '不良待处置',
      display: String(ncOpen.value),
      compareLabel: '对比口径：较昨日新增',
      delta: deltaChange(todayNewNc, ydayNewNc),
      sparkPoints: buildDailySeriesDetailed(nc.value.filter((n) => normHandleStatus(n.handleStatus) !== 'DONE'), (n) => n.registeredAt, () => 1),
      sparkOption: sparklineOption(
        buildDailySeriesDetailed(nc.value.filter((n) => normHandleStatus(n.handleStatus) !== 'DONE'), (n) => n.registeredAt, () => 1),
        QC_COLORS.recheck
      ),
      to: '/quality/fp/defect'
    },
    {
      key: 'ncClosed',
      label: '不良已闭环',
      display: `${ncClosed.value}/${nc.value.length}`,
      compareLabel: `对比口径：本周闭环 · 闭环率 ${pct(ncClosed.value, nc.value.length)}%`,
      delta: weekNewDelta(weekClosedNc.value, '单'),
      sparkPoints: buildDailySeriesDetailed(
        nc.value.filter((n) => normHandleStatus(n.handleStatus) === 'DONE'),
        (n) => n.handledAt || n.registeredAt,
        () => 1
      ),
      sparkOption: sparklineOption(
        buildDailySeriesDetailed(
          nc.value.filter((n) => normHandleStatus(n.handleStatus) === 'DONE'),
          (n) => n.handledAt || n.registeredAt,
          () => 1
        ),
        QC_COLORS.pass
      ),
      to: '/quality/fp/defect'
    }
  ]
})

const processOptions = computed(() => {
  const set = new Set()
  for (const i of inspections.value) {
    const p = processLabel(i)
    if (p !== '—') set.add(p)
  }
  return [...set]
})

const paretoEntries = computed(() =>
  buildParetoData(nc.value, inspections.value, Number(paretoRange.value), paretoProcess.value)
)

const showPareto = computed(() => canShowPareto(paretoEntries.value))

const paretoMaxCount = computed(() =>
  paretoEntries.value.length ? Math.max(...paretoEntries.value.map((e) => e.count)) : 1
)

const paretoOption = computed(() =>
  buildParetoOption(paretoEntries.value, selectedDefect.value)
)

const STATUS_ORDER = [
  { key: 'PASSED', name: '通过', color: QC_COLORS.pass },
  { key: 'PENDING', name: '待判定', color: QC_COLORS.pending },
  { key: 'RECHECK_REQUIRED', name: '需复检', color: QC_COLORS.recheck },
  { key: 'FAILED', name: '不通过', color: QC_COLORS.fail }
]

const statusLegend = computed(() => {
  const c = counts.value
  const m = { PASSED: c.passed, PENDING: c.pending, RECHECK_REQUIRED: c.recheck, FAILED: c.failed }
  return STATUS_ORDER.map((s) => ({ ...s, value: m[s.key] || 0 }))
})

const donutTotal = computed(() => statusLegend.value.reduce((s, x) => s + x.value, 0))

const donutOption = computed(() => {
  const data = statusLegend.value.filter((s) => s.value > 0).map((s) => ({
    name: s.name,
    value: s.value,
    itemStyle: { color: s.color }
  }))
  return buildDonutOption(passRate.value, data, weekPassDelta.value)
})

const sampleRateOption = computed(() =>
  buildSampleRateComboOption(buildDailyPassPoints(judgedRows.value))
)

const closureSteps = computed(() => {
  const g = { PENDING: 0, PROCESSING: 0, DONE: 0 }
  for (const n of nc.value) {
    const st = normHandleStatus(n.handleStatus)
    g[st] = (g[st] || 0) + 1
  }
  return [
    { key: 'PENDING', name: '待处置', value: g.PENDING || 0 },
    { key: 'PROCESSING', name: '处理中', value: g.PROCESSING || 0 },
    { key: 'DONE', name: '已闭环', value: g.DONE || 0 }
  ]
})

const closureStats = computed(() => {
  const done = nc.value.filter((n) => normHandleStatus(n.handleStatus) === 'DONE')
  let totalH = 0
  let cnt = 0
  for (const n of done) {
    const start = parseQcDate(n.registeredAt)
    const end = parseQcDate(n.handledAt)
    if (start && end) {
      totalH += (end - start) / 3600000
      cnt++
    }
  }
  const overdue = nc.value.filter((n) => {
    if (normHandleStatus(n.handleStatus) === 'DONE') return false
    const start = parseQcDate(n.registeredAt)
    return start && (Date.now() - start.getTime()) > 72 * 3600000
  }).length
  return {
    avgHours: cnt ? (totalH / cnt).toFixed(1) : '—',
    overdue
  }
})

const ncDoneOrAll = computed(() => nc.value.length || 1)

const methodRows = computed(() => {
  const g = {}
  for (const n of nc.value) {
    const m = normHandleMethod(n.handleMethod)
    if (['REWORK', 'CONCESSION_ACCEPT', 'SCRAP'].includes(m)) {
    g[m] = (g[m] || 0) + 1
  }
  }
  return [
    { key: 'REWORK', name: '返工', color: QC_COLORS.primary, value: g.REWORK || 0 },
    { key: 'CONCESSION_ACCEPT', name: '让步接收', color: QC_COLORS.recheck, value: g.CONCESSION_ACCEPT || 0 },
    { key: 'SCRAP', name: '报废', color: QC_COLORS.fail, value: g.SCRAP || 0 }
  ]
})

function enrichTask(row) {
  const now = Date.now()
  const created = parseQcDate(row.createdAt)
  const waitMs = created ? now - created.getTime() : 0
  const overdue = isTaskOverdue(row, now)
  const nearOverdue = isTaskNearOverdue(row, now)
  let statusKey = 'pending'
  if (row.inspectionStatus === 'RECHECK_REQUIRED') statusKey = 'recheck'
  else if (overdue) statusKey = 'fail'
  else if (nearOverdue) statusKey = 'recheck'
  return {
    ...row,
    _priority: derivePriority(row, now),
    _waitMs: waitMs,
    _waitText: formatWaitDuration(waitMs),
    _deadline: deriveDeadline(row),
    _overdue: overdue,
    _nearOverdue: nearOverdue,
    _statusKey: statusKey
  }
}

const pendingTasks = computed(() =>
  inspections.value
    .filter((i) => ['PENDING', 'RECHECK_REQUIRED'].includes(i.inspectionStatus))
    .map(enrichTask)
    .sort((a, b) => {
      if (a._priority.sort !== b._priority.sort) return a._priority.sort - b._priority.sort
      if (a._overdue !== b._overdue) return a._overdue ? -1 : 1
      return b._waitMs - a._waitMs
    })
)

const recentCompleted = computed(() =>
  [...inspections.value]
    .filter((i) => ['PASSED', 'FAILED', 'CLOSED'].includes(i.inspectionStatus))
    .sort((a, b) => String(b.inspectedAt || b.updatedAt || '').localeCompare(String(a.inspectedAt || a.updatedAt || '')))
)

const ncSorted = computed(() =>
  [...nc.value].sort((a, b) => String(b.registeredAt || '').localeCompare(String(a.registeredAt || '')))
)

const linkedInspectionIds = computed(() => {
  if (!selectedDefect.value) return null
  return new Set(
    nc.value
      .filter((n) => n.defectType === selectedDefect.value)
      .map((n) => n.inspectionId)
      .filter(Boolean)
  )
})

const displayPendingTasks = computed(() => {
  if (!selectedDefect.value || !linkedInspectionIds.value?.size) return pendingTasks.value
  return pendingTasks.value.filter((t) => linkedInspectionIds.value.has(t.inspectionId))
})

const displayRecent = computed(() => {
  let rows = recentCompleted.value
  if (selectedDefect.value && linkedInspectionIds.value?.size) {
    rows = rows.filter((r) => linkedInspectionIds.value.has(r.inspectionId))
  }
  return rows.slice(0, 12)
})

const displayNc = computed(() => {
  let rows = ncSorted.value
  if (selectedDefect.value) rows = rows.filter((n) => n.defectType === selectedDefect.value)
  return rows.slice(0, 12)
})

const taskTabs = computed(() => [
  { key: 'pending', label: '待检任务', count: displayPendingTasks.value.length || (selectedDefect.value ? 0 : pendingTasks.value.length) },
  { key: 'records', label: '完成记录', count: 0 },
  { key: 'nc', label: '不良品', count: selectedDefect.value ? displayNc.value.length : 0 }
])

const emptyTaskText = computed(() => {
  if (taskTab.value === 'pending') {
    if (selectedDefect.value && !displayPendingTasks.value.length) return '该缺陷类型暂无待检任务'
    return ''
  }
  if (taskTab.value === 'records' && !displayRecent.value.length) {
    return selectedDefect.value ? '暂无关联完成记录' : '暂无已完成质检记录'
  }
  if (taskTab.value === 'nc' && !displayNc.value.length) {
    return selectedDefect.value ? '暂无关联不良品' : '暂无不良品记录'
  }
  return ''
})

function taskPath(row) {
  if (row.inspectionStatus === 'RECHECK_REQUIRED') {
    return row.inspectionCategory === 'RAW_MATERIAL' ? '/quality/material/reinspect' : '/quality/fp/reinspect'
  }
  if (row.inspectionCategory === 'RAW_MATERIAL') return '/quality/material/inspection'
  if (row.inspectionCategory === 'FINISHED_PRODUCT') return '/quality/fp/inspection'
  return '/quality/inspection'
}

function taskRowClass({ row }) {
  const cls = []
  if (row._overdue) cls.push('qc-row--overdue')
  else if (row._nearOverdue || row.inspectionStatus === 'RECHECK_REQUIRED') cls.push('qc-row--recheck')
  return cls.join(' ')
}

function priorityClass(row) {
  if (row._overdue) return 'qc-priority--overdue'
  if (row._priority.key === 'high' || row._nearOverdue) return 'qc-priority--warn'
  return 'qc-priority--normal'
}

function timeClass(row) {
  if (row._overdue) return 'qc-time--overdue'
  if (row._nearOverdue) return 'qc-time--warn'
  return 'qc-time--normal'
}

function toggleDefectFilter(name) {
  selectedDefect.value = selectedDefect.value === name ? '' : name
  if (selectedDefect.value) {
    taskTab.value = displayPendingTasks.value.length ? 'pending' : (displayNc.value.length ? 'nc' : 'records')
  }
}

function onParetoClick(params) {
  if (params?.componentType !== 'series' || params.seriesType !== 'bar') return
  const name = paretoEntries.value[params.dataIndex]?.name
  if (!name) return
  toggleDefectFilter(name)
}

function clearDefectFilter() {
  selectedDefect.value = ''
}

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
.qc-wb {
  min-height: var(--layout-content-min-h, calc(100vh - 92px));
  background: #fff;
  display: flex;
  flex-direction: column;
}

/* —— 顶部指标带 —— */
.qc-wb__metrics {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr)) auto;
  border-bottom: 1px solid #e5e7eb;
  align-items: stretch;
}

.qc-metric {
  padding: 10px 12px 8px;
  border-right: 1px solid #eef1f5;
  min-width: 0;
}

.qc-metric--clickable {
  cursor: pointer;
}

.qc-metric--clickable:hover {
  background: #fafbfc;
}

.qc-metric__label {
  font-size: 11px;
  color: #8a94a6;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.qc-metric__main {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.qc-metric__delta {
  font-size: 11px;
  font-weight: 500;
}

.qc-metric__num {
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
}

.qc-metric__num--rate {
  color: #1f2937;
}

.qc-metric__delta--up {
  color: #2d8a5e;
}

.qc-metric__delta--down,
.qc-metric__delta--neutral {
  color: #6b7280;
}

.qc-metric__delta--flat {
  color: #9aa3b2;
}

.qc-metric__sub {
  font-size: 10px;
  color: #9aa3b2;
  margin-top: 2px;
}

.qc-metric__trend-wrap {
  margin-top: 4px;
}

.qc-metric__trend-label {
  display: block;
  font-size: 9px;
  color: #b8bec8;
  margin-bottom: 1px;
}

.qc-metric__spark {
  height: 18px;
}

.qc-wb__refresh {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  padding: 8px 12px;
  gap: 2px;
  flex-shrink: 0;
}

.qc-wb__time {
  font-size: 11px;
  color: #9aa3b2;
  white-space: nowrap;
}

/* —— 中部图表 —— */
.qc-wb__charts {
  display: grid;
  grid-template-columns: 1.55fr 1fr;
  border-bottom: 1px solid #e5e7eb;
  min-height: 0;
}

.qc-wb__bottom {
  display: grid;
  grid-template-columns: 1fr 1.55fr;
  flex: 1;
  min-height: 0;
}

.qc-panel {
  padding: 12px 14px;
  border-right: 1px solid #eef1f5;
  min-width: 0;
}

.qc-panel:last-child {
  border-right: none;
}

.qc-panel__hd {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.qc-panel__tools {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.qc-panel__select {
  width: 120px;
}

.qc-panel__meta {
  margin-left: auto;
  font-size: 11px;
  font-weight: 500;
  color: #8a94a6;
}

.qc-link {
  font-size: 12px;
  font-weight: 500;
  color: #4a6fa5;
  cursor: pointer;
  white-space: nowrap;
}

.qc-link:hover {
  text-decoration: underline;
}

.qc-filter-chip {
  border: 1px solid #4a6fa5;
  background: #f0f4fa;
  color: #4a6fa5;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 2px;
  cursor: pointer;
}

.qc-chart--pareto {
  height: 248px;
}

.qc-chart--donut {
  width: 168px;
  height: 168px;
  flex-shrink: 0;
}

.qc-chart--combo {
  height: 96px;
}

.qc-rank {
  padding: 4px 0;
}

.qc-rank__hint {
  margin: 0 0 10px;
  font-size: 12px;
  color: #8a94a6;
}

.qc-rank__list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.qc-rank__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 4px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  font-size: 12px;
}

.qc-rank__item:hover,
.qc-rank__item--active {
  background: #f8fafc;
}

.qc-rank__idx {
  width: 18px;
  color: #9aa3b2;
  font-weight: 600;
}

.qc-rank__name {
  width: 88px;
  color: #374151;
  flex-shrink: 0;
}

.qc-rank__bar-wrap {
  flex: 1;
  height: 6px;
  background: #f3f4f6;
}

.qc-rank__bar {
  display: block;
  height: 100%;
  background: #5b7fa8;
  transition: width 0.35s ease;
}

.qc-rank__item--active .qc-rank__bar {
  background: #4a6fa5;
}

.qc-rank__val {
  width: 28px;
  text-align: right;
  font-weight: 700;
  color: #1f2937;
}

.qc-pass-layout {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.qc-pass-side {
  flex: 1;
  min-width: 0;
}

.qc-pass-legend {
  list-style: none;
  margin: 0 0 8px;
  padding: 0;
}

.qc-pass-legend li {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
  border-bottom: 1px solid #f3f4f6;
  font-size: 12px;
  color: #4b5563;
}

.qc-pass-legend li i {
  width: 8px;
  height: 8px;
  border-radius: 1px;
  flex-shrink: 0;
}

.qc-pass-legend em {
  margin-left: auto;
  font-style: normal;
  font-weight: 600;
  color: #1f2937;
}

.qc-pass-legend b {
  width: 36px;
  text-align: right;
  font-weight: 500;
  color: #9aa3b2;
  font-size: 11px;
}

.qc-pass-trend__label {
  font-size: 11px;
  color: #8a94a6;
  margin-bottom: 2px;
}

/* —— 闭环流程 —— */
.qc-flow {
  display: flex;
  align-items: center;
  gap: 0;
  margin-bottom: 12px;
}

.qc-flow__item {
  display: flex;
  align-items: center;
  flex: 1;
}

.qc-flow__node {
  flex: 1;
  text-align: center;
  padding: 10px 6px;
  border: 1px solid #e5e7eb;
  background: #fafbfc;
  opacity: 0.55;
  transition: opacity 0.3s, border-color 0.3s;
}

.qc-flow__node--active {
  opacity: 1;
  border-color: #c5d0de;
  background: #fff;
}

.qc-flow__count {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.qc-flow__name {
  display: block;
  font-size: 11px;
  color: #8a94a6;
  margin-top: 2px;
}

.qc-flow__arrow {
  width: 20px;
  height: 1px;
  background: #d1d5db;
  position: relative;
  flex-shrink: 0;
}

.qc-flow__arrow::after {
  content: '';
  position: absolute;
  right: 0;
  top: -3px;
  border: 4px solid transparent;
  border-left-color: #d1d5db;
}

.qc-method__row {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
  font-size: 12px;
}

.qc-method__name {
  width: 56px;
  color: #6b7280;
}

.qc-method__track {
  flex: 1;
  height: 6px;
  background: #f3f4f6;
  margin: 0 8px;
  overflow: hidden;
}

.qc-method__fill {
  height: 100%;
  transition: width 0.45s ease;
}

.qc-method__val {
  width: 24px;
  text-align: right;
  font-weight: 600;
  color: #374151;
  font-variant-numeric: tabular-nums;
}

/* —— 任务表格 —— */
.qc-tab {
  border: none;
  background: transparent;
  font-size: 12px;
  color: #8a94a6;
  padding: 2px 8px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
}

.qc-tab--active {
  color: #1f2937;
  font-weight: 600;
  border-bottom-color: #4a6fa5;
}

.qc-tab em {
  font-style: normal;
  margin-left: 4px;
  font-size: 10px;
  color: #4a6fa5;
}

.qc-table-wrap {
  border-top: 1px solid #f3f4f6;
}

.qc-priority {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 2px;
}

.qc-priority--overdue {
  color: #c0392b;
  background: #fef2f2;
}

.qc-priority--warn {
  color: #d48806;
  background: #fffbeb;
}

.qc-priority--normal {
  color: #374151;
  background: #f3f4f6;
}

.qc-time--normal {
  color: #374151;
}

.qc-time--warn {
  color: #d48806;
  font-weight: 600;
}

.qc-time--overdue {
  color: #c0392b;
  font-weight: 600;
}

.qc-status {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 2px;
}

.qc-status--pass {
  color: #2d8a5e;
  background: #ecfdf5;
}

.qc-status--recheck {
  color: #d48806;
  background: #fffbeb;
}

.qc-status--fail {
  color: #c0392b;
  background: #fef2f2;
}

.qc-status--pending {
  color: #6b7280;
  background: #f3f4f6;
}

.qc-sev--CRITICAL,
.qc-sev--MAJOR {
  color: #d48806;
}

.qc-empty {
  text-align: center;
  color: #9aa3b2;
  padding: 36px 0;
  font-size: 13px;
  margin: 0;
}

.qc-empty--ok {
  color: #2d8a5e;
}

.clickable :deep(.el-table__row) {
  cursor: pointer;
}

:deep(.qc-row--recheck) {
  background: #fffdf5 !important;
}

:deep(.qc-row--overdue) {
  background: #fef2f2 !important;
}

:deep(.el-table) {
  --el-table-border-color: #f3f4f6;
  --el-table-header-bg-color: #fafbfc;
}

:deep(.el-table th.el-table__cell) {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
}

:deep(.el-table td.el-table__cell) {
  font-size: 12px;
  color: #374151;
}

@media (max-width: 1280px) {
  .qc-wb__metrics {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .qc-wb__refresh {
    grid-column: 1 / -1;
    flex-direction: row;
    justify-content: flex-end;
    border-top: 1px solid #eef1f5;
  }

  .qc-wb__charts,
  .qc-wb__bottom {
    grid-template-columns: 1fr;
  }

  .qc-panel {
    border-right: none;
    border-bottom: 1px solid #eef1f5;
  }
}
</style>
