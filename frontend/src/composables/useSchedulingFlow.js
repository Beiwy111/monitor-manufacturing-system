import { ref } from 'vue'

const PLANNER_FLOW_TEMPLATE = [
  { key: 'order', title: '读取订单', section: null },
  { key: 'inventory', title: '核查库存', section: 'inventory' },
  { key: 'material', title: 'BOM齐套', section: 'inventory' },
  { key: 'equipment', title: '设备测算', section: 'capacity' },
  { key: 'operator', title: '人员评估', section: 'capacity' },
  { key: 'allocate', title: '资源分配', section: 'workshops' },
  { key: 'result', title: '生成方案', section: 'result' }
]

const DISPATCH_FLOW_TEMPLATE = [
  { key: 'route', title: '加载工艺', section: null },
  { key: 'equipment', title: '扫描设备', section: 'table' },
  { key: 'operator', title: '评估人员', section: 'table' },
  { key: 'match', title: '智能匹配', section: 'table' },
  { key: 'result', title: '生成推荐', section: 'result' }
]

const AGENT_AVATARS = {
  order: '订单分析员',
  inventory: '库存协调员',
  material: '物料计划员',
  equipment: '设备调度员',
  operator: '人员协调员',
  allocate: '产能优化员',
  result: '计划汇总员'
}

const DISPATCH_AGENTS = {
  route: '工艺工程师',
  equipment: '设备调度员',
  operator: '人员协调员',
  match: '派工优化员',
  result: '派工汇总员'
}

function agentNameForKey(key, template) {
  if (AGENT_AVATARS[key]) return AGENT_AVATARS[key]
  if (DISPATCH_AGENTS[key]) return DISPATCH_AGENTS[key]
  return template.find((t) => t.key === key)?.title || '排产 Agent'
}

function evidenceItem(id, source, tag, code, title, snippet, reliability, relatedSteps, metrics) {
  return {
    id, source, tag, code, title, snippet,
    reliability,
    relatedSteps: relatedSteps || [],
    metrics: metrics || {}
  }
}

