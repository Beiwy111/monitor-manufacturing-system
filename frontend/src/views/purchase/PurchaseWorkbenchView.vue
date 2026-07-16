<template>
  <div class="pdb">
    <div class="pdb-panel">
      <!-- 顶部工具栏 -->
      <div class="pdb-toolbar">
        <div class="pdb-toolbar__left">
          <div class="pdb-toolbar__title">采购工作台</div>
          <div class="pdb-toolbar__sub">
            <span v-if="stats.pendingRequirements > 0" class="hero-badge hero-badge--warn">待采购 {{ stats.pendingRequirements }}</span>
            <span v-if="stats.overdueOrders > 0" class="hero-badge hero-badge--danger">逾期未到 {{ stats.overdueOrders }}</span>
            <span v-if="aiPending > 0" class="hero-badge hero-badge--info">AI单据待确认 {{ aiPending }}</span>
          </div>
        </div>
      </div>

      <!-- 订单物料需求总览 -->
      <section class="pdb-section pdb-section--last">
        <div class="section-head">
          <span class="section-title">订单物料需求总览</span>
          <span class="section-sub">点击展开查看「需生产台数 → BOM 物料需求 → 实际库存」</span>
          <span class="section-link" @click="recalculate">刷新采购清单</span>
        </div>
        <el-table :data="orderDemands" border stripe size="small" v-loading="loading" style="width:100%"
          empty-text="暂无待处理订单，审核通过后将自动出现在此">
          <el-table-column type="expand" width="42">
            <template #default="{ row }">
              <div v-if="row.needToProduce > 0 && row.materials?.length" class="bom-expand">
                <div class="bom-expand__head">
                  生产 <strong>{{ row.needToProduce }}</strong> 台「{{ row.productName }}」需以下
                  <strong>{{ row.materials.length }}</strong> 种物料
                </div>
                <el-table :data="row.materials" border size="small" class="bom-expand__table">
                  <el-table-column label="物料" min-width="200">
                    <template #default="{ row: m }">
                      <div class="material-cell">
                        <img v-if="matImg(m)" class="material-thumb" :src="matImg(m)" :alt="m.materialName" />
                        <div v-else class="material-thumb material-thumb--fallback">{{ matInitial(m) }}</div>
                        <div class="material-cell__text">
                          <div class="material-cell__name">{{ m.materialName }}</div>
                          <div class="material-cell__code">{{ m.materialCode }}</div>
                        </div>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="requiredQuantity" label="本单需求" width="88" align="right">
                    <template #default="{ row: m }"><strong>{{ fmtQty(m.requiredQuantity) }}</strong> {{ m.unit || '' }}</template>
                  </el-table-column>
                  <el-table-column prop="stockQuantity" label="实际库存" width="88" align="right">
                    <template #default="{ row: m }">
                      <span :class="Number(m.stockQuantity) >= Number(m.requiredQuantity) ? 'stock-ok' : 'stock-low'">{{ fmtQty(m.stockQuantity) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="onPurchaseQuantity" label="在途" width="72" align="right">
                    <template #default="{ row: m }">{{ fmtQty(m.onPurchaseQuantity) }}</template>
                  </el-table-column>
                  <el-table-column prop="netShortage" label="净缺口" width="80" align="right">
                    <template #default="{ row: m }">
                      <el-tag v-if="Number(m.netShortage) > 0" type="danger" size="small">{{ fmtQty(m.netShortage) }}</el-tag>
                      <span v-else style="color:#52c41a">充足</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="supplierName" label="默认供应商" min-width="130" show-overflow-tooltip />
                </el-table>
              </div>
              <div v-else class="bom-expand bom-expand--empty">无需生产或暂无 BOM 物料明细</div>
            </template>
          </el-table-column>
          <el-table-column prop="orderNo" label="订单号" width="130" fixed />
          <el-table-column prop="customerName" label="客户" min-width="120" show-overflow-tooltip />
          <el-table-column prop="productName" label="产品" min-width="140" show-overflow-tooltip />
          <el-table-column prop="orderQuantity" label="订单数量" width="88" align="right" />
          <el-table-column prop="finishedStock" label="成品库存" width="88" align="right">
            <template #default="{ row }">
              <span :class="row.finishedStock >= row.orderQuantity ? 'stock-ok' : ''">{{ row.finishedStock }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="shipFromStock" label="库存直发" width="88" align="right" />
          <el-table-column prop="needToProduce" label="需生产" width="76" align="right">
            <template #default="{ row }">
              <el-tag v-if="row.needToProduce > 0" type="warning" size="small">{{ row.needToProduce }}</el-tag>
              <span v-else style="color:#52c41a">0</span>
            </template>
          </el-table-column>
          <el-table-column prop="shortageMaterialCount" label="缺料物料" width="88" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.shortageMaterialCount > 0" type="danger" size="small">{{ row.shortageMaterialCount }} 种</el-tag>
              <span v-else style="color:#bfbfbf">—</span>
            </template>
          </el-table-column>
          <el-table-column label="建议采购量" width="96" align="right">
            <template #default="{ row }">
              <strong v-if="Number(row.suggestedPurchaseQty) > 0" style="color:#cf1322">{{ fmtQty(row.suggestedPurchaseQty) }}</strong>
              <span v-else>0</span>
            </template>
          </el-table-column>
          <el-table-column prop="auditStatusCn" label="状态" width="80" align="center" />
          <el-table-column prop="requiredDeliveryDate" label="交期" width="108" />
          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template #default>
              <el-button link type="primary" size="small" @click="$router.push('/purchase/demand')">采购</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>

    <!-- 一键生成采购单弹窗 -->
    <el-dialog v-model="generateVisible" title="一键生成采购单" width="720px" @open="onGenerateOpen">
      <el-alert type="info" :closable="false" style="margin-bottom:12px">
        已选 <strong>{{ genSelection.length }}</strong> 种缺料物料，将合并生成 <strong>1</strong> 张采购单，请指定供应商
      </el-alert>
      <el-form :model="genForm" label-width="100px" size="default">
        <el-form-item label="供应商" required>
          <el-select v-model="genForm.supplierId" placeholder="请选择供应商" filterable style="width:100%">
            <el-option
              v-for="s in suppliers"
              :key="s.supplierId"
              :label="s.supplierName"
              :value="s.supplierId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="期望到货">
          <el-date-picker v-model="genForm.expectedArrivalDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="genForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <el-table :data="genSelection" border size="small" max-height="280" @selection-change="onGenSelectionChange">
        <el-table-column type="selection" width="42" />
        <el-table-column prop="materialCode" label="编码" width="100" />
        <el-table-column prop="materialName" label="物料名称" min-width="130" />
        <el-table-column prop="shortageQuantity" label="缺料量" width="80" align="right" />
        <el-table-column label="单价(元)" width="90" align="right">
          <template #default="{ row }">{{ materialUnitPrice(row) }}</template>
        </el-table-column>
        <el-table-column label="预估金额" width="100" align="right">
          <template #default="{ row }">
            {{ estimateLineAmount(row) }}
          </template>
        </el-table-column>
      </el-table>
      <div v-if="genSelection.length" class="gen-total">
        预估采购总额：<strong>¥{{ genTotalAmount.toLocaleString() }}</strong>
      </div>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" :disabled="!genForm.supplierId || genSelection.length === 0" @click="doGenerate">
          确认生成采购单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
<!-- SCRIPT_BLOCK -->

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import {
  fetchWorkbenchList,
  calculatePurchaseRequirements,
  fetchOrderDemandOverview,
  fetchWorkbenchByOrder,
  generatePurchaseOrder,
  fetchActiveSupplierList
} from '@/api/business'
import { materialImageUrl } from '@/utils/materialImages'

const router = useRouter()
const orders = ref([])
const requirements = ref([])
const orderDemands = ref([])
const loading = ref(false)
const generateVisible = ref(false)
const generating = ref(false)
const suppliers = ref([])
const genSelection = ref([])
const genForm = ref({
  supplierId: null,
  expectedArrivalDate: '',
  remark: ''
})

const MATERIAL_PRICE = {
  'MAT-001': 280, 'MAT-002': 85, 'MAT-003': 12.5,
  'MAT-004': 45, 'MAT-005': 65, 'MAT-006': 35
}

const today = new Date().toISOString().slice(0, 10)

const purchasableReqs = computed(() =>
  requirements.value.filter(r => r.status === 'PENDING')
)

function matImg(row) {
  return materialImageUrl(row)
}

function matInitial(row) {
  const name = row.materialName || row.materialCode || '?'
  return name.charAt(0)
}

const genTotalAmount = computed(() =>
  genSelection.value.reduce((sum, row) => sum + Number(estimateLineAmount(row, false)), 0)
)

function materialUnitPrice(row) {
  return MATERIAL_PRICE[row.materialCode] ?? 0
}

function estimateLineAmount(row, formatted = true) {
  const qty = Number(row.suggestedPurchaseQuantity ?? row.shortageQuantity ?? 0)
  const price = materialUnitPrice(row)
  const amount = qty * price
  return formatted ? amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : amount
}

function onGenSelectionChange(rows) {
  genSelection.value = rows
}

async function openGenerate() {
  if (purchasableReqs.value.length === 0) {
    ElMessage.warning('暂无可采购物料，请先刷新采购清单')
    return
  }
  generateVisible.value = true
}

async function onGenerateOpen() {
  genForm.value = { supplierId: null, expectedArrivalDate: '', remark: '' }
  genSelection.value = [...purchasableReqs.value]
  try {
    suppliers.value = await fetchActiveSupplierList() || []
    if (suppliers.value.length === 1) {
      genForm.value.supplierId = suppliers.value[0].supplierId
    }
  } catch {
    suppliers.value = []
  }
}

async function doGenerate() {
  if (!genForm.value.supplierId || genSelection.value.length === 0) {
    ElMessage.warning('请选择供应商并勾选缺料物料')
    return
  }
  const supplier = suppliers.value.find(s => s.supplierId === genForm.value.supplierId)
  generating.value = true
  try {
    const overrides = {}
    overrides[String(genForm.value.supplierId)] = {
      supplierName: supplier?.supplierName,
      supplierContact: supplier?.contactPerson,
      supplierPhone: supplier?.contactPhone,
      expectedArrivalDate: genForm.value.expectedArrivalDate || undefined
    }
    const created = await generatePurchaseOrder({
      requirementIds: genSelection.value.map(r => r.requirementId),
      forceSupplierId: genForm.value.supplierId,
      supplierOverrides: overrides,
      remark: genForm.value.remark || undefined
    })
    const list = Array.isArray(created) ? created : [created]
    ElMessage.success(`已生成采购单 ${list.map(o => o.purchaseOrderNo).join('、')}`)
    generateVisible.value = false
    await loadData()
    router.push('/purchase/order')
  } catch (e) {
    ElMessage.error(e?.message || '生成采购单失败')
  } finally {
    generating.value = false
  }
}

// ---- 接口数据暂时没有 AI 单据待确认字段，用 mock 占位 ----
// TODO: 对接 /purchase/ai/document/pending 接口后替换
const aiPending = ref(0)

const stats = computed(() => {
  const pending = requirements.value.filter(r => r.status === 'PENDING').length
  const overdue = orders.value.filter(o => {
    if (o.status === 'RECEIVED' || o.status === 'CANCELLED') return false
    return o.expectedArrivalDate && o.expectedArrivalDate < today
  }).length
  return { pendingRequirements: pending, overdueOrders: overdue }
})

function fmtQty(v) {
  const n = Number(v)
  if (!Number.isFinite(n)) return '0'
  return n % 1 === 0 ? String(n) : n.toFixed(1)
}

const AUDIT_CN = { PLAN_PENDING: '待计划', APPROVED: '已审核', PLANNED: '已排产', PRODUCING: '生产中' }
const ELIGIBLE_AUDIT = new Set(['APPROVED', 'PLAN_PENDING', 'PLANNED', 'PRODUCING'])

/** 后端未重启时，用现有接口拼装订单需求（活数据兜底） */
async function buildOrderDemandsFallback() {
  try {
    const [orders, items, inventory, byOrder] = await Promise.all([
      request.get('/order/customerOrder/list', { silent: true }),
      request.get('/order/orderItem/list', { silent: true }),
      request.get('/material/inventory/full', { silent: true }),
      fetchWorkbenchByOrder().catch(() => [])
    ])
    const stockByMat = {}
    for (const inv of inventory || []) {
      const id = inv.materialId
      if (!id) continue
      const avail = Number(inv.quantityAvailable ?? inv.quantityOnHand ?? 0)
      stockByMat[id] = (stockByMat[id] || 0) + avail
    }
    const shortageByOrder = {}
    const purchaseByOrder = {}
    for (const g of byOrder || []) {
      if (g.sourceType !== 'ORDER' && g.sourceType !== 'CUSTOMER_ORDER') continue
      const oid = g.sourceId
      const lines = g.lines || []
      shortageByOrder[oid] = lines.length
      purchaseByOrder[oid] = lines.reduce((s, l) => s + Number(l.shortageQuantity || 0), 0)
    }
    const itemsByOrder = {}
    for (const it of items || []) {
      if (!itemsByOrder[it.orderId]) itemsByOrder[it.orderId] = []
      itemsByOrder[it.orderId].push(it)
    }
    const rows = []
    for (const o of orders || []) {
      if (!ELIGIBLE_AUDIT.has(o.auditStatus)) continue
      for (const it of itemsByOrder[o.orderId] || []) {
        const orderQty = Number(it.quantity || 0)
        const matId = it.materialId
        const fgStock = Math.floor(stockByMat[matId] || 0)
        const shipFromStock = Math.min(orderQty, fgStock)
        const needToProduce = Math.max(0, orderQty - fgStock)
        rows.push({
          orderId: o.orderId,
          orderNo: o.orderNo,
          customerName: o.customerName,
          auditStatus: o.auditStatus,
          auditStatusCn: AUDIT_CN[o.auditStatus] || o.auditStatus,
          productName: it.productName || it.materialName || '',
          materialCode: it.materialCode || '',
          materialId: matId,
          orderQuantity: orderQty,
          finishedStock: fgStock,
          shipFromStock,
          needToProduce,
          shortageMaterialCount: shortageByOrder[o.orderId] || 0,
          suggestedPurchaseQty: purchaseByOrder[o.orderId] || 0,
          requiredDeliveryDate: it.deliveryDate || o.requiredDeliveryDate || ''
        })
      }
    }
    return rows
  } catch {
    return []
  }
}

async function recalculate() {
  loading.value = true
  try {
    await calculatePurchaseRequirements()
    await loadData()
  } finally {
    loading.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    const [orderRes, reqRes, demandRes] = await Promise.allSettled([
      request.get('/purchase/purchaseOrder/list'),
      fetchWorkbenchList({ scope: 'all' }),
      fetchOrderDemandOverview()
    ])
    orders.value = orderRes.status === 'fulfilled' ? (orderRes.value || []) : []
    requirements.value = reqRes.status === 'fulfilled' ? (reqRes.value || []) : []
    orderDemands.value = demandRes.status === 'fulfilled' ? (demandRes.value || []) : []
    if (!orderDemands.value.length) {
      orderDemands.value = await buildOrderDemandsFallback()
    }
    if (!requirements.value.length && orderDemands.value.length) {
      try {
        await calculatePurchaseRequirements({ silent: true })
        const retry = await fetchWorkbenchList({ scope: 'all' })
        requirements.value = retry || []
        const retryDemand = await fetchOrderDemandOverview().catch(() => null)
        if (retryDemand?.length) {
          orderDemands.value = retryDemand
        } else {
          orderDemands.value = await buildOrderDemandsFallback()
        }
      } catch { /* 静默：计算失败不阻断页面 */ }
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.pdb { padding: 16px 20px; background: transparent; min-height: 100%; box-sizing: border-box; }

.pdb-panel {
  background: transparent;
  border-radius: 8px;
  box-shadow: none;
  overflow: hidden;
}

.pdb-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  flex-wrap: wrap;
  gap: 12px;
}
.pdb-toolbar__title { font-size: 18px; font-weight: 700; color: #001b3f; line-height: 1.3; }
.pdb-toolbar__sub { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 6px; }
.pdb-toolbar__actions { display: flex; gap: 8px; flex-wrap: wrap; }

.pdb-section { padding: 16px 20px 0; }
.pdb-section--last { padding-bottom: 20px; }

.section-head {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}
.section-title { font-size: 14px; font-weight: 600; color: #001b3f; }
.section-sub { font-size: 12px; color: #8c8c8c; }
.section-link { margin-left: auto; font-size: 12px; color: #4096ff; cursor: pointer; }
.section-link:hover { text-decoration: underline; }

.stock-ok { color: #52c41a; font-weight: 600; }
.stock-low { color: #cf1322; font-weight: 600; }

.hero-badge { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.hero-badge--warn { background: #fff7e6; color: #d46b08; border: 1px solid #ffd591; }
.hero-badge--danger { background: #fff1f0; color: #cf1322; border: 1px solid #ffa39e; }
.hero-badge--info { background: #e6f4ff; color: #0958d9; border: 1px solid #91caff; }

.hero-btn { display: inline-flex; align-items: center; padding: 6px 14px; border-radius: 6px; font-size: 13px; text-decoration: none; border: 1px solid #d9d9d9; background: #fafafa; color: #001b3f; transition: all .15s; white-space: nowrap; }
.hero-btn:hover { border-color: #4096ff; color: #4096ff; background: #e6f4ff; }
.hero-btn--primary { background: #4096ff; color: #fff; border-color: #4096ff; }
.hero-btn--primary:hover { background: #1677ff; border-color: #1677ff; color: #fff; }
.hero-btn--success { background: #52c41a; color: #fff; border-color: #52c41a; }
.hero-btn--success:hover:not(:disabled) { background: #389e0d; border-color: #389e0d; color: #fff; }
.hero-btn:disabled { opacity: .55; cursor: not-allowed; }

.gen-total { text-align: right; margin-top: 10px; font-size: 14px; color: #595959; }
.gen-total strong { color: #cf1322; font-size: 16px; }

.bom-expand { padding: 8px 12px 12px 48px; background: #fafbfc; }
.bom-expand__head { font-size: 12px; color: #595959; margin-bottom: 8px; }
.bom-expand__head strong { color: #d46b08; }
.bom-expand--empty { padding: 12px 48px; font-size: 12px; color: #bfbfbf; }
.bom-expand__table { width: 100%; }

.material-cell { display: flex; align-items: center; gap: 8px; }
.material-thumb { width: 36px; height: 36px; border-radius: 6px; object-fit: cover; border: 1px solid #f0f0f0; flex-shrink: 0; }
.material-thumb--fallback { display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #e6f4ff, #f0f5ff); color: #4096ff; font-weight: 700; font-size: 14px; }
.material-cell__name { font-size: 13px; color: #262626; line-height: 1.3; }
.material-cell__code { font-size: 11px; color: #8c8c8c; }
</style>
