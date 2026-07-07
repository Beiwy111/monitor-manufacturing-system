<template>
  <MesDataPage title="材料成本" data-key="costSettlements" :columns="cols" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'workOrderId', label: '工单', width: 130 }, { prop: 'materialCost', label: '材料成本', width: 110 },
  { prop: 'productModel', label: '型号', width: 130 }
]
</script>
