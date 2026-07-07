<template>
  <RoleWorkbench role-key="warehouse" :status-items="statusItems" :shortcuts="shortcuts" />
</template>
<script setup>
import { computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'

const mes = useMesStore()
const statusItems = computed(() => [
  { label: '待入库', value: mes.pendingInbound.length, warn: mes.pendingInbound.length > 0 },
  { label: '待出库', value: mes.deliveries.filter(d => d.status === '待出库').length },
  { label: '库存预警', value: mes.stats.stockAlert, danger: mes.stats.stockAlert > 0 },
  { label: '库存品种', value: mes.inventory.length }
])
const shortcuts = [
  { label: '成品入库', path: '/warehouse/inbound' },
  { label: '生产领料', path: '/warehouse/issue' },
  { label: '发货出库', path: '/delivery/list' },
  { label: '库存查询', path: '/warehouse/inventory' }
]
</script>
