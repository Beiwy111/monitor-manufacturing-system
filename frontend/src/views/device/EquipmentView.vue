<template>
  <MesPageShell toolbar-title="设备台账" :detail-rows="rows" :logs="mes.operationLogs.slice(0,6)">
    <template #table>
      <el-table :data="mes.equipment" border stripe highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="编号" width="100" />
        <el-table-column prop="name" label="设备名称" min-width="140" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="line" label="产线" width="100" />
        <el-table-column prop="status" label="状态" width="90" />
        <el-table-column prop="downtimeHours" label="停机(h)" width="90" />
      </el-table>
    </template>
    <template #detail-actions>
      <el-button v-if="selected?.status==='故障'" size="small" type="primary" @click="repair">维修完成</el-button>
    </template>
  </MesPageShell>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import MesPageShell from '@/components/mes/MesPageShell.vue'

const mes = useMesStore()
const userStore = useUserStore()
const { selected, onRowClick } = useMesFilter(computed(() => mes.equipment), ['name'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'name', label: '设备' }, { key: 'type', label: '类型' }, { key: 'status', label: '状态' }, { key: 'lastMaint', label: '上次维护' }
]))
function repair() {
  mes.updateEquipment(selected.value.id, { status: '运行中', repairNote: '故障已修复', downtimeHours: 0 }, userStore.displayName, userStore.roleKey)
  ElMessage.success('设备已恢复运行')
}
</script>
