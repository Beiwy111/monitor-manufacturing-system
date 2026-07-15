import { computed } from 'vue'
import { ROLE_QUICK_LINKS, TASK_CARD_COLORS, CALENDAR_MARK_COLORS } from '@/constants/workbenchLayout'

const DATE_KEYS = ['deliveryDate', 'planStart', 'planEnd', 'reportDate', 'operatedAt', 'createdAt']

function pad2(n) {
  return String(n).padStart(2, '0')
}

function toDateKey(d) {
  if (!d) return ''
  if (typeof d === 'string') {
    const m = d.match(/^(\d{4})-(\d{2})-(\d{2})/)
    if (m) return `${m[1]}-${m[2]}-${m[3]}`
    const md = d.match(/^(\d{1,2})\/(\d{1,2})$/)
    if (md) {
      const y = new Date().getFullYear()
      return `${y}-${pad2(md[1])}-${pad2(md[2])}`
    }
  }
  if (d instanceof Date) return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
  return ''
}

function parseMonthDayLabel(label, year) {
  const m = String(label).match(/^(\d{1,2})\/(\d{1,2})$/)
  if (!m) return ''
  return `${year}-${pad2(m[1])}-${pad2(m[2])}`
}

function daysLeft(dateStr) {
  const key = toDateKey(dateStr)
  if (!key) return null
  const target = new Date(key)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  target.setHours(0, 0, 0, 0)
  return Math.ceil((target - today) / 86400000)
}

function rowTitle(row) {
  return row.orderNo || row.dispatchNo || row.planNo || row.settlementNo || row.reportNo || row.label || '任务'
}

function rowSubtitle(row) {
  return row.customerName || row.productModel || row.workOrderNo || row.status || row.tag || ''
}

function calcProgress(row) {
  const plan = Number(row.planQty ?? row.quantity ?? row.max ?? 0)
  const done = Number(row.completedQty ?? row.value ?? 0)
  if (plan > 0) return Math.min(100, Math.round((done / plan) * 100))
  if (row.status?.includes('完成') || row.status === 'COMPLETED') return 100
  if (row.status?.includes('进行') || row.status?.includes('生产')) return 55
  if (row.status?.includes('待') || row.status?.includes('草稿')) return 15
  return 35
}

function extractCalendarEvents(data) {
  const events = []
  const year = new Date().getFullYear()

  for (const tbl of data?.tables || []) {
    for (const row of tbl.rows || []) {
      for (const key of DATE_KEYS) {
        const dk = toDateKey(row[key])
        if (!dk) continue
        events.push({
          date: dk,
          title: rowTitle(row),
          subtitle: rowSubtitle(row),
          link: tbl.link,
          type: key,
          status: row.status
        })
      }
    }
  }

  for (const p of data?.panels || []) {
    if ((p.type === 'line' || p.type === 'combo') && p.categories?.length) {
      const vals = p.series?.[0]?.data || []
      p.categories.forEach((label, i) => {
        const count = Number(vals[i] || 0)
        if (count <= 0) return
        const dk = parseMonthDayLabel(label, year)
        if (!dk) return
        events.push({
          date: dk,
          title: `${p.title} · ${count}`,
          subtitle: p.series?.[0]?.name || '',
          link: p.link,
          type: 'trend',
          count
        })
      })
    }
  }

  return events
}

function buildNotifications(data) {
  const list = []
  let id = 0

  for (const m of data?.metrics || []) {
    if (m.status === 'warn' || m.status === 'danger' || m.warn) {
      list.push({
        id: ++id,
        title: m.label,
        content: `${m.value}${m.suffix || ''}${m.delta ? ` · ${m.delta}` : ''}`,
        time: '刚刚',
        link: m.link,
        status: m.status || (m.warn ? 'warn' : 'normal'),
        pinned: m.status === 'danger'
      })
    }
  }

  for (const p of data?.panels || []) {
    if (p.type !== 'statusList') continue
    for (const s of p.statusList || []) {
      if (s.status === 'normal' && !String(s.tag || '').includes('预警')) continue
      list.push({
        id: ++id,
        title: s.label,
        content: s.tag || s.value || '',
        time: s.value || '',
        link: p.link,
        status: s.status || 'warn',
        pinned: s.status === 'danger'
      })
    }
  }

  for (const tbl of data?.tables || []) {
    for (const row of (tbl.rows || []).slice(0, 3)) {
      list.push({
        id: ++id,
        title: rowTitle(row),
        content: rowSubtitle(row) || tbl.title,
        time: row.deliveryDate || row.reportDate || row.createdAt || '',
        link: tbl.link,
        status: 'normal',
        pinned: false
      })
    }
  }

  return list.slice(0, 12)
}

