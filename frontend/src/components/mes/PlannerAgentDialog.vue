<template>
  <el-dialog
    v-model="visible"
    title="智能排产"
    width="1120px"
    destroy-on-close
    class="planner-dialog"
    :close-on-click-modal="!previewLoading && phase !== 'analyzing'"
    :show-close="!previewLoading"
    @closed="onClosed"
  >
    <el-steps :active="phaseIndex" finish-status="success" simple class="planner-steps">
      <el-step title="排产条件" />
      <el-step title="Agent 分析" />
      <el-step title="方案选择" />
    </el-steps>

    <!-- 排产条件 -->
    <div v-if="phase === 'setup'" class="planner-phase">
      <div class="planner-toolbar">
        <el-select
          v-model="form.orderIds"
          multiple
          collapse-tags
          collapse-tags-tooltip
          class="planner-toolbar__order"
          placeholder="勾选一个或多个订单"
          @change="onOrderChange"
        >
          <el-option v-for="o in pendingOrders" :key="o.id" :label="`${o.id} · ${o.productModel} · ${o.quantity}台`" :value="o.id" />
        </el-select>
        <el-date-picker v-model="form.planStart" type="date" value-format="YYYY-MM-DD" placeholder="开始" />
        <el-date-picker v-model="form.planEnd" type="date" value-format="YYYY-MM-DD" placeholder="截止" />
      </div>

      <div v-if="selectedOrders.length" class="batch-summary">
        已选 <strong>{{ selectedOrders.length }}</strong> 个订单 · 合计 <strong>{{ totalQty }}</strong> 台
        <template v-if="isCombinedBatch">
          · 同型号联合排产 · 单批上限 <strong>{{ BATCH_SIZE }}</strong> 台 · 将拆分为 <strong>{{ combinedBatchCount }}</strong> 个联合批次
        </template>
        <template v-else>
          · 单批上限 <strong>{{ BATCH_SIZE }}</strong> 台 · 将拆分为 <strong>{{ batchPreview.length }}</strong> 个批次计划
        </template>
      </div>

      <div v-else class="setup-hint">
        选择待计划订单与排产周期，系统将校验物料齐套、设备产能并生成三方案对比。
      </div>
    </div>

    <!-- Agent 分析：左侧思维流 + 右侧证据库 -->
    <div v-else-if="phase === 'analyzing' || phase === 'review'" class="planner-phase planner-phase--analysis">
      <SchedulingThoughtPanel
        embedded
        title="智能排产引擎"
        subtitle="订单 → 库存 → 物料 → 设备 → 人员 → 车间 → 计划"
        :thought-stream="thoughtStream"
        :evidence-list="evidenceList"
        :all-evidence="allEvidence"
        :active-step-key="activeStepKey"
        :selected-step-key="selectedStepKey"
        :active-index="activeStep"
        :total-steps="7"
        :running="previewLoading"
        :pending-text="currentDetail"
        @select-step="selectStep"
      />
      <div v-if="phase === 'review'" class="review-summary">
        <span>已搜集证据 <strong>{{ allEvidence.length }}</strong> 条</span>
        <span>推演步骤 <strong>{{ thoughtStream.length }}</strong> 步</span>
        <span v-if="analysis?.recommendedPlanQty">建议产量 <strong>{{ analysis.recommendedPlanQty }}</strong> 台</span>
        <span v-if="analysis?.recommendation">{{ analysis.recommendation }}</span>
      </div>
    </div>

    <!-- 方案选择 -->
    <div v-else-if="phase === 'result'" class="planner-phase planner-phase--result">
      <div class="result-head">
        <div class="result-context">
          <span class="result-context__item"><em>订单</em>{{ selectedOrders.length }} 笔</span>
          <span class="result-context__sep" />
          <span class="result-context__item"><em>合计</em>{{ totalQty }} 台</span>
          <span class="result-context__sep" />
          <span class="result-context__item"><em>周期</em>{{ form.planStart }} ~ {{ form.planEnd }}</span>
          <span class="result-context__sep" />
          <span class="result-context__item"><em>批次</em>{{ batchPreview.length }} 个</span>
        </div>

        <div v-if="resultMetrics" class="result-kpis">
          <div class="result-kpi">
            <span class="result-kpi__label">准时率</span>
            <span class="result-kpi__val">{{ resultMetrics.onTimeRate }}<small>%</small></span>
          </div>
          <div class="result-kpi">
            <span class="result-kpi__label">设备利用率</span>
            <span class="result-kpi__val">{{ resultMetrics.equipmentUtilization }}<small>%</small></span>
          </div>
          <div class="result-kpi">
            <span class="result-kpi__label">换线次数</span>
            <span class="result-kpi__val">{{ resultMetrics.lineChanges }}</span>
          </div>
          <div class="result-kpi" :class="{ 'result-kpi--warn': resultMetrics.materialShortage > 0 }">
            <span class="result-kpi__label">缺料数</span>
            <span class="result-kpi__val">{{ resultMetrics.materialShortage }}</span>
          </div>
          <div class="result-kpi" :class="{ 'result-kpi--warn': resultMetrics.conflictCount > 0 }">
            <span class="result-kpi__label">冲突数</span>
            <span class="result-kpi__val">{{ resultMetrics.conflictCount }}</span>
          </div>
        </div>
      </div>

      <div v-if="schemeRecommend" class="result-recommend">
        <div class="result-recommend__icon">✓</div>
        <div class="result-recommend__body">
          <div class="result-recommend__title">
            推荐方案：<strong>{{ schemeRecommend.label }}</strong>
          </div>
          <div class="result-recommend__summary">{{ schemeRecommend.summary }}</div>
          <div v-if="schemeData?.evidence?.length" class="result-evidence">
            <span v-for="(e, i) in schemeData.evidence" :key="i" class="result-evidence__chip">{{ e.label }} {{ e.value }}</span>
          </div>
        </div>
      </div>

      <div v-if="selectedScheme?.conflicts?.length" class="result-risk">
        <div class="result-risk__title">风险提示</div>
        <div v-for="(c, i) in selectedScheme.conflicts" :key="i" class="result-risk__item" :class="`result-risk__item--${c.level}`">
          {{ c.label }}：{{ c.detail }}
        </div>
      </div>

      <div class="result-section">
        <div class="result-section__hd">
          <span class="result-section__title">方案比较</span>
          <span class="result-section__hint">点击卡片选择排产策略</span>
        </div>
        <div class="scheme-cards">
          <button
            v-for="s in schemeData?.schemes || []"
            :key="s.key"
            type="button"
            class="scheme-card"
            :class="{
              'scheme-card--active': selectedScheme?.key === s.key,
              'scheme-card--best': schemeData?.conclusion?.key === s.key
            }"
            @click="onSchemeKeySelect(s.key)"
          >
            <div class="scheme-card__top">
              <span class="scheme-card__name">{{ s.label }}</span>
              <el-tag v-if="schemeData?.conclusion?.key === s.key" size="small" type="success" effect="plain">推荐</el-tag>
              <el-tag v-else-if="!s.conflicts?.length" size="small" type="info" effect="plain">可用</el-tag>
              <el-tag v-else :type="s.canSubmit ? 'warning' : 'danger'" size="small" effect="plain">
                {{ s.canSubmit ? '有提示' : '需确认' }}
              </el-tag>
            </div>
            <div class="scheme-card__metrics">
              <div class="scheme-card__metric">
                <span>完工</span><b>{{ s.finishDate }}</b>
              </div>
              <div class="scheme-card__metric">
                <span>利用率</span><b>{{ s.equipmentUtilization }}%</b>
              </div>
              <div class="scheme-card__metric">
                <span>缺料</span><b :class="{ 'text-warn': s.materialShortage > 0 }">{{ s.materialShortage }}</b>
              </div>
              <div class="scheme-card__metric">
                <span>延期</span><b :class="{ 'text-warn': s.delayDays > 0 }">{{ s.delayDays }}天</b>
              </div>
              <div class="scheme-card__metric">
                <span>换线</span><b>{{ s.lineChanges }}</b>
              </div>
            </div>
            <p class="scheme-card__desc">{{ s.summary }}</p>
          </button>
        </div>
      </div>

      <div v-if="batchPreview.length" class="result-section">
        <div class="result-section__hd">
          <span class="result-section__title">
            {{ isCombinedBatch ? '联合批次预览' : '批次拆分预览' }}
          </span>
          <span class="result-section__hint">单批上限 {{ BATCH_SIZE }} 台</span>
        </div>
        <div class="result-table-wrap">
          <ExcelGridTable :columns="batchColumns" :data="batchPreview" :show-row-no="true" compact>
            <template #batchNo="{ row }">
              <span class="batch-badge">{{ row.batchNo }}/{{ row.batchCount }}</span>
            </template>
            <template #batchQty="{ row }"><strong>{{ row.batchQty }}</strong></template>
          </ExcelGridTable>
        </div>
      </div>

      <div v-if="selectedScheme" class="result-footer-bar">
        <span class="result-footer-bar__label">已选策略</span>
        <span class="result-footer-bar__value">{{ selectedScheme.label }}</span>
        <span class="result-footer-bar__note">将应用到全部 {{ batchPreview.length }} 个批次计划</span>
      </div>
    </div>

    <template #footer>
      <template v-if="phase === 'setup'">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :disabled="!form.orderIds.length" @click="startAnalysis">
          开始 Agent 分析
        </el-button>
      </template>
      <template v-else-if="phase === 'analyzing'">
        <el-button :disabled="previewLoading" @click="visible = false">取消</el-button>
        <el-button type="primary" loading disabled>分析推演中…</el-button>
      </template>
      <template v-else-if="phase === 'review'">
        <el-button @click="backToSetup">返回修改</el-button>
        <el-button @click="restartAnalysis">重新分析</el-button>
        <el-button type="primary" :loading="schemeLoading" @click="confirmAndLoadSchemes">
          确认分析，进入方案选择
        </el-button>
      </template>
      <template v-else>
        <el-button @click="backToReview">查看分析过程</el-button>
        <el-button @click="restartAnalysis">重新分析</el-button>
        <el-button type="success" :loading="submitLoading" :disabled="!selectedScheme || !batchPreview.length" @click="runCreate">
          确认方案并提交生产主管（{{ submitPlanCount }} 个计划）
        </el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { useSchedulingFlow } from '@/composables/useSchedulingFlow'
