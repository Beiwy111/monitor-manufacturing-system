/** 工业看板 ECharts 主题与图表配置 */

const AXIS = {
  axisLine: { lineStyle: { color: 'rgba(0,180,255,0.2)' } },
  axisLabel: { color: '#6d8fb3', fontSize: 10 },
  splitLine: { lineStyle: { color: 'rgba(0,180,255,0.08)' } }
}

const LEGEND = {
  textStyle: { color: '#8fa8c8', fontSize: 10 },
  itemWidth: 10,
  itemHeight: 8
}

function kpiMap(kpi) {
  const map = {}
  ;(kpi || []).forEach((item) => {
    map[item.key] = item.value
  })
  return map
}

export function buildHeroProductionOption(hourlyOutputTrend, kpiMapObj = {}) {
  const list = hourlyOutputTrend || []
  const labels = list.map((i) => i.label || `${i.hour}:00`)
  const planned = Number(kpiMapObj.todayPlanned || 0)
  const actual = Number(kpiMapObj.todayActual || 0)
  const oee = Number(kpiMapObj.oee || 0)

  return {
    color: ['#ffd166', '#00c8ff', '#3dd598', '#ff9f43'],
    grid: { left: 56, right: 48, top: 48, bottom: 36 },
    title: {
      text: `今日计划 ${planned} 台 · 实际 ${actual} 台 · OEE ${oee}%`,
      left: 'center',
      top: 8,
      textStyle: { color: '#8fa8c8', fontSize: 13, fontWeight: 500 }
    },
    legend: { ...LEGEND, top: 32, data: ['计划产量', '实际产量', 'OEE', '累计实际'] },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { color: '#8fa8c8', fontSize: 11 },
      axisLine: { lineStyle: { color: 'rgba(0,180,255,0.25)' } }
    },
    yAxis: [
      {
        type: 'value',
        name: '产量(台)',
        nameTextStyle: { color: '#6d8fb3', fontSize: 11 },
        axisLabel: { color: '#8fa8c8', fontSize: 11 },
        splitLine: { lineStyle: { color: 'rgba(0,180,255,0.08)' } }
      },
      {
        type: 'value',
        name: 'OEE(%)',
        min: 0,
        max: 100,
        nameTextStyle: { color: '#6d8fb3', fontSize: 11 },
        axisLabel: { color: '#8fa8c8', fontSize: 11 },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '计划产量',
        type: 'bar',
        barWidth: 18,
        itemStyle: { color: '#ffd166', borderRadius: [3, 3, 0, 0] },
        data: list.map((i) => Number(i.planned || 0))
      },
      {
        name: '实际产量',
        type: 'bar',
        barWidth: 18,
        itemStyle: { color: '#00c8ff', borderRadius: [3, 3, 0, 0] },
        data: list.map((i) => Number(i.actual || 0))
      },
      {
        name: 'OEE',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { color: '#3dd598', width: 3 },
        itemStyle: { color: '#3dd598' },
        areaStyle: { color: 'rgba(61, 213, 152, 0.12)' },
        data: list.map((i) => {
          const a = Number(i.actual || 0)
          const p = Number(i.planned || 1)
          return p > 0 ? Math.min(100, Math.round((a / p) * 92 + 8)) : 0
        })
      },
      {
        name: '累计实际',
        type: 'line',
        smooth: true,
        symbol: 'none',
        lineStyle: { color: '#ff9f43', width: 2, type: 'dashed' },
        data: list.reduce((acc, i) => {
          const prev = acc.length ? acc[acc.length - 1] : 0
          acc.push(prev + Number(i.actual || 0))
          return acc
        }, [])
      }
    ]
  }
}

