<template>
  <div class="wb" :class="{ 'wb--fs': isFullscreen }">
    <div class="wb__panel">
      <div class="wb__head">
        <h2 class="wb__title">生产计划工作台</h2>
        <span class="wb__meta">
          待排 <em>{{ pendingOrders.length }}</em>
          · 计划 <em>{{ mes.plans.length }}</em>
          · 待提交 <em>{{ pendingSubmit.length }}</em>
          · 执行中 <em>{{ executingCount }}</em>
        </span>
        <div class="wb__head-actions">
          <el-button size="small" type="success" plain @click="openSmart()">智能排产</el-button>
          <el-button size="small" plain @click="openManual()">手动排产</el-button>
          <el-button size="small" plain @click="refresh">刷新</el-button>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="wb-tabs" @tab-change="onTabChange">
        <!-- 待排订单 -->
        <el-tab-pane label="待排订单" name="pending">
          <div class="wb-pane">
            <div class="wb-toolbar">
              <el-input v-model="pendingKw" clearable placeholder="订单号/型号" class="wb-toolbar__field wb-toolbar__field--wide" />
              <span class="wb-toolbar__hint">共 {{ filteredPending.length }} 笔待排订单</span>
            </div>
            <div class="wb-table-wrap">
              <el-table :data="filteredPending" border size="small" height="100%" class="wb-table" highlight-current-row @row-click="onPendingRowClick">
                <el-table-column prop="id" label="订单编号" min-width="130" />
                <el-table-column prop="productModel" label="产品型号" min-width="140" show-overflow-tooltip />
                <el-table-column prop="quantity" label="数量" min-width="72" align="right" />
                <el-table-column prop="deliveryDate" label="客户交期" min-width="108" />
                <el-table-column label="齐套率" min-width="80" align="center">
                  <template #default="{ row }">{{ kitRateLabel(row.id) }}</template>
                </el-table-column>
                <el-table-column prop="status" label="状态" min-width="88" />
                <el-table-column label="操作" width="160" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" size="small" @click.stop="openSmart(row.id)">智能排产</el-button>
                    <el-button link type="primary" size="small" @click.stop="openManual(row.id)">手动排产</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>

        <!-- 生产计划 -->
        <el-tab-pane label="生产计划" name="plans">
          <div class="wb-pane wb-pane--split">
            <div class="wb-toolbar">
              <el-input v-model="filters.orderNo" clearable placeholder="订单号" class="wb-toolbar__field" />
              <el-input v-model="filters.productModel" clearable placeholder="型号" class="wb-toolbar__field" />
              <el-select v-model="filters.status" clearable placeholder="状态" class="wb-toolbar__field">
                <el-option v-for="s in PLAN_STATUS" :key="s" :label="s" :value="s" />
              </el-select>
              <el-date-picker v-model="filters.dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" class="wb-toolbar__range" />
              <el-button type="primary" size="small" @click="applyQuery">查询</el-button>
              <el-button size="small" @click="resetQuery">重置</el-button>
              <el-button size="small" @click="exportExcel">导出</el-button>
            </div>

            <div class="wb-split">
              <div class="wb-split__top">
                <el-table
                  ref="tableRef"
                  v-loading="tableLoading"
                  :data="pagedRows"
                  border
                  size="small"
                  height="100%"
                  row-key="id"
                  class="wb-table"
                  highlight-current-row
                  :current-row-key="selectedPlan?.id"
                  :default-sort="{ prop: 'planStart', order: 'descending' }"
                  @sort-change="onSortChange"
                  @current-change="onPlanRowSelect"
                >
                  <el-table-column prop="id" label="计划编号" min-width="120" sortable="custom" fixed="left" />
                  <el-table-column prop="orderNo" label="来源订单" min-width="118" sortable="custom" />
                  <el-table-column prop="productModel" label="产品型号" min-width="130" show-overflow-tooltip sortable="custom" />
                  <el-table-column prop="quantity" label="数量" min-width="72" align="right" sortable="custom" />
                  <el-table-column prop="deliveryDate" label="客户交期" min-width="108" sortable="custom" />
                  <el-table-column prop="priorityLabel" label="优先级" min-width="80" sortable="custom" />
                  <el-table-column prop="kitRateLabel" label="齐套率" min-width="80" align="center" sortable="custom" />
                  <el-table-column prop="estimatedHours" label="预计工时" min-width="96" align="right" sortable="custom">
                    <template #default="{ row }">{{ row.estimatedHours || '—' }}</template>
                  </el-table-column>
                  <el-table-column prop="workshop" label="车间" min-width="108" show-overflow-tooltip />
                  <el-table-column prop="equipmentLoadLabel" label="设备负荷" min-width="96" align="center" />
                  <el-table-column prop="schedulingRisk" label="排产风险" min-width="96" align="center">
                    <template #default="{ row }"><span class="wb-tag" :class="schedRiskClass(row.schedulingRisk)">{{ row.schedulingRisk }}</span></template>
                  </el-table-column>
                  <el-table-column prop="status" label="状态" min-width="88">
                    <template #default="{ row }"><span class="wb-tag" :class="statusTagClass(row.status)">{{ row.status }}</span></template>
                  </el-table-column>
                  <el-table-column label="操作" width="168" fixed="right">
                    <template #default="{ row }">
                      <el-button v-if="row.status === '草稿'" link type="primary" size="small" @click.stop="publish(row)">待提交</el-button>
                      <el-button v-if="row.status === '待提交'" link type="primary" size="small" @click.stop="submit(row)">提交</el-button>
                      <el-button link type="primary" size="small" @click.stop="reschedulePlan(row)">重排</el-button>
                      <el-button link type="primary" size="small" @click.stop="copyPlan(row)">版本</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <div class="wb-pagination">
                  <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :page-sizes="[20, 50, 100]" :total="sortedRows.length" layout="total, sizes, prev, pager, next" background small />
                </div>
              </div>
              <div class="wb-split__bottom">
                <PlanDetailPanel
                  :plan="selectedPlan"
                  :schedules="planSchedules"
                  :history="planHistory"
                  :order-context="selectedOrderContext"
                  :conflicts="selectedConflicts"
                  :mes="mes"
                  :loading="planDetailLoading"
                  :validating="validating"
                  @validate="validateSelectedPlan"
                  @reschedule="reschedulePlan(selectedPlan)"
                  @copy-version="copyPlan(selectedPlan)"
                />
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 甘特排程 -->
        <el-tab-pane label="甘特排程" name="gantt">
          <div class="wb-pane wb-pane--gantt">
            <PlanGanttChart
              :rows="ganttDisplayRows"
              :dependencies="ganttDeps"
              :conflict-indices="ganttConflicts"
              :view-mode="ganttViewMode"
              :loading="ganttLoading"
              :fullscreen="isFullscreen"
              @update:view-mode="ganttViewMode = $event"
              @exit-fullscreen="isFullscreen = false"
            />
          </div>
        </el-tab-pane>

        <!-- 产能负荷 -->
        <el-tab-pane label="产能负荷" name="capacity">
          <div class="wb-pane">
            <div class="wb-toolbar">
              <el-radio-group v-model="capacityView" size="small">
                <el-radio-button label="workshop">按车间</el-radio-button>
                <el-radio-button label="equipment">按设备</el-radio-button>
              </el-radio-group>
              <span class="wb-toolbar__hint">基于已排工序工时估算负荷率（单班16h × 5天/周）</span>
            </div>
            <div class="wb-table-wrap">
              <el-table :data="capacityRows" border size="small" height="100%" class="wb-table" v-loading="tableLoading">
                <el-table-column prop="name" :label="capacityView === 'workshop' ? '车间' : '设备'" min-width="120" />
                <el-table-column v-if="capacityView === 'equipment'" prop="workshop" label="所属车间" width="100" />
                <el-table-column prop="planCount" label="关联计划" min-width="88" align="center" />
                <el-table-column prop="scheduledHours" label="已排工时(h)" min-width="108" align="right">
                  <template #default="{ row }">{{ row.scheduledHours?.toFixed?.(1) ?? row.scheduledHours }}</template>
                </el-table-column>
                <el-table-column prop="capacityHours" label="周产能(h)" min-width="96" align="right" />
                <el-table-column label="负荷率" min-width="200">
                  <template #default="{ row }">
                    <div class="load-bar">
                      <div class="load-bar__track">
                        <div class="load-bar__fill" :class="loadBarClass(row.loadPct)" :style="{ width: `${row.loadPct}%` }" />
                      </div>
                      <span class="load-bar__pct" :class="loadPctClass(row.loadPct)">{{ row.loadPct }}%</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column v-if="capacityView === 'equipment'" prop="status" label="设备状态" width="88" align="center" />
              </el-table>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <ManualPlanWizard v-model="manualVisible" :default-order-id="manualOrderId" @success="onPlanSaved" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { PLAN_STATUS } from '@/mock/constants'
