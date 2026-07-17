/** 生产计划衍生指标（基于现有快照与工序排程数据） */

function parseDate(val) {
  if (!val) return null
  const s = String(val).slice(0, 10)
  const d = new Date(`${s}T00:00:00`)
  return Number.isNaN(d.getTime()) ? null : d
}

function parseDateTime(val) {
  if (!val) return null
  const normalized = String(val).replace(' ', 'T')
  const d = new Date(normalized.length === 10 ? `${normalized}T00:00:00` : normalized)
  return Number.isNaN(d.getTime()) ? null : d
}

export function priorityLabel(priority) {
  const key = String(priority || '').toUpperCase()
  const map = {
    HIGH: '紧急',
    URGENT: '紧急',
    NORMAL: '普通',
    LOW: '低',
    高: '紧急',
    中: '普通',
    低: '低',
    紧急: '紧急',
    普通: '普通'
  }
  return map[key] || map[priority] || priority || '普通'
}

export function isUrgentPlan(plan) {
  const p = String(plan?.priority || '').toUpperCase()
  return ['HIGH', 'URGENT', '高', '紧急', '1'].includes(p) || plan?.priority === '紧急'
}

export function computePlanProgress(plan, mes) {
  if (plan?.status === '已完成') return 100
  const workOrders = (mes?.workOrders || []).filter((w) => w.planId === plan.id)
  if (!workOrders.length) {
    if (['执行中', '已发布'].includes(plan?.status)) return 5
    return 0
  }
  const total = workOrders.reduce((s, w) => s + (Number(w.quantity) || 0), 0)
  const done = workOrders.reduce((s, w) => s + (Number(w.completedQty) || 0), 0)
  return total > 0 ? Math.min(100, Math.round((done / total) * 100)) : 0
}

export function summarizeSchedules(schedules = []) {
  if (!schedules.length) {
    return { workshop: '—', estimatedHours: 0 }
  }
  const workshops = [...new Set(schedules.map((s) => s.workshop).filter(Boolean))]
  const hours = schedules.reduce((sum, s) => sum + (Number(s.standardHours) || 0), 0)
  return {
    workshop: workshops.length > 1 ? workshops.slice(0, 2).join('、') + (workshops.length > 2 ? '…' : '') : (workshops[0] || '—'),
    estimatedHours: Math.round(hours * 10) / 10
  }
}

export function computeDelayRisk(plan, progress = 0) {
  if (['已完成', '已取消'].includes(plan?.status)) return '正常'
  const end = parseDate(plan?.planEnd)
  if (!end) return '正常'
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  if (end < today && progress < 100) return '严重延期'
  const daysLeft = Math.ceil((end - today) / 86400000)
  if (daysLeft <= 3 && progress < 80) return '延期风险'
  if (daysLeft <= 7 && progress < 50) return '关注'
  return '正常'
}

export function computeKitRate(materialGaps = []) {
  if (!materialGaps?.length) return null
  const ok = materialGaps.filter((g) => (Number(g.gapQty) || 0) <= 0).length
  return Math.round((ok / materialGaps.length) * 100)
}

export function computeEquipmentLoad(schedules = [], mes) {
  const codes = [...new Set(schedules.map((s) => s.equipmentCode).filter(Boolean))]
  if (!codes.length) return null
  const equipment = mes?.equipment || []
  let busy = 0
  codes.forEach((code) => {
    const eq = equipment.find((e) => e.code === code || e.id === code || e.equipmentCode === code)
    const st = String(eq?.status || '').toUpperCase()
    if (st === 'RUNNING' || st === '运行中' || st === 'BUSY') busy += 1
  })
  return Math.round((busy / codes.length) * 100)
}

export function aggregateSchedulingRisk(delayRisk, conflictCount = 0, hasDanger = false) {
  if (hasDanger || delayRisk === '严重延期') return '高风险'
  if (conflictCount > 0 || delayRisk === '延期风险') return '中风险'
  if (delayRisk === '关注') return '低风险'
  return '正常'
}

