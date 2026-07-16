export const CASE_STATUS_OPTIONS = [
  { value: 'OPEN', label: '待受理' },
  { value: 'ACCEPTED', label: '已受理' },
  { value: 'PENDING_PLAN', label: '待方案' },
  { value: 'PENDING_APPROVAL', label: '待审批' },
  { value: 'EXECUTING', label: '执行中' },
  { value: 'PENDING_RECHECK', label: '待复检' },
  { value: 'PENDING_CONFIRM', label: '待客户确认' },
  { value: 'RESOLVED', label: '已解决' },
  { value: 'CLOSED', label: '已关闭' }
]

export const PLAN_TYPE_OPTIONS = [
  { value: 'REMOTE_GUIDE', label: '远程指导' },
  { value: 'RETURN_INSPECTION', label: '返厂检测' },
  { value: 'REPAIR', label: '维修' },
  { value: 'EXCHANGE', label: '换货' },
  { value: 'RETURN', label: '退货' },
  { value: 'PARTS_RESUPPLY', label: '补发配件' },
  { value: 'SUPPLIER_CLAIM', label: '供应商索赔' },
  { value: 'BATCH_RECALL', label: '批次召回' }
]

export const PROBLEM_TYPES = [
  { value: 'DISPLAY_DEFECT', label: '显示缺陷' },
  { value: 'COLOR_ISSUE', label: '色彩问题' },
  { value: 'DEAD_PIXEL', label: '坏点/亮点' },
  { value: 'INTERFACE_FAULT', label: '接口故障' },
  { value: 'APPEARANCE', label: '外观损伤' },
  { value: 'POWER_ISSUE', label: '电源问题' },
  { value: 'OTHER', label: '其他' }
]

export function nextStepHint(status) {
  const map = {
    OPEN: '请在调查工作台完成 AI 分诊后一键受理',
    ACCEPTED: '请前往「方案审批」制定处置方案',
    PENDING_PLAN: '请完善方案并提交审批',
    PENDING_APPROVAL: '等待方案审批通过',
    EXECUTING: '请在「执行协同」跟进任务进度',
    PENDING_RECHECK: '请在「验证闭环」完成复检',
    PENDING_CONFIRM: '等待客户确认满意度',
    RESOLVED: '请在「验证闭环」归档并关闭案例',
    CLOSED: '案例已关闭'
  }
  return map[status] || ''
}

export function slaTagType(deadline) {
  if (!deadline) return 'info'
  return new Date(deadline) < new Date() ? 'danger' : 'success'
}

const DEPT_LABELS = {
  PURCHASE: '采购', DEVICE: '设备', QUALITY: '质量', COST: '财务',
  PRODUCTION: '生产', AFTERSALE: '售后', ORDER: '订单'
}

export function traceSummaryStorageKey(caseNo) {
  return `aftersale_trace_summary_${caseNo}`
}

/** 将调查工作台追溯/RCA 结论整理为方案「追溯摘要」文本 */
export function buildRcaTraceSummary(analysis, traceChain = []) {
  if (!analysis) return ''
  const parts = []
  if (analysis.conclusion) parts.push(`追溯结论：${analysis.conclusion}`)
  const top = analysis.causes?.[0]
  if (top?.name) {
    const dept = DEPT_LABELS[top.department] || top.department || ''
    parts.push(`主要根因：${top.name}${dept ? `（${dept}）` : ''}${top.score != null ? `，置信度 ${top.score}%` : ''}`)
  }
  const nodes = (analysis.nodes || []).slice(0, 4)
    .map((n) => `${n.name}：${n.summary || n.code || ''}`)
    .filter(Boolean)
  if (nodes.length) parts.push(`关键证据：${nodes.join('；')}`)
  const chain = (traceChain || []).filter((s) => !s.missing).map((s) => s.title).join(' → ')
  if (chain) parts.push(`追溯链路：${chain}`)
  return parts.join('\n')
}

export function stashTraceSummary(caseNo, summary) {
  if (!caseNo || !summary) return
  sessionStorage.setItem(traceSummaryStorageKey(caseNo), summary)
}

export function popTraceSummary(caseNo) {
  if (!caseNo) return ''
  const key = traceSummaryStorageKey(caseNo)
  const text = sessionStorage.getItem(key) || ''
  if (text) sessionStorage.removeItem(key)
  return text
}

const CAUSE_STEP_TYPES = {
  PURCHASE: ['material', 'material_quality', 'supplier'],
  DEVICE: ['production'],
  QUALITY: ['quality'],
  PRODUCTION: ['production']
}

const RISK_LEVEL_LABEL = { HIGH: '高风险', MEDIUM: '中风险', LOW: '低风险', URGENT: '紧急' }

/** 追溯动画结束后生成报告：指出问题最可能出现在哪个环节 */
export function buildTraceReport(analysis, traceChain = []) {
  if (!analysis) return null
  const causes = [...(analysis.causes || [])].sort((a, b) => (b.score || 0) - (a.score || 0))
  const top = causes[0]
  if (!top) return null

  const matchedTypes = CAUSE_STEP_TYPES[top.department] || []
  const steps = (traceChain || []).map((step, index) => {
    let level = null
    let reason = ''
    if (step.missing) {
      level = 'gap'
      reason = '系统无留痕，该环节无法完整核验'
    }
    if (matchedTypes.includes(step.type)) {
      level = level === 'gap' ? 'primary-gap' : 'primary'
      reason = `RCA 嫌疑分 ${top.score}/100（${DEPT_LABELS[top.department] || top.department}）${step.missing ? '，但缺少业务留痕' : ''}`
    } else {
      for (const c of causes.slice(1)) {
        if ((c.score || 0) >= 40 && (CAUSE_STEP_TYPES[c.department] || []).includes(step.type)) {
          if (!level || level === 'gap') {
            level = 'secondary'
            reason = `次要嫌疑：${c.name}（${c.score}分）`
          }
        }
      }
    }
    return {
      index,
      title: step.title,
      no: step.no,
      type: step.type,
      missing: !!step.missing,
      level,
      reason
    }
  })

  const primaryHits = steps.filter((s) => s.level === 'primary' || s.level === 'primary-gap')
  const leadStep = primaryHits.find((s) => !s.missing) || primaryHits[0] || steps.find((s) => matchedTypes.includes(s.type))

  return {
    headline: leadStep
      ? `问题最可能出现在：${leadStep.title}`
      : `问题最可能关联：${top.name}`,
    subHeadline: leadStep?.missing
      ? '（该环节无系统留痕，需人工补录或现场核查）'
      : leadStep
        ? `业务编号：${leadStep.no}`
        : '',
    caseNo: analysis.caseNo,
    analyzedAt: analysis.analyzedAt,
    primaryCause: {
      name: top.name,
      department: top.department,
      departmentLabel: DEPT_LABELS[top.department] || top.department,
      score: top.score,
      riskLevel: RISK_LEVEL_LABEL[top.riskLevel] || top.riskLevel || '',
      evidence: top.evidence || ''
    },
    conclusion: analysis.conclusion || '',
    suspectSteps: steps.filter((s) => s.level && s.level !== 'gap'),
    dataGaps: steps.filter((s) => s.missing).map((s) => s.title),
    allSteps: steps,
    recommendations: (analysis.actions || []).slice(0, 4).map((a) => ({
      title: a.title,
      department: DEPT_LABELS[a.department] || a.department
    }))
  }
}
