<template>
  <div class="calc-engine" :class="{ 'calc-engine--fullscreen': fullscreen }">
    <header class="calc-engine__header">
      <span class="calc-engine__title">排产计算引擎</span>
      <span class="calc-engine__status" :class="statusClass">
        <span class="calc-engine__dot" />
        {{ statusText }}
      </span>
    </header>

    <div v-if="!model && running" class="calc-engine__waiting">
      等待分析数据…
    </div>

    <div v-else-if="!model" class="calc-engine__waiting">
      分析完成后展示公式与代入过程
    </div>

    <div v-else class="calc-engine__body" ref="bodyRef">
      <!-- 1. 算法输入 -->
      <section class="calc-block" :class="{ 'calc-block--lit': sectionLit('inputs') }">
        <div class="calc-block__head">
          <span class="calc-block__idx">1</span>
          <span>算法输入</span>
        </div>
        <div class="calc-input-grid">
          <div
            v-for="row in inputRows"
            :key="row.id"
            class="calc-input-row"
            :class="{ 'calc-input-row--lit': isRevealed(row.revealId) }"
          >
            <span class="calc-input-row__label">{{ row.label }}</span>
            <span class="calc-input-row__val">
              <template v-if="isRevealed(row.revealId)">
                <CalcAnimatedValue
                  v-if="row.numeric != null"
                  :value="row.numeric"
                  :lit="true"
                  :animate="running && justRevealed(row.revealId)"
                  :suffix="row.suffix || ''"
                  :decimals="row.decimals ?? 0"
                />
                <span v-else>{{ row.text }}</span>
              </template>
              <span v-else class="calc-input-row__pending">—</span>
            </span>
          </div>
        </div>
      </section>

      <div class="calc-flow-line" :class="{ 'calc-flow-line--lit': sectionLit('steps') }" />

      <!-- 2. 分步计算 -->
      <section class="calc-block" :class="{ 'calc-block--lit': sectionLit('steps') }">
        <div class="calc-block__head">
          <span class="calc-block__idx">2</span>
          <span>分步计算</span>
        </div>
        <div class="calc-formula-list">
          <div
            v-for="step in model.calcSteps.filter((s) => s.id !== 'calcConstraint')"
            :key="step.id"
            class="calc-formula"
            :class="{
              'calc-formula--lit': isRevealed(step.id),
              'calc-formula--active': activeCalcId === step.id
            }"
          >
            <div class="calc-formula__label">
              <span class="calc-formula__tag">{{ step.label }}</span>
              <span v-if="isRevealed(step.id)" class="calc-formula__state">已计算</span>
              <span v-else class="calc-formula__state calc-formula__state--wait">待解锁</span>
            </div>
            <div class="calc-formula__expr">{{ step.formula }}</div>
            <div v-if="isRevealed(step.id)" class="calc-formula__sub">
              <span class="calc-formula__sub-label">代入</span>
              <span class="calc-formula__sub-val">{{ step.substitution }}</span>
              <span class="calc-formula__eq">=</span>
              <CalcAnimatedValue
                :value="step.result"
                :lit="true"
                :animate="running && justRevealed(step.id)"
                :suffix="` ${step.unit}`"
                :decimals="step.unit === '台/天' ? 1 : 0"
              />
            </div>
            <div v-if="isRevealed(step.id) && step.detail" class="calc-formula__detail">{{ step.detail }}</div>
          </div>
        </div>
      </section>

      <div class="calc-flow-line" :class="{ 'calc-flow-line--lit': isRevealed('calcConstraint') }" />

      <!-- 3. 约束求解 -->
      <section
        class="calc-block calc-block--constraint"
        :class="{ 'calc-block--lit': isRevealed('calcConstraint') }"
      >
        <div class="calc-block__head">
          <span class="calc-block__idx">3</span>
          <span>约束求解</span>
        </div>
        <div v-if="constraintStep" class="calc-constraint">
          <div class="calc-formula__expr">{{ constraintStep.formula }}</div>
          <div v-if="isRevealed('calcConstraint')" class="calc-formula__sub calc-formula__sub--highlight">
            <span class="calc-formula__sub-val">{{ constraintStep.substitution }}</span>
            <span class="calc-formula__eq">=</span>
            <CalcAnimatedValue
              :value="constraintStep.result"
              :lit="true"
              :animate="running && justRevealed('calcConstraint')"
              suffix=" 台"
            />
          </div>
        </div>
      </section>

      <div class="calc-flow-line" :class="{ 'calc-flow-line--lit': isRevealed('output') }" />

      <!-- 4. 结果输出 -->
      <section class="calc-block calc-block--output" :class="{ 'calc-block--lit': isRevealed('output') }">
        <div class="calc-block__head">
          <span class="calc-block__idx">4</span>
          <span>结果输出</span>
        </div>
        <div v-if="isRevealed('output')" class="calc-output">
          <div class="calc-output__row">
            <span>推荐排产</span>
            <CalcAnimatedValue
              :value="model.outputs.recommended"
              :lit="true"
              :animate="running && justRevealed('output')"
              suffix=" 台"
            />
          </div>
          <div class="calc-output__row">
            <span>现货发货</span>
            <CalcAnimatedValue
              :value="model.outputs.shipStock"
              :lit="true"
              :animate="running && justRevealed('output')"
              suffix=" 台"
            />
          </div>
          <div class="calc-output__row calc-output__row--emph">
            <span>总交付</span>
            <CalcAnimatedValue
              :value="model.outputs.totalDelivery"
              :lit="true"
              :animate="running && justRevealed('output')"
              suffix=" 台"
            />
          </div>
          <div class="calc-output__bottleneck">
            <span class="calc-output__bottleneck-label">主要瓶颈</span>
            <span
              v-for="(b, i) in model.outputs.bottlenecks"
              :key="i"
              class="calc-output__tag"
            >{{ b }}</span>
          </div>
          <p v-if="model.outputs.recommendation" class="calc-output__note">{{ model.outputs.recommendation }}</p>
        </div>
        <div v-else class="calc-output calc-output--pending">等待约束求解完成…</div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import CalcAnimatedValue from '@/components/mes/CalcAnimatedValue.vue'
