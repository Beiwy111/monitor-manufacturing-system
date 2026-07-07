<template>
  <MesPageShell toolbar-title="库存查询" :detail-rows="rows" :logs="mes.operationLogs.slice(0,6)">
    <template #table>
      <el-table :data="mes.inventory" highlight-current-row @current-change="onRowClick">
        <el-table-column prop="materialCode" label="编码" width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="quantity" label="库存量" width="90" />
        <el-table-column prop="safeQty" label="安全库存" width="90" />
        <el-table-column prop="unit" label="单位" width="60" />
        <el-table-column prop="location" label="库位" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
      </el-table>
    </template>
  </MesPageShell>
</template>

<script setup>
import { computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const { selected, onRowClick } = useMesFilter(computed(() => mes.inventory), ['materialName'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'materialName', label: '物料' }, { key: 'quantity', label: '库存' }, { key: 'safeQty', label: '安全库存' }, { key: 'location', label: '库位' }
]))
</script>
