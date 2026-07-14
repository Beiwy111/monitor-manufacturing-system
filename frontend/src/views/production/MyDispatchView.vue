<template>
  <MesPageShell toolbar-title="我的派工" :status-options="DISPATCH_STATUS" :detail-rows="rows">
    <template #table>
      <div v-if="binding" class="dispatch-context">
        <span class="dispatch-context__tag">固定车间</span>
        <strong>{{ binding.workshopName }}</strong>
        <el-divider direction="vertical" />
        <span class="dispatch-context__tag">负责工序</span>
        <strong>{{ binding.stageName }}</strong>
        <span class="dispatch-context__hint">（同一时间只能承担一道工序）</span>
      </div>

      <el-alert
        v-if="!myList.length"
        type="info"
        :closable="false"
        title="暂无派工任务。请生产主管在「工单派工」页将任务派给您。"
        style="margin: 12px 0"
      />
      <el-alert
        v-if="isAdminDemo"
        type="info"
        :closable="false"
        show-icon
        title="系统管理员以第 8 道工序操作员身份查看派工"
        style="margin-bottom: 12px"
      />

      <el-table v-if="myList.length" :data="myList" highlight-current-row class="mes-table-light" @current-change="onRowClick">
        <el-table-column prop="id" label="派工单" width="148" show-overflow-tooltip />
        <el-table-column prop="workOrderNo" label="工单" width="148" show-overflow-tooltip />
        <el-table-column prop="workshopName" label="车间" min-width="120" show-overflow-tooltip />
        <el-table-column prop="processStep" label="工序" width="100" show-overflow-tooltip />
        <el-table-column label="工序进度" min-width="168" show-overflow-tooltip>
          <template #default="{ row }">{{ stageProgressLabel(row) }}</template>
        </el-table-column>
        <el-table-column prop="equipment" label="设备" min-width="120" show-overflow-tooltip />
        <el-table-column prop="planQty" label="计划" width="72" align="center" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '已分配'" link type="primary" @click="selectAndAccept(row)">接收</el-button>
            <el-button v-if="row.status === '已接收'" link type="success" @click="selectAndStart(row)">开始生产</el-button>
            <el-button v-if="DISPATCH_REPORTABLE.includes(row.status)" link type="primary" @click="$router.push(reportPath)">去报工</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-actions>
      <el-button v-if="selected?.status === '已分配'" type="primary" size="small" @click="accept">接收派工</el-button>
      <el-button v-if="selected?.status === '已接收'" type="success" size="small" @click="start">开始生产</el-button>
      <el-button v-if="selected && DISPATCH_REPORTABLE.includes(selected.status)" type="primary" size="small" @click="$router.push(reportPath)">提交报工</el-button>
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
import { useOperatorIdentity } from '@/composables/useOperatorIdentity'
import { stageProgressLabel } from '@/utils/operatorWorkshop'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const { operatorUsername, binding, reportPath, isAdminDemo } = useOperatorIdentity()
const myList = computed(() => mes.myDispatches(operatorUsername.value))
const { selected, onRowClick } = useMesFilter(myList, ['id'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'workOrderNo', label: '工单' },
  { key: 'workshopName', label: '车间' },
  { key: 'processStep', label: '工序' },
  { key: 'equipment', label: '设备' },
  { key: 'planQty', label: '计划量' },
  { key: 'status', label: '状态' }
]))

async function accept() {
  if (!selected.value) return
  try {
    const ok = await mes.acceptDispatch(selected.value.id, operatorUsername.value, userStore.roleKey)
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
    const ok = await mes.startDispatch(selected.value.id, operatorUsername.value, userStore.roleKey)
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

<style scoped>
.dispatch-context {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.dispatch-context__tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #909399;
  background: #f4f4f5;
}

.dispatch-context__hint {
  font-size: 12px;
  color: #909399;
}
</style>