import { buildCalcEngineModel, getRevealedCalcIds } from '@/composables/useSchedulingCalcEngine'
import { scrollChildIntoView } from '@/utils/scrollFocus'

const props = defineProps({
  analysis: { type: Object, default: null },
  revealedStepKeys: { type: Array, default: () => [] },
  activeStepKey: { type: String, default: '' },
  running: { type: Boolean, default: false },
  fullscreen: { type: Boolean, default: false }
})

const model = computed(() => buildCalcEngineModel(props.analysis))

const bodyRef = ref(null)
const revealedSet = computed(() => getRevealedCalcIds(props.revealedStepKeys))
const lastRevealedId = ref('')

watch(
  () => Array.from(revealedSet.value).sort().join(','),
  (joined, prev) => {
    const ids = joined ? joined.split(',') : []
    const prevIds = prev ? prev.split(',') : []
    const added = ids.find((id) => !prevIds.includes(id))
    if (added) lastRevealedId.value = added
  }
)

watch(
  () => props.activeStepKey,
  () => {
    scrollCalcToFocus()
  }
)

watch(
  () => Array.from(revealedSet.value).sort().join(','),
  () => {
    if (props.running) scrollCalcToFocus()
  }
)

function scrollCalcToFocus() {
  scrollChildIntoView(bodyRef.value, (container) => {
    return container.querySelector('.calc-formula--active')
      || container.querySelector('.calc-block--constraint.calc-block--lit')
      || container.querySelector('.calc-formula--lit:last-of-type')
      || container.querySelector('.calc-block--lit:last-of-type')
  })
}

