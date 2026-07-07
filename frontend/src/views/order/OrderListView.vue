<template>
  <MesPageShell
    :status-items="statusItems"
    toolbar-title="客户订单"
    :status-options="ORDER_STATUS"
    :toolbar-actions="[{ label: '新建订单', key: 'create', type: 'primary' }]"
    :detail-rows="rows"
    :logs="mes.operationLogs.slice(0, 10)"
    v-model:model-keyword="keyword"
    v-model:model-status="statusFilter"
    @toolbar-action="onAction"
  >
    <template #table>
      <el-table :data="filtered" highlight-current-row style="width:100%" @current-change="onRowClick">
        <el-table-column prop="id" label="订单号" width="140" />
        <el-table-column prop="customerName" label="客户" min-width="140" />
        <el-table-column prop="productModel" label="产品型号" width="140" />
        <el-table-column prop="panelType" label="面板" width="70" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="deliveryDate" label="交付日期" width="110" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-actions>
      <el-button v-if="selected?.status==='待审核'" size="small" @click="submitAudit">提交审核</el-button>
    </template>
  </MesPageShell>
  <el-dialog v-model="dialogVisible" title="新建客户订单" width="480px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="客户">
        <el-select v-model="form.customerId" style="width:100%">
          <el-option v-for="c in mes.customers" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="产品型号">
        <el-select v-model="form.productModel" style="width:100%">
          <el-option v-for="m in PRODUCT_MODELS" :key="m" :label="m" :value="m" />
        </el-select>
      </el-form-item>
      <el-form-item label="面板类型"><el-input v-model="form.panelType" placeholder="LCD / OLED" /></el-form-item>
      <el-form-item label="数量"><el-input-number v-model="form.quantity" :min="1" style="width:100%" /></el-form-item>
      <el-form-item label="交付日期"><el-date-picker v-model="form.deliveryDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible=false">取消</el-button>
      <el-button type="primary" @click="create">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { ORDER_STATUS, PRODUCT_MODELS } from '@/mock/constants'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const dialogVisible = ref(false)
const form = reactive({ customerId: 1, productModel: PRODUCT_MODELS[0], panelType: 'LCD', quantity: 100, deliveryDate: '2026-05-01', remark: '' })

const { keyword, statusFilter, selected, filtered, onRowClick } = useMesFilter(
  computed(() => mes.orders),
  ['id', 'customerName', 'productModel']
)

const statusItems = computed(() => [
  { label: '订单总数', value: mes.orders.length },
  { label: '待审核', value: mes.pendingOrders.length, warn: true },
  { label: '生产中', value: mes.orders.filter(o => o.status === '生产中').length }
])

const rows = computed(() => detailRows(selected.value, [
  { key: 'id', label: '订单号' }, { key: 'customerName', label: '客户' },
  { key: 'productModel', label: '型号' }, { key: 'quantity', label: '数量' },
  { key: 'deliveryDate', label: '交付日' }, { key: 'status', label: '状态' },
  { key: 'remark', label: '备注' }
]))

function onAction(key) { if (key === 'create') dialogVisible.value = true }
function create() {
  mes.createOrder(form, userStore.displayName, userStore.roleKey)
  ElMessage.success('订单已创建')
  dialogVisible.value = false
}
function submitAudit() {
  if (selected.value) mes.submitOrder(selected.value.id, userStore.displayName, userStore.roleKey)
  ElMessage.success('已提交审核')
}
</script>
