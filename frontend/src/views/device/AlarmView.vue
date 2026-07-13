<template>
  <div class="av-wrap">
    <!-- KPI 横条 -->
    <div class="av-kpi-bar">
      <div class="av-kpi-card av-kpi-card--danger">
        <span class="av-kpi-card__val">{{ kpiComputed.open }}</span>
        <span class="av-kpi-card__label">待接收</span>
      </div>
      <div class="av-kpi-card av-kpi-card--warning">
        <span class="av-kpi-card__val">{{ kpiComputed.processing }}</span>
        <span class="av-kpi-card__label">处理中</span>
      </div>
      <div class="av-kpi-card av-kpi-card--primary">
        <span class="av-kpi-card__val">{{ kpiComputed.today }}</span>
        <span class="av-kpi-card__label">今日新增</span>
      </div>
      <div class="av-kpi-card av-kpi-card--success">
        <span class="av-kpi-card__val">{{ kpiComputed.closedMonth }}</span>
        <span class="av-kpi-card__label">本月已关闭</span>
      </div>
      <div class="av-kpi-card">
        <span class="av-kpi-card__val">
          {{ kpiComputed.avgResponse }}<span v-if="kpiComputed.avgResponse !== '—'" class="av-kpi-card__unit">min</span>
        </span>
        <span class="av-kpi-card__label">平均响应</span>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="av-toolbar">
      <div class="av-toolbar__filters">
        <el-select v-model="filterStatus" clearable placeholder="状态" style="width:110px" @change="goPage1">
          <el-option label="待接收" value="OPEN" />
          <el-option label="已接收" value="RECEIVED" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已关闭" value="CLOSED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-select v-model="filterLevel" clearable placeholder="级别" style="width:100px" @change="goPage1">
          <el-option label="🔴 紧急"  value="URGENT" />
          <el-option label="🟠 重要"  value="IMPORTANT" />
          <el-option label="🔵 一般"  value="GENERAL" />
        </el-select>
        <el-select v-model="filterType" clearable placeholder="类型" style="width:110px" @change="goPage1">
          <el-option label="设备异常" value="EQUIPMENT" />
          <el-option label="质量异常" value="QUALITY" />
          <el-option label="物料异常" value="MATERIAL" />
          <el-option label="工艺异常" value="PROCESS" />
          <el-option label="安全隐患" value="SAFETY" />
          <el-option label="其他"     value="OTHER" />
        </el-select>
        <el-input v-model="searchKw" clearable placeholder="搜索单号/设备/描述" style="width:200px" @input="goPage1">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <div class="av-toolbar__actions">
        <el-button :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <!-- 主表格 -->
    <div v-loading="loading" class="av-table-wrap">
      <el-empty v-if="!loading && !paged.length" description="暂无报警记录" :image-size="80" />
      <el-table v-else :data="paged" border stripe size="small"
        highlight-current-row :row-class-name="rowClass" @current-change="onRowChange">
        <el-table-column prop="alarmNo" label="报警号" width="130" />
        <el-table-column label="设备" min-width="160">
          <template #default="{ row }">
            <span class="av-eq-code">{{ row.equipmentCode }}</span>
            <span class="av-eq-name">{{ row.equipmentName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">{{ row.alarmTypeCn }}</template>
        </el-table-column>
        <el-table-column label="级别" width="82" align="center">
          <template #default="{ row }">
            <span class="av-level" :class="`av-level--${(row.alarmLevel||'').toLowerCase()}`">
              {{ LEVEL_ICON[row.alarmLevel] }} {{ row.alarmLevelCn }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="alarmDescription" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="88" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.alarmStatus)" size="small">{{ row.alarmStatusCn }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reporterName" label="上报人" width="80" align="center" />
        <el-table-column prop="reportedAt"   label="上报时间" width="148" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.alarmStatus==='OPEN'" link type="primary" size="small"
              :loading="acting" @click.stop="quickReceive(row)">接收</el-button>
            <el-button v-if="isOpen(row.alarmStatus)" link type="warning" size="small"
              @click.stop="openStartMaint(row)">维保</el-button>
            <el-button v-if="isOpen(row.alarmStatus)" link type="danger" size="small"
              :loading="acting" @click.stop="openResolveDialog(row)">解除</el-button>
            <el-button link type="info" size="small" @click.stop="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="av-pagination">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize"
          :total="filtered.length" :page-sizes="[15,30,50]"
          layout="total, sizes, prev, pager, next" background small />
      </div>
    </div>

    <!-- 详情抽屉 -->
    <AlarmDetailDrawer v-model:visible="drawerVisible" :alarm="currentAlarm"
      @refresh="loadAll" @start-maint="openStartMaint" />

    <!-- 解除确认弹窗 -->
    <el-dialog v-model="resolveVisible" title="解除报警" width="400px" :close-on-click-modal="false">
      <div v-if="resolveTarget" class="rv-alarm-info">
        <el-tag :type="statusTagType(resolveTarget.alarmStatus)" size="small">{{ resolveTarget.alarmStatusCn }}</el-tag>
        <span class="rv-alarm-no">{{ resolveTarget.alarmNo }}</span>
        <span class="rv-alarm-eq">{{ resolveTarget.equipmentName }}</span>
      </div>
      <el-form label-width="80px" style="margin-top:12px">
        <el-form-item label="解除原因">
          <el-input v-model="resolveRemark" type="textarea" :rows="3"
            placeholder="如：误触发、已手动修复、不影响生产…" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveVisible=false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="doResolve">确认解除</el-button>
      </template>
    </el-dialog>

    <!-- 开始维保弹窗 -->
    <el-dialog v-model="maintVisible" title="开始维保" width="460px" :close-on-click-modal="false">
      <el-form :model="maintForm" label-width="80px">
        <el-form-item label="设备">{{ maintTarget?.equipmentName }}</el-form-item>
        <el-form-item label="维护类型">
          <el-select v-model="maintForm.maintenanceType" style="width:100%">
            <el-option label="维修" value="REPAIR" />
            <el-option label="点检" value="INSPECTION" />
            <el-option label="保养" value="MAINTENANCE" />
            <el-option label="大修" value="OVERHAUL" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障描述">
          <el-input v-model="maintForm.faultDescription" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="维护内容">
          <el-input v-model="maintForm.maintenanceContent" type="textarea" :rows="2" placeholder="计划维护内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="maintVisible=false">取消</el-button>
        <el-button type="warning" :loading="acting" @click="doStartMaint">开始维保</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  fetchAlarmViews, fetchEquipmentKpi,
  receiveAlarm, resolveAlarm, startMaintenance,
} from '@/api/business'
import AlarmDetailDrawer from './AlarmDetailDrawer.vue'

const LEVEL_ICON = { URGENT: '🔴', IMPORTANT: '🟠', GENERAL: '🔵' }
const userStore  = useUserStore()
const operator   = computed(() => userStore.userInfo?.username || '')
const loading    = ref(false)
const acting     = ref(false)
const alarms     = ref([])

const filterStatus = ref(''), filterLevel = ref(''), filterType = ref(''), searchKw = ref('')
const page = ref(1), pageSize = ref(15)

const filtered = computed(() => {
  let list = alarms.value
  if (filterStatus.value) list = list.filter(a => a.alarmStatus === filterStatus.value)
  if (filterLevel.value)  list = list.filter(a => a.alarmLevel  === filterLevel.value)
  if (filterType.value)   list = list.filter(a => a.alarmType   === filterType.value)
  if (searchKw.value.trim()) {
    const kw = searchKw.value.trim()
    list = list.filter(a => a.alarmNo?.includes(kw) || a.equipmentName?.includes(kw) ||
      a.equipmentCode?.includes(kw) || a.alarmDescription?.includes(kw))
  }
  return list
})
const paged = computed(() => { const s = (page.value-1)*pageSize.value; return filtered.value.slice(s, s+pageSize.value) })
function goPage1() { page.value = 1 }

const kpiComputed = computed(() => {
  const list = alarms.value
  const today = new Date().toDateString()
  const thisMonth = new Date().toISOString().slice(0,7)
  const open        = list.filter(a => a.alarmStatus === 'OPEN').length
  const processing  = list.filter(a => a.alarmStatus === 'RECEIVED' || a.alarmStatus === 'PROCESSING').length
  const todayNew    = list.filter(a => a.reportedAt && new Date(a.reportedAt).toDateString() === today).length
  const closedMonth = list.filter(a => a.alarmStatus === 'CLOSED' && a.closedAt?.startsWith(thisMonth)).length
  const received    = list.filter(a => a.receivedAt && a.reportedAt)
  const avgResponse = received.length === 0 ? '—' : Math.round(
    received.reduce((s,a) => s + (new Date(a.receivedAt) - new Date(a.reportedAt)) / 60000, 0) / received.length
  )
  return { open, processing, today: todayNew, closedMonth, avgResponse }
})

const drawerVisible = ref(false), currentAlarm = ref(null)
function onRowChange(row) { if (row) { currentAlarm.value = row; drawerVisible.value = true } }
function openDetail(row)  { currentAlarm.value = row; drawerVisible.value = true }

const resolveVisible = ref(false), resolveTarget = ref(null), resolveRemark = ref('')
function openResolveDialog(row) { resolveTarget.value = row; resolveRemark.value = ''; resolveVisible.value = true }
async function doResolve() {
  acting.value = true
  try {
    await resolveAlarm({ alarmId: resolveTarget.value.alarmId, remark: resolveRemark.value||'手动解除', operator: operator.value })
    ElMessage.success('报警已解除'); resolveVisible.value = false; await loadAll()
  } catch(e) { ElMessage.error(e?.message||'解除失败') } finally { acting.value = false }
}

async function quickReceive(row) {
  acting.value = true
  try {
    await receiveAlarm({ alarmId: row.alarmId, operator: operator.value })
    ElMessage.success('已接收报警'); await loadAll()
  } catch(e) { ElMessage.error(e?.message||'接收失败') } finally { acting.value = false }
}

const maintVisible = ref(false), maintTarget = ref(null)
const maintForm = reactive({ maintenanceType: 'REPAIR', faultDescription: '', maintenanceContent: '' })
function openStartMaint(row) {
  maintTarget.value = row; maintForm.maintenanceType = 'REPAIR'
  maintForm.faultDescription = row.alarmDescription||''; maintForm.maintenanceContent = ''
  maintVisible.value = true
}
async function doStartMaint() {
  acting.value = true
  try {
    await startMaintenance({ equipmentId: maintTarget.value.equipmentId, alarmId: maintTarget.value.alarmId, ...maintForm, operator: operator.value })
    ElMessage.success('已开始维保，设备转为维保中'); maintVisible.value = false; await loadAll()
  } catch(e) { ElMessage.error(e?.message||'开始维保失败') } finally { acting.value = false }
}

function isOpen(s) { return s==='OPEN'||s==='RECEIVED'||s==='PROCESSING' }
function statusTagType(s) { return {OPEN:'danger',RECEIVED:'warning',PROCESSING:'primary',CLOSED:'success',CANCELLED:'info'}[s]??'info' }
function rowClass({ row }) {
  const c = []
  if (row.alarmStatus==='OPEN') c.push('row-open')
  if (row.alarmLevel==='URGENT') c.push('row-urgent')
  if (row.alarmLevel==='IMPORTANT') c.push('row-important')
  return c.join(' ')
}

async function loadAll() {
  loading.value = true
  try {
    const [al] = await Promise.all([fetchAlarmViews(), fetchEquipmentKpi()])
    alarms.value = Array.isArray(al) ? al : (al?.data ?? [])
  } catch(e) { ElMessage.error(e?.message||'加载失败') } finally { loading.value = false }
}
onMounted(loadAll)
</script>

<style scoped>
.av-wrap { padding:14px 18px; display:flex; flex-direction:column; gap:12px; }
.av-kpi-bar { display:flex; gap:10px; flex-wrap:wrap; }
.av-kpi-card { flex:1; min-width:100px; background:#fff; border-radius:10px; border:1px solid #eef1f5; padding:10px 16px; display:flex; flex-direction:column; align-items:center; gap:4px; box-shadow:0 1px 4px rgba(0,27,63,.05); }
.av-kpi-card__val  { font-size:24px; font-weight:800; color:#001b3f; line-height:1; }
.av-kpi-card__unit { font-size:12px; font-weight:400; color:#909399; margin-left:2px; }
.av-kpi-card__label { font-size:11px; color:#909399; }
.av-kpi-card--danger  .av-kpi-card__val { color:#ef4444; }
.av-kpi-card--warning .av-kpi-card__val { color:#f59e0b; }
.av-kpi-card--primary .av-kpi-card__val { color:#3b82f6; }
.av-kpi-card--success .av-kpi-card__val { color:#52b788; }
.av-toolbar { display:flex; align-items:center; justify-content:space-between; gap:10px; flex-wrap:wrap; background:#fff; border-radius:8px; padding:10px 14px; border:1px solid #eef1f5; }
.av-toolbar__filters { display:flex; gap:8px; flex-wrap:wrap; align-items:center; }
.av-toolbar__actions { display:flex; gap:8px; }
.av-table-wrap { background:#fff; border-radius:8px; border:1px solid #eef1f5; padding:0 0 12px; overflow:hidden; }
.av-eq-code { font-size:11px; color:#8090a8; font-family:monospace; background:#f0f4f8; padding:1px 5px; border-radius:3px; margin-right:4px; }
.av-eq-name { font-size:13px; color:#1a2a40; }
.av-level { font-size:12px; font-weight:600; }
.av-level--urgent    { color:#ef4444; }
.av-level--important { color:#f59e0b; }
.av-level--general   { color:#3b82f6; }
:deep(.row-open)      { background:#fff5f5 !important; }
:deep(.row-urgent td:first-child)    { border-left:3px solid #ef4444 !important; }
:deep(.row-important td:first-child) { border-left:3px solid #f59e0b !important; }
.av-pagination { padding:10px 14px 0; display:flex; justify-content:flex-end; }
.rv-alarm-info { display:flex; align-items:center; gap:8px; background:#f7faff; border-radius:6px; padding:8px 12px; }
.rv-alarm-no { font-size:13px; font-weight:700; font-family:monospace; color:#1a2a40; }
.rv-alarm-eq { font-size:12px; color:#6b7a90; }
</style>
