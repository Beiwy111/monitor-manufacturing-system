<template>
  <MesPageShell toolbar-title="系统看板" :show-log="false">
    <template #table>
      <div style="padding:16px">
        <el-row :gutter="16">
          <el-col :span="6"><div class="mes-status-item"><div class="mes-status-label">订单</div><div class="mes-status-value">{{ mes.orders.length }}</div></div></el-col>
          <el-col :span="6"><div class="mes-status-item"><div class="mes-status-label">工单</div><div class="mes-status-value">{{ mes.workOrders.length }}</div></div></el-col>
          <el-col :span="6"><div class="mes-status-item"><div class="mes-status-label">待检</div><div class="mes-status-value">{{ mes.pendingInspections.length }}</div></div></el-col>
          <el-col :span="6"><div class="mes-status-item"><div class="mes-status-label">安灯</div><div class="mes-status-value mes-status-value--danger">{{ mes.openAlarms.length }}</div></div></el-col>
        </el-row>
        <h4 style="margin:24px 0 12px;font-weight:600">全流程状态</h4>
        <el-table :data="mes.orders" size="small">
          <el-table-column prop="id" label="订单" width="140" />
          <el-table-column prop="productModel" label="型号" />
          <el-table-column prop="status" label="状态" width="90" />
          <el-table-column prop="planId" label="计划" width="130" />
          <el-table-column prop="workOrderId" label="工单" width="130" />
        </el-table>
      </div>
    </template>
    <template #detail-extra>
      <OperationLogPanel :logs="mes.operationLogs.slice(0,20)" />
    </template>
  </MesPageShell>
</template>

<script setup>
import { useMesStore } from '@/stores/mes'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import OperationLogPanel from '@/components/mes/OperationLogPanel.vue'
const mes = useMesStore()
</script>
