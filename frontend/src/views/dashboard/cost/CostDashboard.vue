<template>
  <div class="dashboard-wrap">
    <RoleWorkbench role-key="cost" :status-items="statusItems" :shortcuts="shortcuts" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import RoleWorkbench from '@/components/workbench/RoleWorkbench.vue'
import { fetchCostKpi } from '@/api/cost'

const kpi = ref({ total: 0, draft: 0, confirmed: 0, exported: 0, totalQualityCost: 0, totalAmount: 0 })

function fmtAmt(v) {
  const n = Number(v)
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}

const statusItems = computed(() => [
  { label: '结算总数', value: kpi.value.total     ?? 0 },
  { label: '草稿',    value: kpi.value.draft      ?? 0, warn: (kpi.value.draft ?? 0) > 0 },
  { label: '已确认',  value: kpi.value.confirmed  ?? 0 },
  { label: '质量成本', value: '¥' + fmtAmt(kpi.value.totalQualityCost) },
])

const shortcuts = [
  { label: '成本结算', path: '/cost/settlement' },
  { label: '工单成本', path: '/cost/work-order' },
  { label: '材料成本', path: '/cost/material' },
  { label: '成本报表', path: '/cost/report' },
]

onMounted(async () => {
  try {
    const res = await fetchCostKpi()
    kpi.value = res.data ?? res
  } catch { /* 加载失败不阻断工作台 */ }
})
</script>