import { navigateToSmartScheduling } from '@/composables/usePlannerAgent'
import ManualPlanWizard from '@/components/mes/ManualPlanWizard.vue'
import PlanGanttChart from '@/components/planner/PlanGanttChart.vue'
import PlanDetailPanel from '@/components/planner/PlanDetailPanel.vue'
import {
  fetchOrderPlanningContext,
  postCopyProductionPlan,
  postListPlanHistory,
  postListPlanSchedules,
  postValidateProductionPlan
} from '@/api/planner'
import {
  enrichPlanRow,
  buildGanttRows,
  regroupGanttRows,
  detectEquipmentConflicts,
  buildGanttDependencies,
  buildCapacityLoad,
  computeKitRate,
  parseDate
} from '@/utils/planMetrics'
import { exportPlanRows, formatPlanExportFilename } from '@/utils/planExcelExport'

const mes = useMesStore()
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const activeTab = ref('plans')
const isFullscreen = ref(false)
const tableLoading = ref(false)
const ganttLoading = ref(false)
const planDetailLoading = ref(false)
const validating = ref(false)
const scheduleMap = ref({})
const orderContextMap = ref({})
const conflictMap = ref({})
let loadingDetailId = ''

function isSchedulableOrder(orderId) {
  const o = mes.orders.find((x) => x.id === orderId || x.orderNo === orderId)
  return o && ['待计划', '已审核'].includes(o.status)
}

