<template>
  <MesDataPage title="售后处理" data-key="aftersaleCases" :columns="cols" :filter-fn="f => f.filter(c => c.status === '处理中' || c.status === '追溯中')" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'id', label: '售后单', width: 130 }, { prop: 'orderId', label: '订单', width: 130 },
  { prop: 'handler', label: '处理人', width: 90 }, { prop: 'status', label: '状态', width: 90 }
]
</script>
