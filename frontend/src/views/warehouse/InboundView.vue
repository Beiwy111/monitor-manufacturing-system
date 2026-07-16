<template>
  <MesPageShell toolbar-title="成品入库" :detail-rows="rows" compact-table>
    <template #table>
      <el-alert
        type="info"
        :closable="false"
        title="入库任务由质检员检验合格后自动生成。确认入库前请选择库位图上的空闲储位，入库后库位图将实时更新。"
        style="margin-bottom: 12px"
      />
      <el-table
        :data="mes.pendingInbound"
        border
        stripe
        style="width:100%"
        highlight-current-row
        @current-change="onRowClick"
      >
        <el-table-column prop="id" label="入库单" min-width="110" show-overflow-tooltip />
        <el-table-column prop="orderId" label="订单" min-width="120" show-overflow-tooltip />
        <el-table-column prop="workOrderId" label="工单" min-width="120" show-overflow-tooltip />
        <el-table-column prop="productModel" label="产品型号" min-width="140" show-overflow-tooltip />
        <el-table-column prop="quantity" label="数量" min-width="90" align="right" />
        <el-table-column prop="batchNo" label="批次" min-width="140" show-overflow-tooltip />
        <el-table-column prop="sourceType" label="来源" min-width="90" align="center" />
        <el-table-column prop="status" label="状态" min-width="100" align="center">
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
          <span v-if="lastSlotLabel"> · 库位：{{ lastSlotLabel }}</span>
          <el-button link type="primary" @click="copyBarcode(lastBarcode.barcodeNo)">复制</el-button>
        </template>
      </el-alert>
    </template>
    <template #detail-extra>
      <div v-if="selected?.status === '待入库'" class="inbound-slot">
        <div class="inbound-slot__head">
          <span class="inbound-slot__label">存放库位</span>
          <el-button link type="primary" @click="router.push('/warehouse/location-map')">查看库位图</el-button>
        </div>
        <LocationSlotPicker v-model="selectedSlot" category="FINISHED" />
      </div>
    </template>
    <template #detail-actions>
      <el-button
        v-if="selected?.status==='待入库'"
        type="primary"
        size="small"
        :loading="confirming"
        :disabled="!selectedSlot"
        @click="confirm"
      >确认入库</el-button>
      <el-button v-if="selected" type="danger" size="small" plain @click="removeInbound(selected)">删除任务</el-button>
    </template>
  </MesPageShell>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import { useMesDelete } from '@/composables/useMesDelete'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'
import LocationSlotPicker from '@/components/warehouse/LocationSlotPicker.vue'

const router = useRouter()
const mes = useMesStore()
const userStore = useUserStore()
const { runDelete } = useMesDelete(mes, userStore)
const confirming = ref(false)
const lastBarcode = ref(null)
const lastSlotLabel = ref('')
const selectedSlot = ref('')
const { selected, onRowClick } = useMesFilter(computed(() => mes.pendingInbound), ['id'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'orderId', label: '订单' }, { key: 'workOrderId', label: '工单' },
  { key: 'productModel', label: '型号' }, { key: 'quantity', label: '数量' },
  { key: 'batchNo', label: '批次' }, { key: 'status', label: '状态' }
]))

watch(selected, () => {
  selectedSlot.value = ''
})

async function confirm() {
  if (!selected.value || confirming.value) return
  if (!selectedSlot.value) {
    ElMessage.warning('请先选择存放库位')
    return
  }
  const taskId = selected.value.id
  confirming.value = true
  try {
    const result = await mes.confirmInbound(
      taskId,
      userStore.userInfo?.username,
      userStore.roleKey,
      selectedSlot.value
    )
    lastBarcode.value = result?.barcodeNo ? result : null
    lastSlotLabel.value = selectedSlot.value
    ElMessage.success(lastBarcode.value?.barcodeNo
      ? `入库成功，已生成条码 ${lastBarcode.value.barcodeNo}`
      : '入库成功，库存与库位图已更新')
    selectedSlot.value = ''
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

<style scoped>
.inbound-slot {
  margin-top: 12px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}
.inbound-slot__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.inbound-slot__label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
</style>
