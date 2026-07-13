<template>
  <div class="shift-calendar-page">
    <div class="calendar-toolbar">
      <el-date-picker
        v-model="currentMonth"
        type="month"
        placeholder="选择月份"
        value-format="YYYY-MM"
        style="width: 140px"
        @change="loadMonth"
      />
      <div class="calendar-legend">
        <span class="legend-item legend-item--day"><el-icon><Briefcase /></el-icon> 白班</span>
        <span class="legend-item legend-item--night"><el-icon><Moon /></el-icon> 夜班</span>
        <span class="legend-item legend-item--rest"><el-icon><Coffee /></el-icon> 休息</span>
      </div>
      <el-button type="primary" size="small" :loading="loading" @click="loadMonth">刷新</el-button>
    </div>

    <div class="calendar-grid" v-loading="loading">
      <div class="calendar-weekdays">
        <span v-for="d in weekdays" :key="d">{{ d }}</span>
      </div>
      <div class="calendar-days">
        <div
          v-for="cell in calendarCells"
          :key="cell.key"
          class="calendar-cell"
          :class="{
            'calendar-cell--empty': !cell.date,
            'calendar-cell--today': cell.isToday,
            'calendar-cell--selected': selectedDate === cell.dateStr
          }"
          @click="cell.date && openDay(cell.dateStr)"
        >
          <template v-if="cell.date">
            <div class="cell-header">
              <span class="cell-badge" :class="badgeClass(cell)">
                <el-icon v-if="cellSummary(cell).type === 'DAY'"><Briefcase /></el-icon>
                <el-icon v-else-if="cellSummary(cell).type === 'NIGHT'"><Moon /></el-icon>
                <el-icon v-else><Coffee /></el-icon>
                {{ cellSummary(cell).label }}
              </span>
              <span class="cell-date">{{ cell.day }}</span>
            </div>
            <div class="cell-body">
              <span v-if="cellSummary(cell).count > 0" class="cell-count">
                {{ cellSummary(cell).count }} 人在岗
              </span>
              <span v-else class="cell-empty">暂无排班</span>
            </div>
            <el-button link type="primary" size="small" class="cell-action" @click.stop="openDay(cell.dateStr)">
              安排排班
            </el-button>
          </template>
        </div>
      </div>
    </div>

    <el-dialog v-model="showDialog" :title="`排班安排 — ${selectedDate}`" width="720px" destroy-on-close>
      <div class="dialog-toolbar">
        <el-button type="primary" size="small" @click="addRow">添加员工</el-button>
        <span class="dialog-hint">为当日安排白班/夜班/休息，并指定负责车间</span>
      </div>
      <el-table :data="daySchedules" border stripe size="small" max-height="360">
        <el-table-column label="员工" min-width="160">
          <template #default="{ row }">
            <el-select
              v-model="row.userId"
              placeholder="选择员工"
              filterable
              style="width: 100%"
              @change="onUserChange(row)"
            >
              <el-option
                v-for="op in operators"
                :key="op.userId"
                :label="`${op.realName}（${op.employeeNo || op.username}）`"
                :value="op.userId"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="班次" width="120">
          <template #default="{ row }">
            <el-select v-model="row.shiftType" style="width: 100%">
              <el-option label="白班" value="DAY" />
              <el-option label="夜班" value="NIGHT" />
              <el-option label="休息" value="REST" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="负责车间" min-width="180">
          <template #default="{ row }">
            <el-select v-model="row.workshop" placeholder="选择车间" clearable filterable style="width: 100%" :disabled="row.shiftType === 'REST'">
              <el-option v-for="w in workshops" :key="w" :label="w" :value="w" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.remark" placeholder="备注" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeRow($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveDay">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Briefcase, Coffee, Moon } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getShiftSchedules, getShiftSchedulesByDate, saveShiftSchedule, deleteShiftSchedule, getOperators } from '@/api/attendance'
import { OPERATOR_BINDINGS } from '@/utils/operatorWorkshop'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const currentMonth = ref(new Date().toISOString().slice(0, 7))
const schedules = ref([])
const operators = ref([])
const showDialog = ref(false)
const selectedDate = ref('')
const daySchedules = ref([])

const weekdays = ['一', '二', '三', '四', '五', '六', '日']

const workshops = [...new Set(Object.values(OPERATOR_BINDINGS).map(b => b.workshopName))].sort()

const scheduleByDate = computed(() => {
  const map = {}
  for (const s of schedules.value) {
    const key = String(s.scheduleDate).slice(0, 10)
    if (!map[key]) map[key] = []
    map[key].push(s)
  }
  return map
})

