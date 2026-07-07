<template>
  <MesDataPage title="生产进度" data-key="workOrders" :columns="cols" :detail-fields="fields" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'id', label: '工单', width: 130 }, { prop: 'productModel', label: '型号', width: 130 },
  { prop: 'quantity', label: '计划', width: 80 }, { prop: 'completedQty', label: '完成', width: 80 },
  { prop: 'status', label: '状态', width: 90 }
]
const fields = [{ key: 'id', label: '工单' }, { key: 'completedQty', label: '完成量' }, { key: 'status', label: '状态' }]
</script>
