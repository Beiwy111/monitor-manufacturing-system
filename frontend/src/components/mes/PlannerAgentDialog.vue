<template>
  <el-dialog
    v-model="visible"
    title="智能排产"
    width="1080px"
    destroy-on-close
    class="planner-dialog"
    @closed="onClosed"
  >
    <div class="planner-toolbar">
      <el-select v-model="form.orderId" class="planner-toolbar__order" placeholder="选择订单" @change="onOrderChange">
        <el-option v-for="o in pendingOrders" :key="o.id" :label="`${o.id} · ${o.productModel} · ${o.quantity}台`" :value="o.id" />
      </el-select>
      <el-date-picker v-model="form.planStart" type="date" value-format="YYYY-MM-DD" placeholder="开始" @change="loadSchemes" />
      <el-date-picker v-model="form.planEnd" type="date" value-format="YYYY-MM-DD" placeholder="截止" @change="loadSchemes" />
      <el-input-number v-model="form.plannedQty" :min="1" controls-position="right" placeholder="数量" @change="loadSchemes" />
      <el-button type="primary" :loading="schemeLoading" @click="loadSchemes">生成三方案</el-button>
    </div>

    <div v-if="schemeData" class="scheme-result">
      <el-alert :title="schemeConclusion" type="success" :closable="false" show-icon class="scheme-conclusion" />
      <div class="scheme-evidence">
        <span v-for="(e, i) in schemeData.evidence || []" :key="i">{{ e.label }}：{{ e.value }}</span>
      </div>
      <div v-if="schemeData.dataNote" class="scheme-data-note">{{ schemeData.dataNote }}</div>

      <el-table
        :data="schemeData.schemes || []"
        border
        stripe
        highlight-current-row
        :row-class-name="schemeRowClass"
        @current-change="onSchemeSelect"
      >
        <el-table-column prop="label" label="方案" width="100" />
        <el-table-column prop="finishDate" label="完工时间" width="110" />
        <el-table-column prop="equipmentUtilization" label="设备利用率" width="100" align="center">
          <template #default="{ row }">{{ row.equipmentUtilization }}%</template>
        </el-table-column>
        <el-table-column prop="materialShortage" label="缺料项" width="80" align="center" />
        <el-table-column prop="delayDays" label="延期(天)" width="80" align="center" />
        <el-table-column prop="lineChanges" label="换线次数" width="90" align="center" />
        <el-table-column prop="summary" label="说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="row.conflicts?.length" placement="top">
              <template #content>
                <div v-for="(c, i) in row.conflicts" :key="i">{{ c.label }}：{{ c.detail }}</div>
              </template>
              <el-tag :type="row.canSubmit ? 'warning' : 'danger'" size="small">
                {{ row.canSubmit ? '有提示' : '需确认' }}
              </el-tag>
            </el-tooltip>
            <el-tag v-else type="success" size="small">可用</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-collapse v-if="analysis" class="analysis-collapse">
        <el-collapse-item title="Agent 分析过程（默认折叠）" name="analysis">
          <SchedulingThoughtPanel
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
        </el-collapse-item>
      </el-collapse>

      <div v-if="selectedScheme" class="scheme-adjust">
        <span>已选：<strong>{{ selectedScheme.label }}</strong></span>
        <el-input-number v-model="form.plannedQty" :min="1" size="small" />
        <span>台</span>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="success" :loading="submitLoading" :disabled="!selectedScheme" @click="runCreate">
        确认方案并提交主管
      </el-button>
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
import { postComparePlanSchemes, postSaveProductionPlan } from '@/api/planner'