/** 后端未返回 schedulingSteps 时，从分析结果拼装思维流 */
function buildPlannerStepsFromResult(result) {
  if (!result) return []
  const inv = result.inventoryCheck || {}
  const cap = result.capacityAnalysis || {}
  const orderNo = result.orderId || '—'
  const product = result.productModel || '—'
  const orderQty = result.orderQuantity ?? 0
  const fgStock = inv.finishedGoodsStock ?? 0
  const shipStock = inv.shipFromStock ?? 0
  const needProduce = inv.needToProduce ?? 0
  const recQty = result.recommendedPlanQty ?? inv.recommendedPlanQty ?? 0
  const mats = (inv.materialChecks || []).filter((m) => m.materialType !== 'FINISHED')
  const matLines = mats.map((m) =>
    `${m.materialName}：可用 ${m.available}，需求 ${m.requiredForPlan ?? m.required}，可支撑 ${m.maxSupportQty} 台${m.sufficient ? '' : '（不足）'}`
  )
  const workshops = result.workshops || []
  const wsLines = workshops.map((w) =>
    `${w.workshopName}：设备 ${w.requiredMachines} 台、人员 ${w.requiredOperators} 人`
  )

  return [
    {
      key: 'order', agentName: '订单分析员', actionType: '发现', badge: '发现',
      action: '读取 ERP 客户订单',
      summary: `「${orderNo}」读取订单：${product} 订购 ${orderQty} 台。`,
      thought: `【订单分析员】订单 ${orderNo}，产品「${product}」，订购 ${orderQty} 台，计划 ${result.planStart} ~ ${result.planEnd}（${result.workDays} 天）。`,
      detailLines: [`订单号：${orderNo}`, `产品：${product}`, `订购数量：${orderQty} 台`, `计划天数：${result.workDays} 天`],
      evidenceCount: 1, section: null
    },
    {
      key: 'inventory', agentName: '库存协调员', actionType: '执行', badge: '执行',
      action: '核查 WMS 成品库存',
      summary: `「成品仓」库存 ${fgStock} 台，可直发 ${shipStock} 台，缺口 ${needProduce} 台；累计证据库 2 条。`,
      thought: `【库存协调员】成品仓 ${fgStock} 台，可直发 ${shipStock} 台，需生产 ${needProduce} 台。`,
      detailLines: [`成品库存：${fgStock} 台`, `可现货发货：${shipStock} 台`, `需生产补足：${needProduce} 台`],
      evidenceCount: 2, section: 'inventory'
    },
    {
      key: 'material', agentName: '物料计划员', actionType: '执行', badge: '执行',
      action: '展开 BOM 做齐套分析',
      summary: `「BOM」核查 ${mats.length} 种物料，物料上限 ${inv.recommendedPlanQty ?? recQty} 台；累计证据库 ${2 + mats.length} 条。`,
      thought: `【物料计划员】${mats.length} 种原材料，瓶颈：${(inv.bottlenecks || []).join('、') || '无'}。`,
      detailLines: matLines.length ? matLines : ['无子项物料'],
      evidenceCount: 2 + mats.length, section: 'inventory'
    },
    {
      key: 'equipment', agentName: '设备调度员', actionType: '派遣', badge: '派遣',
      action: '统计设备资源并测算产能',
      summary: `「设备池」设备上限 ${cap.equipmentLimit ?? '-'} 台；累计证据库 3 条。`,
      thought: `【设备调度员】设备产能上限 ${cap.equipmentLimit ?? '-'} 台，分配设备 ${result.totalMachines ?? '-'} 台。`,
      detailLines: (cap.limits || []).slice(0, 4).length
        ? (cap.limits || []).slice(0, 4)
        : [`设备上限：${cap.equipmentLimit ?? '-'} 台`],
      evidenceCount: 3, section: 'capacity'
    },
    {
      key: 'operator', agentName: '人员协调员', actionType: '派遣', badge: '派遣',
      action: '统计操作员编制与负荷',
      summary: `「人员池」在岗 ${result.availableOperators ?? 0} 人，可行 ${cap.operatorLimit ?? '-'} 台；累计证据库 4 条。`,
      thought: `【人员协调员】在岗 ${result.availableOperators ?? 0} 人，人员可行产量 ${cap.operatorLimit ?? '-'} 台。`,
      detailLines: [`在岗操作员：${result.availableOperators ?? 0} 人`, `人员上限：${cap.operatorLimit ?? '-'} 台`],
      evidenceCount: 4, section: 'capacity'
    },
    {
      key: 'allocate', agentName: '产能优化员', actionType: '执行', badge: '执行',
      action: '分配车间设备与工序资源',
      summary: `「车间分配」${workshops.length} 个车间；累计证据库 5 条。`,
      thought: `【产能优化员】分配至 ${workshops.length} 个车间，设备 ${result.totalMachines} 台、人员 ${result.totalOperators} 人。`,
      detailLines: wsLines.length ? wsLines : ['暂无车间分配'],
      evidenceCount: 5, section: 'workshops'
    },
    {
      key: 'result', agentName: '计划汇总员', actionType: '发现', badge: '发现',
      action: '汇总约束输出生产计划',
      summary: `「排产结论」建议排产 ${recQty} 台；累计证据库 6 条。`,
      thought: `【计划汇总员】建议排产 ${recQty} 台。${result.recommendation || ''}`,
      detailLines: [
        `订单：${orderQty} 台`,
        `物料上限：${cap.materialLimit ?? '-'} 台`,
        `设备上限：${cap.equipmentLimit ?? '-'} 台`,
        `人员上限：${cap.operatorLimit ?? '-'} 台`,
        `建议排产：${recQty} 台`,
        result.recommendation || ''
      ].filter(Boolean),
      evidenceCount: 6, section: 'result'
    }
  ]
}

