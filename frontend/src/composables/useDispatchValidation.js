/**
 * 派工 Agent 分析 — 右侧「派工实时校验」模型（基于 previewSmartDispatch 真实数据）
 */

export const DISPATCH_STEP_ORDER = ['route', 'equipment', 'operator', 'match', 'result']

/** 各分析步骤解锁的校验项 */
export const DISPATCH_REVEAL_BY_STEP = {
  route: ['progress'],
  equipment: ['equipment_status'],
  operator: ['operator_skill', 'operator_load'],
  match: ['duplicate_dispatch', 'equipment_occupancy', 'process_order'],
  result: ['workshop_load', 'delivery_feasibility', 'conclusion']
}

const AUTO_FIX_MAP = {
  operator_duplicate: { fix: '更换重复派工的操作员或拆分工序时段', impact: '不处理将无法生成工单' },
  operator_busy: { fix: '改派同岗位空闲人员或调整工序顺序', impact: '当前人员无法并行接单' },
  operator_queued: { fix: '按队列自动追加，待前序派工接收后执行', impact: '预计顺延 0.5~1 班次' },
  no_operator: { fix: '手动指定岗位匹配的操作员', impact: '阻塞工单生成' },
  equipment_duplicate: { fix: '为重复设备分配不同机台或拆分批次', impact: '设备无法同时执行两道工序' },
  equipment_fault: { fix: '切换同类型可用设备', impact: '故障设备不可派工' },
  equipment_busy: { fix: '自动推荐同类型空闲机台', impact: '可能增加等待时间' },
  process_order: { fix: '按工艺路线 stepNo 重新排序', impact: '工序倒置将影响生产追溯' },
  workshop_overload: { fix: '拆批或分流至并行车间', impact: '车间产能紧张，交期风险上升' },
  delivery_tight: { fix: '优先空闲人员/设备或申请加急', impact: '存在延期风险' }
}

function conflictsByCode(conflicts, codes) {
  return (conflicts || []).filter((c) => codes.includes(c.code))
}

function worstStatus(items) {
  if (items.some((i) => i.status === 'conflict')) return 'conflict'
  if (items.some((i) => i.status === 'warn')) return 'warn'
  if (items.every((i) => i.status === 'pass' || i.status === 'pending')) {
    return items.some((i) => i.status === 'pass') ? 'pass' : 'pending'
  }
  return 'pass'
}

function mapConflictLevel(level) {
  if (level === 'danger') return 'conflict'
  if (level === 'warning') return 'warn'
  return 'pass'
}

function diffPlanDays(plan) {
  if (!plan?.planStart || !plan?.planEnd) return null
  const s = new Date(String(plan.planStart).slice(0, 10))
  const e = new Date(String(plan.planEnd).slice(0, 10))
  if (Number.isNaN(s.getTime()) || Number.isNaN(e.getTime())) return null
  return Math.max(1, Math.round((e - s) / 86400000) + 1)
}

function buildEquipmentCheck(preview, mes, conflicts) {
  const equipment = mes?.equipment || []
  const total = equipment.length
  const idle = equipment.filter((e) => {
    const st = String(e.status || '').toUpperCase()
    return st === 'IDLE' || st === '空闲'
  }).length
  const fault = equipment.filter((e) => {
    const st = String(e.status || '').toUpperCase()
    return st === 'FAULT' || st === 'MAINTENANCE' || st === '故障' || st === '维保'
  }).length

  const hit = conflictsByCode(conflicts, ['equipment_fault', 'equipment_busy', 'equipment_duplicate'])
  let status = 'pass'
  let summary = `共 ${total} 台，空闲 ${idle} 台`
  if (hit.some((c) => c.level === 'danger')) status = 'conflict'
  else if (hit.length || fault > 0) status = hit.length ? 'warn' : (fault > 0 ? 'warn' : 'pass')
  if (fault > 0 && status === 'pass') {
    summary += `，${fault} 台故障/维保`
    status = 'warn'
  }
  const primary = hit[0]
  return {
    id: 'equipment_status',
    label: '设备状态',
    status,
    summary,
    conflictReason: primary?.detail,
    autoFix: primary ? AUTO_FIX_MAP[primary.code]?.fix : '',
    impact: primary ? AUTO_FIX_MAP[primary.code]?.impact : ''
  }
}

