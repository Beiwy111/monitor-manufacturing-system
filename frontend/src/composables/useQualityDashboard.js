/** 质检员工作台 — 指标/图表/筛选计算（仅用现有 inspection + nc 数据） */

export const QC_COLORS = {
  primary: '#4a6fa5',
  bar: '#5b7fa8',
  line: '#8a9bb0',
  pass: '#2d8a5e',
  recheck: '#d48806',
  fail: '#c0392b',
  pending: '#8a94a6',
  grid: '#eef1f5',
  axis: '#b8bec8',
  text: '#1f2937',
  muted: '#6b7280'
}

export const TYPE_CN = {
  INCOMING: '来料检',
  PROCESS: '过程检',
  FINAL: '终检',
  RECHECK: '复检',
  PANEL_INSPECTION: '面板检',
  BACKLIGHT_INSPECTION: '背光检',
  PCB_INSPECTION: 'PCB检',
  ASSEMBLY_INSPECTION: '组装检'
}

export const STATUS_CN = {
  PENDING: '待判定',
  PASSED: '通过',
  FAILED: '不通过',
  RECHECK_REQUIRED: '需复检',
  CLOSED: '已关闭'
}

export function parseQcDate(value) {
  if (!value) return null
  const d = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(d.getTime()) ? null : d
}

export function startOfDay(date = new Date()) {
  const d = new Date(date)
  d.setHours(0, 0, 0, 0)
  return d
}

export function addDays(date, n) {
  const d = new Date(date)
  d.setDate(d.getDate() + n)
  return d
}

