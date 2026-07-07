<template>
  <MesDataPage title="设备状态" data-key="equipment" :columns="cols" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'name', label: '设备', minWidth: 140 }, { prop: 'type', label: '类型', width: 120 },
  { prop: 'line', label: '产线', width: 100 }, { prop: 'status', label: '状态', width: 90 },
  { prop: 'downtimeHours', label: '停机(h)', width: 90 }
]
</script>
