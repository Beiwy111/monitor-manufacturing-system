<template>
  <el-dialog
    v-model="visible"
    width="min(1120px, 94vw)"
    top="4vh"
    append-to-body
    destroy-on-close
    :close-on-click-modal="!loading"
    class="finance-ai-dialog"
  >
    <template #header>
      <div class="finance-ai-header">
        <div class="finance-ai-header__mark">
          <el-icon><MagicStick /></el-icon>
        </div>
        <div>
          <h2>{{ dialogTitle }}</h2>
          <p>{{ dialogSubtitle }}</p>
        </div>
      </div>
    </template>

    <div class="finance-ai-shell">
      <el-steps :active="activeStep" finish-status="success" simple class="finance-ai-steps">
        <el-step title="数据采集" />
        <el-step title="Agent 分析" />
        <el-step title="结论建议" />
      </el-steps>

      <section v-if="loading" class="analysis-running">
        <div class="running-hero">
          <div class="running-hero__ring">
            <el-progress type="circle" :percentage="progress" :width="118" :stroke-width="9" color="#00beff" />
          </div>
          <div class="running-hero__copy">
            <span class="running-hero__eyebrow">{{ agentEyebrow }}</span>
            <h3>{{ runningTitle }}</h3>
            <p>{{ runningDetail }}</p>
            <div class="running-hero__bar"><span :style="{ width: `${progress}%` }" /></div>
          </div>
        </div>

        <div class="agent-run-grid">
          <article
            v-for="(agent, index) in agents"
            :key="agent.key"
            class="agent-run-card"
            :class="`is-${agentRunState(index)}`"
          >
            <span class="agent-run-card__index">{{ String(index + 1).padStart(2, '0') }}</span>
            <div>
              <strong>{{ agent.name }}</strong>
              <p>{{ agent.task }}</p>
            </div>
            <span class="agent-run-card__state">{{ agentRunLabel(index) }}</span>
          </article>
        </div>

        <div class="running-note">
          <span class="running-note__dot" />
          {{ runningNote }}
        </div>
      </section>

      <el-result
        v-else-if="errorMessage"
        icon="error"
        :title="`${dialogTitle}生成失败`"
        :sub-title="errorMessage"
        class="analysis-error"
      >
        <template #extra>
          <el-button type="primary" @click="startAnalysis">重新分析</el-button>
        </template>
      </el-result>

      <section v-else-if="result" class="analysis-result">
        <div class="result-meta">
          <span>{{ result.period?.startDate }} 至 {{ result.period?.endDate }}</span>
          <span>{{ result.model }}</span>
          <span>生成于 {{ result.generatedAt }}</span>
        </div>

        <div class="result-overview">
          <el-tooltip :content="result.ratingDescription" placement="top">
            <div class="health-rating" :class="`is-${String(result.rating || 'C').toLowerCase()}`">
              <strong>{{ result.rating || 'C' }}</strong>
              <span>{{ result.ratingName || '关注' }}</span>
              <small>{{ ratingLabel }}</small>
            </div>
          </el-tooltip>
          <div class="overview-copy">
            <div class="overview-copy__head">
              <span>总体结论</span>
              <el-tag :type="riskTagType(result.riskLevel)" effect="dark" round>
                {{ riskLabel(result.riskLevel) }}
              </el-tag>
            </div>
            <p>{{ result.summary }}</p>
          </div>
        </div>

        <div v-if="result.highlights?.length" class="highlight-grid">
          <article v-for="(item, index) in result.highlights" :key="index" class="highlight-card">
            <span>{{ String(index + 1).padStart(2, '0') }}</span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.evidence }}</p>
              <small>{{ item.impact }}</small>
            </div>
          </article>
        </div>

        <section class="report-section" :class="{ 'is-collapsed': collapsedSections.evidence }">
          <header class="report-section__header">
            <div>
              <span class="report-section__eyebrow">EVIDENCE LIBRARY</span>
              <h3>分析证据库</h3>
            </div>
            <div class="report-section__aside">
              <span>{{ evidenceCaption }}</span>
              <button type="button" class="section-toggle" @click="toggleSection('evidence')">
                {{ collapsedSections.evidence ? '展开' : '收起' }}
                <el-icon :class="{ 'is-open': !collapsedSections.evidence }"><ArrowDown /></el-icon>
              </button>
            </div>
          </header>
          <div v-show="!collapsedSections.evidence" class="evidence-grid">
            <article v-for="card in result.evidenceCards || []" :key="card.title" class="evidence-card">
              <header>
                <strong>{{ card.title }}</strong>
                <span>{{ card.source }}</span>
              </header>
              <div class="evidence-card__metrics">
                <div v-for="metric in card.metrics || []" :key="metric.label">
                  <span>{{ metric.label }}</span>
                  <strong>{{ formatMetric(metric.value, metric.unit) }}</strong>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section
          v-if="result.crossModuleInsights?.length"
          class="report-section"
          :class="{ 'is-collapsed': collapsedSections.crossModule }"
        >
          <header class="report-section__header">
            <div>
              <span class="report-section__eyebrow">CROSS-MODULE INSIGHTS</span>
              <h3>跨模块联动分析</h3>
            </div>
            <div class="report-section__aside">
              <span>{{ result.crossModuleInsights.length }} 条影响链</span>
              <button type="button" class="section-toggle" @click="toggleSection('crossModule')">
                {{ collapsedSections.crossModule ? '展开' : '收起' }}
                <el-icon :class="{ 'is-open': !collapsedSections.crossModule }"><ArrowDown /></el-icon>
              </button>
            </div>
          </header>
          <div v-show="!collapsedSections.crossModule" class="insight-list">
            <article v-for="(item, index) in result.crossModuleInsights" :key="index" class="insight-card">
              <span class="insight-card__index">{{ String(index + 1).padStart(2, '0') }}</span>
              <div>
                <strong>{{ item.title }}</strong>
                <p class="insight-card__chain">{{ item.chain }}</p>
                <p><b>证据</b>{{ item.evidence }}</p>
                <p><b>建议</b>{{ item.recommendation }}</p>
              </div>
            </article>
          </div>
        </section>

        <div class="result-columns">
          <section class="report-section agent-section" :class="{ 'is-collapsed': collapsedSections.agents }">
            <header class="report-section__header">
              <div>
                <span class="report-section__eyebrow">AGENT ANALYSIS</span>
                <h3>专业分析员结论</h3>
              </div>
              <button type="button" class="section-toggle" @click="toggleSection('agents')">
                {{ collapsedSections.agents ? '展开' : '收起' }}
                <el-icon :class="{ 'is-open': !collapsedSections.agents }"><ArrowDown /></el-icon>
              </button>
            </header>
            <div v-show="!collapsedSections.agents" class="agent-result-list">
              <article
                v-for="agent in result.agents || []"
                :key="agent.key"
                class="agent-result-card"
                :class="{ 'is-open': expandedAgent === agent.key }"
              >
                <button type="button" class="agent-result-card__head" @click="toggleAgent(agent.key)">
                  <span class="agent-result-card__avatar">{{ agent.name?.charAt(0) }}</span>
                  <span class="agent-result-card__title">
                    <strong>{{ agent.name }}</strong>
                    <small>{{ agent.summary }}</small>
                  </span>
                  <el-tag :type="agentTagType(agent.status)" size="small" effect="plain">
                    {{ agentStatusLabel(agent.status) }}
                  </el-tag>
                  <span class="agent-result-card__arrow">⌄</span>
                </button>
                <div v-if="expandedAgent === agent.key" class="agent-result-card__body">
                  <ul>
                    <li v-for="(finding, index) in agent.findings || []" :key="index">{{ finding }}</li>
                  </ul>
                </div>
              </article>
            </div>
          </section>

          <section class="report-section" :class="{ 'is-collapsed': collapsedSections.risks }">
            <header class="report-section__header">
              <div>
                <span class="report-section__eyebrow">RISK CONTROL</span>
                <h3>风险清单</h3>
              </div>
              <div class="report-section__aside">
                <span>{{ result.risks?.length || 0 }} 项</span>
                <button type="button" class="section-toggle" @click="toggleSection('risks')">
                  {{ collapsedSections.risks ? '展开' : '收起' }}
                  <el-icon :class="{ 'is-open': !collapsedSections.risks }"><ArrowDown /></el-icon>
                </button>
              </div>
            </header>
            <div v-show="!collapsedSections.risks">
              <div v-if="result.risks?.length" class="risk-list">
                <article v-for="(risk, index) in result.risks" :key="index" :class="`risk-card is-${risk.level?.toLowerCase()}`">
                  <div class="risk-card__head">
                    <span>{{ riskLabel(risk.level) }}</span>
                    <strong>{{ risk.title }}</strong>
                  </div>
                  <p><b>证据</b>{{ risk.evidence }}</p>
                  <p><b>建议</b>{{ risk.suggestion }}</p>
                </article>
              </div>
              <el-empty v-else description="当前数据未识别到明确风险" :image-size="72" />
            </div>
          </section>
        </div>

        <section class="report-section">
          <header class="report-section__header">
            <div>
              <span class="report-section__eyebrow">ACTION PLAN</span>
              <h3>建议行动</h3>
            </div>
          </header>
          <div class="action-list">
            <article v-for="(action, index) in result.actions || []" :key="index" class="action-card">
              <span class="action-card__priority">{{ action.priority }}</span>
              <div>
                <strong>{{ action.action }}</strong>
                <p>{{ action.basis }}</p>
              </div>
              <div class="action-card__aside">
                <span class="action-card__department">{{ action.department }}</span>
                <el-button
                  v-if="isGlobal"
                  size="small"
                  type="primary"
                  plain
                  :loading="notifyingAction === index"
                  :disabled="notifiedActions.includes(index)"
                  @click="sendActionNotification(action, index)"
                >
                  <el-icon><Bell /></el-icon>
                  {{ notifiedActions.includes(index) ? '已通知' : '通知' }}
                </el-button>
              </div>
            </article>
          </div>
        </section>

        <p class="result-disclaimer">{{ result.disclaimer }}</p>
      </section>
    </div>

    <template #footer>
      <div class="finance-ai-footer">
        <span v-if="loading">分析通常需要 10～60 秒，请勿重复提交</span>
        <span v-else />
        <div>
          <el-button @click="visible = false">关闭</el-button>
          <el-button v-if="result" @click="copyReport">
            <el-icon><CopyDocument /></el-icon>
            复制报告
          </el-button>
          <el-button v-if="!loading" type="primary" @click="startAnalysis">
            <el-icon><RefreshRight /></el-icon>
            重新分析
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ArrowDown, Bell, CopyDocument, MagicStick, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { generateFinanceAiAnalysis, generateGlobalAiAnalysis, notifyGlobalAiAction } from '@/api/workbench'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  days: { type: Number, default: 7 },
  analysisType: {
    type: String,
    default: 'finance',
    validator: (value) => ['finance', 'global'].includes(value)
  }
})

