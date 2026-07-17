<template>
  <el-dialog
    v-model="visible"
    class="smart-dispatch-dialog"
    :title="dialogTitle"
    width="1580px"
    destroy-on-close
    :close-on-click-modal="!loading && phase !== 'analyzing'"
    @open="onOpen"
    @closed="resetState"
  >
    <el-steps :active="phaseIndex" finish-status="success" simple class="dispatch-steps">
      <el-step title="计划确认" />
      <el-step :title="manual ? '手动配置' : 'Agent 分析'" />
      <el-step :title="resultStepTitle" />
    </el-steps>

    <div class="dispatch-toolbar">
      <el-select
        v-if="planOptions.length > 1"
        v-model="selectedPlanId"
        class="dispatch-toolbar__plan"
        filterable
        :disabled="loading"
        @change="loadPreview"
      >
        <el-option v-for="p in planOptions" :key="p.id" :label="`${p.id} · ${p.productModel}`" :value="p.id" />
      </el-select>
      <span v-else-if="preview || selectedPlanId" class="plan-label">
        {{ preview?.planId || selectedPlanId }} · {{ preview?.productModel || '—' }}
        <template v-if="preview?.planQuantity"> · {{ preview.planQuantity }}台</template>
      </span>
    </div>

    <!-- Agent 分析工作流 -->
    <div v-if="phase === 'analyzing'" class="dispatch-phase dispatch-phase--analysis">
      <div class="dispatch-workflow">
        <div
          v-for="(step, idx) in flowSteps"
          :key="step.key"
          class="dispatch-workflow__item"
          :class="workflowItemClass(idx)"
        >
          <div class="dispatch-workflow__node">
            <span class="dispatch-workflow__index">{{ idx + 1 }}</span>
          </div>
          <div class="dispatch-workflow__body">
            <div class="dispatch-workflow__title">{{ step.title }}</div>
            <div v-if="workflowItemDetail(step, idx)" class="dispatch-workflow__detail">
              {{ workflowItemDetail(step, idx) }}
            </div>
          </div>
          <div v-if="idx < flowSteps.length - 1" class="dispatch-workflow__connector" />
        </div>
      </div>

      <SchedulingThoughtPanel
        embedded
        right-mode="dispatch-validation"
        :title="engineTitle"
        subtitle="工艺 → 设备 → 人员 → 匹配 → 推荐"
        :dispatch-preview="preview"
        :dispatch-mes="mes"
        :thought-stream="thoughtStream"
        :evidence-list="evidenceList"
        :all-evidence="allEvidence"
        :active-step-key="activeStepKey"
        :selected-step-key="selectedStepKey"
        :active-index="activeStep"
        :total-steps="flowSteps.length || 5"
        :running="loading"
        :pending-text="currentDetail"
        @select-step="selectStep"
      />
    </div>

    <!-- 派工结果 -->
    <div v-else-if="phase === 'result'" class="dispatch-phase dispatch-phase--result">
      <div class="result-head">
        <div class="result-context">
          <span class="result-context__item"><em>计划</em>{{ preview.planId }}</span>
          <span class="result-context__sep" />
          <span class="result-context__item"><em>产品</em>{{ preview.productModel }}</span>
          <span class="result-context__sep" />
          <span class="result-context__item"><em>数量</em>{{ preview.planQuantity }} 台</span>
          <span class="result-context__sep" />
          <span class="result-context__item"><em>工序</em>{{ editableRows.length }} 道</span>
        </div>
      </div>

      <el-alert v-if="preview?.summary" :title="preview.summary" type="success" :closable="false" show-icon class="summary-bar" />

      <el-alert
        v-if="blockingConflicts.length"
        type="error"
        :closable="false"
        show-icon
        class="summary-bar"
        title="以下问题需处理后才能确认"
      >
        <ul class="dispatch-conflict-list">
          <li v-for="(c, i) in blockingConflicts" :key="i">{{ c.detail || c.label }}</li>
        </ul>
      </el-alert>

      <el-alert
        v-else-if="warningConflicts.length"
        type="warning"
        :closable="false"
        show-icon
        class="summary-bar"
        title="存在提示项，确认前请留意"
      >
        <ul class="dispatch-conflict-list">
          <li v-for="(c, i) in warningConflicts" :key="i">{{ c.detail || c.label }}</li>
        </ul>
      </el-alert>

      <el-table :data="editableRows" border class="dispatch-table">
        <el-table-column prop="processStep" label="工序" min-width="120" />
        <el-table-column label="车间" min-width="200">
          <template #default="{ row }"><el-input v-model="row.workshopName" /></template>
        </el-table-column>
        <el-table-column label="设备" min-width="260">
          <template #default="{ row }">
            <el-select
              v-model="row.equipmentCode"
              filterable
              class="dispatch-table__select"
              :fit-input-width="false"
              @change="(code) => onEquipmentPick(row, code)"
            >
              <el-option
                v-for="eq in equipmentOptionsForRow(row)"
                :key="eq.id"
                :label="`${eq.name}（${eq.id}）`"
                :value="eq.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作员" min-width="260">
          <template #default="{ row }">
            <el-select
              v-model="row.recommendedOperator"
              class="dispatch-table__select"
              :class="{ 'dispatch-table__select--danger': !row.recommendedOperator }"
              :fit-input-width="false"
              @change="(v) => onOperatorPick(row, v)"
            >
              <el-option
                v-for="u in operatorOptionsForRow(row)"
                :key="u.username"
                :label="operatorLabel(u)"
                :value="u.username"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="120" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.planQty"
              :min="1"
              controls-position="right"
              @change="() => onPlanQtyChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="预计工时(h)" width="140" align="center">
          <template #default="{ row }">
            <el-input-number
              :model-value="row.estimatedHours"
              disabled
              controls-position="right"
              class="dispatch-hours-readonly"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-else class="dispatch-phase dispatch-phase--idle">
      <div class="idle-hint">{{ idleHint }}</div>
    </div>

    <template #footer>
      <template v-if="phase === 'analyzing' && loading">
        <el-button :disabled="loading" @click="visible = false">取消</el-button>
        <el-button type="primary" loading disabled>分析推演中…</el-button>
      </template>
      <template v-else-if="phase === 'analyzing' && !loading">
        <el-button @click="viewMode = 'auto'">返回{{ resultStepTitle }}</el-button>
        <el-button v-if="!manual" :loading="loading" @click="loadPreview">重新分析</el-button>
      </template>
      <template v-else-if="phase === 'result'">
        <el-button v-if="!manual" @click="showAnalysisReview">查看分析过程</el-button>
        <el-button :loading="loading" @click="manual ? loadManualPreview() : loadPreview()">
          {{ manual ? '重新配置' : '重新分析' }}
        </el-button>
        <el-button type="success" :loading="confirming" :disabled="!canSubmit" :title="submitBlockReason" @click="confirm">{{ confirmLabel }}</el-button>
      </template>
      <template v-else>
        <el-button @click="visible = false">关闭</el-button>
        <el-button type="primary" :disabled="!selectedPlanId" :loading="loading" @click="manual ? loadManualPreview() : loadPreview()">
          {{ manual ? '加载工序' : '开始分析' }}
        </el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { postValidateSmartDispatch, fetchManagerPlanContext } from '@/api/mes'
