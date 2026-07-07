<template>
  <MesPageShell toolbar-title="生产领料" :detail-rows="rows" :logs="mes.operationLogs.slice(0,8)">
    <template #table>
      <el-table :data="mes.issueTasks" highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="领料单" width="100" />
        <el-table-column prop="workOrderId" label="工单" width="130" />
        <el-table-column prop="materialName" label="物料" min-width="120" />
        <el-table-column prop="requiredQty" label="需求量" width="80" />
        <el-table-column prop="issuedQty" label="已领" width="80" />
        <el-table-column prop="status" label="状态" width="100" />
      </el-table>
    </template>
    <template #detail-extra>
      <el-form v-if="selected" inline style="margin-top:12px">
        <el-form-item label="领料数量"><el-input-number v-model="issueQty" :min="1" /></el-form-item>
        <el-button type="primary" @click="issue">确认领料</el-button>
      </el-form>
    </template>
  </MesPageShell>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import MesPageShell from '@/components/mes/MesPageShell.vue'

const mes = useMesStore()
const userStore = useUserStore()
const issueQty = ref(10)
const { selected, onRowClick } = useMesFilter(computed(() => mes.issueTasks), ['id'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'materialName', label: '物料' }, { key: 'requiredQty', label: '需求' }, { key: 'issuedQty', label: '已领' }, { key: 'status', label: '状态' }
]))
function issue() {
  if (mes.issueMaterial(selected.value.id, issueQty.value, userStore.displayName, userStore.roleKey)) {
    ElMessage.success('领料成功')
  } else ElMessage.error('库存不足')
}
</script>