export function useWorkbenchLayout(dataRef, theme, userStore, selectedDateRef, calendarMonthRef) {
  const quickLinks = computed(() => ROLE_QUICK_LINKS[theme] || [])

  const profile = computed(() => {
    const d = dataRef.value || {}
    const metrics = d.metrics || []
    const pending = metrics.find((m) => /待|pending/i.test(m.key + m.label))
    return {
      name: userStore?.displayName || '用户',
      role: d.title || userStore?.userInfo?.roleName || '工作台',
      online: true,
      todayTasks: pending ? pending.value : metrics.reduce((s, m) => s + (Number(m.value) || 0), 0),
      email: userStore?.userInfo?.email,
      phone: userStore?.userInfo?.phone
    }
  })

  const personalStats = computed(() => (dataRef.value?.metrics || []).slice(0, 5))

  const calendarEvents = computed(() => extractCalendarEvents(dataRef.value))

  const calendarMarks = computed(() => {
    const map = {}
    for (const e of calendarEvents.value) {
      if (!e.date) continue
      if (!map[e.date]) map[e.date] = { count: 0, level: 0, color: CALENDAR_MARK_COLORS[0] }
      map[e.date].count += 1
      map[e.date].level = Math.min(3, map[e.date].count)
      map[e.date].color = CALENDAR_MARK_COLORS[(map[e.date].count - 1) % CALENDAR_MARK_COLORS.length]
    }
    return map
  })

  const dayTasks = computed(() => {
    const key = selectedDateRef.value
    if (!key) return []
    return calendarEvents.value.filter((e) => e.date === key)
  })

  const ongoingTasks = computed(() => {
    const tasks = []
    const tables = dataRef.value?.tables || []
    for (const tbl of tables) {
      for (const row of (tbl.rows || []).slice(0, 4)) {
        const dl = daysLeft(row.deliveryDate || row.planEnd)
        tasks.push({
          title: rowTitle(row),
          subtitle: rowSubtitle(row),
          progress: calcProgress(row),
          daysLeft: dl,
          daysLabel: dl == null ? '' : dl < 0 ? `逾期 ${Math.abs(dl)} 天` : dl === 0 ? '今日' : `${dl} 天`,
          color: TASK_CARD_COLORS[tasks.length % TASK_CARD_COLORS.length],
          link: tbl.link,
          status: row.status
        })
      }
    }
    if (tasks.length) return tasks.slice(0, 4)

    const progressPanel = (dataRef.value?.panels || []).find((p) => p.type === 'progressList')
    if (progressPanel?.progress?.length) {
      return progressPanel.progress.slice(0, 4).map((p, i) => ({
        title: p.label,
        subtitle: `${p.value}/${p.max}`,
        progress: Math.min(100, Math.round((Number(p.value) / Math.max(1, Number(p.max))) * 100)),
        daysLeft: null,
        daysLabel: '',
        color: TASK_CARD_COLORS[i % TASK_CARD_COLORS.length],
        link: p.link,
        status: p.status
      }))
    }
    return []
  })

  const notifications = computed(() => buildNotifications(dataRef.value))

  const chartPanels = computed(() => {
    const panels = dataRef.value?.panels || dataRef.value?.charts || []
    return panels
      .filter((p) => ['donut', 'pie', 'line', 'combo', 'horizontalBar'].includes(p.type))
      .slice(0, 3)
  })

  const timelineEvents = computed(() => {
    return calendarEvents.value
      .filter((e) => {
        const dl = daysLeft(e.date)
        return dl != null && dl >= 0 && dl <= 14
      })
      .sort((a, b) => a.date.localeCompare(b.date))
      .slice(0, 6)
  })

  return {
    quickLinks,
    profile,
    personalStats,
    calendarEvents,
    calendarMarks,
    dayTasks,
    ongoingTasks,
    notifications,
    chartPanels,
    timelineEvents
  }
}

export function buildMonthGrid(year, month) {
  const first = new Date(year, month, 1)
  const last = new Date(year, month + 1, 0)
  const startPad = first.getDay()
  const days = []
  for (let i = 0; i < startPad; i++) days.push(null)
  for (let d = 1; d <= last.getDate(); d++) {
    days.push(`${year}-${pad2(month + 1)}-${pad2(d)}`)
  }
  return days
}

export function todayKey() {
  const t = new Date()
  return `${t.getFullYear()}-${pad2(t.getMonth() + 1)}-${pad2(t.getDate())}`
}
