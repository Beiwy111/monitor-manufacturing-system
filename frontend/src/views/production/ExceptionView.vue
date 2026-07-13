<template>
  <MesDataPage
    title="生产异常"
    data-key="alarms"
    :columns="cols"
    :filter-fn="f => f.filter(a => a.status !== '已关闭')"
    delete-action="deleteAlarm"
    delete-payload-key="alarmId"
  />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'id', label: '报警号', width: 130 }, { prop: 'type', label: '类型', width: 90 },
  { prop: 'description', label: '描述', minWidth: 160 }, { prop: 'status', label: '状态', width: 90 }
]
</script>
