<template>
  <div class="plan-detail-panel" v-loading="loading">
    <div v-if="!plan" class="plan-detail-panel__empty">选中上方计划行，查看工序、物料、设备、人员与进度详情</div>
    <template v-else>
      <div class="plan-detail-panel__bar">
        <div class="plan-detail-panel__title">
          <span class="plan-detail-panel__id">{{ plan.id }}</span>
          <span class="plan-detail-panel__meta">{{ plan.orderNo }} · {{ plan.productModel }} · {{ plan.quantity }}台</span>
          <span class="plan-tag plan-tag--muted">V{{ plan.versionNo || '1' }}</span>
          <span v-if="conflictCount" class="plan-detail-panel__conflict">{{ conflictCount }} 项冲突</span>
        </div>
        <div class="plan-detail-panel__actions">
          <el-button size="small" :loading="validating" @click="$emit('validate')">冲突校验</el-button>
          <el-button size="small" @click="$emit('reschedule')">动态重排</el-button>
          <el-button size="small" @click="$emit('copy-version')">复制版本</el-button>
        </div>
      </div>

      <el-tabs v-model="activeSub" class="plan-detail-panel__tabs">
        <el-tab-pane label="工序" name="process">
          <el-table :data="schedules" border size="small" height="100%" class="detail-table">
            <el-table-column prop="stepNo" label="序号" width="50" align="center" />
            <el-table-column prop="stepName" label="工序" min-width="100" />
            <el-table-column prop="workshop" label="车间" width="90" />
            <el-table-column prop="equipmentCode" label="设备" width="90" />
            <el-table-column prop="plannedQuantity" label="数量" width="64" align="right" />
            <el-table-column prop="standardHours" label="工时" width="64" align="right" />
            <el-table-column prop="plannedStart" label="开始" min-width="120" />
            <el-table-column prop="plannedEnd" label="结束" min-width="120" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="物料" name="material">
          <el-table :data="materialRows" border size="small" height="100%" class="detail-table">
            <el-table-column prop="materialName" label="物料" min-width="120" />
            <el-table-column prop="requiredQty" label="需求" width="72" align="right" />
            <el-table-column prop="availableQty" label="库存" width="72" align="right" />
            <el-table-column prop="gapQty" label="缺口" width="72" align="right">
              <template #default="{ row }">
                <span :class="{ 'text-danger': row.gapQty > 0 }">{{ row.gapQty }}</span>
              </template>
            </el-table-column>
            <el-table-column label="齐套" width="64" align="center">
              <template #default="{ row }">
                <span :class="row.gapQty > 0 ? 'text-warn' : 'text-ok'">{{ row.gapQty > 0 ? '缺' : '齐' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="设备" name="equipment">
          <el-table :data="equipmentRows" border size="small" height="100%" class="detail-table">
            <el-table-column prop="equipmentCode" label="设备编号" width="100" />
            <el-table-column prop="stepName" label="工序" min-width="100" />
            <el-table-column prop="workshop" label="车间" width="90" />
            <el-table-column prop="plannedStart" label="占用开始" min-width="120" />
            <el-table-column prop="plannedEnd" label="占用结束" min-width="120" />
            <el-table-column prop="status" label="状态" width="80" align="center" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="人员" name="staff">
          <el-table :data="staffRows" border size="small" height="100%" class="detail-table">
            <el-table-column prop="stepName" label="工序" min-width="100" />
            <el-table-column prop="operator" label="负责人" width="100" />
            <el-table-column prop="dispatchStatus" label="派工状态" width="90" align="center" />
            <el-table-column prop="plannedQuantity" label="计划量" width="72" align="right" />
            <el-table-column prop="workshop" label="车间" width="90" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="进度" name="progress">
          <div class="progress-summary">
            <div class="progress-summary__item">
              <span class="progress-summary__label">完成进度</span>
              <div class="progress-summary__bar-wrap">
                <div class="progress-summary__bar" :style="{ width: `${plan.progress || 0}%` }" />
              </div>
              <span class="progress-summary__val">{{ plan.progress || 0 }}%</span>
            </div>
            <div class="progress-summary__item">
              <span class="progress-summary__label">排产风险</span>
              <span class="plan-tag" :class="riskTagClass(plan.schedulingRisk)">{{ plan.schedulingRisk }}</span>
            </div>
            <div class="progress-summary__item">
              <span class="progress-summary__label">延期风险</span>
              <span class="plan-tag" :class="delayTagClass(plan.delayRisk)">{{ plan.delayRisk }}</span>
            </div>
          </div>
          <el-table :data="progressRows" border size="small" class="detail-table detail-table--progress">
            <el-table-column prop="stepName" label="工序" min-width="100" />
            <el-table-column prop="progress" label="进度" width="120">
              <template #default="{ row }">
                <div class="mini-progress">
                  <div class="mini-progress__bar" :style="{ width: `${row.progress}%` }" />
                  <span>{{ row.progress }}%</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80" align="center" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="版本" name="version">
          <el-table :data="history" border size="small" height="100%" class="detail-table">
            <el-table-column prop="versionNo" label="版本" width="72" align="center" />
            <el-table-column prop="actionType" label="操作" width="100" />
            <el-table-column prop="reason" label="调整原因" min-width="140" show-overflow-tooltip />
            <el-table-column prop="operatorName" label="操作人" width="90" />
            <el-table-column prop="createdAt" label="时间" min-width="140" />
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <div v-if="conflicts.length" class="plan-detail-panel__risks">
        <div class="plan-detail-panel__risks-title">风险提示</div>
        <div v-for="(c, i) in conflicts" :key="i" class="plan-detail-panel__risk-item" :class="`plan-detail-panel__risk-item--${c.level}`">
          {{ c.label }}：{{ c.detail }}
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { stepProgress } from '@/utils/planMetrics'

const props = defineProps({
  plan: { type: Object, default: null },
  schedules: { type: Array, default: () => [] },
  history: { type: Array, default: () => [] },
  orderContext: { type: Object, default: null },
  conflicts: { type: Array, default: () => [] },
  mes: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  validating: { type: Boolean, default: false }
})

defineEmits(['validate', 'reschedule', 'copy-version'])

const activeSub = ref('process')

const conflictCount = computed(() => props.conflicts?.length || 0)
const materialRows = computed(() => props.orderContext?.materialGaps || [])

const equipmentRows = computed(() =>
  props.schedules.map((s) => {
    const eq = (props.mes?.equipment || []).find((e) => e.code === s.equipmentCode || e.equipmentCode === s.equipmentCode)
    return {
      equipmentCode: s.equipmentCode || '—',
      stepName: s.stepName,
      workshop: s.workshop,
      plannedStart: s.plannedStart,
      plannedEnd: s.plannedEnd,
      status: eq?.status || '—'
    }
  })
)

const staffRows = computed(() => {
  if (!props.plan || !props.mes) return []
  const workOrders = (props.mes.workOrders || []).filter((w) => w.planId === props.plan.id)
  return props.schedules.map((s) => {
    let operator = props.plan.planner || '—'
    let dispatchStatus = '—'
    for (const wo of workOrders) {
      const disp = (props.mes.dispatches || []).find(
        (d) => d.workOrderId === wo.id && (d.processStep === s.stepName || d.stepName === s.stepName)
      )
      if (disp) {
        operator = disp.operatorName || operator
        dispatchStatus = disp.status || '—'
        break
      }
    }
    return {
      stepName: s.stepName,
      operator,
      dispatchStatus,
      plannedQuantity: s.plannedQuantity,
      workshop: s.workshop
    }
  })
})

const progressRows = computed(() => {
  if (!props.plan || !props.mes) return []
  return props.schedules.map((s) => {
    const prog = stepProgress(props.plan.id, s.stepName, props.mes)
    let status = '未开始'
    if (prog >= 100) status = '已完成'
    else if (prog > 0) status = '执行中'
    return { stepName: s.stepName, progress: prog, status }
  })
})

function riskTagClass(risk) {
  if (risk === '高风险') return 'plan-tag--danger'
  if (risk === '中风险') return 'plan-tag--warn'
  if (risk === '低风险') return 'plan-tag--info'
  return 'plan-tag--ok'
}

function delayTagClass(risk) {
  if (risk === '严重延期') return 'plan-tag--danger'
  if (risk === '延期风险') return 'plan-tag--warn'
  if (risk === '关注') return 'plan-tag--info'
  return 'plan-tag--muted'
}
</script>

<style scoped>
.plan-detail-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #fff;
  border-top: 1px solid #d4d4d4;
}

.plan-detail-panel__empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 13px;
}

