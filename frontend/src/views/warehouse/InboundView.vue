<template>
  <MesPageShell toolbar-title="成品入库" :detail-rows="rows" :logs="mes.operationLogs.slice(0,8)">
    <template #table>
      <el-alert
        type="info"
        :closable="false"
        title="入库任务由质检员检验合格后自动生成，确认后更新成品库存并创建发货待办。"
        style="margin-bottom: 12px"
      />
      <el-table :data="mes.pendingInbound" border stripe highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="入库单" width="100" />
        <el-table-column prop="orderId" label="订单" width="130" />
        <el-table-column prop="workOrderId" label="工单" width="130" />
        <el-table-column prop="productModel" label="产品型号" min-width="140" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="batchNo" label="批次" min-width="140" />
        <el-table-column prop="sourceType" label="来源" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
      </el-table>
      <el-alert
        v-if="lastBarcode"
        type="success"
        :closable="false"
        style="margin-top: 12px"
      >
        <template #title>
          本次入库条码：{{ lastBarcode.barcodeNo }}
          <el-button link type="primary" @click="copyBarcode(lastBarcode.barcodeNo)">复制</el-button>
        </template>
      </el-alert>
    </template>
    <template #detail-actions>
      <el-button
        v-if="selected?.status==='待入库'"
        type="primary"
        size="small"
        :loading="confirming"
        @click="confirm"
      >确认入库</el-button>
      <el-button v-if="selected" type="danger" size="small" plain @click="removeInbound(selected)">删除任务</el-button>
    </template>
  </MesPageShell>
</template>

<script setup>
import { computed, ref } from 'vue'
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
const confirming = ref(false)
const lastBarcode = ref(null)
const { selected, onRowClick } = useMesFilter(computed(() => mes.pendingInbound), ['id'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'orderId', label: '订单' }, { key: 'workOrderId', label: '工单' },
  { key: 'productModel', label: '型号' }, { key: 'quantity', label: '数量' },
  { key: 'batchNo', label: '批次' }, { key: 'status', label: '状态' }
]))
async function confirm() {
  if (!selected.value || confirming.value) return
  const taskId = selected.value.id
  confirming.value = true
  try {
    const result = await mes.confirmInbound(taskId, userStore.userInfo?.username, userStore.roleKey)
    lastBarcode.value = result?.barcodeNo ? result : null
    ElMessage.success(lastBarcode.value?.barcodeNo
      ? `入库成功，已生成条码 ${lastBarcode.value.barcodeNo}`
      : '入库成功，库存已更新')
  } catch {
    // 全局 request 拦截器已提示后端错误
  } finally {
    confirming.value = false
  }
}
async function copyBarcode(barcodeNo) {
  if (!barcodeNo) return
  await navigator.clipboard?.writeText(barcodeNo)
  ElMessage.success('条码已复制')
}
function removeInbound(row) {
  if (!row) return
  runDelete({
    action: 'deleteInboundTask',
    payload: { taskId: row.id },
    message: `确定删除入库任务 ${row.id}？`,
    onSuccess: () => {
      if (selected.value?.id === row.id) selected.value = null
    }
  }).catch(() => {})
}
</script>