export function dayKey(date) {
  const d = date instanceof Date ? date : parseQcDate(date)
  if (!d) return ''
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export function formatDayLabel(key) {
  const [, m, d] = key.split('-')
  return `${Number(m)}/${Number(d)}`
}

export function inDateRange(value, rangeDays, end = new Date()) {
  const d = parseQcDate(value)
  if (!d) return false
  const start = addDays(startOfDay(end), -(rangeDays - 1))
  return d >= start && d <= end
}

export function isToday(value, ref = new Date()) {
  return dayKey(value) === dayKey(ref)
}

export function isYesterday(value, ref = new Date()) {
  return dayKey(value) === dayKey(addDays(startOfDay(ref), -1))
}

export function formatWaitDuration(ms) {
  if (ms <= 0) return '0分钟'
  const mins = Math.floor(ms / 60000)
  if (mins < 60) return `${mins}分钟`
  const h = Math.floor(mins / 60)
  const m = mins % 60
  if (h < 24) return m ? `${h}小时${m}分` : `${h}小时`
  const d = Math.floor(h / 24)
  return `${d}天${h % 24}小时`
}

export function normHandleStatus(s) {
  return s === 'COMPLETED' ? 'DONE' : s
}

export function normHandleMethod(s) {
  return s === 'CONCESSION' ? 'CONCESSION_ACCEPT' : s
}

export function processLabel(row) {
  return TYPE_CN[row?.inspectionType] || row?.inspectionTypeCn || '—'
}

/** 返回近 N 日带日期标签的序列 */
export function buildDailySeriesDetailed(items, pickDate, pickValue, days = 7, end = new Date()) {
  const points = []
  for (let i = days - 1; i >= 0; i--) {
    const d = addDays(startOfDay(end), -i)
    const key = dayKey(d)
    let value = 0
    for (const item of items) {
      if (dayKey(pickDate(item)) === key) value += pickValue(item)
    }
    points.push({ key, label: formatDayLabel(key), value })
  }
  return points
}

export function buildDailySeries(items, pickDate, pickValue, days = 7, end = new Date()) {
  return buildDailySeriesDetailed(items, pickDate, pickValue, days, end).map((p) => p.value)
}

/** 较昨日变化（允许正负，用于实时状态类指标） */
export function deltaChange(current, previous, suffix = '') {
  const diff = current - previous
  if (diff === 0) return { text: '持平', cls: 'flat' }
  const sign = diff > 0 ? '+' : ''
  return { text: `${sign}${diff}${suffix}`, cls: diff > 0 ? 'up' : 'neutral' }
}

/** 本周新增（累计类指标，不展示负增长） */
export function weekNewDelta(count, suffix = '') {
  return { text: count > 0 ? `+${count}${suffix}` : `0${suffix}`, cls: count > 0 ? 'up' : 'flat' }
}

export function pctChange(current, previous) {
  if (!previous) return current ? '+100%' : '0%'
  const diff = ((current - previous) / previous) * 100
  const sign = diff > 0 ? '+' : ''
  return `${sign}${diff.toFixed(1)}%`
}

export function passRateOfRows(rows) {
  let sample = 0
  let qual = 0
  for (const r of rows) {
    sample += Number(r.sampleQuantity) || 0
    qual += Number(r.qualifiedQuantity) || 0
  }
  if (sample > 0) return Math.round((qual / sample) * 100)
  const judged = rows.filter((r) => r.inspectionResult)
  if (!judged.length) return null
  const ok = judged.filter((r) => r.inspectionResult === 'QUALIFIED').length
  return Math.round((ok / judged.length) * 100)
}

export function taskSlaMs(row) {
  return row.inspectionStatus === 'RECHECK_REQUIRED' ? 48 * 3600000 : 24 * 3600000
}

export function isTaskOverdue(row, now = Date.now()) {
  const created = parseQcDate(row.createdAt)
  if (!created) return false
  return now - created.getTime() > taskSlaMs(row)
}

export function isTaskNearOverdue(row, now = Date.now()) {
  const created = parseQcDate(row.createdAt)
  if (!created || isTaskOverdue(row, now)) return false
  return now - created.getTime() > taskSlaMs(row) * 0.75
}

export function derivePriority(row, now = Date.now()) {
  if (isTaskOverdue(row, now)) return { level: '高', key: 'high', sort: 0 }
  if (row.inspectionStatus === 'RECHECK_REQUIRED') return { level: '高', key: 'high', sort: 1 }
  if (isTaskNearOverdue(row, now)) return { level: '中', key: 'mid', sort: 2 }
  return { level: '低', key: 'low', sort: 3 }
}

export function deriveDeadline(row) {
  const created = parseQcDate(row.createdAt)
  if (!created) return '—'
  const ms = taskSlaMs(row)
  const deadline = new Date(created.getTime() + ms)
  return deadline.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export function buildParetoData(ncList, inspections, rangeDays, processFilter) {
  const inspMap = new Map(inspections.map((i) => [i.inspectionId, i]))
  const filtered = ncList.filter((n) => {
    if (!inDateRange(n.registeredAt, rangeDays)) return false
    if (!processFilter) return true
    const insp = inspMap.get(n.inspectionId)
    return insp && (insp.inspectionType === processFilter || processLabel(insp) === processFilter)
  })

  const map = {}
  const processMap = {}
  for (const n of filtered) {
    const type = n.defectType || '未分类'
    map[type] = (map[type] || 0) + (Number(n.quantity) || 1)
    const insp = inspMap.get(n.inspectionId)
    const proc = insp ? processLabel(insp) : '—'
    if (!processMap[type]) processMap[type] = new Set()
    if (proc !== '—') processMap[type].add(proc)
  }

  const entries = Object.entries(map)
    .map(([name, count]) => ({
      name,
      count,
      processes: [...(processMap[name] || [])]
    }))
    .sort((a, b) => b.count - a.count)

  const total = entries.reduce((s, e) => s + e.count, 0) || 1
  let cum = 0
  return entries.map((e) => {
    cum += e.count
    return {
      ...e,
      pct: Math.round((e.count / total) * 100),
      cumPct: Math.round((cum / total) * 100)
    }
  })
}

/** 样本不足或各缺陷数量相同 — 不展示帕累托 */
export function canShowPareto(entries, minTotal = 3) {
  if (!entries.length) return false
  const total = entries.reduce((s, e) => s + e.count, 0)
  if (total < minTotal) return false
  if (entries.length < 2) return false
  const distinct = new Set(entries.map((e) => e.count))
  return distinct.size > 1
}

export function sparklineOption(points, color = QC_COLORS.primary, valueSuffix = '') {
  const labels = points.map((p) => p.label)
  const values = points.map((p) => p.value)
  return {
    animation: true,
    grid: { left: 0, right: 0, top: 0, bottom: 0 },
    tooltip: {
      trigger: 'axis',
      confine: true,
      formatter(params) {
        const i = params[0]?.dataIndex ?? 0
        const p = points[i]
        if (!p) return ''
        return `${p.label}<br/>${p.value}${valueSuffix}`
      }
    },
    xAxis: { type: 'category', show: false, data: labels },
    yAxis: { type: 'value', show: false, min: 0 },
    series: [{
      type: 'line',
      data: values,
      smooth: true,
      symbol: 'circle',
      symbolSize: 3,
      showSymbol: false,
      emphasis: { focus: 'series', scale: true },
      lineStyle: { width: 1.2, color },
      itemStyle: { color },
      areaStyle: { color: `${color}18` }
    }]
  }
}

export function buildParetoOption(entries, selectedDefect = '') {
  const names = entries.map((e) => e.name)
  const counts = entries.map((e) => e.count)
  const cumPct = entries.map((e) => e.cumPct)

  return {
    animationDuration: 600,
    animationEasing: 'cubicOut',
    grid: { left: 8, right: 36, top: 28, bottom: 4, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        const i = params[0]?.dataIndex ?? 0
        const e = entries[i]
        if (!e) return ''
        const procs = e.processes?.length ? e.processes.join('、') : '—'
        return [
          `<strong>${e.name}</strong>`,
          `缺陷数量：${e.count}`,
          `占比：${e.pct}%`,
          `累计占比：${e.cumPct}%`,
          `关联工序：${procs}`
        ].join('<br/>')
      }
    },
    legend: {
      data: ['缺陷数', '累计占比'],
      top: 0,
      right: 0,
      itemWidth: 10,
      itemHeight: 8,
      textStyle: { color: QC_COLORS.muted, fontSize: 11 }
    },
    xAxis: {
      type: 'category',
      data: names,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: QC_COLORS.grid } },
      axisLabel: { color: QC_COLORS.muted, fontSize: 11, interval: 0, rotate: names.length > 5 ? 18 : 0 }
    },
    yAxis: [
      {
        type: 'value',
        minInterval: 1,
        splitLine: { lineStyle: { color: QC_COLORS.grid, type: 'dashed' } },
        axisLabel: { color: QC_COLORS.axis, fontSize: 11 }
      },
      {
        type: 'value',
        min: 0,
        max: 100,
        splitLine: { show: false },
        axisLabel: { color: QC_COLORS.axis, fontSize: 11, formatter: '{value}%' }
      }
    ],
    series: [
      {
        name: '缺陷数',
        type: 'bar',
        data: counts.map((v, i) => ({
          value: v,
          itemStyle: {
            color: entries[i].name === selectedDefect ? QC_COLORS.primary : QC_COLORS.bar,
            borderRadius: [2, 2, 0, 0]
          }
        })),
        barWidth: '42%',
        label: { show: true, position: 'top', color: QC_COLORS.text, fontSize: 11, fontWeight: 600 },
        emphasis: { focus: 'series' }
      },
      {
        name: '累计占比',
        type: 'line',
        yAxisIndex: 1,
        data: cumPct,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { color: QC_COLORS.line, width: 2 },
        itemStyle: { color: QC_COLORS.line },
        markLine: {
          silent: true,
          symbol: 'none',
          lineStyle: { color: QC_COLORS.recheck, type: 'dashed', width: 1 },
          data: [{ yAxis: 80, label: { formatter: '80%', color: QC_COLORS.recheck, fontSize: 10 } }]
        }
      }
    ]
  }
}

