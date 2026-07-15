<template>
  <RoleWorkbenchScreen
    :data="data"
    :loading="loading"
    theme="order"
    :filter-days="days"
    :filter-status="status"
    :status-options="data.statusOptions || []"
    @refresh="load"
    @filter-change="onFilterChange"
  />
</template>

<script setup>
import RoleWorkbenchScreen from '@/components/workbench/RoleWorkbenchScreen.vue'
import { useRoleWorkbenchDashboard } from '@/composables/useRoleWorkbenchDashboard'

const { loading, data, load, days, status, onFilterChange } = useRoleWorkbenchDashboard('order')
</script>
