<template>
  <MesDataPage title="质检记录" data-key="inspections" :columns="cols" :filter-fn="f => f.filter(i => i.status !== '待检')" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'id', label: '质检单', width: 130 }, { prop: 'inspectorName', label: '质检员', width: 90 },
  { prop: 'result', label: '结果', width: 90 }, { prop: 'createdAt', label: '时间', minWidth: 160 }
]
</script>
