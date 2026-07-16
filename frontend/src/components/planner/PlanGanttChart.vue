<template>
  <div class="plan-gantt" :class="{ 'plan-gantt--fs': fullscreen }">
    <div class="plan-gantt__toolbar">
      <el-radio-group :model-value="viewMode" size="small" @update:model-value="$emit('update:view-mode', $event)">
        <el-radio-button label="plan">按计划</el-radio-button>
        <el-radio-button label="equipment">按设备</el-radio-button>
        <el-radio-button label="workshop">按车间</el-radio-button>
      </el-radio-group>
      <el-radio-group v-model="scale" size="small" @change="render">
        <el-radio-button label="day">日</el-radio-button>
        <el-radio-button label="week">周</el-radio-button>
      </el-radio-group>
      <el-button-group size="small">
        <el-button @click="zoomOut">缩小</el-button>
        <el-button @click="zoomIn">放大</el-button>
        <el-button @click="scrollToday">定位今日</el-button>
      </el-button-group>
      <span class="plan-gantt__legend">
        <i class="lg lg--palette" />按计划分色
        <i class="lg lg--idle" />未开始
        <i class="lg lg--run" />执行中
        <i class="lg lg--done" />已完成
        <i class="lg lg--late" />延期
        <i class="lg lg--urgent" />紧急
        <i class="lg lg--conflict" />冲突
      </span>
      <el-button v-if="fullscreen" size="small" type="primary" plain class="plan-gantt__exit" @click="$emit('exit-fullscreen')">
        退出全屏
      </el-button>
    </div>

    <div v-loading="loading" class="plan-gantt__body">
      <div ref="leftRef" class="plan-gantt__left" @scroll="onLeftScroll">
        <div class="plan-gantt__left-head">
          <span>{{ leftCols[0] }}</span><span>{{ leftCols[1] }}</span><span>{{ leftCols[2] }}</span>
        </div>
        <div
          v-for="(row, idx) in rows"
          :key="rowKey(row, idx)"
          class="plan-gantt__left-row"
          :class="{ 'plan-gantt__left-row--active': hoverIndex === idx, 'plan-gantt__left-row--conflict': hasConflict(idx) }"
          :style="{ height: `${rowHeight}px` }"
          @mouseenter="setHover(idx)"
          @mouseleave="clearHover"
        >
          <span :title="leftVal(row, 0)">{{ leftVal(row, 0) }}</span>
          <span :title="leftVal(row, 1)">{{ leftVal(row, 1) }}</span>
          <span :title="leftVal(row, 2)">{{ leftVal(row, 2) }}</span>
        </div>
        <div v-if="!rows.length" class="plan-gantt__empty">暂无甘特数据。请先在计划表中创建生产计划并保存工序排程，然后点击刷新。</div>
      </div>
      <div ref="chartWrapRef" class="plan-gantt__chart-wrap">
        <div ref="chartRef" class="plan-gantt__chart" :style="{ height: chartHeight + 'px' }" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { ganttPlanColor } from '@/utils/planMetrics'

const STATUS_BORDER = {
  未开始: '#6b7280',
  执行中: '#1d4ed8',
  已完成: '#14532d',
  延期: '#b91c1c'
}

const props = defineProps({
  rows: { type: Array, default: () => [] },
  dependencies: { type: Array, default: () => [] },
  conflictIndices: { type: Object, default: () => new Set() },
  viewMode: { type: String, default: 'plan' },
  loading: { type: Boolean, default: false },
  fullscreen: { type: Boolean, default: false }
})

defineEmits(['exit-fullscreen', 'update:view-mode'])

const leftCols = computed(() => {
  if (props.viewMode === 'equipment') return ['设备', '计划', '工序']
  if (props.viewMode === 'workshop') return ['车间', '计划', '工序']
  return ['计划编号', '产品型号', '工序']
})

function leftVal(row, col) {
  if (props.viewMode === 'equipment') {
    return [row.equipment || '—', row.planId, row.stepName][col]
  }
  if (props.viewMode === 'workshop') {
    return [row.workshop || '—', row.planId, row.stepName][col]
  }
  return [row.planId, row.productModel, row.stepName][col]
}