function isRevealed(id) {
  if (!props.running && model.value) {
    if (!props.revealedStepKeys.length) return true
    return revealedSet.value.has(id)
  }
  return revealedSet.value.has(id)
}

function justRevealed(id) {
  return lastRevealedId.value === id
}

function sectionLit(section) {
  if (section === 'inputs') {
    return isRevealed('inputOrder') || isRevealed('inputStock')
  }
  if (section === 'steps') {
    return model.value?.calcSteps.some((s) => s.id !== 'calcConstraint' && isRevealed(s.id))
  }
  return false
}

const activeCalcId = computed(() => {
  if (!props.activeStepKey) return ''
  const map = {
    inventory: 'calcGap',
    material: 'calcMaterial',
    equipment: 'calcEquipment',
    operator: 'calcOperator',
    allocate: 'calcAllocate',
    result: 'calcConstraint'
  }
  return map[props.activeStepKey] || ''
})

const constraintStep = computed(() =>
  model.value?.calcSteps.find((s) => s.id === 'calcConstraint')
)

const statusText = computed(() => {
  if (props.running) return '计算中'
  if (model.value) return '计算完成'
  return '待开始'
})

const statusClass = computed(() => ({
  'calc-engine__status--run': props.running,
  'calc-engine__status--done': !props.running && model.value
}))

const inputRows = computed(() => {
  const m = model.value
  if (!m) return []
  const { inputs } = m
  const cycleText = inputs.planStart && inputs.planEnd
    ? `${inputs.planStart} ~ ${inputs.planEnd}（${inputs.workDays} 天）`
    : `${inputs.workDays} 天`
  return [
    { id: 'orderQty', revealId: 'inputOrder', label: '订单需求量', numeric: inputs.orderQty, suffix: ' 台' },
    { id: 'fgStock', revealId: 'inputStock', label: '成品库存', numeric: inputs.fgStock, suffix: ' 台' },
    { id: 'needProduce', revealId: 'calcGap', label: '生产缺口', numeric: inputs.needProduce, suffix: ' 台' },
    { id: 'materialLimit', revealId: 'inputMaterial', label: '物料上限', numeric: inputs.materialLimit, suffix: ' 台' },
    { id: 'equipmentLimit', revealId: 'inputEquipment', label: '设备上限', numeric: inputs.equipmentLimit, suffix: ' 台' },
    { id: 'operatorLimit', revealId: 'inputOperator', label: '人员上限', numeric: inputs.operatorLimit, suffix: ' 台' },
    { id: 'cycle', revealId: 'inputCycle', label: '计划周期', text: cycleText }
  ]
})
</script>

<style scoped>
.calc-engine {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fafbfc;
  font-size: 13px;
}

.calc-engine__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #eef2f7;
  background: #fff;
  flex-shrink: 0;
}

.calc-engine--fullscreen .calc-engine__header {
  padding: 18px 22px;
}

.calc-engine__title {
  font-size: 14px;
  font-weight: 700;
  color: #1a2b4a;
}

.calc-engine--fullscreen .calc-engine__title {
  font-size: 17px;
}

.calc-engine__status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  padding: 2px 10px;
  border-radius: 12px;
  background: #f4f4f5;
}

.calc-engine__status--run {
  color: #1677ff;
  background: #e8f4ff;
}

.calc-engine__status--done {
  color: #15803d;
  background: #f0fdf4;
}

.calc-engine__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.calc-engine__status--run .calc-engine__dot {
  animation: calc-pulse 1.2s ease infinite;
}

@keyframes calc-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.85); }
}

.calc-engine__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 14px 16px;
}

.calc-engine--fullscreen .calc-engine__body {
  padding: 16px 20px 20px;
}

.calc-engine__waiting {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  color: #909399;
  font-size: 13px;
  text-align: center;
}

.calc-block {
  opacity: 0.45;
  transition: opacity 0.4s ease;
}

.calc-block--lit {
  opacity: 1;
}

.calc-block__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.calc-engine--fullscreen .calc-block__head {
  font-size: 15px;
}

