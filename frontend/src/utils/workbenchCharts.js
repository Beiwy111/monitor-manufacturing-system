import { WB, PALETTE } from '@/constants/workbenchTheme'

const COMPACT = {
  grid: { left: 4, right: 8, top: 28, bottom: 4, containLabel: true },
  textStyle: { fontFamily: 'inherit', fontSize: 11, color: WB.slate },
  axis: { axisLine: { lineStyle: { color: WB.line } }, axisLabel: { color: WB.gray, fontSize: 10 }, splitLine: { lineStyle: { color: '#eef1f5', type: 'dashed' } } }
}

export function buildPanelOption(panel) {
  if (!panel) return {}
  switch (panel.type) {
    case 'donut': return buildDonut(panel)
    case 'pie': return buildPie(panel)
    case 'bar': return buildBar(panel)
    case 'line': return buildLine(panel)
    case 'combo': return buildCombo(panel)
    case 'horizontalBar': return buildHorizontalBar(panel)
    case 'heatmap': return buildHeatmap(panel)
    case 'gantt': return buildGantt(panel)
    default: return {}
  }
}

function buildDonut(panel) {
  const items = (panel.items || []).filter((i) => i.value > 0)
  const total = items.reduce((s, i) => s + i.value, 0)
  return {
    ...COMPACT,
    tooltip: { trigger: 'item', formatter: '{b}：{c}（{d}%）' },
    title: {
      text: String(total),
      subtext: panel.centerSub || '',
      left: 'center',
      top: '42%',
      textStyle: { fontSize: 18, fontWeight: 700, color: '#2c3540' },
      subtextStyle: { fontSize: 10, color: WB.gray }
    },
    series: [{
      type: 'pie',
      radius: ['52%', '72%'],
      center: ['50%', '52%'],
      itemStyle: { borderColor: '#fff', borderWidth: 1 },
      label: { show: false },
      data: items.map((i, idx) => ({ name: i.name, value: i.value, itemStyle: { color: i.color || PALETTE[idx % PALETTE.length] } }))
    }]
  }
}

function buildPie(panel) {
  const items = (panel.items || []).filter((i) => i.value > 0)
  return {
    ...COMPACT,
    tooltip: { trigger: 'item' },
    legend: { show: false },
    series: [{
      type: 'pie',
      radius: ['0%', '62%'],
      center: ['50%', '55%'],
      label: { fontSize: 10, formatter: '{b}\n{d}%' },
      data: items.map((i, idx) => ({ name: i.name, value: i.value, itemStyle: { color: i.color || PALETTE[idx % PALETTE.length] } }))
    }]
  }
}

function buildBar(panel) {
  const series = (panel.series || []).map((s, idx) => ({
    name: s.name,
    type: 'bar',
    data: s.data,
    barMaxWidth: 18,
    itemStyle: { color: s.color || PALETTE[idx % PALETTE.length], borderRadius: [2, 2, 0, 0] }
  }))
  return cartesian(panel.categories, series)
}

function buildLine(panel) {
  const series = (panel.series || []).map((s, idx) => ({
    name: s.name,
    type: 'line',
    data: s.data,
    smooth: true,
    symbol: 'none',
    lineStyle: { width: 2, color: s.color || PALETTE[idx % PALETTE.length] },
    areaStyle: panel.area ? { color: `${s.color || PALETTE[idx % PALETTE.length]}22` } : undefined
  }))
  return cartesian(panel.categories, series)
}

function buildCombo(panel) {
  const series = (panel.series || []).map((s, idx) => {
    const t = s.chartType === 'line' ? 'line' : 'bar'
    const color = s.color || PALETTE[idx % PALETTE.length]
    const base = { name: s.name, type: t, data: s.data, yAxisIndex: t === 'line' ? 1 : 0, itemStyle: { color } }
    if (t === 'line') {
      base.smooth = true
      base.symbol = 'none'
      base.lineStyle = { width: 2, color }
    } else {
      base.barMaxWidth = 16
    }
    return base
  })
  const opt = cartesian(panel.categories, series)
  opt.yAxis = [
    { type: 'value', minInterval: 1, ...COMPACT.axis },
    { type: 'value', splitLine: { show: false }, axisLabel: { color: WB.gray, fontSize: 10 } }
  ]
  return opt
}

