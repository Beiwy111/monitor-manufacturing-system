<template>
  <div class="planner-workspace" :class="{ 'planner-workspace--fullscreen': fullscreen }">
    <header v-if="fullscreen" class="planner-workspace__header">
      <button type="button" class="planner-workspace__back" @click="handleCancel">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </button>
      <div class="planner-workspace__title-wrap">
        <h1 class="planner-workspace__title">智能排产</h1>
        <p class="planner-workspace__subtitle">订单 → 库存 → 物料 → 设备 → 人员 → 车间 → 计划</p>
      </div>
      <el-steps :active="phaseIndex" finish-status="success" simple class="planner-workspace__steps">
        <el-step title="排产条件" />
        <el-step title="Agent 分析" />
        <el-step title="方案选择" />
      </el-steps>
    </header>

    <el-steps v-else :active="phaseIndex" finish-status="success" simple class="planner-steps">
      <el-step title="排产条件" />
      <el-step title="Agent 分析" />
      <el-step title="方案选择" />
    </el-steps>

    <div class="planner-workspace__body" :class="{
      'planner-workspace__body--scroll': phase === 'setup' && !fullscreen,
      'planner-workspace__body--setup': phase === 'setup' && fullscreen,
      'planner-workspace__body--result': phase === 'result'
    }">
      <!-- 排产条件 -->
      <div v-if="phase === 'setup'" class="planner-phase planner-phase--setup">
        <div class="setup-toolbar">
          <div class="setup-toolbar__left">
            <span v-if="selectedOrders.length" class="batch-summary batch-summary--inline">
              已选 <strong>{{ selectedOrders.length }}</strong> 个订单 · 合计 <strong>{{ totalQty }}</strong> 台
              <template v-if="isCombinedBatch">
                · 联合排产 · <strong>{{ combinedBatchCount }}</strong> 批
              </template>
              <template v-else-if="selectedOrders.length">
                · 将拆 <strong>{{ batchPreview.length }}</strong> 批
              </template>
            </span>
            <span v-else class="setup-toolbar__hint">点击卡片选择待排产订单，支持多选</span>
          </div>
          <div class="setup-toolbar__dates">
            <span class="setup-toolbar__dates-label">排产周期</span>
            <el-date-picker v-model="form.planStart" type="date" value-format="YYYY-MM-DD" placeholder="开始" :size="fullscreen ? 'large' : 'default'" />
            <span class="setup-toolbar__dates-sep">至</span>
            <el-date-picker v-model="form.planEnd" type="date" value-format="YYYY-MM-DD" placeholder="截止" :size="fullscreen ? 'large' : 'default'" />
          </div>
        </div>

        <div v-if="!pendingOrders.length" class="setup-empty">
          <div class="setup-empty__icon">📋</div>
          <p>暂无待计划订单</p>
          <span>请先在订单跟踪中审核通过订单</span>
        </div>

        <div v-else class="order-pick-grid">
          <button
            v-for="o in pendingOrders"
            :key="o.id"
            type="button"
            class="order-pick-card"
            :class="{ 'order-pick-card--selected': isOrderSelected(o.id) }"
            @click="toggleOrderSelection(o.id)"
          >
            <div class="order-pick-card__visual">
              <img class="order-pick-card__img" :src="orderProductImage(o)" :alt="o.productModel" loading="lazy" />
              <span v-if="isOrderSelected(o.id)" class="order-pick-card__badge">
                <el-icon><Check /></el-icon>
              </span>
            </div>
            <div class="order-pick-card__body">
              <div class="order-pick-card__model">{{ o.productModel || '—' }}</div>
              <div class="order-pick-card__id">{{ o.id }}</div>
              <div class="order-pick-card__meta">
                <span><strong>{{ o.quantity }}</strong> 台</span>
                <span class="order-pick-card__dot" />
                <span>交期 {{ o.deliveryDate || '—' }}</span>
              </div>
              <div v-if="o.customerName" class="order-pick-card__customer">{{ o.customerName }}</div>
            </div>
          </button>
        </div>
      </div>

      <!-- Agent 分析 -->
      <div v-else-if="phase === 'analyzing' || phase === 'review'" class="planner-phase planner-phase--analysis">
        <SchedulingThoughtPanel
          :embedded="!fullscreen"
          :fullscreen="fullscreen"
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
        <div class="result-topline">
          <span>{{ selectedOrders.length }} 笔订单</span>
          <span class="result-topline__dot" />
          <span>合计 {{ totalQty }} 台</span>
          <span class="result-topline__dot" />
          <span>{{ form.planStart }} ~ {{ form.planEnd }}</span>
          <span class="result-topline__dot" />
          <span>{{ batchPreview.length }} 个批次</span>
        </div>

        <div class="result-section result-section--schemes">
          <div class="result-section__hd">
            <span class="result-section__title">选择排产方案</span>
            <span class="result-section__hint">点击行选择策略 · 详情查看批次与排程</span>
          </div>
          <div class="scheme-list">
            <div
              v-for="s in schemeData?.schemes || []"
              :key="s.key"
              class="scheme-row"
              :class="{
                'scheme-row--active': selectedScheme?.key === s.key,
                'scheme-row--best': schemeData?.conclusion?.key === s.key
              }"
              @click="onSchemeKeySelect(s.key)"
            >
              <div class="scheme-row__check">
                <span class="scheme-row__radio" :class="{ 'scheme-row__radio--on': selectedScheme?.key === s.key }" />
              </div>
              <div class="scheme-row__main">
                <div class="scheme-row__head">
                  <span class="scheme-row__name">{{ s.label }}</span>
                  <el-tag v-if="schemeData?.conclusion?.key === s.key" size="small" type="success" effect="plain">推荐</el-tag>
                  <el-tag v-else-if="!s.conflicts?.length" size="small" type="info" effect="plain">可用</el-tag>
                  <el-tag v-else :type="s.canSubmit ? 'warning' : 'danger'" size="small" effect="plain">
                    {{ s.canSubmit ? '有提示' : '需确认' }}
                  </el-tag>
                </div>
                <div class="scheme-row__metrics">
                  <span>完工 <b>{{ s.finishDate }}</b></span>
                  <span>利用率 <b>{{ s.equipmentUtilization }}%</b></span>
                  <span>缺料 <b :class="{ 'text-warn': s.materialShortage > 0 }">{{ s.materialShortage }}</b></span>
                  <span>延期 <b :class="{ 'text-warn': s.delayDays > 0 }">{{ s.delayDays }} 天</b></span>
                  <span>换线 <b>{{ s.lineChanges }}</b></span>
                </div>
              </div>
              <el-button class="scheme-row__detail" size="small" plain @click.stop="openSchemeDetail(s)">
                详情
              </el-button>
            </div>
          </div>
        </div>

        <div v-if="selectedScheme?.conflicts?.length" class="result-risk result-risk--compact">
          <span class="result-risk__title">「{{ selectedScheme.label }}」风险提示</span>
          <ul class="result-risk__list">
            <li v-for="(c, i) in selectedScheme.conflicts" :key="i" :class="`result-risk__item--${c.level}`">
              {{ c.label }}：{{ c.detail }}
            </li>
          </ul>
        </div>
      </div>
    </div>

    <footer class="planner-workspace__footer">
      <template v-if="phase === 'setup'">
        <el-button :size="footerBtnSize" @click="handleCancel">取消</el-button>
        <el-button :size="footerBtnSize" type="primary" :disabled="!form.orderIds.length" @click="startAnalysis">
          开始 Agent 分析
        </el-button>
      </template>
      <template v-else-if="phase === 'analyzing'">
        <el-button :size="footerBtnSize" :disabled="previewLoading" @click="handleCancel">取消</el-button>
        <el-button :size="footerBtnSize" type="primary" loading disabled>分析推演中…</el-button>
      </template>
      <template v-else-if="phase === 'review'">
        <el-button :size="footerBtnSize" @click="backToSetup">返回修改</el-button>
        <el-button :size="footerBtnSize" @click="restartAnalysis">重新分析</el-button>
        <el-button :size="footerBtnSize" type="primary" :loading="schemeLoading" @click="confirmAndLoadSchemes">
          确认分析，进入方案选择
        </el-button>
      </template>
      <template v-else>
        <el-button :size="footerBtnSize" @click="backToReview">查看分析过程</el-button>
        <el-button :size="footerBtnSize" @click="restartAnalysis">重新分析</el-button>
        <el-button :size="footerBtnSize" type="success" :loading="submitLoading" :disabled="!selectedScheme || !batchPreview.length" @click="runCreate">
          确认方案并提交生产主管（{{ submitPlanCount }} 个计划）
        </el-button>
      </template>
    </footer>

    <el-drawer
      v-model="schemeDetailVisible"
      :title="schemeDetailTarget ? `${schemeDetailTarget.label} · 方案详情` : '方案详情'"
      :size="fullscreen ? '720px' : '640px'"
      destroy-on-close
      class="scheme-detail-drawer"
    >
      <template v-if="schemeDetailTarget">
        <div class="scheme-detail">
          <div class="scheme-detail__head">
            <div class="scheme-detail__tags">
              <el-tag v-if="schemeData?.conclusion?.key === schemeDetailTarget.key" type="success" effect="plain">推荐</el-tag>
              <el-tag v-else-if="!schemeDetailTarget.conflicts?.length" type="info" effect="plain">可用</el-tag>
              <el-tag v-else :type="schemeDetailTarget.canSubmit ? 'warning' : 'danger'" effect="plain">
                {{ schemeDetailTarget.canSubmit ? '有提示' : '需确认' }}
              </el-tag>
            </div>
            <p class="scheme-detail__summary">{{ schemeDetailTarget.summary }}</p>
          </div>

          <div class="scheme-detail__kpis">
            <div class="scheme-detail__kpi">
              <span>预计完工</span>
              <b>{{ schemeDetailTarget.finishDate }}</b>
            </div>
            <div class="scheme-detail__kpi">
              <span>设备利用率</span>
              <b>{{ schemeDetailTarget.equipmentUtilization }}%</b>
            </div>
            <div class="scheme-detail__kpi">
              <span>缺料数</span>
              <b :class="{ 'text-warn': schemeDetailTarget.materialShortage > 0 }">{{ schemeDetailTarget.materialShortage }}</b>
            </div>
            <div class="scheme-detail__kpi">
              <span>延期</span>
              <b :class="{ 'text-warn': schemeDetailTarget.delayDays > 0 }">{{ schemeDetailTarget.delayDays }} 天</b>
            </div>
            <div class="scheme-detail__kpi">
              <span>换线次数</span>
              <b>{{ schemeDetailTarget.lineChanges }}</b>
            </div>
          </div>

          <div v-if="schemeDetailTarget.conflicts?.length" class="scheme-detail__block scheme-detail__block--risk">
            <div class="scheme-detail__block-title">风险提示</div>
            <ul class="scheme-detail__risk-list">
              <li v-for="(c, i) in schemeDetailTarget.conflicts" :key="i" :class="`result-risk__item--${c.level}`">
                {{ c.label }}：{{ c.detail }}
              </li>
            </ul>
          </div>

          <div v-if="batchPreview.length" class="scheme-detail__block">
            <div class="scheme-detail__block-title">
              {{ isCombinedBatch ? '联合批次预览' : '批次拆分预览' }}
              <em>单批上限 {{ BATCH_SIZE }} 台</em>
            </div>
            <div class="scheme-detail__table">
              <ExcelGridTable :columns="batchColumns" :data="batchPreview" :show-row-no="true" compact>
                <template #batchNo="{ row }">
                  <span class="batch-badge">{{ row.batchNo }}/{{ row.batchCount }}</span>
                </template>
                <template #batchQty="{ row }"><strong>{{ row.batchQty }}</strong></template>
              </ExcelGridTable>
            </div>
          </div>

          <div v-if="schemeDetailSchedules.length" class="scheme-detail__block">
            <div class="scheme-detail__block-title">工序排程预览</div>
            <div class="scheme-detail__table">
              <el-table :data="schemeDetailSchedules" border size="small" max-height="280">
                <el-table-column prop="stepName" label="工序" min-width="100" />
                <el-table-column prop="workshop" label="车间" min-width="88" />
                <el-table-column prop="equipmentCode" label="设备" min-width="96" />
                <el-table-column prop="plannedQuantity" label="数量" width="72" align="right" />
                <el-table-column prop="plannedStart" label="开始" min-width="140" />
                <el-table-column prop="plannedEnd" label="结束" min-width="140" />
              </el-table>
            </div>
          </div>
        </div>
      </template>

      <template #footer>
        <el-button @click="schemeDetailVisible = false">关闭</el-button>
        <el-button type="primary" @click="selectSchemeFromDetail">选用此方案</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref, toRef } from 'vue'