/** 后端未返回 evidenceBase 时，从分析结果拼装证据库 */
function buildPlannerEvidenceFromResult(result) {
  if (!result) return []
  const inv = result.inventoryCheck || {}
  const cap = result.capacityAnalysis || {}
  const orderNo = result.orderId || '—'
  const list = []

  list.push(evidenceItem('ev-order', 'ERP', '订单', orderNo, '客户订单主数据',
    `【订单数据】${orderNo} 订购 ${result.productModel} ${result.orderQuantity} 台。`,
    96, ['order'], {
      订单号: orderNo,
      产品型号: result.productModel,
      订购数量: `${result.orderQuantity} 台`,
      计划天数: `${result.workDays} 天`
    }))

  list.push(evidenceItem('ev-inv', 'WMS', '库存', 'FG-STOCK', '成品库存核查结果',
    `【库存数据】成品仓 ${inv.finishedGoodsStock ?? 0} 台，可发货 ${inv.shipFromStock ?? 0} 台，缺口 ${inv.needToProduce ?? 0} 台。`,
    92, ['inventory'], {
      成品可用库存: `${inv.finishedGoodsStock ?? 0} 台`,
      可现货发货: `${inv.shipFromStock ?? 0} 台`,
      需生产补足: `${inv.needToProduce ?? 0} 台`
    }))

  for (const mat of (inv.materialChecks || []).filter((m) => m.materialType !== 'FINISHED')) {
    list.push(evidenceItem(`ev-mat-${mat.materialCode}`, 'BOM', '物料', mat.materialCode, mat.materialName,
      `可用 ${mat.available}，可支撑 ${mat.maxSupportQty} 台${mat.sufficient ? '' : '（不足）'}`,
      88, ['material'], {
        物料编码: mat.materialCode,
        可用量: String(mat.available),
        可支撑产量: `${mat.maxSupportQty} 台`,
        是否充足: mat.sufficient ? '是' : '否'
      }))
  }

  list.push(evidenceItem('ev-eq', 'MES', '设备', 'EQ-POOL', '设备资源池',
    `【设备数据】设备上限 ${cap.equipmentLimit ?? '-'} 台，分配 ${result.totalMachines ?? '-'} 台。`,
    90, ['equipment', 'allocate'], {
      设备上限: `${cap.equipmentLimit ?? '-'} 台`,
      分配设备: `${result.totalMachines ?? '-'} 台`
    }))

  list.push(evidenceItem('ev-hr', 'HR', '人员', 'OP-POOL', '操作员编制',
    `【人员数据】在岗 ${result.availableOperators ?? 0} 人，可行产量 ${cap.operatorLimit ?? '-'} 台。`,
    87, ['operator', 'allocate'], {
      在岗操作员: `${result.availableOperators ?? 0} 人`,
      人员可行产量: `${cap.operatorLimit ?? '-'} 台`
    }))

  list.push(evidenceItem('ev-cap', 'APS', '结论', 'PLAN-RESULT', '排产结论',
    `【排产结论】建议排产 ${result.recommendedPlanQty ?? 0} 台。${result.recommendation || ''}`,
    95, ['result'], {
      建议排产量: `${result.recommendedPlanQty ?? 0} 台`,
      物料上限: `${cap.materialLimit ?? '-'} 台`,
      设备上限: `${cap.equipmentLimit ?? '-'} 台`,
      人员上限: `${cap.operatorLimit ?? '-'} 台`,
      综合可行产量: `${cap.feasibleQty ?? '-'} 台`
    }))

  return list
}

function buildDispatchStepsFromResult(result) {
  if (!result) return []
  const recs = result.recommendations || []
  const recLines = recs.map((r) =>
    `${r.processStep} → ${r.recommendedOperatorName} @ ${r.equipmentName}（${r.planQty} 台）`
  )
  return [
    {
      key: 'route', agentName: '工艺工程师', actionType: '发现', badge: '发现',
      action: '加载计划关联的工艺路线',
      summary: `「${result.planId}」排产 ${result.planQuantity ?? '-'} 台，${recs.length} 道工序。`,
      thought: `【工艺工程师】计划 ${result.planId}，产量 ${result.planQuantity} 台。`,
      detailLines: [`计划号：${result.planId}`, `计划数量：${result.planQuantity} 台`, `工序数：${recs.length} 道`],
      evidenceCount: 1, section: null
    },
    {
      key: 'equipment', agentName: '设备调度员', actionType: '派遣', badge: '派遣',
      action: '扫描设备状态并匹配工序',
      summary: '「设备池」按工序类型筛选可用机台；累计证据库 2 条。',
      thought: '【设备调度员】扫描全厂设备，按工序类型匹配。',
      detailLines: ['排除故障/维保机台', '按工序设备类型筛选'],
      evidenceCount: 2, section: 'table'
    },
    {
      key: 'operator', agentName: '人员协调员', actionType: '执行', badge: '执行',
      action: '评估操作员岗位与在途负荷',
      summary: '「人员池」统计在岗操作员；累计证据库 3 条。',
      thought: '【人员协调员】计算岗位匹配度与空闲度。',
      detailLines: recs.map((r) => `候选：${r.recommendedOperatorName}`).slice(0, 5),
      evidenceCount: 3, section: 'table'
    },
    {
      key: 'match', agentName: '派工优化员', actionType: '执行', badge: '执行',
      action: '逐道工序匹配负责人与设备',
      summary: `「智能匹配」完成 ${recs.length} 道工序；累计证据库 ${3 + recs.length} 条。`,
      thought: `【派工优化员】生成 ${recs.length} 条推荐。`,
      detailLines: recLines,
      evidenceCount: 3 + recs.length, section: 'table'
    },
    {
      key: 'result', agentName: '派工汇总员', actionType: '发现', badge: '发现',
      action: '输出派工方案',
      summary: `「派工结论」${recs.length} 道工序推荐完成；累计证据库 ${8 + recs.length} 条。`,
      thought: result.summary || '派工推荐完成。',
      detailLines: [result.summary || '确认后将生成工单并派工'],
      evidenceCount: 4 + recs.length, section: 'result'
    }
  ]
}

