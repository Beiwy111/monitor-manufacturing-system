<template>
  <MesDataPage title="菜单管理" data-key="sysMenus" :columns="cols" />
</template>
<script setup>
import MesDataPage from '@/components/mes/MesDataPage.vue'
const cols = [
  { prop: 'menuName', label: '菜单名', minWidth: 140 }, { prop: 'path', label: '路径', minWidth: 160 },
  { prop: 'sort', label: '排序', width: 70 }
]
</script>