import { operatorBinding, operatorLabel, operatorsForProcessStep } from '@/utils/operatorWorkshop'
import { useSchedulingFlow, DISPATCH_FLOW_TEMPLATE } from '@/composables/useSchedulingFlow'
import SchedulingThoughtPanel from '@/components/mes/SchedulingThoughtPanel.vue'

const props = defineProps({
  modelValue: Boolean,
  defaultPlanId: { type: String, default: '' },
  /** 手动模式：跳过 Agent 分析，自行选择设备与人员 */
  manual: { type: Boolean, default: false },
  /** create=生成工单，dispatch=对已下达工单派工 */
  intent: { type: String, default: 'create' }
})
const emit = defineEmits(['update:modelValue', 'success'])

const mes = useMesStore()
const userStore = useUserStore()
const loading = ref(false)
const confirming = ref(false)
const preview = ref(null)
const selectedPlanId = ref('')
const editableRows = ref([])
const validation = ref(null)
const viewMode = ref('auto')

const {
  activeStep, activeStepKey, selectedStepKey, thoughtStream, evidenceList, allEvidence,
  currentDetail, flowSteps, reset: resetFlow, runAnimatedPreview, selectStep
} = useSchedulingFlow(DISPATCH_FLOW_TEMPLATE)

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const phase = computed(() => {
  if (viewMode.value === 'analysis') return 'analyzing'
  if (loading.value) return 'analyzing'
  if (editableRows.value.length) return 'result'
  return 'idle'
})

