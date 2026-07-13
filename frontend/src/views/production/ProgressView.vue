<template>
  <div class="progress-page">
    <!-- KPI 概览 -->
    <section class="progress-kpi">
      <div
        v-for="item in kpiItems"
        :key="item.key"
        class="progress-kpi__card"
        :class="{ 'progress-kpi__card--warn': item.warn, 'progress-kpi__card--accent': item.accent }"
      >
        <span class="progress-kpi__label">{{ item.label }}</span>
        <span class="progress-kpi__value">
          {{ item.value }}<small v-if="item.unit">{{ item.unit }}</small>
        </span>
      </div>
    </section>

    <!-- 图表区 -->
    <section class="progress-charts">
      <div class="progress-panel">
        <div class="progress-panel__head">
          <span class="progress-panel__title">近7日产量趋势</span>
          <span v-if="lastRefresh" class="progress-panel__meta">{{ lastRefresh }}</span>
        </div>
        <ProgressChart
          :option="trendOption"
          :loading="initialLoading"
          :error="apiError"
          :empty="!trendHasData"
          empty-text="近7日暂无报工产量"
        />
      </div>
      <div class="progress-panel">
        <div class="progress-panel__head">
          <span class="progress-panel__title">工单状态分布</span>
        </div>
        <ProgressChart
          :option="donutOption"
          :loading="initialLoading"
          :error="apiError"
          :empty="!statusHasData"
          empty-text="暂无工单状态数据"
        />
      </div>
      <div class="progress-panel progress-panel--wide">
        <div class="progress-panel__head">
          <span class="progress-panel__title">八道生产工序完成进度</span>
        </div>
        <ProgressChart
          :option="stageBarOption"
          :loading="initialLoading"
          :error="apiError"
          :empty="!stageHasData"
          empty-text="暂无工序派工数据"
        />
      </div>
    </section>

    <!-- 搜索 -->
    <div class="progress-query">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="关键字">
          <el-input
            v-model="keyword"
            placeholder="搜索生产进度（工单号/型号/订单号）"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 工单表格 -->
    <div class="progress-table-section">
      <div class="progress-table-head">
        <span class="progress-table-head__title">生产进度</span>
        <span class="progress-table-head__meta">共 {{ filtered.length }} 条</span>
      </div>
      <div class="progress-table-wrap">
        <el-table
          :data="pageData"
          border
          stripe
          highlight-current-row
          row-key="id"
          class="progress-table"
          style="width: 100%"
          @current-change="onRowClick"
        >
          <el-table-column prop="id" label="工单" min-width="130" show-overflow-tooltip />
          <el-table-column prop="productModel" label="型号" min-width="140" show-overflow-tooltip />
          <el-table-column prop="quantity" label="计划" width="72" align="right" />
          <el-table-column prop="completedQty" label="完成" width="72" align="right" />
          <el-table-column label="进度" min-width="160">
            <template #default="{ row }">
              <div class="progress-cell">
                <el-progress
                  :percentage="rowProgress(row)"
                  :stroke-width="8"
                  :color="rowProgress(row) >= 100 ? '#0d9488' : '#2563eb'"
                />
              </div>
            </template>
          </el-table-column>
          <el-table-column label="当前工序" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.currentStep }}</template>
          </el-table-column>
          <el-table-column label="预计完成" width="110">
            <template #default="{ row }">{{ row.eta }}</template>
          </el-table-column>
          <el-table-column label="延期" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.delayed" type="danger" size="small" effect="plain">延期</el-tag>
              <el-tag v-else type="success" size="small" effect="plain">正常</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <StatusBadge :status="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="72" fixed="right">
            <template #default="{ row }">
              <el-button link type="danger" @click.stop="removeRow(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="total > pageSize" class="progress-pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </div>

    <!-- 工序时间轴 -->
    <div v-if="selected" class="progress-timeline">
      <div class="progress-timeline__head">
        <div>
          <span class="progress-timeline__title">工序时间轴 · {{ selected.id }}</span>
          <span class="progress-timeline__sub">{{ selected.productModel }} · 计划 {{ selected.quantity }} 台</span>
        </div>
        <el-button type="danger" size="small" plain @click="removeSelected">删除工单</el-button>
      </div>
      <div class="progress-timeline__track">
        <div
          v-for="step in timelineSteps"
          :key="step.order"
          class="progress-timeline__step"
          :class="`progress-timeline__step--${stepStatusClass(step.status)}`"
        >
          <div class="progress-timeline__dot">
            <span class="progress-timeline__order">{{ step.order }}</span>
          </div>
          <div class="progress-timeline__body">
            <div class="progress-timeline__row">
              <strong>{{ step.stepName }}</strong>
              <el-tag size="small" effect="plain">{{ step.status }}</el-tag>
            </div>
            <div class="progress-timeline__metrics">
              <span>完成 <em>{{ step.completedQty }}</em>/{{ step.planQty }} 台</span>
              <span>开工 {{ step.startTime }}</span>
              <span v-if="step.endTime !== '—'">完工 {{ step.endTime }}</span>
              <span>操作员 {{ step.operatorName }}</span>
            </div>
            <el-progress
              :percentage="step.percent"
              :stroke-width="6"
              :show-text="false"
              :color="step.percent >= 100 ? '#0d9488' : '#2563eb'"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { fetchMesSnapshot } from '@/api/mes'
