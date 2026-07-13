import { PRODUCTION_STAGES, stageForStepName, isProductionStep } from '@/utils/productionProgress'

const ACCENT = '#2563eb'
const ACCENT2 = '#0d9488'
const MUTED = '#94a3b8'
const STATUS_COLORS = {
  已完成: '#0d9488',
  生产中: '#2563eb',
  待质检: '#d97706',
  已派工: '#6366f1',
  已下达: '#64748b',
  草稿: '#cbd5e1'
}

function parseDateKey(value) {
  if (!value) return ''
  const s = String(value).trim()
  if (s.length >= 10) return s.slice(0, 10)
  return ''
}

function lastNDays(n) {
  const days = []
  const now = new Date()
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(d.getDate() - i)
    const key = d.toISOString().slice(0, 10)
    const label = `${d.getMonth() + 1}/${d.getDate()}`
    days.push({ key, label })
  }
  return days
}

export function buildSevenDayOutput(workReports = []) {
  const days = lastNDays(7)
  const bucket = Object.fromEntries(days.map((d) => [d.key, 0]))
  for (const r of workReports) {
    if (r.status === '已驳回') continue
    const key = parseDateKey(r.endTime || r.createdAt)
    if (!key || bucket[key] === undefined) continue
    bucket[key] += r.qualifiedQty || r.reportQty || 0
  }
  return {
    labels: days.map((d) => d.label),
    values: days.map((d) => bucket[d.key])
  }
}

export function buildStatusDistribution(workOrders = []) {
  const active = workOrders.filter((w) => w.status !== '草稿')
  const counts = {}
  for (const wo of active) {
    const s = wo.status || '其他'
    counts[s] = (counts[s] || 0) + 1
  }
  const items = Object.entries(counts)
    .map(([name, value]) => ({ name, value, color: STATUS_COLORS[name] || MUTED }))
    .sort((a, b) => b.value - a.value)
  return items
}

export function buildStageProgress(dispatches = []) {
  return PRODUCTION_STAGES.map((stage) => {
    const related = dispatches.filter((d) => {
      if (!isProductionStep(d.processStep)) return false
      const s = stageForStepName(d.processStep)
      return s?.stepKey === stage.stepKey
    })
    const plan = related.reduce((sum, d) => sum + (d.planQty || 0), 0)
    const done = related.reduce((sum, d) => sum + (d.completedQty || 0), 0)
    const pct = plan > 0 ? Math.min(100, Math.round((done / plan) * 100)) : 0
    return {
      stepName: stage.stepName,
      planQty: plan,
      completedQty: done,
      percent: pct
    }
  })
}

export function computeProgressKpi(workOrders = [], plans = [], orders = []) {
  const list = workOrders.filter((w) => w.status !== '草稿')
  const planMap = new Map(plans.map((p) => [p.id, p]))
  const orderMap = new Map(orders.map((o) => [o.id, o]))

  let plannedTotal = 0
  let completedTotal = 0
  let inProduction = 0
  let finished = 0
  let delayed = 0

  for (const wo of list) {
    plannedTotal += wo.quantity || 0
    completedTotal += wo.completedQty || 0
    if (wo.status === '已完成') finished += 1
    if (['生产中', '已派工', '待质检', '已下达'].includes(wo.status)) inProduction += 1
    if (isWorkOrderDelayed(wo, planMap, orderMap)) delayed += 1
  }

  const completionRate = plannedTotal > 0 ? Math.min(100, Math.round((completedTotal / plannedTotal) * 100)) : 0

  return {
    plannedTotal,
    completedTotal,
    inProduction,
    finished,
    delayed,
    completionRate
  }
}

export function isWorkOrderDelayed(wo, planMap, orderMap) {
  if (!wo || wo.status === '已完成' || wo.status === '草稿') return false
  const pct = wo.quantity ? Math.round(((wo.completedQty || 0) / wo.quantity) * 100) : 0
  const plan = planMap.get(wo.planId)
  const order = orderMap.get(wo.orderId || wo.orderNo)
  const endStr = plan?.planEnd || order?.deliveryDate || ''
  if (endStr) {
    const end = new Date(endStr)
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    end.setHours(0, 0, 0, 0)
    const daysLeft = Math.ceil((end - today) / 86400000)
    if (daysLeft < 0 && wo.status !== '已完成') return true
    if (daysLeft <= 3 && pct < 80) return true
    if (daysLeft <= 5 && pct < 70) return true
  }
  return ['生产中', '已派工', '待质检'].includes(wo.status) && pct < 35
}

export function resolveCurrentStep(wo, dispatches) {
  const related = dispatches.filter((d) => d.workOrderId === wo.id || d.workOrderNo === wo.id)
  const running = related.find((d) => ['生产中', '已接收'].includes(d.status))
  if (running) return running.processStep || '—'
  const prod = related.filter((d) => isProductionStep(d.processStep))
  const incomplete = prod.find((d) => (d.completedQty || 0) < (d.planQty || wo.quantity || 0))
  if (incomplete) return incomplete.processStep || '—'
  const lastDone = [...prod].reverse().find((d) => (d.completedQty || 0) > 0)
  return lastDone?.processStep || (prod[0]?.processStep ?? '—')
}

