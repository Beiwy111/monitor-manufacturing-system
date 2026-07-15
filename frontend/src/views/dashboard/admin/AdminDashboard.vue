<template>
  <RoleWorkbenchScreen
    :data="data"
    :loading="loading"
    theme="admin"
    :filter-days="days"
    @refresh="load"
    @filter-change="onFilterChange"
  />
</template>

<script setup>
import RoleWorkbenchScreen from '@/components/workbench/RoleWorkbenchScreen.vue'
import { useRoleWorkbenchDashboard } from '@/composables/useRoleWorkbenchDashboard'

const { loading, data, load, days, onFilterChange } = useRoleWorkbenchDashboard('admin')
</script>