export function buildOutputTrendOption(hourlyOutputTrend) {
  const list = hourlyOutputTrend || []
  const labels = list.map((i) => i.label || `${i.hour}:00`)
  return {
    color: ['#ffd166', '#00c8ff', '#3dd598'],
    grid: { left: 42, right: 16, top: 32, bottom: 24 },
    legend: { ...LEGEND, data: ['计划产量', '实际产量', 'OEE'] },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: labels, ...AXIS },
    yAxis: [
      { type: 'value', name: '台', ...AXIS },
      { type: 'value', name: '%', min: 0, max: 100, ...AXIS }
    ],
    series: [
      {
        name: '计划产量',
        type: 'bar',
        barWidth: 10,
        itemStyle: { color: '#ffd166' },
        data: list.map((i) => Number(i.planned || 0))
      },
      {
        name: '实际产量',
        type: 'bar',
        barWidth: 10,
        itemStyle: { color: '#00c8ff' },
        data: list.map((i) => Number(i.actual || 0))
      },
      {
        name: 'OEE',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#3dd598', width: 2 },
        itemStyle: { color: '#3dd598' },
        data: list.map((i) => {
          const actual = Number(i.actual || 0)
          const planned = Number(i.planned || 1)
          const q = actual > 0 ? actual * 0.97 : 0
          const rate = planned > 0 ? Math.min(100, Math.round((actual / planned) * 92 + (q / actual) * 8)) : 0
          return rate
        })
      }
    ]
  }
}

export function buildPlanFulfillOption(workOrderProgress) {
  const list = (workOrderProgress || []).slice(0, 6)
  return {
    color: ['#00c8ff', '#ff9f43', '#3dd598'],
    grid: { left: 42, right: 42, top: 32, bottom: 24 },
    legend: { ...LEGEND, data: ['计划数量', '完成数量', '达成率'] },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: list.map((w) => w.workOrderNo?.slice(-6) || '-'),
      ...AXIS
    },
    yAxis: [
      { type: 'value', name: '台', ...AXIS },
      { type: 'value', name: '%', min: 0, max: 100, ...AXIS }
    ],
    series: [
      {
        name: '计划数量',
        type: 'bar',
        barWidth: 14,
        itemStyle: { color: '#00c8ff' },
        data: list.map((w) => Number(w.plannedQty || 0))
      },
      {
        name: '完成数量',
        type: 'bar',
        barWidth: 14,
        itemStyle: { color: '#ff9f43' },
        data: list.map((w) => Number(w.completedQty || 0))
      },
      {
        name: '达成率',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        label: { show: true, color: '#3dd598', fontSize: 10, formatter: '{c}%' },
        lineStyle: { color: '#3dd598' },
        itemStyle: { color: '#3dd598' },
        data: list.map((w) => w.progress || 0)
      }
    ]
  }
}

export function buildWipOption(stations) {
  const list = stations || []
  return {
    color: ['#00c8ff', '#ff9f43', '#3dd598'],
    grid: { left: 80, right: 16, top: 28, bottom: 20 },
    legend: { ...LEGEND, data: ['在制品 WIP', '小时产能'] },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', ...AXIS },
    yAxis: {
      type: 'category',
      data: list.map((s) => s.stationName),
      ...AXIS
    },
    series: [
      {
        name: '在制品 WIP',
        type: 'bar',
        barWidth: 8,
        itemStyle: { color: '#00c8ff' },
        data: list.map((s) => Number(s.currentQty || 0))
      },
      {
        name: '小时产能',
        type: 'bar',
        barWidth: 8,
        itemStyle: { color: '#ff9f43' },
        data: list.map((s) => Number(s.throughputPerHour || 0))
      }
    ]
  }
}

export function buildShiftOption(shiftCapacity) {
  const list = shiftCapacity || []
  return {
    color: ['#00c8ff', '#3dd598'],
    grid: { left: 42, right: 16, top: 28, bottom: 24 },
    legend: { ...LEGEND, data: ['计划产能', '实际产能'] },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: list.map((t) => t.teamName), ...AXIS },
    yAxis: { type: 'value', name: '台', ...AXIS },
    series: [
      {
        name: '计划产能',
        type: 'bar',
        barWidth: 16,
        itemStyle: { color: '#00c8ff' },
        data: list.map((t) => Number(t.plannedQty || 0))
      },
      {
        name: '实际产能',
        type: 'bar',
        barWidth: 16,
        itemStyle: { color: '#3dd598' },
        data: list.map((t) => Number(t.actualQty || 0))
      }
    ]
  }
}

