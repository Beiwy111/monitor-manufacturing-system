/** 四道生产工序（每道工序 2~3 个并行车间） */
export const PRODUCTION_STAGES = [
  {
    stepKey: 'display',
    stepName: '显示屏加工',
    order: 1,
    workshops: [
      { key: 'display-1', name: '显示屏加工一车间' },
      { key: 'display-2', name: '显示屏加工二车间' },
      { key: 'display-3', name: '显示屏加工三车间' }
    ]
  },
  {
    stepKey: 'motherboard',
    stepName: '主板装配',
    order: 2,
    workshops: [
      { key: 'mb-1', name: '主板装配一车间' },
      { key: 'mb-2', name: '主板装配二车间' },
      { key: 'mb-3', name: '主板装配三车间' }
    ]
  },
  {
    stepKey: 'attach',
    stepName: '贴附',
    order: 3,
    workshops: [
      { key: 'attach-1', name: '贴附一车间' },
      { key: 'attach-2', name: '贴附二车间' }
    ]
  },
  {
    stepKey: 'assembly',
    stepName: '组装',
    order: 4,
    workshops: [
      { key: 'assembly-1', name: '组装一车间' },
      { key: 'assembly-2', name: '组装二车间' },
      { key: 'assembly-3', name: '组装三车间' }
    ]
  }
]

export const PRODUCTION_WORKSHOPS = PRODUCTION_STAGES.flatMap((s) =>
  s.workshops.map((w) => ({ ...w, parentStepKey: s.stepKey, parentStepName: s.stepName, order: s.order }))
)

/** @deprecated 使用 PRODUCTION_STAGES */
export const PRODUCTION_STEP_KEYWORDS = [
  '显示屏加工', '主板装配', '面板贴附', '贴附', '整机组装', '背光组装'
]

export const NON_PRODUCTION_STEP_KEYWORDS = [
  '老化', '调校', '包装', '质检', '检验', '终检', '发货', '售后'
]

export function stageForStepName(stepName) {
  if (!stepName) return null
  if (stepName.includes('显示屏')) return PRODUCTION_STAGES[0]
  if (stepName.includes('主板')) return PRODUCTION_STAGES[1]
  if (stepName.includes('贴附')) return PRODUCTION_STAGES[2]
  if (stepName.includes('组装')) return PRODUCTION_STAGES[3]
  return null
}

export function isProductionStep(stepName) {
  if (!stepName) return false
  if (NON_PRODUCTION_STEP_KEYWORDS.some((k) => stepName.includes(k))) return false
  return !!stageForStepName(stepName)
}

/** 成品完成量 = 各工序完成量累加后取最小值（瓶颈工序） */
export function finishedGoodsQty(dispatches, workOrderId) {
  const list = (dispatches || []).filter((d) => {
    const wo = d.workOrderId || d.workOrderNo
    return wo === workOrderId && isProductionStep(d.processStep)
  })
  if (!list.length) return 0

  const stageTotals = PRODUCTION_STAGES.map((stage) => {
    return list
      .filter((d) => {
        const s = stageForStepName(d.processStep)
        return s && s.stepKey === stage.stepKey
      })
      .reduce((sum, d) => sum + (d.completedQty || 0), 0)
  })

  const active = stageTotals.filter((_, i) =>
    list.some((d) => stageForStepName(d.processStep)?.stepKey === PRODUCTION_STAGES[i].stepKey)
  )
  if (!active.length) {
    return Math.min(...list.map((d) => d.completedQty || 0))
  }
  return Math.min(...stageTotals)
}

export function workshopForStep(stepName, equipmentWorkshop = '') {
  if (equipmentWorkshop) return equipmentWorkshop
  const stage = stageForStepName(stepName)
  if (!stage) return '生产一部'
  const hit = stage.workshops.find((w) => stepName.includes(w.name)) || stage.workshops[0]
  return hit?.name || stage.stepName
}
