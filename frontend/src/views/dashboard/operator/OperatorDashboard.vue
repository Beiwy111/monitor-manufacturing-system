<template>
  <div class="ruoyi-page">
    <div class="ruoyi-stats">
      <span class="ruoyi-stats__item">我的派工：<em>{{ myDispatches.length }}</em></span>
      <span class="ruoyi-stats__item">进行中：<em>{{ activeCount }}</em></span>
      <span class="ruoyi-stats__item" :class="{ 'ruoyi-stats__item--warn': pendingAccept > 0 }">
        待接收：<em>{{ pendingAccept }}</em>
      </span>
    </div>

    <div v-if="isAdminDemo" class="ruoyi-operator-bar ruoyi-operator-bar--binding">
      <p class="ruoyi-operator-bar__meta">
        演示模式：以第 8 道工序操作员 <strong>韩操作（han_operator）</strong> 身份操作真实派工
      </p>
    </div>

    <div v-if="binding" class="ruoyi-operator-bar ruoyi-operator-bar--binding">
      <p class="ruoyi-operator-bar__meta">
        固定车间：<strong>{{ binding.workshopName }}</strong>
        · 负责工序：<strong>{{ binding.stageName }}</strong>
      </p>
    </div>

    <div class="ruoyi-operator-bar ruoyi-operator-bar--attendance">
      <div class="attendance-info">
        <h3 class="ruoyi-operator-bar__title">今日考勤</h3>
        <p style="margin:0;font-size:13px;color:#606266">
          上班：{{ fmtTime(todayRecord?.checkInTime) }}
          · 下班：{{ fmtTime(todayRecord?.checkOutTime) }}
          <el-tag v-if="todayRecord?.status" :type="attendanceTag(todayRecord.status)" size="small" style="margin-left:8px">
            {{ attendanceLabel(todayRecord.status) }}
          </el-tag>
        </p>
      </div>
      <div class="ruoyi-operator-bar__actions">
        <el-button type="primary" size="small" :loading="checkingIn" :disabled="!!todayRecord?.checkInTime" @click="doCheckIn">
          上班打卡
        </el-button>
        <el-button type="success" size="small" :loading="checkingOut" :disabled="!todayRecord?.checkInTime || !!todayRecord?.checkOutTime" @click="doCheckOut">
          下班打卡
        </el-button>
      </div>
    </div>

    <div v-if="currentDispatch" class="ruoyi-operator-bar">
      <h3 class="ruoyi-operator-bar__title">
        当前生产：{{ currentDispatch.workOrderNo }} · {{ stageProgressLabel(currentDispatch) }}
      </h3>
      <p style="margin:0">
        车间：{{ currentDispatch.workshopName || binding?.workshopName }}
        · 设备：{{ currentDispatch.equipment }}
        · 计划 {{ currentDispatch.planQty }} 件 · 已完成 {{ currentDispatch.completedQty }}
      </p>
      <div class="ruoyi-operator-bar__actions">
        <el-button v-if="currentDispatch.status==='已分配'" type="primary" size="small" :loading="acting" @click="accept">接收工单</el-button>
        <el-button v-if="currentDispatch.status==='已接收'" type="success" size="small" :loading="acting" @click="start">开始生产</el-button>
        <el-button v-if="DISPATCH_REPORTABLE.includes(currentDispatch.status)" size="small" @click="$router.push(reportPath)">提交报工</el-button>
        <el-button type="danger" size="small" @click="showAlarm=true">触发安灯</el-button>
      </div>
    </div>

    <RoleWorkbench role-key="operator" :status-items="[]" :shortcuts="shortcuts" embedded />

    <el-dialog v-model="showAlarm" title="触发安灯报警" width="420px">
      <el-input v-model="alarmDesc" type="textarea" rows="3" placeholder="描述异常情况" />
      <template #footer>
        <el-button @click="showAlarm=false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="submitAlarm">上报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useMesStore } from '@/stores/mes'
import { DISPATCH_REPORTABLE, DISPATCH_ACTIVE } from '@/mock/constants'
import { pickCurrentDispatch, stageProgressLabel } from '@/utils/operatorWorkshop'
import { useOperatorIdentity } from '@/composables/useOperatorIdentity'
import { triggerAlarm } from '@/api/business'
import { getTodayAttendance, checkIn, checkOut } from '@/api/attendance'
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'

const userStore = useUserStore()
const mes = useMesStore()
const { operatorUsername, binding, reportPath, isAdminDemo, loginUsername } = useOperatorIdentity()
const showAlarm = ref(false)
const alarmDesc = ref('')
const acting = ref(false)
const todayRecord = ref(null)
const checkingIn = ref(false)
const checkingOut = ref(false)

