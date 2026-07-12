<template>
  <div class="ruoyi-page">
    <div class="ruoyi-stats">
      <span class="ruoyi-stats__item">我的派工：<em>{{ myDispatches.length }}</em></span>
      <span class="ruoyi-stats__item">进行中：<em>{{ activeCount }}</em></span>
      <span class="ruoyi-stats__item" :class="{ 'ruoyi-stats__item--warn': pendingAccept > 0 }">
        待接收：<em>{{ pendingAccept }}</em>
      </span>
    </div>

    <div v-if="binding" class="ruoyi-operator-bar ruoyi-operator-bar--binding">
      <p class="ruoyi-operator-bar__meta">
        固定车间：<strong>{{ binding.workshopName }}</strong>
        · 负责工序：<strong>{{ binding.stageName }}</strong>
      </p>
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
import { operatorBinding, pickCurrentDispatch, stageProgressLabel, operatorReportPath } from '@/utils/operatorWorkshop'
import { triggerAlarm } from '@/api/business'
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'

const userStore = useUserStore()
const mes = useMesStore()
const showAlarm = ref(false)
const alarmDesc = ref('')
const acting = ref(false)

const username = computed(() => userStore.userInfo?.username)
const binding = computed(() => operatorBinding(username.value))
const myDispatches = computed(() => mes.myDispatches(username.value))
const activeCount = computed(() => myDispatches.value.filter(d => DISPATCH_REPORTABLE.includes(d.status)).length)
const pendingAccept = computed(() => myDispatches.value.filter(d => d.status === '已分配').length)
const currentDispatch = computed(() => pickCurrentDispatch(
  myDispatches.value.filter((d) => DISPATCH_ACTIVE.includes(d.status))
))

const reportPath = computed(() => operatorReportPath(username.value))

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
})

async function accept() {
  if (!currentDispatch.value || acting.value) return
  acting.value = true
  try {
    await mes.acceptDispatch(currentDispatch.value.id, username.value, 'operator')
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
    await mes.startDispatch(currentDispatch.value.id, username.value, 'operator')
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
      operator: username.value
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

.ruoyi-operator-bar__meta {
  margin: 0;
  font-size: 13px;
  font-weight: 400;
  color: var(--layout-text-secondary, #6b7280);
}
</style>