const emit = defineEmits(['update:modelValue', 'loading-change'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const loading = ref(false)
const progress = ref(0)
const activeAgentIndex = ref(-1)
const result = ref(null)
const errorMessage = ref('')
const expandedAgent = ref('')
const collapsedSections = ref({ evidence: true, crossModule: true, agents: true, risks: true })
const notifyingAction = ref(-1)
const notifiedActions = ref([])
let progressTimer = null
let requestSequence = 0

const financeAgents = [
  { key: 'revenue', name: '收入分析员', task: '核对收入规模与周期趋势' },
  { key: 'cost', name: '成本分析员', task: '拆解材料、人工与设备成本' },
  { key: 'collection', name: '回款分析员', task: '识别应收、逾期和客户风险' },
  { key: 'profit', name: '盈利分析员', task: '定位低毛利与亏损订单' },
  { key: 'risk', name: '风险控制员', task: '汇总风险并形成行动建议' }
]

const globalAgents = [
  { key: 'system', name: '系统安全分析员', task: '检查用户、权限与系统操作态势' },
  { key: 'order', name: '订单履约分析员', task: '分析订单状态、交付与履约风险' },
  { key: 'production', name: '生产运营分析员', task: '联动计划、工单、派工和报工数据' },
  { key: 'quality', name: '质量分析员', task: '分析检验、不良与质量风险' },
  { key: 'supply', name: '供应链分析员', task: '检查采购、库存与发货协同' },
  { key: 'equipment', name: '设备分析员', task: '评估设备、报警和维保状态' },
  { key: 'aftersales', name: '售后分析员', task: '识别售后案例与闭环风险' },
  { key: 'finance', name: '财务分析员', task: '分析结算与成本结构' }
]

const isGlobal = computed(() => props.analysisType === 'global')
const agents = computed(() => isGlobal.value ? globalAgents : financeAgents)
const dialogTitle = computed(() => isGlobal.value ? 'AI 全局分析' : 'AI 财务分析')
const dialogSubtitle = computed(() => isGlobal.value
  ? '系统 → 订单 → 生产 → 质量 → 供应链 → 设备 → 售后 → 财务'
  : '收入 → 成本 → 回款 → 盈利 → 风险 → 行动建议')
const agentEyebrow = computed(() => isGlobal.value ? 'DEEPSEEK MES GLOBAL AGENT' : 'DEEPSEEK FINANCE AGENT')
const ratingLabel = computed(() => isGlobal.value ? '运营评级' : '财务评级')
const evidenceCaption = computed(() => isGlobal.value
  ? '数据来自当前 MES 各业务模块聚合结果'
  : '数据来自当前 MES 财务聚合结果')
const runningNote = computed(() => isGlobal.value
  ? '正在读取 MES 跨模块聚合数据。AI 仅形成分析建议，不会修改任何业务数据或代替审批。'
  : '正在读取 MES 实时聚合数据。AI 仅形成分析建议，不会修改订单、成本或结算数据。')

const activeStep = computed(() => {
  if (result.value) return 3
  if (progress.value >= 24) return 1
  return 0
})

const runningTitle = computed(() => {
  if (progress.value < 18) return `正在建立${isGlobal.value ? '全局经营' : '财务'}证据库`
  return activeAgentIndex.value >= 0
    ? `${agents.value[activeAgentIndex.value]?.name || 'AI Agent'}正在分析`
    : `正在启动${isGlobal.value ? '全局' : '财务'}分析引擎`
})

const runningDetail = computed(() => {
  if (progress.value < 18) {
    return isGlobal.value
      ? `读取近 ${props.days} 天各模块活动与当前业务状态快照`
      : `读取近 ${props.days} 天收入、成本、利润、应收和结算数据`
  }
  return agents.value[activeAgentIndex.value]?.task || `正在校验${isGlobal.value ? '跨模块' : '财务'}指标与业务记录`
})

watch(() => props.modelValue, (opened) => {
  if (opened) startAnalysis()
})

async function startAnalysis() {
  const sequence = ++requestSequence
  clearProgressTimer()
  loading.value = true
  emit('loading-change', true)
  result.value = null
  errorMessage.value = ''
  expandedAgent.value = ''
  collapsedSections.value = { evidence: true, crossModule: true, agents: true, risks: true }
  notifyingAction.value = -1
  notifiedActions.value = []
  progress.value = 8
  activeAgentIndex.value = -1

  progressTimer = window.setInterval(() => {
    progress.value = Math.min(92, progress.value + (progress.value < 35 ? 5 : 3))
    const step = isGlobal.value ? 10 : 16
    activeAgentIndex.value = Math.min(agents.value.length - 1, Math.max(0, Math.floor((progress.value - 18) / step)))
  }, 650)

  try {
    const data = isGlobal.value
      ? await generateGlobalAiAnalysis(props.days)
      : await generateFinanceAiAnalysis(props.days)
    if (sequence !== requestSequence) return
    progress.value = 100
    activeAgentIndex.value = agents.value.length - 1
    result.value = data
  } catch (error) {
    if (sequence !== requestSequence) return
    errorMessage.value = error?.message || `${dialogTitle.value}服务暂不可用，请稍后重试`
  } finally {
    if (sequence === requestSequence) {
      clearProgressTimer()
      loading.value = false
      emit('loading-change', false)
    }
  }
}

function clearProgressTimer() {
  if (progressTimer) window.clearInterval(progressTimer)
  progressTimer = null
}

function agentRunState(index) {
  if (index < activeAgentIndex.value) return 'done'
  if (index === activeAgentIndex.value) return 'active'
  return 'pending'
}

function agentRunLabel(index) {
  const state = agentRunState(index)
  return state === 'done' ? '完成' : state === 'active' ? '分析中' : '等待'
}

function toggleAgent(key) {
  expandedAgent.value = expandedAgent.value === key ? '' : key
}

function toggleSection(key) {
  collapsedSections.value[key] = !collapsedSections.value[key]
}

function riskLabel(level) {
  return ({ LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' })[String(level || '').toUpperCase()] || '待评估'
}

function riskTagType(level) {
  return ({ LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' })[String(level || '').toUpperCase()] || 'info'
}

function agentTagType(status) {
  return ({ NORMAL: 'success', WARNING: 'warning', RISK: 'danger' })[String(status || '').toUpperCase()] || 'info'
}

function agentStatusLabel(status) {
  return ({ NORMAL: '正常', WARNING: '关注', RISK: '风险' })[String(status || '').toUpperCase()] || '待评估'
}

function formatMetric(value, unit) {
  const number = Number(value || 0)
  const digits = unit === '%' ? 2 : 0
  return `${Number.isFinite(number) ? number.toLocaleString('zh-CN', { maximumFractionDigits: digits }) : value || 0}${unit || ''}`
}

async function sendActionNotification(action, index) {
  if (!isGlobal.value || notifyingAction.value >= 0 || notifiedActions.value.includes(index)) return
  notifyingAction.value = index
  try {
    const response = await notifyGlobalAiAction({
      priority: action.priority,
      department: action.department,
      action: action.action,
      basis: action.basis,
      reportGeneratedAt: result.value?.generatedAt,
      actionIndex: index
    })
    notifiedActions.value.push(index)
    ElMessage.success(`已通知${response?.targetDepartment || action.department || '对应部门'}`)
  } catch (error) {
    ElMessage.error(error?.message || '发送通知失败，请稍后重试')
  } finally {
    notifyingAction.value = -1
  }
}

async function copyReport() {
  if (!result.value) return
  const lines = [
    result.value.title,
    `周期：${result.value.period?.startDate || ''} 至 ${result.value.period?.endDate || ''}`,
    `${ratingLabel.value}：${result.value.rating || 'C'}（${result.value.ratingName || '关注'}），${riskLabel(result.value.riskLevel)}`,
    '',
    result.value.summary,
    '',
    '风险：',
    ...(result.value.risks || []).map((item, index) => `${index + 1}. ${item.title}；证据：${item.evidence}；建议：${item.suggestion}`),
    '',
    '行动建议：',
    ...(result.value.actions || []).map((item, index) => `${index + 1}. [${item.priority}] ${item.action}（${item.department}）`),
    '',
    result.value.disclaimer
  ]
  try {
    await navigator.clipboard.writeText(lines.join('\n'))
    ElMessage.success('分析报告已复制')
  } catch {
    ElMessage.warning('浏览器未授权剪贴板，请手动复制')
  }
}

onBeforeUnmount(() => {
  requestSequence++
  clearProgressTimer()
})
</script>

<style scoped>
.finance-ai-header {
  display: flex;
  align-items: center;
  gap: 13px;
}

.finance-ai-header__mark {
  width: 42px;
  height: 42px;
  border-radius: 13px;
  display: grid;
  place-items: center;
  color: #073848;
  font-size: 22px;
  background: linear-gradient(135deg, #b6f2ff, #00beff);
  box-shadow: 0 6px 18px rgba(0, 190, 255, 0.22);
}

.finance-ai-header h2 {
  margin: 0;
  font-size: 20px;
  color: #24343a;
}

.finance-ai-header p {
  margin: 4px 0 0;
  color: #7a898f;
  font-size: 12px;
}

.finance-ai-shell {
  min-height: 520px;
  max-height: 72vh;
  overflow-y: auto;
  padding-right: 4px;
  background: #eef5f1;
  border-radius: 16px;
}

.finance-ai-steps {
  position: sticky;
  top: 0;
  z-index: 2;
  border-radius: 14px 14px 0 0;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(8px);
}

.analysis-running,
.analysis-result {
  padding: 20px;
}

.running-hero {
  display: grid;
  grid-template-columns: 138px 1fr;
  align-items: center;
  gap: 26px;
  padding: 24px 28px;
  border-radius: 18px;
  color: #e8fbff;
  background:
    radial-gradient(circle at 90% 0%, rgba(0, 190, 255, 0.3), transparent 42%),
    linear-gradient(135deg, #18353e, #244a52);
}

.running-hero__ring :deep(.el-progress__text) { color: #e9fbff; }
.running-hero__eyebrow,
.report-section__eyebrow {
  font-size: 10px;
  letter-spacing: 0.14em;
  color: #00beff;
  font-weight: 700;
}

.running-hero h3 {
  margin: 7px 0 5px;
  font-size: 22px;
}

.running-hero p {
  margin: 0;
  color: rgba(232, 251, 255, 0.72);
}

.running-hero__bar {
  height: 5px;
  margin-top: 18px;
  border-radius: 999px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.12);
}

.running-hero__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #74e3ff, #00beff);
  transition: width 0.35s ease;
}

.agent-run-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.agent-run-card {
  position: relative;
  min-height: 128px;
  padding: 16px 14px 13px;
  border: 1px solid #dfe8e3;
  border-radius: 15px;
  background: #fff;
  transition: 0.2s ease;
}

.agent-run-card.is-active {
  border-color: #00beff;
  box-shadow: 0 8px 24px rgba(0, 190, 255, 0.14);
  transform: translateY(-3px);
}

.agent-run-card.is-done { border-color: #9ad4aa; background: #f7fcf8; }
.agent-run-card.is-pending { opacity: 0.58; }
.agent-run-card__index { font-size: 10px; color: #9aa8ad; }
.agent-run-card strong { display: block; margin-top: 12px; color: #2d4047; }
.agent-run-card p { margin: 6px 0 0; font-size: 11px; line-height: 1.55; color: #829096; }
.agent-run-card__state {
  position: absolute;
  right: 12px;
  top: 12px;
  font-size: 10px;
  color: #84939a;
}
.agent-run-card.is-active .agent-run-card__state { color: #008eba; }
.agent-run-card.is-done .agent-run-card__state { color: #4f9563; }

.running-note {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding: 11px 14px;
  font-size: 12px;
  color: #708087;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
}
.running-note__dot { width: 7px; height: 7px; border-radius: 50%; background: #00beff; box-shadow: 0 0 0 5px rgba(0, 190, 255, 0.1); }

.insight-list {
  display: grid;
  gap: 10px;
}

.insight-card {
  display: grid;
  grid-template-columns: 34px 1fr;
  gap: 12px;
  padding: 15px 16px;
  border: 1px solid #dce8e2;
  border-radius: 13px;
  background: #f9fcfa;
}

.insight-card__index {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 9px;
  color: #087b9d;
  font-size: 11px;
  font-weight: 700;
  background: #dff7ff;
}

.insight-card strong { color: #2d4047; }
.insight-card p { margin: 7px 0 0; color: #66777d; font-size: 12px; line-height: 1.6; }
.insight-card p b { margin-right: 8px; color: #385159; }
.insight-card__chain { color: #0788af !important; font-weight: 600; }

.analysis-error { min-height: 480px; background: #fff; border-radius: 0 0 16px 16px; }
.result-meta { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.result-meta span { padding: 5px 10px; border-radius: 999px; font-size: 11px; color: #728187; background: rgba(255, 255, 255, 0.86); }

.result-overview {
  display: grid;
  grid-template-columns: 170px 1fr;
  align-items: center;
  gap: 18px;
  padding: 20px 24px;
  color: #ecfbff;
  border-radius: 18px;
  background:
    radial-gradient(circle at 85% 0%, rgba(0, 190, 255, 0.25), transparent 45%),
    linear-gradient(135deg, #18353e, #2a4c50);
}

.health-rating {
  width: 126px;
  height: 126px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 8px solid rgba(255, 255, 255, .22);
  border-radius: 50%;
  background: rgba(255, 255, 255, .08);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, .08);
  cursor: help;
}
.health-rating.is-a { border-color: #78d3a0; }
.health-rating.is-b { border-color: #72c8d8; }
.health-rating.is-c { border-color: #d9a568; }
.health-rating.is-d { border-color: #d66f6f; }
.health-rating strong { font-size: 42px; line-height: 1; color: #fff; }
.health-rating span { margin-top: 6px; font-size: 14px; font-weight: 700; color: #e9fbff; }
.health-rating small { margin-top: 3px; font-size: 10px; color: rgba(255,255,255,.66); }
.overview-copy__head { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.overview-copy__head > span { font-size: 12px; letter-spacing: .08em; color: #83e7ff; }
.overview-copy p { margin: 12px 0 0; line-height: 1.85; font-size: 14px; color: rgba(245, 253, 255, .9); }

.highlight-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; margin-top: 14px; }
.highlight-card { display: grid; grid-template-columns: 32px 1fr; gap: 10px; padding: 13px 14px; border: 1px solid #e0e9e4; border-radius: 14px; background: rgba(255, 255, 255, .9); }
.highlight-card > span { width: 30px; height: 30px; display: grid; place-items: center; border-radius: 9px; font-size: 10px; font-weight: 700; color: #087997; background: #e2f8ff; }
.highlight-card strong { display: block; font-size: 12px; color: #30434a; }
.highlight-card p { margin: 4px 0 0; font-size: 11px; line-height: 1.5; color: #718087; }
.highlight-card small { display: block; margin-top: 5px; color: #8c9a9f; }

.report-section {
  margin-top: 14px;
  padding: 17px;
  border: 1px solid #e0e9e4;
  border-radius: 16px;
  background: #fff;
}
.report-section__header { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; margin-bottom: 13px; }
.report-section.is-collapsed .report-section__header { margin-bottom: 0; }
.report-section__header h3 { margin: 3px 0 0; font-size: 15px; color: #2b3d43; }
.report-section__header > span { font-size: 10px; color: #92a0a5; }
.report-section__aside { display: flex; align-items: center; gap: 12px; }
.report-section__aside > span { font-size: 10px; color: #92a0a5; }
.section-toggle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 9px;
  border: 1px solid #dbe7e1;
  border-radius: 8px;
  color: #64777d;
  background: #f7faf8;
  font-size: 11px;
  cursor: pointer;
}
.section-toggle:hover { color: #087997; border-color: rgba(0, 190, 255, .45); background: #f0fbff; }
.section-toggle .el-icon { transition: transform .2s ease; }
.section-toggle .el-icon.is-open { transform: rotate(180deg); }

.evidence-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.evidence-card { padding: 13px; border-radius: 13px; background: #f5faf7; border: 1px solid #e4eee8; }
.evidence-card header { display: flex; justify-content: space-between; gap: 8px; }
.evidence-card header strong { font-size: 13px; color: #30434a; }
.evidence-card header span { font-size: 10px; color: #93a099; }
.evidence-card__metrics { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin-top: 13px; }
.evidence-card__metrics div { min-width: 0; }
.evidence-card__metrics span { display: block; font-size: 10px; color: #8a999f; }
.evidence-card__metrics strong { display: block; margin-top: 3px; overflow: hidden; text-overflow: ellipsis; font-size: 13px; color: #263b42; }

.result-columns { display: flex; flex-direction: column; gap: 14px; }
.agent-result-list,
.risk-list,
.action-list { display: flex; flex-direction: column; gap: 8px; }
.agent-result-card { overflow: hidden; border: 1px solid #e5ebe8; border-radius: 12px; }
.agent-result-card.is-open { border-color: rgba(0, 190, 255, .48); box-shadow: 0 5px 16px rgba(0, 190, 255, .08); }
.agent-result-card__head { width: 100%; display: grid; grid-template-columns: 34px 1fr auto 18px; align-items: center; gap: 9px; padding: 10px; border: 0; text-align: left; background: #fff; cursor: pointer; }
.agent-result-card__avatar { width: 34px; height: 34px; border-radius: 10px; display: grid; place-items: center; color: #087997; background: #e6f9ff; font-weight: 700; }
.agent-result-card__title { min-width: 0; }
.agent-result-card__title strong { display: block; font-size: 12px; color: #30434a; }
.agent-result-card__title small { display: block; margin-top: 3px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; color: #89979c; }
.agent-result-card__arrow { color: #91a0a5; transition: transform .2s; }
.agent-result-card.is-open .agent-result-card__arrow { transform: rotate(180deg); }
.agent-result-card__body { padding: 0 13px 11px 53px; background: #fbfdfc; }
.agent-result-card__body ul { margin: 0; padding-left: 16px; }
.agent-result-card__body li { margin-top: 7px; font-size: 11px; line-height: 1.55; color: #66777d; }

.risk-card { padding: 11px 12px; border-left: 3px solid #9aa7aa; border-radius: 10px; background: #f8faf9; }
.risk-card.is-high { border-left-color: #c45a5a; background: #fff8f7; }
.risk-card.is-medium { border-left-color: #c9956a; background: #fffaf4; }
.risk-card.is-low { border-left-color: #5a9a6a; background: #f7fcf8; }
.risk-card__head { display: flex; align-items: center; gap: 8px; }
.risk-card__head span { padding: 2px 6px; border-radius: 6px; font-size: 9px; background: rgba(0,0,0,.05); color: #6a777c; }
.risk-card__head strong { font-size: 12px; color: #34464c; }
.risk-card p { margin: 7px 0 0; font-size: 11px; line-height: 1.55; color: #718087; }
.risk-card p b { margin-right: 7px; color: #45575d; }

.action-card { display: grid; grid-template-columns: 38px 1fr auto; align-items: center; gap: 12px; padding: 11px 13px; border-radius: 11px; background: #f7faf8; }
.action-card__priority { width: 34px; height: 28px; display: grid; place-items: center; border-radius: 8px; font-size: 11px; font-weight: 700; color: #087997; background: #e1f8ff; }
.action-card strong { font-size: 12px; color: #31434a; }
.action-card p { margin: 4px 0 0; font-size: 10px; color: #87969b; }
.action-card__aside { display: flex; align-items: center; gap: 8px; }
.action-card__department { padding: 4px 8px; border-radius: 999px; font-size: 10px; color: #5f7278; background: #e8efeb; }
.result-disclaimer { margin: 13px 4px 0; font-size: 10px; line-height: 1.6; color: #8c999e; }

.finance-ai-footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; width: 100%; }
.finance-ai-footer > span { font-size: 11px; color: #8b999e; }

@media (max-width: 900px) {
  .agent-run-grid { grid-template-columns: 1fr 1fr; }
  .highlight-grid,
  .evidence-grid { grid-template-columns: 1fr; }
}

@media (max-width: 640px) {
  .analysis-running,
  .analysis-result { padding: 12px; }
  .running-hero,
  .result-overview { grid-template-columns: 1fr; text-align: center; }
  .agent-run-grid { grid-template-columns: 1fr; }
  .report-section__aside > span { display: none; }
  .action-card { grid-template-columns: 38px 1fr; }
  .action-card__aside { grid-column: 2; justify-content: flex-end; }
  .finance-ai-footer { align-items: flex-end; }
  .finance-ai-footer > span { display: none; }
}
</style>