const phaseIndex = computed(() => {
  if (phase.value === 'analyzing') return 1
  if (phase.value === 'result') return 2
  return 0
})

const planOptions = computed(() =>
  mes.pendingManagerPlans.length ? mes.pendingManagerPlans : mes.plans.filter((p) => p.status === '已发布')
)
const isDispatchIntent = computed(() => props.intent === 'dispatch')
const dialogTitle = computed(() => {
  if (isDispatchIntent.value) return props.manual ? '手动派工' : '一键派工'
  return props.manual ? '手动生成工单' : '一键生成工单'
})
const resultStepTitle = computed(() => (isDispatchIntent.value ? '派工结果' : '工单结果'))
const confirmLabel = computed(() => (isDispatchIntent.value ? '确认派工' : '确认生成工单'))
const engineTitle = computed(() => (isDispatchIntent.value ? '智能派工引擎' : '智能工单引擎'))
const idleHint = computed(() => {
  if (props.manual) {
    return isDispatchIntent.value
      ? '选择计划后加载工序，手动指定设备与操作员并下发给生产操作员。'
      : '选择计划后加载工序，手动指定每道工序的设备、车间与操作员。'
  }
  return isDispatchIntent.value
    ? 'Agent 将按工艺路线、设备负荷与人员技能推演派工方案，并下发给生产操作员。'
    : 'Agent 将按工艺路线、设备负荷与人员技能逐步推演工单安排方案。'
})
const canSubmit = computed(() => {
  if (!editableRows.value.length) return false
  const rowsReady = editableRows.value.every((row) => row.equipmentCode && row.recommendedOperator)
  if (!rowsReady) return false
  if (props.manual) return true
  return validation.value?.canSubmit !== false
})

const blockingConflicts = computed(() =>
  (validation.value?.conflicts || []).filter((c) => c.level === 'danger')
)
const warningConflicts = computed(() =>
  (validation.value?.conflicts || []).filter((c) => c.level === 'warning')
)
const submitBlockReason = computed(() => {
  if (!editableRows.value.length) return '暂无派工明细'
  const missing = editableRows.value.filter((row) => !row.equipmentCode || !row.recommendedOperator)
  if (missing.length) {
    const steps = missing.map((row) => row.processStep).filter(Boolean).join('、')
    return steps ? `请为「${steps}」补全设备与操作员` : '请补全设备与操作员'
  }
  if (blockingConflicts.value.length) {
    return blockingConflicts.value[0]?.detail || blockingConflicts.value[0]?.label || '存在冲突'
  }
  return ''
})

const STEP_EQUIP_TYPE = {
  主板装配: '主板线',
  电源板装配: '电源板线',
  接口板装配: '接口板线',
  显示屏加工: '显示屏线',
  面板贴附: '贴附机',
  外壳装配: '外壳线',
  整机组装: '组装线',
  支架底座装配: '支架线'
}

function workflowItemClass(idx) {
  const revealed = thoughtStream.value.length
  if (!loading.value && revealed > idx) return 'dispatch-workflow__item--done'
  if (loading.value) {
    if (idx < revealed - 1) return 'dispatch-workflow__item--done'
    if (idx === revealed || (revealed === 0 && idx === 0)) return 'dispatch-workflow__item--active'
    if (idx < revealed) return 'dispatch-workflow__item--done'
  }
  return 'dispatch-workflow__item--pending'
}

function workflowItemDetail(step, idx) {
  const thought = thoughtStream.value[idx]
  if (thought?.summary) return thought.summary
  if (thought?.thought) return thought.thought
  if (loading.value && idx === thoughtStream.value.length) return currentDetail.value
  return step.detail || ''
}

function showAnalysisReview() {
  viewMode.value = 'analysis'
}

function roundHours(n) {
  return Math.round(Number(n) * 10) / 10
}

function resolveHoursPerUnit(row) {
  if (row.hoursPerUnit > 0) return row.hoursPerUnit
  if (row.standardWorkHours > 0) return Number(row.standardWorkHours)
  const qty = Number(row.planQty) || 1
  const hours = Number(row.estimatedHours) || 0
  if (hours > 0 && qty > 0) return roundHours(hours / qty)
  return 1
}