.plan-detail-panel__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 36px;
  padding: 0 12px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.plan-detail-panel__title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.plan-detail-panel__id {
  font-weight: 600;
  color: #111827;
}

.plan-detail-panel__meta {
  color: #6b7280;
}

.plan-detail-panel__conflict {
  color: #b45309;
  font-size: 12px;
}

.plan-detail-panel__actions {
  display: flex;
  gap: 6px;
}

.plan-detail-panel__tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.plan-detail-panel__tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 12px;
  border-bottom: 1px solid #e5e7eb;
}

.plan-detail-panel__tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  padding: 0;
}

.plan-detail-panel__tabs :deep(.el-tab-pane) {
  height: 100%;
}

.detail-table {
  height: 100%;
}

.detail-table--progress {
  margin-top: 8px;
  height: auto;
  max-height: calc(100% - 52px);
}

.detail-table :deep(.el-table th.el-table__cell) {
  background: #f3f4f6;
  color: #374151;
  font-weight: 500;
  font-size: 12px;
  padding: 4px 0;
}

.detail-table :deep(.el-table td.el-table__cell) {
  font-size: 12px;
  padding: 3px 0;
}

.progress-summary {
  display: flex;
  gap: 24px;
  padding: 8px 12px;
  border-bottom: 1px solid #f3f4f6;
}

