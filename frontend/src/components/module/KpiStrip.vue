<template>
  <div class="module-kpi-strip">
    <button
      v-for="card in cards"
      :key="card.key"
      class="module-kpi-card"
      :class="[card.cls, { 'is-active': modelValue === card.key }]"
      type="button"
      @click="selectCard(card)"
    >
      <span class="module-kpi-card__num">{{ formatCardValue(card) }}</span>
      <span class="module-kpi-card__label">{{ card.label }}</span>
    </button>
    <div v-if="$slots.actions" class="module-kpi-strip__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  cards: { type: Array, required: true },
  metrics: { type: Object, default: () => ({}) },
  modelValue: { type: String, default: '' },
  formatter: { type: Function, default: null }
})

const emit = defineEmits(['update:modelValue', 'select'])

function formatCardValue(card) {
  const value = props.metrics?.[card.kpiKey] ?? 0
  return props.formatter ? props.formatter(value, card) : value
}

function selectCard(card) {
  emit('update:modelValue', card.key)
  emit('select', card)
}
</script>

<style scoped>
.module-kpi-strip {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.module-kpi-card {
  min-width: 76px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 16px;
  border: 1px solid #e4e7ed;
  border-top: 3px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  user-select: none;
  transition: box-shadow .15s, outline-color .15s;
  font: inherit;
}

.module-kpi-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, .1);
}

.module-kpi-card.is-active {
  outline: 2px solid #409eff;
}

.module-kpi-card.pending,
.module-kpi-card.draft,
.module-kpi-card.closed,
.module-kpi-card.semi {
  border-top-color: #909399;
}

.module-kpi-card.open,
.module-kpi-card.failed {
  border-top-color: #f56c6c;
}

.module-kpi-card.processing,
.module-kpi-card.recheck,
.module-kpi-card.amount,
.module-kpi-card.quality {
  border-top-color: #e6a23c;
}

.module-kpi-card.confirmed,
.module-kpi-card.resolved,
.module-kpi-card.finished {
  border-top-color: #409eff;
}

.module-kpi-card.exported,
.module-kpi-card.passed {
  border-top-color: #67c23a;
}

.module-kpi-card.amount,
.module-kpi-card.quality {
  min-width: 110px;
}

.module-kpi-card__num {
  font-size: 22px;
  font-weight: 700;
  color: #2c3e50;
  line-height: 1.2;
}

.module-kpi-card__label {
  margin-top: 2px;
  font-size: 11px;
  color: #8492a6;
}

.module-kpi-strip__actions {
  margin-left: auto;
  display: flex;
  align-items: center;
}
</style>
