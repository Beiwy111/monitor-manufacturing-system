<template>
  <el-dialog v-model="visible" title="智能派工推荐" width="960px" destroy-on-close @open="onOpen">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="smart-tip"
      title="系统根据操作员空闲度、所属岗位、历史任务与当前负载，为每道工序推荐负责人。确认后将自动生成正式工单并派工。"
    />

    <el-form v-if="planOptions.length > 1" inline class="plan-filter">
      <el-form-item label="生产计划">
        <el-select v-model="selectedPlanId" style="width: 220px" @change="loadPreview">
          <el-option v-for="p in planOptions" :key="p.id" :label="`${p.id} · ${p.productModel}`" :value="p.id" />
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

    <div v-if="preview?.summary" class="smart-summary">{{ preview.summary }}</div>

    <el-table v-loading="loading" :data="recommendations" border stripe max-height="400">
      <el-table-column prop="workOrderId" label="工单编号" width="130" />
      <el-table-column prop="processStep" label="工序名称" width="120" />
      <el-table-column label="推荐操作员" width="120">
        <template #default="{ row }">{{ row.recommendedOperatorName }}</template>
      </el-table-column>
      <el-table-column prop="recommendReason" label="推荐原因" min-width="200" show-overflow-tooltip />
      <el-table-column prop="equipmentCode" label="设备编号" width="100" />
      <el-table-column prop="equipmentName" label="设备名称" min-width="120" show-overflow-tooltip />
      <el-table-column prop="estimatedHours" label="预计工时(h)" width="100" align="right" />
      <el-table-column prop="planQty" label="派工数量" width="90" align="right" />
    </el-table>

    <el-descriptions v-if="preview" :column="4" border size="small" class="plan-meta">
      <el-descriptions-item label="计划号">{{ preview.planId || selectedPlanId }}</el-descriptions-item>
      <el-descriptions-item label="产品型号">{{ preview.productModel || '-' }}</el-descriptions-item>
      <el-descriptions-item label="计划数量">{{ preview.planQuantity || '-' }}</el-descriptions-item>
      <el-descriptions-item label="工单状态">
        <el-tag :type="preview.hasWorkOrder ? 'success' : 'warning'" size="small">
          {{ preview.hasWorkOrder ? '已有工单' : '确认后生成' }}
        </el-tag>
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'

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

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const planOptions = computed(() => mes.pendingManagerPlans)

const recommendations = computed(() => preview.value?.recommendations || [])

function onOpen() {
  preview.value = null
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
    preview.value = await mes.previewSmartDispatch(
      selectedPlanId.value,
      userStore.username,
      userStore.roleKey
    )
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
      `将为计划 ${selectedPlanId.value} 生成正式工单并按推荐结果派工 ${recommendations.value.length} 道工序，是否继续？`,
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
  margin-bottom: 12px;
}
.smart-summary {
  font-size: 13px;
  color: #606266;
  margin-bottom: 10px;
  padding: 8px 12px;
  background: #f0f9ff;
  border-radius: 4px;
}
.plan-meta { margin-top: 12px; }
</style>
