<template>
  <RoleWorkbench role-key="aftersale" :status-items="statusItems" :shortcuts="shortcuts" />
</template>
<script setup>
import { computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'

const mes = useMesStore()
const statusItems = computed(() => [
  { label: '售后单', value: mes.aftersaleCases.length },
  { label: '待处理', value: mes.aftersaleCases.filter(c => c.status !== '已关闭').length, warn: true },
  { label: '追溯中', value: mes.aftersaleCases.filter(c => c.status === '追溯中').length },
  { label: '已关闭', value: mes.aftersaleCases.filter(c => c.status === '已关闭').length }
])
const shortcuts = [
  { label: '售后登记', path: '/aftersale/case' },
  { label: '客户反馈', path: '/aftersale/feedback' },
  { label: '质量追溯', path: '/aftersale/trace' },
  { label: '发货查询', path: '/delivery/list' }
]
</script>
