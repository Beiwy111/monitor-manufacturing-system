<template>
  <el-dialog v-model="visible" title="智能派工" width="1080px" destroy-on-close @open="onOpen" @closed="resetState">
    <div class="dispatch-toolbar">
      <el-select v-if="planOptions.length > 1" v-model="selectedPlanId" style="width: 260px" @change="loadPreview">
        <el-option v-for="p in planOptions" :key="p.id" :label="`${p.id} · ${p.productModel}`" :value="p.id" />
      </el-select>
      <span v-else-if="preview" class="plan-label">{{ preview.planId }} · {{ preview.productModel }} · {{ preview.planQuantity }}台</span>
      <el-button type="primary" :loading="loading" @click="loadPreview">重新分析</el-button>
      <el-button type="success" :loading="confirming" :disabled="!canSubmit" @click="confirm">确认派工</el-button>
    </div>

    <el-alert v-if="preview?.summary" :title="preview.summary" type="info" :closable="false" show-icon class="summary-bar" />

    <el-table v-loading="loading" :data="editableRows" border stripe size="small" class="dispatch-table">
      <el-table-column prop="processStep" label="工序" width="110" />
      <el-table-column label="车间" min-width="120">
        <template #default="{ row }"><el-input v-model="row.workshopName" size="small" /></template>
      </el-table-column>
      <el-table-column label="设备" min-width="150">
        <template #default="{ row }">
          <el-select
            v-model="row.equipmentCode"
            size="small"
            filterable
            style="width:100%"
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
      <el-table-column label="操作员" min-width="150">
        <template #default="{ row }">
          <el-select v-model="row.recommendedOperator" size="small" style="width:100%" @change="(v) => onOperatorPick(row, v)">
            <el-option
              v-for="u in operatorOptionsForRow(row)"
              :key="u.username"
              :label="operatorLabel(u)"
              :value="u.username"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="数量" width="90">
        <template #default="{ row }">
          <el-input-number v-model="row.planQty" :min="1" size="small" controls-position="right" @change="validateRows" />
        </template>
      </el-table-column>
      <el-table-column label="预计工时(h)" width="100">
        <template #default="{ row }">
          <el-input-number v-model="row.estimatedHours" :min="0.1" :step="0.5" size="small" controls-position="right" />
        </template>
      </el-table-column>
      <el-table-column prop="recommendReason" label="推荐原因" min-width="130" show-overflow-tooltip />
      <el-table-column label="冲突" width="80" align="center">
        <template #default="{ row }">
          <el-tooltip v-if="row.conflicts?.length" placement="top">
            <template #content>
              <div v-for="(c, i) in row.conflicts" :key="i">{{ c.label }}：{{ c.detail }}</div>
            </template>
            <el-tag :type="row.conflictStatus === '冲突' ? 'danger' : 'warning'" size="small">{{ row.conflictStatus }}</el-tag>
          </el-tooltip>
          <el-tag v-else type="success" size="small">正常</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <el-alert v-if="validation?.hasDanger" type="error" :closable="false" title="存在严重冲突，禁止提交" class="conflict-bar" />

    <el-collapse class="agent-collapse">
      <el-collapse-item title="Agent 分析过程（默认折叠）" name="agent">
        <SchedulingThoughtPanel
          v-if="preview?.schedulingSteps"
          title="智能派工引擎"
          subtitle="工艺 → 设备 → 人员 → 匹配"
          :thought-stream="thoughtStream"
          :evidence-list="evidenceList"
          :all-evidence="allEvidence"
          :active-step-key="activeStepKey"
          :selected-step-key="selectedStepKey"
          :active-index="activeStep"
          :total-steps="5"
          :running="loading"
          :pending-text="currentDetail"
          @select-step="selectStep"
        />
      </el-collapse-item>
    </el-collapse>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { postValidateSmartDispatch } from '@/api/mes'
import { operatorBinding, operatorLabel, operatorsForProcessStep } from '@/utils/operatorWorkshop'
import { useSchedulingFlow, DISPATCH_FLOW_TEMPLATE } from '@/composables/useSchedulingFlow'
import SchedulingThoughtPanel from '@/components/mes/SchedulingThoughtPanel.vue'