import { useMesFilter } from '@/composables/useMesPage'
import { useMesDelete } from '@/composables/useMesDelete'
import StatusBadge from '@/components/mes/StatusBadge.vue'
import ProgressChart from '@/components/production/ProgressChart.vue'
import {
  buildSevenDayOutput,
  buildStatusDistribution,
  buildStageProgress,
  computeProgressKpi,
  buildWorkOrderTimeline,
  resolveCurrentStep,
  resolveEta,
  isWorkOrderDelayed,
  buildTrendOption,
  buildDonutOption,
  buildStageBarOption
} from '@/composables/useProductionProgressCharts'

const POLL_MS = 5000
const SEARCH_FIELDS = ['id', 'productModel', 'orderId', 'orderNo', 'planId', 'status']

const mes = useMesStore()
const userStore = useUserStore()
const { runDelete } = useMesDelete(mes, userStore)

const initialLoading = ref(true)
const apiError = ref('')
const lastRefresh = ref('')
const pageNum = ref(1)
const pageSize = ref(10)

let pollTimer = null

const planMap = computed(() => new Map(mes.plans.map((p) => [p.id, p])))
const orderMap = computed(() => new Map(mes.orders.map((o) => [o.id, o])))

const workOrderList = computed(() => mes.workOrders || [])

const enrichedList = computed(() =>
  workOrderList.value.map((wo) => ({
    ...wo,
    currentStep: resolveCurrentStep(wo, mes.dispatches),
    eta: resolveEta(wo, planMap.value, orderMap.value),
    delayed: isWorkOrderDelayed(wo, planMap.value, orderMap.value)
  }))
)

const { selected, filtered, onRowClick, keyword } = useMesFilter(enrichedList, SEARCH_FIELDS)

const total = computed(() => filtered.value.length)
const pageData = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})

const kpi = computed(() =>
  computeProgressKpi(workOrderList.value, mes.plans, mes.orders)
)

const kpiItems = computed(() => [
  { key: 'planned', label: '计划总量', value: kpi.value.plannedTotal, unit: '台' },
  { key: 'finished', label: '已完成', value: kpi.value.finished, unit: '单' },
  { key: 'active', label: '生产中', value: kpi.value.inProduction, unit: '单', accent: true },
  { key: 'delayed', label: '延期工单', value: kpi.value.delayed, unit: '单', warn: kpi.value.delayed > 0 },
  { key: 'rate', label: '总体完成率', value: kpi.value.completionRate, unit: '%', accent: true }
])

const trendData = computed(() => buildSevenDayOutput(mes.workReports))
const statusData = computed(() => buildStatusDistribution(workOrderList.value))
const stageData = computed(() => buildStageProgress(mes.dispatches))

const trendHasData = computed(() => trendData.value.values.some((v) => v > 0))
const statusHasData = computed(() => statusData.value.length > 0)
const stageHasData = computed(() => stageData.value.some((s) => s.planQty > 0))

const trendOption = computed(() => buildTrendOption(trendData.value.labels, trendData.value.values))
const donutOption = computed(() => buildDonutOption(statusData.value))
const stageBarOption = computed(() => buildStageBarOption(stageData.value))

const timelineSteps = computed(() => {
  if (!selected.value) return []
  return buildWorkOrderTimeline(selected.value, mes.dispatches, mes.workReports)
})

function rowProgress(row) {
  if (!row?.quantity) return 0
  return Math.min(100, Math.round(((row.completedQty || 0) / row.quantity) * 100))
}

function stepStatusClass(status) {
  if (status === '已完成') return 'done'
  if (status === '生产中' || status === '进行中') return 'active'
  if (status === '未派工') return 'idle'
  return 'pending'
}

function handleSearch() {
  pageNum.value = 1
}

function resetQuery() {
  keyword.value = ''
  pageNum.value = 1
}

function buildPayload(row) {
  return { workOrderId: row?.id }
}

async function removeRow(row) {
  if (!row) return
  await runDelete({
    action: 'deleteWorkOrder',
    payload: buildPayload(row),
    message: '确定删除该工单？关联派工、报工、质检记录将一并删除。',
    onSuccess: () => {
      if (selected.value?.id === row.id) selected.value = null
    }
  })
}

function removeSelected() {
  if (selected.value) removeRow(selected.value)
}

async function refreshData(silent = false) {
  if (!silent) initialLoading.value = true
  try {
    const data = await fetchMesSnapshot()
    if (data && typeof data === 'object') {
      const selectedId = selected.value?.id
      Object.keys(data).forEach((key) => {
        mes[key] = data[key]
      })
      if (selectedId) {
        selected.value = enrichedList.value.find((w) => w.id === selectedId) || null
      }
      lastRefresh.value = new Date().toLocaleString('zh-CN', { hour12: false })
      apiError.value = ''
    } else {
      apiError.value = '接口返回异常'
    }
  } catch (e) {
    apiError.value = e?.message || '无法连接生产数据接口'
  } finally {
    if (!silent) initialLoading.value = false
  }
}