import SchedulingThoughtPanel from '@/components/mes/SchedulingThoughtPanel.vue'
import ExcelGridTable from '@/components/mes/ExcelGridTable.vue'
import { postComparePlanSchemes, postSaveBatchProductionPlans } from '@/api/planner'

/** 单批次最大生产数量 */
const BATCH_SIZE = 500

const props = defineProps({
  modelValue: Boolean,
  defaultOrderId: { type: String, default: '' },
  defaultOrderIds: { type: Array, default: () => [] },
  combinedBatch: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'success'])

const mes = useMesStore()
const userStore = useUserStore()
const previewLoading = ref(false)
const schemeLoading = ref(false)
const submitLoading = ref(false)
const analysis = ref(null)
const schemeData = ref(null)
const selectedScheme = ref(null)
const phase = ref('setup')

const phaseIndex = computed(() => {
  if (phase.value === 'setup') return 0
  if (phase.value === 'analyzing' || phase.value === 'review') return 1
  return 2
})

const resultMetrics = computed(() => {
  const s = selectedScheme.value
  if (!s) return null
  const delay = Number(s.delayDays) || 0
  return {
    onTimeRate: delay === 0 ? 100 : Math.max(0, 100 - delay * 12),
    equipmentUtilization: s.equipmentUtilization ?? '—',
    lineChanges: s.lineChanges ?? 0,
    materialShortage: s.materialShortage ?? 0,
    conflictCount: (s.conflicts || []).length
  }
})

