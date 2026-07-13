<template>
  <el-dialog v-model="visible" title="智能生成生产计划" width="1000px" destroy-on-close @open="onOpen">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="smart-tip"
      title="系统根据订单交期、库存、设备可用性自动评分。生成前可修改数量、时间与优先级；生成后为草稿状态，可在计划页继续修改再发布。"
    />

    <div class="smart-toolbar">
      <el-button type="primary" :loading="loading" @click="loadPreview">重新分析</el-button>
      <el-button
        type="success"
        :loading="generating"
        :disabled="!selectedIds.length"
        @click="generateSelected"
      >
        生成选中计划（{{ selectedIds.length }}）
      </el-button>
      <el-button
        type="warning"
        :loading="generating"
        :disabled="!feasibleRows.length"
        @click="generateAll"
      >
        一键生成全部可排产
      </el-button>
    </div>

    <div v-if="preview?.summary" class="smart-summary">{{ preview.summary }}</div>

    <el-table
      v-loading="loading"
      :data="editableRows"
      border
      stripe
      max-height="380"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="45" :selectable="row => row.feasible" />
      <el-table-column prop="priorityScore" label="评分" width="70" align="center" sortable>
        <template #default="{ row }">
          <el-tag :type="priorityTag(row.priorityLevel)" size="small">{{ row.priorityScore }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="优先级" width="100">
        <template #default="{ row }">
          <el-select v-model="row.priorityLevel" size="small" :disabled="!row.feasible">
            <el-option label="高" value="高" />
            <el-option label="中" value="中" />
            <el-option label="低" value="低" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column prop="orderNo" label="订单编号" width="130" />
      <el-table-column prop="productModel" label="产品型号" min-width="120" />
      <el-table-column label="计划数量" width="110">
        <template #default="{ row }">
          <el-input-number
            v-model="row.planQuantity"
            :min="0"
            :max="99999"
            size="small"
            controls-position="right"
            :disabled="!row.feasible"
          />
        </template>
      </el-table-column>
      <el-table-column label="开始时间" width="140">
        <template #default="{ row }">
          <el-date-picker
            v-model="row.planStart"
            type="date"
            value-format="YYYY-MM-DD"
            size="small"
            style="width: 120px"
            :disabled="!row.feasible"
          />
        </template>
      </el-table-column>
      <el-table-column label="完成时间" width="140">
        <template #default="{ row }">
          <el-date-picker
            v-model="row.planEnd"
            type="date"
            value-format="YYYY-MM-DD"
            size="small"
            style="width: 120px"
            :disabled="!row.feasible"
          />
        </template>
      </el-table-column>
      <el-table-column prop="planStatus" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.planStatus)" size="small">{{ row.planStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="140">
        <template #default="{ row }">
          <el-input v-model="row.remark" size="small" placeholder="可填写调整说明" :disabled="!row.feasible" />
        </template>
      </el-table-column>
    </el-table>

    <el-collapse v-if="selectedProposal" class="score-detail">
      <el-collapse-item title="选中订单评分明细" name="score">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="交期评分">{{ selectedProposal.scoreBreakdown?.deliveryScore }}</el-descriptions-item>
          <el-descriptions-item label="库存评分">{{ selectedProposal.scoreBreakdown?.inventoryScore }}</el-descriptions-item>
          <el-descriptions-item label="设备评分">{{ selectedProposal.scoreBreakdown?.equipmentScore }}</el-descriptions-item>
          <el-descriptions-item label="所需设备">{{ (selectedProposal.requiredEquipment || []).join('、') || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所需人员">{{ selectedProposal.requiredPersonnel }}</el-descriptions-item>
          <el-descriptions-item label="风险提示">{{ (selectedProposal.riskWarnings || []).join('；') || '无' }}</el-descriptions-item>
        </el-descriptions>
        <div class="recommend-text">{{ selectedProposal.recommendation }}</div>
      </el-collapse-item>
    </el-collapse>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue', 'success'])

const mes = useMesStore()
const userStore = useUserStore()
const loading = ref(false)
const generating = ref(false)
const preview = ref(null)
const editableRows = ref([])
const selectedIds = ref([])
const selectedProposal = ref(null)

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const feasibleRows = computed(() => editableRows.value.filter((p) => p.feasible && p.planQuantity > 0))

function priorityTag(level) {
  if (level === '高') return 'danger'
  if (level === '中') return 'warning'
  return 'info'
}

function statusTag(status) {
  if (status === '可生成') return 'success'
  if (status === '库存满足') return 'info'
  return 'warning'
}

function cloneProposals(list) {
  return (list || []).map((p) => ({
    ...p,
    planQuantity: Number(p.planQuantity || 0),
    remark: p.remark || ''
  }))
}

function onOpen() {
  preview.value = null
  editableRows.value = []
  selectedIds.value = []
  selectedProposal.value = null
  loadPreview()
}

async function loadPreview() {
  loading.value = true
  try {
    preview.value = await mes.previewSmartPlans(userStore.username, userStore.roleKey)
    editableRows.value = cloneProposals(preview.value?.proposals)
    selectedIds.value = feasibleRows.value.map((p) => p.orderId)
  } catch (e) {
    preview.value = null
    editableRows.value = []
    ElMessage.error(e?.message || '智能分析失败')
  } finally {
    loading.value = false
  }
}

function onSelectionChange(rows) {
  selectedIds.value = rows.map((r) => r.orderId)
  selectedProposal.value = rows.length === 1 ? rows[0] : null
}

function buildPayload(orderIds) {
  const proposals = editableRows.value
    .filter((r) => orderIds.includes(r.orderId))
    .map((r) => ({
      orderId: r.orderId,
      planQuantity: r.planQuantity,
      planStart: r.planStart,
      planEnd: r.planEnd,
      priorityLevel: r.priorityLevel,
      remark: r.remark,
      feasible: r.feasible
    }))
  return { orderIds, proposals }
}

async function generateSelected() {
  if (!selectedIds.value.length) return
  await runGenerate(selectedIds.value)
}

async function generateAll() {
  const ids = feasibleRows.value.map((p) => p.orderId)
  if (!ids.length) {
    ElMessage.warning('没有可生成的计划')
    return
  }
  await runGenerate(ids)
}

async function runGenerate(orderIds) {
  generating.value = true
  try {
    const payload = buildPayload(orderIds)
    const res = await mes.generateSmartPlans(
      payload.orderIds,
      userStore.username,
      userStore.roleKey,
      false,
      payload.proposals
    )
    ElMessage.success(res?.message || `已生成 ${res?.createdCount || 0} 份计划`)
    visible.value = false
    emit('success', res)
  } catch (e) {
    ElMessage.error(e?.message || '生成计划失败')
  } finally {
    generating.value = false
  }
}
</script>

<style scoped>
.smart-tip { margin-bottom: 12px; }
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
.score-detail { margin-top: 12px; }
.recommend-text {
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
}
</style>
