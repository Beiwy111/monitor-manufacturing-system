<template>
  <MesPageShell toolbar-title="成本报表" :show-log="false">
    <template #table>
      <el-table :data="mes.costSettlements" border stripe>
        <el-table-column prop="workOrderId" label="工单" width="130" />
        <el-table-column prop="materialCost" label="材料" width="100" />
        <el-table-column prop="laborCost" label="人工" width="100" />
        <el-table-column prop="equipmentCost" label="设备" width="100" />
        <el-table-column prop="qualityCost" label="质量" width="100" />
        <el-table-column prop="totalCost" label="合计" width="110" />
        <el-table-column prop="status" label="状态" width="90" />
      </el-table>
      <p class="ruoyi-summary">合计成本：{{ total }} 元</p>
    </template>
  </MesPageShell>
</template>
<script setup>
import { computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import MesPageShell from '@/components/mes/MesPageShell.vue'
const mes = useMesStore()
const total = computed(() => mes.costSettlements.reduce((s, c) => s + c.totalCost, 0))
</script>
