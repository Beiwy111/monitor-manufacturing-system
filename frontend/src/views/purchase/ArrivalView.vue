<template>
  <div class="arrival-page">
    <div class="arrival-header">
      <div class="arrival-header__left">
        <span class="arrival-title">到货进度</span>
        <el-tag v-if="pendingCount > 0" type="warning" size="small" style="margin-left:10px">
          {{ pendingCount }} 张待到货
        </el-tag>
      </div>
      <div class="arrival-header__right">
        <el-input v-model="filterNo" placeholder="采购单号" clearable style="width:180px;margin-right:8px" />
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:140px;margin-right:8px" @change="filterNo = filterNo">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已审核" value="APPROVED" />
          <el-option label="部分到货" value="PART_RECEIVED" />
          <el-option label="已到货" value="RECEIVED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-button :loading="loading" @click="loadOrders">刷新</el-button>
      </div>
    </div>

    <div v-loading="loading" class="arrival-table-wrap">
      <el-table :data="filtered" border stripe highlight-current-row>
        <el-table-column prop="purchaseOrderNo" label="采购单号" width="170" />
        <el-table-column prop="supplierName" label="供应商" min-width="130" show-overflow-tooltip />
        <el-table-column prop="purchaseDate" label="采购日期" width="110" />
        <el-table-column prop="expectedArrivalDate" label="期望到货" width="110">
          <template #default="{ row }">
            <span :class="isOverdue(row) ? 'overdue-text' : ''">
              {{ row.expectedArrivalDate || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额(元)" width="120" align="right">
          <template #default="{ row }">
            <span style="font-weight:600">
              {{ row.totalAmount != null ? '¥' + Number(row.totalAmount).toLocaleString() : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="orderStatusType(row.status)" size="small">{{ orderStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="170">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">明细</el-button>
            <el-button
              v-if="row.status !== 'RECEIVED' && row.status !== 'CANCELLED'"
              link type="success" size="small"
              :loading="confirmingId === row.purchaseOrderId"
              @click="doConfirm(row)"
            >
              确认到货
            </el-button>
            <el-button
              v-if="row.status !== 'RECEIVED' && row.status !== 'CANCELLED'"
              link type="danger" size="small"
              :loading="revokingId === row.purchaseOrderId"
              @click="doRevoke(row)"
            >
              撤销
            </el-button>
            <el-tag v-if="row.status === 'RECEIVED'" type="success" size="small" style="margin-left:4px">已完成</el-tag>
            <el-tag v-if="row.status === 'CANCELLED'" type="info" size="small" style="margin-left:4px">已撤销</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && filtered.length === 0" class="arrival-empty">
        暂无采购订单数据
      </div>
    </div>

    <!-- 明细弹窗 -->
    <el-dialog v-model="detailVisible" :title="`到货明细 — ${currentOrder?.purchaseOrderNo}`" width="92vw" class="arrival-detail-dialog">
      <div v-if="currentOrder" style="margin-bottom:12px">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="供应商">{{ currentOrder.supplierName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="采购日期">{{ currentOrder.purchaseDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="期望到货">
            <span :class="isOverdue(currentOrder) ? 'overdue-text' : ''">
              {{ currentOrder.expectedArrivalDate || '-' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="总金额">
            <span style="color:#e6a23c;font-weight:600">
              ¥{{ currentOrder.totalAmount != null ? Number(currentOrder.totalAmount).toLocaleString() : '-' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="状态" :span="2">
            <el-tag :type="orderStatusType(currentOrder.status)" size="small">
              {{ orderStatusLabel(currentOrder.status) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <el-table :data="detailItems" border size="small" v-loading="detailLoading">
        <el-table-column prop="materialCode" label="物料编码" min-width="96" />
        <el-table-column prop="materialName" label="物料名称" min-width="120" />
        <el-table-column prop="quantity" label="订购数量" min-width="82" align="right" />
        <el-table-column prop="receivedQuantity" label="已收货" min-width="74" align="right">
          <template #default="{ row }">
            <span :style="{ color: Number(row.receivedQuantity) >= Number(row.quantity) ? '#67c23a' : '#e6a23c' }">
              {{ row.receivedQuantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" min-width="52" align="center" />
        <el-table-column prop="unitPrice" label="单价" min-width="76" align="right" />
        <el-table-column prop="lineAmount" label="行金额" min-width="92" align="right">
          <template #default="{ row }">¥{{ Number(row.lineAmount).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="itemStatus" label="状态" min-width="76" align="center">
          <template #default="{ row }">
            <el-tag :type="itemStatusType(row.itemStatus)" size="small">{{ itemStatusLabel(row.itemStatus) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button
          v-if="currentOrder?.status !== 'RECEIVED' && currentOrder?.status !== 'CANCELLED'"
          type="success"
          :loading="confirmingId === currentOrder?.purchaseOrderId"
          @click="doConfirm(currentOrder)"
        >
          确认全部到货
        </el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { confirmArrival, revokePurchaseOrder } from '@/api/business'

const orders = ref([])
const loading = ref(false)
const confirmingId = ref(null)
const revokingId = ref(null)
const filterNo = ref('')
const filterStatus = ref('')

const currentOrder = ref(null)
const detailVisible = ref(false)
const detailItems = ref([])
const detailLoading = ref(false)

const filtered = computed(() => {
  return orders.value.filter(o => {
    if (filterNo.value && !(o.purchaseOrderNo || '').includes(filterNo.value)) return false
    if (filterStatus.value && o.status !== filterStatus.value) return false
    return true
  })
})

const pendingCount = computed(() =>
  orders.value.filter(o => o.status !== 'RECEIVED' && o.status !== 'CANCELLED').length
)

function isOverdue(row) {
  if (!row.expectedArrivalDate || row.status === 'RECEIVED' || row.status === 'CANCELLED') return false
  return new Date(row.expectedArrivalDate) < new Date()
}

function orderStatusLabel(status) {
  const map = {
    DRAFT: '待提交', SUBMITTED: '已提交', APPROVED: '已审核',
    PART_RECEIVED: '部分到货', RECEIVED: '已到货', CANCELLED: '已取消'
  }
  return map[status] || status || '-'
}

function orderStatusType(status) {
  const map = {
    DRAFT: 'info', SUBMITTED: 'warning', APPROVED: 'primary',
    PART_RECEIVED: 'warning', RECEIVED: 'success', CANCELLED: 'info'
  }
  return map[status] || ''
}

function itemStatusLabel(s) {
  const map = { PENDING: '待收货', PART_RECEIVED: '部分收货', RECEIVED: '已收货', CANCELLED: '已取消' }
  return map[s] || s || '-'
}

function itemStatusType(s) {
  const map = { PENDING: 'warning', PART_RECEIVED: 'warning', RECEIVED: 'success', CANCELLED: 'info' }
  return map[s] || ''
}

async function loadOrders() {
  loading.value = true
  try {
    orders.value = await request.get('/purchase/purchaseOrder/list') || []
  } catch {
    ElMessage.error('加载采购订单失败')
  } finally {
    loading.value = false
  }
}

async function viewDetail(row) {
  currentOrder.value = row
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await request.get('/purchase/purchaseOrderItem/listByOrder', {
      params: { purchaseOrderId: row.purchaseOrderId }
    })
    detailItems.value = res?.data || res || []
  } catch {
    ElMessage.error('加载明细失败')
  } finally {
    detailLoading.value = false
  }
}

async function doConfirm(row) {
  try {
    await ElMessageBox.confirm(
      `确认采购单 ${row.purchaseOrderNo} 全部到货？到货数量将自动入库。`,
      '到货确认',
      { confirmButtonText: '确认到货', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  confirmingId.value = row.purchaseOrderId
  try {
    await confirmArrival(row.purchaseOrderId)
    ElMessage.success(`采购单 ${row.purchaseOrderNo} 已确认到货，库存已更新`)
    detailVisible.value = false
    await loadOrders()
  } catch (e) {
    ElMessage.error(e?.message || '到货确认失败')
  } finally {
    confirmingId.value = null
  }
}

async function doRevoke(row) {
  try {
    await ElMessageBox.confirm(
      `确定撤销采购单 ${row.purchaseOrderNo}？撤销后关联的采购需求将恢复为"待采购"状态。`,
      '撤销采购单',
      { confirmButtonText: '确认撤销', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  revokingId.value = row.purchaseOrderId
  try {
    await revokePurchaseOrder(row.purchaseOrderId)
    ElMessage.success(`采购单 ${row.purchaseOrderNo} 已撤销，需求已恢复`)
    detailVisible.value = false
    await loadOrders()
  } catch (e) {
    ElMessage.error(e?.message || '撤销失败')
  } finally {
    revokingId.value = null
  }
}

onMounted(() => { loadOrders() })
</script>

<style scoped>
.arrival-page {
  padding: 16px 20px;
  background: #f5f7fa;
  min-height: 100%;
}
.arrival-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 10px;
}
.arrival-header__left {
  display: flex;
  align-items: center;
}
.arrival-header__right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.arrival-title {
  font-size: 18px;
  font-weight: 700;
  color: #001b3f;
}
.arrival-table-wrap {
  background: #fff;
  border-radius: 6px;
  padding: 12px;
  min-height: 200px;
}
.arrival-empty {
  text-align: center;
  color: #909399;
  padding: 60px 0;
  font-size: 14px;
}
.overdue-text {
  color: #f56c6c;
  font-weight: 600;
}
.arrival-detail-dialog :deep(.el-table__body-wrapper) {
  overflow-x: hidden;
}
</style>
