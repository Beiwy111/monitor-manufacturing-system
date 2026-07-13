<template>
  <div class="att-cal">
    <!-- 工具栏 -->
    <div class="att-cal__toolbar">
      <el-date-picker
        v-model="month"
        type="month"
        value-format="YYYY-MM"
        placeholder="选择月份"
        :clearable="false"
        style="width: 132px"
        @change="load"
      />
      <el-select v-model="filter.department" placeholder="全部部门" clearable filterable style="width: 140px" @change="load">
        <el-option v-for="d in departmentOptions" :key="d" :label="d" :value="d" />
      </el-select>
      <el-select v-model="filter.realName" placeholder="全部人员" clearable filterable style="width: 150px" @change="load">
        <el-option v-for="p in personOptions" :key="p" :label="p" :value="p" />
      </el-select>
      <el-button type="primary" plain size="small" :loading="loading" @click="load">刷新</el-button>
      <div class="att-cal__legend">
        <span v-for="s in STATUS_LIST" :key="s.key" class="att-cal__legend-item">
          <i class="att-cal__dot" :style="{ background: s.color }" />
          {{ s.label }}
        </span>
      </div>
    </div>

    <!-- 主体：日历 + 明细 -->
    <div class="att-cal__body" v-loading="loading">
      <section class="att-cal__calendar">
        <div class="att-cal__weekdays">
          <span v-for="w in weekdays" :key="w">{{ w }}</span>
        </div>
        <div class="att-cal__grid">
          <div
            v-for="cell in calendarCells"
            :key="cell.key"
            class="att-cal__cell"
            :class="{
              'att-cal__cell--pad': !cell.dateStr,
              'att-cal__cell--today': cell.isToday,
              'att-cal__cell--active': selectedDate === cell.dateStr,
              'att-cal__cell--muted': cell.isOtherMonth
            }"
            @click="cell.dateStr && selectDate(cell.dateStr)"
          >
            <template v-if="cell.dateStr">
              <div class="att-cal__cell-top">
                <span class="att-cal__cell-day">{{ cell.day }}</span>
              </div>
              <div class="att-cal__cell-stats">
                <span
                  v-for="s in STATUS_LIST"
                  :key="s.key"
                  v-show="cellSummary(cell.dateStr)[s.key]"
                  class="att-cal__stat"
                  :style="{ color: s.color, borderColor: s.color + '33', background: s.color + '10' }"
                >
                  <el-icon :size="11"><component :is="s.icon" /></el-icon>
                  {{ cellSummary(cell.dateStr)[s.key] }}
                </span>
                <span v-if="!hasAnyStat(cell.dateStr)" class="att-cal__stat att-cal__stat--empty">—</span>
              </div>
            </template>
          </div>
        </div>
      </section>

      <aside class="att-cal__detail" :class="{ 'att-cal__detail--empty': !selectedDate }">
        <div class="att-cal__detail-head">
          <span class="att-cal__detail-title">{{ selectedDate ? `${selectedDate} 考勤明细` : '点击日期查看明细' }}</span>
          <span v-if="selectedDate" class="att-cal__detail-count">共 {{ dayRecords.length }} 人</span>
        </div>
        <div v-if="selectedDate" class="att-cal__detail-table">
          <el-table :data="dayRecords" size="small" stripe max-height="420" empty-text="当日无考勤记录">
            <el-table-column prop="realName" label="姓名" width="80" />
            <el-table-column prop="department" label="部门" min-width="100" show-overflow-tooltip />
            <el-table-column label="上班" width="72">
              <template #default="{ row }">{{ fmtTime(row.checkInTime) }}</template>
            </el-table-column>
            <el-table-column label="下班" width="72">
              <template #default="{ row }">{{ fmtTime(row.checkOutTime) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="68" align="center">
              <template #default="{ row }">
                <span class="att-cal__status-tag" :style="statusStyle(row.status)">{{ statusLabel(row.status) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="att-cal__detail-placeholder">在左侧日历中选择某一天，查看当天人员考勤明细</div>
      </aside>
    </div>

    <!-- 图表 -->
    <div class="att-cal__charts">
      <div class="att-cal__chart">
        <div class="att-cal__chart-label">每日出勤趋势</div>
        <BoardChart :option="lineOption" class="att-cal__chart-box" />
      </div>
      <div class="att-cal__chart">
        <div class="att-cal__chart-label">部门出勤率</div>
        <BoardChart :option="barOption" class="att-cal__chart-box" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { CircleCheck, Clock, Sunset, Document, Close } from '@element-plus/icons-vue'
import BoardChart from '@/components/board/BoardChart.vue'
import { getAttendanceRecords, getAttendanceStatistics } from '@/api/attendance'

const STATUS_LIST = [
  { key: 'NORMAL', label: '出勤', color: '#5b9a8b', icon: CircleCheck },
  { key: 'LATE', label: '迟到', color: '#c9a227', icon: Clock },
  { key: 'EARLY_LEAVE', label: '早退', color: '#c98b6d', icon: Sunset },
  { key: 'LEAVE', label: '请假', color: '#8b7ec8', icon: Document },
  { key: 'ABSENT', label: '缺勤', color: '#a0a4ab', icon: Close }
]

const weekdays = ['一', '二', '三', '四', '五', '六', '日']
const loading = ref(false)
const month = ref(new Date().toISOString().slice(0, 7))
const records = ref([])
const allMonthRecords = ref([])
const stats = ref({})
const selectedDate = ref('')
const filter = ref({ department: '', realName: '' })

const monthRange = computed(() => {
  const [y, m] = month.value.split('-').map(Number)
  const last = new Date(y, m, 0).getDate()
  return {
    startDate: `${y}-${String(m).padStart(2, '0')}-01`,
    endDate: `${y}-${String(m).padStart(2, '0')}-${String(last).padStart(2, '0')}`
  }
})

const departmentOptions = computed(() => {
  const set = new Set()
  allMonthRecords.value.forEach(r => { if (r.department) set.add(r.department) })
  return [...set].sort()
})

const personOptions = computed(() => {
  const set = new Set()
  const source = filter.value.department
    ? allMonthRecords.value.filter(r => r.department === filter.value.department)
    : allMonthRecords.value
  source.forEach(r => { if (r.realName) set.add(r.realName) })
  return [...set].sort()
})

const recordsByDate = computed(() => {
  const map = {}
  records.value.forEach(r => {
    const key = String(r.attendanceDate).slice(0, 10)
    if (!map[key]) map[key] = []
    map[key].push(r)
  })
  return map
})

const calendarCells = computed(() => {
  const [y, m] = month.value.split('-').map(Number)
  const first = new Date(y, m - 1, 1)
  const lastDay = new Date(y, m, 0).getDate()
  const startPad = (first.getDay() + 6) % 7
  const today = new Date().toISOString().slice(0, 10)
  const cells = []

  const prevMonthLast = new Date(y, m - 1, 0).getDate()
  for (let i = startPad - 1; i >= 0; i--) {
    const day = prevMonthLast - i
    const pm = m === 1 ? 12 : m - 1
    const py = m === 1 ? y - 1 : y
    const dateStr = `${py}-${String(pm).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    cells.push({ key: `p-${dateStr}`, dateStr, day: String(day).padStart(2, '0'), isToday: false, isOtherMonth: true })
  }

  for (let d = 1; d <= lastDay; d++) {
    const dateStr = `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    cells.push({ key: dateStr, dateStr, day: String(d).padStart(2, '0'), isToday: dateStr === today, isOtherMonth: false })
  }

  const tail = (7 - (cells.length % 7)) % 7
  const nm = m === 12 ? 1 : m + 1
  const ny = m === 12 ? y + 1 : y
  for (let d = 1; d <= tail; d++) {
    const dateStr = `${ny}-${String(nm).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    cells.push({ key: `n-${dateStr}`, dateStr, day: String(d).padStart(2, '0'), isToday: false, isOtherMonth: true })
  }
  return cells
})

const dayRecords = computed(() => {
  if (!selectedDate.value) return []
  return (recordsByDate.value[selectedDate.value] || []).slice().sort((a, b) => (a.realName || '').localeCompare(b.realName || ''))
})

function cellSummary(dateStr) {
  const items = recordsByDate.value[dateStr] || []
  const counts = { NORMAL: 0, LATE: 0, EARLY_LEAVE: 0, LEAVE: 0, ABSENT: 0 }
  items.forEach(r => {
    const st = r.status || 'ABSENT'
    if (counts[st] !== undefined) counts[st]++
  })
  return counts
}

function hasAnyStat(dateStr) {
  const c = cellSummary(dateStr)
  return STATUS_LIST.some(s => c[s.key] > 0)
}

function selectDate(dateStr) {
  selectedDate.value = dateStr
}

function statusLabel(status) {
  return STATUS_LIST.find(s => s.key === status)?.label || status
}

function statusStyle(status) {
  const meta = STATUS_LIST.find(s => s.key === status)
  if (!meta) return {}
  return { color: meta.color, background: meta.color + '14', borderColor: meta.color + '40' }
}

function fmtTime(val) {
  if (!val) return '—'
  return String(val).replace('T', ' ').slice(11, 16)
}

const lineOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 16, top: 20, bottom: 28 },
  xAxis: {
    type: 'category',
    data: stats.value.dailyDates || [],
    axisLine: { lineStyle: { color: '#e4e7ed' } },
    axisLabel: { color: '#909399', fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#f0f2f5' } },
    axisLabel: { color: '#909399', fontSize: 11 }
  },
  series: [{
    type: 'line',
    data: stats.value.dailyCounts || [],
    smooth: true,
    symbol: 'circle',
    symbolSize: 5,
    lineStyle: { color: '#5b9a8b', width: 2 },
    itemStyle: { color: '#5b9a8b' },
    areaStyle: { color: 'rgba(91,154,139,0.1)' }
  }]
}))

const barOption = computed(() => ({
  tooltip: { trigger: 'axis', formatter: '{b}: {c}%' },
  grid: { left: 40, right: 16, top: 20, bottom: 36 },
  xAxis: {
    type: 'category',
    data: stats.value.departmentNames || [],
    axisLine: { lineStyle: { color: '#e4e7ed' } },
    axisLabel: { color: '#909399', fontSize: 10, rotate: 18 }
  },
  yAxis: {
    type: 'value',
    max: 100,
    splitLine: { lineStyle: { color: '#f0f2f5' } },
    axisLabel: { color: '#909399', fontSize: 11, formatter: '{value}%' }
  },
  series: [{
    type: 'bar',
    data: stats.value.departmentRates || [],
    barMaxWidth: 28,
    itemStyle: {
      color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [{ offset: 0, color: '#5b9a8b' }, { offset: 1, color: '#8ec5b8' }] },
      borderRadius: [3, 3, 0, 0]
    }
  }]
}))

async function load() {
  loading.value = true
  try {
    const params = { ...monthRange.value }
    if (filter.value.department) params.department = filter.value.department
    if (filter.value.realName) params.realName = filter.value.realName
    const [recs, allRecs, st] = await Promise.all([
      getAttendanceRecords(params),
      getAttendanceRecords(monthRange.value),
      getAttendanceStatistics(month.value, {
        department: filter.value.department || undefined,
        realName: filter.value.realName || undefined
      })
    ])
    records.value = recs || []
    allMonthRecords.value = allRecs || []
    stats.value = st || {}
    if (selectedDate.value && !recordsByDate.value[selectedDate.value]?.length) {
      const today = new Date().toISOString().slice(0, 10)
      if (recordsByDate.value[today]?.length) selectedDate.value = today
      else if (records.value.length) {
        selectedDate.value = String(records.value[0].attendanceDate).slice(0, 10)
      }
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const today = new Date().toISOString().slice(0, 10)
  if (today.startsWith(month.value)) selectedDate.value = today
  load()
})
</script>

<style scoped>
.att-cal {
  --accent: #5b9a8b;
  --border: #e8ecf0;
  --bg: #f7f9fb;
  --text: #303133;
  --muted: #909399;
  background: var(--bg);
  padding: 12px;
  min-height: 100%;
}

.att-cal__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 4px;
  margin-bottom: 10px;
}

.att-cal__legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-left: auto;
  font-size: 12px;
  color: var(--muted);
}

.att-cal__legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.att-cal__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.att-cal__body {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 10px;
  margin-bottom: 10px;
}

.att-cal__calendar {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 4px;
  padding: 12px;
}

.att-cal__weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-size: 12px;
  color: var(--muted);
  font-weight: 500;
  margin-bottom: 6px;
}

.att-cal__grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  border-top: 1px solid var(--border);
  border-left: 1px solid var(--border);
}

.att-cal__cell {
  min-height: 88px;
  border-right: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  padding: 6px 5px;
  cursor: pointer;
  transition: background 0.12s;
  background: #fff;
}

.att-cal__cell--pad { cursor: default; background: #fafbfc; }
.att-cal__cell--muted { opacity: 0.45; }
.att-cal__cell:not(.att-cal__cell--pad):hover { background: #f5faf8; }
.att-cal__cell--today { background: #eef6f3; }
.att-cal__cell--active { box-shadow: inset 0 0 0 2px var(--accent); background: #f0f8f5; z-index: 1; }

.att-cal__cell-top {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 4px;
}

.att-cal__cell-day {
  font-size: 13px;
  color: var(--text);
  font-weight: 500;
}

.att-cal__cell-stats {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.att-cal__stat {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  padding: 1px 4px;
  border-radius: 3px;
  border: 1px solid;
  line-height: 1.4;
  width: fit-content;
}

.att-cal__stat--empty {
  color: #c0c4cc;
  border-color: transparent;
  background: transparent;
}

.att-cal__detail {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  min-height: 320px;
}

.att-cal__detail--empty .att-cal__detail-head { border-bottom: none; }

.att-cal__detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.att-cal__detail-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.att-cal__detail-count {
  font-size: 12px;
  color: var(--muted);
}

.att-cal__detail-table {
  flex: 1;
  overflow: hidden;
  padding: 0 4px 4px;
}

.att-cal__detail-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #c0c4cc;
  padding: 24px;
  text-align: center;
}

.att-cal__status-tag {
  display: inline-block;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 3px;
  border: 1px solid;
}

.att-cal__charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.att-cal__chart {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 4px;
  padding: 10px 12px 4px;
}

.att-cal__chart-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 2px;
}

.att-cal__chart-box {
  height: 200px;
}

@media (max-width: 1100px) {
  .att-cal__body {
    grid-template-columns: 1fr;
  }
  .att-cal__detail {
    min-height: 240px;
  }
}

@media (max-width: 768px) {
  .att-cal__legend {
    margin-left: 0;
    width: 100%;
  }
  .att-cal__charts {
    grid-template-columns: 1fr;
  }
  .att-cal__cell {
    min-height: 72px;
  }
}
</style>
