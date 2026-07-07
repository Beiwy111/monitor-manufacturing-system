<template>
  <MesDataPage title="操作日志" data-key="operationLogs" :columns="cols" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'createdAt', label: '时间', minWidth: 160 }, { prop: 'operator', label: '操作人', width: 100 },
  { prop: 'module', label: '模块', width: 100 }, { prop: 'action', label: '动作', width: 100 },
  { prop: 'target', label: '对象', minWidth: 120 }
]
</script>
