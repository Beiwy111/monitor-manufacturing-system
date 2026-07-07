<template>
  <MesDataPage title="库存流水" data-key="stockFlows" :columns="cols" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'flowType', label: '类型', width: 100 }, { prop: 'materialName', label: '物料', minWidth: 120 },
  { prop: 'direction', label: '方向', width: 60 }, { prop: 'quantity', label: '数量', width: 80 },
  { prop: 'operator', label: '操作人', width: 90 }, { prop: 'createdAt', label: '时间', minWidth: 160 }
]
</script>