function hasConflict(idx) {
  const set = props.conflictIndices
  return set?.has?.(idx) || (Array.isArray(set) && set.includes(idx))
}

const scale = ref('day')
const zoom = ref(1)
const rowHeight = 48
const leftRef = ref(null)
const chartRef = ref(null)
const chartWrapRef = ref(null)
const hoverIndex = ref(-1)
let chart = null
let syncing = false

const chartHeight = computed(() => Math.max(320, props.rows.length * rowHeight + 72))

function rowKey(row, idx) {
  return `${row.planId}-${row.stepName}-${idx}`
}

function setHover(idx) {
  hoverIndex.value = idx
}

function clearHover() {
  hoverIndex.value = -1
}

function parseTs(val) {
  if (!val) return null
  const s = String(val).replace(' ', 'T')
  const d = new Date(s.length === 10 ? `${s}T08:00:00` : s)
  return Number.isNaN(d.getTime()) ? null : d.getTime()
}

function barEndTs(row) {
  const start = parseTs(row.plannedStart)
  let end = parseTs(row.plannedEnd)
  if (!end && start) end = start + 86400000
  if (start && end && end <= start) end = start + 3600000 * 4
  return end || start
}

function shortStartDate(meta) {
  if (!meta?.plannedStart) return ''
  const s = String(meta.plannedStart).slice(0, 10)
  const parts = s.split('-')
  return parts.length === 3 ? `${parts[1]}/${parts[2]}` : ''
}

function buildBarLabel(meta, barWidth) {
  if (!meta) return ''
  const step = meta.stepName || ''
  const pct = `${meta.progress || 0}%`
  if (barWidth >= 240) return `${step}  ${meta.dateRangeLabel}  ${pct}`
  if (barWidth >= 150) return `${step}  ${shortStartDate(meta)}  ${pct}`
  if (barWidth >= 80) return step
  if (barWidth >= 40) return pct
  return ''
}

function timeRange() {
  const stamps = []
  props.rows.forEach((r) => {
    const s = parseTs(r.plannedStart)
    const e = barEndTs(r)
    if (s) stamps.push(s)
    if (e) stamps.push(e)
  })
  const now = Date.now()
  if (!stamps.length) {
    return [now - 3 * 86400000, now + 14 * 86400000]
  }
  const min = Math.min(...stamps)
  const max = Math.max(...stamps, now)
  return [min - 2 * 86400000, max + 2 * 86400000]
}

function buildSeriesData() {
  return props.rows.map((row, idx) => ({
    name: row.stepName,
    value: [idx, parseTs(row.plannedStart), barEndTs(row), row.progress || 0, row.status, row.urgent, row.planId],
    meta: row
  }))
}

function tooltipHtml(meta) {
  if (!meta) return ''
  return [
    `<div style="font-size:14px;font-weight:600;color:#111827;margin-bottom:8px">${meta.stepName} <span style="color:#6b7280;font-weight:400">(${meta.planId})</span></div>`,
    `<table style="border-collapse:collapse;font-size:13px;line-height:1.7;color:#374151">`,
    `<tr><td style="color:#9ca3af;padding-right:12px">订单编号</td><td>${meta.orderNo}</td></tr>`,
    `<tr><td style="color:#9ca3af;padding-right:12px">产品型号</td><td>${meta.productModel}</td></tr>`,
    `<tr><td style="color:#9ca3af;padding-right:12px">计划状态</td><td>${meta.planStatus}</td></tr>`,
    `<tr><td style="color:#9ca3af;padding-right:12px">车间 / 设备</td><td>${meta.workshop} / ${meta.equipment}</td></tr>`,
    `<tr><td style="color:#9ca3af;padding-right:12px">计划时间</td><td>${meta.startLabel} ~ ${meta.endLabel}</td></tr>`,
    `<tr><td style="color:#9ca3af;padding-right:12px">负责人</td><td>${meta.operator}</td></tr>`,
    `<tr><td style="color:#9ca3af;padding-right:12px">数量 / 工时</td><td>${meta.plannedQuantity} 台 / ${meta.standardHours || '—'} h</td></tr>`,
    `<tr><td style="color:#9ca3af;padding-right:12px">完成进度</td><td><b>${meta.progress}%</b>（${meta.status}）</td></tr>`,
    `</table>`
  ].join('')
}

