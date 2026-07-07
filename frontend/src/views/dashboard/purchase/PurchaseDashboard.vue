<template>
  <RoleWorkbench role-key="purchase" :status-items="statusItems" :shortcuts="shortcuts" />
</template>
<script setup>
import { computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'

const mes = useMesStore()
const statusItems = computed(() => [
  { label: '采购需求', value: mes.purchaseDemands.length },
  { label: '待采购', value: mes.purchaseDemands.filter(d => d.status === '待采购').length, warn: true },
  { label: '采购订单', value: mes.purchaseOrders.length },
  { label: '部分到货', value: mes.purchaseOrders.filter(p => p.status === '部分到货').length }
])
const shortcuts = [
  { label: '采购需求', path: '/purchase/demand' },
  { label: '采购订单', path: '/purchase/order' },
  { label: '供应商', path: '/purchase/supplier' },
  { label: '到货进度', path: '/purchase/arrival' }
]
</script>
