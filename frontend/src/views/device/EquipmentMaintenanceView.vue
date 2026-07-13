<template>
  <ModulePageShell>
    <div class="dm-header">
      <div class="dm-header__left">
        <span class="dm-title">故障处理台</span>
        <el-tag type="info" size="small" style="margin-left:10px">聚焦待处理故障与未闭环报警</el-tag>
      </div>
      <el-button :loading="loading" @click="loadAll">刷新</el-button>
    </div>

    <!-- KPI -->
    <KpiStrip :cards="kpiCards" :metrics="kpi" />

    <!-- 待处理设备（故障 + 维保中） -->
    <div v-loading="loading" class="dm-card">
      <div class="dm-card__title">
        待处理设备
        <el-tag type="danger" size="small" effect="plain" style="margin-left:8px">{{ pendingEquipments.length }}</el-tag>
      </div>
      <el-empty v-if="!loading && !pendingEquipments.length" description="暂无待处理设备" :image-size="80" />
      <el-table v-else :data="pendingEquipments" border stripe size="small">
        <el-table-column prop="equipmentCode" label="编码" width="100" />
        <el-table-column prop="equipmentName" label="设备名称" min-width="150" />
        <el-table-column prop="workshop" label="车间" width="100" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="eqStatusType(row.status)" size="small">{{ row.statusCn }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="未闭环报警" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.openAlarmCount > 0" type="danger" size="small">{{ row.openAlarmCount }}</el-tag>
            <span v-else style="color:#bbb">0</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastMaintenanceAt" label="最近维护" width="150" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status==='FAULT'" link type="warning" size="small" @click="openStart(row)">开始维保</el-button>
            <el-button v-if="row.status==='MAINTAINING' && row.activeMaintenanceId" link type="success" size="small"
              @click="openFinish(row)">完成维保</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 未闭环安灯报警 -->
    <div v-loading="loading" class="dm-card">
      <div class="dm-card__title">
        未闭环报警
        <el-tag type="warning" size="small" effect="plain" style="margin-left:8px">{{ openAlarms.length }}</el-tag>
      </div>
      <el-empty v-if="!loading && !openAlarms.length" description="所有报警已闭环" :image-size="80" />
      <el-table v-else :data="openAlarms" border stripe size="small">
        <el-table-column prop="alarmNo" label="报警号" width="130" />
        <el-table-column prop="equipmentName" label="设备" min-width="130" />
        <el-table-column prop="alarmTypeCn" label="类型" width="70" align="center" />
        <el-table-column label="级别" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="levelType(row.alarmLevel)" size="small">{{ row.alarmLevelCn }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alarmDescription" label="描述" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="alarmStatusType(row.alarmStatus)" size="small">{{ row.alarmStatusCn }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reporterName" label="上报人" width="80" />
        <el-table-column prop="reportedAt" label="上报时间" width="150" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.alarmStatus==='OPEN'" link type="primary" size="small"
              :loading="acting" @click="doReceive(row)">接收</el-button>
            <el-button link type="info" size="small"
              :loading="acting" @click="openResolve(row)">解除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 开始维保弹窗 -->
    <el-dialog v-model="startVisible" title="开始维保" width="440px">
      <el-form :model="startForm" label-width="80px">
        <el-form-item label="设备">{{ current?.equipmentName }}</el-form-item>
        <el-form-item label="维护类型">
          <el-select v-model="startForm.maintenanceType" style="width:100%">
            <el-option label="维修" value="REPAIR" />
            <el-option label="点检" value="INSPECTION" />
            <el-option label="保养" value="MAINTENANCE" />
            <el-option label="大修" value="OVERHAUL" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障描述">
          <el-input v-model="startForm.faultDescription" type="textarea" :rows="2" placeholder="故障现象" />
        </el-form-item>
        <el-form-item label="维护内容">
          <el-input v-model="startForm.maintenanceContent" type="textarea" :rows="2" placeholder="计划维护内容" />
        </el-form-item>
        <el-alert v-if="linkedAlarm" type="info" :closable="false" style="margin-top:4px">
          将关联报警 {{ linkedAlarm.alarmNo }}，维保完成后自动关闭
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="startVisible=false">取消</el-button>
        <el-button type="warning" :loading="acting" @click="doStart">开始维保</el-button>
      </template>
    </el-dialog>

    <!-- 完成维保弹窗 -->
    <el-dialog v-model="finishVisible" title="完成维保" width="440px">
      <el-form :model="finishForm" label-width="80px">
        <el-form-item label="设备">{{ current?.equipmentName }}</el-form-item>
        <el-form-item label="维保结果">
          <el-select v-model="finishForm.result" style="width:100%">
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="临时修复" value="TEMPORARY_FIXED" />
            <el-option label="未解决" value="UNRESOLVED" />
          </el-select>
        </el-form-item>
        <el-form-item label="维护成本">
          <el-input-number v-model="finishForm.costAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="维护小结">
          <el-input v-model="finishForm.maintenanceContent" type="textarea" :rows="3" placeholder="实际维护内容/结果" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="finishVisible=false">取消</el-button>
        <el-button type="success" :loading="acting" @click="doFinish">完成维保</el-button>
      </template>
    </el-dialog>

    <!-- 解除确认弹窗 -->
    <el-dialog v-model="resolveVisible" title="解除报警" width="400px" :close-on-click-modal="false">
      <div v-if="resolveTarget" class="rv-info">
        <el-tag :type="alarmStatusType(resolveTarget.alarmStatus)" size="small">{{ resolveTarget.alarmStatusCn }}</el-tag>
        <span class="rv-no">{{ resolveTarget.alarmNo }}</span>
        <span class="rv-eq">{{ resolveTarget.equipmentName }}</span>
      </div>
      <el-form label-width="80px" style="margin-top:12px">
        <el-form-item label="解除原因">
          <el-input v-model="resolveForm.remark" type="textarea" :rows="3"
            placeholder="如：误触发、已手动修复、不影响生产…" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="doResolve">确认解除</el-button>
      </template>
    </el-dialog>
  </ModulePageShell>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import KpiStrip from '@/components/module/KpiStrip.vue'
import ModulePageShell from '@/components/module/ModulePageShell.vue'
import { moduleStatusType } from '@/constants/moduleStatus'
import {
  fetchEquipmentViews, fetchEquipmentKpi, fetchAlarmViews, fetchMaintenanceViews,
  receiveAlarm, resolveAlarm, startMaintenance, finishMaintenance
} from '@/api/business'

const kpiCards = [
  { key: 'total', kpiKey: 'total', label: '设备总数', cls: '' },
  { key: 'normal', kpiKey: 'normal', label: '正常', cls: 'passed' },
  { key: 'fault', kpiKey: 'fault', label: '故障', cls: 'failed' },
  { key: 'maintaining', kpiKey: 'maintaining', label: '维保中', cls: 'recheck' },
  { key: 'openAlarms', kpiKey: 'openAlarms', label: '未闭环报警', cls: 'open' },
]

const userStore = useUserStore()
const operator = computed(() => userStore.userInfo?.username || '')

const loading = ref(false)
const acting = ref(false)
const kpi = ref({})
const equipments = ref([])
const alarms = ref([])
const records = ref([])

const pendingEquipments = computed(() =>
  equipments.value.filter(e => e.status === 'FAULT' || e.status === 'MAINTAINING' || Number(e.openAlarmCount) > 0)
)
const openAlarms = computed(() => alarms.value.filter(a => isAlarmOpen(a.alarmStatus)))

const current = ref(null)
const startVisible = ref(false)
const finishVisible = ref(false)
const startForm = reactive({ maintenanceType: 'REPAIR', faultDescription: '', maintenanceContent: '' })
const finishForm = reactive({ result: 'COMPLETED', costAmount: 0, maintenanceContent: '' })

// 开始维保时可关联的该设备未闭环报警
const linkedAlarm = computed(() => {
  if (!current.value) return null
  return alarms.value.find((a) => a.equipmentId === current.value.equipmentId && isAlarmOpen(a.alarmStatus)) || null
})

function eqStatusType(s) {
  return moduleStatusType('equipmentStatus', s)
}
function levelType(l) {
  return moduleStatusType('alarmLevel', l)
}
function alarmStatusType(s) {
  return moduleStatusType('alarmStatus', s)
}
function isAlarmOpen(s) {
  return s === 'OPEN' || s === 'RECEIVED' || s === 'PROCESSING'
}
async function loadAll() {
  loading.value = true
  try {
    const [k, eq, al, mr] = await Promise.all([
      fetchEquipmentKpi(), fetchEquipmentViews(), fetchAlarmViews(), fetchMaintenanceViews()
    ])
    kpi.value = k || {}
    equipments.value = eq || []
    alarms.value = al || []
    records.value = mr || []
  } catch (e) {
    ElMessage.error(e?.message || '加载设备数据失败')
  } finally {
    loading.value = false
  }
}

function openStart(row) {
  current.value = row
  startForm.maintenanceType = 'REPAIR'
  startForm.faultDescription = linkedAlarm.value?.alarmDescription || ''
  startForm.maintenanceContent = ''
  startVisible.value = true
}
async function doStart() {
  acting.value = true
  try {
    await startMaintenance({
      equipmentId: current.value.equipmentId,
      alarmId: linkedAlarm.value?.alarmId || null,
      ...startForm,
      operator: operator.value
    })
    ElMessage.success('已开始维保，设备转为维保中')
    startVisible.value = false
    await loadAll()
  } catch (e) {
    ElMessage.error(e?.message || '开始维保失败')
  } finally {
    acting.value = false
  }
}

function openFinish(row) {
  current.value = row
  finishForm.result = 'COMPLETED'
  finishForm.costAmount = 0
  finishForm.maintenanceContent = ''
  finishVisible.value = true
}
async function doFinish() {
  acting.value = true
  try {
    await finishMaintenance({
      maintenanceId: current.value.activeMaintenanceId,
      ...finishForm,
      operator: operator.value
    })
    ElMessage.success('维保完成，设备已恢复正常')
    finishVisible.value = false
    await loadAll()
  } catch (e) {
    ElMessage.error(e?.message || '完成维保失败')
  } finally {
    acting.value = false
  }
}

async function doReceive(row) {
  acting.value = true
  try {
    await receiveAlarm({ alarmId: row.alarmId, operator: operator.value })
    ElMessage.success('已接收报警')
    await loadAll()
  } catch (e) {
    ElMessage.error(e?.message || '接收失败')
  } finally {
    acting.value = false
  }
}

const resolveForm = reactive({ alarmId: null, remark: '' })
const resolveVisible = ref(false)
const resolveTarget  = ref(null)

function openResolve(row) {
  resolveTarget.value  = row
  resolveForm.alarmId  = row.alarmId
  resolveForm.remark   = ''
  resolveVisible.value = true
}
async function doResolve() {
  acting.value = true
  try {
    await resolveAlarm({ alarmId: resolveForm.alarmId, remark: resolveForm.remark || '手动解除', operator: operator.value })
    ElMessage.success('报警已解除')
    resolveVisible.value = false
    await loadAll()
  } catch (e) {
    ElMessage.error(e?.message || '解除失败')
  } finally {
    acting.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.dm-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.dm-title { font-size: 18px; font-weight: 700; color: #001b3f; }
.dm-card { background: #fff; border-radius: 8px; padding: 12px 14px; margin-bottom: 14px; }
.rv-info { display: flex; align-items: center; gap: 8px; background: #f7faff; border-radius: 6px; padding: 8px 12px; }
.rv-no   { font-size: 13px; font-weight: 700; font-family: monospace; color: #1a2a40; }
.rv-eq   { font-size: 12px; color: #6b7a90; }
.dm-card__title { font-size: 15px; font-weight: 600; color: #001b3f; margin-bottom: 10px; }
</style>
