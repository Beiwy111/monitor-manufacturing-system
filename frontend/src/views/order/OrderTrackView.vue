<template>
  <div class="order-track-page">
    <div class="order-track-panel">
      <div class="order-track-header">
        <h2 class="order-track-header__title">订单跟踪</h2>
        <div v-if="showPlannerActions" class="order-track-header__actions">
            <span v-if="selectedRows.length > 1" class="order-track-header__hint">
              已选 {{ selectedRows.length }} 个同型号订单 · {{ lockedModel }}
            </span>
            <el-button
              type="primary"
              size="small"
              :disabled="!scheduleTargets.length"
              @click="handleSmartSchedule"
            >
              智能排产{{ selectedRows.length > 1 ? `（${selectedRows.length}单）` : '' }}
            </el-button>
            <el-button size="small" :disabled="!sel" @click="goGantt">查看甘特图</el-button>
        </div>
      </div>

      <div class="order-track-top">
        <div class="order-track-query">
          <el-form :inline="true" size="small" @submit.prevent="applyFilter">
            <el-form-item label="关键字">
              <el-input
                v-model="keyword"
                clearable
                placeholder="订单号/客户/型号"
                style="width: 180px"
                @clear="applyFilter"
              />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="statusFilter" clearable placeholder="全部" style="width: 120px" @change="applyFilter">
                <el-option v-for="s in ORDER_STATUS" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="applyFilter">查询</el-button>
              <el-button @click="resetFilter">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="order-track-table-wrap">
          <el-table
            ref="tableRef"
            v-loading="tableLoading"
            :data="filteredOrders"
            border
            stripe
            height="100%"
            highlight-current-row
            size="small"
            @row-click="onRowClick"
            @current-change="onCurrentChange"
            @selection-change="onSelectionChange"
          >
            <el-table-column
              v-if="showPlannerActions"
              type="selection"
              width="42"
              fixed="left"
              :selectable="rowSelectable"
            />
            <el-table-column prop="id" label="订单号" width="130" fixed="left" />
            <el-table-column prop="customerName" label="客户" min-width="110" show-overflow-tooltip />
            <el-table-column prop="productModel" label="型号" min-width="120" show-overflow-tooltip />
            <el-table-column prop="quantity" label="订单数量" width="88" align="right">
              <template #default="{ row }">{{ row.quantity ?? '—' }}</template>
            </el-table-column>
            <el-table-column prop="deliveryDate" label="要求交期" width="100" />
            <el-table-column label="剩余天数" width="88" align="center">
              <template #default="{ row }">
                <span :class="remainClass(row)">{{ formatRemainDays(row) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="优先级" width="72" align="center">
              <template #default="{ row }">
                <span class="ot-tag" :class="priorityClass(row)">{{ priorityLabel(row) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="物料齐套率" width="96" align="center">
              <template #default="{ row }">
                <span v-if="metricOf(row.id).kitRate != null">{{ metricOf(row.id).kitRate }}%</span>
                <span v-else class="ot-muted">—</span>
              </template>
            </el-table-column>
            <el-table-column label="排产风险" width="88" align="center">
              <template #default="{ row }">
                <span class="ot-tag" :class="riskClass(metricOf(row.id).risk)">{{ metricOf(row.id).risk || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="计划状态" width="88" align="center">
              <template #default="{ row }">
                <span class="ot-tag" :class="planStatusClass(row)">{{ planStatusLabel(row) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="订单状态" width="88" align="center">
              <template #default="{ row }">
                <span class="ot-tag ot-tag--neutral">{{ row.status }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="detailVisible"
      class="order-track-dialog"
      width="920px"
      top="6vh"
      destroy-on-close
      :title="detailDialogTitle"
      @closed="onDetailClosed"
    >
      <div v-if="sel" v-loading="ctxLoading" class="order-track-dialog__body">
        <el-tabs v-model="detailTab" class="order-track-tabs">
          <el-tab-pane label="基本信息" name="basic">
            <div class="order-track-pane">
              <el-table :data="basicRows" border stripe size="small" class="ot-compact-table">
                <el-table-column prop="label" label="字段" width="120" />
                <el-table-column prop="value" label="内容" min-width="200" show-overflow-tooltip />
              </el-table>
            </div>
          </el-tab-pane>

          <el-tab-pane label="物料齐套" name="material">
            <div class="order-track-pane">
              <el-table :data="materialRows" border stripe size="small" class="ot-compact-table" empty-text="暂无物料数据">
                <el-table-column prop="materialCode" label="物料编码" width="110" />
                <el-table-column prop="materialName" label="物料名称" min-width="140" show-overflow-tooltip />
                <el-table-column prop="requiredQty" label="需求" width="72" align="right" />
                <el-table-column prop="availableQty" label="库存" width="72" align="right" />
                <el-table-column prop="gapQty" label="缺口" width="72" align="right" />
                <el-table-column label="齐套" width="72" align="center">
                  <template #default="{ row }">
                    <span class="ot-tag" :class="row.status === '齐套' ? 'ot-tag--ok' : 'ot-tag--warn'">{{ row.status }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <el-tab-pane label="设备产能" name="capacity">
            <div class="order-track-pane">
              <div class="ot-section-title">设备资源</div>
              <el-table :data="equipmentRows" border stripe size="small" class="ot-compact-table" empty-text="暂无设备数据">
                <el-table-column prop="code" label="设备编号" width="100" />
                <el-table-column prop="name" label="设备名称" min-width="130" show-overflow-tooltip />
                <el-table-column prop="type" label="类型" width="100" />
                <el-table-column prop="line" label="产线/区域" width="100" />
                <el-table-column prop="status" label="状态" width="80" align="center">
                  <template #default="{ row }">
                    <span class="ot-tag" :class="equipStatusClass(row.status)">{{ row.status }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="risk" label="产能风险" min-width="120" show-overflow-tooltip />
              </el-table>
              <div class="ot-section-title">人员配置</div>
              <el-table :data="personnelRows" border stripe size="small" class="ot-compact-table">
                <el-table-column prop="role" label="岗位" width="120" />
                <el-table-column prop="available" label="在岗人数" width="90" align="right" />
                <el-table-column prop="capacity" label="人员产能上限" width="110" align="right" />
                <el-table-column prop="status" label="状态" width="80" align="center">
                  <template #default="{ row }">
                    <span class="ot-tag" :class="row.status === '充足' ? 'ot-tag--ok' : 'ot-tag--danger'">{{ row.status }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="remark" label="说明" min-width="160" show-overflow-tooltip />
              </el-table>
            </div>
          </el-tab-pane>

          <el-tab-pane label="工艺路线" name="route">
            <div class="order-track-pane">
              <el-table :data="routeRows" border stripe size="small" class="ot-compact-table" empty-text="未配置工艺路线">
                <el-table-column prop="stepNo" label="序号" width="56" align="center" />
                <el-table-column prop="stepCode" label="工序编码" width="100" />
                <el-table-column prop="stepName" label="工序名称" min-width="120" />
                <el-table-column prop="standardEquipmentType" label="建议设备" width="110" />
                <el-table-column prop="standardWorkHours" label="标准工时" width="88" align="right" />
                <el-table-column prop="qualityRequiredText" label="需质检" width="72" align="center" />
              </el-table>
            </div>
          </el-tab-pane>

          <el-tab-pane label="流程跟踪" name="flow">
            <div class="order-track-pane">
              <div class="track-steps">
                <div
                  v-for="(step, i) in timeline"
                  :key="i"
                  class="track-step"
                  :class="`track-step--${step.status}`"
                >
                  <div class="track-step__indicator">
                    <span class="track-step__dot" />
                    <span v-if="i < timeline.length - 1" class="track-step__connector" />
                  </div>
                  <div class="track-step__content">
                    <div class="track-step__title">{{ step.title }}</div>
                    <div class="track-step__desc">{{ step.desc }}</div>
                  </div>
                </div>
              </div>
              <el-table :data="flowStatsRows" border stripe size="small" class="ot-compact-table ot-flow-stats">
                <el-table-column prop="label" label="环节" width="120" />
                <el-table-column prop="count" label="记录数" width="80" align="right" />
                <el-table-column prop="status" label="当前状态" min-width="120" />
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { ORDER_STATUS } from '@/mock/constants'
import { fetchOrderPlanningContext } from '@/api/planner'
import { navigateToSmartScheduling } from '@/composables/usePlannerAgent'

const router = useRouter()
const route = useRoute()
const mes = useMesStore()
const userStore = useUserStore()

const showPlannerActions = computed(() => ['planner', 'admin'].includes(userStore.roleKey))

const keyword = ref('')
const statusFilter = ref('')
const appliedKeyword = ref('')
const appliedStatus = ref('')
const sel = ref(null)
const tableRef = ref(null)
const selectedRows = ref([])
const lockedModel = ref('')
const detailTab = ref('basic')
const detailVisible = ref(false)
const tableLoading = ref(false)
const ctxLoading = ref(false)
const planningCtx = ref(null)
const metricsMap = reactive({})

const chain = computed(() => (sel.value ? mes.traceChain(sel.value.id) : null))

const detailDialogTitle = computed(() => {
  if (!sel.value) return '订单详情'
  return `订单详情 · ${sel.value.id} · ${sel.value.productModel || '—'} · ${sel.value.customerName || '—'}`
})

const scheduleTargets = computed(() => {
  if (selectedRows.value.length) return selectedRows.value
  if (sel.value && canSchedule(sel.value)) return [sel.value]
  return []
})

const filteredOrders = computed(() => {
  const kw = appliedKeyword.value.trim().toLowerCase()
  return mes.orders.filter((o) => {
    if (appliedStatus.value && o.status !== appliedStatus.value) return false
    if (!kw) return true
    const hay = `${o.id} ${o.customerName || ''} ${o.productModel || ''}`.toLowerCase()
    return hay.includes(kw)
  })
})

const basicRows = computed(() => {
  if (!sel.value) return []
  const o = sel.value
  const plan = mes.plans.find((p) => p.orderId === o.id)
  const ctx = planningCtx.value
  return [
    { label: '订单号', value: o.id },
    { label: '客户', value: o.customerName || '—' },
    { label: '产品型号', value: o.productModel || '—' },
    { label: '面板类型', value: o.panelType || '—' },
    { label: '订单数量', value: o.quantity != null ? `${o.quantity} 台` : '—' },
    { label: '要求交期', value: o.deliveryDate || ctx?.deliveryDate || '—' },
    { label: '订单金额', value: o.amount != null ? `¥${Number(o.amount).toLocaleString()}` : (ctx?.amount != null ? `¥${Number(ctx.amount).toLocaleString()}` : '—') },
    { label: '销售人员', value: o.salesPerson || '—' },
    { label: '订单状态', value: o.status },
    { label: '计划编号', value: o.planId || plan?.id || '—' },
    { label: '工单编号', value: o.workOrderId || '—' },
    { label: '计划状态', value: planStatusLabel(o) },
    { label: '备注', value: o.remark || '—' }
  ]
})

const materialRows = computed(() => {
  const ctx = planningCtx.value
  if (!ctx) return []
  const checks = ctx.inventory?.materialChecks
  if (Array.isArray(checks) && checks.length) {
    return checks.map((m) => ({
      materialCode: m.materialCode || '—',
      materialName: m.materialName || '—',
      requiredQty: m.requiredQty ?? '—',
      availableQty: m.availableQty ?? '—',
      gapQty: m.shortage ?? 0,
      status: m.sufficient !== false && !(intVal(m.shortage) > 0) ? '齐套' : '缺料'
    }))
  }
  return (ctx.materialGaps || []).map((g) => ({
    materialCode: g.materialCode || '—',
    materialName: g.materialName || '—',
    requiredQty: g.requiredQty ?? '—',
    availableQty: g.availableQty ?? '—',
    gapQty: g.gapQty ?? g.shortage ?? '—',
    status: '缺料'
  }))
})

const routeRows = computed(() => planningCtx.value?.processRoute || [])

const equipmentRows = computed(() => {
  const ctx = planningCtx.value
  const routeTypes = new Set((ctx?.processRoute || []).map((s) => s.standardEquipmentType).filter(Boolean))
  const riskDetail = (ctx?.capacityRisks || []).map((r) => r.detail).join('；')
  const list = mes.equipment.filter((e) => !routeTypes.size || routeTypes.has(e.type))
  return list.map((e) => ({
    code: e.id,
    name: e.name,
    type: e.type,
    line: e.line || '—',
    status: e.status || '—',
    risk: e.status === '故障' ? '设备故障，影响产能' : (riskDetail || '—')
  }))
})

const personnelRows = computed(() => {
  const ctx = planningCtx.value
  const operators = mes.sysUsers.filter((u) => u.roleKey === 'operator' && u.status === '启用')
  const orderQty = intVal(ctx?.orderQuantity ?? sel.value?.quantity)
  const recQty = intVal(ctx?.recommendedPlanQty)
  let status = '充足'
  let remark = '人员可满足当前排产需求'
  if (!operators.length) {
    status = '不足'
    remark = '无在岗操作员'
  } else if (recQty > 0 && recQty < orderQty) {
    status = '偏紧'
    remark = `建议排产量 ${recQty} 台，低于订单 ${orderQty} 台`
  } else if (operators.length <= 2) {
    status = '偏紧'
    remark = `在岗操作员仅 ${operators.length} 人`
  }
  return [{
    role: '生产操作员',
    available: operators.length,
    capacity: recQty > 0 ? `${recQty} 台` : '—',
    status,
    remark
  }]
})

const flowStatsRows = computed(() => {
  const c = chain.value
  if (!c) return []
  const issueDone = c.issueTasks?.length ? c.issueTasks.every((t) => t.status === '已完成') : false
  const inboundDone = c.inboundTasks?.length ? c.inboundTasks.every((t) => t.status === '已入库') : false
  return [
    { label: '领料任务', count: c.issueTasks?.length || 0, status: issueDone ? '已完成' : (c.issueTasks?.length ? '进行中' : '—') },
    { label: '派工记录', count: c.dispatches?.length || 0, status: c.dispatches?.length ? '已派工' : '未派工' },
    { label: '质检记录', count: c.inspections?.length || 0, status: c.inspections?.length ? '有记录' : '未送检' },
    { label: '入库任务', count: c.inboundTasks?.length || 0, status: inboundDone ? '已入库' : (c.inboundTasks?.length ? '待入库' : '—') },
    { label: '发货出库', count: c.deliveries?.length || 0, status: c.deliveries?.[0]?.status || '未发货' }
  ]
})

const timeline = computed(() => {
  if (!chain.value) return []
  const c = chain.value
  const issueDone = c.issueTasks?.length ? c.issueTasks.every((t) => t.status === '已完成') : false
  const inboundDone = c.inboundTasks?.length ? c.inboundTasks.every((t) => t.status === '已入库') : false
  const deliveryStatus = c.deliveries?.[0]?.status || '未发货'
  const step = (value, fallback = '未开始') => value || fallback
  return [
    { title: '客户订单', desc: step(c.order?.status), status: c.order ? 'done' : 'pending' },
    { title: '生产计划', desc: step(c.plan?.status, '未创建'), status: c.plan ? 'done' : 'pending' },
    { title: '生产工单', desc: step(c.wo?.status, '未创建'), status: c.wo ? 'done' : 'pending' },
    { title: '生产领料', desc: c.issueTasks?.length ? `${c.issueTasks.length} 项` : '待下达', status: issueDone ? 'done' : (c.issueTasks?.length ? 'active' : 'pending') },
    { title: '派工报工', desc: c.dispatches?.length ? `${c.dispatches.length} 条` : '未派工', status: c.dispatches?.length ? 'done' : 'pending' },
    { title: '质量检验', desc: c.inspections?.length ? `${c.inspections.length} 条` : '未送检', status: c.inspections?.length ? 'done' : 'pending' },
    { title: '成品入库', desc: c.inboundTasks?.length ? `${c.inboundTasks.length} 项` : '待质检', status: inboundDone ? 'done' : (c.inboundTasks?.length ? 'active' : 'pending') },
    { title: '发货出库', desc: deliveryStatus, status: deliveryStatus === '已出库' ? 'done' : (deliveryStatus === '待出库' ? 'active' : 'pending') }
  ]
})

function intVal(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

function remainDays(order) {
  const d = order?.deliveryDate
  if (!d) return null
  const end = new Date(d)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  end.setHours(0, 0, 0, 0)
  return Math.ceil((end - today) / 86400000)
}

function formatRemainDays(order) {
  const d = remainDays(order)
  if (d == null) return '—'
  if (d < 0) return `逾期${Math.abs(d)}天`
  return `${d}天`
}

function remainClass(order) {
  const d = remainDays(order)
  if (d == null) return ''
  if (d < 0) return 'ot-danger-text'
  if (d <= 7) return 'ot-warn-text'
  return ''
}

function priorityLabel(order) {
  const d = remainDays(order)
  if (d == null) return '—'
  if (d <= 0) return '紧急'
  if (d <= 7) return '高'
  if (d <= 14) return '中'
  return '低'
}

function priorityClass(order) {
  const p = priorityLabel(order)
  if (p === '紧急') return 'ot-tag--danger'
  if (p === '高') return 'ot-tag--warn'
  if (p === '中') return 'ot-tag--info'
  return 'ot-tag--neutral'
}

function planStatusLabel(order) {
  const plan = mes.plans.find((p) => p.orderId === order.id || p.id === order.planId)
  if (plan) return plan.status
  if (['待计划', '已审核'].includes(order.status) && !order.planId) return '待编制'
  if (order.planId) return '已关联'
  return '—'
}

function planStatusClass(order) {
  const s = planStatusLabel(order)
  if (['执行中', '已发布'].includes(s)) return 'ot-tag--ok'
  if (['待编制', '草稿', '待提交'].includes(s)) return 'ot-tag--warn'
  if (['已完成'].includes(s)) return 'ot-tag--neutral'
  return 'ot-tag--info'
}

function calcKitRate(ctx) {
  if (!ctx) return null
  const checks = ctx.inventory?.materialChecks
  if (Array.isArray(checks) && checks.length) {
    const ok = checks.filter((m) => m.sufficient !== false && intVal(m.shortage) <= 0).length
    return Math.round((ok / checks.length) * 100)
  }
  const gaps = ctx.materialGaps || []
  if (!gaps.length) return 100
  return Math.max(0, 100 - gaps.length * 12)
}

function calcRisk(ctx, order) {
  if (!ctx && !order) return '—'
  const dangers = []
  if (!ctx?.processRoute?.length) dangers.push('route')
  const gaps = ctx?.materialGaps?.length || 0
  if (gaps >= 3) dangers.push('material')
  const faultEq = mes.equipment.some((e) => e.status === '故障')
  if (faultEq) dangers.push('equip')
  const remain = remainDays(order)
  if (remain != null && remain <= 3) dangers.push('delivery')
  if (dangers.length >= 2 || !ctx?.processRoute?.length) return '高'
  if (gaps > 0 || faultEq || (remain != null && remain <= 7)) return '中'
  return '低'
}

function riskClass(risk) {
  if (risk === '高') return 'ot-tag--danger'
  if (risk === '中') return 'ot-tag--warn'
  if (risk === '低') return 'ot-tag--ok'
  return 'ot-tag--neutral'
}

function equipStatusClass(status) {
  if (status === '运行中') return 'ot-tag--ok'
  if (status === '故障' || status === '停机') return 'ot-tag--danger'
  return 'ot-tag--neutral'
}

function metricOf(orderId) {
  return metricsMap[orderId] || { kitRate: null, risk: '—' }
}

function updateMetrics(orderId, ctx, order) {
  metricsMap[orderId] = {
    kitRate: calcKitRate(ctx),
    risk: calcRisk(ctx, order)
  }
}

async function loadContext(orderId, { silent = false } = {}) {
  const order = mes.orders.find((o) => o.id === orderId)
  if (!order) return null
  try {
    const ctx = await fetchOrderPlanningContext(orderId, { silent: silent || !canSchedule(order) })
    updateMetrics(orderId, ctx, order)
    return ctx
  } catch {
    updateMetrics(orderId, null, order)
    return null
  }
}

async function prefetchMetrics() {
  const orders = mes.orders.filter(canSchedule)
  await Promise.allSettled(orders.map(async (o) => {
    if (metricsMap[o.id]) return
    await loadContext(o.id, { silent: true })
  }))
}

async function openOrderDetail(row) {
  if (!row) return
  sel.value = row
  detailTab.value = 'basic'
  detailVisible.value = true
  ctxLoading.value = true
  try {
    planningCtx.value = await loadContext(row.id)
  } finally {
    ctxLoading.value = false
  }
}

function onRowClick(row) {
  openOrderDetail(row)
}

function onCurrentChange(row) {
  sel.value = row
  if (!row) {
    planningCtx.value = null
  }
}

function onDetailClosed() {
  planningCtx.value = null
}

function applyFilter() {
  appliedKeyword.value = keyword.value
  appliedStatus.value = statusFilter.value
}

function resetFilter() {
  keyword.value = ''
  statusFilter.value = ''
  appliedKeyword.value = ''
  appliedStatus.value = ''
}

function canSchedule(order) {
  if (!order) return false
  return ['待计划', '已审核'].includes(order.status) && !order.planId
}

function rowSelectable(row) {
  if (!canSchedule(row)) return false
  if (!lockedModel.value) return true
  return row.productModel === lockedModel.value
}

function onSelectionChange(rows) {
  const schedulable = rows.filter(canSchedule)
  if (!schedulable.length) {
    selectedRows.value = []
    lockedModel.value = ''
    return
  }

  const byModel = {}
  schedulable.forEach((r) => {
    const m = r.productModel || ''
    if (!byModel[m]) byModel[m] = []
    byModel[m].push(r)
  })
  const groups = Object.values(byModel)
  if (groups.length > 1) {
    const largest = groups.sort((a, b) => b.length - a.length)[0]
    ElMessage.warning(`联合排产须同型号，已保留「${largest[0].productModel}」的 ${largest.length} 个订单`)
    nextTick(() => {
      tableRef.value?.clearSelection()
      largest.forEach((r) => tableRef.value?.toggleRowSelection(r, true))
    })
    selectedRows.value = largest
    lockedModel.value = largest[0].productModel || ''
    return
  }

  selectedRows.value = schedulable
  lockedModel.value = schedulable[0].productModel || ''
}

function onPlannerSuccess() {
  selectedRows.value = []
  lockedModel.value = ''
  tableRef.value?.clearSelection()
  prefetchMetrics()
}

async function validateBeforeSchedule(order) {
  ctxLoading.value = true
  let ctx = planningCtx.value
  if (!ctx || ctx.orderId !== order.id) {
    ctx = await loadContext(order.id)
    planningCtx.value = ctx
  }
  ctxLoading.value = false

  const issues = []

  if (!ctx?.processRoute?.length) {
    issues.push({ level: 'danger', text: '未配置工艺路线，无法开展排产' })
  }

  const gaps = ctx?.materialGaps || []
  if (gaps.length >= 3) {
    const names = gaps.slice(0, 3).map((g) => g.materialName).join('、')
    issues.push({ level: 'danger', text: `物料严重缺料（${gaps.length} 项）：${names}` })
  } else if (gaps.some((g) => intVal(g.gapQty ?? g.shortage) >= intVal(g.requiredQty) * 0.5)) {
    issues.push({ level: 'danger', text: '关键物料缺口超过需求 50%，暂不建议排产' })
  }

  const routeTypes = new Set((ctx?.processRoute || []).map((s) => s.standardEquipmentType).filter(Boolean))
  const faultEquip = mes.equipment.filter((e) => {
    if (e.status !== '故障' && e.status !== '停机') return false
    return !routeTypes.size || routeTypes.has(e.type)
  })
  if (faultEquip.length) {
    issues.push({ level: 'danger', text: `关键设备故障：${faultEquip.map((e) => e.name).join('、')}` })
  }

  const operators = mes.sysUsers.filter((u) => u.roleKey === 'operator' && u.status === '启用')
  const orderQty = intVal(ctx?.orderQuantity ?? order.quantity)
  const recQty = intVal(ctx?.recommendedPlanQty)
  if (!operators.length) {
    issues.push({ level: 'danger', text: '无在岗操作员，无法排产' })
  } else if (recQty > 0 && recQty < orderQty * 0.6) {
    issues.push({ level: 'danger', text: `人员/产能不足：建议排产量 ${recQty} 台，订单需求 ${orderQty} 台` })
  }

  ;(ctx?.capacityRisks || []).forEach((r) => {
    if (r.level === 'danger') {
      issues.push({ level: 'danger', text: `${r.label}：${r.detail}` })
    }
  })

  const blocked = issues.filter((i) => i.level === 'danger')
  return { issues, blocked: blocked.length > 0, ctx }
}

async function handleSmartSchedule() {
  const targets = scheduleTargets.value
  if (!targets.length) {
    ElMessage.warning('请勾选待排产订单（相同型号可联合排产）')
    return
  }

  for (const order of targets) {
    if (!canSchedule(order)) {
      await ElMessageBox.alert('所选订单中存在已关联计划或状态不允许排产的订单。', '无法排产', { type: 'warning' })
      return
    }
  }

  if (targets.length > 1) {
    const model = targets[0].productModel
    if (!targets.every((t) => t.productModel === model)) {
      ElMessage.warning('联合排产须选择相同型号的订单')
      return
    }
  }

  const { issues, blocked } = await validateBeforeSchedule(targets[0])
  if (blocked) {
    const html = issues.map((i) => `• ${i.text}`).join('<br/>')
    await ElMessageBox.alert(`<div style="line-height:1.6">${html}</div>`, '排产前检查未通过', {
      type: 'error',
      dangerouslyUseHTMLString: true
    })
    return
  }

  if (issues.length) {
    const html = issues.map((i) => `• ${i.text}`).join('<br/>')
    try {
      await ElMessageBox.confirm(`<div style="line-height:1.6">${html}</div><br/>是否仍继续进入智能排产？`, '排产风险提示', {
        type: 'warning',
        dangerouslyUseHTMLString: true,
        confirmButtonText: '继续排产',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
  }

  navigateToSmartScheduling(router, {
    orderIds: targets.map((t) => t.id),
    combined: targets.length > 1,
    from: route.fullPath
  })
}

function goGantt() {
  if (!sel.value) return
  router.push({ path: '/production/plan', query: { orderId: sel.value.id, tab: 'gantt' } })
}

watch(() => mes.orders.length, () => prefetchMetrics())

onMounted(async () => {
  tableLoading.value = true
  try {
    if (!mes.orders.length) {
      await mes.hydrateForPage()
    }
    await prefetchMetrics()
  } finally {
    tableLoading.value = false
  }
})
</script>

<style scoped>
.order-track-page {
  margin: -12px;
  min-height: var(--layout-viewport-h, calc(100vh - 52px));
  background: #f0f2f5;
  padding: 12px;
  font-size: 14px;
  font-weight: 400;
  color: #374151;
}

.order-track-panel {
  height: var(--layout-content-min-h, calc(100vh - 92px));
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.order-track-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 44px;
  padding: 0 16px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.order-track-header__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-track-header__hint {
  font-size: 12px;
  color: #409eff;
  margin-right: 4px;
}

.order-track-header__title {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #111827;
}

.order-track-header__actions {
  display: flex;
  gap: 8px;
}

.order-track-top {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.order-track-query {
  padding: 8px 12px 0;
  flex-shrink: 0;
}

.order-track-query :deep(.el-form-item) {
  margin-bottom: 8px;
}

.order-track-table-wrap {
  flex: 1;
  min-height: 0;
  padding: 0 12px 8px;
}

.order-track-table-wrap :deep(.el-table th.el-table__cell) {
  background: #f9fafb;
  color: #4b5563;
  font-weight: 500;
}

.order-track-table-wrap :deep(.el-table__row) {
  cursor: pointer;
}

.order-track-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}

.order-track-dialog__body {
  min-height: 420px;
  max-height: calc(84vh - 120px);
}

.order-track-tabs {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 400px;
}

.order-track-tabs :deep(.el-tabs__header) {
  margin: 0 0 8px;
}

.order-track-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.order-track-tabs :deep(.el-tab-pane) {
  max-height: calc(84vh - 200px);
}

.order-track-pane {
  max-height: calc(84vh - 200px);
  overflow: auto;
}

.ot-compact-table :deep(.el-table__cell) {
  padding: 6px 0;
}

.ot-section-title {
  margin: 8px 0 6px;
  font-size: 13px;
  font-weight: 500;
  color: #4b5563;
}

.ot-section-title:first-child {
  margin-top: 0;
}

.ot-muted {
  color: #9ca3af;
}

.ot-danger-text {
  color: #b91c1c;
}

.ot-warn-text {
  color: #b45309;
}

.ot-tag {
  display: inline-block;
  padding: 1px 6px;
  font-size: 12px;
  line-height: 18px;
  border-radius: 2px;
  border: 1px solid transparent;
}

.ot-tag--ok {
  color: #166534;
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.ot-tag--warn {
  color: #92400e;
  background: #fffbeb;
  border-color: #fde68a;
}

.ot-tag--danger {
  color: #991b1b;
  background: #fef2f2;
  border-color: #fecaca;
}

.ot-tag--info {
  color: #1e40af;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.ot-tag--neutral {
  color: #4b5563;
  background: #f9fafb;
  border-color: #e5e7eb;
}

.track-steps {
  display: flex;
  align-items: flex-start;
  gap: 0;
  padding: 8px 4px 12px;
  overflow-x: auto;
}

.track-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  min-width: 88px;
  max-width: 140px;
}

.track-step__indicator {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  justify-content: center;
  height: 20px;
}

.track-step__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d1d5db;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px #d1d5db;
  z-index: 1;
  flex-shrink: 0;
}

.track-step--done .track-step__dot {
  background: #6b7280;
  box-shadow: 0 0 0 1px #6b7280;
}

.track-step--active .track-step__dot {
  background: #2563eb;
  box-shadow: 0 0 0 1px #2563eb;
}

.track-step__connector {
  position: absolute;
  left: calc(50% + 6px);
  right: calc(-50% + 6px);
  top: 50%;
  height: 1px;
  background: #e5e7eb;
  transform: translateY(-50%);
}

.track-step--done .track-step__connector {
  background: #9ca3af;
}

.track-step__content {
  margin-top: 6px;
  text-align: center;
  width: 100%;
}

.track-step__title {
  font-size: 12px;
  font-weight: 500;
  color: #374151;
  line-height: 1.3;
}

.track-step__desc {
  margin-top: 2px;
  font-size: 11px;
  color: #9ca3af;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ot-flow-stats {
  margin-top: 8px;
}
</style>
