import imgMotherboard from '@picture/PCB主板.jpg'
import imgDisplay from '@picture/LCD面板图片.jpg'
import imgAttach from '@picture/背光模组.png'
import imgAssembly from '@picture/2.jpg'

/** 操作员四道生产工序（与 OperatorWorkshopCatalog 一致） */
export const OPERATOR_PROCESS_STATIONS = [
  {
    id: 'motherboard',
    order: 1,
    stageName: '主板装配',
    title: '主板装配',
    subtitle: 'PCB 贴装 · 主控焊接 · 电路通电测试',
    image: imgMotherboard,
    workshops: ['主板装配一车间', '主板装配二车间']
  },
  {
    id: 'display',
    order: 2,
    stageName: '显示屏加工',
    title: '显示屏加工',
    subtitle: '面板点亮 · 背光组装 · 显示功能初检',
    image: imgDisplay,
    workshops: ['显示屏加工一车间', '显示屏加工二车间']
  },
  {
    id: 'attach',
    order: 3,
    stageName: '面板贴附',
    title: '面板贴附',
    subtitle: '面板与背光贴合 · 边框压合 · 贴合精度校验',
    image: imgAttach,
    workshops: ['贴附一车间', '贴附二车间']
  },
  {
    id: 'assembly',
    order: 4,
    stageName: '整机组装',
    title: '整机组装',
    subtitle: '整机总装 · 线缆连接 · 老化前整机联调',
    image: imgAssembly,
    workshops: ['组装一车间', '组装二车间', '组装三车间']
  }
]

export function stationById(id) {
  return OPERATOR_PROCESS_STATIONS.find((s) => s.id === id) || null
}

export function stationByStageName(name) {
  const n = String(name || '')
  return OPERATOR_PROCESS_STATIONS.find((s) =>
    s.stageName === n
    || (n.includes('主板') && s.id === 'motherboard')
    || (n.includes('显示屏') && s.id === 'display')
    || (n.includes('贴附') && s.id === 'attach')
    || (n.includes('组装') && s.id === 'assembly')
  ) || null
}

/** 派工单是否属于该工序 */
export function dispatchMatchesStage(dispatch, station) {
  if (!dispatch || !station) return false
  const step = String(dispatch.processStep || dispatch.stageName || '')
  const stage = station.stageName
  if (dispatch.stageName === stage || step === stage) return true
  if (stage === '整机组装' && (step.includes('组装') || dispatch.finalProductionStep)) return true
  if (stage === '面板贴附' && step.includes('贴附')) return true
  if (stage === '显示屏加工' && step.includes('显示屏')) return true
  if (stage === '主板装配' && step.includes('主板')) return true
  return false
}
