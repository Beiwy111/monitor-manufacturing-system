<template>
  <MesPageShell toolbar-title="订单跟踪" :status-options="ORDER_STATUS" :detail-rows="rows" :logs="mes.operationLogs.slice(0,8)">
    <template #table>
      <el-table :data="mes.orders" border stripe highlight-current-row @current-change="sel = $event">
        <el-table-column prop="id" label="订单号" width="140" />
        <el-table-column prop="customerName" label="客户" min-width="120" />
        <el-table-column prop="productModel" label="型号" width="130" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="planId" label="计划号" width="130" />
        <el-table-column prop="workOrderId" label="工单号" width="130" />
      </el-table>
    </template>
    <template #detail-extra>
      <ProcessTimeline v-if="sel" :items="timeline" />
    </template>
  </MesPageShell>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import { ORDER_STATUS } from '@/mock/constants'
import { detailRows } from '@/composables/useMesPage'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'
import ProcessTimeline from '@/components/mes/ProcessTimeline.vue'

const mes = useMesStore()
const sel = ref(null)
const rows = computed(() => detailRows(sel.value, [
  { key: 'id', label: '订单号' }, { key: 'status', label: '状态' },
  { key: 'planId', label: '计划' }, { key: 'workOrderId', label: '工单' }
]))
const timeline = computed(() => {
  if (!sel.value) return []
  const chain = mes.traceChain(sel.value.id)
  return [
    { title: '客户订单', desc: chain.order?.status },
    { title: '生产计划', desc: chain.plan?.status || '未创建' },
    { title: '生产工单', desc: chain.wo?.status || '未创建' },
    { title: '质检记录', desc: `${chain.inspections?.length || 0} 条` },
    { title: '发货', desc: chain.deliveries?.[0]?.status || '未发货' }
  ]
})
</script>