const myDispatches = computed(() => mes.myDispatches(operatorUsername.value))
const activeCount = computed(() => myDispatches.value.filter(d => DISPATCH_REPORTABLE.includes(d.status)).length)
const pendingAccept = computed(() => myDispatches.value.filter(d => d.status === '已分配').length)
const currentDispatch = computed(() => pickCurrentDispatch(
  myDispatches.value.filter((d) => DISPATCH_ACTIVE.includes(d.status))
))


const shortcuts = computed(() => [
  { label: '我的派工', path: '/production/my-dispatch' },
  { label: '工序报工', path: reportPath.value },
  { label: '生产报表', path: '/report/production-progress' },
  { label: '工艺说明', path: '/production/process-guide' },
  { label: '安灯报警', path: '/device/alarm' }
])

onMounted(async () => {
  try {
    await mes.hydrateFromApi()
  } catch {
    /* 路由守卫已尝试加载 */
  }
  await loadTodayAttendance()
})

async function loadTodayAttendance() {
  const userId = userStore.userInfo?.userId
  if (!userId) return
  try {
    todayRecord.value = await getTodayAttendance(userId)
  } catch {
    todayRecord.value = null
  }
}

function fmtTime(val) {
  if (!val) return '未打卡'
  return String(val).replace('T', ' ').slice(11, 19)
}

function attendanceLabel(status) {
  return { NORMAL: '正常', LATE: '迟到', EARLY_LEAVE: '早退', ABSENT: '缺勤' }[status] || status
}

function attendanceTag(status) {
  return { NORMAL: 'success', LATE: 'warning', EARLY_LEAVE: 'warning', ABSENT: 'danger' }[status] || 'info'
}

async function doCheckIn() {
  const userId = userStore.userInfo?.userId
  if (!userId || checkingIn.value) return
  checkingIn.value = true
  try {
    todayRecord.value = await checkIn(userId)
    ElMessage.success('上班打卡成功')
  } catch (e) {
    ElMessage.error(e?.message || '打卡失败')
  } finally {
    checkingIn.value = false
  }
}

async function doCheckOut() {
  const userId = userStore.userInfo?.userId
  if (!userId || checkingOut.value) return
  checkingOut.value = true
  try {
    todayRecord.value = await checkOut(userId)
    ElMessage.success('下班打卡成功')
  } catch (e) {
    ElMessage.error(e?.message || '打卡失败')
  } finally {
    checkingOut.value = false
  }
}

async function accept() {
  if (!currentDispatch.value || acting.value) return
  acting.value = true
  try {
    await mes.acceptDispatch(currentDispatch.value.id, operatorUsername.value, 'operator')
    ElMessage.success('已接收派工')
  } catch (e) {
    ElMessage.error(e?.message || '接收失败')
  } finally {
    acting.value = false
  }
}

async function start() {
  if (!currentDispatch.value || acting.value) return
  acting.value = true
  try {
    await mes.startDispatch(currentDispatch.value.id, operatorUsername.value, 'operator')
    ElMessage.success('已开始生产')
  } catch (e) {
    ElMessage.error(e?.message || '开始生产失败')
  } finally {
    acting.value = false
  }
}

async function submitAlarm() {
  if (acting.value) return
  const dispatch = currentDispatch.value
  if (!dispatch?.equipmentId) {
    ElMessage.warning('请先接收派工并关联设备后再触发安灯')
    return
  }
  if (!alarmDesc.value.trim()) {
    ElMessage.warning('请描述异常情况')
    return
  }
  acting.value = true
  try {
    await triggerAlarm({
      equipmentId: dispatch.equipmentId,
      alarmType: 'EQUIPMENT',
      alarmLevel: 'IMPORTANT',
      description: alarmDesc.value.trim(),
      operator: loginUsername.value
    })
    ElMessage.success('安灯已上报，设备维护人员将收到报警')
    showAlarm.value = false
    alarmDesc.value = ''
  } catch (e) {
    ElMessage.error(e?.message || '上报失败')
  } finally {
    acting.value = false
  }
}
</script>

<style scoped>
.ruoyi-page :deep(.ruoyi-workbench) {
  min-height: auto;
  box-shadow: none;
}

.ruoyi-operator-bar--binding {
  margin-bottom: 8px;
  padding: 10px 14px;
  background: var(--layout-accent-soft, rgba(45, 138, 102, 0.08));
  border: 1px solid var(--layout-accent-border, rgba(45, 138, 102, 0.2));
  border-radius: var(--layout-card-radius, 12px);
}

.ruoyi-operator-bar--attendance {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  padding: 12px 14px;
  background: #f0f9ff;
  border: 1px solid #b3d8ff;
  border-radius: var(--layout-card-radius, 12px);
}

.ruoyi-operator-bar__meta {
  margin: 0;
  font-size: 13px;
  font-weight: 400;
  color: var(--layout-text-secondary, #6b7280);
}
</style>