function buildDispatchEvidenceFromResult(result) {
  if (!result) return []
  const recs = result.recommendations || []
  const list = []
  list.push(evidenceItem('dv-plan', 'APS', '计划', result.planId, '生产计划数据',
    `计划 ${result.planId} 需生产 ${result.planQuantity} 台。`, 95, ['route'], {
      计划号: result.planId,
      计划数量: `${result.planQuantity} 台`,
      工序数: `${recs.length} 道`
    }))
  recs.forEach((r, i) => {
    list.push(evidenceItem(`dv-rec-${i}`, 'AI', '推荐', r.processStep, `${r.processStep} 派工推荐`,
      `${r.processStep} → ${r.recommendedOperatorName} @ ${r.equipmentName}，${r.recommendReason}`,
      86, ['match', 'result'], {
        工序: r.processStep,
        推荐人: r.recommendedOperatorName,
        设备: r.equipmentName,
        派工数量: `${r.planQty} 台`,
        推荐原因: r.recommendReason
      }))
  })
  return list
}

function resolveFlowData(result, template) {
  let steps = result?.schedulingSteps
  let evidence = result?.evidenceBase
  if (!steps?.length) {
    steps = template === DISPATCH_FLOW_TEMPLATE
      ? buildDispatchStepsFromResult(result)
      : buildPlannerStepsFromResult(result)
  }
  if (!evidence?.length) {
    evidence = template === DISPATCH_FLOW_TEMPLATE
      ? buildDispatchEvidenceFromResult(result)
      : buildPlannerEvidenceFromResult(result)
  }
  return { steps, evidence }
}

function mergeSteps(template, serverSteps) {
  if (!serverSteps?.length) {
    return template.map((s) => ({
      ...s,
      agentName: agentNameForKey(s.key, template),
      actionType: '执行',
      badge: '执行',
      title: s.title,
      action: s.title,
      summary: '分析中…',
      thought: '分析中…',
      detail: '分析中…',
      detailLines: [],
      evidenceCount: 0
    }))
  }
  return serverSteps.map((s, i) => ({
    key: s.key || template[i]?.key || `step-${i}`,
    title: s.title || s.action || template[i]?.title || `步骤 ${i + 1}`,
    action: s.action || s.title || template[i]?.title || '',
    summary: s.summary || s.thought || s.detail || '',
    detail: s.detail || s.thought || '',
    detailLines: s.detailLines || [],
    evidenceCount: s.evidenceCount ?? 0,
    agentName: s.agentName || agentNameForKey(s.key || template[i]?.key, template),
    actionType: s.actionType || '执行',
    badge: s.badge || s.actionType || '执行',
    thought: s.thought || s.detail || s.summary || '',
    section: template.find((t) => t.key === s.key)?.section
      ?? template[i]?.section
      ?? null
  }))
}

function appendEvidenceForStep(allEvidence, revealed, stepKey) {
  const related = allEvidence.filter((ev) => (ev.relatedSteps || []).includes(stepKey))
  const merged = [...revealed]
  for (const ev of related) {
    if (!merged.some((e) => e.id === ev.id)) {
      merged.push(ev)
    }
  }
  return merged.length ? merged : revealed
}

