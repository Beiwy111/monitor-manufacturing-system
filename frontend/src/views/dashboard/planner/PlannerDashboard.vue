<template>
  <RoleWorkbench role-key="planner" :status-items="statusItems" :shortcuts="shortcuts" />
</template>
<script setup>
import { computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'

const mes = useMesStore()
const statusItems = computed(() => [
  { label: '待计划订单', value: mes.pendingPlanOrders.length, warn: mes.pendingPlanOrders.length > 0 },
  { label: '草稿计划', value: mes.plans.filter((p) => p.status === '草稿').length, warn: mes.plans.filter((p) => p.status === '草稿').length > 0 },
  { label: '待提交主管', value: mes.pendingSubmitPlans.length, warn: mes.pendingSubmitPlans.length > 0 },
  { label: '执行中计划', value: mes.plans.filter((p) => p.status === '执行中').length }
])
const shortcuts = [
  { label: '生产计划', path: '/production/plan' },
  { label: '订单跟踪', path: '/order/track' }
]
</script>