function schedulableOrderIds(orderIds = []) {
  return orderIds.filter((id) => isSchedulableOrder(id))
}

const pendingKw = ref('')
const capacityView = ref('workshop')
const ganttViewMode = ref('plan')

const filters = reactive({ orderNo: '', productModel: '', status: '', dateRange: null })
const appliedFilters = reactive({ orderNo: '', productModel: '', status: '', dateRange: null })
const page = reactive({ current: 1, size: 20 })
const sortState = ref({ prop: 'planStart', order: 'descending' })

const selectedPlan = ref(null)
const planSchedules = ref([])
const planHistory = ref([])
const selectedOrderContext = ref(null)
const manualVisible = ref(false)
const manualOrderId = ref('')

const operatorName = computed(() => userStore.userInfo?.username || userStore.username || '')

const pendingOrders = computed(() => mes.pendingPlanOrders)
const pendingSubmit = computed(() => mes.pendingSubmitPlans)
const executingCount = computed(() => mes.plans.filter((p) => p.status === '执行中').length)

const filteredPending = computed(() => {
  const kw = pendingKw.value.trim().toLowerCase()
  if (!kw) return pendingOrders.value
  return pendingOrders.value.filter((o) =>
    String(o.id).toLowerCase().includes(kw) || String(o.productModel || '').toLowerCase().includes(kw)
  )
})