const schemeRecommend = computed(() => {
  const c = schemeData.value?.conclusion
  if (!c) return null
  let summary = c.summary || ''
  if (c.label && summary.startsWith(c.label)) {
    summary = summary.slice(c.label.length).replace(/^[：:\s]+/, '')
  }
  return { label: c.label, summary }
})

const {
  activeStep,
  activeStepKey,
  selectedStepKey,
  thoughtStream,
  evidenceList,
  allEvidence,
  currentDetail,
  reset: resetFlow,
  runAnimatedPreview,
  selectStep
} = useSchedulingFlow()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const pendingOrders = computed(() => mes.pendingPlanOrders)

const form = reactive({
  orderIds: [],
  planStart: new Date().toISOString().slice(0, 10),
  planEnd: new Date(Date.now() + 14 * 86400000).toISOString().slice(0, 10),
  plannedQty: 0
})

const selectedOrders = computed(() =>
  form.orderIds
    .map((id) => pendingOrders.value.find((o) => o.id === id))
    .filter(Boolean)
)

const totalQty = computed(() =>
  selectedOrders.value.reduce((sum, o) => sum + (Number(o.quantity) || 0), 0)
)

const isCombinedBatch = computed(() =>
  props.combinedBatch && selectedOrders.value.length > 1
    && new Set(selectedOrders.value.map((o) => o.productModel)).size === 1
)