export function enrichPlanRow(plan, mes, scheduleMap = {}, extras = {}) {
  const schedules = scheduleMap[plan.id] || []
  const { workshop, estimatedHours } = summarizeSchedules(schedules)
  const progress = computePlanProgress(plan, mes)
  const delayRisk = computeDelayRisk(plan, progress)
  const order = (mes?.orders || []).find((o) => o.id === plan.orderId || o.orderNo === plan.orderNo)
  const orderCtx = extras.orderContextMap?.[plan.orderId] || extras.orderContextMap?.[plan.orderNo]
  const validation = extras.conflictMap?.[plan.id]
  const conflicts = validation?.conflicts || []
  const kitRate = orderCtx ? computeKitRate(orderCtx.materialGaps) : null
  const equipmentLoad = computeEquipmentLoad(schedules, mes)
  const conflictCount = conflicts.length
  const schedulingRisk = aggregateSchedulingRisk(delayRisk, conflictCount, validation?.hasDanger)
  return {
    ...plan,
    schedules,
    workshop,
    estimatedHours,
    progress,
    delayRisk,
    deliveryDate: order?.deliveryDate || orderCtx?.deliveryDate || '—',
    kitRate,
    kitRateLabel: kitRate == null ? '—' : `${kitRate}%`,
    equipmentLoad,
    equipmentLoadLabel: equipmentLoad == null ? '—' : `${equipmentLoad}%`,
    conflictCount,
    conflicts,
    schedulingRisk,
    priorityLabel: priorityLabel(plan.priority),
    urgent: isUrgentPlan(plan)
  }
}

export function formatDisplayTime(val) {
  if (!val) return '—'
  return String(val).replace('T', ' ').slice(0, 16)
}

export function formatShortDate(val) {
  if (!val) return '—'
  const s = String(val).slice(0, 10)
  const parts = s.split('-')
  if (parts.length === 3) return `${parts[1]}/${parts[2]}`
  return s
}

function findStepDispatch(planId, stepName, mes) {
  const workOrders = (mes?.workOrders || []).filter((w) => w.planId === planId)
  for (const wo of workOrders) {
    const disp = (mes?.dispatches || []).find(
      (d) => d.workOrderId === wo.id && (d.processStep === stepName || d.stepName === stepName)
    )
    if (disp) return disp
  }
  return null
}

export function stepProgress(planId, stepName, mes) {
  const workOrders = (mes?.workOrders || []).filter((w) => w.planId === planId)
  if (!workOrders.length) return 0
  let total = 0
  let done = 0
  workOrders.forEach((wo) => {
    const related = (mes?.dispatches || []).filter(
      (d) => d.workOrderId === wo.id && (d.processStep === stepName || d.stepName === stepName)
    )
    related.forEach((d) => {
      total += Number(d.planQty) || 0
      done += Number(d.completedQty) || 0
    })
  })
  if (total > 0) return Math.min(100, Math.round((done / total) * 100))
  const plan = (mes?.plans || []).find((p) => p.id === planId)
  return plan?.status === '已完成' ? 100 : 0
}

export const GANTT_PLAN_COLORS = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
  '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#6b8fc7',
  '#5ba8a8', '#c9956a', '#6b9fd4', '#8fad94', '#b89a4a'
]

export function ganttPlanColor(planId) {
  const s = String(planId ?? '')
  let hash = 0
  for (let i = 0; i < s.length; i++) {
    hash = ((hash << 5) - hash) + s.charCodeAt(i)
    hash |= 0
  }
  return GANTT_PLAN_COLORS[Math.abs(hash) % GANTT_PLAN_COLORS.length]
}

export function ganttTaskStatus(plan, progress, scheduleEnd) {
  if (plan?.status === '已完成') return '已完成'
  const end = parseDateTime(scheduleEnd) || parseDate(plan?.planEnd)
  const now = new Date()
  if (end && end < now && progress < 100) return '延期'
  if (progress >= 100) return '已完成'
  if (progress > 0 || ['执行中', '已发布'].includes(plan?.status)) return '执行中'
  return '未开始'
}

function isAssemblyStep(name) {
  const n = String(name || '')
  return n.includes('整机组装') || n === '组装' || n.includes('背光组装')
}