function buildOperatorSkillCheck(recommendations, conflicts) {
  const recs = recommendations || []
  const mismatches = recs.filter((r) => {
    if (!r.recommendedOperator) return true
    return false
  })
  const hit = conflictsByCode(conflicts, ['no_operator'])
  let status = 'pass'
  let summary = `已匹配 ${recs.length} 道工序岗位`
  if (hit.length || mismatches.length) {
    status = hit.length ? 'conflict' : 'warn'
    summary = hit[0]?.detail || `${mismatches.length} 道工序待指定操作员`
  } else if (recs.every((r) => r.recommendReason)) {
    summary = recs.map((r) => r.processStep).slice(0, 2).join('、') + (recs.length > 2 ? ' 等岗位匹配' : ' 岗位匹配')
  }
  const primary = hit[0]
  return {
    id: 'operator_skill',
    label: '人员技能',
    status,
    summary,
    conflictReason: primary?.detail,
    autoFix: primary ? AUTO_FIX_MAP[primary.code]?.fix : '',
    impact: primary ? AUTO_FIX_MAP[primary.code]?.impact : ''
  }
}

function buildOperatorLoadCheck(conflicts) {
  const hit = conflictsByCode(conflicts, ['operator_busy', 'operator_queued', 'operator_duplicate'])
  let status = 'pass'
  let summary = '无人员重复派工'
  if (hit.some((c) => c.level === 'danger')) status = 'conflict'
  else if (hit.length) status = 'warn'
  if (hit.length) summary = hit.map((c) => c.detail).join('；')
  const primary = hit.find((c) => c.level === 'danger') || hit[0]
  return {
    id: 'operator_load',
    label: '人员负荷',
    status,
    summary,
    conflictReason: primary?.detail,
    autoFix: primary ? AUTO_FIX_MAP[primary.code]?.fix : '',
    impact: primary ? AUTO_FIX_MAP[primary.code]?.impact : ''
  }
}

function buildDuplicateCheck(conflicts) {
  const hit = conflictsByCode(conflicts, ['operator_duplicate', 'equipment_duplicate'])
  const status = hit.some((c) => c.level === 'danger') ? 'conflict' : hit.length ? 'warn' : 'pass'
  const primary = hit[0]
  return {
    id: 'duplicate_dispatch',
    label: '重复派工',
    status,
    summary: hit.length ? hit.map((c) => c.detail).join('；') : '人员与设备无重复分配',
    conflictReason: primary?.detail,
    autoFix: primary ? AUTO_FIX_MAP[primary.code]?.fix : '',
    impact: primary ? AUTO_FIX_MAP[primary.code]?.impact : ''
  }
}

function buildEquipmentOccupancyCheck(conflicts) {
  const hit = conflictsByCode(conflicts, ['equipment_busy', 'equipment_fault'])
  const status = hit.some((c) => c.level === 'danger') ? 'conflict' : hit.length ? 'warn' : 'pass'
  const primary = hit[0]
  return {
    id: 'equipment_occupancy',
    label: '设备占用',
    status,
    summary: hit.length ? hit.map((c) => c.detail).join('；') : '推荐设备均可调度',
    conflictReason: primary?.detail,
    autoFix: primary ? AUTO_FIX_MAP[primary.code]?.fix : '',
    impact: primary ? AUTO_FIX_MAP[primary.code]?.impact : ''
  }
}

function buildProcessOrderCheck(conflicts) {
  const hit = conflictsByCode(conflicts, ['process_order'])
  const status = hit.length ? 'conflict' : 'pass'
  const primary = hit[0]
  return {
    id: 'process_order',
    label: '工序顺序',
    status,
    summary: hit.length ? primary.detail : '符合工艺路线顺序',
    conflictReason: primary?.detail,
    autoFix: primary ? AUTO_FIX_MAP.process_order?.fix : '',
    impact: primary ? AUTO_FIX_MAP.process_order?.impact : ''
  }
}

function buildWorkshopLoadCheck(recommendations, conflicts) {
  const hit = conflictsByCode(conflicts, ['workshop_overload'])
  const status = hit.length ? 'warn' : 'pass'
  const workshops = [...new Set((recommendations || []).map((r) => r.workshopName).filter(Boolean))]
  const primary = hit[0]
  return {
    id: 'workshop_load',
    label: '车间负载',
    status,
    summary: hit.length ? primary.detail : `涉及 ${workshops.length || '—'} 个车间，负载正常`,
    conflictReason: primary?.detail,
    autoFix: primary ? AUTO_FIX_MAP.workshop_overload?.fix : '',
    impact: primary ? AUTO_FIX_MAP.workshop_overload?.impact : ''
  }
}