const enrichedRows = computed(() =>
  mes.plans.map((p) => enrichPlanRow(p, mes, scheduleMap.value, {
    orderContextMap: orderContextMap.value,
    conflictMap: conflictMap.value
  }))
)

const filteredRows = computed(() => {
  const orderKw = appliedFilters.orderNo.trim().toLowerCase()
  const modelKw = appliedFilters.productModel.trim().toLowerCase()
  const [rangeStart, rangeEnd] = appliedFilters.dateRange || []
  return enrichedRows.value.filter((row) => {
    if (appliedFilters.status && row.status !== appliedFilters.status) return false
    if (orderKw && !String(row.orderNo || '').toLowerCase().includes(orderKw)) return false
    if (modelKw && !String(row.productModel || '').toLowerCase().includes(modelKw)) return false
    if (rangeStart || rangeEnd) {
      const start = parseDate(row.planStart)
      const end = parseDate(row.planEnd)
      const from = rangeStart ? parseDate(rangeStart) : null
      const to = rangeEnd ? parseDate(rangeEnd) : null
      if (from && end && end < from) return false
      if (to && start && start > to) return false
    }
    return true
  })
})

const sortedRows = computed(() => {
  const rows = [...filteredRows.value]
  const { prop, order } = sortState.value
  if (!prop || !order) return rows
  const dir = order === 'ascending' ? 1 : -1
  rows.sort((a, b) => {
    const av = a[prop]
    const bv = b[prop]
    if (prop === 'planStart' || prop === 'planEnd' || prop === 'deliveryDate') {
      return ((parseDate(av)?.getTime() || 0) - (parseDate(bv)?.getTime() || 0)) * dir
    }
    if (typeof av === 'number' && typeof bv === 'number') return (av - bv) * dir
    return String(av ?? '').localeCompare(String(bv ?? ''), 'zh-CN') * dir
  })
  return rows
})

const pagedRows = computed(() => {
  const start = (page.current - 1) * page.size
  return sortedRows.value.slice(start, start + page.size)
})

const selectedConflicts = computed(() => {
  if (!selectedPlan.value) return []
  return conflictMap.value[selectedPlan.value.id]?.conflicts || selectedPlan.value.conflicts || []
})

const baseGanttRows = computed(() => buildGanttRows(filteredRows.value, scheduleMap.value, mes))
const ganttDisplayRows = computed(() => regroupGanttRows(baseGanttRows.value, ganttViewMode.value))
const ganttDeps = computed(() => buildGanttDependencies(ganttDisplayRows.value))
const ganttConflicts = computed(() => detectEquipmentConflicts(ganttDisplayRows.value))

const capacityData = computed(() => buildCapacityLoad(scheduleMap.value, mes, mes.plans))
const capacityRows = computed(() =>
  capacityView.value === 'workshop' ? capacityData.value.workshopRows : capacityData.value.equipmentRows
)

function kitRateLabel(orderId) {
  const ctx = orderContextMap.value[orderId]
  if (!ctx) return '—'
  const rate = computeKitRate(ctx.materialGaps)
  return rate == null ? '—' : `${rate}%`
}

function statusTagClass(status) {
  const map = {
    草稿: 'wb-tag--muted', 待提交: 'wb-tag--warn', 已发布: 'wb-tag--info',
    执行中: 'wb-tag--info', 已调整: 'wb-tag--warn', 已完成: 'wb-tag--ok', 已取消: 'wb-tag--muted'
  }
  return map[status] || 'wb-tag--muted'
}

function schedRiskClass(risk) {
  if (risk === '高风险') return 'wb-tag--danger'
  if (risk === '中风险') return 'wb-tag--warn'
  if (risk === '低风险') return 'wb-tag--info'
  return 'wb-tag--ok'
}

function loadBarClass(pct) {
  if (pct >= 90) return 'load-bar__fill--high'
  if (pct >= 70) return 'load-bar__fill--mid'
  return 'load-bar__fill--ok'
}

