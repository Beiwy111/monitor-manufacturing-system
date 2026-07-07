<template>
  <MesDataPage title="设备成本" data-key="costSettlements" :columns="cols" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'workOrderId', label: '工单', width: 130 }, { prop: 'equipmentCost', label: '设备成本', width: 110 },
  { prop: 'qualityCost', label: '质量成本', width: 110 }
]
</script>
