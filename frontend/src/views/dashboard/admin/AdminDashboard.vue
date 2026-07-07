<template>
  <RoleWorkbench role-key="admin" :status-items="statusItems" :shortcuts="shortcuts" />
</template>
<script setup>
import { computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'

const mes = useMesStore()
const s = computed(() => mes.stats)
const statusItems = computed(() => [
  { label: '在线用户', value: s.value.onlineUsers },
  { label: '角色数', value: s.value.roleCount },
  { label: '菜单数', value: s.value.menuCount },
  { label: '今日日志', value: s.value.todayLogs },
  { label: '异常告警', value: s.value.openAlarm, danger: s.value.openAlarm > 0 }
])
const shortcuts = [
  { label: '用户管理', path: '/system/user' },
  { label: '角色管理', path: '/system/role' },
  { label: '菜单管理', path: '/system/menu' },
  { label: '操作日志', path: '/system/log' },
  { label: '系统看板', path: '/system/board' }
]
</script>