const combinedBatchCount = computed(() => {
  if (!isCombinedBatch.value) return 0
  return Math.ceil(totalQty.value / BATCH_SIZE)
})

const submitPlanCount = computed(() => batchPreview.value.length)

const batchColumns = computed(() => {
  if (isCombinedBatch.value) {
    return [
      { prop: 'batchNo', label: '联合批次', width: '80px', align: 'center' },
      { prop: 'orderId', label: '订单编号', width: '130px', align: 'left' },
      { prop: 'productModel', label: '产品型号', align: 'left' },
      { prop: 'batchQty', label: '本批数量', width: '80px', align: 'right' },
      { prop: 'batchTotal', label: '联合批次合计', width: '100px', align: 'right' },
      { prop: 'window', label: '预计周期', width: '180px', align: 'center' }
    ]
  }
  return [
    { prop: 'orderId', label: '订单编号', width: '130px', align: 'left' },
    { prop: 'productModel', label: '产品型号', align: 'left' },
    { prop: 'orderQty', label: '订单数量', width: '80px', align: 'right' },
    { prop: 'batchNo', label: '批次', width: '70px', align: 'center' },
    { prop: 'batchQty', label: '本批数量', width: '80px', align: 'right' },
    { prop: 'window', label: '预计周期', width: '180px', align: 'center' }
  ]
})

function buildBatchWindow(batchNo, batchCount, start, end) {
  const totalDays = Math.max(1, Math.round((end - start) / 86400000) + 1)
  const daysPerBatch = Math.max(1, Math.floor(totalDays / batchCount))
  const bStart = new Date(start.getTime() + (batchNo - 1) * daysPerBatch * 86400000)
  const bEnd = batchNo === batchCount ? end : new Date(bStart.getTime() + (daysPerBatch - 1) * 86400000)
  return `${fmtDate(bStart)} ~ ${fmtDate(bEnd > end ? end : bEnd)}`
}

