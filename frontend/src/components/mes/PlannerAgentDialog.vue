<template>
  <el-dialog v-model="visible" title="计划员 Agent · 智能排产" width="860px" destroy-on-close @closed="reset">
    <el-alert type="info" :closable="false" show-icon class="agent-tip"
      title="选择订单后系统自动核查成品库存与 BOM 物料，给出建议排产量；确认后 Agent 将测算车间、设备与人员并创建计划。" />

    <el-form label-width="100px" class="agent-form">
      <el-form-item label="待计划订单">
        <el-select v-model="form.orderId" style="width:100%" placeholder="请选择订单" @change="onOrderChange">
          <el-option v-for="o in pendingOrders" :key="o.id" :label="`${o.id} · ${o.productModel} · ${o.quantity}台`" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="计划开始">
        <el-date-picker v-model="form.planStart" type="date" value-format="YYYY-MM-DD" style="width:100%" @change="onDateChange" />
      </el-form-item>
      <el-form-item label="计划截止">
        <el-date-picker v-model="form.planEnd" type="date" value-format="YYYY-MM-DD" style="width:100%" @change="onDateChange" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="previewLoading" @click="runPreview">重新分析</el-button>
        <el-button
          type="success"
          :loading="submitLoading"
          :disabled="!canSubmit"
          @click="runCreate"
        >
          确认并提交主管
        </el-button>
      </el-form-item>
    </el-form>

    <div v-if="previewLoading && !analysis" class="agent-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      正在核查库存并生成排产建议…
    </div>

    <div v-if="analysis" class="agent-result">
      <el-alert
        :type="decisionAlertType"
        :closable="false"
        show-icon
        class="agent-decision"
        :title="analysis.recommendation || analysis.summary"
      />

      <div class="inventory-section">
        <div class="section-title">库存核查</div>
        <div class="inventory-kpi">
          <div class="kpi-item">
            <span class="kpi-label">订单数量</span>
            <span class="kpi-value">{{ inventory.orderQuantity ?? analysis.orderQuantity }} 台</span>
          </div>
          <div class="kpi-item">
            <span class="kpi-label">成品可用</span>
            <span class="kpi-value">{{ inventory.finishedGoodsStock ?? '-' }} 台</span>
          </div>
          <div class="kpi-item">
            <span class="kpi-label">现货交付</span>
            <span class="kpi-value kpi-value--success">{{ inventory.shipFromStock ?? 0 }} 台</span>
          </div>
          <div class="kpi-item">
            <span class="kpi-label">需生产</span>
            <span class="kpi-value">{{ inventory.needToProduce ?? 0 }} 台</span>
          </div>
          <div class="kpi-item kpi-item--highlight">
            <span class="kpi-label">建议排产</span>
            <el-input-number
              v-model="form.plannedQty"
              :min="0"
              :max="99999"
              size="small"
              controls-position="right"
              style="width: 120px"
            />
            <span class="kpi-unit">台</span>
          </div>
        </div>

        <el-table
          v-if="materialChecks.length"
          :data="materialChecks"
          border
          stripe
          size="small"
          max-height="220"
          class="material-table"
        >
          <el-table-column prop="materialName" label="物料" min-width="140" />
          <el-table-column prop="materialCode" label="编码" width="100" />
          <el-table-column prop="available" label="可用" width="80" align="right" />
          <el-table-column label="需求" width="90" align="right">
            <template #default="{ row }">
              {{ row.requiredForPlan ?? row.required ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="maxSupportQty" label="可支撑" width="80" align="right">
            <template #default="{ row }">
              {{ row.materialType === 'FINISHED' ? row.available : row.maxSupportQty }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.sufficient ? 'success' : 'danger'" size="small">
                {{ row.sufficient ? '充足' : '不足' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="说明" min-width="120" show-overflow-tooltip />
        </el-table>

        <div v-if="bottlenecks.length" class="bottleneck-tip">
          瓶颈物料：{{ bottlenecks.join('、') }}
        </div>
      </div>

      <template v-if="showCapacitySection">
        <div class="section-title">产能排布（基于建议排产量 {{ analysis.recommendedPlanQty || analysis.quantity }} 台）</div>
        <div class="agent-result__summary">{{ analysis.summary }}</div>
        <div v-if="analysis.planExplanation" class="explain-card">
          <div class="explain-card__title">算法解释</div>
          <div class="explain-card__text">{{ analysis.planExplanation }}</div>
          <div class="capacity-tags">
            <el-tag size="small" type="info">物料上限 {{ capacity.materialLimit ?? '-' }} 台</el-tag>
            <el-tag size="small" type="info">设备上限 {{ capacity.equipmentLimit ?? '-' }} 台</el-tag>
            <el-tag size="small" type="info">人员可行 {{ capacity.operatorLimit ?? '-' }} 台</el-tag>
            <el-tag size="small" type="success">可用操作员 {{ capacity.availableOperators ?? analysis.availableOperators ?? 0 }} 人</el-tag>
          </div>
          <div v-if="capacityLimits.length" class="capacity-limits">
            约束说明：{{ capacityLimits.join('；') }}
          </div>
        </div>
        <div class="agent-result__stats">
          <span>周期 {{ analysis.workDays }} 天</span>
          <span>日均 {{ analysis.dailyTarget }} 台</span>
          <span>设备 {{ analysis.totalMachines }} 台</span>
          <span>人员 {{ analysis.totalOperators }} 人</span>
        </div>
        <el-table :data="analysis.workshops" border stripe size="small" max-height="220">
          <el-table-column prop="workshopName" label="车间" width="120" />
          <el-table-column prop="department" label="部门" width="100" />
          <el-table-column prop="steps" label="工序" min-width="140">
            <template #default="{ row }">{{ (row.steps || []).join('、') }}</template>
          </el-table-column>
          <el-table-column prop="requiredMachines" label="设备" width="70" align="center" />
          <el-table-column prop="requiredOperators" label="人员" width="70" align="center" />
          <el-table-column prop="availableOperators" label="可用人员" width="90" align="center" />
          <el-table-column prop="utilization" label="负荷" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.utilization >= 90 ? 'warning' : 'success'" size="small">{{ row.utilization }}%</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-table
          v-if="capacityAllocations.length"
          :data="capacityAllocations"
          border
          stripe
          size="small"
          max-height="180"
          class="allocation-table"
        >
          <el-table-column prop="stepName" label="工序" min-width="120" />
          <el-table-column prop="workshopName" label="车间" width="120" />
          <el-table-column prop="equipmentType" label="设备类型" width="110" />
          <el-table-column prop="machines" label="分配设备" width="90" align="center" />
          <el-table-column prop="operators" label="分配人员" width="90" align="center" />
          <el-table-column prop="dailyCapacity" label="日产能" width="90" align="center" />
        </el-table>
      </template>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'

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
const capacityLimits = computed(() => capacity.value.limits || [])
const capacityAllocations = computed(() => capacity.value.allocations || [])
const materialChecks = computed(() => inventory.value.materialChecks || [])
const bottlenecks = computed(() => inventory.value.bottlenecks || analysis.value?.inventoryCheck?.bottlenecks || [])
const decision = computed(() => analysis.value?.decision || inventory.value.decision || '')
const showCapacitySection = computed(() => (analysis.value?.recommendedPlanQty ?? 0) > 0)

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
    if (form.orderId) {
      runPreview()
    }
  }
})

function onOrderChange() {
  analysis.value = null
  if (form.orderId) {
    runPreview()
  }
}

function onDateChange() {
  if (form.orderId && analysis.value) {
    runPreview()
  }
}

function reset() {
  analysis.value = null
}

async function runPreview() {
  if (!form.orderId || !form.planStart || !form.planEnd) {
    return
  }
  previewLoading.value = true
  try {
    analysis.value = await mes.previewPlanAgent(form, userStore.username, userStore.roleKey)
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
.agent-tip { margin-bottom: 16px; }
.agent-form { margin-bottom: 12px; }
.agent-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 24px;
  color: #606266;
  font-size: 13px;
  justify-content: center;
}
.agent-decision { margin-bottom: 14px; }
.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #001b3f;
  margin-bottom: 10px;
}
.inventory-section {
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px dashed #e4e7ed;
}
.inventory-kpi {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.kpi-item {
  flex: 1;
  min-width: 100px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  text-align: center;
}
.kpi-item--highlight {
  background: #ecf5ff;
  border: 1px solid #b3d8ff;
}
.kpi-unit {
  margin-left: 6px;
  font-size: 12px;
  color: #909399;
}
.kpi-label {
  display: block;
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
}
.kpi-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.kpi-value--success { color: #67c23a; }
.material-table { margin-bottom: 8px; }
.bottleneck-tip {
  font-size: 12px;
  color: #e6a23c;
  padding: 6px 0;
}
.agent-result__summary {
  font-size: 13px;
  color: #303133;
  line-height: 1.6;
  margin-bottom: 10px;
  padding: 10px 12px;
  background: #f0f9ff;
  border-radius: 4px;
}
.explain-card {
  padding: 10px 12px;
  margin-bottom: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fafcff;
}
.explain-card__title {
  font-size: 13px;
  font-weight: 600;
  color: #001b3f;
  margin-bottom: 6px;
}
.explain-card__text {
  font-size: 12px;
  color: #606266;
  line-height: 1.7;
  margin-bottom: 8px;
}
.capacity-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}
.capacity-limits {
  font-size: 12px;
  color: #e6a23c;
}
.allocation-table {
  margin-top: 10px;
}
.agent-result__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 12px;
  color: #606266;
}
</style>
