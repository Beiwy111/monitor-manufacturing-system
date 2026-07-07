<template>
  <MesDataPage title="维护记录" data-key="maintenanceRecords" :columns="cols" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'equipmentName', label: '设备', minWidth: 140 }, { prop: 'content', label: '内容', minWidth: 160 },
  { prop: 'downtimeHours', label: '停机(h)', width: 90 }, { prop: 'operator', label: '维护人', width: 90 },
  { prop: 'createdAt', label: '时间', minWidth: 160 }
]
</script>
