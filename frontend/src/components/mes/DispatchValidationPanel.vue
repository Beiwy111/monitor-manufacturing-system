<template>
  <div class="dispatch-validation">
    <header class="dispatch-validation__header">
      <span class="dispatch-validation__title">派工实时校验</span>
      <span class="dispatch-validation__badge" :class="headerBadgeClass">
        {{ running ? '校验中' : preview ? '校验完成' : '待开始' }}
      </span>
    </header>

    <div v-if="!preview && running" class="dispatch-validation__waiting">等待派工分析数据…</div>
    <div v-else-if="!preview" class="dispatch-validation__waiting">开始分析后展示校验项</div>

    <div v-else ref="bodyRef" class="dispatch-validation__body">
      <section class="dv-block" :class="{ 'dv-block--lit': model.progress > 0 }">
        <div class="dv-progress-head">
          <span class="dv-progress-head__label">当前分析</span>
          <span class="dv-progress-head__step">{{ model.currentStepLabel }}</span>
        </div>
        <div v-if="model.currentProcess" class="dv-progress-process">
          <span class="dv-progress-process__tag">{{ model.currentProcess }}</span>
        </div>
        <div class="dv-progress-bar">
          <div class="dv-progress-bar__fill" :style="{ width: `${model.progress}%` }" />
        </div>
        <div class="dv-progress-meta">{{ model.progress }}%</div>
      </section>

      <div class="dv-flow-line" :class="{ 'dv-flow-line--lit': model.checks.some((c) => !c.pending) }" />

      <section class="dv-block dv-block--checks">
        <div class="dv-block__title">校验项</div>
        <ul class="dv-check-list">
          <li
            v-for="item in model.checks"
            :key="item.id"
            class="dv-check-row"
            :class="{
              'dv-check-row--lit': !item.pending,
              'dv-check-row--active': activeCheckId === item.id
            }"
          >
            <span class="dv-check-row__label">{{ item.label }}</span>
            <span class="dv-check-row__status" :class="`dv-check-row__status--${item.pending ? 'pending' : item.status}`">
              {{ item.pending ? '待校验' : statusLabel(item.status) }}
            </span>
            <p v-if="!item.pending && item.summary" class="dv-check-row__summary">{{ item.summary }}</p>
            <div
              v-if="!item.pending && item.status !== 'pass' && (item.conflictReason || item.autoFix || item.impact)"
              class="dv-check-detail"
            >
              <p v-if="item.conflictReason"><em>原因</em>{{ item.conflictReason }}</p>
              <p v-if="item.autoFix"><em>处理</em>{{ item.autoFix }}</p>
              <p v-if="item.impact"><em>影响</em>{{ item.impact }}</p>
            </div>
          </li>
        </ul>
      </section>

      <div class="dv-flow-line" :class="{ 'dv-flow-line--lit': model.conclusion }" />

      <section v-if="model.conclusion" class="dv-block dv-block--conclusion">
        <div class="dv-block__title">综合结论</div>
        <div class="dv-conclusion" :class="{ 'dv-conclusion--block': !model.conclusion.canGenerate }">
          <div class="dv-conclusion__main">
            <span class="dv-conclusion__action">{{ model.conclusion.label }}</span>
            <span class="dv-conclusion__risk" :class="`dv-conclusion__risk--${model.conclusion.risk}`">
              {{ model.conclusion.risk }}
            </span>
          </div>
          <p class="dv-conclusion__detail">{{ model.conclusion.detail }}</p>
        </div>
      </section>
      <section v-else-if="running" class="dv-block dv-block--conclusion dv-block--pending">
        <div class="dv-block__title">综合结论</div>
        <p class="dv-conclusion__placeholder">等待匹配完成后汇总…</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import {
  buildDispatchValidationModel,
  statusLabel,
  DISPATCH_REVEAL_BY_STEP
} from '@/composables/useDispatchValidation'
import { scrollChildIntoView } from '@/utils/scrollFocus'

const props = defineProps({
  preview: { type: Object, default: null },
  mes: { type: Object, default: null },
  revealedStepKeys: { type: Array, default: () => [] },
  activeStepKey: { type: String, default: '' },
  running: { type: Boolean, default: false }
})

const model = computed(() =>
  buildDispatchValidationModel(props.preview, props.mes, {
    activeStepKey: props.activeStepKey,
    revealedStepKeys: props.revealedStepKeys,
    running: props.running
  })
)

const bodyRef = ref(null)

const headerBadgeClass = computed(() => ({
  'dispatch-validation__badge--run': props.running,
  'dispatch-validation__badge--done': !props.running && props.preview
}))

const activeCheckId = computed(() => {
  const stepIds = (DISPATCH_REVEAL_BY_STEP[props.activeStepKey] || [])
    .filter((id) => id !== 'progress' && id !== 'conclusion')
  let lastInStep = ''
  for (const check of model.value.checks) {
    if (!check.pending && stepIds.includes(check.id)) {
      lastInStep = check.id
    }
  }
  if (lastInStep) return lastInStep

  const lit = model.value.checks.filter((c) => !c.pending)
  return lit.length ? lit[lit.length - 1].id : ''
})

const litCheckSignature = computed(() =>
  model.value.checks.filter((c) => !c.pending).map((c) => c.id).join(',')
)