export function buildYieldTrendOption(yieldTrend) {
  const list = yieldTrend || []
  return {
    color: ['#3dd598', '#ff6b6b'],
    grid: { left: 42, right: 16, top: 28, bottom: 24 },
    legend: { ...LEGEND, data: ['良品率', '不良数'] },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: list.map((i) => i.label), ...AXIS },
    yAxis: [
      { type: 'value', name: '%', min: 90, max: 100, ...AXIS },
      { type: 'value', name: '台', ...AXIS }
    ],
    series: [
      {
        name: '良品率',
        type: 'line',
        smooth: true,
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(61,213,152,0.35)' },
              { offset: 1, color: 'rgba(61,213,152,0.02)' }
            ]
          }
        },
        lineStyle: { color: '#3dd598' },
        itemStyle: { color: '#3dd598' },
        data: list.map((i) => i.yieldRate || 0)
      },
      {
        name: '不良数',
        type: 'bar',
        yAxisIndex: 1,
        barWidth: 8,
        itemStyle: { color: 'rgba(255,107,107,0.7)' },
        data: list.map((i) => Number(i.unqualified || 0))
      }
    ]
  }
}

export function buildDowntimeOption(downtimeReasons) {
  const list = (downtimeReasons || []).slice(0, 5)
  return {
    color: ['#00c8ff'],
    grid: { left: 90, right: 24, top: 16, bottom: 16 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', name: '分钟', ...AXIS },
    yAxis: { type: 'category', data: list.map((d) => d.reasonName), ...AXIS },
    series: [{
      type: 'bar',
      barWidth: 10,
      itemStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: '#0066aa' },
            { offset: 1, color: '#00c8ff' }
          ]
        }
      },
      data: list.map((d) => d.downtimeMinutes || 0)
    }]
  }
}

export function buildEquipmentGaugeOption(equipment, globalYield, globalOee) {
  const statusRate = (status) => {
    if (status === 'RUNNING') return 92
    if (status === 'IDLE') return 45
    if (status === 'FAULT') return 8
    return 60
  }
  const groups = {}
  ;(equipment || []).forEach((eq) => {
    const type = eq.equipmentName?.includes('贴附') ? '贴附机'
      : eq.equipmentName?.includes('组装') ? '组装线'
      : eq.equipmentName?.includes('老化') ? '老化架'
      : eq.equipmentName?.includes('包装') ? '包装线'
      : eq.equipmentName?.includes('调校') ? '调校台'
      : '其他设备'
    if (!groups[type]) groups[type] = { total: 0, running: 0, fault: 0 }
    groups[type].total++
    if (eq.status === 'RUNNING') groups[type].running++
    if (eq.status === 'FAULT') groups[type].fault++
  })

  const types = Object.keys(groups)
  const gauges = types.slice(0, 4).map((type, idx) => {
    const g = groups[type]
    const rate = g.total ? Math.round((g.running / g.total) * 100) : 0
    const col = idx % 2
    const row = Math.floor(idx / 2)
    return {
      type: 'gauge',
      center: [`${25 + col * 50}%`, `${35 + row * 45}%`],
      radius: '38%',
      min: 0,
      max: 100,
      progress: { show: true, width: 6, itemStyle: { color: '#00c8ff' } },
      axisLine: { lineStyle: { width: 6, color: [[1, 'rgba(0,180,255,0.15)']] } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      pointer: { show: false },
      title: { offsetCenter: [0, '72%'], fontSize: 10, color: '#8fa8c8' },
      detail: {
        valueAnimation: true,
        fontSize: 14,
        color: '#e8f4ff',
        offsetCenter: [0, '18%'],
        formatter: '{value}%'
      },
      data: [{ value: rate, name: type }]
    }
  })

  return {
    series: gauges.length ? gauges : [{
      type: 'gauge',
      center: ['50%', '55%'],
      radius: '60%',
      progress: { show: true, width: 8, itemStyle: { color: '#3dd598' } },
      axisLine: { lineStyle: { width: 8, color: [[1, 'rgba(61,213,152,0.15)']] } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      pointer: { show: false },
      detail: { fontSize: 18, color: '#e8f4ff', formatter: '{value}%', offsetCenter: [0, '10%'] },
      title: { offsetCenter: [0, '65%'], color: '#8fa8c8', fontSize: 11 },
      data: [{ value: globalOee || 0, name: '综合 OEE' }]
    }]
  }
}

export function equipmentCardMetrics(eq, globalYield, globalOee) {
  const runRate = eq.status === 'RUNNING' ? 95 : eq.status === 'IDLE' ? 38 : 5
  const yieldRate = globalYield || 97
  const perfRate = eq.status === 'RUNNING' ? 88 : 42
  const oee = Math.round((runRate * perfRate * yieldRate) / 10000)
  return { runRate, yieldRate, perfRate, oee: oee || globalOee || 0 }
}

export { kpiMap }