const batchPreview = computed(() => {
  const rows = []
  const start = form.planStart ? new Date(form.planStart) : new Date()
  const end = form.planEnd ? new Date(form.planEnd) : new Date(Date.now() + 14 * 86400000)

  if (isCombinedBatch.value) {
    const orders = selectedOrders.value.map((o) => ({
      id: o.id,
      productModel: o.productModel,
      remaining: Number(o.quantity) || 0
    })).filter((o) => o.remaining > 0)
    const total = orders.reduce((sum, o) => sum + o.remaining, 0)
    if (total <= 0) return rows

    const batchCount = Math.ceil(total / BATCH_SIZE)
    let orderIdx = 0
    let orderRemain = orders[0]?.remaining || 0
    let globalLeft = total

    for (let b = 1; b <= batchCount; b++) {
      const batchCapacity = Math.min(BATCH_SIZE, globalLeft)
      globalLeft -= batchCapacity
      const window = buildBatchWindow(b, batchCount, start, end)
      let capacityLeft = batchCapacity
      const batchLines = []

      while (capacityLeft > 0 && orderIdx < orders.length) {
        const take = Math.min(orderRemain, capacityLeft)
        batchLines.push({
          orderId: orders[orderIdx].id,
          productModel: orders[orderIdx].productModel,
          batchQty: take,
          batchNo: b,
          batchCount,
          batchTotal: batchCapacity,
          window
        })
        capacityLeft -= take
        orderRemain -= take
        if (orderRemain <= 0) {
          orderIdx += 1
          orderRemain = orders[orderIdx]?.remaining || 0
        }
      }
      rows.push(...batchLines)
    }
    return rows
  }

  const totalDays = Math.max(1, Math.round((end - start) / 86400000) + 1)
  for (const o of selectedOrders.value) {
    const qty = Number(o.quantity) || 0
    if (qty <= 0) continue
    const batchCount = Math.ceil(qty / BATCH_SIZE)
    const daysPerBatch = Math.max(1, Math.floor(totalDays / batchCount))
    let remaining = qty
    for (let b = 1; b <= batchCount; b++) {
      const batchQty = Math.min(BATCH_SIZE, remaining)
      remaining -= batchQty
      const bStart = new Date(start.getTime() + (b - 1) * daysPerBatch * 86400000)
      const bEnd = b === batchCount ? end : new Date(bStart.getTime() + (daysPerBatch - 1) * 86400000)
      rows.push({
        orderId: o.id,
        productModel: o.productModel,
        orderQty: qty,
        batchNo: b,
        batchCount,
        batchQty,
        window: `${fmtDate(bStart)} ~ ${fmtDate(bEnd > end ? end : bEnd)}`
      })
    }
  }
  return rows
})

function fmtDate(d) {
  return d.toISOString().slice(0, 10)
}

watch(() => props.modelValue, (open) => {
  if (open) {
    const ids = props.defaultOrderIds?.length
      ? [...props.defaultOrderIds]
      : (props.defaultOrderId ? [props.defaultOrderId] : [])
    const validIds = ids.filter((id) => pendingOrders.value.some((o) => o.id === id))
    form.orderIds = validIds.length ? validIds : (pendingOrders.value[0]?.id ? [pendingOrders.value[0].id] : [])
    analysis.value = null
    schemeData.value = null
    selectedScheme.value = null
    phase.value = 'setup'
    resetFlow()
  }
})

function onOrderChange() {
  if (phase.value === 'result' || phase.value === 'review') {
    analysis.value = null
    schemeData.value = null
    selectedScheme.value = null
    phase.value = 'setup'
    resetFlow()
  }
}

function onSchemeKeySelect(key) {
  const row = schemeData.value?.schemes?.find((s) => s.key === key)
  selectedScheme.value = row || null
}

function onClosed() {
  analysis.value = null
  schemeData.value = null
  selectedScheme.value = null
  phase.value = 'setup'
  resetFlow()
}

function backToSetup() {
  phase.value = 'setup'
}

function backToReview() {
  phase.value = 'review'
}

function restartAnalysis() {
  analysis.value = null
  schemeData.value = null
  selectedScheme.value = null
  resetFlow()
  startAnalysis()
}

