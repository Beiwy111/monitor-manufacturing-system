<template>
  <ModulePageShell>
    <div class="alarm-header">
      <span class="alarm-title">安灯报警</span>
      <el-tag type="info" size="small">生产车间设备报警 · 数据库活数据</el-tag>
      <el-button :loading="loading" style="margin-left:auto" @click="loadData">刷新</el-button>
    </div>

    <div class="alarm-stats">
      <span class="stat stat--danger">未闭环：<em>{{ openCount }}</em></span>
      <span class="stat stat--warn">待接收：<em>{{ pendingCount }}</em></span>
      <span class="stat">处理中：<em>{{ processingCount }}</em></span>
    </div>

    <div class="alarm-toolbar">
      <el-select v-model="statusFilter" clearable placeholder="状态" style="width: 120px">
        <el-option label="待接收" value="OPEN" />
        <el-option label="已接收" value="RECEIVED" />
        <el-option label="处理中" value="PROCESSING" />
        <el-option label="已关闭" value="CLOSED" />
      </el-select>
      <el-button v-if="canReportAlarm" type="danger" @click="showReport = true">上报报警</el-button>
    </div>

    <el-table v-loading="loading" :data="filteredAlarms" border stripe size="small" highlight-current-row @current-change="onRowClick">
      <el-table-column prop="alarmNo" label="报警号" width="130" />
      <el-table-column prop="alarmTypeCn" label="类型" width="72" align="center" />
      <el-table-column prop="parentStepName" label="工序" width="96" />
      <el-table-column prop="workshop" label="车间" width="130" show-overflow-tooltip />
      <el-table-column prop="equipmentName" label="设备" min-width="120" show-overflow-tooltip />
      <el-table-column label="级别" width="72" align="center">
        <template #default="{ row }">
          <el-tag :type="levelType(row.alarmLevel)" size="small">{{ row.alarmLevelCn }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="alarmDescription" label="描述" min-width="150" show-overflow-tooltip />
      <el-table-column prop="reportedAt" label="上报时间" width="150" />
      <el-table-column label="状态" width="88" align="center">
        <template #default="{ row }">
          <el-tag :type="alarmStatusType(row.alarmStatus)" size="small">{{ row.alarmStatusCn }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <template v-if="canHandleAlarm">
            <el-button v-if="row.alarmStatus==='OPEN'" link type="primary" size="small" :loading="acting" @click="doReceive(row)">接收</el-button>
            <el-button v-if="isOpen(row.alarmStatus)" link type="success" size="small" :loading="acting" @click="openResolve(row)">解除</el-button>
          </template>
          <span v-else class="alarm-readonly">—</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showReport" title="上报安灯" width="460px">
      <el-form label-width="88px" size="small">
        <el-form-item label="设备" required>
          <el-select v-model="reportForm.equipmentId" filterable placeholder="选择生产车间设备" style="width:100%">
            <el-option
              v-for="e in productionEquipments"
              :key="e.equipmentId"
              :label="`${e.equipmentCode} ${e.equipmentName}（${e.workshop}）`"
              :value="e.equipmentId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="报警类型">
          <el-select v-model="reportForm.alarmType" style="width:100%">
            <el-option label="设备" value="EQUIPMENT" />
            <el-option label="质量" value="QUALITY" />
            <el-option label="物料" value="MATERIAL" />
            <el-option label="工艺" value="PROCESS" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度">
          <el-radio-group v-model="reportForm.alarmLevel">
            <el-radio value="URGENT">紧急</el-radio>
            <el-radio value="IMPORTANT">重要</el-radio>
            <el-radio value="GENERAL">一般</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="reportForm.description" type="textarea" rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReport = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="report">上报</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resolveVisible" title="解除报警" width="420px">
      <el-input v-model="resolveResult" type="textarea" rows="3" placeholder="处理结果" />
      <template #footer>
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="doResolve">确认解除</el-button>
      </template>
    </el-dialog>
  </ModulePageShell>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import ModulePageShell from '@/components/module/ModulePageShell.vue'
import { moduleStatusType } from '@/constants/moduleStatus'
import {
  fetchEquipmentViews, fetchAlarmViews, receiveAlarm, resolveAlarm, triggerAlarm
} from '@/api/business'

const userStore = useUserStore()
const operator = computed(() => userStore.userInfo?.username || '')
const roleKey = computed(() => userStore.roleKey || '')
/** 操作员可上报；设备维修员仅接收/解除，不上报 */
const canReportAlarm = computed(() => ['operator', 'admin', 'manager'].includes(roleKey.value))
const canHandleAlarm = computed(() => ['device', 'admin'].includes(roleKey.value))

const loading = ref(false)
const acting = ref(false)
const alarms = ref([])
const equipments = ref([])
const statusFilter = ref('')
const showReport = ref(false)
const resolveVisible = ref(false)
const resolveTarget = ref(null)
const resolveResult = ref('')

const reportForm = reactive({
  equipmentId: null,
  alarmType: 'EQUIPMENT',
  alarmLevel: 'IMPORTANT',
  description: ''
})

const productionEquipments = computed(() =>
  equipments.value.filter((e) => e.isProductionWorkshop !== false)
)
const openCount = computed(() => alarms.value.filter((a) => isOpen(a.alarmStatus)).length)
const pendingCount = computed(() => alarms.value.filter((a) => a.alarmStatus === 'OPEN').length)
const processingCount = computed(() => alarms.value.filter((a) => a.alarmStatus === 'PROCESSING').length)
const filteredAlarms = computed(() => {
  if (!statusFilter.value) return alarms.value
  return alarms.value.filter((a) => a.alarmStatus === statusFilter.value)
})

function isOpen(s) {
  return s === 'OPEN' || s === 'RECEIVED' || s === 'PROCESSING'
}
function levelType(l) {
  return moduleStatusType('alarmLevel', l)
}
function alarmStatusType(s) {
  return moduleStatusType('alarmStatus', s)
}
function onRowClick() {}

async function loadData() {
  loading.value = true
  try {
    const [al, eq] = await Promise.all([fetchAlarmViews(), fetchEquipmentViews()])
    alarms.value = al || []
    equipments.value = eq || []
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function doReceive(row) {
  acting.value = true
  try {
    await receiveAlarm({ alarmId: row.alarmId, operator: operator.value })
    ElMessage.success('已接收')
    await loadData()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    acting.value = false
  }
}
function openResolve(row) {
  resolveTarget.value = row
  resolveResult.value = ''
  resolveVisible.value = true
}
async function doResolve() {
  if (!resolveResult.value.trim()) {
    ElMessage.warning('请填写处理结果')
    return
  }
  acting.value = true
  try {
    await resolveAlarm({ alarmId: resolveTarget.value.alarmId, remark: resolveResult.value, operator: operator.value })
    resolveVisible.value = false
    ElMessage.success('报警已解除')
    await loadData()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    acting.value = false
  }
}
async function report() {
  if (!reportForm.equipmentId || !reportForm.description.trim()) {
    ElMessage.warning('请选择设备并填写描述')
    return
  }
  acting.value = true
  try {
    await triggerAlarm({
      equipmentId: reportForm.equipmentId,
      alarmType: reportForm.alarmType,
      alarmLevel: reportForm.alarmLevel,
      description: reportForm.description,
      operator: operator.value
    })
    showReport.value = false
    ElMessage.success('报警已上报')
    await loadData()
  } catch (e) {
    ElMessage.error(e?.message || '上报失败')
  } finally {
    acting.value = false
  }
}
onMounted(loadData)
</script>

<style scoped>
.alarm-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.alarm-title { font-size: 18px; font-weight: 700; color: #001b3f; }
.alarm-stats { display: flex; gap: 16px; margin-bottom: 10px; font-size: 13px; color: #606266; }
.stat em { font-style: normal; font-weight: 700; margin-left: 4px; }
.stat--danger em { color: #f56c6c; }
.stat--warn em { color: #e6a23c; }
.alarm-toolbar { display: flex; gap: 8px; margin-bottom: 12px; }
.alarm-readonly { color: #c0c4cc; font-size: 13px; }
</style>
