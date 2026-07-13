<template>
  <div class="thought-shell">
    <div class="thought-shell__hero">
      <div class="thought-shell__hero-left">
        <h3>{{ title }}</h3>
        <p>{{ subtitle }}</p>
      </div>
      <div class="thought-shell__hero-right">
        <div class="progress-ring">
          <span class="progress-ring__num">{{ progressPercent }}%</span>
          <span class="progress-ring__label">{{ running ? '推演中' : thoughtStream.length ? '已完成' : '待开始' }}</span>
        </div>
        <div class="progress-meta">
          <span>步骤 {{ Math.min(activeIndex, totalSteps) }}/{{ totalSteps }}</span>
          <el-progress :percentage="progressPercent" :stroke-width="8" :show-text="false" />
        </div>
      </div>
    </div>

    <div class="thought-layout">
      <div class="thought-stream">
        <div class="thought-stream__header">
          <span class="thought-stream__title">
            <span class="pulse-dot" :class="{ 'pulse-dot--on': running }" />
            实时协作思维流
          </span>
          <span class="thought-stream__hint">点击步骤查看详细推演</span>
        </div>

        <div v-if="!thoughtStream.length && !running" class="thought-empty">
          <div class="thought-empty__icon">◎</div>
          <div>点击「重新分析」后，Agent 将按步骤展示订单、库存、设备、人员的推演路径</div>
        </div>

        <div class="thought-list">
          <div
            v-for="(item, idx) in thoughtStream"
            :key="item.key || idx"
            class="thought-item"
            :class="{
              'thought-item--active': idx === thoughtStream.length - 1 && running,
              'thought-item--done': !running || idx < thoughtStream.length - 1,
              'thought-item--selected': expandedKey === item.key,
              'thought-item--linked': selectedStepKey === item.key
            }"
          >
            <div class="thought-item__rail">
              <div class="thought-item__avatar" :style="{ background: avatarColor(idx) }">
                {{ avatarText(item.agentName) }}
              </div>
              <div v-if="idx < thoughtStream.length - 1" class="thought-item__line" />
            </div>
            <div
              class="thought-item__card"
              role="button"
              tabindex="0"
              @click="toggleExpand(item.key)"
              @keydown.enter="toggleExpand(item.key)"
            >
              <div class="thought-item__meta">
                <span class="thought-badge" :class="`thought-badge--${badgeType(item.actionType)}`">
                  {{ item.badge || item.actionType || '执行' }}
                </span>
                <span class="thought-item__agent">{{ item.agentName }}</span>
                <span v-if="item.evidenceCount" class="thought-item__ev-count">证据 {{ item.evidenceCount }}</span>
                <span class="thought-item__step">#{{ idx + 1 }}</span>
              </div>

              <div class="thought-item__summary">
                {{ item.summary || item.thought || item.detail }}
              </div>

              <div v-if="expandedKey === item.key" class="thought-item__expand">
                <div class="thought-item__expand-title">执行动作</div>
                <div class="thought-item__expand-action">{{ item.action || item.title }}</div>

                <div class="thought-item__expand-title">详细推演</div>
                <div class="thought-item__text">{{ item.thought || item.detail }}</div>

                <ul v-if="item.detailLines?.length" class="thought-item__lines">
                  <li v-for="(line, li) in item.detailLines" :key="li">{{ line }}</li>
                </ul>

                <div v-if="stepEvidence(item.key).length" class="thought-item__linked-ev">
                  <span>关联证据 {{ stepEvidence(item.key).length }} 条</span>
                  <el-button link type="primary" size="small" @click.stop="focusEvidence(item.key)">
                    在证据库中查看
                  </el-button>
                </div>
              </div>

              <div v-else class="thought-item__fold-hint">
                点击查看详细内容
                <el-icon><ArrowDown /></el-icon>
              </div>
            </div>
          </div>

          <div v-if="running" class="thought-item thought-item--pending">
            <div class="thought-item__rail">
              <div class="thought-item__avatar thought-item__avatar--loading">
                <el-icon class="is-loading"><Loading /></el-icon>
              </div>
            </div>
            <div class="thought-item__card thought-item__card--pending">
              <div class="thought-item__text">{{ pendingText }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="evidence-panel">
        <div class="evidence-panel__header">
          <span>证据库</span>
          <em>({{ visibleEvidence.length }}{{ allEvidenceCount ? ` / ${allEvidenceCount}` : '' }})</em>
        </div>
        <div v-if="selectedStepKey" class="evidence-filter">
          当前筛选：<strong>{{ stepLabel(selectedStepKey) }}</strong>
          <el-button link type="primary" size="small" @click="clearFilter">显示全部</el-button>
        </div>
        <div class="evidence-list" ref="evidenceListRef">
          <div
            v-for="(ev, idx) in visibleEvidence"
            :key="ev.id || idx"
            class="evidence-card"
            :class="{
              'evidence-card--new': idx === visibleEvidence.length - 1 && running,
              'evidence-card--highlight': highlightedEvidenceId === ev.id
            }"
            @click="toggleEvidence(ev.id)"
          >
            <div class="evidence-card__top">
              <span class="evidence-tag">{{ ev.tag || ev.source }}</span>
              <span class="evidence-code">#{{ idx + 1 }}</span>
              <span class="evidence-reliability">可信度 {{ ev.reliability ?? 85 }}%</span>
            </div>
            <div class="evidence-card__title">{{ ev.title }}</div>
            <div class="evidence-card__snippet">{{ ev.snippet }}</div>
            <div v-if="metricEntries(ev).length" class="evidence-metrics">
              <div v-for="(m, mi) in metricEntries(ev)" :key="mi" class="evidence-metric">
                <span class="evidence-metric__key">{{ m.key }}</span>
                <span class="evidence-metric__val">{{ m.value }}</span>
              </div>
            </div>
            <div v-if="expandedEvidenceId === ev.id && ev.detail" class="evidence-card__detail">
              {{ ev.detail }}
            </div>
            <div class="evidence-card__source">{{ ev.source }} · {{ ev.code }}</div>
          </div>
          <div v-if="!visibleEvidence.length" class="evidence-empty">
            {{ running ? '推演中，正在抓取数据源…' : '等待规划路径触发数据源…' }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { ArrowDown, Loading } from '@element-plus/icons-vue'

const props = defineProps({
  title: { type: String, default: '智能排产引擎' },
  subtitle: { type: String, default: '订单 → 库存 → 物料 → 设备 → 人员 → 车间分配 → 生产计划' },
  thoughtStream: { type: Array, default: () => [] },
  evidenceList: { type: Array, default: () => [] },
  allEvidence: { type: Array, default: () => [] },
  activeStepKey: { type: String, default: '' },
  selectedStepKey: { type: String, default: '' },
  activeIndex: { type: Number, default: 0 },
  totalSteps: { type: Number, default: 7 },
  running: { type: Boolean, default: false },
  pendingText: { type: String, default: '正在推演下一环节…' }
})

const emit = defineEmits(['select-step'])

const expandedKey = ref('')
const expandedEvidenceId = ref('')
const highlightedEvidenceId = ref('')
const evidenceListRef = ref(null)

const AVATAR_COLORS = ['#4f8cff', '#6c5ce7', '#00b894', '#e17055', '#0984e3', '#fdcb6e', '#e84393']

const allEvidenceCount = computed(() => {
  const all = props.allEvidence?.length ? props.allEvidence : props.evidenceList
  return all.length
})

const progressPercent = computed(() => {
  if (!props.totalSteps) return 0
  if (props.running) {
    return Math.min(95, Math.round((props.activeIndex / props.totalSteps) * 100))
  }
  return props.thoughtStream.length ? 100 : 0
})

const filterKey = computed(() => props.selectedStepKey || '')

const visibleEvidence = computed(() => {
  const list = props.allEvidence?.length ? props.allEvidence : props.evidenceList
  if (!list.length) return []
  if (filterKey.value) {
    const filtered = list.filter((ev) => (ev.relatedSteps || []).includes(filterKey.value))
    return filtered.length ? filtered : list
  }
  if (!props.running) return list
  const revealedKeys = props.thoughtStream.map((t) => t.key).filter(Boolean)
  if (!revealedKeys.length) return []
  const cumulative = list.filter((ev) => {
    const related = ev.relatedSteps || []
    return related.some((k) => revealedKeys.includes(k))
  })
  return cumulative.length ? cumulative : list.slice(0, Math.max(1, revealedKeys.length))
})

watch(() => props.activeStepKey, (key) => {
  if (props.running && key) {
    expandedKey.value = key
    emit('select-step', key)
  }
})

watch(() => props.thoughtStream.length, (len, prev) => {
  if (len > prev && props.running) {
    const last = props.thoughtStream[len - 1]
    if (last?.key) expandedKey.value = last.key
  }
  if (!props.running && len && !expandedKey.value) {
    expandedKey.value = props.thoughtStream[len - 1]?.key || ''
  }
})

function stepEvidence(stepKey) {
  const list = props.allEvidence?.length ? props.allEvidence : props.evidenceList
  return list.filter((ev) => (ev.relatedSteps || []).includes(stepKey))
}

function stepLabel(key) {
  const step = props.thoughtStream.find((t) => t.key === key)
  return step ? `${step.agentName} · ${step.action || step.title}` : key
}

function toggleExpand(key) {
  expandedKey.value = expandedKey.value === key ? '' : key
  emit('select-step', key)
}

function focusEvidence(stepKey) {
  emit('select-step', stepKey)
  nextTick(() => {
    const first = stepEvidence(stepKey)[0]
    if (first) {
      highlightedEvidenceId.value = first.id
      expandedEvidenceId.value = first.id
    }
  })
}

function clearFilter() {
  emit('select-step', '')
  highlightedEvidenceId.value = ''
}

function toggleEvidence(id) {
  expandedEvidenceId.value = expandedEvidenceId.value === id ? '' : id
  highlightedEvidenceId.value = id
}

function avatarText(name) {
  if (!name) return 'AI'
  return name.replace(/员$/, '').slice(-2) || name.slice(0, 2)
}

function avatarColor(idx) {
  return AVATAR_COLORS[idx % AVATAR_COLORS.length]
}

function badgeType(actionType) {
  const t = String(actionType || '')
  if (t.includes('派遣')) return 'dispatch'
  if (t.includes('发现')) return 'discover'
  return 'execute'
}

function metricEntries(ev) {
  const metrics = ev?.metrics
  if (!metrics || typeof metrics !== 'object') return []
  return Object.entries(metrics).map(([key, value]) => ({
    key,
    value: value == null ? '—' : String(value)
  }))
}
</script>

<style scoped>
.thought-shell {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #dbe4f3;
  background: #fff;
  box-shadow: 0 8px 28px rgba(26, 43, 74, 0.08);
}
.thought-shell__hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #0f2744 0%, #1a4a8a 55%, #2d6cdf 100%);
  color: #fff;
}
.thought-shell__hero h3 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 700;
}
.thought-shell__hero p {
  margin: 0;
  font-size: 12px;
  opacity: 0.88;
}
.thought-shell__hero-right {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 180px;
}
.progress-ring {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: 4px solid rgba(255,255,255,0.25);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.08);
}
.progress-ring__num { font-size: 15px; font-weight: 700; }
.progress-ring__label { font-size: 10px; opacity: 0.85; }
.progress-meta { flex: 1; font-size: 11px; }
.progress-meta :deep(.el-progress-bar__outer) { background: rgba(255,255,255,0.2); }
.progress-meta :deep(.el-progress-bar__inner) { background: linear-gradient(90deg, #7ee8fa, #eec0ff); }
.thought-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 0;
  min-height: 360px;
}
.thought-stream { border-right: 1px solid #eef2f7; }
.thought-stream__header,
.evidence-panel__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  border-bottom: 1px solid #eef2f7;
  background: #f8fafc;
  font-size: 14px;
  font-weight: 600;
  color: #1a2b4a;
}
.thought-stream__hint {
  margin-left: auto;
  font-size: 11px;
  font-weight: 400;
  color: #909399;
}
.pulse-dot {
  display: inline-block;
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
  margin-right: 6px;
}
.pulse-dot--on {
  background: #67c23a;
  animation: pulse 1.5s infinite;
}
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.5); }
  70% { box-shadow: 0 0 0 10px rgba(103, 194, 58, 0); }
  100% { box-shadow: 0 0 0 0 rgba(103, 194, 58, 0); }
}
.evidence-panel__header em {
  font-style: normal;
  color: #909399;
  font-weight: 400;
}
.evidence-filter {
  padding: 8px 12px;
  font-size: 12px;
  color: #606266;
  border-bottom: 1px dashed #eef2f7;
  background: #fffdf6;
}
.thought-empty, .evidence-empty {
  padding: 48px 20px;
  text-align: center;
  color: #909399;
  font-size: 13px;
  line-height: 1.7;
}
.thought-empty__icon { font-size: 28px; margin-bottom: 8px; opacity: 0.5; }
.thought-list {
  padding: 12px;
  max-height: 400px;
  overflow-y: auto;
}
.thought-item {
  display: flex;
  gap: 14px;
  margin-bottom: 6px;
}
.thought-item__rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 44px;
  flex-shrink: 0;
}
.thought-item__avatar {
  width: 40px; height: 40px;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.12);
}
.thought-item__avatar--loading {
  background: #f0f2f5 !important;
  color: #909399;
  box-shadow: none;
}
.thought-item__line {
  flex: 1;
  width: 3px;
  min-height: 24px;
  background: linear-gradient(180deg, #7aa7f5, #e8edf5);
  margin: 6px 0;
  border-radius: 2px;
}
.thought-item__card {
  flex: 1;
  padding: 12px 14px;
  margin-bottom: 10px;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: all 0.22s ease;
}
.thought-item__card:hover {
  border-color: #b3d4ff;
  box-shadow: 0 2px 10px rgba(79, 140, 255, 0.1);
}
.thought-item--active .thought-item__card {
  border-color: #4f8cff;
  box-shadow: 0 4px 16px rgba(79, 140, 255, 0.18);
}
.thought-item--selected .thought-item__card,
.thought-item--linked .thought-item__card {
  border-color: #4f8cff;
  background: #f8fbff;
}
.thought-item__card--pending {
  border-style: dashed;
  background: #fffdf6;
  cursor: default;
}
.thought-item__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.thought-badge {
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 12px;
  font-weight: 600;
}
.thought-badge--dispatch { background: #e8f4ff; color: #1677ff; }
.thought-badge--execute { background: #fff7e6; color: #d48806; }
.thought-badge--discover { background: #f0fff4; color: #389e0d; }
.thought-item__agent {
  font-size: 14px;
  font-weight: 700;
  color: #1a2b4a;
}
.thought-item__ev-count {
  font-size: 11px;
  color: #67c23a;
  background: #f0fff4;
  padding: 1px 8px;
  border-radius: 10px;
}
.thought-item__step {
  margin-left: auto;
  font-size: 11px;
  color: #909399;
}
.thought-item__summary {
  font-size: 13px;
  color: #303133;
  line-height: 1.7;
}
.thought-item__fold-hint {
  margin-top: 8px;
  font-size: 11px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}
.thought-item__expand {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e8edf5;
}
.thought-item__expand-title {
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
}
.thought-item__expand-action {
  font-size: 13px;
  font-weight: 600;
  color: #1a2b4a;
  margin-bottom: 10px;
}
.thought-item__text {
  font-size: 13px;
  color: #303133;
  line-height: 1.7;
  margin-bottom: 8px;
}
.thought-item__lines {
  margin: 0 0 10px;
  padding-left: 18px;
  font-size: 12px;
  color: #606266;
  line-height: 1.8;
}
.thought-item__linked-ev {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #606266;
  padding: 8px 10px;
  background: #f6f8fb;
  border-radius: 8px;
}
.evidence-list {
  padding: 10px;
  max-height: 400px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #fbfcfe;
}
.evidence-card {
  padding: 12px 14px;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: box-shadow 0.22s ease, border-color 0.22s ease;
}
.evidence-card:hover { border-color: #b3d4ff; }
.evidence-card--new { box-shadow: 0 0 0 2px rgba(79, 140, 255, 0.15); }
.evidence-card--highlight {
  border-color: #4f8cff;
  box-shadow: 0 0 0 2px rgba(79, 140, 255, 0.2);
}
.evidence-card__top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 11px;
}
.evidence-tag {
  background: #e8f4ff;
  color: #1677ff;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
}
.evidence-code { color: #909399; }
.evidence-reliability {
  margin-left: auto;
  color: #67c23a;
  font-weight: 600;
}
.evidence-card__title {
  font-size: 13px;
  font-weight: 700;
  color: #1a2b4a;
  margin-bottom: 4px;
}
.evidence-card__snippet {
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 6px;
}
.evidence-metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 10px;
  margin-bottom: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f6f8fb;
}
.evidence-metric {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.evidence-metric__key { font-size: 10px; color: #909399; }
.evidence-metric__val {
  font-size: 12px;
  font-weight: 600;
  color: #303133;
  word-break: break-all;
}
.evidence-card__detail {
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 6px;
  padding: 8px;
  background: #fafcff;
  border-radius: 6px;
}
.evidence-card__source { font-size: 11px; color: #909399; }
</style>
