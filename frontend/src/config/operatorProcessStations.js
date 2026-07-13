import imgMotherboard from '@picture/主板装配.jpg'
import imgPowerboard from '@picture/电源板装配.png'
import imgInterface from '@picture/接口板装配.jpg'
import imgDisplay from '@picture/显示屏加工.jpg'
import imgAttach from '@picture/面板贴附.jpg'
import imgShell from '@picture/外壳装配.jpg'
import imgAssembly from '@picture/整机组装.jpg'
import imgBracket from '@picture/支架底座装配.jpg'

/** 操作员八道生产工序（与 OperatorWorkshopCatalog / ProductionWorkshopCatalog 一致） */
export const OPERATOR_PROCESS_STATIONS = [
  {
    id: 'motherboard',
    order: 1,
    stageName: '主板装配',
    title: '主板装配',
    subtitle: 'PCB 贴装 · 主控焊接 · 电路通电测试',
    image: imgMotherboard,
    workshops: ['主板装配一车间', '主板装配二车间', '主板装配三车间']
  },
  {
    id: 'powerboard',
    order: 2,
    stageName: '电源板装配',
    title: '电源板装配',
    subtitle: 'PCB 贴装 · 主控焊接 · 电路通电测试',
    image: imgPowerboard,
    workshops: ['电源板装配一车间', '电源板装配二车间']
  },
  {
    id: 'interface',
    order: 3,
    stageName: '接口板装配',
    title: '接口板装配',
    subtitle: 'PCB 贴装 · 主控焊接 · 电路通电测试',
    image: imgInterface,
    workshops: ['接口板装配一车间', '接口板装配二车间']
  },
  {
    id: 'display',
    order: 4,
    stageName: '显示屏加工',
    title: '显示屏加工',
    subtitle: '面板点亮 · 背光组装 · 显示功能初检',
    image: imgDisplay,
    workshops: ['显示屏加工一车间', '显示屏加工二车间', '显示屏加工三车间']
  },
  {
    id: 'attach',
    order: 5,
    stageName: '面板贴附',
    title: '面板贴附',
    subtitle: '面板与背光贴合 · 边框压合 · 贴合精度校验',
    image: imgAttach,
    workshops: ['贴附一车间', '贴附二车间']
  },
  {
    id: 'shell',
    order: 6,
    stageName: '外壳装配',
    title: '外壳装配',
    subtitle: '面板与背光贴合 · 边框压合 · 贴合精度校验',
    image: imgShell,
    workshops: ['外壳装配一车间', '外壳装配二车间']
  },
  {
    id: 'assembly',
    order: 7,
    stageName: '整机组装',
    title: '整机组装',
    subtitle: '整机总装 · 线缆连接 · 老化前整机联调',
    image: imgAssembly,
    workshops: ['组装一车间', '组装二车间', '组装三车间']
  },
  {
    id: 'bracket',
    order: 8,
    stageName: '支架底座装配',
    title: '支架底座装配',
    subtitle: '整机总装 · 线缆连接 · 老化前整机联调',
    image: imgBracket,
    workshops: ['支架底座装配一车间', '支架底座装配二车间']
  }
]

const STAGE_MATCH_RULES = [
  { id: 'bracket', keywords: ['支架底座', '支架', '底座'] },
  { id: 'assembly', keywords: ['整机组装', '背光组装'] },
  { id: 'shell', keywords: ['外壳装配', '外壳'] },
  { id: 'attach', keywords: ['面板贴附', '贴附'] },
  { id: 'display', keywords: ['显示屏加工', '显示屏'] },
  { id: 'interface', keywords: ['接口板装配', '接口板'] },
  { id: 'powerboard', keywords: ['电源板装配', '电源板'] },
  { id: 'motherboard', keywords: ['主板装配', '主板'] }
]

export function stationById(id) {
  return OPERATOR_PROCESS_STATIONS.find((s) => s.id === id) || null
}

export function stationByStageName(name) {
  const n = String(name || '')
  const exact = OPERATOR_PROCESS_STATIONS.find((s) => s.stageName === n)
  if (exact) return exact
  for (const rule of STAGE_MATCH_RULES) {
    if (rule.keywords.some((k) => n.includes(k))) {
      return stationById(rule.id)
    }
  }
  return null
}

/** 派工单是否属于该工序 */
export function dispatchMatchesStage(dispatch, station) {
  if (!dispatch || !station) return false
  const step = String(dispatch.processStep || dispatch.stageName || '')
  const stage = station.stageName
  if (dispatch.stageName === stage || step === stage) return true
  const matched = stationByStageName(step)
  return matched?.id === station.id
}
