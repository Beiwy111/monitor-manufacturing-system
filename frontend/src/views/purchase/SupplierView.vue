<template>
  <MesDataPage title="供应商管理" data-key="suppliers" :columns="cols" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'name', label: '供应商', minWidth: 140 }, { prop: 'materials', label: '供应物料', minWidth: 140 },
  { prop: 'contact', label: '联系人', width: 100 }, { prop: 'phone', label: '电话', width: 130 }
]
</script>