async function startAnalysis() {
  const firstOrderId = form.orderIds[0]
  if (!firstOrderId || !form.planStart || !form.planEnd) {
    ElMessage.warning('请选择订单并设置排产周期')
    return
  }
  phase.value = 'analyzing'
  previewLoading.value = true
  analysis.value = null
  schemeData.value = null
  selectedScheme.value = null
  resetFlow()
  try {
    analysis.value = await runAnimatedPreview(async () => {
      return await mes.previewPlanAgent(
        { orderId: firstOrderId, planStart: form.planStart, planEnd: form.planEnd, plannedQty: 0 },
        userStore.username,
        userStore.roleKey
      )
    })
    phase.value = 'review'
  } catch (e) {
    phase.value = 'setup'
    const msg = e?.message || ''
    if (!msg.includes('订单状态不允许 Agent 排产')) {
      ElMessage.error(msg || '分析失败')
    }
  } finally {
    previewLoading.value = false
  }
}

async function confirmAndLoadSchemes() {
  const firstOrderId = form.orderIds[0]
  if (!firstOrderId || !analysis.value) return
  schemeLoading.value = true
  try {
    schemeData.value = await postComparePlanSchemes({
      orderId: firstOrderId,
      planStart: form.planStart,
      planEnd: form.planEnd,
      plannedQty: 0
    })
    selectedScheme.value = schemeData.value?.schemes?.find((s) => s.key === schemeData.value?.conclusion?.key)
      || schemeData.value?.schemes?.[0]
      || null
    phase.value = 'result'
  } catch (e) {
    ElMessage.error(e?.message || '方案生成失败')
  } finally {
    schemeLoading.value = false
  }
}