import { ArrowLeft, Check } from '@element-plus/icons-vue'
import SchedulingThoughtPanel from '@/components/mes/SchedulingThoughtPanel.vue'
import ExcelGridTable from '@/components/mes/ExcelGridTable.vue'
import { usePlannerAgent } from '@/composables/usePlannerAgent'
import { getOrderProductImage } from '@/utils/orderProductImage'

const props = defineProps({
  fullscreen: { type: Boolean, default: false },
  combinedBatch: { type: Boolean, default: false },
  onClose: { type: Function, default: undefined },
  onSuccess: { type: Function, default: undefined }
})

const footerBtnSize = computed(() => (props.fullscreen ? 'large' : 'default'))

const agent = usePlannerAgent({
  combinedBatch: toRef(props, 'combinedBatch'),
  onClose: () => props.onClose?.(),
  onSuccess: () => props.onSuccess?.()
})

const {
  BATCH_SIZE,
  previewLoading,
  schemeLoading,
  submitLoading,
  analysis,
  schemeData,
  selectedScheme,
  phase,
  phaseIndex,
  form,
  pendingOrders,
  selectedOrders,
  totalQty,
  isCombinedBatch,
  combinedBatchCount,
  batchColumns,
  batchPreview,
  submitPlanCount,
  activeStep,
  activeStepKey,
  selectedStepKey,
  thoughtStream,
  evidenceList,
  allEvidence,
  currentDetail,
  selectStep,
  initOrders,
  resetState,
  onOrderChange,
  isOrderSelected,
  toggleOrderSelection,
  onSchemeKeySelect,
  backToSetup,
  backToReview,
  restartAnalysis,
  startAnalysis,
  confirmAndLoadSchemes,
  runCreate,
  handleCancel
} = agent

