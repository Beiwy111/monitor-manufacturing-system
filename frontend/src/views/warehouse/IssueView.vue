<template>
  <MesPageShell
    toolbar-title="生产领料记录"
    :status-options="['待领料', '部分领料', '已完成']"
    :detail-rows="rows"
    compact-table
  >
    <template #table>
      <el-table
        :data="mes.issueTasks"
        border
        stripe
        style="width:100%"
        highlight-current-row
        @current-change="onRowClick"
      >
        <el-table-column prop="id" label="领料单" min-width="110" show-overflow-tooltip />
        <el-table-column prop="workOrderId" label="工单" min-width="140" show-overflow-tooltip />
        <el-table-column prop="materialName" label="物料" min-width="180" show-overflow-tooltip />
        <el-table-column prop="requiredQty" label="需求量" min-width="100" align="right" />
        <el-table-column prop="issuedQty" label="已领" min-width="100" align="right" />
        <el-table-column prop="status" label="状态" min-width="110" align="center">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-extra>
      <p v-if="selected" class="issue-view__hint">
        领料由操作员在「我的派工」中完成；仓储此处仅查看领料进度与出库记录，无需确认领料。
      </p>
    </template>
    <template #detail-actions>
      <el-button v-if="selected" type="danger" size="small" plain @click="removeIssue(selected)">删除任务</el-button>
    </template>
  </MesPageShell>
</template>

<script setup>
import { computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import { useMesDelete } from '@/composables/useMesDelete'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const { runDelete } = useMesDelete(mes, userStore)
const { selected, onRowClick } = useMesFilter(computed(() => mes.issueTasks), ['id'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'workOrderId', label: '工单' }, { key: 'materialCode', label: '物料编码' },
  { key: 'materialName', label: '物料' }, { key: 'requiredQty', label: '需求' },
  { key: 'issuedQty', label: '已领' }, { key: 'status', label: '状态' }
]))

function removeIssue(row) {
  if (!row) return
  runDelete({
    action: 'deleteIssueTask',
    payload: { taskId: row.id },
    message: `确定删除领料任务 ${row.id}？`,
    onSuccess: () => {
      if (selected.value?.id === row.id) selected.value = null
    }
  }).catch(() => {})
}
</script>

<style scoped>
.issue-view__hint {
  margin: 12px 0 0;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
  background: #f4f6f8;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}
</style>
