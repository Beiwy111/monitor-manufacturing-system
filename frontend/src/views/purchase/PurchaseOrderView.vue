<template>
  <MesPageShell
    toolbar-title="采购订单"
    :status-options="PURCHASE_STATUS"
    :toolbar-actions="[{ label: '新建采购', key: 'add', type: 'primary' }]"
    :detail-rows="rows"
    :logs="mes.operationLogs.slice(0,8)"
    @toolbar-action="dialogVisible=true"
  >
    <template #table>
      <el-table :data="mes.purchaseOrders" border stripe highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="采购单" width="130" />
        <el-table-column prop="supplier" label="供应商" min-width="120" />
        <el-table-column prop="materialName" label="物料" width="100" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="arrivedQty" label="到货" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="72" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="removePurchase(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-actions>
      <el-button v-if="selected" type="danger" size="small" plain @click="removePurchase(selected)">删除采购单</el-button>
    </template>
    <template #detail-extra>
      <el-form v-if="selected" inline style="margin-top:12px">
        <el-form-item label="到货数量"><el-input-number v-model="arriveQty" :min="1" /></el-form-item>
        <el-button type="primary" @click="receive">登记到货</el-button>
      </el-form>
    </template>
  </MesPageShell>
  <el-dialog v-model="dialogVisible" title="新建采购订单" width="440px">
    <el-form label-width="90px">
      <el-form-item label="供应商"><el-input v-model="form.supplier" /></el-form-item>
      <el-form-item label="物料">
        <el-select v-model="form.materialCode" style="width:100%">
          <el-option v-for="m in materialOptions" :key="m.code" :label="`${m.name}（${m.code}）`" :value="m.code" />
        </el-select>
      </el-form-item>
      <el-form-item label="数量"><el-input-number v-model="form.quantity" :min="1" style="width:100%" /></el-form-item>
      <el-form-item label="单价"><el-input-number v-model="form.unitPrice" :min="1" style="width:100%" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="create">创建</el-button></template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { PURCHASE_STATUS } from '@/mock/constants'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import { useMesDelete } from '@/composables/useMesDelete'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const { runDelete } = useMesDelete(mes, userStore)
const dialogVisible = ref(false)
const arriveQty = ref(50)
const materialOptions = computed(() =>
  (mes.inventory || []).map((i) => ({ code: i.materialCode, name: i.materialName }))
)
const defaultExpected = new Date(Date.now() + 14 * 86400000).toISOString().slice(0, 10)
const form = reactive({ supplier: '', materialCode: '', materialName: '', quantity: 100, unitPrice: 0, expectedDate: defaultExpected })
const { selected, onRowClick } = useMesFilter(computed(() => mes.purchaseOrders), ['id'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'supplier', label: '供应商' }, { key: 'materialName', label: '物料' }, { key: 'totalAmount', label: '金额' }, { key: 'status', label: '状态' }
]))
function create() {
  const m = materialOptions.value.find((x) => x.code === form.materialCode)
  if (!m) {
    ElMessage.warning('请选择物料')
    return
  }
  form.materialName = m.name
  mes.createPurchaseOrder(form, userStore.displayName, userStore.roleKey)
  ElMessage.success('采购订单已创建')
  dialogVisible.value = false
}
function receive() {
  if (mes.receivePurchase(selected.value.id, arriveQty.value, userStore.displayName, userStore.roleKey)) {
    ElMessage.success('到货登记成功，库存已更新')
  }
}
function removePurchase(row) {
  if (!row) return
  runDelete({
    action: 'deletePurchaseOrder',
    payload: { purchaseOrderId: row.id },
    message: `确定删除采购单 ${row.id}？`,
    onSuccess: () => {
      if (selected.value?.id === row.id) selected.value = null
    }
  }).catch(() => {})
}
</script>
