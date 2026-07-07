<template>
  <MesPageShell :toolbar-title="title" :detail-rows="rows" :logs="mes.operationLogs.slice(0,6)">
    <template #table>
      <el-table :data="list" highlight-current-row @current-change="onRowClick">
        <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label" :width="col.width" :min-width="col.minWidth" show-overflow-tooltip />
      </el-table>
    </template>
    <slot />
  </MesPageShell>
</template>

<script setup>
import { computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import MesPageShell from '@/components/mes/MesPageShell.vue'

const props = defineProps({
  title: { type: String, required: true },
  dataKey: { type: String, required: true },
  columns: { type: Array, required: true },
  detailFields: { type: Array, default: () => [] },
  filterFn: { type: Function, default: null }
})

const mes = useMesStore()
const list = computed(() => {
  let data = mes[props.dataKey] || []
  if (props.filterFn) data = props.filterFn(data)
  return data
})
const { selected, onRowClick } = useMesFilter(list, props.columns.map(c => c.prop))
const rows = computed(() => detailRows(selected.value, props.detailFields.length ? props.detailFields : props.columns.map(c => ({ key: c.prop, label: c.label }))))
</script>
