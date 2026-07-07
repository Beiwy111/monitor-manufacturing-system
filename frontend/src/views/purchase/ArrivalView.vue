<template>
  <MesDataPage title="到货进度" data-key="purchaseOrders" :columns="cols" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'id', label: '采购单', width: 130 }, { prop: 'materialName', label: '物料', width: 100 },
  { prop: 'quantity', label: '订购', width: 80 }, { prop: 'arrivedQty', label: '到货', width: 80 },
  { prop: 'status', label: '状态', width: 100 }
]
</script>
