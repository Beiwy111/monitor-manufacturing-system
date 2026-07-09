<template>
  <MesPageShell toolbar-title="我的派工" :status-options="DISPATCH_STATUS" :detail-rows="rows" :logs="mes.operationLogs.slice(0, 8)">
    <template #table>
      <el-alert
        v-if="!myList.length"
        type="info"
        :closable="false"
        title="暂无派工任务。请生产主管在「工单派工」页将任务派给您。"
        style="margin-bottom: 12px"
      />
      <el-table v-else :data="myList" border stripe highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="派工单" width="130" />
        <el-table-column prop="workOrderNo" label="工单" width="130" />
        <el-table-column prop="processStep" label="工序" width="100" />
        <el-table-column prop="equipment" label="设备" min-width="120" />
        <el-table-column prop="planQty" label="计划" width="70" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '已分配'" link type="primary" @click="selectAndAccept(row)">接收</el-button>
            <el-button v-if="row.status === '已接收'" link type="success" @click="selectAndStart(row)">开始生产</el-button>
            <el-button v-if="DISPATCH_REPORTABLE.includes(row.status)" link type="primary" @click="$router.push('/production/report')">去报工</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-actions>
      <el-button v-if="selected?.status === '已分配'" type="primary" size="small" @click="accept">接收派工</el-button>
      <el-button v-if="selected?.status === '已接收'" type="success" size="small" @click="start">开始生产</el-button>
      <el-button v-if="selected && DISPATCH_REPORTABLE.includes(selected.status)" type="primary" size="small" @click="$router.push('/production/report')">提交报工</el-button>
    </template>
  </MesPageShell>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { DISPATCH_STATUS, DISPATCH_REPORTABLE } from '@/mock/constants'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const username = computed(() => userStore.userInfo?.username)
const myList = computed(() => mes.myDispatches(username.value))
const { selected, onRowClick } = useMesFilter(myList, ['id'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'workOrderNo', label: '工单' }, { key: 'processStep', label: '工序' },
  { key: 'equipment', label: '设备' }, { key: 'planQty', label: '计划量' }, { key: 'status', label: '状态' }
]))

async function accept() {
  if (!selected.value) return
  try {
    const ok = await mes.acceptDispatch(selected.value.id, username.value, userStore.roleKey)
    if (ok !== false) {
      ElMessage.success('已接收派工，请点击「开始生产」')
    } else {
      ElMessage.error('接收失败，请确认该任务分配给当前账号')
    }
  } catch (e) {
    ElMessage.error(e?.message || '接收派工失败')
  }
}

async function start() {
  if (!selected.value) return
  try {
    const ok = await mes.startDispatch(selected.value.id, username.value, userStore.roleKey)
    if (ok !== false) {
      ElMessage.success('已开始生产，可前往报工')
    } else {
      ElMessage.warning('请先接收派工')
    }
  } catch (e) {
    ElMessage.error(e?.message || '开始生产失败')
  }
}

function selectAndAccept(row) {
  selected.value = row
  accept()
}

function selectAndStart(row) {
  selected.value = row
  start()
}
</script>