function buildHorizontalBar(panel) {
  const cats = panel.categories || panel.items?.map((i) => i.name) || []
  const vals = panel.values || panel.items?.map((i) => i.value) || []
  const colors = panel.colors || vals.map((_, i) => PALETTE[i % PALETTE.length])
  return {
    ...COMPACT,
    grid: { left: 4, right: 16, top: 8, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', minInterval: 1, ...COMPACT.axis },
    yAxis: { type: 'category', data: cats, inverse: true, axisLabel: { fontSize: 10, color: WB.slate, width: 72, overflow: 'truncate' }, axisTick: { show: false }, axisLine: { show: false } },
    series: [{
      type: 'bar',
      data: vals.map((v, i) => ({ value: v, itemStyle: { color: colors[i] || PALETTE[i % PALETTE.length], borderRadius: [0, 2, 2, 0] } })),
      barMaxWidth: 12,
      label: { show: true, position: 'right', fontSize: 10, color: WB.slate }
    }]
  }
}

function buildHeatmap(panel) {
  const xLabels = panel.xLabels || []
  const yLabels = panel.yLabels || []
  const data = panel.data || []
  const max = Math.max(1, ...data.map((d) => d[2] || 0))
  return {
    ...COMPACT,
    grid: { left: 4, right: 8, top: 8, bottom: 4, containLabel: true },
    tooltip: { position: 'top', formatter: (p) => `${yLabels[p.data[1]]} / ${xLabels[p.data[0]]}：${p.data[2]}` },
    xAxis: { type: 'category', data: xLabels, splitArea: { show: true }, axisLabel: { fontSize: 9, color: WB.gray } },
    yAxis: { type: 'category', data: yLabels, splitArea: { show: true }, axisLabel: { fontSize: 9, color: WB.gray } },
    visualMap: { show: false, min: 0, max, inRange: { color: ['#eef2f7', WB.cyan, WB.blue] } },
    series: [{ type: 'heatmap', data, label: { show: true, fontSize: 9, color: WB.slate }, emphasis: { itemStyle: { shadowBlur: 4 } } }]
  }
}

function buildGantt(panel) {
  const rows = panel.rows || []
  const min = panel.rangeStart || 0
  const max = panel.rangeEnd || 100
  return {
    ...COMPACT,
    grid: { left: 4, right: 12, top: 8, bottom: 4, containLabel: true },
    tooltip: { formatter: (p) => `${p.name}<br/>${p.data[3] || ''}` },
    xAxis: { type: 'value', min, max, axisLabel: { fontSize: 9, color: WB.gray }, splitLine: { lineStyle: { type: 'dashed', color: '#eef1f5' } } },
    yAxis: { type: 'category', data: rows.map((r) => r.label), inverse: true, axisLabel: { fontSize: 10, color: WB.slate, width: 80, overflow: 'truncate' }, axisTick: { show: false }, axisLine: { show: false } },
    series: [{
      type: 'bar',
      stack: 'g',
      silent: true,
      itemStyle: { borderColor: 'transparent', color: 'transparent' },
      data: rows.map((r) => r.start)
    }, {
      type: 'bar',
      stack: 'g',
      data: rows.map((r, i) => ({
        value: r.duration,
        name: r.label,
        itemStyle: { color: r.color || PALETTE[i % PALETTE.length], borderRadius: 2 },
        label: { show: !!r.tag, formatter: r.tag, fontSize: 9, color: '#fff', position: 'inside' }
      })),
      barMaxWidth: 14
    }]
  }
}

function cartesian(categories, series) {
  return {
    ...COMPACT,
    tooltip: { trigger: 'axis' },
    legend: series.length > 1 ? { data: series.map((s) => s.name), top: 0, right: 0, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 10, color: WB.gray } } : undefined,
    xAxis: { type: 'category', data: categories || [], axisTick: { show: false }, ...COMPACT.axis },
    yAxis: { type: 'value', minInterval: 1, ...COMPACT.axis },
    series
  }
}

export function donutLegend(items) {
  const total = (items || []).reduce((s, i) => s + (i.value || 0), 0) || 1
  return (items || []).map((i) => ({ ...i, pct: Math.round(((i.value || 0) / total) * 100) }))
}

export function panelHasData(panel) {
  if (!panel) return false
  if (panel.items?.length) return panel.items.some((i) => i.value > 0)
  if (panel.rows?.length) return true
  if (panel.progress?.length) return true
  if (panel.statusList?.length) return true
  if (panel.values?.length) return panel.values.some((v) => Number(v) > 0)
  if (panel.series?.length) return panel.series.some((s) => (s.data || []).some((v) => Number(v) > 0))
  return false
}