defineExpose({ initOrders, resetState, phase, previewLoading })

const schemeDetailVisible = ref(false)
const schemeDetailTarget = ref(null)

const schemeDetailSchedules = computed(() => {
  const list = schemeDetailTarget.value?.schedules
  return Array.isArray(list) ? list : []
})

function orderProductImage(order) {
  return getOrderProductImage(order)
}

function openSchemeDetail(scheme) {
  schemeDetailTarget.value = scheme
  schemeDetailVisible.value = true
}

function selectSchemeFromDetail() {
  if (!schemeDetailTarget.value) return
  onSchemeKeySelect(schemeDetailTarget.value.key)
  schemeDetailVisible.value = false
}
</script>

<style scoped>
.planner-workspace {
  display: flex;
  flex-direction: column;
  min-height: 200px;
}

.planner-workspace--fullscreen {
  height: calc(100vh - 98px);
  max-height: calc(100vh - 98px);
  overflow: hidden;
  background: #f8faf9;
}

.planner-workspace__header {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 24px;
  padding: 16px 28px;
  background: #fff;
  border-bottom: 1px solid #e8ece9;
  flex-shrink: 0;
}

.planner-workspace__back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid #dce3de;
  border-radius: 8px;
  background: #fff;
  color: #4b5563;
  font-size: 15px;
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
}

