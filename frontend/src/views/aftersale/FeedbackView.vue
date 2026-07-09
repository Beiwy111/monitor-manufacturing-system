<template>
  <MesDataPage
    title="客户反馈"
    data-key="aftersaleCases"
    :columns="cols"
    delete-action="deleteAftersale"
    delete-payload-key="caseId"
  />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'customerName', label: '客户', width: 120 }, { prop: 'feedback', label: '反馈', minWidth: 180 },
  { prop: 'status', label: '状态', width: 90 }
]
</script>