.calc-block__idx {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #e5e7eb;
  color: #6b7280;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.calc-block--lit .calc-block__idx {
  background: #2d8a66;
  color: #fff;
}

.calc-input-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 12px;
}

.calc-input-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 6px;
  background: #fff;
  border: 1px solid #eef2f7;
  opacity: 0.5;
  transition: opacity 0.35s, border-color 0.35s, box-shadow 0.35s;
}

.calc-input-row--lit {
  opacity: 1;
  border-color: #bbf7d0;
  box-shadow: 0 0 0 1px rgba(45, 138, 102, 0.08);
}

.calc-input-row__label {
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap;
}

.calc-input-row__val {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  text-align: right;
}

.calc-input-row__pending {
  color: #d1d5db;
  font-weight: 400;
}

.calc-flow-line {
  width: 2px;
  height: 14px;
  margin: 4px auto 4px 18px;
  background: #e5e7eb;
  border-radius: 1px;
  transition: background 0.4s;
}

.calc-flow-line--lit {
  background: linear-gradient(180deg, #2d8a66, #86efac);
}

.calc-formula-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.calc-formula {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #eef2f7;
  background: #fff;
  opacity: 0.45;
  transition: opacity 0.4s, border-color 0.35s, box-shadow 0.35s;
}

.calc-formula--lit {
  opacity: 1;
  border-color: #d1fae5;
}

.calc-formula--active {
  box-shadow: 0 0 0 2px rgba(45, 138, 102, 0.15);
}

.calc-formula__label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.calc-formula__tag {
  font-size: 12px;
  font-weight: 600;
  color: #2d5a40;
}

.calc-formula__state {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 8px;
  background: #f0fdf4;
  color: #15803d;
}

.calc-formula__state--wait {
  background: #f4f4f5;
  color: #909399;
}

.calc-formula__expr {
  font-size: 12px;
  color: #6b7280;
  font-family: ui-monospace, 'Cascadia Code', 'Consolas', monospace;
  line-height: 1.5;
  margin-bottom: 4px;
}

.calc-engine--fullscreen .calc-formula__expr {
  font-size: 13px;
}

.calc-formula__sub {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed #e5e7eb;
  font-family: ui-monospace, 'Cascadia Code', 'Consolas', monospace;
  font-size: 13px;
  color: #374151;
}

.calc-formula__sub--highlight {
  border-top-color: #bbf7d0;
  background: #f0fdf4;
  margin: 8px -4px 0;
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid #bbf7d0;
}

.calc-formula__sub-label {
  font-size: 11px;
  color: #9ca3af;
  font-family: inherit;
}

.calc-formula__sub-val {
  color: #4b5563;
}

.calc-formula__eq {
  color: #9ca3af;
  font-weight: 600;
}

.calc-formula__detail {
  margin-top: 4px;
  font-size: 11px;
  color: #909399;
}

.calc-block--constraint .calc-constraint {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #eef2f7;
  background: #fff;
}

.calc-block--constraint.calc-block--lit .calc-constraint {
  border-color: #bbf7d0;
}

.calc-output {
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #bbf7d0;
  background: linear-gradient(135deg, #f0fdf4 0%, #fff 100%);
}

.calc-output--pending {
  border-color: #eef2f7;
  background: #fff;
  color: #909399;
  font-size: 12px;
  text-align: center;
}

.calc-output__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  font-size: 13px;
  color: #4b5563;
}

.calc-output__row--emph {
  margin-top: 4px;
  padding-top: 8px;
  border-top: 1px solid #bbf7d0;
  font-weight: 600;
  color: #1a2b22;
  font-size: 14px;
}

.calc-output__bottleneck {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
}

.calc-output__bottleneck-label {
  font-size: 12px;
  color: #6b7280;
}

.calc-output__tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #fef3c7;
  color: #b45309;
  border: 1px solid #fde68a;
}

.calc-output__note {
  margin: 10px 0 0;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.6;
}
</style>