.progress-summary__item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.progress-summary__label {
  color: #6b7280;
  min-width: 56px;
}

.progress-summary__bar-wrap {
  width: 120px;
  height: 6px;
  background: #e5e7eb;
  border-radius: 2px;
  overflow: hidden;
}

.progress-summary__bar {
  height: 100%;
  background: #3d7a5f;
  border-radius: 2px;
}

.progress-summary__val {
  font-weight: 600;
  color: #111827;
}

.mini-progress {
  display: flex;
  align-items: center;
  gap: 6px;
}

.mini-progress__bar {
  flex: 1;
  height: 4px;
  background: #3d7a5f;
  border-radius: 2px;
  max-width: 60px;
}

.plan-detail-panel__risks {
  flex-shrink: 0;
  max-height: 72px;
  overflow-y: auto;
  padding: 6px 12px;
  border-top: 1px solid #fde68a;
  background: #fffbeb;
  font-size: 12px;
}

.plan-detail-panel__risks-title {
  font-weight: 600;
  color: #92400e;
  margin-bottom: 4px;
}

.plan-detail-panel__risk-item--warning { color: #b45309; }
.plan-detail-panel__risk-item--danger { color: #b91c1c; }

.text-danger { color: #b91c1c; }
.text-warn { color: #b45309; }
.text-ok { color: #15803d; }

.plan-tag {
  display: inline-block;
  padding: 1px 6px;
  font-size: 11px;
  line-height: 1.4;
  border-radius: 2px;
  border: 1px solid transparent;
}
.plan-tag--muted { background: #f9fafb; color: #6b7280; border-color: #e5e7eb; }
.plan-tag--info { background: #eff6ff; color: #1d4ed8; border-color: #dbeafe; }
.plan-tag--warn { background: #fffbeb; color: #b45309; border-color: #fde68a; }
.plan-tag--ok { background: #f0fdf4; color: #15803d; border-color: #bbf7d0; }
.plan-tag--danger { background: #fef2f2; color: #b91c1c; border-color: #fecaca; }
</style>
