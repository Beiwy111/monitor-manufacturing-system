<template>
  <MesDataPage title="采购入库" data-key="stockFlows" :columns="cols" :filter-fn="f => f.filter(x => x.flowType === '采购入库')" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'id', label: '流水号', width: 100 }, { prop: 'materialName', label: '物料', minWidth: 120 },
  { prop: 'quantity', label: '数量', width: 80 }, { prop: 'refNo', label: '关联单', width: 130 },
  { prop: 'createdAt', label: '时间', minWidth: 160 }
]
</script>
