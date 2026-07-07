<template>
  <RoleWorkbench role-key="order" :status-items="statusItems" :shortcuts="shortcuts" />
</template>
<script setup>
import { computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'

const mes = useMesStore()
const statusItems = computed(() => [
  { label: '订单总数', value: mes.orders.length },
  { label: '待审核', value: mes.pendingOrders.length, warn: mes.pendingOrders.length > 0 },
  { label: '已审核', value: mes.approvedOrders.length },
  { label: '生产中', value: mes.orders.filter(o => o.status === '生产中').length },
  { label: '已发货', value: mes.orders.filter(o => o.status === '已发货').length }
])
const shortcuts = [
  { label: '客户订单', path: '/order/list' },
  { label: '订单审核', path: '/order/audit' },
  { label: '订单跟踪', path: '/order/track' }
]
</script>