function bindChartEvents() {
  if (!chart) return
  chart.off('mouseover')
  chart.off('mouseout')
  chart.off('globalout')

  chart.on('mouseover', (params) => {
    if (params.componentType !== 'series' || params.dataIndex == null) return
    setHover(params.dataIndex)
  })
  chart.on('mouseout', (params) => {
    if (params.componentType !== 'series') return
    clearHover()
  })
  chart.on('globalout', () => clearHover())
}

function render() {
  if (!chart) return
  const [min, max] = timeRange()
  const dayMs = 86400000
  const unit = scale.value === 'week' ? 7 * dayMs : dayMs
  const span = max - min
  const pxPerUnit = (scale.value === 'week' ? 72 : 32) * zoom.value
  const chartWidth = Math.max(chartWrapRef.value?.clientWidth || 800, (span / unit) * pxPerUnit + 160)

  chart.resize({ width: chartWidth, height: chartHeight.value })

  const now = Date.now()
  chart.setOption({
    animation: false,
    grid: { left: 8, right: 24, top: 36, bottom: 48 },
    tooltip: {
      show: true,
      trigger: 'item',
      confine: true,
      appendToBody: true,
      borderColor: '#e5e7eb',
      backgroundColor: '#ffffff',
      padding: [12, 14],
      extraCssText: 'box-shadow:0 4px 14px rgba(15,23,42,.12);border-radius:6px;max-width:360px;',
      formatter(p) {
        return tooltipHtml(p.data?.meta)
      }
    },
    xAxis: {
      type: 'time',
      min,
      max,
      axisLine: { lineStyle: { color: '#d1d5db' } },
      axisLabel: {
        color: '#6b7280',
        fontSize: 12,
        formatter(v) {
          const d = new Date(v)
          if (scale.value === 'week') {
            return `${d.getMonth() + 1}/${d.getDate()}`
          }
          return `${d.getMonth() + 1}/${d.getDate()}`
        }
      },
      splitLine: { show: true, lineStyle: { color: '#f3f4f6' } }
    },
    yAxis: {
      type: 'category',
      data: props.rows.map((_, i) => i),
      inverse: true,
      show: false
    },
    dataZoom: [
      { type: 'slider', xAxisIndex: 0, height: 18, bottom: 6, filterMode: 'none', zoomLock: true }
    ],
    series: [{
      type: 'custom',
      name: '工序任务',
      emphasis: { focus: 'self' },
      renderItem(params, api) {
        const idx = api.value(0)
        const start = api.value(1)
        const end = api.value(2)
        const progress = api.value(3) || 0
        const status = api.value(4)
        const urgent = api.value(5)
        const planId = api.value(6)
        const startPt = api.coord([start, idx])
        const endPt = api.coord([end, idx])
        const barH = rowHeight * 0.56
        const y = startPt[1] - barH / 2
        const w = Math.max(endPt[0] - startPt[0], 12)
        const meta = props.rows[idx]
        const active = hoverIndex.value === idx
        const conflict = hasConflict(idx)
        const fillColor = meta?.planColor || ganttPlanColor(planId)
        const borderColor = conflict ? '#dc2626' : (STATUS_BORDER[status] || STATUS_BORDER['未开始'])
        const children = [
          {
            type: 'rect',
            shape: { x: startPt[0] - 2, y: y - 6, width: w + 4, height: barH + 12, r: 2 },
            style: { fill: active ? 'rgba(61,122,95,0.08)' : 'transparent' },
            silent: false
          },
          {
            type: 'rect',
            shape: { x: startPt[0], y, width: w, height: barH, r: 2 },
            style: {
              fill: fillColor,
              opacity: active ? 1 : 0.88,
              stroke: borderColor,
              lineWidth: conflict ? 2 : (active ? 2 : 1)
            }
          },
          {
            type: 'rect',
            shape: { x: startPt[0], y, width: w * (progress / 100), height: barH, r: 3 },
            style: { fill: 'rgba(255,255,255,0.28)' }
          }
        ]
        if (urgent) {
          children.splice(1, 0, {
            type: 'rect',
            shape: { x: startPt[0] - 5, y: y + 5, width: 3, height: barH - 10, r: 1 },
            style: { fill: '#d97706' }
          })
        }
        if (conflict) {
          children.push({
            type: 'text',
            style: {
              text: '⚠',
              x: startPt[0] + w - 14,
              y: y + 4,
              fill: '#dc2626',
              fontSize: 12,
              fontWeight: 700
            }
          })
        }
        if (meta) {
          const label = buildBarLabel(meta, w)
          if (label) {
            children.push({
              type: 'text',
              style: {
                text: label,
                x: startPt[0] + 8,
                y: y + barH / 2,
                fill: '#fff',
                fontSize: 12,
                fontWeight: 500,
                textVerticalAlign: 'middle',
                width: Math.max(w - 16, 0),
                overflow: 'truncate',
                ellipsis: '…'
              }
            })
          }
        }
        return { type: 'group', children }
      },
      encode: { x: [1, 2], y: 0 },
      data: buildSeriesData(),
      markLine: {
        symbol: 'none',
        silent: true,
        data: [
          {
            xAxis: new Date().setHours(0, 0, 0, 0),
            lineStyle: { color: '#3d7a5f', width: 1.5, type: 'solid' },
            label: { formatter: '今日', color: '#3d7a5f', fontSize: 11, position: 'end' }
          }
        ]
      }
    },
    {
      type: 'custom',
      name: '工序依赖',
      silent: true,
      z: 1,
      renderItem(params, api) {
        const dep = props.dependencies[params.dataIndex]
        if (!dep) return null
        const fromRow = props.rows[dep.fromIdx]
        const toRow = props.rows[dep.toIdx]
        if (!fromRow || !toRow) return null
        const fromEnd = barEndTs(fromRow)
        const toStart = parseTs(toRow.plannedStart)
        const fromPt = api.coord([fromEnd, dep.fromIdx])
        const toPt = api.coord([toStart, dep.toIdx])
        const midX = (fromPt[0] + toPt[0]) / 2
        return {
          type: 'group',
          children: [
            {
              type: 'line',
              shape: { x1: fromPt[0], y1: fromPt[1], x2: midX, y2: fromPt[1] },
              style: { stroke: '#9ca3af', lineWidth: 1 }
            },
            {
              type: 'line',
              shape: { x1: midX, y1: fromPt[1], x2: midX, y2: toPt[1] },
              style: { stroke: '#9ca3af', lineWidth: 1 }
            },
            {
              type: 'line',
              shape: { x1: midX, y1: toPt[1], x2: toPt[0] - 4, y2: toPt[1] },
              style: { stroke: '#9ca3af', lineWidth: 1 }
            },
            {
              type: 'polygon',
              shape: { points: [[toPt[0] - 4, toPt[1]], [toPt[0] - 10, toPt[1] - 4], [toPt[0] - 10, toPt[1] + 4]] },
              style: { fill: '#9ca3af' }
            }
          ]
        }
      },
      encode: { x: -1, y: -1 },
      data: props.dependencies.map((d, i) => ({ value: i, ...d }))
    }]
  }, true)
  bindChartEvents()
  bindWheelScroll()
}

