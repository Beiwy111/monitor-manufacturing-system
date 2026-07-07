<template>
  <RoleWorkbench role-key="manager" :status-items="statusItems" :shortcuts="shortcuts" />
</template>
<script setup>
import { computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'

const mes = useMesStore()
const statusItems = computed(() => [
  { label: '待计划订单', value: mes.pendingPlanOrders.length, warn: mes.pendingPlanOrders.length > 0 },
  { label: '生产计划', value: mes.plans.length },
  { label: '待派工', value: mes.workOrders.filter(w => w.status === '已下达').length, warn: true },
  { label: '生产中', value: mes.workOrders.filter(w => w.status === '生产中').length },
  { label: '安灯报警', value: mes.openAlarms.length, danger: mes.openAlarms.length > 0 }
])
const shortcuts = [
  { label: '生产计划', path: '/production/plan' },
  { label: '生产工单', path: '/production/work-order' },
  { label: '工单派工', path: '/production/dispatch' },
  { label: '生产进度', path: '/production/progress' },
  { label: '安灯报警', path: '/device/alarm' }
]
</script>
