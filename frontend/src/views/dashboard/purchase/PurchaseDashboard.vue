<template>
  <RoleWorkbenchScreen
    :data="data"
    :loading="loading"
    theme="purchase"
    :filter-days="days"
    @refresh="load"
    @filter-change="onFilterChange"
  >
    <template #toolbar>
      <div class="pur-toolbar">
        <div class="pur-toolbar__summary">
          <strong>采购概览</strong>
          <span v-if="pendingCount > 0" class="pur-badge pur-badge--warn">待采购 {{ pendingCount }}</span>
          <span v-if="overdueCount > 0" class="pur-badge pur-badge--danger">逾期未到 {{ overdueCount }}</span>
        </div>
        <div class="pur-toolbar__actions">
          <el-button size="small" type="primary" @click="$router.push('/purchase/workbench')">采购工作台</el-button>
          <el-button size="small" @click="$router.push('/purchase/demand')">采购需求</el-button>
          <el-button size="small" @click="$router.push('/purchase/order')">采购订单</el-button>
          <el-button size="small" @click="$router.push('/purchase/supplier')">供应商管理</el-button>
        </div>
      </div>
    </template>
  </RoleWorkbenchScreen>
</template>

<script setup>
import { computed } from 'vue'
import RoleWorkbenchScreen from '@/components/workbench/RoleWorkbenchScreen.vue'
import { useRoleWorkbenchDashboard } from '@/composables/useRoleWorkbenchDashboard'

const { loading, data, load, days, onFilterChange } = useRoleWorkbenchDashboard('purchase')

const pendingCount = computed(() => {
  const m = data.value.metrics?.find((x) => x.key === 'pendingReq')
  return Number(m?.value || 0)
})

const overdueCount = computed(() => {
  const m = data.value.metrics?.find((x) => x.key === 'overdue')
  return Number(m?.value || 0)
})
</script>

<style scoped>
.pur-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 14px;
  margin: 0 14px 10px;
  background: #fff;
  border: 1px solid #e8ece9;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(30, 50, 40, 0.04);
  flex-shrink: 0;
}

.pur-toolbar__summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #606266;
}

.pur-toolbar__summary strong {
  color: #303133;
  margin-right: 4px;
}

.pur-toolbar__actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.pur-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.pur-badge--warn {
  background: #fff7e6;
  color: #d46b08;
  border: 1px solid #ffd591;
}

.pur-badge--danger {
  background: #fff1f0;
  color: #cf1322;
  border: 1px solid #ffa39e;
}
</style>
