<template>
  <MesPageShell
    toolbar-title="生产领料"
    :status-options="['待领料', '部分领料', '已完成']"
    :detail-rows="rows"
    :logs="mes.operationLogs.slice(0,8)"
  >
    <template #table>
      <el-table :data="mes.issueTasks" border stripe highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="领料单" width="100" />
        <el-table-column prop="workOrderId" label="工单" width="130" />
        <el-table-column prop="materialName" label="物料" min-width="120" />
        <el-table-column prop="requiredQty" label="需求量" width="80" />
        <el-table-column prop="issuedQty" label="已领" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-extra>
      <el-form v-if="selected" inline style="margin-top:12px">
        <el-form-item label="库存余量">
          <el-tag :type="stockQty >= issueQty ? 'success' : 'danger'">{{ stockQty }}</el-tag>
        </el-form-item>
        <el-form-item label="领料数量"><el-input-number v-model="issueQty" :min="1" :max="Math.max(1, remainQty)" /></el-form-item>
        <el-button
          type="primary"
          :loading="issuing"
          :disabled="selected.status === '已完成' || stockQty < issueQty"
          @click="issue"
        >确认领料</el-button>
      </el-form>
    </template>
    <template #detail-actions>
      <el-button v-if="selected" type="danger" size="small" plain @click="removeIssue(selected)">删除任务</el-button>
    </template>
  </MesPageShell>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import { useMesDelete } from '@/composables/useMesDelete'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const { runDelete } = useMesDelete(mes, userStore)
const issueQty = ref(10)
const issuing = ref(false)
const { selected, onRowClick } = useMesFilter(computed(() => mes.issueTasks), ['id'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'workOrderId', label: '工单' }, { key: 'materialCode', label: '物料编码' },
  { key: 'materialName', label: '物料' }, { key: 'requiredQty', label: '需求' },
  { key: 'issuedQty', label: '已领' }, { key: 'status', label: '状态' }
]))
const stockQty = computed(() => {
  if (!selected.value) return 0
  const inv = mes.inventory.find((i) => i.materialCode === selected.value.materialCode)
  return inv?.quantity ?? 0
})
const remainQty = computed(() => {
  if (!selected.value) return 1
  return Math.max(1, (selected.value.requiredQty || 0) - (selected.value.issuedQty || 0))
})
watch(selected, (row) => {
  if (row) issueQty.value = Math.min(remainQty.value, Math.max(1, stockQty.value))
})
async function issue() {
  if (!selected.value || issuing.value) return
  issuing.value = true
  try {
    const ok = await mes.issueMaterial(
      selected.value.id,
      issueQty.value,
      userStore.displayName,
      userStore.roleKey
    )
    if (ok !== false) {
      ElMessage.success('领料成功')
    } else {
      ElMessage.error('库存不足')
    }
  } catch {
    // 全局 request 拦截器已提示后端错误
  } finally {
    issuing.value = false
  }
}
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
