/** 操作员固定车间绑定（与后端 OperatorWorkshopCatalog 一致） */
/** 四道工序默认主责操作员 */
export const PRIMARY_OPERATORS = {
  display: 'zhao_operator',
  motherboard: 'li_operator',
  attach: 'wang_operator',
  assembly: 'sun_operator'
}

export const OPERATOR_BINDINGS = {
  zhao_operator: { workshopKey: 'display-1', workshopName: '显示屏加工一车间', stageName: '显示屏加工', stageOrder: 1 },
  ma_operator: { workshopKey: 'display-2', workshopName: '显示屏加工二车间', stageName: '显示屏加工', stageOrder: 1 },
  li_operator: { workshopKey: 'mb-1', workshopName: '主板装配一车间', stageName: '主板装配', stageOrder: 2 },
  wu_operator: { workshopKey: 'mb-2', workshopName: '主板装配二车间', stageName: '主板装配', stageOrder: 2 },
  wang_operator: { workshopKey: 'attach-1', workshopName: '贴附一车间', stageName: '面板贴附', stageOrder: 3 },
  zhou_operator: { workshopKey: 'attach-2', workshopName: '贴附二车间', stageName: '面板贴附', stageOrder: 3 },
  sun_operator: { workshopKey: 'assembly-1', workshopName: '组装一车间', stageName: '整机组装', stageOrder: 4 },
  operator01: { workshopKey: 'assembly-2', workshopName: '组装二车间', stageName: '整机组装', stageOrder: 4 },
  operator02: { workshopKey: 'assembly-3', workshopName: '组装三车间', stageName: '整机组装', stageOrder: 4 }
}

const TOTAL_STAGES = 4

export function operatorBinding(username) {
  return OPERATOR_BINDINGS[username] || null
}

export function pickCurrentDispatch(dispatches = [], reportableOnly = false) {
  const list = [...(dispatches || [])]
  const priority = reportableOnly
    ? ['生产中', '已接收']
    : ['生产中', '已接收', '已分配']
  for (const status of priority) {
    const hit = list.find((d) => d.status === status)
    if (hit) return hit
  }
  return list[0] || null
}

export function stageProgressLabel(dispatch) {
  if (!dispatch) return ''
  const order = dispatch.stageOrder || operatorBinding(dispatch.operator)?.stageOrder
  if (!order) return dispatch.processStep || ''
  const total = dispatch.totalStages || TOTAL_STAGES
  return `第 ${order}/${total} 道工序 · ${dispatch.stageName || dispatch.processStep || ''}`
}