watch(
  [
    () => props.activeStepKey,
    litCheckSignature,
    () => model.value.conclusion?.label,
    () => model.value.progress
  ],
  () => {
    if (!props.preview && !props.running) return
    scrollValidationToFocus()
  }
)

function scrollValidationToFocus() {
  scrollChildIntoView(bodyRef.value, (container) => {
    if (model.value.conclusion) {
      return container.querySelector('.dv-block--conclusion:not(.dv-block--pending)')
    }
    const active = container.querySelector('.dv-check-row--active')
    if (active) return active
    const litRows = container.querySelectorAll('.dv-check-row--lit')
    if (litRows.length) return litRows[litRows.length - 1]
    return container.querySelector('.dv-block--lit')
  })
}
</script>

<style scoped>
.dispatch-validation {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fafbfc;
  font-size: 13px;
}

.dispatch-validation__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #eef2f7;
  background: #fff;
  flex-shrink: 0;
}

.dispatch-validation__title {
  font-size: 14px;
  font-weight: 700;
  color: #1a2b4a;
}

.dispatch-validation__badge {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 12px;
  background: #f4f4f5;
  color: #909399;
}

.dispatch-validation__badge--run {
  color: #1677ff;
  background: #e8f4ff;
}

.dispatch-validation__badge--done {
  color: #15803d;
  background: #f0fdf4;
}

.dispatch-validation__waiting {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  color: #909399;
  font-size: 13px;
  text-align: center;
}

.dispatch-validation__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 14px 16px;
}

.dv-block {
  opacity: 0.45;
  transition: opacity 0.35s ease;
}

.dv-block--lit,
.dv-block--checks,
.dv-block--conclusion {
  opacity: 1;
}

.dv-block__title {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 8px;
  text-transform: none;
  letter-spacing: 0.02em;
}

.dv-progress-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 6px;
}

.dv-progress-head__label {
  font-size: 12px;
  color: #9ca3af;
}

.dv-progress-head__step {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.dv-progress-process {
  margin-bottom: 8px;
}

.dv-progress-process__tag {
  display: inline-block;
  font-size: 12px;
  color: #1570ef;
  background: #eff8ff;
  border: 1px solid #d1e9ff;
  padding: 2px 8px;
  border-radius: 4px;
}

.dv-progress-bar {
  height: 4px;
  background: #e5e7eb;
  border-radius: 2px;
  overflow: hidden;
}

.dv-progress-bar__fill {
  height: 100%;
  background: linear-gradient(90deg, #1570ef, #2e90fa);
  border-radius: 2px;
  transition: width 0.45s ease;
}

.dv-progress-meta {
  margin-top: 4px;
  font-size: 11px;
  color: #9ca3af;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.dv-flow-line {
  width: 2px;
  height: 12px;
  margin: 8px auto 8px 12px;
  background: #e5e7eb;
  border-radius: 1px;
  transition: background 0.35s;
}

.dv-flow-line--lit {
  background: linear-gradient(180deg, #1570ef, #93c5fd);
}

.dv-check-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.dv-check-row {
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
  opacity: 0.4;
  transition: opacity 0.35s;
}

.dv-check-row--lit {
  opacity: 1;
}

.dv-check-row--active {
  background: linear-gradient(90deg, rgba(21, 112, 239, 0.06), transparent);
  margin: 0 -8px;
  padding-left: 8px;
  padding-right: 8px;
}

.dv-check-row__label {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.dv-check-row__status {
  float: right;
  font-size: 11px;
  font-weight: 600;
  padding: 1px 8px;
  border-radius: 10px;
}

.dv-check-row__status--pending {
  color: #9ca3af;
  background: #f3f4f6;
}

.dv-check-row__status--pass {
  color: #15803d;
  background: #f0fdf4;
}

.dv-check-row__status--warn {
  color: #b45309;
  background: #fffbeb;
}

.dv-check-row__status--conflict {
  color: #b91c1c;
  background: #fef2f2;
}

.dv-check-row__summary {
  clear: both;
  margin: 6px 0 0;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
}

.dv-check-detail {
  margin-top: 6px;
  padding: 8px 10px;
  background: #f9fafb;
  border-left: 2px solid #e5e7eb;
  font-size: 11px;
  color: #4b5563;
  line-height: 1.55;
}

.dv-check-detail p {
  margin: 0 0 4px;
}

.dv-check-detail p:last-child {
  margin-bottom: 0;
}

.dv-check-detail em {
  font-style: normal;
  color: #9ca3af;
  margin-right: 6px;
}

.dv-conclusion {
  padding: 10px 12px;
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  border-radius: 6px;
}

.dv-conclusion--block {
  border-color: #fecaca;
  background: #fef2f2;
}

.dv-conclusion__main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.dv-conclusion__action {
  font-size: 14px;
  font-weight: 700;
  color: #1a2b22;
}

.dv-conclusion__risk {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
}

.dv-conclusion__risk--低风险 {
  color: #15803d;
  background: #dcfce7;
}

.dv-conclusion__risk--中风险 {
  color: #b45309;
  background: #fef3c7;
}

.dv-conclusion__risk--高风险 {
  color: #b91c1c;
  background: #fee2e2;
}

.dv-conclusion__detail {
  margin: 0;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.55;
}

.dv-conclusion__placeholder {
  margin: 0;
  font-size: 12px;
  color: #9ca3af;
}

.dv-block--pending {
  opacity: 0.65;
}
</style>