const calendarCells = computed(() => {
  const [y, m] = currentMonth.value.split('-').map(Number)
  const firstDay = new Date(y, m - 1, 1)
  const lastDay = new Date(y, m, 0)
  const startPad = (firstDay.getDay() + 6) % 7
  const today = new Date().toISOString().slice(0, 10)
  const cells = []

  for (let i = 0; i < startPad; i++) {
    cells.push({ key: `pad-${i}`, date: null })
  }
  for (let d = 1; d <= lastDay.getDate(); d++) {
    const dateStr = `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    cells.push({
      key: dateStr,
      date: new Date(y, m - 1, d),
      dateStr,
      day: String(d).padStart(2, '0'),
      isToday: dateStr === today
    })
  }
  return cells
})

function cellSummary(cell) {
  const items = scheduleByDate.value[cell.dateStr] || []
  const working = items.filter(s => s.shiftType !== 'REST')
  if (!working.length) return { type: 'REST', label: '休', count: 0 }
  const night = working.filter(s => s.shiftType === 'NIGHT').length
  const day = working.length - night
  if (night > day) return { type: 'NIGHT', label: '夜', count: working.length }
  return { type: 'DAY', label: '班', count: working.length }
}

function badgeClass(cell) {
  const s = cellSummary(cell)
  return {
    'cell-badge--day': s.type === 'DAY',
    'cell-badge--night': s.type === 'NIGHT',
    'cell-badge--rest': s.type === 'REST'
  }
}

async function loadMonth() {
  loading.value = true
  try {
    const [y, m] = currentMonth.value.split('-').map(Number)
    const startDate = `${y}-${String(m).padStart(2, '0')}-01`
    const lastDay = new Date(y, m, 0).getDate()
    const endDate = `${y}-${String(m).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
    schedules.value = await getShiftSchedules({ startDate, endDate }) || []
  } finally {
    loading.value = false
  }
}

async function openDay(dateStr) {
  selectedDate.value = dateStr
  const items = await getShiftSchedulesByDate(dateStr) || []
  daySchedules.value = items.length
    ? items.map(s => ({
        scheduleId: s.scheduleId,
        userId: s.userId,
        shiftType: s.shiftType || 'DAY',
        workshop: s.workshop || '',
        remark: s.remark || ''
      }))
    : [newRow()]
  showDialog.value = true
}

function newRow() {
  return { scheduleId: null, userId: null, shiftType: 'DAY', workshop: '', remark: '' }
}

function addRow() {
  daySchedules.value.push(newRow())
}

function removeRow(index) {
  daySchedules.value.splice(index, 1)
}

function onUserChange(row) {
  const op = operators.value.find(o => o.userId === row.userId)
  if (!op) return
  const binding = OPERATOR_BINDINGS[op.username]
  if (binding && !row.workshop) {
    row.workshop = binding.workshopName
  }
}

async function saveDay() {
  const valid = daySchedules.value.filter(r => r.userId)
  if (!valid.length) {
    ElMessage.warning('请至少安排一名员工')
    return
  }
  saving.value = true
  try {
    const existing = await getShiftSchedulesByDate(selectedDate.value) || []
    for (const old of existing) {
      if (!valid.find(v => v.scheduleId === old.scheduleId)) {
        await deleteShiftSchedule(old.scheduleId)
      }
    }
    for (const row of valid) {
      await saveShiftSchedule({
        scheduleDate: selectedDate.value,
        userId: row.userId,
        shiftType: row.shiftType,
        workshop: row.workshop,
        remark: row.remark,
        createdBy: userStore.userInfo?.username
      })
    }
    ElMessage.success('排班已保存')
    showDialog.value = false
    await loadMonth()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  operators.value = await getOperators() || []
  await loadMonth()
})
</script>

<style scoped>
.shift-calendar-page { padding: 0 4px; }
.calendar-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.calendar-legend {
  display: flex;
  gap: 16px;
  flex: 1;
  font-size: 13px;
  color: #606266;
}
.legend-item { display: flex; align-items: center; gap: 4px; }
.legend-item--day { color: #409eff; }
.legend-item--night { color: #7c3aed; }
.legend-item--rest { color: #e6a23c; }
.calendar-grid {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 16px;
}
.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
  font-weight: 600;
}
.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}
.calendar-cell {
  min-height: 110px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 8px;
  cursor: pointer;
  transition: background 0.15s;
  display: flex;
  flex-direction: column;
}
.calendar-cell--empty { border: none; cursor: default; }
.calendar-cell:not(.calendar-cell--empty):hover { background: #f5f7fa; }
.calendar-cell--today { border-color: #409eff; background: #ecf5ff; }
.calendar-cell--selected { box-shadow: 0 0 0 2px #409eff; }
.cell-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.cell-badge {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  font-weight: 600;
}
.cell-badge--day { color: #409eff; }
.cell-badge--night { color: #7c3aed; }
.cell-badge--rest { color: #e6a23c; }
.cell-date { font-size: 13px; color: #909399; }
.cell-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #606266;
}
.cell-count { font-weight: 600; color: #303133; }
.cell-empty { color: #c0c4cc; }
.cell-action { margin-top: auto; align-self: center; }
.dialog-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.dialog-hint { font-size: 12px; color: #909399; }
</style>
