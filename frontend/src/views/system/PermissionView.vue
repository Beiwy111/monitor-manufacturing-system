<template>
  <MesDataPage title="权限管理" data-key="sysPermissions" :columns="cols" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'code', label: '权限码', width: 140 }, { prop: 'name', label: '名称', width: 120 },
  { prop: 'module', label: '模块', minWidth: 120 }
]
</script>
