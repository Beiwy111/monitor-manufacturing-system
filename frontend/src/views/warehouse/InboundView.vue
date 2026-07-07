<template>
  <MesPageShell toolbar-title="成品入库" :detail-rows="rows" :logs="mes.operationLogs.slice(0,8)">
    <template #table>
      <el-alert
        type="info"
        :closable="false"
        title="入库任务由质检员检验合格后自动生成，确认后更新成品库存并创建发货待办。"
        style="margin-bottom: 12px"
      />
      <el-table :data="mes.pendingInbound" highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="入库单" width="100" />
        <el-table-column prop="productModel" label="产品型号" min-width="140" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="batchNo" label="批次" min-width="140" />
        <el-table-column prop="sourceType" label="来源" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-actions>
      <el-button v-if="selected?.status==='待入库'" type="primary" size="small" @click="confirm">确认入库</el-button>
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
const { selected, onRowClick } = useMesFilter(computed(() => mes.pendingInbound), ['id'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'productModel', label: '型号' }, { key: 'quantity', label: '数量' }, { key: 'batchNo', label: '批次' }, { key: 'status', label: '状态' }
]))
function confirm() {
  mes.confirmInbound(selected.value.id, userStore.displayName, userStore.roleKey)
  ElMessage.success('入库成功，库存已更新')
}
</script>
