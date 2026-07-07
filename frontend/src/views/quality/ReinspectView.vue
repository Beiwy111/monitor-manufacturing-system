<template>
  <MesDataPage title="复检处理" data-key="inspections" :columns="cols" :filter-fn="f => f.filter(i => i.status === '复检' || i.status === '不合格')" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'id', label: '质检单', width: 130 }, { prop: 'batchNo', label: '批次', minWidth: 140 },
  { prop: 'qcType', label: '类型', width: 90 }, { prop: 'status', label: '状态', width: 90 }
]
</script>
