/** 工业管理大屏统一色板（低饱和） */
export const WB = {
  blue: '#4a6fa5',
  cyan: '#4a9090',
  orange: '#c4854a',
  purple: '#7a6a9a',
  red: '#c45a5a',
  green: '#5a9a6a',
  yellow: '#b89a4a',
  gray: '#8a9199',
  slate: '#5c6672',
  grid: '#e4e8ee',
  bg: '#f4f6f9',
  panel: '#fafbfc',
  line: '#d8dee6'
}

export const STATUS = {
  normal: WB.green,
  warn: WB.yellow,
  danger: WB.red,
  info: WB.blue
}

export const PALETTE = [WB.blue, WB.cyan, WB.orange, WB.purple, WB.green, WB.yellow, WB.red, WB.gray]

export function statusClass(s) {
  if (s === 'danger') return 'is-danger'
  if (s === 'warn') return 'is-warn'
  return 'is-normal'
}