function buildDeliveryCheck(preview, mes, recommendations) {
  const plan = (mes?.plans || []).find((p) => p.id === preview?.planId)
  const days = diffPlanDays(plan)
  const totalHours = (recommendations || []).reduce((s, r) => s + (Number(r.estimatedHours) || 0), 0)
  if (!days) {
    return {
      id: 'delivery_feasibility',
      label: '交期可行性',
      status: 'pass',
      summary: `预计总工时 ${totalHours.toFixed(1)} h`
    }
  }
  const windowHours = days * 16
  let status = 'pass'
  let summary = `预计 ${totalHours.toFixed(1)} h / 计划窗口 ${windowHours} h（${days} 天）`
  if (totalHours > windowHours * 1.15) {
    status = 'warn'
    summary = `工时 ${totalHours.toFixed(1)} h 超出计划窗口 ${windowHours} h`
  }
  if (totalHours > windowHours * 1.35) status = 'conflict'
  return {
    id: 'delivery_feasibility',
    label: '交期可行性',
    status,
    summary,
    conflictReason: status !== 'pass' ? summary : '',
    autoFix: status !== 'pass' ? AUTO_FIX_MAP.delivery_tight?.fix : '',
    impact: status !== 'pass' ? AUTO_FIX_MAP.delivery_tight?.impact : ''
  }
}

function buildConclusion(preview, checks) {
  const validation = preview?.validation || {}
  const hasDanger = validation.hasDanger || checks.some((c) => c.status === 'conflict')
  const hasWarn = checks.some((c) => c.status === 'warn')
  let risk = '低风险'
  if (hasDanger) risk = '高风险'
  else if (hasWarn) risk = '中风险'

  const actionable = validation.canSubmit !== false && !hasDanger
  return {
    canGenerate: actionable,
    risk,
    label: actionable ? '可生成工单' : '需处理冲突后生成',
    detail: preview?.summary || (actionable ? '校验通过，可确认生成工单' : '存在阻塞项，请调整派工方案')
  }
}

export function getRevealedDispatchIds(revealedStepKeys) {
  const set = new Set()
  for (const key of revealedStepKeys) {
    for (const id of DISPATCH_REVEAL_BY_STEP[key] || []) {
      set.add(id)
    }
  }
  return set
}

export function buildDispatchValidationModel(preview, mes, options = {}) {
  const { activeStepKey = '', revealedStepKeys = [], running = false } = options
  const conflicts = preview?.validation?.conflicts || []
  const recommendations = preview?.recommendations || []

  const checks = [
    buildEquipmentCheck(preview, mes, conflicts),
    buildOperatorSkillCheck(recommendations, conflicts),
    buildOperatorLoadCheck(conflicts),
    buildDuplicateCheck(conflicts),
    buildEquipmentOccupancyCheck(conflicts),
    buildProcessOrderCheck(conflicts),
    buildWorkshopLoadCheck(recommendations, conflicts),
    buildDeliveryCheck(preview, mes, recommendations)
  ]

  const revealed = getRevealedDispatchIds(revealedStepKeys)
  const stepIdx = DISPATCH_STEP_ORDER.indexOf(activeStepKey)
  const progress = running
    ? Math.min(95, Math.round(((stepIdx + 1) / DISPATCH_STEP_ORDER.length) * 100))
    : (preview ? 100 : 0)

  const currentStep = DISPATCH_STEP_ORDER[Math.max(0, stepIdx)] || 'route'
  const stepLabels = {
    route: '加载工艺路线',
    equipment: '扫描设备状态',
    operator: '评估人员技能',
    match: '逐道匹配派工',
    result: '汇总派工方案'
  }

  const visibleChecks = checks.map((c) => ({
    ...c,
    pending: running ? !revealed.has(c.id) : false
  }))

  const activeChecks = visibleChecks.filter((c) => !c.pending)
  const conclusion = buildConclusion(preview, activeChecks)

  const currentProcess = (() => {
    if (currentStep === 'match' || currentStep === 'result') {
      const idx = Math.min(recommendations.length - 1, Math.max(0, revealedStepKeys.length - 4))
      const rec = recommendations[idx]
      if (rec) return rec.processStep
    }
    if (currentStep === 'route') return '工艺路线'
    if (currentStep === 'equipment') return '设备池'
    if (currentStep === 'operator') return '人员池'
    return stepLabels[currentStep] || '—'
  })()

  return {
    progress,
    currentStep,
    currentStepLabel: stepLabels[currentStep] || '分析中',
    currentProcess,
    checks: visibleChecks,
    conclusion: revealed.has('conclusion') || !running ? conclusion : null
  }
}

export function statusLabel(status) {
  if (status === 'conflict') return '冲突'
  if (status === 'warn') return '警告'
  if (status === 'pass') return '通过'
  return '待校验'
}