const props = defineProps({
  modelValue: Boolean,
  defaultOrderId: { type: String, default: '' }
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

const schemeConclusion = computed(() => {
  const c = schemeData.value?.conclusion
  return c ? `${c.label}：${c.summary}` : '请选择订单并生成三方案对比'
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
  orderId: '',
  planStart: new Date().toISOString().slice(0, 10),
  planEnd: new Date(Date.now() + 14 * 86400000).toISOString().slice(0, 10),
  plannedQty: 0
})

const inventory = computed(() => analysis.value?.inventoryCheck || {})
const capacity = computed(() => analysis.value?.capacityAnalysis || {})
const bottlenecks = computed(() => inventory.value.bottlenecks || analysis.value?.inventoryCheck?.bottlenecks || [])
const decision = computed(() => analysis.value?.decision || inventory.value.decision || '')
const showCapacitySection = computed(() => (analysis.value?.recommendedPlanQty ?? 0) > 0)

const bottleneckLabel = computed(() => {
  const limits = [
    capacity.value.materialLimit != null ? `物料 ${capacity.value.materialLimit}` : null,
    capacity.value.operatorLimit != null ? `人员 ${capacity.value.operatorLimit}` : null,
    capacity.value.equipmentLimit != null ? `设备 ${capacity.value.equipmentLimit}` : null
  ].filter(Boolean)
  if (!limits.length) return '—'
  return limits.join(' / ')
})

const decisionAlertType = computed(() => {
  switch (decision.value) {
    case 'FULL_STOCK': return 'success'
    case 'PARTIAL_STOCK':
    case 'PRODUCE_ALL': return 'info'
    case 'PARTIAL_PRODUCE': return 'warning'
    case 'CAPACITY_SHORTAGE':
    case 'MATERIAL_SHORTAGE': return 'error'
    default: return 'info'
  }
})

const canSubmit = computed(() => {
  if (!analysis.value) return false
  const qty = form.plannedQty || (analysis.value.recommendedPlanQty ?? inventory.value.recommendedPlanQty ?? 0)
  return qty > 0 && !['FULL_STOCK', 'CAPACITY_SHORTAGE'].includes(decision.value)
})

watch(() => props.modelValue, (open) => {
  if (open) {
    form.orderId = props.defaultOrderId || pendingOrders.value[0]?.id || ''
    analysis.value = null
    schemeData.value = null
    selectedScheme.value = null
    resetFlow()
    if (form.orderId) loadSchemes()
  }
})

function onOrderChange() {
  analysis.value = null
  schemeData.value = null
  selectedScheme.value = null
  resetFlow()
  if (form.orderId) loadSchemes()
}

function onSchemeSelect(row) {
  selectedScheme.value = row || null
}

function onClosed() {
  analysis.value = null
  schemeData.value = null
  selectedScheme.value = null
  resetFlow()
}

async function loadSchemes() {
  if (!form.orderId || !form.planStart || !form.planEnd) return
  schemeLoading.value = true
  previewLoading.value = true
  try {
    schemeData.value = await postComparePlanSchemes({
      orderId: form.orderId,
      planStart: form.planStart,
      planEnd: form.planEnd,
      plannedQty: form.plannedQty
    })
    analysis.value = await runAnimatedPreview(async () => {
      const result = await mes.previewPlanAgent(form, userStore.username, userStore.roleKey)
      return result
    })
    selectedScheme.value = schemeData.value?.schemes?.find((s) => s.key === schemeData.value?.conclusion?.key)
      || schemeData.value?.schemes?.[0]
      || null
    const recommended = schemeData.value?.plannedQty || analysis.value?.recommendedPlanQty
    if (recommended && (!form.plannedQty || form.plannedQty < recommended)) {
      form.plannedQty = recommended
    }
  } catch (e) {
    schemeData.value = null
    ElMessage.error(e?.message || '方案生成失败')
  } finally {
    schemeLoading.value = false
    previewLoading.value = false
  }
}

function schemeRowClass({ row }) {
  return selectedScheme.value?.key === row.key ? 'scheme-row--active' : ''
}

async function runCreate() {
  if (!selectedScheme.value) {
    ElMessage.warning('请先选择一种排产方案')
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
    await postSaveProductionPlan({
      orderId: form.orderId,
      plannedQty: form.plannedQty,
      planStart: form.planStart,
      planEnd: selectedScheme.value.finishDate || form.planEnd,
      schedules: selectedScheme.value.schedules,
      saveAction: 'submit',
      schedulingMode: selectedScheme.value.key,
      remark: '智能排产：' + selectedScheme.value.label,
      operator: userStore.username
    })
    ElMessage.success('已按所选方案创建计划并提交主管')
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
.scheme-data-note { font-size: 12px; color: #909399; margin-bottom: 8px; }
.scheme-result { margin-top: 8px; }
.scheme-conclusion { margin-bottom: 10px; }
.scheme-evidence { display: flex; flex-wrap: wrap; gap: 12px; font-size: 12px; color: #606266; margin-bottom: 10px; }
.analysis-collapse { margin-top: 12px; }
.scheme-adjust { margin-top: 12px; display: flex; align-items: center; gap: 8px; font-size: 13px; }

:deep(.scheme-row--active > td) {
  background: #ecf5ff !important;
}

.planner-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.planner-toolbar__order {
  flex: 1;
  min-width: 280px;
}

.result-strip {
  margin-top: 14px;
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafbfc;
}

.result-strip__conclusion {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
  margin-bottom: 12px;
}

.result-strip__conclusion strong {
  flex-shrink: 0;
  color: #1a2b4a;
}

.result-strip__conclusion--success strong { color: #67c23a; }
.result-strip__conclusion--warning strong { color: #e6a23c; }
.result-strip__conclusion--error strong { color: #f56c6c; }

.result-strip__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.metric {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  min-width: 100px;
}

.metric--highlight {
  border-color: #b3d8ff;
  background: #ecf5ff;
}

.metric__label {
  font-size: 12px;
  color: #909399;
}

.metric__value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.metric__value--ok {
  color: #67c23a;
}

.metric__unit {
  font-size: 12px;
  color: #909399;
}

.result-strip__hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: #e6a23c;
}
</style>
