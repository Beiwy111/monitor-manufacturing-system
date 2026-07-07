<template>
  <MesPageShell
    toolbar-title="生产工单"
    :status-options="WORK_ORDER_STATUS"
    :detail-rows="rows"
    :logs="mes.operationLogs.slice(0, 10)"
  >
    <template #table>
      <el-table :data="filtered" highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="工单号" width="130" />
        <el-table-column prop="orderNo" label="订单号" width="130" />
        <el-table-column prop="productModel" label="型号" width="130" />
        <el-table-column prop="quantity" label="计划量" width="80" />
        <el-table-column prop="completedQty" label="完成量" width="80" />
        <el-table-column prop="line" label="产线" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '草稿'" link type="primary" @click="selectAndRelease(row)">下达工单</el-button>
            <el-button v-if="row.status === '已下达'" link type="primary" @click="$router.push('/production/dispatch')">去派工</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-actions>
      <el-button v-if="selected?.status === '草稿'" type="primary" size="small" @click="release">下达工单</el-button>
      <el-button v-if="selected?.status === '已下达'" type="primary" size="small" @click="$router.push('/production/dispatch')">前往派工</el-button>
    </template>
  </MesPageShell>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { WORK_ORDER_STATUS } from '@/mock/constants'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const { selected, filtered, onRowClick } = useMesFilter(computed(() => mes.workOrders), ['id', 'orderNo'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'id', label: '工单号' }, { key: 'productModel', label: '型号' },
  { key: 'quantity', label: '计划' }, { key: 'completedQty', label: '完成' }, { key: 'status', label: '状态' }
]))

function release() {
  if (!selected.value) return
  mes.releaseWorkOrder(selected.value.id, userStore.displayName, userStore.roleKey)
  ElMessage.success('工单已下达，请到「工单派工」分配操作员')
}

function selectAndRelease(row) {
  selected.value = row
  release()
}
</script>
