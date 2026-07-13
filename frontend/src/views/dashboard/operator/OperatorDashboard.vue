<template>
  <div class="ruoyi-page">
    <div class="ruoyi-stats">
      <span class="ruoyi-stats__item">我的派工：<em>{{ myDispatches.length }}</em></span>
      <span class="ruoyi-stats__item">进行中：<em>{{ activeCount }}</em></span>
      <span class="ruoyi-stats__item" :class="{ 'ruoyi-stats__item--warn': pendingAccept > 0 }">
        待接收：<em>{{ pendingAccept }}</em>
      </span>
    </div>

    <div v-if="currentDispatch" class="ruoyi-operator-bar">
      <h3 class="ruoyi-operator-bar__title">
        当前工单：{{ currentDispatch.workOrderNo }} · {{ currentDispatch.processStep }}
      </h3>
      <p style="margin:0">
        设备：{{ currentDispatch.equipment }} · 计划 {{ currentDispatch.planQty }} 件 · 已完成 {{ currentDispatch.completedQty }}
      </p>
      <div class="ruoyi-operator-bar__actions">
        <el-button v-if="currentDispatch.status==='已分配'" type="primary" size="small" :loading="acting" @click="accept">接收工单</el-button>
        <el-button v-if="['已接收','生产中'].includes(currentDispatch.status)" type="success" size="small" :loading="acting" @click="start">开始生产</el-button>
        <el-button v-if="currentDispatch.status==='生产中'" size="small" @click="$router.push('/production/report')">提交报工</el-button>
        <el-button type="danger" size="small" @click="showAlarm=true">🔴 触发安灯</el-button>
      </div>
    </div>

    <RoleWorkbench role-key="operator" :status-items="[]" :shortcuts="shortcuts" embedded />

    <!-- 完整安灯上报弹窗 -->
    <AlarmReportDialog v-model:visible="showAlarm" @reported="onAlarmReported" />
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useMesStore } from '@/stores/mes'
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'
import AlarmReportDialog from '@/views/device/AlarmReportDialog.vue'

const userStore = useUserStore()
const mes       = useMesStore()
const showAlarm = ref(false)
const acting    = ref(false)

const username       = computed(() => userStore.userInfo?.username)
const myDispatches   = computed(() => mes.myDispatches(username.value))
const activeCount    = computed(() => myDispatches.value.filter(d => ['已接收','生产中'].includes(d.status)).length)
const pendingAccept  = computed(() => myDispatches.value.filter(d => d.status === '已分配').length)
const currentDispatch = computed(() => myDispatches.value.find(d => ['已分配','已接收','生产中'].includes(d.status)))

const shortcuts = [
  { label: '我的派工', path: '/production/my-dispatch' },
  { label: '生产报工', path: '/production/report' },
  { label: '工艺说明', path: '/production/process-guide' },
  { label: '安灯上报', path: '/production/andon-report' },
]

onMounted(async () => {
  try { await mes.hydrateFromApi() } catch { /* 路由守卫已尝试加载 */ }
})

async function accept() {
  if (!currentDispatch.value || acting.value) return
  acting.value = true
  try {
    await mes.acceptDispatch(currentDispatch.value.id, username.value, 'operator')
    ElMessage.success('已接收派工')
  } catch (e) {
    ElMessage.error(e?.message || '接收失败')
  } finally { acting.value = false }
}

async function start() {
  if (!currentDispatch.value || acting.value) return
  acting.value = true
  try {
    await mes.startDispatch(currentDispatch.value.id, username.value, 'operator')
    ElMessage.success('已开始生产')
  } catch (e) {
    ElMessage.error(e?.message || '开始生产失败')
  } finally { acting.value = false }
}

function onAlarmReported() {
  ElMessage.success('安灯上报成功，设备工程师已收到通知')
}
</script>

<style scoped>
.ruoyi-page :deep(.ruoyi-workbench) {
  min-height: auto;
  box-shadow: none;
}
</style>