const props = defineProps({
  modelValue: Boolean,
  defaultPlanId: { type: String, default: '' }
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

const {
  activeStep, activeStepKey, selectedStepKey, thoughtStream, evidenceList, allEvidence,
  currentDetail, reset: resetFlow, runAnimatedPreview, selectStep
} = useSchedulingFlow(DISPATCH_FLOW_TEMPLATE)

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const planOptions = computed(() =>
  mes.pendingManagerPlans.length ? mes.pendingManagerPlans : mes.plans.filter((p) => p.status === '已发布')
)
const canSubmit = computed(() => editableRows.value.length > 0 && validation.value?.canSubmit !== false)

const STEP_EQUIP_TYPE = {
  显示屏加工: '显示屏线',
  主板装配: '主板线',
  面板贴附: '贴附机',
  整机组装: '组装线',
  背光组装: '组装线'
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
  resetFlow()
}

function onOpen() {
  resetState()
  selectedPlanId.value = props.defaultPlanId || planOptions.value[0]?.id || ''
  if (selectedPlanId.value) loadPreview()
}

async function loadPreview() {
  if (!selectedPlanId.value && !planOptions.value.length) {
    ElMessage.warning('暂无待派工计划')
    return
  }
  loading.value = true
  try {
    preview.value = await runAnimatedPreview(() =>
      mes.previewSmartDispatch(selectedPlanId.value, userStore.username, userStore.roleKey)
    )
    if (!selectedPlanId.value && preview.value?.planId) selectedPlanId.value = preview.value.planId
    editableRows.value = (preview.value?.recommendations || []).map((r) => ({
      ...r,
      equipmentCode: r.equipmentCode || mes.equipment.find((e) => e.name === r.equipmentName)?.id || ''
    }))
    validation.value = preview.value?.validation || null
    attachRowConflicts()
    if (!validation.value) await validateRows()
  } catch (e) {
    preview.value = null
    editableRows.value = []
    ElMessage.error(e?.message || '分析失败')
  } finally {
    loading.value = false
  }
}

function attachRowConflicts() {
  const conflicts = validation.value?.conflicts || []
  editableRows.value = editableRows.value.map((row) => {
    const rowConflicts = conflicts.filter((c) => String(c.detail || '').includes(row.processStep))
    return {
      ...row,
      conflicts: rowConflicts,
      conflictStatus: rowConflicts.some((c) => c.level === 'danger') ? '冲突' : rowConflicts.length ? '提示' : '正常'
    }
  })
}

async function validateRows() {
  if (!selectedPlanId.value || !editableRows.value.length) return
  try {
    validation.value = await postValidateSmartDispatch({ planId: selectedPlanId.value, recommendations: editableRows.value })
    attachRowConflicts()
  } catch (e) {
    // 校验接口不可用时沿用预览校验结果，避免阻塞派工
    if (!validation.value && preview.value?.validation) {
      validation.value = preview.value.validation
      attachRowConflicts()
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
    ElMessage.warning('存在严重冲突，请修改后重试')
    return
  }
  try {
    await ElMessageBox.confirm(`确认按计划 ${selectedPlanId.value} 派工 ${editableRows.value.length} 道工序？`, '确认派工')
  } catch { return }
  confirming.value = true
  try {
    const res = await mes.confirmSmartDispatch(selectedPlanId.value, userStore.username, userStore.roleKey, editableRows.value)
    ElMessage.success(res?.message || '派工成功')
    visible.value = false
    emit('success', res)
  } catch (e) {
    ElMessage.error(e?.message || '派工失败')
  } finally {
    confirming.value = false
  }
}
</script>

<style scoped>
.dispatch-toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin-bottom: 10px; }
.plan-label { font-size: 13px; color: #606266; }
.summary-bar, .conflict-bar { margin-bottom: 8px; }
.dispatch-table { margin-bottom: 8px; }
.agent-collapse { margin-top: 8px; }
</style>