function loadPctClass(pct) {
  if (pct >= 90) return 'load-bar__pct--high'
  if (pct >= 70) return 'load-bar__pct--mid'
  return 'load-bar__pct--ok'
}

function applyQuery() {
  appliedFilters.orderNo = filters.orderNo
  appliedFilters.productModel = filters.productModel
  appliedFilters.status = filters.status
  appliedFilters.dateRange = filters.dateRange ? [...filters.dateRange] : null
  page.current = 1
}

function resetQuery() {
  filters.orderNo = ''
  filters.productModel = ''
  filters.status = ''
  filters.dateRange = null
  applyQuery()
}

function onSortChange({ prop, order }) {
  sortState.value = { prop, order }
}

async function loadSchedules(plans = mes.plans, force = false) {
  const map = force ? {} : { ...scheduleMap.value }
  const pending = force ? plans : plans.filter((p) => !map[p.id])
  if (!pending.length) {
    if (force) scheduleMap.value = map
    return
  }
  await Promise.all(pending.map(async (plan) => {
    try {
      map[plan.id] = await postListPlanSchedules({ planId: plan.id, operator: operatorName.value })
    } catch {
      map[plan.id] = []
    }
  }))
  scheduleMap.value = map
}

async function prefetchOrderContexts(orderIds = []) {
  const ids = schedulableOrderIds([...new Set(orderIds)]).filter((id) => id && !orderContextMap.value[id])
  if (!ids.length) return
  await Promise.all(ids.map(async (id) => {
    try {
      const ctx = await fetchOrderPlanningContext(id, { silent: true })
      orderContextMap.value = { ...orderContextMap.value, [id]: ctx }
    } catch {
      orderContextMap.value = { ...orderContextMap.value, [id]: { materialGaps: [] } }
    }
  }))
}

async function validatePlanRow(row, { silent = true } = {}) {
  if (!row) return
  const schedules = scheduleMap.value[row.id] || []
  try {
    const res = await postValidateProductionPlan({
      orderId: row.orderNo || row.orderId,
      planStart: row.planStart,
      planEnd: row.planEnd,
      plannedQty: row.quantity,
      schedules,
      operator: operatorName.value
    }, { silent })
    conflictMap.value = { ...conflictMap.value, [row.id]: res }
    return res
  } catch (e) {
    conflictMap.value = { ...conflictMap.value, [row.id]: { conflicts: [], hasDanger: false } }
    return null
  }
}

async function refresh() {
  tableLoading.value = true
  try {
    await mes.hydrateFromApi()
    await loadSchedules(mes.plans, true)
    const orderIds = mes.pendingPlanOrders.map((o) => o.id)
    await prefetchOrderContexts(orderIds)
    if (selectedPlan.value) {
      const updated = enrichedRows.value.find((r) => r.id === selectedPlan.value.id)
      if (updated) {
        selectedPlan.value = updated
        await loadPlanDetail(updated)
      }
    }
  } finally {
    tableLoading.value = false
  }
}

function exportExcel() {
  if (!sortedRows.value.length) {
    ElMessage.warning('当前筛选结果为空')
    return
  }
  exportPlanRows(sortedRows.value, formatPlanExportFilename())
  ElMessage.success('导出成功')
}

async function onTabChange(name) {
  if (name === 'gantt' || name === 'capacity') {
    ganttLoading.value = name === 'gantt'
    tableLoading.value = name === 'capacity'
    try {
      await loadSchedules(mes.plans)
    } finally {
      ganttLoading.value = false
      tableLoading.value = false
    }
  }
  if (name === 'pending') {
    await prefetchOrderContexts(pendingOrders.value.map((o) => o.id))
  }
}

