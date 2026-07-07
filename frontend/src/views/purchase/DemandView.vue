<template>
  <MesDataPage title="采购需求" data-key="purchaseDemands" :columns="cols" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'materialName', label: '物料', width: 120 }, { prop: 'requiredQty', label: '需求', width: 80 },
  { prop: 'stockQty', label: '库存', width: 80 }, { prop: 'gapQty', label: '缺口', width: 80 },
  { prop: 'status', label: '状态', width: 90 }
]
</script>
