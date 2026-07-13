<template>
  <div class="pdb">
    <!-- ① 顶部工作概览 -->
    <div class="pdb-hero">
      <div class="pdb-hero__left">
        <div class="pdb-hero__title">采购工作台</div>
        <div class="pdb-hero__sub">
          <span v-if="stats.pendingRequirements > 0" class="hero-badge hero-badge--warn">待采购 {{ stats.pendingRequirements }}</span>
          <span v-if="stats.overdueOrders > 0" class="hero-badge hero-badge--danger">逾期未到 {{ stats.overdueOrders }}</span>
          <span v-if="aiPending > 0" class="hero-badge hero-badge--info">AI单据待确认 {{ aiPending }}</span>
          <span v-if="stats.pendingRequirements === 0 && stats.overdueOrders === 0" class="hero-badge hero-badge--ok">今日无紧急事项</span>
        </div>
      </div>
      <div class="pdb-hero__actions">
        <button
          class="hero-btn hero-btn--success"
          :disabled="pendingReqs.length === 0"
          @click="openGenerate"
        >一键生成采购单</button>
        <router-link to="/purchase/ai-document" class="hero-btn hero-btn--primary">AI 单据录入</router-link>
        <router-link to="/purchase/demand" class="hero-btn">查看缺料需求</router-link>
        <router-link to="/purchase/order" class="hero-btn">采购订单</router-link>
      </div>
    </div>

    <!-- ② 订单需求与成品库存（审核后同步采购） -->
    <div class="pdb-order-section">
      <div class="pane-header">
        <span class="pane-title">订单物料需求总览</span>
        <span class="pane-sub">审核通过订单自动同步 · 扣减成品库存后计算缺料</span>
        <span class="pane-link" style="cursor:pointer;margin-left:auto" @click="recalculate">重新计算缺料</span>
      </div>
      <el-table :data="orderDemands" border stripe size="small" v-loading="loading" style="width:100%"
        empty-text="暂无待处理订单，审核通过后将自动出现在此">
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
    </div>

    <!-- ②b 缺料物料明细 -->
    <div class="pdb-order-section">
      <div class="pane-header">
        <span class="pane-title">缺料物料明细</span>
        <span class="pane-sub">展示需采购的物料、缺口数量与建议采购量</span>
        <span class="pane-link" style="cursor:pointer;margin-left:auto" @click="$router.push('/purchase/demand')">进入需求工作台</span>
      </div>
      <el-table :data="pendingReqs" border stripe size="small" v-loading="loading" style="width:100%"
        empty-text="暂无缺料，审核订单后将自动计算">
        <el-table-column prop="materialCode" label="物料编码" width="110" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="supplierName" label="默认供应商" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.supplierName">{{ row.supplierName }}</span>
            <span v-else style="color:#bfbfbf">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="requiredQuantity" label="总需求" width="80" align="right" />
        <el-table-column prop="stockQuantity" label="库存" width="72" align="right" />
        <el-table-column prop="onPurchaseQuantity" label="在途" width="72" align="right" />
        <el-table-column prop="shortageQuantity" label="净缺料" width="80" align="right">
          <template #default="{ row }">
            <strong style="color:#cf1322">{{ row.shortageQuantity }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="suggestedPurchaseQuantity" label="建议采购" width="88" align="right">
          <template #default="{ row }">
            <strong style="color:#d46b08">{{ row.suggestedPurchaseQuantity }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="76" align="center">
          <template #default="{ row }">
            <el-tag :type="row.priority === 1 ? 'danger' : row.priority === 2 ? 'warning' : 'info'" size="small">
              {{ row.priority === 1 ? '紧急' : row.priority === 2 ? '高' : '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expectedArrivalDate" label="期望到货" width="108" />
      </el-table>
    </div>

    <!-- ③ KPI 卡片 -->
    <div class="pdb-kpi-row">
      <div v-for="kpi in kpiCards" :key="kpi.label" class="kpi-card" :class="'kpi-card--' + kpi.color">
        <div class="kpi-card__icon">{{ kpi.icon }}</div>
        <div class="kpi-card__body">
          <div class="kpi-card__value">{{ kpi.value }}</div>
          <div class="kpi-card__label">{{ kpi.label }}</div>
          <div class="kpi-card__hint" :class="'hint--' + kpi.hintColor">{{ kpi.hint }}</div>
        </div>
      </div>
    </div>

    <!-- ③ 核心任务区 + Top物料 + 订单摘要 -->
    <div class="pdb-main-row">
      <!-- 左：待办优先级列表 -->
      <div class="pane pane--task">
        <div class="pane-header">
          <span class="pane-title">待办事项</span>
          <el-tag size="small" type="warning">{{ todos.length }}</el-tag>
        </div>
        <div class="task-list">
          <div v-if="todos.length === 0" class="task-empty">暂无待办事项</div>
          <div
            v-for="t in todos"
            :key="t.id"
            class="task-item"
            :class="'task-item--' + t.level"
            @click="$router.push(t.path)"
          >
            <span class="task-item__dot"></span>
            <div class="task-item__body">
              <div class="task-item__title">{{ t.title }}</div>
              <div class="task-item__meta">{{ t.meta }}</div>
            </div>
            <el-tag :type="t.level === 'danger' ? 'danger' : t.level === 'warn' ? 'warning' : 'info'" size="small">{{ t.tag }}</el-tag>
          </div>
        </div>
      </div>

      <!-- 中：缺料 Top 物料排行 -->
      <div class="pane pane--chart">
        <div class="pane-header">
          <span class="pane-title">缺料 Top 物料</span>
          <router-link to="/purchase/demand" class="pane-link">查看全部</router-link>
        </div>
        <div v-if="topMaterials.length === 0" class="chart-empty">暂无缺料数据，请先执行缺料计算</div>
        <div v-else class="rank-list">
          <div v-for="(item, idx) in topMaterials" :key="item.materialId || idx" class="rank-item">
            <span class="rank-item__no" :class="idx < 3 ? 'rank-item__no--top' : ''">{{ idx + 1 }}</span>
            <div class="rank-item__info">
              <div class="rank-item__name">{{ item.materialName || item.materialCode || '未知物料' }}</div>
              <div class="rank-item__bar-wrap">
                <div class="rank-item__bar" :style="{ width: barWidth(item.shortageQuantity) + '%' }" :class="idx === 0 ? 'bar--red' : idx === 1 ? 'bar--orange' : 'bar--blue'"></div>
              </div>
            </div>
            <span class="rank-item__val">{{ item.shortageQuantity }}</span>
            <el-tag :type="item.priority === 1 ? 'danger' : item.priority === 2 ? 'warning' : 'info'" size="small">{{ item.priority === 1 ? '紧急' : item.priority === 2 ? '高' : '普通' }}</el-tag>
          </div>
        </div>
      </div>

      <!-- 右：订单状态摘要 + 到货风险 -->
      <div class="pane pane--summary">
        <div class="pane-header"><span class="pane-title">订单状态摘要</span></div>
        <div class="summary-list">
          <div v-for="s in orderSummary" :key="s.label" class="summary-item">
            <span class="summary-item__label">{{ s.label }}</span>
            <div class="summary-item__bar-wrap">
              <div class="summary-item__bar" :style="{ width: s.pct + '%', background: s.color }"></div>
            </div>
            <span class="summary-item__val" :style="{ color: s.color }">{{ s.value }}</span>
          </div>
        </div>
        <div class="pane-header" style="margin-top:16px"><span class="pane-title">到货风险</span></div>
        <div v-if="overdueOrders.length === 0" class="chart-empty">无逾期订单</div>
        <div v-else class="risk-list">
          <div v-for="o in overdueOrders.slice(0, 5)" :key="o.purchaseOrderId" class="risk-item" @click="$router.push('/purchase/order')">
            <span class="risk-item__no">{{ o.purchaseOrderNo }}</span>
            <span class="risk-item__supplier">{{ o.supplierName || '—' }}</span>
            <el-tag type="danger" size="small">逾期</el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- ④ 今日待办表格 -->
    <div class="pdb-table-section">
      <div class="pane-header">
        <span class="pane-title">今日重点任务</span>
        <span class="pane-link" style="cursor:pointer" @click="loadData">刷新</span>
      </div>
      <el-table :data="taskTable" border stripe size="small" style="width:100%">
        <el-table-column prop="type" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.typeColor" size="small">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="单号 / 物料" min-width="160" show-overflow-tooltip />
        <el-table-column prop="priority" label="优先级" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.priorityColor" size="small">{{ row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止时间" width="110" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.statusColor" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(row.path)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="taskTable.length === 0" class="chart-empty" style="padding:24px 0">暂无重点任务</div>
    </div>

    <!-- ⑤ 快捷入口 -->
    <div class="pdb-shortcuts">
      <router-link v-for="s in shortcuts" :key="s.path" :to="s.path" class="sc-card">
        <span class="sc-card__icon">{{ s.icon }}</span>
        <span class="sc-card__label">{{ s.label }}</span>
        <span class="sc-card__arrow">›</span>
      </router-link>
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

const pendingReqs = computed(() =>
  requirements.value.filter(r => r.status === 'PENDING')
)

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
  if (pendingReqs.value.length === 0) {
    ElMessage.warning('当前无缺料物料，请先重新计算缺料')
    return
  }
  generateVisible.value = true
}

async function onGenerateOpen() {
  genForm.value = { supplierId: null, expectedArrivalDate: '', remark: '' }
  genSelection.value = [...pendingReqs.value]
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
  const totalO = orders.value.length
  const arrived = orders.value.filter(o => o.status === 'RECEIVED').length
  const overdue = orders.value.filter(o => {
    if (o.status === 'RECEIVED' || o.status === 'CANCELLED') return false
    return o.expectedArrivalDate && o.expectedArrivalDate < today
  }).length
  return {
    totalRequirements: requirements.value.length,
    pendingRequirements: pending,
    totalOrders: totalO,
    arrivedOrders: arrived,
    overdueOrders: overdue
  }
})

const kpiCards = computed(() => [
  {
    icon: '📑', label: '待处理订单', value: orderDemands.value.length, color: 'primary',
    hint: orderDemands.value.length > 0 ? '审核后已同步' : '暂无订单', hintColor: orderDemands.value.length > 0 ? 'neutral' : 'ok'
  },
  {
    icon: '📋', label: '缺料需求', value: stats.value.totalRequirements, color: 'default',
    hint: stats.value.totalRequirements > 0 ? '需跟进处理' : '无缺料', hintColor: stats.value.totalRequirements > 0 ? 'warn' : 'ok'
  },
  {
    icon: '⏳', label: '待采购', value: stats.value.pendingRequirements, color: 'warn',
    hint: stats.value.pendingRequirements > 0 ? '待生成采购单' : '全部已处理', hintColor: stats.value.pendingRequirements > 0 ? 'warn' : 'ok'
  },
  {
    icon: '📦', label: '采购订单', value: stats.value.totalOrders, color: 'primary',
    hint: '本期订单总数', hintColor: 'neutral'
  },
  {
    icon: '✅', label: '已到货', value: stats.value.arrivedOrders, color: 'success',
    hint: stats.value.totalOrders > 0 ? Math.round(stats.value.arrivedOrders / stats.value.totalOrders * 100) + '% 完成率' : '—', hintColor: 'ok'
  },
  {
    icon: '🚨', label: '逾期未到', value: stats.value.overdueOrders, color: 'danger',
    hint: stats.value.overdueOrders > 0 ? '需立即跟催' : '无逾期', hintColor: stats.value.overdueOrders > 0 ? 'danger' : 'ok'
  }
])

const topMaterials = computed(() =>
  [...requirements.value]
    .sort((a, b) => Number(b.shortageQuantity) - Number(a.shortageQuantity))
    .slice(0, 8)
)

const maxShortage = computed(() =>
  topMaterials.value.reduce((m, r) => Math.max(m, Number(r.shortageQuantity) || 0), 1)
)

function barWidth(val) {
  return Math.max(4, Math.round((Number(val) || 0) / maxShortage.value * 100))
}

const overdueOrders = computed(() =>
  orders.value.filter(o => {
    if (o.status === 'RECEIVED' || o.status === 'CANCELLED') return false
    return o.expectedArrivalDate && o.expectedArrivalDate < today
  })
)

const orderSummary = computed(() => {
  const total = orders.value.length || 1
  const groups = [
    { label: '草稿', key: 'DRAFT', color: '#8c8c8c' },
    { label: '已审核', key: 'APPROVED', color: '#4096ff' },
    { label: '已到货', key: 'RECEIVED', color: '#52c41a' },
    { label: '逾期未到', key: '_overdue', color: '#f5222d' }
  ]
  return groups.map(g => {
    const count = g.key === '_overdue'
      ? overdueOrders.value.length
      : orders.value.filter(o => o.status === g.key).length
    return { ...g, value: count, pct: Math.round(count / total * 100) }
  })
})

// 待办列表 — 结合真实接口数据动态生成
const todos = computed(() => {
  const list = []
  orderDemands.value
    .filter(o => Number(o.shortageMaterialCount) > 0)
    .slice(0, 3)
    .forEach(o => list.push({
      id: 'ord_' + o.orderId,
      level: 'danger',
      title: '订单缺料：' + o.orderNo,
      meta: `${o.productName} · 缺 ${o.shortageMaterialCount} 种物料，建议采购 ${fmtQty(o.suggestedPurchaseQty)}`,
      tag: '订单',
      path: '/purchase/demand'
    }))
  orderDemands.value
    .filter(o => o.auditStatus === 'PLAN_PENDING')
    .slice(0, 2)
    .forEach(o => list.push({
      id: 'new_' + o.orderId,
      level: 'info',
      title: '新审核订单：' + o.orderNo,
      meta: `${o.customerName} · ${o.orderQuantity} 台，需生产 ${o.needToProduce}`,
      tag: '新单',
      path: '/purchase/demand'
    }))
  requirements.value
    .filter(r => r.status === 'PENDING' && r.priority === 1)
    .slice(0, 3)
    .forEach(r => list.push({
      id: 'req_' + r.requirementId,
      level: 'danger',
      title: '紧急缺料：' + (r.materialName || r.materialCode),
      meta: '缺口 ' + r.shortageQuantity + ' 件',
      tag: '紧急',
      path: '/purchase/demand'
    }))
  overdueOrders.value.slice(0, 3).forEach(o => list.push({
    id: 'od_' + o.purchaseOrderId,
    level: 'warn',
    title: '逾期未到：' + o.purchaseOrderNo,
    meta: '期望到货 ' + (o.expectedArrivalDate || '—'),
    tag: '逾期',
    path: '/purchase/order'
  }))
  requirements.value
    .filter(r => r.status === 'PENDING' && r.priority === 2)
    .slice(0, 2)
    .forEach(r => list.push({
      id: 'req2_' + r.requirementId,
      level: 'info',
      title: '待采购：' + (r.materialName || r.materialCode),
      meta: '建议采购 ' + r.suggestedPurchaseQuantity + ' 件',
      tag: '高',
      path: '/purchase/demand'
    }))
  // mock: AI 单据待确认（TODO: 对接真实接口后替换）
  if (aiPending.value > 0) {
    list.push({ id: 'ai_pending', level: 'info', title: 'AI 单据待人工确认', meta: aiPending.value + ' 张待录入', tag: '待确认', path: '/purchase/ai-document' })
  }
  return list
})

// 今日重点任务表格 — 综合 todos 生成，方便后续直接替换成接口
const taskTable = computed(() => {
  const rows = []
  requirements.value.filter(r => r.status === 'PENDING').slice(0, 5).forEach(r => {
    rows.push({
      type: '缺料', typeColor: r.priority === 1 ? 'danger' : 'warning',
      title: (r.materialName || r.materialCode || '—') + (r.specification ? ' ' + r.specification : ''),
      priority: r.priority === 1 ? '紧急' : r.priority === 2 ? '高' : '普通',
      priorityColor: r.priority === 1 ? 'danger' : r.priority === 2 ? 'warning' : 'info',
      deadline: r.expectedArrivalDate || '—',
      status: '待采购', statusColor: 'warning',
      path: '/purchase/demand'
    })
  })
  overdueOrders.value.slice(0, 3).forEach(o => {
    rows.push({
      type: '到货', typeColor: 'danger',
      title: o.purchaseOrderNo + (o.supplierName ? ' / ' + o.supplierName : ''),
      priority: '紧急', priorityColor: 'danger',
      deadline: o.expectedArrivalDate || '—',
      status: '逾期', statusColor: 'danger',
      path: '/purchase/order'
    })
  })
  return rows
})

const shortcuts = [
  { icon: '🔍', label: '采购需求工作台', path: '/purchase/demand' },
  { icon: '📦', label: '采购订单', path: '/purchase/order' },
  { icon: '🏢', label: '供应商管理', path: '/purchase/supplier' },
  { icon: '🤖', label: 'AI 单据录入', path: '/purchase/ai-document' }
]

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
      fetchWorkbenchList({}),
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
        const retry = await fetchWorkbenchList({})
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
.pdb { padding: 16px 20px; background: #f0f2f5; min-height: 100%; box-sizing: border-box; }

.pdb-order-section {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 14px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
}

.pane-sub {
  font-size: 11px;
  color: #8c8c8c;
  margin-left: 8px;
}

.stock-ok { color: #52c41a; font-weight: 600; }

/* ---- 顶部 Hero ---- */
.pdb-hero { display: flex; align-items: center; justify-content: space-between; background: #fff; border-radius: 8px; padding: 16px 20px; margin-bottom: 14px; box-shadow: 0 1px 4px rgba(0,0,0,.06); flex-wrap: wrap; gap: 10px; }
.pdb-hero__title { font-size: 20px; font-weight: 700; color: #001b3f; line-height: 1.3; }
.pdb-hero__sub { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 6px; }
.hero-badge { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.hero-badge--warn { background: #fff7e6; color: #d46b08; border: 1px solid #ffd591; }
.hero-badge--danger { background: #fff1f0; color: #cf1322; border: 1px solid #ffa39e; }
.hero-badge--info { background: #e6f4ff; color: #0958d9; border: 1px solid #91caff; }
.hero-badge--ok { background: #f6ffed; color: #389e0d; border: 1px solid #b7eb8f; }
.pdb-hero__actions { display: flex; gap: 8px; flex-wrap: wrap; }
.hero-btn { display: inline-flex; align-items: center; padding: 6px 16px; border-radius: 6px; font-size: 13px; text-decoration: none; border: 1px solid #d9d9d9; background: #fafafa; color: #001b3f; transition: all .15s; white-space: nowrap; }
.hero-btn:hover { border-color: #4096ff; color: #4096ff; background: #e6f4ff; }
.hero-btn--primary { background: #4096ff; color: #fff; border-color: #4096ff; }
.hero-btn--primary:hover { background: #1677ff; border-color: #1677ff; color: #fff; }
.hero-btn--success { background: #52c41a; color: #fff; border-color: #52c41a; }
.hero-btn--success:hover:not(:disabled) { background: #389e0d; border-color: #389e0d; color: #fff; }
.hero-btn:disabled { opacity: .55; cursor: not-allowed; }

.gen-total { text-align: right; margin-top: 10px; font-size: 14px; color: #595959; }
.gen-total strong { color: #cf1322; font-size: 16px; }

/* ---- KPI 卡片 ---- */
.pdb-kpi-row { display: flex; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.kpi-card { flex: 1; min-width: 140px; background: #fff; border-radius: 8px; padding: 14px 16px; display: flex; align-items: flex-start; gap: 12px; box-shadow: 0 1px 4px rgba(0,0,0,.06); border-left: 3px solid #e8ecf0; }
.kpi-card--warn { border-left-color: #faad14; }
.kpi-card--primary { border-left-color: #4096ff; }
.kpi-card--success { border-left-color: #52c41a; }
.kpi-card--danger { border-left-color: #f5222d; }
.kpi-card__icon { font-size: 22px; line-height: 1; margin-top: 2px; }
.kpi-card__value { font-size: 26px; font-weight: 700; color: #001b3f; line-height: 1.2; }
.kpi-card__label { font-size: 12px; color: #8c8c8c; margin-top: 2px; }
.kpi-card__hint { font-size: 11px; margin-top: 4px; }
.hint--warn { color: #d46b08; }
.hint--danger { color: #cf1322; }
.hint--ok { color: #389e0d; }
.hint--neutral { color: #8c8c8c; }

/* ---- 核心任务三栏 ---- */
.pdb-main-row { display: flex; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.pane { background: #fff; border-radius: 8px; padding: 14px 16px; box-shadow: 0 1px 4px rgba(0,0,0,.06); overflow: hidden; }
.pane--task { flex: 1; min-width: 220px; }
.pane--chart { flex: 1.6; min-width: 260px; }
.pane--summary { flex: 1; min-width: 200px; }
.pane-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.pane-title { font-size: 13px; font-weight: 600; color: #001b3f; }
.pane-link { font-size: 12px; color: #4096ff; text-decoration: none; cursor: pointer; }
.pane-link:hover { text-decoration: underline; }

/* 待办列表 */
.task-list { display: flex; flex-direction: column; gap: 6px; }
.task-empty { font-size: 13px; color: #bfbfbf; text-align: center; padding: 20px 0; }
.task-item { display: flex; align-items: flex-start; gap: 8px; padding: 8px 10px; border-radius: 6px; background: #fafafa; border: 1px solid #f0f0f0; cursor: pointer; transition: background .15s; }
.task-item:hover { background: #f0f7ff; border-color: #91caff; }
.task-item--danger .task-item__dot { background: #f5222d; }
.task-item--warn .task-item__dot { background: #faad14; }
.task-item--info .task-item__dot { background: #4096ff; }
.task-item__dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 4px; flex-shrink: 0; }
.task-item__body { flex: 1; min-width: 0; }
.task-item__title { font-size: 13px; color: #001b3f; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.task-item__meta { font-size: 11px; color: #8c8c8c; margin-top: 2px; }

/* 排行榜 */
.rank-list { display: flex; flex-direction: column; gap: 8px; }
.chart-empty { font-size: 13px; color: #bfbfbf; text-align: center; padding: 20px 0; }
.rank-item { display: flex; align-items: center; gap: 8px; }
.rank-item__no { width: 20px; font-size: 12px; font-weight: 700; color: #8c8c8c; text-align: center; flex-shrink: 0; }
.rank-item__no--top { color: #f5222d; }
.rank-item__info { flex: 1; min-width: 0; }
.rank-item__name { font-size: 12px; color: #001b3f; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 3px; }
.rank-item__bar-wrap { height: 6px; background: #f0f0f0; border-radius: 3px; overflow: hidden; }
.rank-item__bar { height: 100%; border-radius: 3px; transition: width .4s; }
.bar--red { background: #f5222d; }
.bar--orange { background: #faad14; }
.bar--blue { background: #4096ff; }
.rank-item__val { font-size: 12px; font-weight: 600; color: #001b3f; width: 40px; text-align: right; flex-shrink: 0; }

/* 摘要条 */
.summary-list { display: flex; flex-direction: column; gap: 8px; }
.summary-item { display: flex; align-items: center; gap: 8px; }
.summary-item__label { font-size: 12px; color: #595959; width: 56px; flex-shrink: 0; }
.summary-item__bar-wrap { flex: 1; height: 6px; background: #f0f0f0; border-radius: 3px; overflow: hidden; }
.summary-item__bar { height: 100%; border-radius: 3px; transition: width .4s; min-width: 2px; }
.summary-item__val { font-size: 12px; font-weight: 600; width: 24px; text-align: right; flex-shrink: 0; }

/* 风险列表 */
.risk-list { display: flex; flex-direction: column; gap: 5px; }
.risk-item { display: flex; align-items: center; gap: 8px; padding: 6px 8px; background: #fff1f0; border-radius: 4px; cursor: pointer; }
.risk-item:hover { background: #ffd6d6; }
.risk-item__no { font-size: 12px; font-weight: 600; color: #001b3f; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.risk-item__supplier { font-size: 11px; color: #8c8c8c; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* ---- 今日任务表格 ---- */
.pdb-table-section { background: #fff; border-radius: 8px; padding: 14px 16px; margin-bottom: 14px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }

/* ---- 快捷入口 ---- */
.pdb-shortcuts { display: flex; gap: 10px; flex-wrap: wrap; }
.sc-card { flex: 1; min-width: 140px; display: flex; align-items: center; gap: 10px; background: #fff; border-radius: 8px; padding: 12px 16px; text-decoration: none; color: #001b3f; box-shadow: 0 1px 4px rgba(0,0,0,.06); border: 1px solid transparent; transition: all .15s; }
.sc-card:hover { border-color: #4096ff; background: #e6f4ff; color: #0958d9; }
.sc-card__icon { font-size: 20px; flex-shrink: 0; }
.sc-card__label { font-size: 13px; font-weight: 500; flex: 1; }
.sc-card__arrow { font-size: 16px; color: #bfbfbf; }
.sc-card:hover .sc-card__arrow { color: #4096ff; }
</style>
