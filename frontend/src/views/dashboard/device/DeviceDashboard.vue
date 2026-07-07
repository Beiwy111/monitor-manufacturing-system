<template>
  <RoleWorkbench role-key="device" :status-items="statusItems" :shortcuts="shortcuts" />
</template>
<script setup>
import { computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'

const mes = useMesStore()
const statusItems = computed(() => [
  { label: '设备总数', value: mes.equipment.length },
  { label: '故障设备', value: mes.equipment.filter(e => e.status === '故障').length, danger: true },
  { label: '待处理报警', value: mes.openAlarms.length, warn: true },
  { label: '维护记录', value: mes.maintenanceRecords.length }
])
const shortcuts = [
  { label: '设备台账', path: '/device/equipment' },
  { label: '安灯报警', path: '/device/alarm' },
  { label: '维修处理', path: '/device/maintenance' },
  { label: '维护记录', path: '/device/records' }
]
</script>