async function loadPlanDetail(row) {
  if (!row || loadingDetailId === row.id) return
  loadingDetailId = row.id
  planDetailLoading.value = true
  try {
    planSchedules.value = scheduleMap.value[row.id] || await postListPlanSchedules({ planId: row.id, operator: operatorName.value })
    if (!scheduleMap.value[row.id]) {
      scheduleMap.value = { ...scheduleMap.value, [row.id]: planSchedules.value }
    }
    planHistory.value = await postListPlanHistory({ planId: row.id, operator: operatorName.value })
    const oid = row.orderId || row.orderNo
    if (!orderContextMap.value[oid]) {
      try {
        const ctx = await fetchOrderPlanningContext(oid, { silent: true })
        orderContextMap.value = { ...orderContextMap.value, [oid]: ctx }
      } catch {
        orderContextMap.value = { ...orderContextMap.value, [oid]: { materialGaps: [] } }
      }
    }
    selectedOrderContext.value = orderContextMap.value[oid]
  } finally {
    planDetailLoading.value = false
    loadingDetailId = ''
  }
}

async function onPlanRowSelect(row) {
  if (!row) return
  selectedPlan.value = row
  await loadPlanDetail(row)
}

function onPendingRowClick(row) {
  prefetchOrderContexts([row.id])
}

async function validateSelectedPlan() {
  if (!selectedPlan.value) return
  validating.value = true
  try {
    const res = await validatePlanRow(selectedPlan.value, { silent: false })
    const cnt = res?.conflicts?.length || 0
    ElMessage.success(cnt ? `发现 ${cnt} 项提示/冲突` : '校验通过，无冲突')
    selectedPlan.value = enrichedRows.value.find((r) => r.id === selectedPlan.value.id) || selectedPlan.value
  } finally {
    validating.value = false
  }
}

function openSmart(orderId) {
  const id = orderId || selectedPlan.value?.orderId || pendingOrders.value[0]?.id || ''
  navigateToSmartScheduling(router, { orderId: id })
}

function openManual(orderId) {
  manualOrderId.value = orderId || selectedPlan.value?.orderId || pendingOrders.value[0]?.id || ''
  manualVisible.value = true
}

function reschedulePlan(row) {
  if (!row) return
  manualOrderId.value = row.orderId || row.orderNo
  manualVisible.value = true
}

async function publish(row) {
  if (await mes.publishPlan(row.id, operatorName.value, userStore.roleKey)) {
    ElMessage.success('计划已进入待提交')
    await refresh()
  }
}

async function submit(row) {
  const ok = await mes.submitPlanToManager(row.id, operatorName.value, userStore.roleKey)
  if (ok !== false) {
    ElMessage.success('已提交生产主管')
    await refresh()
  }
}

async function copyPlan(row) {
  if (!row) return
  try {
    await postCopyProductionPlan({ planId: row.id, operator: operatorName.value })
    ElMessage.success('已复制为新版本草稿')
    await refresh()
  } catch {
    ElMessage.error('复制失败')
  }
}

function onPlanSaved() {
  refresh()
}

watch(isFullscreen, (v) => {
  document.body.classList.toggle('mes-page-fs', v)
})

onMounted(async () => {
  tableLoading.value = true
  try {
    if (!mes.hydrated) await mes.hydrateFromApi()
    await loadSchedules(mes.plans, true)
    await prefetchOrderContexts(mes.pendingPlanOrders.map((o) => o.id))
    if (pagedRows.value.length) {
      selectedPlan.value = pagedRows.value[0]
      await loadPlanDetail(pagedRows.value[0])
    }
  } catch { /* ignore */ } finally {
    tableLoading.value = false
  }
  if (route.query.orderId) {
    const orderId = String(route.query.orderId)
    if (route.query.action === 'schedule') navigateToSmartScheduling(router, { orderId })
    else activeTab.value = 'pending'
  }
})

onBeforeUnmount(() => {
  document.body.classList.remove('mes-page-fs')
})
</script>

<style scoped>
.wb {
  margin: -12px;
  min-height: calc(100vh - 98px);
  background: #f5f5f5;
  padding: 12px;
  font-size: 13px;
  color: #374151;
}

.wb__panel {
  height: calc(100vh - 122px);
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #d4d4d4;
}

