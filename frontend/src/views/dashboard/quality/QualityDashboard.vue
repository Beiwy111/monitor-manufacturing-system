<template>
  <RoleWorkbench role-key="quality" :status-items="statusItems" :shortcuts="shortcuts" />
</template>
<script setup>
import { computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'

const mes = useMesStore()
const statusItems = computed(() => [
  { label: '待检任务', value: mes.pendingInspections.length, warn: mes.pendingInspections.length > 0 },
  { label: '已合格', value: mes.inspections.filter(i => i.status === '合格').length },
  { label: '不合格品', value: mes.defects.length, danger: mes.defects.length > 0 },
  { label: '质检总数', value: mes.inspections.length }
])
const shortcuts = [
  { label: '待检任务', path: '/quality/inspection' },
  { label: '不合格品', path: '/quality/defect' },
  { label: '复检处理', path: '/quality/reinspect' },
  { label: '质量追溯', path: '/quality/trace' }
]
</script>
