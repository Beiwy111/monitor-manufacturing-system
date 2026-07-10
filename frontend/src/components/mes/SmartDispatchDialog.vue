<template>
  <el-dialog v-model="visible" title="智能派工 · 实时规划" width="1160px" destroy-on-close @open="onOpen" @closed="resetFlow">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="smart-tip"
      title="四道工序将分别派给四名固定车间操作员：显示屏加工（赵）→ 主板装配（李）→ 面板贴附（王）→ 整机组装（孙），每人只负责本车间一道工序。"
    />

    <el-form v-if="planOptions.length > 1" inline class="plan-filter">
      <el-form-item label="生产计划">
        <el-select v-model="selectedPlanId" style="width: 240px" @change="loadPreview">
          <el-option v-for="p in planOptions" :key="p.id" :label="`${p.id} · ${p.productModel} · ${p.quantity}台`" :value="p.id" />
        </el-select>
      </el-form-item>
    </el-form>

    <div class="smart-toolbar">
      <el-button type="primary" :loading="loading" @click="loadPreview">重新分析</el-button>
      <el-button
        type="success"
        :loading="confirming"
        :disabled="!recommendations.length || !selectedPlanId"
        @click="confirm"
      >
        确认派工并生成工单
      </el-button>
    </div>

    <SchedulingThoughtPanel
      title="智能派工引擎"
      subtitle="工艺路线 → 设备扫描 → 人员评估 → 工序匹配 → 派工方案"
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

    <div v-if="preview && visibleSections.has('result')" class="result-block">
      <el-alert type="success" :closable="false" show-icon :title="preview.summary" class="smart-summary" />
      <el-descriptions :column="4" border size="small" class="plan-meta">
        <el-descriptions-item label="计划号">{{ preview.planId || selectedPlanId }}</el-descriptions-item>
        <el-descriptions-item label="产品型号">{{ preview.productModel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划数量">{{ preview.planQuantity || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工单状态">
          <el-tag :type="preview.hasWorkOrder ? 'success' : 'warning'" size="small">
            {{ preview.hasWorkOrder ? '已有工单' : '确认后生成' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <el-table
      v-if="visibleSections.has('table') && recommendations.length"
      v-loading="loading"
      :data="recommendations"
      border
      stripe
      max-height="360"
      class="rec-table"
    >
      <el-table-column prop="processStep" label="工序" width="120" />
      <el-table-column prop="workshopName" label="车间" min-width="140" />
      <el-table-column label="推荐操作员" width="100">
        <template #default="{ row }">{{ row.recommendedOperatorName }}</template>
      </el-table-column>
      <el-table-column prop="recommendReason" label="推荐原因" min-width="200" show-overflow-tooltip />
      <el-table-column prop="equipmentName" label="设备" min-width="130" show-overflow-tooltip />
      <el-table-column prop="estimatedHours" label="工时(h)" width="90" align="right" />
      <el-table-column prop="planQty" label="派工数量" width="100" align="right" />
    </el-table>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
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

const {
  activeStep,
  activeStepKey,
  selectedStepKey,
  thoughtStream,
  evidenceList,
  allEvidence,
  currentDetail,
  visibleSections,
  reset: resetFlow,
  runAnimatedPreview,
  selectStep
} = useSchedulingFlow(DISPATCH_FLOW_TEMPLATE)

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const planOptions = computed(() => mes.pendingManagerPlans)
const recommendations = computed(() => preview.value?.recommendations || [])

function onOpen() {
  preview.value = null
  resetFlow()
  selectedPlanId.value = props.defaultPlanId || planOptions.value[0]?.id || ''
  if (selectedPlanId.value) {
    loadPreview()
  }
}

async function loadPreview() {
  if (!selectedPlanId.value && !planOptions.value.length) {
    ElMessage.warning('当前没有待派工的生产计划')
    return
  }
  loading.value = true
  try {
    preview.value = await runAnimatedPreview(async () => {
      const result = await mes.previewSmartDispatch(
        selectedPlanId.value,
        userStore.username,
        userStore.roleKey
      )
      preview.value = result
      return result
    })
    if (!selectedPlanId.value && preview.value?.planId) {
      selectedPlanId.value = preview.value.planId
    }
  } catch (e) {
    preview.value = null
    ElMessage.error(e?.message || '智能派工分析失败')
  } finally {
    loading.value = false
  }
}

async function confirm() {
  if (!selectedPlanId.value) return
  try {
    await ElMessageBox.confirm(
      `将为计划 ${selectedPlanId.value} 生成工单，并按四道工序分别派给 ${recommendations.value.length} 名不同操作员（每人固定一个车间），是否继续？`,
      '确认智能派工',
      { type: 'info' }
    )
  } catch {
    return
  }
  confirming.value = true
  try {
    const res = await mes.confirmSmartDispatch(
      selectedPlanId.value,
      userStore.username,
      userStore.roleKey
    )
    ElMessage.success(res?.message || '智能派工已确认')
    visible.value = false
    emit('success', res)
  } catch (e) {
    ElMessage.error(e?.message || '确认派工失败')
  } finally {
    confirming.value = false
  }
}
</script>

<style scoped>
.smart-tip { margin-bottom: 12px; }
.plan-filter { margin-bottom: 8px; }
.smart-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}
.result-block {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed #e4e7ed;
}
.smart-summary { margin-bottom: 12px; }
.plan-meta { margin-bottom: 12px; }
.rec-table { margin-top: 8px; }
</style>
