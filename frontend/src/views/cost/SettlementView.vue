<template>
  <MesPageShell
    toolbar-title="成本结算"
    :status-options="COST_STATUS"
    :detail-rows="rows"
    :logs="mes.operationLogs.slice(0,8)"
  >
    <template #table>
      <el-table :data="mes.costSettlements" border stripe highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="结算单" width="130" />
        <el-table-column prop="workOrderId" label="工单" width="130" />
        <el-table-column prop="productModel" label="型号" width="130" />
        <el-table-column prop="materialCost" label="材料" width="90" />
        <el-table-column prop="laborCost" label="人工" width="90" />
        <el-table-column prop="totalCost" label="合计" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-actions>
      <el-button v-if="selected?.status==='草稿'" type="primary" size="small" @click="confirm">确认结算</el-button>
      <el-button v-if="selected?.status==='已确认'" size="small" @click="exportCs">导出</el-button>
    </template>
  </MesPageShell>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { COST_STATUS } from '@/mock/constants'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const { selected, onRowClick } = useMesFilter(computed(() => mes.costSettlements), ['id'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'workOrderId', label: '工单' }, { key: 'materialCost', label: '材料成本' },
  { key: 'laborCost', label: '人工成本' }, { key: 'equipmentCost', label: '设备成本' },
  { key: 'qualityCost', label: '质量成本' }, { key: 'totalCost', label: '合计' }, { key: 'status', label: '状态' }
]))
function confirm() {
  mes.confirmCostSettlement(selected.value.id, userStore.displayName, userStore.roleKey)
  ElMessage.success('结算已确认')
}
function exportCs() {
  mes.exportCostSettlement(selected.value.id, userStore.displayName, userStore.roleKey)
  ElMessage.success('已导出')
}
</script>
