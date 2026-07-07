<template>
  <MesPageShell
    toolbar-title="订单审核"
    :status-options="['待审核']"
    :detail-rows="rows"
    :logs="mes.operationLogs.slice(0, 10)"
    v-model:model-keyword="keyword"
    @toolbar-action="onAction"
  >
    <template #table>
      <el-table :data="filtered" highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="订单号" width="140" />
        <el-table-column prop="customerName" label="客户" min-width="120" />
        <el-table-column prop="productModel" label="型号" width="140" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="amount" label="金额" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === '待审核'">
              <el-button link type="primary" @click="selectAndAudit(row, true)">通过</el-button>
              <el-button link type="danger" @click="selectAndAudit(row, false)">驳回</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-actions>
      <el-button v-if="selected?.status==='待审核'" type="primary" size="small" @click="audit(true)">审核通过</el-button>
      <el-button v-if="selected?.status==='待审核'" size="small" @click="audit(false)">驳回作废</el-button>
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
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const { keyword, selected, filtered, onRowClick } = useMesFilter(
  computed(() => mes.orders.filter(o => o.status === '待审核' || keyword.value)),
  ['id', 'customerName']
)
const rows = computed(() => detailRows(selected.value, [
  { key: 'id', label: '订单号' }, { key: 'customerName', label: '客户' },
  { key: 'productModel', label: '型号' }, { key: 'quantity', label: '数量' },
  { key: 'remark', label: '备注' }, { key: 'status', label: '状态' }
]))
function audit(pass) {
  if (!selected.value) return
  mes.auditOrder(selected.value.id, pass, userStore.displayName, userStore.roleKey)
  ElMessage.success(pass ? '审核通过，可创建生产计划' : '订单已作废')
  selected.value = null
}
function selectAndAudit(row, pass) {
  selected.value = row
  audit(pass)
}
function onAction() {}
</script>
