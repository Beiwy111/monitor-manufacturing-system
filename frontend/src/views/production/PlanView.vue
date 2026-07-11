<template>
  <div class="planner-plan-page" :class="{ 'planner-plan-page--fs': isFullscreen }">
    <div class="planner-plan-page__panel">
      <div class="planner-plan-page__title-bar">
        <h2 class="planner-plan-page__title">生产计划</h2>
        <span class="planner-plan-page__meta">
          待排 <em>{{ pendingOrders.length }}</em>
          · 待提交 <em>{{ pendingSubmit.length }}</em>
          · 执行中 <em>{{ executingCount }}</em>
        </span>
      </div>

      <el-tabs v-model="activeTab" class="planner-tabs" @tab-change="onTabChange">
        <!-- 计划表 -->
        <el-tab-pane label="计划表" name="table">
          <div class="planner-pane">
            <div class="planner-toolbar">
              <el-input v-model="filters.orderNo" clearable placeholder="订单号" class="planner-toolbar__field" />
              <el-input v-model="filters.productModel" clearable placeholder="产品型号" class="planner-toolbar__field planner-toolbar__field--wide" />
              <el-select v-model="filters.status" clearable placeholder="计划状态" class="planner-toolbar__field">
                <el-option v-for="s in PLAN_STATUS" :key="s" :label="s" :value="s" />
              </el-select>
              <el-date-picker
                v-model="filters.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                class="planner-toolbar__daterange"
              />
              <el-button type="primary" @click="applyQuery">查询</el-button>
              <el-button @click="resetQuery">重置</el-button>
              <el-button @click="refresh">刷新</el-button>
              <el-button @click="exportExcel">导出 Excel</el-button>
              <el-button @click="toggleFullscreen">{{ isFullscreen ? '退出全屏' : '全屏' }}</el-button>
              <div class="planner-toolbar__actions">
                <el-button type="success" link @click="openSmart()">智能排产</el-button>
                <el-button type="primary" link @click="openManual()">手动排产</el-button>
              </div>
            </div>

            <div class="planner-table-wrap">
              <el-table
                ref="tableRef"
                v-loading="tableLoading"
                :data="pagedRows"
                border
                stripe
                height="100%"
                row-key="id"
                :default-sort="{ prop: 'planStart', order: 'descending' }"
                @sort-change="onSortChange"
                @row-dblclick="openPlanDetail"
              >
                <el-table-column prop="id" label="计划编号" min-width="128" sortable="custom" fixed="left" />
                <el-table-column prop="orderNo" label="订单编号" min-width="120" sortable="custom" />
                <el-table-column prop="productModel" label="产品型号" min-width="140" sortable="custom" show-overflow-tooltip />
                <el-table-column prop="quantity" label="计划数量" width="96" align="right" sortable="custom" />
                <el-table-column prop="priorityLabel" label="优先级" width="88" sortable="custom" :filters="priorityFilters" :filter-method="filterPriority" />
                <el-table-column prop="planStart" label="计划开始时间" width="118" sortable="custom" />
                <el-table-column prop="planEnd" label="计划完成时间" width="118" sortable="custom" />
                <el-table-column prop="workshop" label="车间" min-width="110" show-overflow-tooltip :filters="workshopFilters" :filter-method="filterWorkshop" />
                <el-table-column prop="estimatedHours" label="预计工时" width="96" align="right" sortable="custom">
                  <template #default="{ row }">{{ row.estimatedHours || '—' }}</template>
                </el-table-column>
                <el-table-column prop="progress" label="完成进度" width="120" sortable="custom">
                  <template #default="{ row }">
                    <div class="planner-progress">
                      <div class="planner-progress__track">
                        <div class="planner-progress__bar" :style="{ width: `${row.progress}%` }" />
                      </div>
                      <span>{{ row.progress }}%</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="计划状态" width="100" :filters="statusFilters" :filter-method="filterStatus">
                  <template #default="{ row }"><span class="plan-tag" :class="statusTagClass(row.status)">{{ row.status }}</span></template>
                </el-table-column>
                <el-table-column prop="delayRisk" label="延期风险" width="100" :filters="riskFilters" :filter-method="filterRisk">
                  <template #default="{ row }"><span class="plan-tag" :class="riskTagClass(row.delayRisk)">{{ row.delayRisk }}</span></template>
                </el-table-column>
                <el-table-column label="操作" width="200" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="openPlanDetail(row)">详情</el-button>
                    <el-button v-if="row.status === '草稿'" link type="primary" @click="publish(row)">待提交</el-button>
                    <el-button v-if="row.status === '待提交'" link type="primary" @click="submit(row)">提交主管</el-button>
                    <el-button link type="primary" @click="copyPlan(row)">复制</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div class="planner-pagination">
              <el-pagination
                v-model:current-page="page.current"
                v-model:page-size="page.size"
                :page-sizes="[20, 50, 100]"
                :total="sortedRows.length"
                layout="total, sizes, prev, pager, next, jumper"
                background
                small
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- 甘特图 -->
        <el-tab-pane label="甘特图" name="gantt">
          <div class="planner-pane planner-pane--gantt">
            <div class="planner-toolbar planner-toolbar--gantt">
              <el-button @click="refresh">刷新</el-button>
              <el-button @click="toggleFullscreen">{{ isFullscreen ? '退出全屏' : '全屏' }}</el-button>
            </div>
            <PlanGanttChart
              :rows="ganttRows"
              :loading="ganttLoading"
              :fullscreen="isFullscreen"
              @exit-fullscreen="isFullscreen = false"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-drawer v-model="orderDrawer" title="订单排产上下文" size="520px" destroy-on-close>
      <template v-if="orderContext">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单">{{ orderContext.orderId }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ orderContext.customerName }}</el-descriptions-item>
          <el-descriptions-item label="型号">{{ orderContext.productModel }}</el-descriptions-item>
          <el-descriptions-item label="数量">{{ orderContext.orderQuantity }}</el-descriptions-item>
          <el-descriptions-item label="交期">{{ orderContext.deliveryDate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="建议产量">{{ orderContext.recommendedPlanQty ?? '—' }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">物料缺口</el-divider>
        <el-table :data="orderContext.materialGaps || []" border stripe size="small">
          <el-table-column prop="materialName" label="物料" min-width="120" />
          <el-table-column prop="requiredQty" label="需求" width="72" align="right" />
          <el-table-column prop="availableQty" label="库存" width="72" align="right" />
          <el-table-column prop="gapQty" label="缺口" width="72" align="right" />
        </el-table>
        <el-divider content-position="left">工艺路线</el-divider>
        <el-table :data="orderContext.processRoute || []" border stripe size="small">
          <el-table-column prop="stepNo" label="序号" width="56" />
          <el-table-column prop="stepName" label="工序" min-width="120" />
          <el-table-column prop="standardEquipmentType" label="建议设备" width="100" />
          <el-table-column prop="standardWorkHours" label="工时" width="72" align="right" />
        </el-table>
        <div class="drawer-actions">
          <el-button type="success" @click="openSmart(selectedOrder?.id)">智能排产</el-button>
          <el-button type="primary" @click="openManual(selectedOrder?.id)">手动排产</el-button>
        </div>
      </template>
    </el-drawer>

    <el-drawer v-model="planDrawer" title="计划详情" size="560px" destroy-on-close>
      <template v-if="selectedPlan">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="计划号">{{ selectedPlan.id }}</el-descriptions-item>
          <el-descriptions-item label="版本">{{ selectedPlan.versionNo || 'V1' }}</el-descriptions-item>
          <el-descriptions-item label="订单">{{ selectedPlan.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态"><span class="plan-tag" :class="statusTagClass(selectedPlan.status)">{{ selectedPlan.status }}</span></el-descriptions-item>
          <el-descriptions-item label="数量">{{ selectedPlan.quantity }}</el-descriptions-item>
          <el-descriptions-item label="模式">{{ modeLabel(selectedPlan.schedulingMode) }}</el-descriptions-item>
          <el-descriptions-item label="周期" :span="2">{{ selectedPlan.planStart }} ~ {{ selectedPlan.planEnd }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">工序排程</el-divider>
        <el-table :data="planSchedules" border stripe size="small" v-loading="planDetailLoading">
          <el-table-column prop="stepName" label="工序" min-width="100" />
          <el-table-column prop="workshop" label="车间" width="100" />
          <el-table-column prop="equipmentCode" label="设备" width="90" />
          <el-table-column prop="plannedQuantity" label="数量" width="64" align="right" />
          <el-table-column prop="plannedStart" label="开始" min-width="130" />
          <el-table-column prop="plannedEnd" label="结束" min-width="130" />
        </el-table>
        <el-divider content-position="left">变更历史</el-divider>
        <el-timeline>
          <el-timeline-item v-for="h in planHistory" :key="h.id" :timestamp="h.createdAt">
            {{ h.actionType }} · {{ h.operatorName }}
          </el-timeline-item>
        </el-timeline>
        <el-empty v-if="!planHistory.length" description="暂无历史" :image-size="48" />
      </template>
    </el-drawer>

    <PlannerAgentDialog v-model="smartVisible" :default-order-id="smartOrderId" @success="onPlanSaved" />
    <ManualPlanWizard v-model="manualVisible" :default-order-id="manualOrderId" @success="onPlanSaved" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { PLAN_STATUS } from '@/mock/constants'
import PlannerAgentDialog from '@/components/mes/PlannerAgentDialog.vue'
import ManualPlanWizard from '@/components/mes/ManualPlanWizard.vue'
import PlanGanttChart from '@/components/planner/PlanGanttChart.vue'
import { fetchOrderPlanningContext, postCopyProductionPlan, postListPlanHistory, postListPlanSchedules } from '@/api/planner'
import { enrichPlanRow, buildGanttRows, parseDate } from '@/utils/planMetrics'
import { exportPlanRows, formatPlanExportFilename } from '@/utils/planExcelExport'

const mes = useMesStore()
const userStore = useUserStore()
const route = useRoute()

const activeTab = ref('table')
const isFullscreen = ref(false)
const tableLoading = ref(false)
const ganttLoading = ref(false)
const scheduleMap = ref({})

const filters = reactive({
  orderNo: '',
  productModel: '',
  status: '',
  dateRange: null
})
const appliedFilters = reactive({
  orderNo: '',
  productModel: '',
  status: '',
  dateRange: null
})

const page = reactive({ current: 1, size: 20 })
const sortState = ref({ prop: 'planStart', order: 'descending' })

const selectedOrder = ref(null)
const selectedPlan = ref(null)
const orderDrawer = ref(false)
const planDrawer = ref(false)
const orderContext = ref(null)
const planSchedules = ref([])
const planHistory = ref([])
const planDetailLoading = ref(false)
const smartVisible = ref(false)
const manualVisible = ref(false)
const smartOrderId = ref('')
const manualOrderId = ref('')

const operatorName = computed(() => userStore.userInfo?.username || '')

const pendingOrders = computed(() => mes.pendingPlanOrders)
const pendingSubmit = computed(() => mes.pendingSubmitPlans)
const executingCount = computed(() => mes.plans.filter((p) => p.status === '执行中').length)

const enrichedRows = computed(() =>
  mes.plans.map((p) => enrichPlanRow(p, mes, scheduleMap.value))
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
    if (prop === 'planStart' || prop === 'planEnd') {
      const ad = parseDate(av)?.getTime() || 0
      const bd = parseDate(bv)?.getTime() || 0
      return (ad - bd) * dir
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

const ganttRows = computed(() => buildGanttRows(filteredRows.value, scheduleMap.value, mes))

const statusFilters = PLAN_STATUS.map((s) => ({ text: s, value: s }))
const priorityFilters = computed(() => [...new Set(enrichedRows.value.map((r) => r.priorityLabel))].map((v) => ({ text: v, value: v })))
const workshopFilters = computed(() => [...new Set(enrichedRows.value.map((r) => r.workshop).filter((w) => w && w !== '—'))].map((v) => ({ text: v, value: v })))
const riskFilters = ['正常', '关注', '延期风险', '严重延期'].map((v) => ({ text: v, value: v }))

function filterStatus(value, row) {
  return row.status === value
}
function filterPriority(value, row) {
  return row.priorityLabel === value
}
function filterWorkshop(value, row) {
  return row.workshop === value
}
function filterRisk(value, row) {
  return row.delayRisk === value
}

function statusTagClass(status) {
  const map = {
    草稿: 'plan-tag--muted',
    待提交: 'plan-tag--warn',
    已发布: 'plan-tag--info',
    执行中: 'plan-tag--info',
    已调整: 'plan-tag--warn',
    已完成: 'plan-tag--ok',
    已取消: 'plan-tag--muted'
  }
  return map[status] || 'plan-tag--muted'
}

function riskTagClass(risk) {
  if (risk === '严重延期') return 'plan-tag--danger'
  if (risk === '延期风险') return 'plan-tag--warn'
  if (risk === '关注') return 'plan-tag--info'
  return 'plan-tag--muted'
}

function modeLabel(mode) {
  return { MANUAL: '手动', DELIVERY: '交期优先', BALANCE: '负载均衡', COST: '成本优先' }[mode] || mode || '手动'
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
  const loadingGantt = activeTab.value === 'gantt'
  tableLoading.value = true
  if (loadingGantt) ganttLoading.value = true
  try {
    await Promise.all(pending.map(async (plan) => {
      try {
        map[plan.id] = await postListPlanSchedules({ planId: plan.id, operator: operatorName.value })
      } catch {
        map[plan.id] = []
      }
    }))
    scheduleMap.value = map
  } finally {
    tableLoading.value = false
    ganttLoading.value = false
  }
}

async function refresh() {
  tableLoading.value = true
  try {
    await mes.hydrateFromApi()
    await loadSchedules(mes.plans, true)
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

function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
}

async function onTabChange(name) {
  if (name === 'gantt') {
    ganttLoading.value = true
    try {
      await mes.hydrateFromApi()
      await loadSchedules(mes.plans, true)
    } finally {
      ganttLoading.value = false
    }
  }
}

async function openPlanDetail(row) {
  if (!row) return
  selectedPlan.value = row
  planDrawer.value = true
  planDetailLoading.value = true
  try {
    planSchedules.value = scheduleMap.value[row.id] || await postListPlanSchedules({ planId: row.id, operator: operatorName.value })
    if (!scheduleMap.value[row.id]) {
      scheduleMap.value = { ...scheduleMap.value, [row.id]: planSchedules.value }
    }
    planHistory.value = await postListPlanHistory({ planId: row.id, operator: operatorName.value })
  } finally {
    planDetailLoading.value = false
  }
}

async function openOrderContext(orderId) {
  const order = mes.orders.find((o) => o.id === orderId)
  if (!order) return
  selectedOrder.value = order
  orderDrawer.value = true
  try {
    orderContext.value = await fetchOrderPlanningContext(orderId)
  } catch {
    ElMessage.error('加载订单上下文失败')
  }
}

function openSmart(orderId) {
  smartOrderId.value = orderId || selectedOrder.value?.id || pendingOrders.value[0]?.id || ''
  smartVisible.value = true
}

function openManual(orderId) {
  manualOrderId.value = orderId || selectedOrder.value?.id || pendingOrders.value[0]?.id || ''
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
  try {
    await postCopyProductionPlan({ planId: row.id, operator: operatorName.value })
    ElMessage.success('计划已复制为新版草稿')
    await refresh()
  } catch {
    ElMessage.error('复制失败')
  }
}

function onPlanSaved() {
  refresh()
  orderDrawer.value = false
}

watch(isFullscreen, (v) => {
  document.body.classList.toggle('mes-page-fs', v)
})

onMounted(async () => {
  tableLoading.value = true
  try {
    if (!mes.hydrated) {
      await mes.hydrateFromApi()
    }
    await loadSchedules(mes.plans, true)
  } catch {
    /* ignore */
  } finally {
    tableLoading.value = false
  }
  if (route.query.orderId) {
    const orderId = String(route.query.orderId)
    if (route.query.action === 'schedule') {
      openSmart(orderId)
    } else {
      openOrderContext(orderId)
    }
  }
})

onBeforeUnmount(() => {
  document.body.classList.remove('mes-page-fs')
})
</script>

<style scoped>
.planner-plan-page {
  margin: -12px;
  min-height: calc(100vh - 98px);
  background: #f0f2f5;
  padding: 12px;
  font-size: 14px;
  font-weight: 400;
  color: #374151;
}

.planner-plan-page__panel {
  height: calc(100vh - 122px);
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.planner-plan-page__title-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 44px;
  padding: 0 16px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.planner-plan-page__title {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #111827;
}

.planner-plan-page__meta {
  font-size: 13px;
  color: #6b7280;
}

.planner-plan-page__meta em {
  font-style: normal;
  font-weight: 500;
  color: #2563eb;
}

.planner-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.planner-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 12px;
  border-bottom: 1px solid #e5e7eb;
}

.planner-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
}

.planner-tabs :deep(.el-tab-pane) {
  height: 100%;
}

.planner-pane {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.planner-pane--gantt .plan-gantt {
  flex: 1;
  min-height: 0;
  border: none;
  border-top: 1px solid #e5e7eb;
}

.planner-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 48px;
  padding: 6px 12px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.planner-toolbar--gantt {
  min-height: 40px;
}

.planner-toolbar__field {
  width: 120px;
}

.planner-toolbar__field--wide {
  width: 150px;
}

.planner-toolbar__daterange {
  width: 240px !important;
}

.planner-toolbar__actions {
  margin-left: auto;
  display: flex;
  gap: 4px;
}

.planner-table-wrap {
  flex: 1;
  min-height: 0;
  padding: 0 12px;
}

.planner-table-wrap :deep(.el-table) {
  font-size: 14px;
}

.planner-table-wrap :deep(.el-table th.el-table__cell) {
  background: #fafafa;
  color: #6b7280;
  font-weight: 500;
  height: 44px;
}

.planner-table-wrap :deep(.el-table .el-table__row) {
  height: 44px;
}

.planner-pagination {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 8px 12px;
  border-top: 1px solid #e5e7eb;
  background: #fff;
}

.planner-progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

.planner-progress__track {
  flex: 1;
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
}

.planner-progress__bar {
  height: 100%;
  background: #3b82f6;
  border-radius: 3px;
}

.plan-tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: 12px;
  line-height: 1.4;
  border-radius: 2px;
  border: 1px solid transparent;
}
.plan-tag--muted { background: #f9fafb; color: #6b7280; border-color: #e5e7eb; }
.plan-tag--info { background: #eff6ff; color: #1d4ed8; border-color: #dbeafe; }
.plan-tag--warn { background: #fffbeb; color: #b45309; border-color: #fde68a; }
.plan-tag--ok { background: #f0fdf4; color: #15803d; border-color: #bbf7d0; }
.plan-tag--danger { background: #fef2f2; color: #b91c1c; border-color: #fecaca; }

.drawer-actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}
</style>

<style>
body.mes-page-fs .layout-aside,
body.mes-page-fs .layout-header,
body.mes-page-fs .tags-view {
  display: none !important;
}

body.mes-page-fs .layout-main.ruoyi-app-main {
  padding: 0 !important;
  overflow: hidden;
}

body.mes-page-fs .planner-plan-page {
  margin: 0;
  padding: 0;
  min-height: 100vh;
  height: 100vh;
}

body.mes-page-fs .planner-plan-page__panel {
  height: 100vh;
  border: none;
  box-shadow: none;
}
</style>