export function useSchedulingFlow(template = PLANNER_FLOW_TEMPLATE) {
  const activeStep = ref(-1)
  const activeStepKey = ref('')
  const flowSteps = ref([])
  const thoughtStream = ref([])
  const evidenceList = ref([])
  const allEvidence = ref([])
  const currentDetail = ref('')
  const visibleSections = ref(new Set())
  const isRunning = ref(false)
  const selectedStepKey = ref('')

  let timers = []

  function clearTimers() {
    timers.forEach(clearTimeout)
    timers = []
  }

  function reset() {
    clearTimers()
    activeStep.value = -1
    activeStepKey.value = ''
    selectedStepKey.value = ''
    flowSteps.value = []
    thoughtStream.value = []
    evidenceList.value = []
    allEvidence.value = []
    currentDetail.value = ''
    visibleSections.value = new Set()
    isRunning.value = false
  }

  function revealSection(section) {
    if (!section) return
    const next = new Set(visibleSections.value)
    next.add(section)
    visibleSections.value = next
  }

  function animateThoughtStream(steps, evidence, stepMs = 850) {
    return new Promise((resolve) => {
      thoughtStream.value = []
      evidenceList.value = []
      let i = 0
      const reveal = () => {
        if (i >= steps.length) {
          evidenceList.value = [...evidence]
          resolve()
          return
        }
        const step = steps[i]
        thoughtStream.value = [...thoughtStream.value, step]
        activeStep.value = i + 1
        activeStepKey.value = step.key
        selectedStepKey.value = step.key
        currentDetail.value = step.summary || step.thought || step.detail
        evidenceList.value = appendEvidenceForStep(evidence, evidenceList.value, step.key)
        revealSection(step.section)
        i += 1
        timers.push(setTimeout(reveal, stepMs))
      }
      reveal()
    })
  }

  function finishWithResult(result) {
    const { steps: rawSteps, evidence } = resolveFlowData(result, template)
    const serverSteps = mergeSteps(template, rawSteps)
    flowSteps.value = serverSteps
    allEvidence.value = evidence
    evidenceList.value = evidence
    activeStep.value = flowSteps.value.length
    const last = flowSteps.value[flowSteps.value.length - 1]
    activeStepKey.value = last?.key || 'result'
    selectedStepKey.value = last?.key || 'result'
    currentDetail.value = last?.summary || last?.thought || last?.detail || result?.summary || '排产分析完成'
    thoughtStream.value = [...flowSteps.value]
    template.forEach((s) => revealSection(s.section))
    revealSection('result')
  }

  async function runAnimatedPreview(loadFn, { stepMs = 850 } = {}) {
    clearTimers()
    isRunning.value = true
    activeStep.value = 0
    activeStepKey.value = ''
    selectedStepKey.value = ''
    visibleSections.value = new Set()
    thoughtStream.value = []
    evidenceList.value = []
    allEvidence.value = []
    flowSteps.value = mergeSteps(template, null)
    currentDetail.value = template === DISPATCH_FLOW_TEMPLATE
      ? '正在启动智能派工引擎，接入工艺路线、设备状态与人员编制…'
      : '正在启动智能排产引擎，接入订单、库存、设备与人员数据…'

    try {
      const result = await loadFn()
      clearTimers()
      const { steps: rawSteps, evidence } = resolveFlowData(result, template)
      const steps = mergeSteps(template, rawSteps)
      flowSteps.value = steps
      allEvidence.value = evidence
      await animateThoughtStream(steps, evidence, stepMs)
      finishWithResult(result)
      return result
    } catch (e) {
      clearTimers()
      currentDetail.value = e?.message || '排产分析失败'
      throw e
    } finally {
      isRunning.value = false
    }
  }

  function showCompletedResult(result) {
    finishWithResult(result)
  }

  function selectStep(key) {
    selectedStepKey.value = key
    activeStepKey.value = key
  }

  return {
    activeStep,
    activeStepKey,
    selectedStepKey,
    flowSteps,
    thoughtStream,
    evidenceList,
    allEvidence,
    currentDetail,
    visibleSections,
    isRunning,
    reset,
    runAnimatedPreview,
    showCompletedResult,
    revealSection,
    selectStep
  }
}

export { PLANNER_FLOW_TEMPLATE, DISPATCH_FLOW_TEMPLATE }