.planner-workspace__back:hover {
  border-color: #8fad94;
  color: #2d5a40;
}

.planner-workspace__title-wrap {
  min-width: 0;
}

.planner-workspace__title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1a2b22;
  line-height: 1.3;
}

.planner-workspace__subtitle {
  margin: 4px 0 0;
  font-size: 14px;
  color: #6b7280;
}

.planner-workspace__steps {
  min-width: 420px;
}

.planner-workspace__steps :deep(.el-step__title) {
  font-size: 15px;
}

.planner-workspace__body {
  flex: 1;
  min-height: 0;
  padding: 20px 28px;
  overflow: hidden;
}

.planner-workspace__body--scroll {
  overflow: auto;
}

.planner-workspace--fullscreen .planner-workspace__body--result {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 12px 20px 8px;
}

.planner-workspace--fullscreen .planner-workspace__body {
  display: flex;
  flex-direction: column;
  padding: 12px 20px 8px;
  overflow: hidden;
}

.planner-workspace__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 14px 28px;
  background: #fff;
  border-top: 1px solid #e8ece9;
  flex-shrink: 0;
}

.planner-steps {
  margin-bottom: 18px;
  padding: 0 4px;
}

.planner-phase {
  min-height: 200px;
}

.planner-workspace--fullscreen .planner-workspace__body--setup {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 12px 20px 8px;
}

