/**
 * 从排产分析 API 结果构建「排产计算引擎」展示模型（与 MesPlannerAgentService 公式一致）
 */

const STEP_ORDER = ['order', 'inventory', 'material', 'equipment', 'operator', 'allocate', 'result']

/** 各分析步骤解锁的计算区块 */
export const CALC_REVEAL_BY_STEP = {
  order: ['inputOrder', 'inputCycle'],
  inventory: ['inputStock', 'calcGap'],
  material: ['inputMaterial', 'calcMaterial'],
  equipment: ['inputEquipment', 'calcEquipment'],
  operator: ['inputOperator', 'calcOperator'],
  allocate: ['calcAllocate'],
  result: ['calcConstraint', 'output']
}

function num(v, fallback = 0) {
  const n = Number(v)
  return Number.isFinite(n) ? n : fallback
}

function rawMaterialLimit(inv, needProduce) {
  const mats = (inv.materialChecks || []).filter((m) => m.materialType !== 'FINISHED')
  if (!mats.length) {
    return needProduce > 0 ? needProduce : 0
  }
  const supports = mats.map((m) => num(m.maxSupportQty))
  if (!supports.length) return 0
  return Math.min(...supports)
}

function bottleneckLabels(needProduce, materialLimit, equipmentLimit, operatorLimit, recommended) {
  if (needProduce === 0 && recommended === 0) return ['无生产缺口']
  const items = [
    { label: '生产缺口', value: needProduce },
    { label: '物料', value: materialLimit },
    { label: '设备', value: equipmentLimit },
    { label: '人员', value: operatorLimit }
  ]
  const binding = items.filter((i) => i.value === recommended)
  const withoutGap = binding.filter((i) => i.label !== '生产缺口')
  if (withoutGap.length) return withoutGap.map((i) => i.label)
  if (binding.length) return binding.map((i) => i.label)
  const minVal = Math.min(needProduce, materialLimit, equipmentLimit, operatorLimit)
  return items.filter((i) => i.value === minVal).map((i) => i.label)
}

export function buildCalcEngineModel(analysis) {
  if (!analysis) return null

  const inv = analysis.inventoryCheck || {}
  const cap = analysis.capacityAnalysis || {}

  const orderQty = num(analysis.orderQuantity ?? inv.orderQuantity)
  const fgStock = num(inv.finishedGoodsStock)
  const shipStock = num(inv.shipFromStock)
  const needProduce = num(inv.needToProduce, Math.max(0, orderQty - shipStock))
  const materialLimit = rawMaterialLimit(inv, needProduce)
  const equipmentLimit = num(cap.equipmentLimit)
  const operatorLimit = num(cap.operatorLimit)
  const workDays = Math.max(1, num(analysis.workDays, 1))
  const recommended = num(analysis.recommendedPlanQty)
  const totalDelivery = shipStock + recommended

  const mats = (inv.materialChecks || []).filter((m) => m.materialType !== 'FINISHED')
  const bottleneckMat = mats.length
    ? mats.reduce((a, b) => (num(a.maxSupportQty) <= num(b.maxSupportQty) ? a : b))
    : null

  const equipmentLimits = cap.limits || []
  const bottleneckEq = equipmentLimits.length ? equipmentLimits[0] : null

  const calcSteps = []

  calcSteps.push({
    id: 'calcGap',
    stepKey: 'inventory',
    label: '生产缺口',
    formula: '生产缺口 = 订单需求量 − 可现货发货',
    substitution: `${orderQty} − ${shipStock}`,
    result: needProduce,
    unit: '台'
  })

  if (mats.length) {
    const perUnit = bottleneckMat?.requiredPerUnit ?? bottleneckMat?.required
    calcSteps.push({
      id: 'calcMaterial',
      stepKey: 'material',
      label: '物料上限',
      formula: '物料上限 = min(各子项 floor(可用量 ÷ 单位用量))',
      substitution: bottleneckMat
        ? `${bottleneckMat.materialName}：floor(${bottleneckMat.available} ÷ ${perUnit ?? '—'})`
        : `min(${mats.map((m) => m.maxSupportQty).join(', ')})`,
      result: materialLimit,
      unit: '台',
      detail: mats.length > 1 ? `瓶颈物料：${bottleneckMat?.materialName || '—'}` : undefined
    })
  } else if (needProduce > 0) {
    calcSteps.push({
      id: 'calcMaterial',
      stepKey: 'material',
      label: '物料上限',
      formula: '无 BOM 子项，物料上限 = 生产缺口',
      substitution: String(needProduce),
      result: materialLimit,
      unit: '台'
    })
  }

  calcSteps.push({
    id: 'calcEquipment',
    stepKey: 'equipment',
    label: '设备上限',
    formula: '设备上限 = min(各工序 floor(可用机台 × 16h ÷ 标准工时 × 计划天数))',
    substitution: bottleneckEq || `瓶颈工序产能 ${equipmentLimit}`,
    result: equipmentLimit,
    unit: '台'
  })

  calcSteps.push({
    id: 'calcOperator',
    stepKey: 'operator',
    label: '人员上限',
    formula: '人员上限 = 设备+人员约束下可行产量（向下搜索）',
    substitution: `在岗 ${num(cap.availableOperators ?? analysis.availableOperators)} 人 → 可行 ${operatorLimit}`,
    result: operatorLimit,
    unit: '台'
  })

  if ((analysis.workshops || []).length) {
    calcSteps.push({
      id: 'calcAllocate',
      stepKey: 'allocate',
      label: '日均目标',
      formula: '日均目标 = 建议排产量 ÷ 计划天数',
      substitution: recommended > 0 ? `${recommended} ÷ ${workDays}` : `— ÷ ${workDays}`,
      result: recommended > 0 ? Math.round((recommended / workDays) * 10) / 10 : 0,
      unit: '台/天'
    })
  }

  const constraintParts = [
    `生产缺口 ${needProduce}`,
    `物料上限 ${materialLimit}`,
    `设备上限 ${equipmentLimit}`,
    `人员上限 ${operatorLimit}`
  ]

  calcSteps.push({
    id: 'calcConstraint',
    stepKey: 'result',
    label: '约束求解',
    formula: '建议排产量 = min(生产缺口, 物料上限, 设备上限, 人员上限)',
    substitution: constraintParts.join('、'),
    result: recommended,
    unit: '台'
  })

  return {
    inputs: {
      orderQty,
      fgStock,
      shipStock,
      needProduce,
      materialLimit,
      equipmentLimit,
      operatorLimit,
      workDays,
      planStart: analysis.planStart,
      planEnd: analysis.planEnd
    },
    calcSteps,
    outputs: {
      recommended,
      shipStock,
      totalDelivery,
      bottlenecks: bottleneckLabels(needProduce, materialLimit, equipmentLimit, operatorLimit, recommended),
      recommendation: analysis.recommendation || inv.recommendation || '',
      feasibleQty: num(cap.feasibleQty)
    }
  }
}

export function getRevealedCalcIds(revealedStepKeys) {
  const set = new Set()
  for (const key of revealedStepKeys) {
    for (const id of CALC_REVEAL_BY_STEP[key] || []) {
      set.add(id)
    }
  }
  return set
}

export function isCalcStepRevealed(calcStepId, revealedStepKeys, running) {
  if (!running && revealedStepKeys.length) {
    return getRevealedCalcIds(revealedStepKeys).has(calcStepId)
  }
  if (!running) return true
  return getRevealedCalcIds(revealedStepKeys).has(calcStepId)
}

export { STEP_ORDER }
