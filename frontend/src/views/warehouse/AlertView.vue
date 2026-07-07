<template>
  <MesDataPage title="库存预警" data-key="inventory" :columns="cols" :filter-fn="f => f.filter(i => i.quantity < i.safeQty)" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'materialName', label: '物料', minWidth: 140 }, { prop: 'quantity', label: '当前库存', width: 90 },
  { prop: 'safeQty', label: '安全库存', width: 90 }, { prop: 'location', label: '库位', width: 100 }
]
</script>
