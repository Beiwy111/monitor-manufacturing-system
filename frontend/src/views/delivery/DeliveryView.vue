<template>
  <MesPageShell toolbar-title="发货管理" :status-options="DELIVERY_STATUS" :detail-rows="rows" :logs="mes.operationLogs.slice(0,8)">
    <template #table>
      <el-table :data="mes.deliveries" border stripe highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="发货单" width="130" />
        <el-table-column prop="orderNo" label="订单" width="130" />
        <el-table-column prop="customerName" label="客户" min-width="120" />
        <el-table-column prop="productModel" label="型号" width="130" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="72" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeDelivery(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-actions>
      <el-button
        v-if="selected?.status==='待出库'"
        type="primary"
        size="small"
        :loading="shipping"
        @click="ship"
      >确认出库发货</el-button>
      <el-button v-if="selected" type="danger" size="small" plain @click="removeDelivery(selected)">删除发货单</el-button>
    </template>
  </MesPageShell>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { DELIVERY_STATUS } from '@/mock/constants'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import { useMesDelete } from '@/composables/useMesDelete'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const { runDelete } = useMesDelete(mes, userStore)
const shipping = ref(false)
const { selected, onRowClick } = useMesFilter(computed(() => mes.deliveries), ['id'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'orderNo', label: '订单' }, { key: 'quantity', label: '数量' },
  { key: 'trackingNo', label: '运单' }, { key: 'status', label: '状态' }
]))
async function ship() {
  if (!selected.value || shipping.value) return
  shipping.value = true
  try {
    await mes.shipDelivery(selected.value.id, userStore.displayName, userStore.roleKey)
    ElMessage.success('发货成功，订单状态已更新')
  } catch {
    // 全局 request 拦截器已提示后端错误
  } finally {
    shipping.value = false
  }
}
function removeDelivery(row) {
  if (!row) return
  runDelete({
    action: 'deleteDelivery',
    payload: { dlvId: row.id },
    message: `确定删除发货单 ${row.id}？`,
    onSuccess: () => {
      if (selected.value?.id === row.id) selected.value = null
    }
  }).catch(() => {})
}
</script>
