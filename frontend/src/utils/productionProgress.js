/** 八道生产工序（每道工序 2~3 个并行车间） */
export const PRODUCTION_STAGES = [
  {
    stepKey: 'motherboard',
    stepName: '主板装配',
    order: 1,
    keywords: ['主板装配', '主板'],
    workshops: [
      { key: 'mb-1', name: '主板装配一车间' },
      { key: 'mb-2', name: '主板装配二车间' },
      { key: 'mb-3', name: '主板装配三车间' }
    ]
  },
  {
    stepKey: 'powerboard',
    stepName: '电源板装配',
    order: 2,
    keywords: ['电源板装配', '电源板'],
    workshops: [
      { key: 'pb-1', name: '电源板装配一车间' },
      { key: 'pb-2', name: '电源板装配二车间' }
    ]
  },
  {
    stepKey: 'interface',
    stepName: '接口板装配',
    order: 3,
    keywords: ['接口板装配', '接口板'],
    workshops: [
      { key: 'if-1', name: '接口板装配一车间' },
      { key: 'if-2', name: '接口板装配二车间' }
    ]
  },
  {
    stepKey: 'display',
    stepName: '显示屏加工',
    order: 4,
    keywords: ['显示屏加工', '显示屏'],
    workshops: [
      { key: 'display-1', name: '显示屏加工一车间' },
      { key: 'display-2', name: '显示屏加工二车间' },
      { key: 'display-3', name: '显示屏加工三车间' }
    ]
  },
  {
    stepKey: 'attach',
    stepName: '面板贴附',
    order: 5,
    keywords: ['面板贴附', '贴附'],
    workshops: [
      { key: 'attach-1', name: '贴附一车间' },
      { key: 'attach-2', name: '贴附二车间' }
    ]
  },
  {
    stepKey: 'shell',
    stepName: '外壳装配',
    order: 6,
    keywords: ['外壳装配', '外壳'],
    workshops: [
      { key: 'shell-1', name: '外壳装配一车间' },
      { key: 'shell-2', name: '外壳装配二车间' }
    ]
  },
  {
    stepKey: 'assembly',
    stepName: '整机组装',
    order: 7,
    keywords: ['整机组装', '背光组装'],
    workshops: [
      { key: 'assembly-1', name: '组装一车间' },
      { key: 'assembly-2', name: '组装二车间' },
      { key: 'assembly-3', name: '组装三车间' }
    ]
  },
  {
    stepKey: 'bracket',
    stepName: '支架底座装配',
    order: 8,
    keywords: ['支架底座装配', '支架', '底座'],
    workshops: [
      { key: 'bracket-1', name: '支架底座装配一车间' },
      { key: 'bracket-2', name: '支架底座装配二车间' }
    ]
  }
]

export const PRODUCTION_WORKSHOPS = PRODUCTION_STAGES.flatMap((s) =>
  s.workshops.map((w) => ({ ...w, parentStepKey: s.stepKey, parentStepName: s.stepName, order: s.order }))
)

export const PRODUCTION_STEP_KEYWORDS = PRODUCTION_STAGES.flatMap((s) => [s.stepName, ...(s.keywords || [])])

export const NON_PRODUCTION_STEP_KEYWORDS = [
  '老化', '调校', '包装', '质检', '检验', '终检', '发货', '售后'
]

export function stageForStepName(stepName) {
  if (!stepName) return null
  const name = String(stepName)
  const exact = PRODUCTION_STAGES.find((s) => s.stepName === name)
  if (exact) return exact
  for (const stage of [...PRODUCTION_STAGES].reverse()) {
    if (stage.keywords?.some((k) => name.includes(k))) return stage
  }
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