function syncEstimatedHours(row) {
  const perUnit = resolveHoursPerUnit(row)
  row.hoursPerUnit = perUnit
  const qty = Math.max(1, Number(row.planQty) || 1)
  row.planQty = qty
  row.estimatedHours = roundHours(perUnit * qty)
}

function normalizeEditableRow(row, defaultQty = 1) {
  const normalized = { ...row }
  normalized.planQty = Math.max(1, Number(normalized.planQty) || defaultQty || 1)
  syncEstimatedHours(normalized)
  return normalized
}

function onPlanQtyChange(row) {
  syncEstimatedHours(row)
  validateRows()
}

function mapEditableRows(rows, defaultQty = 1) {
  return rows.map((r) => normalizeEditableRow(r, defaultQty))
}

function isReadableText(text) {
  const s = String(text || '')
  return s.length > 0 && !s.includes('?') && !s.includes('？')
}

function equipmentOptionsForRow(row) {
  const expectType = STEP_EQUIP_TYPE[row.processStep] || ''
  return mes.equipment.filter((eq) => {
    if (!isReadableText(eq.name)) return false
    if (!expectType) return true
    return !eq.type || eq.type === expectType
  })
}

function operatorOptionsForRow(row) {
  const allowed = new Set(operatorsForProcessStep(row.processStep))
  return mes.boundOperatorUsers.filter((u) => allowed.has(u.username))
}

function onEquipmentPick(row, code) {
  const eq = mes.equipment.find((e) => e.id === code)
  row.equipmentCode = code || ''
  row.equipmentName = eq?.name || ''
  if (eq?.line && !row.workshopName) row.workshopName = eq.line
}

let validateTimer = null
watch(editableRows, () => {
  clearTimeout(validateTimer)
  validateTimer = setTimeout(validateRows, 300)
}, { deep: true })

function resetState() {
  preview.value = null
  editableRows.value = []
  validation.value = null
  viewMode.value = 'auto'
  resetFlow()
}

function onOpen() {
  resetState()
  selectedPlanId.value = props.defaultPlanId || planOptions.value[0]?.id || ''
  if (!selectedPlanId.value) return
  if (props.manual) loadManualPreview()
  else loadPreview()
}

