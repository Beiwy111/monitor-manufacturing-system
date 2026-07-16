<template>
  <RoleWorkbenchScreen
    :data="data"
    :loading="loading"
    theme="warehouse"
    :filter-days="days"
    @refresh="load"
    @filter-change="onFilterChange"
  >
    <template #toolbar>
      <div class="wh-toolbar">
        <div class="wh-toolbar__summary">
          <strong>仓储概览</strong>
          <span v-if="pendingInbound > 0" class="wh-badge wh-badge--warn">待入库 {{ pendingInbound }}</span>
          <span v-if="pendingOutbound > 0" class="wh-badge wh-badge--info">待出库 {{ pendingOutbound }}</span>
          <span v-if="stockAlert > 0" class="wh-badge wh-badge--danger">库存预警 {{ stockAlert }}</span>
        </div>
        <div class="wh-toolbar__actions">
          <el-button size="small" type="primary" @click="$router.push('/warehouse/workbench')">仓储管理工作台</el-button>
          <el-button size="small" @click="$router.push('/warehouse/inbound-hub')">入库</el-button>
          <el-button size="small" @click="$router.push('/warehouse/outbound-hub')">出库</el-button>
          <el-button size="small" @click="$router.push('/warehouse/capacity')">库存容量查询</el-button>
        </div>
      </div>
    </template>
  </RoleWorkbenchScreen>
</template>

<script setup>
import { computed } from 'vue'
import RoleWorkbenchScreen from '@/components/workbench/RoleWorkbenchScreen.vue'
import { useRoleWorkbenchDashboard } from '@/composables/useRoleWorkbenchDashboard'

const { loading, data, load, days, onFilterChange } = useRoleWorkbenchDashboard('warehouse')

const pendingInbound = computed(() => metricVal('pendingInbound'))
const pendingOutbound = computed(() => metricVal('pendingOutbound'))
const stockAlert = computed(() => metricVal('stockAlert'))

function metricVal(key) {
  const m = data.value.metrics?.find((x) => x.key === key)
  return Number(m?.value || 0)
}
</script>

<style scoped>
.wh-toolbar {
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

.wh-toolbar__summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #606266;
}

.wh-toolbar__summary strong {
  color: #303133;
  margin-right: 4px;
}

.wh-toolbar__actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.wh-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.wh-badge--warn {
  background: #fff7e6;
  color: #d46b08;
  border: 1px solid #ffd591;
}

.wh-badge--info {
  background: #e6f4ff;
  color: #0958d9;
  border: 1px solid #91caff;
}

.wh-badge--danger {
  background: #fff1f0;
  color: #cf1322;
  border: 1px solid #ffa39e;
}
</style>