export function resolveEta(wo, planMap, orderMap) {
  const plan = planMap.get(wo.planId)
  const order = orderMap.get(wo.orderId || wo.orderNo)
  return plan?.planEnd || order?.deliveryDate || '—'
}

export function buildWorkOrderTimeline(wo, dispatches, workReports = []) {
  const relatedDispatches = dispatches.filter(
    (d) => d.workOrderId === wo.id || d.workOrderNo === wo.id
  )
  const relatedReports = workReports.filter(
    (r) => r.workOrderId === wo.id && r.status !== '已驳回'
  )

  return PRODUCTION_STAGES.map((stage) => {
    const stageDispatches = relatedDispatches.filter((d) => {
      const s = stageForStepName(d.processStep)
      return s?.stepKey === stage.stepKey
    })
    const planQty = stageDispatches.reduce((s, d) => s + (d.planQty || 0), 0) || wo.quantity || 0
    const completedQty = stageDispatches.reduce((s, d) => s + (d.completedQty || 0), 0)
    const dispatchIds = new Set(stageDispatches.map((d) => d.id || d.dispatchId))
    const stageReports = relatedReports.filter((r) => dispatchIds.has(r.dispatchId))
    const startTime = stageReports
      .map((r) => r.startTime || r.createdAt)
      .filter(Boolean)
      .sort()[0] || stageDispatches.map((d) => d.planStart).filter(Boolean).sort()[0] || ''
    const endTime = stageReports
      .map((r) => r.endTime)
      .filter(Boolean)
      .sort()
      .pop() || ''
    const status = stageDispatches.some((d) => d.status === '生产中')
      ? '生产中'
      : stageDispatches.some((d) => d.status === '已接收')
        ? '已接收'
        : completedQty >= planQty && planQty > 0
          ? '已完成'
          : completedQty > 0
            ? '进行中'
            : stageDispatches.length
              ? '待开工'
              : '未派工'
    const percent = planQty > 0 ? Math.min(100, Math.round((completedQty / planQty) * 100)) : 0

    return {
      order: stage.order,
      stepName: stage.stepName,
      planQty,
      completedQty,
      percent,
      status,
      startTime: startTime || '—',
      endTime: endTime || '—',
      operatorName: stageDispatches.map((d) => d.operatorName).filter(Boolean)[0] || '—'
    }
  })
}

const CHART_ANIM = {
  animationDurationUpdate: 600,
  animationEasingUpdate: 'cubicOut'
}

export function buildTrendOption(labels, values) {
  return {
    ...CHART_ANIM,
    grid: { left: 48, right: 16, top: 28, bottom: 28 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: labels,
      boundaryGap: false,
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      name: '产量(台)',
      nameTextStyle: { fontSize: 11, color: '#64748b' },
      splitLine: { lineStyle: { type: 'dashed', color: '#eef2f6' } }
    },
    series: [
      {
        name: '合格产量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: ACCENT },
        itemStyle: { color: ACCENT },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(37,99,235,0.12)' },
              { offset: 1, color: 'rgba(37,99,235,0)' }
            ]
          }
        },
        data: values
      }
    ]
  }
}

export function buildDonutOption(items) {
  return {
    ...CHART_ANIM,
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: {
      orient: 'vertical',
      right: 4,
      top: 'center',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { fontSize: 12 }
    },
    series: [
      {
        type: 'pie',
        radius: ['48%', '68%'],
        center: ['38%', '50%'],
        avoidLabelOverlap: true,
        label: { show: false },
        labelLine: { show: false },
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        data: items.map((i) => ({
          name: i.name,
          value: i.value,
          itemStyle: { color: i.color }
        }))
      }
    ]
  }
}

export function buildStageBarOption(stages) {
  const names = stages.map((s) => s.stepName)
  const percents = stages.map((s) => s.percent)
  return {
    ...CHART_ANIM,
    grid: { left: 96, right: 48, top: 8, bottom: 8 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        const p = params[0]
        const row = stages[p.dataIndex]
        return `${p.name}<br/>完成 ${row.completedQty}/${row.planQty} 台 · ${row.percent}%`
      }
    },
    xAxis: {
      type: 'value',
      max: 100,
      axisLabel: { formatter: '{value}%' },
      splitLine: { lineStyle: { type: 'dashed', color: '#eef2f6' } }
    },
    yAxis: {
      type: 'category',
      data: names,
      inverse: true,
      axisTick: { show: false },
      axisLine: { show: false }
    },
    series: [
      {
        type: 'bar',
        barWidth: 14,
        data: percents.map((v, i) => ({
          value: v,
          itemStyle: {
            color: i % 2 === 0 ? ACCENT : ACCENT2,
            borderRadius: [0, 3, 3, 0]
          }
        })),
        label: {
          show: true,
          position: 'right',
          formatter: ({ value }) => `${value}%`,
          fontSize: 11,
          color: '#64748b'
        }
      }
    ]
  }
}
