<template>
  <MesPageShell toolbar-title="发货管理" :status-options="DELIVERY_STATUS" :detail-rows="rows" :logs="mes.operationLogs.slice(0,8)">
    <template #table>
      <el-table :data="mes.deliveries" border stripe highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="发货单" width="130" />
        <el-table-column prop="orderNo" label="订单" width="130" />
        <el-table-column prop="customerName" label="客户" min-width="120" />
        <el-table-column prop="productModel" label="型号" width="130" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-actions>
      <el-button v-if="selected?.status==='待出库'" type="primary" size="small" @click="ship">确认出库发货</el-button>
    </template>
  </MesPageShell>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { DELIVERY_STATUS } from '@/mock/constants'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const { selected, onRowClick } = useMesFilter(computed(() => mes.deliveries), ['id'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'orderNo', label: '订单' }, { key: 'quantity', label: '数量' }, { key: 'trackingNo', label: '运单' }, { key: 'status', label: '状态' }
]))
function ship() {
  mes.shipDelivery(selected.value.id, userStore.displayName, userStore.roleKey)
  ElMessage.success('发货成功，订单状态已更新')
}
</script>