function initChart() {
  if (!chartRef.value) return
  chart?.dispose()
  chart = echarts.init(chartRef.value, 'mes')
  render()
}

function onLeftScroll(e) {
  if (syncing) return
  syncing = true
  if (chartWrapRef.value) chartWrapRef.value.scrollTop = e.target.scrollTop
  syncing = false
}

function bindChartScroll() {
  chartWrapRef.value?.addEventListener('scroll', (e) => {
    if (syncing) return
    syncing = true
    if (leftRef.value) leftRef.value.scrollTop = e.target.scrollTop
    syncing = false
  })
}

let wheelHandler = null

function bindWheelScroll() {
  if (!chart) return
  const zr = chart.getZr()
  zr.off('mousewheel')
  zr.on('mousewheel', (e) => {
    scrollChartHorizontally(e.event)
  })

  const wrap = chartWrapRef.value
  if (wrap) {
    if (wheelHandler) wrap.removeEventListener('wheel', wheelHandler)
    wheelHandler = (e) => scrollChartHorizontally(e)
    wrap.addEventListener('wheel', wheelHandler, { passive: false })
  }
}

function scrollChartHorizontally(e) {
  const el = chartWrapRef.value
  if (!el) return
  e.preventDefault()
  el.scrollLeft += (e.deltaY || 0) + (e.deltaX || 0)
}

