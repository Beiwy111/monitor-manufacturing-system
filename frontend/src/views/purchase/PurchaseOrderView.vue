<template>
  <div class="po-page">
    <div class="po-header">
      <div class="po-header__left">
        <span class="po-title">采购订单</span>
        <el-tag type="info" size="small">由采购需求工作台自动生成，不可手动创建</el-tag>
        <el-tag v-if="pendingCount > 0" type="warning" size="small">{{ pendingCount }} 张待到货</el-tag>
        <el-tag v-if="overdueCount > 0" type="danger" size="small">{{ overdueCount }} 张已逾期</el-tag>
      </div>
      <el-button :loading="loading" @click="loadOrders">刷新</el-button>
    </div>

    <div class="po-filter">
      <el-input v-model="filterNo" placeholder="采购单号搜索" clearable style="width:200px;margin-right:8px" />
      <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:150px">
        <el-option label="待到货" value="PENDING_ARRIVAL" />
        <el-option label="部分到货" value="PART_RECEIVED" />
        <el-option label="已到货" value="RECEIVED" />
        <el-option label="已取消" value="CANCELLED" />
        <el-option label="逾期未到" value="OVERDUE" />
      </el-select>
    </div>

    <div v-loading="loading" class="po-table-wrap">
      <div class="po-section-head">
        <span class="po-section-title">未收货采购单</span>
        <el-tag type="warning" size="small">{{ activeOrders.length }} 张待处理</el-tag>
      </div>
      <el-table :data="activeOrders" border stripe highlight-current-row @current-change="onRowClick">
        <el-table-column prop="purchaseOrderNo" label="采购单号" width="170" />
        <el-table-column prop="supplierName" label="供应商" min-width="130" />
        <el-table-column prop="purchaseDate" label="采购日期" width="110" />
        <el-table-column prop="expectedArrivalDate" label="期望到货" width="110">
          <template #default="{ row }">
            <span :class="isOverdue(row) ? 'overdue-text' : ''">{{ row.expectedArrivalDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额(元)" width="110" align="right">
          <template #default="{ row }">
            {{ row.totalAmount != null ? Number(row.totalAmount).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewItems(row)">明细</el-button>
            <el-button
              v-if="canConfirm(row)"
              link
              type="success"
              size="small"
              :loading="confirmingId === row.purchaseOrderId"
              @click.stop="confirmOrder(row)"
            >
              确认到货
            </el-button>
            <el-button
              v-if="canRevoke(row)"
              link
              type="danger"
              size="small"
              :loading="revokingId === row.purchaseOrderId"
              @click.stop="revokeOrder(row)"
            >
              撤销
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && activeOrders.length === 0" class="po-empty po-empty--small">
        暂无未收货采购单
      </div>

      <div class="po-section-head po-section-head--history">
        <span class="po-section-title">历史明细</span>
        <el-tag type="info" size="small">{{ historyOrders.length }} 张</el-tag>
      </div>
      <el-table :data="historyOrders" border stripe highlight-current-row @current-change="onRowClick">
        <el-table-column prop="purchaseOrderNo" label="采购单号" width="170" />
        <el-table-column prop="supplierName" label="供应商" min-width="130" />
        <el-table-column prop="purchaseDate" label="采购日期" width="110" />
        <el-table-column prop="expectedArrivalDate" label="期望到货" width="110" />
        <el-table-column prop="totalAmount" label="金额(元)" width="110" align="right">
          <template #default="{ row }">
            {{ row.totalAmount != null ? Number(row.totalAmount).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="76">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewItems(row)">明细</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && historyOrders.length === 0" class="po-empty po-empty--small">
        暂无历史采购单
      </div>
    </div>

    <!-- 采购明细弹窗 -->
    <el-dialog v-model="itemVisible" :title="`采购单/到货明细 — ${currentOrder?.purchaseOrderNo}`" width="92vw" class="po-detail-dialog">
      <div v-if="currentOrder" style="margin-bottom:12px">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="供应商">{{ currentOrder.supplierName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="采购日期">{{ currentOrder.purchaseDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="期望到货">
            <span :class="isOverdue(currentOrder) ? 'overdue-text' : ''">{{ currentOrder.expectedArrivalDate || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="总金额">
            <span style="color:#e6a23c;font-weight:600">
              ¥{{ currentOrder.totalAmount != null ? Number(currentOrder.totalAmount).toLocaleString() : '-' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="到货状态" :span="2">
            <el-tag :type="statusType(currentOrder.status)" size="small">{{ statusLabel(currentOrder.status) }}</el-tag>
            <el-tag v-if="isOverdue(currentOrder)" type="danger" size="small" style="margin-left:6px">逾期未到</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <el-table :data="items" border size="small" v-loading="itemLoading">
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
          <template #default="{ row }">
            <span style="color:#606266">{{ Number(row.lineAmount).toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="itemStatus" label="状态" min-width="76">
          <template #default="{ row }">
            <el-tag :type="itemStatusType(row.itemStatus)" size="small">{{ itemStatusLabel(row.itemStatus) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button
          v-if="canConfirm(currentOrder)"
          type="success"
          :loading="confirmingId === currentOrder?.purchaseOrderId"
          @click="confirmOrder(currentOrder)"
        >
          确认全部到货
        </el-button>
        <el-button @click="itemVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const orders = ref([])
const loading = ref(false)
const confirmingId = ref(null)
const revokingId = ref(null)
const filterNo = ref('')
const filterStatus = ref('')
const currentOrder = ref(null)
const itemVisible = ref(false)
const items = ref([])
const itemLoading = ref(false)

const filteredOrders = computed(() => {
  return orders.value.filter(o => {
    if (filterNo.value && !(o.purchaseOrderNo || '').includes(filterNo.value)) return false
    if (filterStatus.value === 'PENDING_ARRIVAL' && isHistoryOrder(o)) return false
    if (filterStatus.value === 'OVERDUE' && !isOverdue(o)) return false
    if (filterStatus.value && !['PENDING_ARRIVAL', 'OVERDUE'].includes(filterStatus.value) && o.status !== filterStatus.value) return false
    return true
  })
})

const activeOrders = computed(() => filteredOrders.value.filter(o => !isHistoryOrder(o)))
const historyOrders = computed(() => filteredOrders.value.filter(o => isHistoryOrder(o)))
const pendingCount = computed(() => orders.value.filter(o => !isHistoryOrder(o)).length)
const overdueCount = computed(() => orders.value.filter(o => isOverdue(o)).length)

function isHistoryOrder(row) {
  return row?.status === 'RECEIVED' || row?.status === 'CANCELLED'
}

function isOverdue(row) {
  if (!row?.expectedArrivalDate || isHistoryOrder(row)) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const expected = new Date(row.expectedArrivalDate)
  expected.setHours(0, 0, 0, 0)
  return expected < today
}

function statusLabel(status) {
  const map = { DRAFT: '待到货', SUBMITTED: '待到货', APPROVED: '待到货', PART_RECEIVED: '部分到货', RECEIVED: '已到货', CANCELLED: '已取消' }
  return map[status] || status || '-'
}

function statusType(status) {
  const map = { DRAFT: 'warning', SUBMITTED: 'warning', APPROVED: 'warning', PART_RECEIVED: 'warning', RECEIVED: 'success', CANCELLED: 'info' }
  return map[status] || ''
}

function onRowClick(row) { currentOrder.value = row }

function canRevoke(row) {
  return row && row.status !== 'RECEIVED' && row.status !== 'CANCELLED'
}

function canConfirm(row) {
  return row && row.status !== 'RECEIVED' && row.status !== 'CANCELLED'
}

async function confirmOrder(row) {
  if (!row) return
  try {
    await ElMessageBox.confirm(
      `确认采购单 ${row.purchaseOrderNo} 全部到货？到货数量将自动入库。`,
      '到货确认',
      { type: 'warning', confirmButtonText: '确认到货', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  confirmingId.value = row.purchaseOrderId
  try {
    await request.post('/purchase/order/confirmArrival', null, { params: { purchaseOrderId: row.purchaseOrderId } })
    ElMessage.success(`采购单 ${row.purchaseOrderNo} 已确认到货，库存已更新`)
    itemVisible.value = false
    await loadOrders()
  } catch (e) {
    ElMessage.error(e?.message || '到货确认失败')
  } finally {
    confirmingId.value = null
  }
}

async function revokeOrder(row) {
  try {
    await ElMessageBox.confirm(
      `确认撤销采购单 ${row.purchaseOrderNo}？撤销后该单会进入历史明细，关联需求会回到采购需求工作台重新计算。已到货采购单已入库，不能撤销。`,
      '撤销采购单',
      { type: 'warning', confirmButtonText: '确认撤销', cancelButtonText: '取消' }
    )
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    return
  }
  revokingId.value = row.purchaseOrderId
  try {
    await request.post('/purchase/order/revoke', null, { params: { purchaseOrderId: row.purchaseOrderId } })
    ElMessage.success('采购单已撤销')
    if (currentOrder.value?.purchaseOrderId === row.purchaseOrderId) {
      currentOrder.value = null
    }
    itemVisible.value = false
    await loadOrders()
  } catch (e) {
    ElMessage.error(e?.message || '撤销采购单失败')
  } finally {
    revokingId.value = null
  }
}

async function viewItems(row) {
  currentOrder.value = row
  itemVisible.value = true
  itemLoading.value = true
  try {
    const res = await request.get('/purchase/purchaseOrderItem/listByOrder', { params: { purchaseOrderId: row.purchaseOrderId } })
    items.value = res?.data || res || []
  } catch {
    ElMessage.error('加载明细失败')
  } finally {
    itemLoading.value = false
  }
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

onMounted(() => { loadOrders() })
</script>

<style scoped>
.po-page { padding: 16px 20px; background: #f5f7fa; min-height: 100%; }
.po-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.po-header__left { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
.po-title { font-size: 18px; font-weight: 700; color: #001b3f; }
.po-filter { margin-bottom: 12px; display: flex; align-items: center; }
.po-table-wrap { background: #fff; border-radius: 6px; padding: 12px; min-height: 200px; }
.po-empty { text-align: center; color: #909399; padding: 60px 0; font-size: 14px; }
.po-empty--small { padding: 24px 0; }
.po-section-head { display: flex; align-items: center; gap: 10px; margin: 2px 0 10px; }
.po-section-head--history { margin-top: 22px; padding-top: 18px; border-top: 1px solid #ebeef5; }
.po-section-title { font-size: 15px; font-weight: 700; color: #001b3f; }
.overdue-text { color: #f56c6c; font-weight: 600; }
.po-detail-dialog :deep(.el-table__body-wrapper) { overflow-x: hidden; }
</style>

