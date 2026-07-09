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
      <el-descriptions v-if="chain" :column="2" border size="small" style="margin-top:12px">
        <el-descriptions-item label="领料任务">{{ chain.issueTasks?.length || 0 }} 条</el-descriptions-item>
        <el-descriptions-item label="入库任务">{{ chain.inboundTasks?.length || 0 }} 条</el-descriptions-item>
        <el-descriptions-item label="派工记录">{{ chain.dispatches?.length || 0 }} 条</el-descriptions-item>
        <el-descriptions-item label="质检记录">{{ chain.inspections?.length || 0 }} 条</el-descriptions-item>
      </el-descriptions>
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
const chain = computed(() => (sel.value ? mes.traceChain(sel.value.id) : null))

function stepDesc(value, fallback = '未开始') {
  return value || fallback
}

const timeline = computed(() => {
  if (!chain.value) return []
  const c = chain.value
  const issueDone = c.issueTasks?.length
    ? c.issueTasks.every((t) => t.status === '已完成')
    : false
  const inboundDone = c.inboundTasks?.length
    ? c.inboundTasks.every((t) => t.status === '已入库')
    : false
  const deliveryStatus = c.deliveries?.[0]?.status || '未发货'
  return [
    { title: '客户订单', desc: stepDesc(c.order?.status), status: c.order ? 'done' : 'pending' },
    { title: '生产计划', desc: stepDesc(c.plan?.status, '未创建'), status: c.plan ? 'done' : 'pending' },
    { title: '生产工单', desc: stepDesc(c.wo?.status, '未创建'), status: c.wo ? 'done' : 'pending' },
    { title: '生产领料', desc: c.issueTasks?.length ? `${c.issueTasks.length} 项 / ${issueDone ? '已完成' : '进行中'}` : '待下达工单', status: issueDone ? 'done' : (c.issueTasks?.length ? 'active' : 'pending') },
    { title: '派工报工', desc: c.dispatches?.length ? `${c.dispatches.length} 条派工` : '未派工', status: c.dispatches?.length ? 'done' : 'pending' },
    { title: '质量检验', desc: c.inspections?.length ? `${c.inspections.length} 条记录` : '未送检', status: c.inspections?.length ? 'done' : 'pending' },
    { title: '成品入库', desc: c.inboundTasks?.length ? `${c.inboundTasks.length} 项 / ${inboundDone ? '已入库' : '待入库'}` : '待质检合格', status: inboundDone ? 'done' : (c.inboundTasks?.length ? 'active' : 'pending') },
    { title: '发货出库', desc: deliveryStatus, status: deliveryStatus === '已出库' ? 'done' : (deliveryStatus === '待出库' ? 'active' : 'pending') }
  ]
})
</script>
