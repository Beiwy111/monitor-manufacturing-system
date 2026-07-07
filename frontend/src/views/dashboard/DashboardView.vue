<template>
  <div class="page-card">
    <div class="page-title">工作台</div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="item in cards" :key="item.title">
        <el-card shadow="hover">
          <div class="stat-title">{{ item.title }}</div>
          <div class="stat-value">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-alert
      style="margin-top: 20px"
      title="系统已切换为前后端分离架构，左侧菜单来自后端 /auth/menus 接口。"
      type="success"
      :closable="false"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { fetchOrderList, fetchWorkOrderList, fetchInspectionList, fetchInventoryList } from '@/api/business'

const cards = ref([
  { title: '客户订单', value: 0 },
  { title: '生产工单', value: 0 },
  { title: '质检任务', value: 0 },
  { title: '库存记录', value: 0 }
])

onMounted(async () => {
  const [orders, workOrders, inspections, inventories] = await Promise.all([
    fetchOrderList().catch(() => []),
    fetchWorkOrderList().catch(() => []),
    fetchInspectionList().catch(() => []),
    fetchInventoryList().catch(() => [])
  ])
  cards.value = [
    { title: '客户订单', value: orders.length },
    { title: '生产工单', value: workOrders.length },
    { title: '质检任务', value: inspections.length },
    { title: '库存记录', value: inventories.length }
  ]
})
</script>

<style scoped>
.stat-title { color: var(--text-placeholder); font-size: var(--fs-kpi-label); font-weight: var(--nav-weight); }
.stat-value { font-size: var(--fs-kpi-value); font-weight: var(--heading-weight); line-height: var(--lh-tight); margin-top: 8px; color: var(--heading-color); }
</style>
