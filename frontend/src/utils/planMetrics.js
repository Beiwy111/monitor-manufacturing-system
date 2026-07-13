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

export function enrichPlanRow(plan, mes, scheduleMap = {}) {
  const schedules = scheduleMap[plan.id] || []
  const { workshop, estimatedHours } = summarizeSchedules(schedules)
  const progress = computePlanProgress(plan, mes)
  return {
    ...plan,
    schedules,
    workshop,
    estimatedHours,
    progress,
    delayRisk: computeDelayRisk(plan, progress),
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
    urgent: isUrgentPlan(plan),
    priorityLabel: priorityLabel(plan.priority)
  }
}

export function buildGanttRows(plans, scheduleMap, mes) {
  const rows = []
  plans.forEach((plan) => {
    const schedules = scheduleMap[plan.id] || []
    if (!schedules.length) {
      rows.push(buildGanttRow(plan, null, mes, true))
      return
    }
    schedules
      .slice()
      .sort((a, b) => (a.stepNo ?? 0) - (b.stepNo ?? 0))
      .forEach((sch) => rows.push(buildGanttRow(plan, sch, mes, false)))
  })
  return dedupeAssemblyRows(rows)
}

export { parseDate, parseDateTime }
