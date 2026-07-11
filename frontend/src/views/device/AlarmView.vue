<template>
  <div class="ruoyi-page alarm-page">
    <div class="ruoyi-stats">
      <span class="ruoyi-stats__item ruoyi-stats__item--danger">未关闭：<em>{{ openCount }}</em></span>
      <span class="ruoyi-stats__item ruoyi-stats__item--warn">待确认：<em>{{ pendingCount }}</em></span>
      <span class="ruoyi-stats__item">处理中：<em>{{ processingCount }}</em></span>
    </div>

    <div class="ruoyi-toolbar">
      <span class="ruoyi-toolbar__title">安灯报警</span>
      <el-select v-model="typeFilter" clearable placeholder="报警类型" style="width: 130px">
        <el-option v-for="t in ALARM_TYPES" :key="t" :label="t" :value="t" />
      </el-select>
      <el-select v-model="statusFilter" clearable placeholder="状态" style="width: 110px">
        <el-option v-for="s in ALARM_STATUS" :key="s" :label="s" :value="s" />
      </el-select>
      <el-button type="danger" @click="showReport = true">上报报警</el-button>
      <el-button @click="refresh">刷新</el-button>
    </div>

    <el-table :data="filteredAlarms" border stripe size="small" highlight-current-row @current-change="onRowClick">
      <el-table-column prop="id" label="报警号" width="120" />
      <el-table-column prop="type" label="报警类型" width="100">
        <template #default="{ row }">
          <el-tag :type="levelTagType(row.levelTone)" size="small" effect="plain">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="source" label="来源车间/设备" min-width="150" show-overflow-tooltip />
      <el-table-column prop="workOrderId" label="关联工单" width="120" />
      <el-table-column prop="level" label="等级" width="72" align="center">
        <template #default="{ row }"><span :class="'level-' + (row.levelTone || 'warning')">{{ row.level }}</span></template>
      </el-table-column>
      <el-table-column prop="occurredAt" label="发生时间" width="150" />
      <el-table-column prop="durationText" label="持续时长" width="100" />
      <el-table-column prop="handlerName" label="负责人" width="90" />
      <el-table-column prop="status" label="状态" width="88">
        <template #default="{ row }"><StatusBadge :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === '待确认'" link type="primary" @click="doAction(row, 'confirm')">确认</el-button>
          <el-button v-if="['待确认','已确认'].includes(row.status)" link type="warning" @click="openAssign(row)">指派</el-button>
          <el-button v-if="['已确认','已指派'].includes(row.status)" link @click="doAction(row, 'processing')">处理中</el-button>
          <el-button v-if="row.status !== '已关闭'" link type="success" @click="openClose(row)">关闭</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="detailDrawer" title="报警详情" size="480px">
      <el-descriptions v-if="selected" :column="1" border size="small">
        <el-descriptions-item label="报警号">{{ selected.id }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ selected.type }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ selected.source }}</el-descriptions-item>
        <el-descriptions-item label="关联工单">{{ selected.workOrderId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="等级">{{ selected.level }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ selected.description }}</el-descriptions-item>
        <el-descriptions-item label="发生时间">{{ selected.occurredAt }}</el-descriptions-item>
        <el-descriptions-item label="持续">{{ selected.durationText }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ selected.handlerName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ selected.status }}</el-descriptions-item>
        <el-descriptions-item v-if="selected.handleResult" label="处理结果">{{ selected.handleResult }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog v-model="showReport" title="上报安灯" width="460px">
      <el-form label-width="88px" size="small">
        <el-form-item label="报警类型">
          <el-select v-model="reportForm.type" style="width:100%">
            <el-option v-for="t in ALARM_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度">
          <el-radio-group v-model="reportForm.level">
            <el-radio value="严重">严重</el-radio>
            <el-radio value="较重">较重</el-radio>
            <el-radio value="一般">一般</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="来源车间"><el-input v-model="reportForm.workshop" /></el-form-item>
        <el-form-item label="关联工单"><el-input v-model="reportForm.workOrderId" placeholder="可选" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="reportForm.description" type="textarea" rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReport = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="report">上报</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="指派负责人" width="400px">
      <el-select v-model="assignee" style="width:100%">
        <el-option v-for="u in managerUsers" :key="u.username" :label="u.realName" :value="u.username" />
      </el-select>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="confirmAssign">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="closeVisible" title="关闭报警" width="420px">
      <el-input v-model="handleResult" type="textarea" rows="3" placeholder="请填写处理结果" />
      <template #footer>
        <el-button @click="closeVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="confirmClose">确认关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const ALARM_TYPES = ['设备故障', '物料短缺', '质量异常', '进度延期', '人员异常']
const ALARM_STATUS = ['待确认', '已确认', '已指派', '处理中', '已关闭']

const mes = useMesStore()
const userStore = useUserStore()

const typeFilter = ref('')
const statusFilter = ref('')
const selected = ref(null)
const detailDrawer = ref(false)
const showReport = ref(false)
const assignVisible = ref(false)
const closeVisible = ref(false)
const assignTarget = ref(null)
const assignee = ref('')
const handleResult = ref('')
const acting = ref(false)

const reportForm = reactive({ type: '设备故障', level: '较重', workshop: '', workOrderId: '', description: '' })

const openCount = computed(() => mes.alarms.filter((a) => a.status !== '已关闭').length)
const pendingCount = computed(() => mes.alarms.filter((a) => a.status === '待确认').length)
const processingCount = computed(() => mes.alarms.filter((a) => a.status === '处理中').length)
const managerUsers = computed(() => mes.sysUsers.filter((u) => u.roleKey === 'manager' && u.status === '启用'))

const filteredAlarms = computed(() => {
  let list = mes.alarms
  if (typeFilter.value) list = list.filter((a) => a.type === typeFilter.value)
  if (statusFilter.value) list = list.filter((a) => a.status === statusFilter.value)
  return list
})

onMounted(refresh)

async function refresh() {
  try { await mes.hydrateFromApi() } catch { /* ignore */ }
}

function onRowClick(row) {
  selected.value = row || null
  detailDrawer.value = !!row
}

function levelTagType(tone) {
  return tone === 'danger' ? 'danger' : 'warning'
}

async function doAction(row, action) {
  acting.value = true
  try {
    await mes.handleAlarm(row.id, action, userStore.username, userStore.roleKey)
    ElMessage.success('操作成功')
    await refresh()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    acting.value = false
  }
}

function openAssign(row) {
  assignTarget.value = row
  assignee.value = managerUsers.value[0]?.username || ''
  assignVisible.value = true
}

async function confirmAssign() {
  const u = managerUsers.value.find((x) => x.username === assignee.value)
  acting.value = true
  try {
    await mes.handleAlarm(assignTarget.value.id, 'assign', userStore.username, userStore.roleKey, {
      assignee: assignee.value,
      assigneeName: u?.realName || assignee.value
    })
    assignVisible.value = false
    await refresh()
  } catch (e) {
    ElMessage.error(e?.message || '指派失败')
  } finally {
    acting.value = false
  }
}

function openClose(row) {
  assignTarget.value = row
  handleResult.value = ''
  closeVisible.value = true
}

async function confirmClose() {
  if (!handleResult.value.trim()) {
    ElMessage.warning('请填写处理结果')
    return
  }
  acting.value = true
  try {
    await mes.handleAlarm(assignTarget.value.id, 'close', userStore.username, userStore.roleKey, { handleResult: handleResult.value })
    closeVisible.value = false
    await refresh()
  } catch (e) {
    ElMessage.error(e?.message || '关闭失败')
  } finally {
    acting.value = false
  }
}

async function report() {
  if (!reportForm.description.trim()) {
    ElMessage.warning('请填写描述')
    return
  }
  acting.value = true
  try {
    await mes.createAlarm({
      type: reportForm.type,
      level: reportForm.level,
      workshop: reportForm.workshop,
      source: reportForm.workshop,
      workOrderId: reportForm.workOrderId,
      description: reportForm.description,
      reporterName: userStore.displayName
    }, userStore.username, userStore.roleKey)
    showReport.value = false
    await refresh()
  } catch (e) {
    ElMessage.error(e?.message || '上报失败')
  } finally {
    acting.value = false
  }
}
</script>

<style scoped>
.alarm-page { padding: 0 4px; }
.level-danger { color: #f56c6c; font-weight: 600; }
.level-warning { color: #e6a23c; font-weight: 600; }
</style>
