<template>
  <div class="dashboard-wrap">
    <RoleWorkbench role-key="aftersale" :status-items="statusItems" :shortcuts="shortcuts" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'
import { fetchAfterSalesKpi } from '@/api/aftersale'

const kpi = ref({ total: 0, open: 0, processing: 0, resolved: 0, closed: 0 })

const statusItems = computed(() => [
  { label: '售后总数',  value: kpi.value.total      ?? 0 },
  { label: '待受理',   value: kpi.value.open        ?? 0, warn: (kpi.value.open ?? 0) > 0 },
  { label: '处理中',   value: kpi.value.processing  ?? 0, warn: (kpi.value.processing ?? 0) > 0 },
  { label: '已解决',   value: kpi.value.resolved    ?? 0 },
])

const shortcuts = [
  { label: '售后登记',   path: '/aftersale/case' },
  { label: '售后处理',   path: '/aftersale/process' },
  { label: '客户反馈',   path: '/aftersale/feedback' },
  { label: '质量追溯',   path: '/aftersale/trace' },
]

onMounted(async () => {
  try {
    const res = await fetchAfterSalesKpi()
    kpi.value = res.data ?? res
  } catch { /* 加载失败不阻断工作台 */ }
})
</script>
