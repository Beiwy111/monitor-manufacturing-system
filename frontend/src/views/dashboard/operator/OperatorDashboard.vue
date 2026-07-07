<template>
  <div class="mes-workbench mes-operator-terminal">
    <div class="mes-status-strip">
      <div class="mes-status-item">
        <div class="mes-status-label">我的派工</div>
        <div class="mes-status-value">{{ myDispatches.length }}</div>
      </div>
      <div class="mes-status-item">
        <div class="mes-status-label">进行中</div>
        <div class="mes-status-value">{{ activeCount }}</div>
      </div>
      <div class="mes-status-item">
        <div class="mes-status-label">待接收</div>
        <div class="mes-status-value">{{ pendingAccept }}</div>
      </div>
    </div>
    <div v-if="currentDispatch" class="mes-operator-current">
      <h3>当前工单：{{ currentDispatch.workOrderNo }} · {{ currentDispatch.processStep }}</h3>
      <p style="margin:0;color:#4f5f73;font-size:14px">设备：{{ currentDispatch.equipment }} · 计划 {{ currentDispatch.planQty }} 件 · 已完成 {{ currentDispatch.completedQty }}</p>
      <div class="mes-operator-btns">
        <el-button v-if="currentDispatch.status==='已分配'" type="primary" @click="accept">接收工单</el-button>
        <el-button v-if="['已接收','生产中'].includes(currentDispatch.status)" type="success" @click="start">开始生产</el-button>
        <el-button v-if="currentDispatch.status==='生产中'" @click="$router.push('/production/report')">提交报工</el-button>
        <el-button type="danger" @click="showAlarm=true">触发安灯</el-button>
      </div>
    </div>
    <RoleWorkbench role-key="operator" :status-items="[]" :shortcuts="shortcuts" />
    <el-dialog v-model="showAlarm" title="触发安灯报警" width="420px">
      <el-input v-model="alarmDesc" type="textarea" rows="3" placeholder="描述异常情况" />
      <template #footer>
        <el-button @click="showAlarm=false">取消</el-button>
        <el-button type="danger" @click="submitAlarm">上报</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useMesStore } from '@/stores/mes'
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'

const userStore = useUserStore()
const mes = useMesStore()
const showAlarm = ref(false)
const alarmDesc = ref('')

const username = computed(() => userStore.userInfo?.username)
const myDispatches = computed(() => mes.myDispatches(username.value))
const activeCount = computed(() => myDispatches.value.filter(d => ['已接收','生产中'].includes(d.status)).length)
const pendingAccept = computed(() => myDispatches.value.filter(d => d.status === '已分配').length)
const currentDispatch = computed(() => myDispatches.value.find(d => ['已分配','已接收','生产中'].includes(d.status)))

const shortcuts = [
  { label: '我的派工', path: '/production/my-dispatch' },
  { label: '生产报工', path: '/production/report' },
  { label: '工艺说明', path: '/production/process-guide' },
  { label: '安灯报警', path: '/device/alarm' }
]

function accept() {
  if (currentDispatch.value && mes.acceptDispatch(currentDispatch.value.id, username.value, 'operator')) {
    ElMessage.success('已接收派工')
  }
}
function start() {
  if (currentDispatch.value && mes.startDispatch(currentDispatch.value.id, username.value, 'operator')) {
    ElMessage.success('已开始生产')
  }
}
function submitAlarm() {
  mes.createAlarm({ type: '生产异常', source: currentDispatch.value?.equipment || '工位', workOrderId: currentDispatch.value?.workOrderId || '', reporterName: userStore.displayName, description: alarmDesc.value }, username.value, 'operator')
  ElMessage.success('安灯已上报')
  showAlarm.value = false
  alarmDesc.value = ''
}
</script>
