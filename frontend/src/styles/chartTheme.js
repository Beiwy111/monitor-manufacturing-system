import * as echarts from 'echarts'

const FONT =
  'Inter, "PingFang SC", "Microsoft YaHei", "Noto Sans CJK SC", Arial, sans-serif'

const AXIS_TEXT = { color: '#25272A', fontSize: 13, fontFamily: FONT }
const GRID_LINE = { lineStyle: { color: '#E8E8E3' } }

/** 全局图表主题：仅统一文字/网格，不覆盖 series 业务色 */
export const MES_CHART_THEME = {
  textStyle: { color: '#25272A', fontFamily: FONT, fontSize: 13 },
  title: {
    textStyle: { color: '#25272A', fontSize: 14, fontWeight: 600, fontFamily: FONT },
    subtextStyle: { color: '#7E838B', fontSize: 13, fontFamily: FONT }
  },
  legend: { textStyle: { color: '#25272A', fontSize: 13, fontFamily: FONT } },
  tooltip: {
    textStyle: { color: '#25272A', fontSize: 13, fontFamily: FONT }
  },
  categoryAxis: {
    axisLabel: AXIS_TEXT,
    axisLine: { lineStyle: { color: '#E8E8E3' } },
    splitLine: GRID_LINE
  },
  valueAxis: {
    axisLabel: AXIS_TEXT,
    axisLine: { lineStyle: { color: '#E8E8E3' } },
    splitLine: GRID_LINE
  },
  timeAxis: {
    axisLabel: AXIS_TEXT,
    axisLine: { lineStyle: { color: '#E8E8E3' } },
    splitLine: GRID_LINE
  },
  logAxis: {
    axisLabel: AXIS_TEXT,
    splitLine: GRID_LINE
  }
}

let registered = false

export function registerMesChartTheme() {
  if (registered) return
  echarts.registerTheme('mes', MES_CHART_THEME)
  registered = true
}

export function mergeChartOption(option = {}) {
  const base = {
    textStyle: { ...MES_CHART_THEME.textStyle, ...(option.textStyle || {}) }
  }
  return { ...base, ...option }
}
