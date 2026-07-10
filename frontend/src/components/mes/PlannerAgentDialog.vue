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
      <el-select
        v-model="form.orderId"
        class="planner-toolbar__order"
        placeholder="选择订单"
        @change="onOrderChange"
      >
        <el-option
          v-for="o in pendingOrders"
          :key="o.id"
          :label="`${o.id} · ${o.productModel} · ${o.quantity}台`"
          :value="o.id"
        />
      </el-select>
      <el-date-picker
        v-model="form.planStart"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="开始"
        @change="onDateChange"
      />
      <el-date-picker
        v-model="form.planEnd"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="截止"
        @change="onDateChange"
      />
      <el-button type="primary" :loading="previewLoading" @click="runPreview">重新分析</el-button>
      <el-button type="success" :loading="submitLoading" :disabled="!canSubmit" @click="runCreate">
        确认并提交主管
      </el-button>
    </div>

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

    <div v-if="analysis" class="result-strip">
      <div class="result-strip__conclusion" :class="`result-strip__conclusion--${decisionAlertType}`">
        <strong>排产结论</strong>
        <span>{{ analysis.recommendation || analysis.summary }}</span>
      </div>
      <div class="result-strip__metrics">
        <div class="metric">
          <span class="metric__label">订单</span>
          <span class="metric__value">{{ inventory.orderQuantity ?? analysis.orderQuantity }} 台</span>
        </div>
        <div class="metric">
          <span class="metric__label">现货可发</span>
          <span class="metric__value metric__value--ok">{{ inventory.shipFromStock ?? 0 }} 台</span>
        </div>
        <div class="metric">
          <span class="metric__label">需生产</span>
          <span class="metric__value">{{ inventory.needToProduce ?? 0 }} 台</span>
        </div>
        <div class="metric metric--highlight">
          <span class="metric__label">建议排产</span>
          <el-input-number
            v-model="form.plannedQty"
            :min="0"
            :max="99999"
            size="small"
            controls-position="right"
          />
          <span class="metric__unit">台</span>
        </div>
        <div v-if="showCapacitySection" class="metric">
          <span class="metric__label">周期</span>
          <span class="metric__value">{{ analysis.workDays }} 天</span>
        </div>
        <div v-if="showCapacitySection" class="metric">
          <span class="metric__label">瓶颈</span>
          <span class="metric__value">{{ bottleneckLabel }}</span>
        </div>
      </div>
      <p v-if="bottlenecks.length" class="result-strip__hint">
        物料瓶颈：{{ bottlenecks.join('、') }}。详细推演见左侧思维流与右侧证据库。
      </p>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { useSchedulingFlow } from '@/composables/useSchedulingFlow'
import SchedulingThoughtPanel from '@/components/mes/SchedulingThoughtPanel.vue'

const props = defineProps({
  modelValue: Boolean,
  defaultOrderId: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'success'])

const mes = useMesStore()
const userStore = useUserStore()
const previewLoading = ref(false)
const submitLoading = ref(false)
const analysis = ref(null)

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
    resetFlow()
    if (form.orderId) {
      runPreview()
    }
  }
})

function onOrderChange() {
  analysis.value = null
  resetFlow()
  if (form.orderId) {
    runPreview()
  }
}

function onDateChange() {
  if (form.orderId && analysis.value) {
    runPreview()
  }
}

function onClosed() {
  analysis.value = null
  resetFlow()
}

async function runPreview() {
  if (!form.orderId || !form.planStart || !form.planEnd) {
    return
  }
  previewLoading.value = true
  try {
    analysis.value = await runAnimatedPreview(async () => {
      const result = await mes.previewPlanAgent(form, userStore.username, userStore.roleKey)
      analysis.value = result
      return result
    })
    form.plannedQty = analysis.value?.recommendedPlanQty
      ?? inventory.value?.recommendedPlanQty
      ?? 0
  } catch (e) {
    analysis.value = null
    ElMessage.error(e?.message || 'Agent 分析失败')
  } finally {
    previewLoading.value = false
  }
}

async function runCreate() {
  if (!canSubmit.value) {
    ElMessage.warning('当前库存可满足订单或物料不足，无法创建生产计划')
    return
  }
  if (!analysis.value) {
    await runPreview()
    if (!canSubmit.value) return
  }
  submitLoading.value = true
  try {
    const res = await mes.agentCreatePlan(form, userStore.username, userStore.roleKey)
    ElMessage.success(res?.message || 'Agent 已创建计划并提交主管')
    visible.value = false
    emit('success', res)
  } catch (e) {
    ElMessage.error(e?.message || '创建计划失败')
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
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
