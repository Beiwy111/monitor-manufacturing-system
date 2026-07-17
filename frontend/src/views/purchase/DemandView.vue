<template>
  <ModulePageShell>
    <div class="wb-header">
      <div class="wb-header__left">
        <span class="wb-title">采购需求工作台</span>
        <el-tag v-if="requirements.length" type="warning" size="small" style="margin-left:10px">
          {{ requirements.length }} 条待处理
        </el-tag>
      </div>
      <div class="wb-header__right">
        <el-button @click="openPrintDialog">打印报表</el-button>
        <el-button :loading="calculating" type="primary" @click="doCalculate">刷新采购清单</el-button>
        <el-button type="success" :disabled="selection.length === 0" @click="generateVisible = true">
          批量生成采购单（{{ selection.length }}）
        </el-button>
      </div>
    </div>

    <div class="wb-toolbar">
      <el-radio-group v-model="viewMode" size="small" style="margin-right:16px">
        <el-radio-button value="material">物料视图</el-radio-button>
        <el-radio-button value="order">订单视图</el-radio-button>
      </el-radio-group>
      <el-radio-group v-if="viewMode==='material'" v-model="listScope" size="small" style="margin-right:16px" @change="loadList">
        <el-radio-button value="all">全部可采购</el-radio-button>
        <el-radio-button value="shortage">仅缺料</el-radio-button>
      </el-radio-group>
      <el-input v-if="viewMode==='material'" v-model="filterName" placeholder="物料名称" clearable
        style="width:180px;margin-right:8px" @input="loadList" />
      <el-select v-if="viewMode==='material'" v-model="filterPriority" placeholder="优先级" clearable
        style="width:120px" @change="loadList">
        <el-option label="紧急(1)" :value="1" />
        <el-option label="高(2)" :value="2" />
        <el-option label="普通(5)" :value="5" />
      </el-select>
    </div>

    <!-- 物料维度表格 -->
    <div v-if="viewMode==='material'" v-loading="loading" class="wb-table-wrap">
      <el-table :data="requirements" border stripe @selection-change="selection = $event">
        <el-table-column type="selection" width="42" :selectable="canSelect" />
        <el-table-column prop="materialCode" label="物料编码" width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="190">
          <template #default="{ row }">
            <div class="material-cell">
              <img
                v-if="materialImage(row)"
                class="material-thumb"
                :src="materialImage(row)"
                :alt="row.materialName"
              />
              <div v-else class="material-thumb material-thumb--fallback" :style="materialThumbStyle(row)">
                {{ materialInitial(row) }}
              </div>
              <div class="material-cell__text">
                <div class="material-cell__name">{{ row.materialName }}</div>
                <div class="material-cell__code">{{ row.materialCode }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="supplierName" label="默认供应商" width="130">
          <template #default="{ row }">
            <el-tag v-if="row.supplierName" type="info" size="small">{{ row.supplierName }}</el-tag>
            <span v-else style="color:#bfbfbf;font-size:12px">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="requiredQuantity" label="总需求" width="88" align="right" />
        <el-table-column prop="stockQuantity" label="库存" width="80" align="right" />
        <el-table-column prop="onPurchaseQuantity" label="在途" width="80" align="right" />
        <el-table-column prop="shortageQuantity" label="净缺料" width="88" align="right">
          <template #default="{ row }">
            <el-tag v-if="Number(row.shortageQuantity) > 0" type="danger" size="small">{{ row.shortageQuantity }}</el-tag>
            <el-tag v-else type="success" size="small">充足</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="suggestedPurchaseQuantity" label="建议采购" width="96" align="right">
          <template #default="{ row }">
            <span style="color:#d46b08;font-weight:600">{{ row.suggestedPurchaseQuantity }}</span>
            <el-tooltip v-if="Number(row.shortageQuantity) <= 0" content="库存充足，建议量为备库参考，生成时可修改" placement="top">
              <span class="backup-hint">备库</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="采购数量" width="118" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="rowQtyMap[row.requirementId]"
              :min="1"
              :step="1"
              size="small"
              controls-position="right"
              style="width:100px"
              @click.stop
            />
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="76" align="center">
          <template #default="{ row }">
            <el-tag :type="row.priority===1?'danger':row.priority===2?'warning':'info'" size="small">
              {{ row.priority===1?'紧急':row.priority===2?'高':'普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expectedArrivalDate" label="期望到货" width="108" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canSelect(row)"
              link
              type="success"
              size="small"
              @click="openSinglePurchase(row)"
            >采购</el-button>
            <el-button link type="primary" size="small" @click="viewDetail(row)">来源</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && requirements.length===0" class="wb-empty">
        {{ listScope === 'shortage' ? '当前无缺料物料' : '暂无可采购物料，请点击「刷新采购清单」' }}
      </div>
    </div>

    <!-- 订单维度视图 -->
    <div v-if="viewMode==='order'" v-loading="orderLoading" class="wb-table-wrap">
      <div v-for="group in orderGroups" :key="group.sourceType+group.sourceId" class="order-group">
        <div class="order-group__header">
          <el-tag :type="group.sourceType==='ORDER'?'primary':'success'" size="small">
            {{ group.sourceType==='ORDER'?'销售订单':'生产工单' }}
          </el-tag>
          <span class="order-group__no">{{ group.sourceNo || group.sourceId }}</span>
          <span class="order-group__count">共 {{ group.lines?.length }} 种缺料</span>
        </div>
        <el-table :data="group.lines" border size="small" style="margin-top:6px">
          <el-table-column prop="materialCode" label="物料编码" width="120" />
          <el-table-column prop="materialName" label="物料名称" min-width="140" />
          <el-table-column prop="requiredQuantity" label="需求数量" width="100" align="right">
            <template #default="{ row }">{{ fmtQty(row.requiredQuantity) }}</template>
          </el-table-column>
          <el-table-column prop="shortageQuantity" label="缺料数量" width="100" align="right">
            <template #default="{ row }">
              <span style="color:#e6a23c;font-weight:600">{{ fmtQty(row.shortageQuantity) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-if="!orderLoading && orderGroups.length===0" class="wb-empty">暂无真实订单维度缺料数据</div>
    </div>

    <!-- 来源明细弹窗 -->
    <el-dialog v-model="detailVisible" title="物料需求来源明细" width="640px">
      <div v-if="detailData" style="margin-bottom:12px">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="物料编码">{{ detailData.requirement?.materialCode }}</el-descriptions-item>
          <el-descriptions-item label="物料名称">{{ detailData.requirement?.materialName }}</el-descriptions-item>
          <el-descriptions-item label="净缺料">
            <span style="color:#e6a23c;font-weight:600">{{ detailData.requirement?.shortageQuantity }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <el-table :data="detailData?.sources||[]" border size="small">
        <el-table-column prop="sourceType" label="来源类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.sourceType==='ORDER'?'primary':'success'" size="small">
              {{ row.sourceType==='ORDER'?'销售订单':'生产工单' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源单号" min-width="140">
          <template #default="{ row }">{{ row.customerOrderNo||row.workOrderNo||'-' }}</template>
        </el-table-column>
        <el-table-column prop="requiredQuantity" label="需求数量" width="100" align="right">
          <template #default="{ row }">{{ fmtQty(row.requiredQuantity) }}</template>
        </el-table-column>
        <el-table-column prop="shortageQuantity" label="缺料数量" width="100" align="right">
          <template #default="{ row }"><span style="color:#e6a23c">{{ fmtQty(row.shortageQuantity) }}</span></template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 打印报表弹窗 -->
    <el-dialog v-model="printDialogVisible" title="打印报表" width="460px">
      <el-form :model="printForm" label-width="90px">
        <el-form-item label="报表类型">
          <el-radio-group v-model="printForm.reportType">
            <el-radio value="material-shortage">采购需求缺料表</el-radio>
            <el-radio value="order-shortage">订单维度缺料表</el-radio>
            <el-radio value="purchase-orders">采购订单表</el-radio>
            <el-radio value="arrival-progress">到货进度表</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="printDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="printLoading" @click="printSelectedReport">打印</el-button>
      </template>
    </el-dialog>

    <!-- 单物料创建采购单 -->
    <el-dialog v-model="singleVisible" title="创建采购单" width="520px" @open="onSingleOpen">
      <div v-if="singleForm.row" class="single-mat-preview">
        <img v-if="materialImage(singleForm.row)" class="material-thumb" :src="materialImage(singleForm.row)" :alt="singleForm.row.materialName" />
        <div v-else class="material-thumb material-thumb--fallback" :style="materialThumbStyle(singleForm.row)">
          {{ materialInitial(singleForm.row) }}
        </div>
        <div>
          <div class="single-mat-preview__name">{{ singleForm.row.materialName }}</div>
          <div class="single-mat-preview__meta">
            {{ singleForm.row.materialCode }} · 库存 {{ singleForm.row.stockQuantity ?? 0 }} · 在途 {{ singleForm.row.onPurchaseQuantity ?? 0 }}
          </div>
        </div>
      </div>
      <el-form :model="singleForm" label-width="96px" size="default" style="margin-top:12px">
        <el-form-item label="采购数量" required>
          <el-input-number v-model="singleForm.quantity" :min="1" :step="1" style="width:200px" />
          <span class="form-hint">可自定义数量，不限于建议采购量</span>
        </el-form-item>
        <el-form-item label="供应商" required>
          <el-select v-model="singleForm.supplierId" placeholder="请选择供应商" filterable style="width:100%">
            <el-option v-for="s in suppliers" :key="s.supplierId" :label="s.supplierName" :value="s.supplierId" />
          </el-select>
        </el-form-item>
        <el-form-item label="期望到货">
          <el-date-picker v-model="singleForm.expectedArrivalDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="singleForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
        <el-form-item label="预估金额">
          <span class="single-amount">¥{{ singleLineAmount }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="singleVisible = false">取消</el-button>
        <el-button type="primary" :loading="singleGenerating" :disabled="!singleForm.supplierId" @click="doSinglePurchase">
          生成采购单
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量生成采购单弹窗（按供应商分组预览） -->
    <el-dialog v-model="generateVisible" title="批量生成采购订单" width="680px" @open="onGenerateOpen">
      <el-form label-width="100px" size="small" style="margin-bottom:12px">
        <el-form-item label="拆单方式">
          <el-radio-group v-model="generateMode">
            <el-radio value="split">按默认供应商自动拆单</el-radio>
            <el-radio value="single">指定供应商（合并一张单）</el-radio>
            <el-radio value="individual">每项物料各生成一张单</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="generateMode === 'single'" label="选择供应商" required>
          <el-select v-model="selectedSupplierId" placeholder="请选择供应商" filterable style="width:100%">
            <el-option v-for="s in suppliers" :key="s.supplierId" :label="s.supplierName" :value="s.supplierId" />
          </el-select>
        </el-form-item>
      </el-form>
      <div v-if="generateMode === 'split' && supplierGroups.length === 0" style="color:#909399;text-align:center;padding:20px">
        所选需求暂无供应商绑定信息，将统一生成一张采购单
      </div>
      <div v-else-if="generateMode === 'split'">
        <el-alert type="info" :closable="false" style="margin-bottom:14px">
          已勾选 <strong>{{ selection.length }}</strong> 条需求，将按供应商拆分为
          <strong>{{ supplierGroups.length }}</strong> 张采购单
        </el-alert>
        <div v-for="grp in supplierGroups" :key="grp.supplierId" class="sup-group">
          <div class="sup-group__header">
            <el-tag :type="grp.supplierId === 0 ? 'warning' : 'primary'" size="small">
              {{ grp.supplierName || '未分配供应商' }}
            </el-tag>
            <span class="sup-group__count">{{ grp.items.length }} 种物料</span>
          </div>
          <el-table :data="grp.items" border size="small" style="margin:6px 0 10px">
            <el-table-column prop="materialCode" label="编码" width="110" />
            <el-table-column prop="materialName" label="物料名称" min-width="120" />
            <el-table-column label="采购数量" width="120" align="right">
              <template #default="{ row }">
                <el-input-number
                  v-model="purchaseQtyMap[row.requirementId]"
                  :min="1"
                  :step="1"
                  size="small"
                  controls-position="right"
                  style="width:108px"
                />
              </template>
            </el-table-column>
            <el-table-column prop="stockQuantity" label="库存" width="72" align="right" />
          </el-table>
          <el-form :model="grp" label-width="90px" size="small">
            <el-form-item label="期望到货">
              <el-date-picker v-model="grp.expectedArrivalDate" type="date"
                value-format="YYYY-MM-DD" style="width:200px" placeholder="可选" />
            </el-form-item>
          </el-form>
        </div>
      </div>
      <div v-else-if="generateMode === 'individual'">
        <el-alert type="info" :closable="false" style="margin-bottom:12px">
          已勾选 <strong>{{ selection.length }}</strong> 条需求，将分别生成 <strong>{{ selection.length }}</strong> 张独立采购单
        </el-alert>
        <el-table :data="selection" border size="small" max-height="280">
          <el-table-column prop="materialCode" label="编码" width="110" />
          <el-table-column prop="materialName" label="物料名称" min-width="120" />
          <el-table-column prop="supplierName" label="供应商" min-width="120" show-overflow-tooltip />
          <el-table-column label="采购数量" width="120" align="right">
            <template #default="{ row }">
              <el-input-number
                v-model="purchaseQtyMap[row.requirementId]"
                :min="1"
                :step="1"
                size="small"
                controls-position="right"
                style="width:108px"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-else-if="generateMode === 'single'">
        <el-alert type="info" :closable="false" style="margin-bottom:12px">
          已勾选 <strong>{{ selection.length }}</strong> 条需求，将合并为 <strong>1</strong> 张采购单
        </el-alert>
        <el-table :data="selection" border size="small" max-height="280">
          <el-table-column prop="materialCode" label="编码" width="110" />
          <el-table-column prop="materialName" label="物料名称" min-width="120" />
          <el-table-column label="采购数量" width="120" align="right">
            <template #default="{ row }">
              <el-input-number
                v-model="purchaseQtyMap[row.requirementId]"
                :min="1"
                :step="1"
                size="small"
                controls-position="right"
                style="width:108px"
              />
            </template>
          </el-table-column>
          <el-table-column prop="stockQuantity" label="库存" width="72" align="right" />
          <el-table-column label="单价(元)" width="90" align="right">
            <template #default="{ row }">{{ unitPriceOf(row) }}</template>
          </el-table-column>
          <el-table-column label="行金额" width="100" align="right">
            <template #default="{ row }">{{ lineAmountOf(row, purchaseQtyMap[row.requirementId]) }}</template>
          </el-table-column>
        </el-table>
        <el-form :model="globalForm" label-width="90px" size="small" style="margin-top:10px">
          <el-form-item label="期望到货">
            <el-date-picker v-model="globalForm.expectedArrivalDate" type="date" value-format="YYYY-MM-DD" style="width:200px" />
          </el-form-item>
        </el-form>
      </div>
      <el-form :model="globalForm" label-width="90px" size="small" style="margin-top:8px;border-top:1px solid #f0f0f0;padding-top:12px">
        <el-form-item label="全局备注">
          <el-input v-model="globalForm.remark" type="textarea" :rows="2" placeholder="选填，各采购单共用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible=false">取消</el-button>
        <el-button type="primary" :loading="generating" :disabled="generateMode === 'single' && !selectedSupplierId" @click="doGenerate">
          确认生成 {{ generateMode === 'individual' ? selection.length : (generateMode === 'single' ? 1 : (supplierGroups.length || 1)) }} 张采购单
        </el-button>
      </template>
    </el-dialog>
  </ModulePageShell>
</template>
<!-- SCRIPT_PLACEHOLDER -->

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  calculatePurchaseRequirements,
  fetchWorkbenchList,
  fetchWorkbenchDetail,
  fetchWorkbenchByOrder,
  fetchPurchaseOrderList,
  generatePurchaseOrder,
  fetchActiveSupplierList
} from '@/api/business'
import { materialImageByCode } from '@/utils/materialImages'
import { sortNewestFirst } from '@/utils/sortNewestFirst'
import ModulePageShell from '@/components/module/ModulePageShell.vue'
import { moduleStatusType } from '@/constants/moduleStatus'

const router = useRouter()
const viewMode = ref('material')
const listScope = ref('all')
const filterName = ref('')
const filterPriority = ref(null)

const requirements = ref([])
const selection = ref([])
const loading = ref(false)
const calculating = ref(false)

const orderGroups = ref([])
const orderLoading = ref(false)

const detailVisible = ref(false)
const detailData = ref(null)

const generateVisible = ref(false)
const generating = ref(false)
const generateMode = ref('split')
const selectedSupplierId = ref(null)
const suppliers = ref([])
const purchaseQtyMap = reactive({})
const rowQtyMap = reactive({})

const singleVisible = ref(false)
const singleGenerating = ref(false)
const singleForm = reactive({
  row: null,
  quantity: 1,
  supplierId: null,
  expectedArrivalDate: '',
  remark: ''
})

const MATERIAL_PRICE = {
  'MAT-001': 280, 'MAT-002': 85, 'MAT-003': 12.5,
  'MAT-004': 45, 'MAT-005': 65, 'MAT-006': 35
}

// 按供应商分组的预览数据，弹窗打开时从 selection 计算
const supplierGroups = ref([])
const globalForm = reactive({ remark: '' })
const printDialogVisible = ref(false)
const printLoading = ref(false)
const printForm = reactive({ reportType: 'material-shortage' })

function openPrintDialog() {
  printForm.reportType = viewMode.value === 'order' ? 'order-shortage' : 'material-shortage'
  printDialogVisible.value = true
}

function buildSupplierGroups() {
  const map = new Map()
  for (const row of selection.value) {
    const key = row.supplierId ?? 0
    if (!map.has(key)) {
      map.set(key, {
        supplierId: key,
        supplierName: row.supplierName || null,
        expectedArrivalDate: '',
        items: []
      })
    }
    map.get(key).items.push(row)
  }
  supplierGroups.value = [...map.values()]
}

function onGenerateOpen() {
  globalForm.remark = ''
  globalForm.expectedArrivalDate = ''
  generateMode.value = 'split'
  selectedSupplierId.value = null
  buildSupplierGroups()
  initPurchaseQtyMap(selection.value)
  fetchActiveSupplierList().then(list => {
    suppliers.value = list || []
    if (suppliers.value.length === 1) selectedSupplierId.value = suppliers.value[0].supplierId
  }).catch(() => { suppliers.value = [] })
}

function unitPriceOf(row) {
  return MATERIAL_PRICE[row.materialCode] ?? '—'
}

function lineAmountOf(row, qtyOverride) {
  const qty = Number(qtyOverride ?? row.suggestedPurchaseQuantity ?? row.shortageQuantity ?? 0)
  const price = Number(MATERIAL_PRICE[row.materialCode] ?? 0)
  return (qty * price).toLocaleString(undefined, { minimumFractionDigits: 2 })
}

function initRowQtyMap(rows = []) {
  for (const row of rows) {
    if (!row.requirementId) continue
    if (rowQtyMap[row.requirementId] == null) {
      const suggested = Number(row.suggestedPurchaseQuantity ?? row.shortageQuantity ?? 0)
      rowQtyMap[row.requirementId] = suggested > 0 ? suggested : 100
    }
  }
}

function initPurchaseQtyMap(rows = []) {
  Object.keys(purchaseQtyMap).forEach(k => delete purchaseQtyMap[k])
  for (const row of rows) {
    if (!row.requirementId) continue
    const suggested = Number(row.suggestedPurchaseQuantity ?? row.shortageQuantity ?? 0)
    purchaseQtyMap[row.requirementId] = rowQtyMap[row.requirementId] ?? (suggested > 0 ? suggested : 100)
  }
}

function statusLabel(status) {
  const map = {
    PENDING: '待采购', SELECTED: '已锁定', PURCHASED: '已生成采购单',
    PART_ARRIVED: '部分到货', ARRIVED: '已到货', CANCELLED: '已取消'
  }
  return map[status] || status
}

function statusType(status) {
  return moduleStatusType('purchaseRequirement', status, '')
}

function fmtQty(value) {
  const n = Number(value)
  if (!Number.isFinite(n)) return value ?? ''
  return Number.isInteger(n) ? n : Math.ceil(n)
}

function canSelect(row) {
  return row.status === 'PENDING'
}

function materialImage(row) {
  return row.materialImageUrl || row.imageUrl || row.pictureUrl || materialImageByCode(row.materialCode) || ''
}

function materialInitial(row) {
  const name = row.materialName || row.materialCode || '?'
  return name.slice(0, 1).toUpperCase()
}

function materialThumbStyle(row) {
  const palette = [
    ['#e8f3ff', '#1677ff'],
    ['#fff7e6', '#d46b08'],
    ['#f6ffed', '#389e0d'],
    ['#fff1f0', '#cf1322'],
    ['#f9f0ff', '#722ed1'],
    ['#e6fffb', '#08979c']
  ]
  const code = row.materialCode || row.materialName || ''
  const sum = [...code].reduce((acc, ch) => acc + ch.charCodeAt(0), 0)
  const [bg, color] = palette[sum % palette.length]
  return { background: bg, color }
}

function normalizeRequirements(rows = []) {
  const map = new Map()
  for (const row of rows || []) {
    if (!row || row.status !== 'PENDING') continue
    const key = row.materialId ?? row.materialCode ?? row.materialName
    if (!key) continue
    const existing = map.get(key)
    if (!existing || isBetterRequirement(row, existing)) {
      map.set(key, row)
    }
  }
  return [...map.values()]
}

function isBetterRequirement(candidate, existing) {
  const cp = candidate.priority ?? Number.MAX_SAFE_INTEGER
  const ep = existing.priority ?? Number.MAX_SAFE_INTEGER
  if (cp !== ep) return cp < ep
  const cs = Number(candidate.shortageQuantity || 0)
  const es = Number(existing.shortageQuantity || 0)
  if (cs !== es) return cs > es
  return Number(candidate.requirementId || 0) > Number(existing.requirementId || 0)
}

async function loadList() {
  loading.value = true
  try {
    const params = { scope: listScope.value }
    if (filterName.value) params.materialName = filterName.value
    if (filterPriority.value != null) params.priority = filterPriority.value
    requirements.value = sortNewestFirst(normalizeRequirements(await fetchWorkbenchList(params) || []))
    initRowQtyMap(requirements.value)
    selection.value = []
  } catch {
    ElMessage.error('加载采购清单失败')
  } finally {
    loading.value = false
  }
}

async function loadOrderView() {
  orderLoading.value = true
  try {
    orderGroups.value = sortNewestFirst(await fetchWorkbenchByOrder() || [])
  } catch {
    ElMessage.error('加载订单维度数据失败')
  } finally {
    orderLoading.value = false
  }
}

async function doCalculate(showMessage = true) {
  calculating.value = true
  try {
    await calculatePurchaseRequirements()
    await loadList()
    if (showMessage) {
      const shortageCount = requirements.value.filter(r => Number(r.shortageQuantity) > 0).length
      ElMessage.success(`刷新完成，共 ${requirements.value.length} 种可采购物料（缺料 ${shortageCount} 种）`)
    }
    if (viewMode.value === 'order') await loadOrderView()
  } catch (e) {
    ElMessage.error(e?.message || '刷新采购清单失败')
  } finally {
    calculating.value = false
  }
}

async function viewDetail(row) {
  try {
    detailData.value = await fetchWorkbenchDetail(row.requirementId)
    detailVisible.value = true
  } catch {
    ElMessage.error('加载来源明细失败')
  }
}

function esc(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
}

function formatReportDate() {
  const d = new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function buildMaterialReportRows() {
  return requirements.value.map(row => `
    <tr>
      <td>${esc(row.materialCode)}</td>
      <td>${esc(row.materialName)}</td>
      <td>${esc(row.supplierName || '未分配')}</td>
      <td class="num">${esc(row.requiredQuantity)}</td>
      <td class="num">${esc(row.stockQuantity)}</td>
      <td class="num">${esc(row.onPurchaseQuantity)}</td>
      <td class="num strong">${esc(row.shortageQuantity)}</td>
      <td class="num">${esc(row.suggestedPurchaseQuantity)}</td>
      <td>${esc(row.priority)}</td>
      <td>${esc(row.expectedArrivalDate || '-')}</td>
      <td>${esc(statusLabel(row.status))}</td>
    </tr>
  `).join('')
}

function buildOrderReportRows() {
  return orderGroups.value.flatMap(group => {
    const groupLabel = `${group.sourceType === 'ORDER' ? '销售订单' : '生产工单'} ${group.sourceNo || group.sourceId || ''}`
    return (group.lines || []).map(row => `
      <tr>
        <td>${esc(groupLabel)}</td>
        <td>${esc(row.materialCode)}</td>
        <td>${esc(row.materialName)}</td>
        <td class="num">${esc(fmtQty(row.requiredQuantity))}</td>
        <td class="num strong">${esc(fmtQty(row.shortageQuantity))}</td>
      </tr>
    `)
  }).join('')
}

function purchaseStatusLabel(status) {
  const map = {
    DRAFT: '草稿', SUBMITTED: '已提交', APPROVED: '已审核',
    PART_RECEIVED: '部分到货', RECEIVED: '已到货', CANCELLED: '已取消'
  }
  return map[status] || status || '-'
}

function isArrivalOverdue(row) {
  if (!row.expectedArrivalDate || row.status === 'RECEIVED' || row.status === 'CANCELLED') return false
  return new Date(row.expectedArrivalDate) < new Date()
}

function buildPurchaseOrderReportRows(orders) {
  return orders.map(row => `
    <tr>
      <td>${esc(row.purchaseOrderNo)}</td>
      <td>${esc(row.supplierName || '-')}</td>
      <td>${esc(row.purchaseDate || '-')}</td>
      <td>${esc(row.expectedArrivalDate || '-')}</td>
      <td class="num">${esc(row.totalAmount != null ? Number(row.totalAmount).toLocaleString() : '-')}</td>
      <td>${esc(purchaseStatusLabel(row.status))}</td>
    </tr>
  `).join('')
}

function buildArrivalProgressReportRows(orders) {
  return orders.map(row => `
    <tr>
      <td>${esc(row.purchaseOrderNo)}</td>
      <td>${esc(row.supplierName || '-')}</td>
      <td>${esc(row.expectedArrivalDate || '-')}</td>
      <td>${esc(purchaseStatusLabel(row.status))}</td>
      <td>${isArrivalOverdue(row) ? '是' : '否'}</td>
      <td class="num">${esc(row.totalAmount != null ? Number(row.totalAmount).toLocaleString() : '-')}</td>
    </tr>
  `).join('')
}

async function printSelectedReport() {
  printLoading.value = true
  try {
    if (printForm.reportType === 'material-shortage' && requirements.value.length === 0) {
      await loadList()
    }
    if (printForm.reportType === 'order-shortage' && orderGroups.value.length === 0) {
      await loadOrderView()
    }
    let externalRows = null
    if (printForm.reportType === 'purchase-orders' || printForm.reportType === 'arrival-progress') {
      externalRows = await fetchPurchaseOrderList() || []
    }
    printDialogVisible.value = false
    printReport(printForm.reportType, externalRows)
  } catch (e) {
    ElMessage.error(e?.message || '加载报表数据失败')
  } finally {
    printLoading.value = false
  }
}

function printReport(reportType, externalRows = null) {
  const reportMap = {
    'material-shortage': {
      title: '采购需求缺料报表',
      filters: `物料名称：${filterName.value || '全部'}　状态：当前待采购　优先级：${filterPriority.value || '全部'}`,
      head: '<tr><th>物料编码</th><th>物料名称</th><th>默认供应商</th><th>总需求</th><th>库存</th><th>在途</th><th>净缺料</th><th>建议采购</th><th>优先级</th><th>期望到货</th><th>状态</th></tr>',
      rows: buildMaterialReportRows()
    },
    'order-shortage': {
      title: '订单维度缺料报表',
      filters: '订单视图',
      head: '<tr><th>来源单据</th><th>物料编码</th><th>物料名称</th><th>需求数量</th><th>缺料数量</th></tr>',
      rows: buildOrderReportRows()
    },
    'purchase-orders': {
      title: '采购订单报表',
      filters: '采购订单',
      head: '<tr><th>采购单号</th><th>供应商</th><th>采购日期</th><th>期望到货</th><th>金额</th><th>状态</th></tr>',
      rows: buildPurchaseOrderReportRows(externalRows || [])
    },
    'arrival-progress': {
      title: '到货进度报表',
      filters: '到货进度',
      head: '<tr><th>采购单号</th><th>供应商</th><th>期望到货</th><th>状态</th><th>是否逾期</th><th>金额</th></tr>',
      rows: buildArrivalProgressReportRows(externalRows || [])
    }
  }
  const report = reportMap[reportType] || reportMap['material-shortage']
  const html = `<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>${esc(report.title)}</title>
  <style>
    body { font-family: "Microsoft YaHei", Arial, sans-serif; color: #1f2d3d; padding: 24px; }
    h1 { font-size: 22px; text-align: center; margin: 0 0 12px; }
    .meta { display: flex; justify-content: space-between; font-size: 12px; color: #606266; margin-bottom: 14px; }
    table { width: 100%; border-collapse: collapse; font-size: 12px; }
    th, td { border: 1px solid #dcdfe6; padding: 7px 8px; text-align: left; }
    th { background: #f5f7fa; font-weight: 700; }
    .num { text-align: right; }
    .strong { color: #c27a00; font-weight: 700; }
    @media print { body { padding: 0; } }
  </style>
</head>
<body>
  <h1>${esc(report.title)}</h1>
  <div class="meta">
    <span>${esc(report.filters)}</span>
    <span>打印时间：${formatReportDate()}</span>
  </div>
  <table>
    <thead>${report.head}</thead>
    <tbody>${report.rows}</tbody>
  </table>
</body>
</html>`
  const win = window.open('', '_blank')
  if (!win) {
    ElMessage.error('浏览器阻止了打印窗口，请允许弹窗后重试')
    return
  }
  win.document.open()
  win.document.write(html)
  win.document.close()
  win.focus()
  setTimeout(() => win.print(), 300)
}

const singleLineAmount = computed(() => {
  if (!singleForm.row) return '0.00'
  return lineAmountOf(singleForm.row, singleForm.quantity)
})

function openSinglePurchase(row) {
  singleForm.row = row
  singleForm.quantity = Number(rowQtyMap[row.requirementId]) || Number(row.suggestedPurchaseQuantity) || 100
  singleForm.supplierId = row.supplierId || null
  singleForm.expectedArrivalDate = row.expectedArrivalDate || ''
  singleForm.remark = ''
  singleVisible.value = true
}

function onSingleOpen() {
  fetchActiveSupplierList().then(list => {
    suppliers.value = list || []
    if (!singleForm.supplierId && suppliers.value.length === 1) {
      singleForm.supplierId = suppliers.value[0].supplierId
    }
  }).catch(() => { suppliers.value = [] })
}

async function doSinglePurchase() {
  const row = singleForm.row
  if (!row?.requirementId) return
  const qty = Number(singleForm.quantity)
  if (!qty || qty <= 0) {
    ElMessage.warning('采购数量必须大于 0')
    return
  }
  if (!singleForm.supplierId) {
    ElMessage.warning('请选择供应商')
    return
  }
  const supplier = suppliers.value.find(s => s.supplierId === singleForm.supplierId)
  singleGenerating.value = true
  try {
    const created = await generatePurchaseOrder({
      requirementIds: [row.requirementId],
      forceSupplierId: singleForm.supplierId,
      quantityOverrides: { [row.requirementId]: qty },
      supplierOverrides: {
        [String(singleForm.supplierId)]: {
          supplierName: supplier?.supplierName,
          supplierContact: supplier?.contactPerson,
          supplierPhone: supplier?.contactPhone,
          expectedArrivalDate: singleForm.expectedArrivalDate || undefined
        }
      },
      remark: singleForm.remark || undefined
    })
    const order = Array.isArray(created) ? created[0] : created
    rowQtyMap[row.requirementId] = qty
    singleVisible.value = false
    ElMessage.success(`已生成采购单 ${order?.purchaseOrderNo || ''}`)
    await doCalculate(false)
    try {
      await ElMessageBox.confirm('是否前往采购订单页查看？', '创建成功', {
        confirmButtonText: '去查看',
        cancelButtonText: '留在此页',
        type: 'success'
      })
      router.push('/purchase/order')
    } catch { /* 留在此页 */ }
  } catch (e) {
    ElMessage.error(e?.message || '生成采购单失败')
  } finally {
    singleGenerating.value = false
  }
}

async function doGenerate() {
  if (selection.value.length === 0) {
    ElMessage.warning('请先勾选采购需求')
    return
  }
  if (generateMode.value === 'single' && !selectedSupplierId.value) {
    ElMessage.warning('请选择供应商')
    return
  }
  const quantityOverrides = {}
  for (const row of selection.value) {
    const qty = Number(purchaseQtyMap[row.requirementId])
    if (!qty || qty <= 0) {
      ElMessage.warning(`请为「${row.materialName}」填写大于 0 的采购数量`)
      return
    }
    quantityOverrides[row.requirementId] = qty
  }
  generating.value = true
  try {
    if (generateMode.value === 'individual') {
      const createdNos = []
      for (const row of selection.value) {
        const qty = Number(purchaseQtyMap[row.requirementId])
        if (!qty || qty <= 0) {
          ElMessage.warning(`请为「${row.materialName}」填写大于 0 的采购数量`)
          return
        }
        const supplierId = row.supplierId || selectedSupplierId.value
        if (!supplierId) {
          ElMessage.warning(`物料「${row.materialName}」未绑定供应商，请先指定默认供应商`)
          return
        }
        const supplier = suppliers.value.find(s => s.supplierId === supplierId)
        const created = await generatePurchaseOrder({
          requirementIds: [row.requirementId],
          forceSupplierId: supplierId,
          quantityOverrides: { [row.requirementId]: qty },
          supplierOverrides: {
            [String(supplierId)]: {
              supplierName: supplier?.supplierName || row.supplierName,
              supplierContact: supplier?.contactPerson,
              supplierPhone: supplier?.contactPhone,
              expectedArrivalDate: globalForm.expectedArrivalDate || undefined
            }
          },
          remark: globalForm.remark || undefined
        })
        const order = Array.isArray(created) ? created[0] : created
        if (order?.purchaseOrderNo) createdNos.push(order.purchaseOrderNo)
      }
      ElMessage.success(`成功生成 ${createdNos.length} 张采购单：${createdNos.join('、')}`)
      generateVisible.value = false
      await doCalculate(false)
      return
    }

    const overrides = {}
    if (generateMode.value === 'split') {
      for (const grp of supplierGroups.value) {
        if (grp.expectedArrivalDate) {
          overrides[String(grp.supplierId)] = { expectedArrivalDate: grp.expectedArrivalDate }
        }
      }
    } else {
      const supplier = suppliers.value.find(s => s.supplierId === selectedSupplierId.value)
      overrides[String(selectedSupplierId.value)] = {
        supplierName: supplier?.supplierName,
        supplierContact: supplier?.contactPerson,
        supplierPhone: supplier?.contactPhone,
        expectedArrivalDate: globalForm.expectedArrivalDate || undefined
      }
    }
    const payload = {
      requirementIds: selection.value.map(r => r.requirementId),
      supplierOverrides: overrides,
      quantityOverrides,
      remark: globalForm.remark || undefined
    }
    if (generateMode.value === 'single') {
      payload.forceSupplierId = selectedSupplierId.value
    }
    const orders = await generatePurchaseOrder(payload)
    const orderList = Array.isArray(orders) ? orders : [orders]
    const nos = orderList.map(o => o.purchaseOrderNo).join('、')
    ElMessage.success(`成功生成 ${orderList.length} 张采购单：${nos}`)
    generateVisible.value = false
    await doCalculate(false)
  } catch (e) {
    ElMessage.error(e?.message || '生成采购单失败')
  } finally {
    generating.value = false
  }
}

watch(viewMode, (val) => {
  if (val === 'order') loadOrderView()
  else loadList()
})

onMounted(() => { doCalculate(false) })
</script>

<style scoped>
.backup-hint {
  margin-left: 4px;
  font-size: 10px;
  color: #8c8c8c;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 0 4px;
}
.form-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #8c8c8c;
}
.single-mat-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}
.single-mat-preview__name {
  font-size: 15px;
  font-weight: 600;
  color: #001b3f;
}
.single-mat-preview__meta {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}
.single-amount {
  font-size: 18px;
  font-weight: 700;
  color: #cf1322;
}
.wb-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.wb-title {
  font-size: 18px;
  font-weight: 700;
  color: #001b3f;
}
.wb-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}
.wb-table-wrap {
  background: #fff;
  border-radius: 6px;
  padding: 12px;
  min-height: 200px;
}
.wb-empty {
  text-align: center;
  color: #909399;
  padding: 60px 0;
  font-size: 14px;
}
.material-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.material-thumb {
  width: 38px;
  height: 38px;
  border-radius: 6px;
  object-fit: cover;
  flex: 0 0 38px;
  border: 1px solid #ebeef5;
}
.material-thumb--fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
}
.material-cell__text {
  min-width: 0;
  line-height: 1.35;
}
.material-cell__name {
  color: #001b3f;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.material-cell__code {
  margin-top: 3px;
  color: #909399;
  font-size: 12px;
}
.order-group {
  margin-bottom: 20px;
}
.order-group__header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 4px;
}
.order-group__no {
  font-weight: 600;
  font-size: 14px;
  color: #001b3f;
}
.order-group__count {
  font-size: 13px;
  color: #909399;
}
.sup-group {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 12px;
}
.sup-group__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}
.sup-group__count {
  font-size: 12px;
  color: #8c8c8c;
}
</style>
