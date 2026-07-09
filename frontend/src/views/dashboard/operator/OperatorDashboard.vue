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
        <el-button v-if="currentDispatch.status==='已接收'" type="success" size="small" :loading="acting" @click="start">开始生产</el-button>
        <el-button v-if="DISPATCH_REPORTABLE.includes(currentDispatch.status)" size="small" @click="$router.push('/production/report')">提交报工</el-button>
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
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'

const userStore = useUserStore()
const mes = useMesStore()
const showAlarm = ref(false)
const alarmDesc = ref('')
const acting = ref(false)

const username = computed(() => userStore.userInfo?.username)
const myDispatches = computed(() => mes.myDispatches(username.value))
const activeCount = computed(() => myDispatches.value.filter(d => DISPATCH_REPORTABLE.includes(d.status)).length)
const pendingAccept = computed(() => myDispatches.value.filter(d => d.status === '已分配').length)
const currentDispatch = computed(() => myDispatches.value.find(d => DISPATCH_ACTIVE.slice(0, 3).includes(d.status)))

const shortcuts = [
  { label: '我的派工', path: '/production/my-dispatch' },
  { label: '生产报工', path: '/production/report' },
  { label: '工艺说明', path: '/production/process-guide' },
  { label: '安灯报警', path: '/device/alarm' }
]

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
  acting.value = true
  try {
    await mes.createAlarm({
      type: '生产异常',
      source: currentDispatch.value?.equipment || '工位',
      workOrderId: currentDispatch.value?.workOrderId || '',
      reporterName: userStore.displayName,
      description: alarmDesc.value
    }, username.value, 'operator')
    ElMessage.success('安灯已上报')
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
</style>
