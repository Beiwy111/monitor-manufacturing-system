<template>
  <MesDataPage
    title="工单成本"
    data-key="costSettlements"
    :columns="cols"
    delete-action="deleteCostSettlement"
    delete-payload-key="settlementId"
  />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'workOrderId', label: '工单', width: 130 }, { prop: 'totalCost', label: '总成本', width: 100 },
  { prop: 'status', label: '状态', width: 90 }
]
</script>
