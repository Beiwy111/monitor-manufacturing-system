<template>
  <RoleWorkbench role-key="cost" :status-items="statusItems" :shortcuts="shortcuts" />
</template>
<script setup>
import { computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'

const mes = useMesStore()
const statusItems = computed(() => [
  { label: '结算单', value: mes.costSettlements.length },
  { label: '草稿', value: mes.costSettlements.filter(c => c.status === '草稿').length, warn: true },
  { label: '已确认', value: mes.costSettlements.filter(c => c.status === '已确认').length },
  { label: '工单数', value: mes.workOrders.length }
])
const shortcuts = [
  { label: '成本结算', path: '/cost/settlement' },
  { label: '工单成本', path: '/cost/work-order' },
  { label: '材料成本', path: '/cost/material' },
  { label: '成本报表', path: '/cost/report' }
]
</script>
