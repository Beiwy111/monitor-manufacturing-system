/** 操作员固定车间绑定（与后端 OperatorWorkshopCatalog 一致） */
/** 八道生产工序默认主责操作员 */
export const PRIMARY_OPERATORS = {
  motherboard: 'li_operator',
  powerboard: 'huang_operator',
  interface: 'yang_operator',
  display: 'zhao_operator',
  attach: 'wang_operator',
  shell: 'gu_operator',
  assembly: 'sun_operator',
  bracket: 'han_operator'
}

/** 系统管理员演示操作员模块时，映射为第 8 道工序（支架底座装配）主责账号 */
export const ADMIN_DEMO_OPERATOR = PRIMARY_OPERATORS.bracket

/** 解析 MES 操作员 API 使用的登录名（admin → han_operator） */
export function resolveOperatorUsername(roleKey, username) {
  if (roleKey === 'admin') return ADMIN_DEMO_OPERATOR
  return username || ''
}

export const OPERATOR_BINDINGS = {
  li_operator: { workshopKey: 'mb-1', workshopName: '主板装配一车间', stageName: '主板装配', stageOrder: 1 },
  wu_operator: { workshopKey: 'mb-2', workshopName: '主板装配二车间', stageName: '主板装配', stageOrder: 1 },
  bai_operator: { workshopKey: 'mb-3', workshopName: '主板装配三车间', stageName: '主板装配', stageOrder: 1 },
  huang_operator: { workshopKey: 'pb-1', workshopName: '电源板装配一车间', stageName: '电源板装配', stageOrder: 2 },
  xu_operator: { workshopKey: 'pb-2', workshopName: '电源板装配二车间', stageName: '电源板装配', stageOrder: 2 },
  yang_operator: { workshopKey: 'if-1', workshopName: '接口板装配一车间', stageName: '接口板装配', stageOrder: 3 },
  he_operator: { workshopKey: 'if-2', workshopName: '接口板装配二车间', stageName: '接口板装配', stageOrder: 3 },
  zhao_operator: { workshopKey: 'display-1', workshopName: '显示屏加工一车间', stageName: '显示屏加工', stageOrder: 4 },
  ma_operator: { workshopKey: 'display-2', workshopName: '显示屏加工二车间', stageName: '显示屏加工', stageOrder: 4 },
  feng_operator: { workshopKey: 'display-3', workshopName: '显示屏加工三车间', stageName: '显示屏加工', stageOrder: 4 },
  wang_operator: { workshopKey: 'attach-1', workshopName: '贴附一车间', stageName: '面板贴附', stageOrder: 5 },
  zhou_operator: { workshopKey: 'attach-2', workshopName: '贴附二车间', stageName: '面板贴附', stageOrder: 5 },
  gu_operator: { workshopKey: 'shell-1', workshopName: '外壳装配一车间', stageName: '外壳装配', stageOrder: 6 },
  xie_operator: { workshopKey: 'shell-2', workshopName: '外壳装配二车间', stageName: '外壳装配', stageOrder: 6 },
  sun_operator: { workshopKey: 'assembly-1', workshopName: '组装一车间', stageName: '整机组装', stageOrder: 7 },
  chen_operator: { workshopKey: 'assembly-2', workshopName: '组装二车间', stageName: '整机组装', stageOrder: 7 },
  lin_operator: { workshopKey: 'assembly-3', workshopName: '组装三车间', stageName: '整机组装', stageOrder: 7 },
  han_operator: { workshopKey: 'bracket-1', workshopName: '支架底座装配一车间', stageName: '支架底座装配', stageOrder: 8 },
  tang_operator: { workshopKey: 'bracket-2', workshopName: '支架底座装配二车间', stageName: '支架底座装配', stageOrder: 8 }
}

/** 操作员显示名（与数据库 seed 一致） */
export const OPERATOR_DISPLAY_NAMES = {
  li_operator: '李操作',
  wu_operator: '吴操作',
  bai_operator: '白操作',
  huang_operator: '黄操作',
  xu_operator: '徐操作',
  yang_operator: '杨操作',
  he_operator: '何操作',
  zhao_operator: '赵操作',
  ma_operator: '马操作',
  feng_operator: '冯操作',
  wang_operator: '王操作',
  zhou_operator: '周操作',
  gu_operator: '顾操作',
  xie_operator: '谢操作',
  sun_operator: '孙操作',
  chen_operator: '陈操作',
  lin_operator: '林操作',
  han_operator: '韩操作',
  tang_operator: '唐操作'
}

/** 获取某车间绑定的操作员列表 */
export function operatorsForWorkshop(workshopKey) {
  return Object.entries(OPERATOR_BINDINGS)
    .filter(([, bind]) => bind.workshopKey === workshopKey)
    .map(([username, bind]) => ({
      username,
      displayName: OPERATOR_DISPLAY_NAMES[username] || username.replace('_operator', ''),
      workshopKey: bind.workshopKey,
      workshopName: bind.workshopName,
      stageName: bind.stageName
    }))
}

const TOTAL_STAGES = 8

const STAGE_MATCH_RULES = [
  { stageName: '支架底座装配', keywords: ['支架底座', '支架', '底座'] },
  { stageName: '整机组装', keywords: ['整机组装', '背光组装'] },
  { stageName: '外壳装配', keywords: ['外壳装配', '外壳'] },
  { stageName: '面板贴附', keywords: ['面板贴附', '贴附'] },
  { stageName: '显示屏加工', keywords: ['显示屏加工', '显示屏'] },
  { stageName: '接口板装配', keywords: ['接口板装配', '接口板'] },
  { stageName: '电源板装配', keywords: ['电源板装配', '电源板'] },
  { stageName: '主板装配', keywords: ['主板装配', '主板'] }
]

export function operatorBinding(username) {
  return OPERATOR_BINDINGS[username] || null
}

export function operatorStageId(username) {
  const bind = operatorBinding(username)
  if (!bind) return null
  const map = {
    主板装配: 'motherboard',
    电源板装配: 'powerboard',
    接口板装配: 'interface',
    显示屏加工: 'display',
    面板贴附: 'attach',
    外壳装配: 'shell',
    整机组装: 'assembly',
    支架底座装配: 'bracket'
  }
  return map[bind.stageName] || null
}

export function operatorReportPath() {
  return '/production/report'
}

export function operatorLabel(user) {
  if (!user) return ''
  const bind = operatorBinding(user.username)
  if (bind) return `${user.realName} · ${bind.workshopName}`
  return `${user.realName}（${user.username}）`
}

function resolveStageName(processStep) {
  const name = String(processStep || '')
  if (!name) return ''
  const exact = STAGE_MATCH_RULES.find((r) => r.stageName === name)
  if (exact) return exact.stageName
  for (const rule of STAGE_MATCH_RULES) {
    if (rule.keywords.some((k) => name.includes(k))) return rule.stageName
  }
  return name
}

export function operatorsForProcessStep(processStep) {
  const stageName = resolveStageName(processStep)
  if (!stageName) {
    return Object.keys(OPERATOR_BINDINGS)
  }
  return Object.entries(OPERATOR_BINDINGS)
    .filter(([, bind]) => bind.stageName === stageName)
    .map(([username]) => username)
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