async function runCreate() {
  if (!selectedScheme.value) {
    ElMessage.warning('请先选择一种排产方案')
    return
  }
  if (!batchPreview.value.length) {
    ElMessage.warning('请先勾选订单')
    return
  }
  const conflicts = selectedScheme.value.conflicts || []
  if (conflicts.length) {
    const lines = conflicts.map((c) => `· ${c.label}：${c.detail}`).join('\n')
    try {
      await ElMessageBox.confirm(
        `所选「${selectedScheme.value.label}」存在以下提示，确认仍要提交吗？\n\n${lines}`,
        '确认排产方案',
        { type: 'warning', confirmButtonText: '确认提交', cancelButtonText: '返回修改' }
      )
    } catch {
      return
    }
  }
  submitLoading.value = true
  try {
    const res = await postSaveBatchProductionPlans({
      orders: selectedOrders.value.map((o) => ({ orderId: o.id, plannedQty: Number(o.quantity) || 0 })),
      planStart: form.planStart,
      planEnd: form.planEnd,
      batchSize: BATCH_SIZE,
      schedulingMode: selectedScheme.value.key,
      saveAction: 'submit',
      combinedBatch: isCombinedBatch.value,
      operator: userStore.username
    })
    ElMessage.success(res?.message || `已生成 ${submitPlanCount.value} 个计划并提交生产主管`)
    visible.value = false
    emit('success')
    await mes.hydrateFromApi()
  } catch (e) {
    ElMessage.error(e?.message || '创建计划失败')
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.planner-dialog :deep(.el-dialog__body) {
  padding: 16px 20px 12px;
}

.planner-steps {
  margin-bottom: 18px;
  padding: 0 4px;
}

.planner-phase {
  min-height: 200px;
}

.planner-phase--result {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.planner-phase--analysis {
  min-height: 460px;
}

/* —— 方案选择页 —— */
.result-head {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
  overflow: hidden;
}

.result-context {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 0;
  padding: 10px 14px;
  border-bottom: 1px solid #ebeef5;
  font-size: 13px;
  color: #606266;
}

.result-context__item em {
  font-style: normal;
  color: #909399;
  margin-right: 4px;
}

.result-context__sep {
  width: 1px;
  height: 12px;
  background: #dcdfe6;
  margin: 0 14px;
}

.result-kpis {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 0;
  background: #fff;
}

.result-kpi {
  padding: 12px 8px;
  text-align: center;
  border-right: 1px solid #f0f0f0;
}

.result-kpi:last-child {
  border-right: none;
}

.result-kpi__label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.result-kpi__val {
  font-size: 22px;
  font-weight: 600;
  color: #2d8a66;
  line-height: 1;
}

.result-kpi__val small {
  font-size: 13px;
  font-weight: 500;
  margin-left: 1px;
}

.result-kpi--warn .result-kpi__val {
  color: #e6a23c;
}

.result-recommend {
  display: flex;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #d4edda;
  border-radius: 4px;
  background: #f6ffed;
}

.result-recommend__icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #2d8a66;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.result-recommend__title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}

.result-recommend__title strong {
  color: #2d8a66;
}

.result-recommend__summary {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.result-evidence {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.result-evidence__chip {
  display: inline-block;
  padding: 2px 8px;
  font-size: 12px;
  color: #606266;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 2px;
}

.result-risk {
  padding: 10px 14px;
  border: 1px solid #faecd8;
  border-radius: 4px;
  background: #fdf6ec;
  font-size: 12px;
}

.result-risk__title {
  font-weight: 600;
  color: #e6a23c;
  margin-bottom: 4px;
}

.result-risk__item--warning { color: #e6a23c; }
.result-risk__item--danger { color: #f56c6c; }

.result-section__hd {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
}

.result-section__title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.result-section__title::before {
  content: '';
  display: inline-block;
  width: 3px;
  height: 14px;
  background: #2d8a66;
  margin-right: 8px;
  vertical-align: -2px;
  border-radius: 1px;
}

.result-section__hint {
  font-size: 12px;
  color: #909399;
}

.scheme-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.scheme-card {
  text-align: left;
  padding: 14px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.scheme-card:hover {
  border-color: #b3d8c4;
}

.scheme-card--active {
  border-color: #2d8a66;
  box-shadow: 0 0 0 1px #2d8a66;
  background: #f6ffed;
}

.scheme-card--best:not(.scheme-card--active) {
  border-color: #c2e7b0;
}

.scheme-card__top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.scheme-card__name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.scheme-card__metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px 6px;
  margin-bottom: 10px;
}

.scheme-card__metric {
  font-size: 12px;
  color: #909399;
}

.scheme-card__metric span {
  display: block;
  margin-bottom: 2px;
}

.scheme-card__metric b {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.scheme-card__desc {
  margin: 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.text-warn { color: #e6a23c !important; }

.result-table-wrap {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.batch-badge {
  display: inline-block;
  padding: 1px 6px;
  font-size: 12px;
  color: #2d8a66;
  background: #f0f9eb;
  border: 1px solid #c2e7b0;
  border-radius: 2px;
}

.result-footer-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fafafa;
  font-size: 13px;
}

.result-footer-bar__label {
  color: #909399;
}

.result-footer-bar__value {
  font-weight: 600;
  color: #2d8a66;
}

.result-footer-bar__note {
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}

@media (max-width: 900px) {
  .result-kpis { grid-template-columns: repeat(3, 1fr); }
  .scheme-cards { grid-template-columns: 1fr; }
}

.review-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 10px;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  background: #f0fdf4;
  font-size: 13px;
  color: #374151;
}

.review-summary strong {
  color: #15803d;
}

.analyzing-tip { margin: 12px 0 0; text-align: center; font-size: 13px; color: #909399; }

.setup-hint {
  padding: 24px 16px;
  text-align: center;
  font-size: 13px;
  color: #909399;
  background: #fafafa;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
}

.scheme-data-note { font-size: 12px; color: #909399; margin-bottom: 8px; }
.batch-summary {
  margin-top: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  font-size: 13px;
  color: #409eff;
}
.batch-summary strong { font-size: 15px; }

.planner-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.planner-toolbar__order {
  flex: 1;
  min-width: 280px;
}
</style>