export function buildDonutOption(passRate, ringData, weekDelta) {
  const deltaSign = weekDelta > 0 ? '+' : ''
  const subColor = weekDelta >= 0 ? QC_COLORS.pass : QC_COLORS.muted
  return {
    animationDuration: 500,
    tooltip: { trigger: 'item', formatter: '{b}：{c} 单（{d}%）' },
    title: {
      text: `${passRate}%`,
      subtext: `环比 ${deltaSign}${weekDelta.toFixed(1)}%`,
      left: 'center',
      top: '34%',
      textStyle: { fontSize: 22, fontWeight: 700, color: QC_COLORS.text },
      subtextStyle: { fontSize: 11, color: subColor }
    },
    series: [{
      type: 'pie',
      radius: ['56%', '76%'],
      center: ['50%', '46%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      labelLine: { show: false },
      data: ringData
    }]
  }
}

/** 近7日：抽检柱状 + 合格率折线（无数据日 rate 为 null） */
export function buildSampleRateComboOption(dailyPoints) {
  const labels = dailyPoints.map((d) => d.label)
  const samples = dailyPoints.map((d) => (d.hasData ? d.sample : 0))
  const rates = dailyPoints.map((d) => (d.hasData ? d.rate : null))

  return {
    animationDuration: 500,
    grid: { left: 4, right: 4, top: 22, bottom: 4, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross', crossStyle: { color: QC_COLORS.axis } },
      formatter(params) {
        const i = params[0]?.dataIndex ?? 0
        const d = dailyPoints[i]
        if (!d) return ''
        if (!d.hasData) return `${d.label}<br/>暂无数据`
        return [
          d.label,
          `抽检数：${d.sample}`,
          `合格数：${d.qual}`,
          `合格率：${d.rate}%`
        ].join('<br/>')
      }
    },
    legend: {
      data: ['抽检数', '合格率'],
      top: 0,
      right: 0,
      itemWidth: 10,
      itemHeight: 8,
      textStyle: { color: QC_COLORS.muted, fontSize: 10 }
    },
    xAxis: {
      type: 'category',
      data: labels,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: QC_COLORS.grid } },
      axisLabel: {
        color: QC_COLORS.muted,
        fontSize: 10,
        formatter: (val, idx) => (dailyPoints[idx]?.hasData ? val : `${val}\n暂无`)
      }
    },
    yAxis: [
      {
        type: 'value',
        minInterval: 1,
        splitLine: { lineStyle: { color: QC_COLORS.grid, type: 'dashed' } },
        axisLabel: { color: QC_COLORS.axis, fontSize: 10 }
      },
      {
        type: 'value',
        min: 0,
        max: 100,
        splitLine: { show: false },
        axisLabel: { color: QC_COLORS.axis, fontSize: 10, formatter: '{value}%' }
      }
    ],
    series: [
      {
        name: '抽检数',
        type: 'bar',
        data: samples.map((v, i) => ({
          value: dailyPoints[i].hasData ? v : 0,
          itemStyle: {
            color: dailyPoints[i].hasData ? QC_COLORS.bar : '#f3f4f6',
            borderRadius: [2, 2, 0, 0]
          }
        })),
        barWidth: '46%',
        barGap: '30%'
      },
      {
        name: '合格率',
        type: 'line',
        yAxisIndex: 1,
        data: rates,
        connectNulls: false,
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: { color: QC_COLORS.pass, width: 2 },
        itemStyle: { color: QC_COLORS.pass }
      }
    ]
  }
}

export function buildDailyPassPoints(judgedRows, days = 7, end = new Date()) {
  const points = []
  for (let i = days - 1; i >= 0; i--) {
    const d = addDays(startOfDay(end), -i)
    const key = dayKey(d)
    const rows = judgedRows.filter((r) => dayKey(r.inspectedAt || r.updatedAt) === key)
    let sample = 0
    let qual = 0
    for (const r of rows) {
      sample += Number(r.sampleQuantity) || 0
      qual += Number(r.qualifiedQuantity) || 0
    }
    const hasData = rows.length > 0 && sample > 0
    points.push({
      key,
      label: formatDayLabel(key),
      sample,
      qual,
      rate: hasData ? Math.round((qual / sample) * 100) : null,
      hasData
    })
  }
  return points
}