function dedupeAssemblyRows(rows) {
  const seen = new Set()
  return rows.filter((row) => {
    if (!isAssemblyStep(row.stepName)) return true
    const key = `${row.planId}:${row.stepName}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

function buildGanttRow(plan, sch, mes, fallback = false) {
  const stepName = fallback ? '整体计划' : sch.stepName
  const progress = fallback ? computePlanProgress(plan, mes) : stepProgress(plan.id, stepName, mes)
  const dispatch = fallback ? null : findStepDispatch(plan.id, stepName, mes)
  const plannedStart = fallback ? plan.planStart : (sch.plannedStart || plan.planStart)
  const plannedEnd = fallback ? plan.planEnd : (sch.plannedEnd || plan.planEnd)
  return {
    planId: plan.id,
    orderNo: plan.orderNo || '—',
    productModel: plan.productModel || '—',
    planStatus: plan.status || '—',
    planQuantity: plan.quantity ?? '—',
    stepNo: fallback ? 0 : (sch.stepNo ?? 0),
    stepName,
    workshop: fallback ? '—' : (sch.workshop || '—'),
    equipment: fallback ? '—' : (sch.equipmentCode || '—'),
    plannedQuantity: fallback ? plan.quantity : (sch.plannedQuantity ?? plan.quantity),
    standardHours: fallback ? 0 : (Number(sch.standardHours) || 0),
    operator: dispatch?.operatorName || plan.planner || '—',
    dispatchStatus: dispatch?.status || '—',
    plannedStart,
    plannedEnd,
    startLabel: formatDisplayTime(plannedStart),
    endLabel: formatDisplayTime(plannedEnd),
    dateRangeLabel: `${formatShortDate(plannedStart)} ~ ${formatShortDate(plannedEnd)}`,
    progress,
    status: ganttTaskStatus(plan, progress, plannedEnd),
    planColor: ganttPlanColor(plan.id),
    urgent: isUrgentPlan(plan),
    priorityLabel: priorityLabel(plan.priority)
  }
}

const GANTT_SHIFT_HOURS = 16
const GANTT_DAY_MS = 86400000

function formatGanttDateTime(ms, hour = 8) {
  const d = new Date(ms)
  d.setHours(hour, 0, 0, 0)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:00:00`
}

function ganttStepWorkDays(row) {
  const hours = Number(row.standardHours) || 0.5
  const qty = Number(row.plannedQuantity) || 1
  return Math.max(1, Math.ceil((hours * qty) / GANTT_SHIFT_HOURS))
}

/** 按标准工时×数量重算甘特条时间跨度，避免前几道工序仅 1 天导致条带过窄 */
function applyGanttTimelineFromWorkHours(rows) {
  if (!rows.length) return rows
  const byPlan = new Map()
  rows.forEach((row, idx) => {
    if (!byPlan.has(row.planId)) byPlan.set(row.planId, [])
    byPlan.get(row.planId).push({ row, idx })
  })

  const adjusted = rows.map((r) => ({ ...r }))
  for (const items of byPlan.values()) {
    items.sort((a, b) => (a.row.stepNo ?? 0) - (b.row.stepNo ?? 0) || a.idx - b.idx)
    const planStartMs = items.reduce((min, { row }) => {
      const t = parseTs(row.plannedStart)
      return t && (!min || t < min) ? t : min
    }, null) || Date.now()

    let cursor = planStartMs
    for (const { row, idx } of items) {
      const days = ganttStepWorkDays(row)
      const startMs = cursor
      const endMs = startMs + (days - 1) * GANTT_DAY_MS
      const plannedStart = formatGanttDateTime(startMs, 8)
      const plannedEnd = formatGanttDateTime(endMs, 18)
      adjusted[idx] = {
        ...row,
        plannedStart,
        plannedEnd,
        startLabel: formatDisplayTime(plannedStart),
        endLabel: formatDisplayTime(plannedEnd),
        dateRangeLabel: `${formatShortDate(plannedStart)} ~ ${formatShortDate(plannedEnd)}`
      }
      cursor = startMs + days * GANTT_DAY_MS
    }
  }
  return adjusted
}

export function buildGanttRows(plans, scheduleMap, mes) {
  const rows = []
  plans.forEach((plan) => {
    const schedules = scheduleMap[plan.id] || []
    if (!schedules.length) return
    schedules
      .slice()
      .sort((a, b) => (a.stepNo ?? 0) - (b.stepNo ?? 0))
      .forEach((sch) => rows.push(buildGanttRow(plan, sch, mes, false)))
  })
  return applyGanttTimelineFromWorkHours(dedupeAssemblyRows(rows))
}

/** 甘特视图分组：plan | equipment | workshop */
export function regroupGanttRows(rows, viewMode = 'plan') {
  if (!rows?.length || viewMode === 'plan') return rows
  const sorted = [...rows]
  if (viewMode === 'equipment') {
    sorted.sort((a, b) => {
      const ea = a.equipment || '—'
      const eb = b.equipment || '—'
      if (ea !== eb) return ea.localeCompare(eb, 'zh-CN')
      return (parseTs(a.plannedStart) || 0) - (parseTs(b.plannedStart) || 0)
    })
    return sorted.map((r) => ({ ...r, groupLabel: r.equipment || '—', subLabel: r.planId }))
  }
  sorted.sort((a, b) => {
    const wa = a.workshop || '—'
    const wb = b.workshop || '—'
    if (wa !== wb) return wa.localeCompare(wb, 'zh-CN')
    return (parseTs(a.plannedStart) || 0) - (parseTs(b.plannedStart) || 0)
  })
  return sorted.map((r) => ({ ...r, groupLabel: r.workshop || '—', subLabel: r.planId }))
}

function parseTs(val) {
  if (!val) return null
  const s = String(val).replace(' ', 'T')
  const d = new Date(s.length === 10 ? `${s}T08:00:00` : s)
  return Number.isNaN(d.getTime()) ? null : d.getTime()
}

/** 设备时间重叠冲突检测 */
export function detectEquipmentConflicts(rows = []) {
  const conflictIdx = new Set()
  const byEquip = {}
  rows.forEach((row, idx) => {
    const eq = row.equipment
    if (!eq || eq === '—') return
    const start = parseTs(row.plannedStart)
    const end = parseTs(row.plannedEnd) || start
    if (!start) return
    if (!byEquip[eq]) byEquip[eq] = []
    byEquip[eq].forEach((other) => {
      const os = parseTs(other.row.plannedStart)
      const oe = parseTs(other.row.plannedEnd) || os
      if (os && start < oe && end > os) {
        conflictIdx.add(idx)
        conflictIdx.add(other.idx)
      }
    })
    byEquip[eq].push({ idx, row })
  })
  return conflictIdx
}

/** 同计划工序依赖连线 */
export function buildGanttDependencies(rows = []) {
  const deps = []
  const byPlan = {}
  rows.forEach((row, idx) => {
    if (!byPlan[row.planId]) byPlan[row.planId] = []
    byPlan[row.planId].push({ idx, stepNo: row.stepNo ?? 0, row })
  })
  Object.values(byPlan).forEach((items) => {
    items.sort((a, b) => a.stepNo - b.stepNo)
    for (let i = 1; i < items.length; i++) {
      deps.push({
        fromIdx: items[i - 1].idx,
        toIdx: items[i].idx,
        planId: items[i].row.planId
      })
    }
  })
  return deps
}

/** 产能负荷：按车间/设备聚合已排工时 */
export function buildCapacityLoad(scheduleMap = {}, mes = {}, plans = []) {
  const workshopMap = {}
  const equipmentMap = {}
  const shiftHours = 16

  plans.forEach((plan) => {
    const schedules = scheduleMap[plan.id] || []
    schedules.forEach((sch) => {
      const hours = Number(sch.standardHours) || 0
      const ws = sch.workshop || '未分配'
      const eq = sch.equipmentCode || '未分配'
      if (!workshopMap[ws]) workshopMap[ws] = { name: ws, scheduledHours: 0, planCount: 0, plans: new Set() }
      workshopMap[ws].scheduledHours += hours
      workshopMap[ws].plans.add(plan.id)
      if (!equipmentMap[eq]) equipmentMap[eq] = { name: eq, workshop: ws, scheduledHours: 0, planCount: 0, plans: new Set() }
      equipmentMap[eq].scheduledHours += hours
      equipmentMap[eq].plans.add(plan.id)
    })
  })

  const workshopRows = Object.values(workshopMap).map((w) => {
    const capacity = shiftHours * 5
    const load = capacity > 0 ? Math.min(100, Math.round((w.scheduledHours / capacity) * 100)) : 0
    return { ...w, planCount: w.plans.size, capacityHours: capacity, loadPct: load }
  }).sort((a, b) => b.loadPct - a.loadPct)

  const equipmentRows = Object.values(equipmentMap).map((e) => {
    const capacity = shiftHours * 5
    const load = capacity > 0 ? Math.min(100, Math.round((e.scheduledHours / capacity) * 100)) : 0
    const eqMeta = (mes?.equipment || []).find((x) => x.code === e.name || x.equipmentCode === e.name)
    return {
      ...e,
      planCount: e.plans.size,
      capacityHours: capacity,
      loadPct: load,
      status: eqMeta?.status || '—'
    }
  }).sort((a, b) => b.loadPct - a.loadPct)

  return { workshopRows, equipmentRows }
}

export { parseDate, parseDateTime }