.planner-workspace--fullscreen .planner-phase--setup {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  max-width: none;
  margin: 0;
}

.setup-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px 20px;
  margin-bottom: 14px;
  flex-shrink: 0;
}

.setup-toolbar__left {
  flex: 1;
  min-width: 200px;
}

.setup-toolbar__hint {
  font-size: 15px;
  color: #6b7280;
}

.setup-toolbar__dates {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.setup-toolbar__dates-label {
  font-size: 14px;
  color: #6b7280;
  white-space: nowrap;
}

.setup-toolbar__dates-sep {
  font-size: 14px;
  color: #9ca3af;
}

.batch-summary--inline {
  margin: 0;
  display: inline-block;
}

.order-pick-grid {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  padding: 4px 2px 8px;
  align-content: start;
}

.order-pick-card {
  display: flex;
  flex-direction: column;
  text-align: left;
  padding: 0;
  border: 2px solid #e5ebe7;
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.15s;
}

.order-pick-card:hover {
  border-color: #b8d4c4;
  box-shadow: 0 6px 20px rgba(45, 90, 64, 0.08);
  transform: translateY(-2px);
}

.order-pick-card--selected {
  border-color: #2d8a66;
  box-shadow: 0 0 0 2px rgba(45, 138, 102, 0.18);
  background: #fafff8;
}

.order-pick-card__visual {
  position: relative;
  aspect-ratio: 4 / 3;
  background: linear-gradient(145deg, #f0f4f2 0%, #e8eeea 100%);
  overflow: hidden;
}

.order-pick-card__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.order-pick-card__badge {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #2d8a66;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(45, 138, 102, 0.35);
}

.order-pick-card__body {
  padding: 12px 14px 14px;
}

.order-pick-card__model {
  font-size: 15px;
  font-weight: 700;
  color: #1a2b22;
  line-height: 1.35;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.order-pick-card__id {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 8px;
  word-break: break-all;
}

.order-pick-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #4b5563;
}

.order-pick-card__meta strong {
  color: #2d8a66;
  font-size: 16px;
}

.order-pick-card__dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #d1d5db;
}

.order-pick-card__customer {
  margin-top: 6px;
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.setup-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #9ca3af;
  font-size: 15px;
}

.setup-empty__icon {
  font-size: 40px;
  opacity: 0.6;
}

.setup-empty p {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #6b7280;
}

@media (max-width: 1400px) {
  .order-pick-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .order-pick-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .order-pick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.planner-phase--result {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.planner-workspace--fullscreen .planner-phase--result {
  max-height: 100%;
}

.result-topline {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #e8ece9;
  border-radius: 8px;
  font-size: 15px;
  color: #374151;
  flex-shrink: 0;
}

.result-topline__dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #c5cdc7;
}

.result-section--schemes {
  flex-shrink: 0;
}

.result-section--batch {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.scheme-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.scheme-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  text-align: left;
  padding: 14px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.scheme-row:hover {
  border-color: #b3d8c4;
}

.scheme-row--active {
  border-color: #2d8a66;
  background: #f6ffed;
  box-shadow: 0 0 0 1px #2d8a66;
}

.scheme-row--best:not(.scheme-row--active) {
  border-color: #c2e7b0;
}

.scheme-row__check {
  padding-top: 4px;
  flex-shrink: 0;
}

.scheme-row__radio {
  display: block;
  width: 18px;
  height: 18px;
  border: 2px solid #c0c4cc;
  border-radius: 50%;
  transition: border-color 0.15s;
}

.scheme-row__radio--on {
  border-color: #2d8a66;
  box-shadow: inset 0 0 0 4px #2d8a66;
}

.scheme-row__main {
  flex: 1;
  min-width: 0;
}

.scheme-row__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.scheme-row__name {
  font-size: 16px;
  font-weight: 600;
  color: #1a2b22;
}

.scheme-row__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  font-size: 14px;
  color: #6b7280;
}

.scheme-row__metrics b {
  color: #303133;
  font-weight: 600;
}

.scheme-row__desc {
  margin: 8px 0 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}

.scheme-row__detail {
  flex-shrink: 0;
  margin-left: auto;
}

.scheme-detail__head {
  margin-bottom: 16px;
}

.scheme-detail__tags {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.scheme-detail__summary {
  margin: 0;
  font-size: 15px;
  color: #4b5563;
  line-height: 1.65;
}

.scheme-detail__kpis {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-bottom: 18px;
}

.scheme-detail__kpi {
  padding: 12px 10px;
  border: 1px solid #eef2f7;
  border-radius: 10px;
  background: #f8fafc;
  text-align: center;
}

.scheme-detail__kpi span {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.scheme-detail__kpi b {
  font-size: 16px;
  color: #1a2b22;
}

.scheme-detail__block {
  margin-bottom: 18px;
}

.scheme-detail__block--risk {
  padding: 12px 14px;
  border: 1px solid #faecd8;
  border-radius: 10px;
  background: #fdf6ec;
}

.scheme-detail__block-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.scheme-detail__block-title em {
  margin-left: 8px;
  font-style: normal;
  font-size: 13px;
  font-weight: 400;
  color: #909399;
}

.scheme-detail__risk-list {
  margin: 0;
  padding-left: 18px;
  font-size: 14px;
  line-height: 1.65;
}

.scheme-detail__table {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

:deep(.scheme-detail-drawer .el-drawer__body) {
  padding-top: 8px;
}

@media (max-width: 768px) {
  .scheme-detail__kpis {
    grid-template-columns: repeat(2, 1fr);
  }
}

.result-risk--compact {
  flex-shrink: 0;
  padding: 10px 14px;
}

.result-risk__list {
  margin: 6px 0 0;
  padding-left: 18px;
  font-size: 13px;
  line-height: 1.6;
}

.result-table-wrap--scroll {
  flex: 1;
  min-height: 120px;
  max-height: 220px;
  overflow: auto;
}

.planner-workspace--fullscreen .result-table-wrap--scroll {
  max-height: none;
  flex: 1;
  min-height: 0;
}

.planner-workspace--fullscreen .planner-phase--analysis {
  flex: 1;
  min-height: 0;
  max-height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.planner-workspace--fullscreen .planner-phase--analysis :deep(.thought-shell) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.planner-workspace--fullscreen .review-summary {
  font-size: 14px;
  padding: 10px 14px;
  margin-top: 8px;
  flex-shrink: 0;
}

/* —— 方案选择页 —— */
.result-head {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
  overflow: hidden;
}

.result-context {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 0;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  font-size: 14px;
  color: #606266;
}

.planner-workspace--fullscreen .result-context {
  font-size: 15px;
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
  padding: 14px 8px;
  text-align: center;
  border-right: 1px solid #f0f0f0;
}

.result-kpi:last-child {
  border-right: none;
}

.result-kpi__label {
  display: block;
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.planner-workspace--fullscreen .result-kpi__label {
  font-size: 14px;
}

.result-kpi__val {
  font-size: 24px;
  font-weight: 600;
  color: #2d8a66;
  line-height: 1;
}

.result-kpi__val small {
  font-size: 14px;
  font-weight: 500;
  margin-left: 1px;
}

.result-kpi--warn .result-kpi__val {
  color: #e6a23c;
}

.result-recommend {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid #d4edda;
  border-radius: 8px;
  background: #f6ffed;
}

.result-recommend__icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #2d8a66;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.result-recommend__title {
  font-size: 15px;
  color: #303133;
  margin-bottom: 4px;
}

.result-recommend__title strong {
  color: #2d8a66;
}

.result-recommend__summary {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.result-evidence {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.result-evidence__chip {
  display: inline-block;
  padding: 3px 10px;
  font-size: 13px;
  color: #606266;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.result-risk {
  padding: 12px 16px;
  border: 1px solid #faecd8;
  border-radius: 8px;
  background: #fdf6ec;
  font-size: 14px;
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
  font-size: 15px;
  font-weight: 600;
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
  font-size: 13px;
  color: #909399;
}

.scheme-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.scheme-card {
  text-align: left;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
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
  font-size: 16px;
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
  font-size: 13px;
  color: #909399;
}

.scheme-card__metric span {
  display: block;
  margin-bottom: 2px;
}

.scheme-card__metric b {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.scheme-card__desc {
  margin: 0;
  font-size: 13px;
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
  border-radius: 8px;
  overflow: hidden;
}

.batch-badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 13px;
  color: #2d8a66;
  background: #f0f9eb;
  border: 1px solid #c2e7b0;
  border-radius: 4px;
}

.result-footer-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
  font-size: 14px;
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
  font-size: 13px;
}

@media (max-width: 900px) {
  .result-kpis { grid-template-columns: repeat(3, 1fr); }
  .scheme-cards { grid-template-columns: 1fr; }
  .planner-workspace__header {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .planner-workspace__steps { min-width: 0; }
}

.review-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-top: 10px;
  padding: 10px 14px;
  border: 1px solid #e5e7eb;
  background: #f0fdf4;
  font-size: 14px;
  color: #374151;
  border-radius: 8px;
}

.review-summary strong {
  color: #15803d;
}

.batch-summary {
  padding: 10px 14px;
  border-radius: 8px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  font-size: 14px;
  color: #409eff;
}

.planner-workspace--fullscreen .batch-summary {
  font-size: 15px;
  padding: 10px 14px;
}

.batch-summary strong {
  font-size: 16px;
}

.planner-workspace--fullscreen .batch-summary strong {
  font-size: 17px;
}

</style>