async function loadManualPreview() {
  if (!selectedPlanId.value && !planOptions.value.length) {
    ElMessage.warning('暂无待处理计划')
    return
  }
  viewMode.value = 'auto'
  loading.value = true
  try {
    const ctx = await fetchManagerPlanContext(selectedPlanId.value)
    const routes = ctx.processRoute || []
    preview.value = {
      planId: ctx.planId || selectedPlanId.value,
      productModel: ctx.productModel,
      planQuantity: ctx.quantity,
      summary: routes.length
        ? `共 ${routes.length} 道工序，请手动选择设备与操作员`
        : '未找到工艺路线，请确认计划数据'
    }
    editableRows.value = mapEditableRows(
      routes.map((step) => ({
        processStep: step.stepName,
        workshopName: '',
        equipmentCode: '',
        equipmentName: '',
        recommendedOperator: '',
        recommendedOperatorName: '',
        planQty: ctx.quantity,
        standardWorkHours: step.standardWorkHours,
        recommendReason: '手动配置'
      })),
      ctx.quantity
    )
    validation.value = null
  } catch (e) {
    preview.value = null
    editableRows.value = []
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadPreview() {
  if (!selectedPlanId.value && !planOptions.value.length) {
    ElMessage.warning('暂无待派工计划')
    return
  }
  viewMode.value = 'auto'
  loading.value = true
  try {
    const data = await runAnimatedPreview(async () => {
      const result = await mes.previewSmartDispatch(selectedPlanId.value, userStore.username, userStore.roleKey)
      preview.value = result
      return result
    })
    preview.value = data
    if (!selectedPlanId.value && preview.value?.planId) selectedPlanId.value = preview.value.planId
    editableRows.value = mapEditableRows(
      (preview.value?.recommendations || []).map((r) => ({
        ...r,
        equipmentCode: r.equipmentCode || mes.equipment.find((e) => e.name === r.equipmentName)?.id || ''
      })),
      preview.value?.planQuantity
    )
    validation.value = preview.value?.validation || null
    if (!validation.value) await validateRows()
  } catch (e) {
    preview.value = null
    editableRows.value = []
    ElMessage.error(e?.message || '分析失败')
  } finally {
    loading.value = false
  }
}

async function validateRows() {
  if (!selectedPlanId.value || !editableRows.value.length) return
  try {
    validation.value = await postValidateSmartDispatch({ planId: selectedPlanId.value, recommendations: editableRows.value })
  } catch (e) {
    if (!validation.value && preview.value?.validation) {
      validation.value = preview.value.validation
      return
    }
    validation.value = { canSubmit: validation.value?.canSubmit !== false, hasDanger: true, conflicts: [] }
  }
}

function onOperatorPick(row, username) {
  const u = mes.boundOperatorUsers.find((o) => o.username === username)
  row.recommendedOperatorName = u?.realName || username
  const bind = operatorBinding(username)
  if (bind) row.workshopName = bind.workshopName
}

async function confirm() {
  if (!canSubmit.value) return
  await validateRows()
  if (validation.value?.hasDanger) {
    ElMessage.warning('派工方案校验未通过，请修改后重试')
    return
  }
  try {
    const actionLabel = isDispatchIntent.value ? '派工' : '生成工单'
    await ElMessageBox.confirm(
      `确认按计划 ${selectedPlanId.value} ${actionLabel} ${editableRows.value.length} 道工序？`,
      confirmLabel.value
    )
  } catch { return }
  confirming.value = true
  try {
    const res = await mes.confirmSmartDispatch(selectedPlanId.value, userStore.username, userStore.roleKey, editableRows.value)
    ElMessage.success(res?.message || (isDispatchIntent.value ? '派工成功' : '工单生成成功'))
    visible.value = false
    emit('success', res)
  } catch (e) {
    ElMessage.error(e?.message || (isDispatchIntent.value ? '派工失败' : '生成失败'))
  } finally {
    confirming.value = false
  }
}
</script>

<style scoped>
.dispatch-steps {
  margin-bottom: 16px;
  padding: 0 4px;
}

.dispatch-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.dispatch-toolbar__plan {
  width: min(420px, 100%);
}

.plan-label {
  font-size: 14px;
  font-weight: 500;
  color: #344054;
}

.dispatch-phase {
  flex: 1;
  min-height: 0;
}

.dispatch-phase--analysis {
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.dispatch-phase--result {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.dispatch-phase--idle {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.idle-hint {
  max-width: 520px;
  text-align: center;
  font-size: 14px;
  line-height: 1.6;
  color: #667085;
}

.dispatch-workflow {
  display: flex;
  align-items: flex-start;
  gap: 0;
  padding: 14px 16px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  background: linear-gradient(180deg, #f8fafc 0%, #fff 100%);
  overflow-x: auto;
}

.dispatch-workflow__item {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex: 1;
  min-width: 148px;
}

.dispatch-workflow__node {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #d0d5dd;
  background: #fff;
  transition: all 0.25s ease;
}

.dispatch-workflow__index {
  font-size: 12px;
  font-weight: 600;
  color: #98a2b3;
}

.dispatch-workflow__body {
  flex: 1;
  min-width: 0;
  padding-top: 2px;
}

.dispatch-workflow__title {
  font-size: 13px;
  font-weight: 600;
  color: #344054;
  line-height: 1.3;
}

.dispatch-workflow__detail {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.45;
  color: #667085;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.dispatch-workflow__connector {
  position: absolute;
  top: 14px;
  right: -8px;
  width: 16px;
  height: 2px;
  background: #e4e7ec;
}

.dispatch-workflow__item--done .dispatch-workflow__node {
  border-color: #2e90fa;
  background: #eff8ff;
}

.dispatch-workflow__item--done .dispatch-workflow__index {
  color: #1570ef;
}

.dispatch-workflow__item--done .dispatch-workflow__title {
  color: #1570ef;
}

.dispatch-workflow__item--active .dispatch-workflow__node {
  border-color: #1570ef;
  background: #1570ef;
  box-shadow: 0 0 0 4px rgba(21, 112, 239, 0.15);
}

.dispatch-workflow__item--active .dispatch-workflow__index {
  color: #fff;
}

.dispatch-workflow__item--active .dispatch-workflow__title {
  color: #172033;
}

.dispatch-workflow__item--active .dispatch-workflow__detail {
  color: #344054;
}

.result-head {
  border: 1px solid #ebeef5;
  border-radius: 0;
  background: #fafafa;
  overflow: hidden;
}

.result-context {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 0;
  padding: 10px 14px;
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

.summary-bar {
  margin: 0;
}

.dispatch-conflict-list {
  margin: 6px 0 0;
  padding-left: 18px;
  line-height: 1.6;
}
</style>

<style>
.smart-dispatch-dialog.el-dialog {
  border-radius: 0 !important;
  max-width: calc(100vw - 32px);
  width: min(1580px, calc(100vw - 32px)) !important;
  height: min(820px, calc(100vh - 48px));
  max-height: calc(100vh - 48px);
  margin-top: 24px !important;
  margin-bottom: 24px !important;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.smart-dispatch-dialog .el-dialog__header {
  border-radius: 0 !important;
  flex-shrink: 0;
}

.smart-dispatch-dialog .el-dialog__body {
  font-size: 14px;
  color: #344054;
  padding-left: 20px;
  padding-right: 20px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.smart-dispatch-dialog .el-dialog__footer {
  flex-shrink: 0;
}

.smart-dispatch-dialog .dispatch-steps,
.smart-dispatch-dialog .dispatch-toolbar {
  flex-shrink: 0;
}

.smart-dispatch-dialog .dispatch-phase {
  min-height: 0;
}

.smart-dispatch-dialog .dispatch-workflow {
  flex-shrink: 0;
}

.smart-dispatch-dialog .el-dialog__title {
  font-size: 18px;
  font-weight: 600;
  color: #172033;
}

.smart-dispatch-dialog .summary-bar {
  border-radius: 0 !important;
}

.smart-dispatch-dialog .summary-bar .el-alert__title {
  font-size: 14px;
  line-height: 1.5;
}

.smart-dispatch-dialog .dispatch-table {
  width: 100%;
  border-radius: 0 !important;
}

.smart-dispatch-dialog .dispatch-table .el-table__cell {
  padding: 12px 10px !important;
}

.smart-dispatch-dialog .dispatch-table th.el-table__cell {
  font-size: 14px;
  font-weight: 600;
  color: #344054;
  background: #f7f8fa !important;
}

.smart-dispatch-dialog .dispatch-table td.el-table__cell {
  font-size: 14px;
  font-weight: 400;
  color: #344054;
}

.smart-dispatch-dialog .dispatch-table .cell {
  white-space: normal;
  word-break: keep-all;
  overflow: visible;
  text-overflow: clip;
  line-height: 1.45;
}

.smart-dispatch-dialog .dispatch-table__select {
  width: 100%;
}

.smart-dispatch-dialog .dispatch-table__select--danger .el-select__wrapper {
  box-shadow: 0 0 0 1px #f56c6c inset;
}

.smart-dispatch-dialog .dispatch-table .el-input__wrapper,
.smart-dispatch-dialog .dispatch-table .el-select__wrapper {
  min-height: 34px;
  font-size: 14px;
  border-radius: 0 !important;
}

.smart-dispatch-dialog .dispatch-table .el-input-number {
  width: 100%;
}

.smart-dispatch-dialog .dispatch-table .el-input-number .el-input-number__decrease,
.smart-dispatch-dialog .dispatch-table .el-input-number .el-input-number__increase {
  border-radius: 0 !important;
}

.smart-dispatch-dialog .dispatch-table .el-select__wrapper .el-select__selected-item {
  max-width: 100%;
  overflow: visible;
  text-overflow: clip;
  white-space: normal;
  line-height: 1.4;
}

.smart-dispatch-dialog .dispatch-table .el-input__inner {
  font-size: 14px;
}

.smart-dispatch-dialog .dispatch-table .el-input-number .el-input__wrapper {
  padding-left: 8px;
  padding-right: 36px;
}

.smart-dispatch-dialog .dispatch-hours-readonly .el-input__wrapper {
  background: #f7f8fa;
  box-shadow: 0 0 0 1px #e4e7ec inset;
}

.smart-dispatch-dialog .dispatch-hours-readonly .el-input__inner {
  color: #344054;
  -webkit-text-fill-color: #344054;
}
</style>