function zoomIn() {
  zoom.value = Math.min(2.5, zoom.value + 0.2)
  render()
}

function zoomOut() {
  zoom.value = Math.max(0.6, zoom.value - 0.2)
  render()
}

function scrollToday() {
  if (!chart) return
  const today = new Date().setHours(0, 0, 0, 0)
  const [min, max] = timeRange()
  const percent = ((today - min) / (max - min)) * 100
  chart.dispatchAction({ type: 'dataZoom', start: Math.max(0, percent - 20), end: Math.min(100, percent + 30) })
}

function onResize() {
  render()
}

watch(hoverIndex, () => nextTick(render))
watch(() => props.rows, () => {
  clearHover()
  nextTick(render)
}, { deep: true })
watch(() => [props.dependencies, props.viewMode, props.conflictIndices], () => nextTick(render), { deep: true })
watch(() => props.loading, (v) => { if (!v) nextTick(render) })
watch(() => props.fullscreen, () => nextTick(() => { chart?.resize(); render() }))

onMounted(() => {
  initChart()
  bindChartScroll()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (chartWrapRef.value && wheelHandler) {
    chartWrapRef.value.removeEventListener('wheel', wheelHandler)
  }
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.plan-gantt {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.plan-gantt__toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 44px;
  padding: 0 12px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.plan-gantt__hint {
  font-size: 12px;
  color: #9ca3af;
}

.plan-gantt__legend {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #6b7280;
}

.lg {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
  margin-right: 4px;
  vertical-align: -1px;
}
.lg--palette { background: linear-gradient(90deg, #5470c6, #91cc75, #fac858, #ee6666); }
.lg--idle { background: #9ca3af; }
.lg--run { background: #3d7a5f; }
.lg--done { background: #217346; }
.lg--late { background: #c4564c; }
.lg--urgent { background: #d97706; width: 3px; border-radius: 1px; }
.lg--conflict { background: #fff; border: 2px solid #dc2626; width: 8px; height: 8px; border-radius: 50%; }

.plan-gantt__exit { margin-left: 8px; }

.plan-gantt__body {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

.plan-gantt__left {
  width: 380px;
  flex-shrink: 0;
  border-right: 1px solid #e5e7eb;
  overflow-y: auto;
  overflow-x: hidden;
  background: #fafafa;
}

.plan-gantt__left-head,
.plan-gantt__left-row {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr) 132px;
  gap: 8px;
  padding: 0 10px;
  align-items: center;
  font-size: 13px;
  color: #374151;
}

.plan-gantt__left-head {
  height: 36px;
  font-weight: 500;
  color: #6b7280;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 2;
}

.plan-gantt__left-row {
  border-bottom: 1px solid #f3f4f6;
  cursor: default;
  transition: background 0.15s;
}

.plan-gantt__left-row--active {
  background: #e8f5e9;
}

.plan-gantt__left-row--conflict {
  border-left: 3px solid #dc2626;
}

.plan-gantt__left-row span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-gantt__chart-wrap {
  flex: 1;
  overflow: auto;
}

.plan-gantt__chart {
  min-width: 100%;
}

.plan-gantt__empty {
  padding: 48px 16px;
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
}

.plan-gantt--fs {
  border: none;
}
</style>
