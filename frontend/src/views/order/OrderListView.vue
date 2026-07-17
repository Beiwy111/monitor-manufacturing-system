<template>
  <MesPageShell
    :status-items="statusItems"
    :status-options="ORDER_STATUS"
    :toolbar-actions="[]"
    :show-detail-panel="false"
    v-model:model-keyword="keyword"
    v-model:model-status="statusFilter"
  >
    <template #table>
      <div v-if="mes.pendingSubmitToPlanner.length" class="pending-block">
        <div class="pending-block__title">已审核待提交计划员（{{ mes.pendingSubmitToPlanner.length }}）</div>
        <el-table :data="mes.pendingSubmitToPlanner" size="small" highlight-current-row class="mes-table-light" style="margin-bottom: 12px">
          <el-table-column prop="id" label="订单号" width="148" show-overflow-tooltip />
          <el-table-column prop="customerName" label="客户" min-width="120" />
          <el-table-column prop="productModel" label="型号" width="140" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" @click="submitToPlanner(row.id)">提交计划员</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-table
        :data="filtered"
        highlight-current-row
        class="mes-table-light"
        style="width:100%"
        @row-click="openDetail"
      >
        <el-table-column prop="id" label="订单号" width="148" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" min-width="140" />
        <el-table-column prop="productModel" label="产品型号" width="140" />
        <el-table-column prop="panelType" label="面板" width="70" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="deliveryDate" label="交付日期" width="110" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
            <el-button link type="danger" @click.stop="removeOrder(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </MesPageShell>

  <el-dialog
    v-model="detailVisible"
    :title="detailOrder ? `订单详情 · ${detailOrder.id}` : '订单详情'"
    width="720px"
    class="order-detail-dialog"
    destroy-on-close
    align-center
  >
    <template v-if="detailOrder">
      <div class="order-detail-dialog__head">
        <StatusBadge :status="detailOrder.status" />
        <span class="order-detail-dialog__sub">{{ detailOrder.customerName }} · {{ detailOrder.productModel }}</span>
      </div>
      <el-descriptions :column="2" border size="large" class="order-detail-dialog__desc">
        <el-descriptions-item label="订单号">{{ detailOrder.id }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ detailOrder.customerName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="产品型号">{{ detailOrder.productModel || '—' }}</el-descriptions-item>
        <el-descriptions-item label="物料编码">{{ detailOrder.materialCode || '—' }}</el-descriptions-item>
        <el-descriptions-item label="面板类型">{{ detailOrder.panelType || '—' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ detailOrder.quantity ?? '—' }} 台</el-descriptions-item>
        <el-descriptions-item label="单价">{{ formatMoney(detailOrder.unitPrice) }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">{{ formatMoney(detailOrder.amount) }}</el-descriptions-item>
        <el-descriptions-item label="交付日期">{{ detailOrder.deliveryDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="销售员">{{ detailOrder.salesPerson || '—' }}</el-descriptions-item>
        <el-descriptions-item label="规格" :span="2">{{ detailOrder.specification || '—' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailOrder.createdAt || '—' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detailOrder.updatedAt || '—' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailOrder.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
    </template>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
      <el-button v-if="detailOrder?.status === '待审核'" type="primary" @click="submitAudit">提交审核</el-button>
      <el-button v-if="detailOrder?.status === '已审核'" type="primary" @click="submitToPlanner(detailOrder.id)">提交计划员</el-button>
      <el-button v-if="detailOrder" type="danger" plain @click="removeOrder(detailOrder)">删除订单</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="dialogVisible" title="新建客户订单" width="480px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="客户">
        <el-select v-model="form.customerId" style="width:100%">
          <el-option v-for="c in mes.customers" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="产品型号">
        <el-select v-model="form.productModel" style="width:100%">
          <el-option v-for="m in productOptions" :key="m.code" :label="`${m.name}（${m.code}）`" :value="m.name" />
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
import { ORDER_STATUS } from '@/mock/constants'
import { useMesFilter } from '@/composables/useMesPage'
import { useMesDelete } from '@/composables/useMesDelete'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const { runDelete } = useMesDelete(mes, userStore)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const detailOrder = ref(null)

const productOptions = computed(() => mes.productModels || [])
const defaultDelivery = new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10)

const form = reactive({
  customerId: null,
  productModel: '',
  panelType: 'LCD',
  quantity: 100,
  deliveryDate: defaultDelivery,
  remark: ''
})

function resetForm() {
  const firstCustomer = mes.customers[0]
  const firstProduct = productOptions.value[0]
  form.customerId = firstCustomer?.id ?? null
  form.productModel = firstProduct?.name || firstProduct?.code || ''
  form.panelType = firstProduct?.panelType || 'LCD'
  form.quantity = 100
  form.deliveryDate = defaultDelivery
  form.remark = ''
}

const { keyword, statusFilter, filtered } = useMesFilter(
  computed(() => mes.orders),
  ['id', 'customerName', 'productModel']
)

const statusItems = computed(() => [
  { label: '订单总数', value: mes.orders.length },
  { label: '待审核', value: mes.pendingOrders.length, warn: true },
  { label: '待计划', value: mes.pendingPlanOrders.length, warn: mes.pendingPlanOrders.length > 0 },
  { label: '生产中', value: mes.orders.filter(o => o.status === '生产中').length }
])

function formatMoney(val) {
  if (val == null || val === '') return '—'
  return `${Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} 元`
}

function openDetail(row) {
  if (!row) return
  detailOrder.value = row
  detailVisible.value = true
}

function onAction(key) {
  if (key === 'create') {
    resetForm()
    dialogVisible.value = true
  }
}

async function create() {
  try {
    const customer = mes.customers.find((c) => c.id === form.customerId)
    await mes.createOrder(
      { ...form, customerName: customer?.name || '' },
      userStore.username,
      userStore.roleKey
    )
    ElMessage.success('订单已创建，请提交审核')
    dialogVisible.value = false
  } catch (e) {
    ElMessage.error(e?.message || '创建订单失败')
  }
}

function submitAudit() {
  if (!detailOrder.value) return
  mes.submitOrder(detailOrder.value.id, userStore.username, userStore.roleKey)
  ElMessage.success('已提交审核')
  detailVisible.value = false
}

async function submitToPlanner(orderId) {
  const ok = await mes.submitOrderToPlanner(orderId, userStore.username, userStore.roleKey)
  if (ok !== false) {
    ElMessage.success('已提交计划员，请计划员编制生产计划')
    detailVisible.value = false
  } else {
    ElMessage.warning('订单状态不允许提交计划员')
  }
}

function removeOrder(row) {
  if (!row) return
  runDelete({
    action: 'deleteOrder',
    payload: { orderId: row.id },
    message: `确定删除订单 ${row.id}？关联计划、工单、发货等记录将一并删除。`,
    onSuccess: () => {
      if (detailOrder.value?.id === row.id) {
        detailOrder.value = null
        detailVisible.value = false
      }
    }
  }).catch(() => {})
}
</script>

<style scoped>
.pending-block {
  padding: 12px 0 4px;
  border-bottom: 1px solid #e8ecf0;
  margin-bottom: 8px;
}

.pending-block__title {
  font-size: 14px;
  font-weight: 600;
  color: #001b3f;
  margin-bottom: 8px;
}

.order-detail-dialog__head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.order-detail-dialog__sub {
  font-size: 15px;
  color: #4e5969;
}

.order-detail-dialog__desc :deep(.el-descriptions__label) {
  width: 110px;
  font-size: 14px;
  font-weight: 600;
  color: #4e5969;
  background: #f7f8fa;
}

.order-detail-dialog__desc :deep(.el-descriptions__content) {
  font-size: 15px;
  color: #1f2329;
  line-height: 1.5;
}
</style>