.wb__head {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 40px;
  padding: 0 12px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.wb__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.wb__meta {
  font-size: 12px;
  color: #6b7280;
}

.wb__meta em {
  font-style: normal;
  font-weight: 600;
  color: #3d7a5f;
}

.wb__head-actions {
  margin-left: auto;
  display: flex;
  gap: 6px;
}

.wb-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.wb-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 12px;
  border-bottom: 1px solid #e5e7eb;
}

.wb-tabs :deep(.el-tabs__item) {
  height: 36px;
  line-height: 36px;
  font-size: 13px;
}

.wb-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
}

.wb-tabs :deep(.el-tab-pane) {
  height: 100%;
}

.wb-pane {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.wb-pane--split {
  overflow: hidden;
}

.wb-pane--gantt {
  overflow: hidden;
}

.wb-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 40px;
  padding: 4px 12px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.wb-toolbar__field {
  width: 110px;
}

.wb-toolbar__field--wide {
  width: 180px;
}

.wb-toolbar__range {
  width: 220px !important;
}

.wb-toolbar__hint {
  margin-left: auto;
  font-size: 12px;
  color: #9ca3af;
}

.wb-table-wrap {
  flex: 1;
  min-height: 0;
  padding: 0 12px 8px;
}

.wb-split {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.wb-split__top {
  flex: 1 1 58%;
  min-height: 180px;
  display: flex;
  flex-direction: column;
  padding: 0 12px;
  overflow: hidden;
}

.wb-split__bottom {
  flex: 0 0 38%;
  min-height: 200px;
  max-height: 42%;
  overflow: hidden;
}

.wb-pagination {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 4px 0 6px;
}

.wb-table :deep(.el-table th.el-table__cell) {
  background: #f3f4f6;
  color: #374151;
  font-weight: 500;
  font-size: 12px;
  padding: 4px 0;
  border-color: #d4d4d4 !important;
}

.wb-table :deep(.el-table td.el-table__cell) {
  font-size: 12px;
  padding: 2px 0;
  border-color: #e5e7eb !important;
}

.wb-table :deep(.el-table__body tr.current-row > td.el-table__cell) {
  background: #e8f5e9 !important;
}

.wb-tag {
  display: inline-block;
  padding: 1px 6px;
  font-size: 11px;
  border-radius: 2px;
  border: 1px solid transparent;
}
.wb-tag--muted { background: #f9fafb; color: #6b7280; border-color: #e5e7eb; }
.wb-tag--info { background: #eff6ff; color: #1d4ed8; border-color: #dbeafe; }
.wb-tag--warn { background: #fffbeb; color: #b45309; border-color: #fde68a; }
.wb-tag--ok { background: #f0fdf4; color: #15803d; border-color: #bbf7d0; }
.wb-tag--danger { background: #fef2f2; color: #b91c1c; border-color: #fecaca; }

.load-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.load-bar__track {
  flex: 1;
  height: 8px;
  background: #e5e7eb;
  border-radius: 2px;
  overflow: hidden;
}

.load-bar__fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.2s;
}
.load-bar__fill--ok { background: #3d7a5f; }
.load-bar__fill--mid { background: #d97706; }
.load-bar__fill--high { background: #dc2626; }

.load-bar__pct { font-size: 12px; font-weight: 600; min-width: 36px; }
.load-bar__pct--ok { color: #15803d; }
.load-bar__pct--mid { color: #b45309; }
.load-bar__pct--high { color: #b91c1c; }
</style>

<style>
body.mes-page-fs .layout-aside,
body.mes-page-fs .layout-header,
body.mes-page-fs .tags-view { display: none !important; }
body.mes-page-fs .layout-main.ruoyi-app-main { padding: 0 !important; overflow: hidden; }
body.mes-page-fs .wb { margin: 0; padding: 0; min-height: 100vh; height: 100vh; }
body.mes-page-fs .wb__panel { height: 100vh; border: none; }
</style>
