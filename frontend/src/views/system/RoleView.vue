<template>
  <MesDataPage title="角色管理" data-key="sysRoles" :columns="cols" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'roleName', label: '角色', width: 120 }, { prop: 'roleKey', label: '标识', width: 100 },
  { prop: 'userCount', label: '用户数', width: 80 }, { prop: 'permCount', label: '权限数', width: 80 },
  { prop: 'status', label: '状态', width: 80 }
]
</script>