function startPolling() {
  refreshData(false)
  pollTimer = setInterval(() => refreshData(true), POLL_MS)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

watch(enrichedList, () => {
  if (!selected.value?.id) return
  const hit = enrichedList.value.find((w) => w.id === selected.value.id)
  if (hit) selected.value = hit
})

onMounted(startPolling)
onUnmounted(stopPolling)
</script>

<style scoped>
.progress-page {
  --pp-bg: #f5f7fa;
  --pp-border: #e5e7eb;
  --pp-text: #1e293b;
  --pp-muted: #64748b;
  --pp-accent: #2563eb;
  --pp-accent2: #0d9488;
  min-height: calc(100vh - 130px);
  padding: 12px 14px 16px;
  background: var(--pp-bg);
  color: var(--pp-text);
}

.progress-kpi {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-bottom: 12px;
}

.progress-kpi__card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid var(--pp-border);
  border-radius: 4px;
}

.progress-kpi__card--accent .progress-kpi__value {
  color: var(--pp-accent);
}

.progress-kpi__card--warn .progress-kpi__value {
  color: #dc2626;
}

.progress-kpi__label {
  font-size: 12px;
  color: var(--pp-muted);
}

.progress-kpi__value {
  font-size: 22px;
  font-weight: 600;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.progress-kpi__value small {
  margin-left: 2px;
  font-size: 12px;
  font-weight: 400;
  color: var(--pp-muted);
}

.progress-charts {
  display: grid;
  grid-template-columns: 1fr 1fr 1.4fr;
  gap: 10px;
  margin-bottom: 12px;
}

.progress-panel {
  display: flex;
  flex-direction: column;
  min-height: 240px;
  background: #fff;
  border: 1px solid var(--pp-border);
  border-radius: 4px;
  overflow: hidden;
}

.progress-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f2f5;
}

.progress-panel__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--pp-text);
}

.progress-panel__meta {
  font-size: 11px;
  color: var(--pp-muted);
}

.progress-panel :deep(.progress-chart) {
  flex: 1;
  padding: 4px 8px 8px;
}

.progress-query {
  padding: 10px 12px;
  margin-bottom: 10px;
  background: #fff;
  border: 1px solid var(--pp-border);
  border-radius: 4px;
}

.progress-query :deep(.el-form-item) {
  margin-bottom: 0;
}

.progress-table-section {
  background: #fff;
  border: 1px solid var(--pp-border);
  border-radius: 4px;
  overflow: hidden;
}

.progress-table-head {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f2f5;
}

.progress-table-head__title {
  font-size: 13px;
  font-weight: 600;
}

.progress-table-head__meta {
  margin-left: auto;
  font-size: 12px;
  color: var(--pp-muted);
}

.progress-table-wrap {
  padding: 0 10px 10px;
}

.progress-table :deep(.el-table__header th) {
  background: #f8fafc !important;
  color: #334155;
  font-weight: 600;
}

.progress-cell {
  padding-right: 8px;
}

.progress-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 0 12px 12px;
}

.progress-timeline {
  margin-top: 10px;
  padding: 12px 14px;
  background: #fff;
  border: 1px solid var(--pp-border);
  border-radius: 4px;
}

.progress-timeline__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}

.progress-timeline__title {
  display: block;
  font-size: 14px;
  font-weight: 600;
}

.progress-timeline__sub {
  font-size: 12px;
  color: var(--pp-muted);
}

.progress-timeline__track {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.progress-timeline__step {
  display: flex;
  gap: 8px;
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #eef2f6;
  border-radius: 4px;
}

.progress-timeline__dot {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #e2e8f0;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

.progress-timeline__step--done .progress-timeline__dot {
  background: #ccfbf1;
  color: var(--pp-accent2);
}

.progress-timeline__step--active .progress-timeline__dot {
  background: #dbeafe;
  color: var(--pp-accent);
}

.progress-timeline__step--pending .progress-timeline__dot {
  background: #fef3c7;
  color: #b45309;
}

.progress-timeline__body {
  flex: 1;
  min-width: 0;
}

.progress-timeline__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 4px;
}

.progress-timeline__row strong {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.progress-timeline__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  margin-bottom: 6px;
  font-size: 11px;
  color: var(--pp-muted);
}

.progress-timeline__metrics em {
  font-style: normal;
  font-weight: 600;
  color: var(--pp-text);
}

@media (max-width: 1400px) {
  .progress-charts {
    grid-template-columns: 1fr 1fr;
  }
  .progress-panel--wide {
    grid-column: 1 / -1;
  }
  .progress-timeline__track {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 900px) {
  .progress-kpi {
    grid-template-columns: repeat(2, 1fr);
  }
  .progress-charts {
    grid-template-columns: 1fr;
  }
  .progress-timeline__track {
    grid-template-columns: 1fr;
  }
}
</style>
