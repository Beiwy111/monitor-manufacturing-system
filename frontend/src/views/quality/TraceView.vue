<template>
  <MesPageShell toolbar-title="质量追溯" :show-log="false">
    <template #toolbar-extra>
      <el-input v-model="orderId" placeholder="输入订单号" style="width:180px" />
      <el-button type="primary" @click="search">追溯</el-button>
    </template>
    <template #table>
      <div class="ruoyi-content-block">
        <ProcessTimeline v-if="chain" :items="items" />
        <p v-else class="ruoyi-empty-tip">输入订单号查询全流程追溯</p>
      </div>
    </template>
  </MesPageShell>
</template>
<script setup>
import { ref, computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import ProcessTimeline from '@/components/mes/ProcessTimeline.vue'
const mes = useMesStore()
const orderId = ref('ORD-2026-003')
const chain = computed(() => orderId.value ? mes.traceChain(orderId.value) : null)
const items = computed(() => {
  if (!chain.value?.order) return []
  const c = chain.value
  return [
    { title: '订单', desc: `${c.order.id} · ${c.order.status}` },
    { title: '计划', desc: c.plan ? `${c.plan.id} · ${c.plan.status}` : '无' },
    { title: '工单', desc: c.wo ? `${c.wo.id} · 完成 ${c.wo.completedQty}/${c.wo.quantity}` : '无' },
    { title: '派工', desc: `${c.dispatches.length} 条` },
    { title: '报工', desc: `${c.reports.length} 条` },
    { title: '质检', desc: `${c.inspections.length} 条` },
    { title: '发货', desc: c.deliveries[0]?.status || '未发货' }
  ]
})
function search() {}
</script>
