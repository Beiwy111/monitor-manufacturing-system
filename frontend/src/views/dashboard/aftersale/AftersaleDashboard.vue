<template>
  <div class="after-workbench">
    <header class="command-bar">
      <div class="command-title">
        <span class="live-mark" />
        <div>
          <h1>售后调查与质量追溯</h1>
          <p>AI 分诊客户诉求，质量结论仅采用 MES 可核验证据</p>
        </div>
      </div>
      <div class="command-actions">
        <span v-if="bulkTriaging" class="batch-progress">正在分诊 {{ bulkProgress.done }}/{{ bulkProgress.total }}</span>
        <button class="text-action" @click="$router.push('/aftersale/case')">登记售后</button>
        <button class="ai-action" :disabled="bulkTriaging || !cases.length" @click="triageAllCases">
          <i>AI</i>{{ bulkTriaging ? '分诊中' : '一键 AI 分诊' }}
        </button>
      </div>
    </header>

    <section class="data-ribbon">
      <div><strong>{{ kpi.total || cases.length }}</strong><span>售后工单</span></div>
      <div><strong>{{ kpi.open || 0 }}</strong><span>待受理</span></div>
      <div><strong>{{ urgentCount }}</strong><span>紧急</span></div>
      <div><strong>{{ qualityCount }}</strong><span>质量风险</span></div>
      <div><strong>{{ triagedCount }}/{{ activeCaseCount }}</strong><span>AI 已分诊</span></div>
      <p><i />{{ bulkTriaging ? 'AI 正在读取工单并评估优先级' : '● 分析引擎在线' }}</p>
    </section>

    <main class="workbench-grid">
      <aside class="case-queue">
        <div class="section-head">
          <div><b>处置队列</b><span>按 AI 紧急程度排序</span></div>
          <span>{{ filteredCases.length }}</span>
        </div>
        <div class="queue-search">
          <input v-model="keyword" placeholder="工单 / 客户 / 批次" />
        </div>
        <div class="queue-list">
          <button
            v-for="item in filteredCases"
            :key="item.caseNo"
            class="queue-row"
            :class="[{ active:selectedCase?.caseNo===item.caseNo }, urgencyClass(triageMap[item.caseNo])]"
            @click="selectCase(item)"
          >
            <span class="urgency-bar" />
            <span class="queue-main">
              <span class="queue-line"><b>{{ item.caseNo }}</b><em>{{ urgencyText(triageMap[item.caseNo], item) }}</em></span>
              <strong>{{ item.customerName || '未填写客户' }}</strong>
              <small>{{ item.problemDescription || item.problemTypeCn }}</small>
              <span class="queue-meta">{{ item.materialName || '-' }} · {{ item.batchNo || '无批次' }}</span>
            </span>
          </button>
          <div v-if="!filteredCases.length" class="queue-empty">没有匹配的售后工单</div>
        </div>
      </aside>

      <section class="main-stage">
        <header class="stage-head">
          <div>
            <h2>{{ stageTitle }}</h2>
            <p>{{ stageSubtitle }}</p>
          </div>
          <div class="stage-actions">
            <button v-if="analysis" class="plain-button" @click="playGraph">重新播放</button>
            <button v-else-if="selectedCase && !triage" class="plain-button" :disabled="triaging" @click="runTriage(true)">单独分诊</button>
          </div>
        </header>

        <div v-if="analyzing || triaging || bulkTriaging" class="process-stage">
          <div class="process-head">
            <div class="process-head__title">
              <span class="process-head__badge">{{ bulkTriaging ? '批量分诊' : triaging ? '单独分诊' : '质量追溯' }}</span>
              <strong>{{ currentFlowSteps[flowStepIndex] }}</strong>
            </div>
            <p class="process-head__sub">
              <template v-if="bulkTriaging">
                正在处理 {{ bulkProgress.done + 1 }}/{{ bulkProgress.total }} · {{ triageProcessCase?.caseNo || '—' }}
              </template>
              <template v-else-if="triaging">
                {{ triageProcessCase?.customerName || '—' }} · {{ triageProcessCase?.caseNo || '—' }}
              </template>
              <template v-else>正在穿透生产、质量、设备与物料记录</template>
            </p>
          </div>

          <div class="process-body">
            <ol class="process-steps">
              <li
                v-for="(step, index) in currentFlowStepDefs"
                :key="step.title"
                :class="{
                  done: index < flowStepIndex,
                  active: index === flowStepIndex,
                  pending: index > flowStepIndex
                }"
              >
                <span class="process-steps__idx">{{ index + 1 }}</span>
                <div class="process-steps__main">
                  <b>{{ step.title }}</b>
                  <small>{{ step.desc }}</small>
                  <p v-if="index === flowStepIndex && step.hint">{{ step.hint }}</p>
                </div>
              </li>
            </ol>

            <aside class="process-log">
              <div class="process-log__head">分析过程</div>
              <ul>
                <li v-for="(line, idx) in processLog" :key="idx">{{ line }}</li>
                <li v-if="!processLog.length" class="process-log__empty">正在启动分析引擎…</li>
              </ul>
            </aside>
          </div>

          <div class="process-progress">
            <i :style="{ width: processProgress + '%' }" />
          </div>
        </div>

        <div v-else-if="analysis" class="trace-stage">
          <div
            ref="traceScrollRef"
            class="trace-viewport"
            :class="{ dragging:traceDragging }"
            @pointerdown="startTraceDrag"
            @pointermove="moveTraceDrag"
            @pointerup="endTraceDrag"
            @pointercancel="endTraceDrag"
            @wheel.prevent="scrollTrace"
          >
            <div class="ecg-track" :style="{ width:`${traceWaveWidth}px` }">
              <svg class="ecg-svg" :viewBox="`0 0 ${traceWaveWidth} 470`" preserveAspectRatio="none" aria-hidden="true">
                <path class="ecg-base" :d="traceWavePath" />
                <path ref="tracePathRef" class="ecg-progress" :d="traceWavePath" pathLength="1" :style="{ strokeDashoffset:1-traceProgress }" />
                <circle v-if="traceProgress>0 && traceProgress<1" class="runner-aura" :cx="traceCursor.x" :cy="traceCursor.y" r="17" />
                <circle v-if="traceProgress>0 && traceProgress<1" class="runner-core" :cx="traceCursor.x" :cy="traceCursor.y" r="6" />
              </svg>
              <button
                v-for="(step,index) in traceChain"
                :key="step.title"
                class="ecg-event"
                :class="[eventPositionClass(index),{visible:index<traceRevealCount,current:index===traceRevealCount-1&&traceProgress<1,missing:step.missing}]"
                :style="{ left:`${historyPointX(index)}px` }"
                @pointerdown.stop
                @pointermove.stop
                @pointerup.stop
                @pointercancel.stop
                @click.stop="openStep(step)"
              >
                <span class="event-stem" />
                <span class="event-dot">{{ String(index+1).padStart(2,'0') }}</span>
                <strong>{{ step.title }}</strong>
                <small>{{ step.missing ? '无留痕' : step.no }}</small>
              </button>
            </div>
          </div>
          <footer class="trace-footer">
            <span><i />镜头跟随 · 可拖动查看</span>
            <span>{{ Math.round(traceProgress*100) }}%</span>
            <p>点击节点查看完整记录与责任人</p>
          </footer>
        </div>

        <div v-else-if="triage" class="triage-focus">
          <header class="verdict-head" :class="urgencyClass(triage)">
            <div class="verdict-state"><i /><span>{{ urgencyText(triage) }}</span></div>
            <div class="verdict-kind">{{ triage.qualityRelated ? '质量问题' : triage.categoryName }}</div>
            <div class="verdict-case">{{ selectedCase?.caseNo }} · {{ selectedCase?.customerName }}</div>
          </header>
          <section class="diagnosis-stack">
            <article class="customer-brief">
              <div class="brief-title"><span>客户反馈</span><b>{{ selectedCase?.customerName || '未填写客户' }}</b></div>
              <p>{{ selectedCase?.problemDescription || '客户未填写详细问题描述' }}</p>
              <div class="brief-meta"><span>{{ selectedCase?.materialName || '-' }}</span><span>{{ selectedCase?.batchNo || '无批次' }}</span><span>{{ selectedCase?.problemTypeCn || '问题类型未记录' }}</span></div>
            </article>
            <article class="ai-judgement">
              <header><span>AI 研判</span><em>{{ triage.qualityRelated ? '建议进入质量追溯' : '建议按售后流程处理' }}</em></header>
              <h3>{{ aiConclusion }}</h3>
            </article>
          </section>
          <footer class="verdict-footer compact">
            <div><span>处置操作</span><strong>{{ triage.qualityRelated ? '受理工单后可继续追溯完整质量链路' : '受理工单并进入常规售后处理' }}</strong></div>
            <div class="verdict-actions">
              <button class="accept-button" :disabled="accepting" @click="acceptAndOpen">{{ accepting ? '受理中' : '一键受理' }}</button>
              <button v-if="triage.qualityRelated" class="trace-button" @click="startRca">质量追溯</button>
            </div>
          </footer>
        </div>

        <div v-else class="ready-state">
          <span>AI TRIAGE</span>
          <h3>点击右上角「一键 AI 分诊」</h3>
          <p>理解诉求 → 核对证据 → 排序处置 → 质量追溯</p>
          <div class="ready-flow"><i>理解诉求</i><b>→</b><i>核对证据</i><b>→</b><i>排序处置</i><b>→</b><i>质量追溯</i></div>
        </div>
      </section>

      <aside class="insight-rail">
        <template v-if="analysis">
          <div class="rail-status"><i />{{ analysis.cached ? '历史分析' : '追溯完成' }}<time>{{ analysis.analyzedAt }}</time></div>
          <section>
            <h3>追溯结论</h3>
            <p class="rail-conclusion">{{ analysis.conclusion }}</p>
          </section>
          <section>
            <div class="rail-heading"><h3>关键证据</h3><span>{{ analysis.nodes?.length || 0 }}</span></div>
            <button v-for="node in (analysis.nodes || []).slice(0,6)" :key="node.id" class="rail-row" @click="focusNode(node.id)">
              <i>{{ node.nodeType?.slice(0,2) }}</i><span><b>{{ node.name }}</b><small>{{ node.code }} · {{ node.summary }}</small></span><em>›</em>
            </button>
          </section>
          <section>
            <div class="rail-heading"><h3>协同处置</h3><span>{{ analysis.actions?.length || 0 }}</span></div>
            <label v-for="action in analysis.actions || []" :key="action.department+action.title" class="rail-check">
              <el-checkbox v-model="selectedDepartments" :value="action.department" />
              <span><b>{{ action.title }}</b><small>{{ departmentName(action.department) }}</small></span>
            </label>
            <button class="rail-primary" :disabled="dispatching" @click="dispatchTasks">派发协查任务</button>
            <button class="rail-secondary" @click="openConfirmRoot">人工确认根因</button>
          </section>
        </template>

        <template v-else-if="triage">
          <div class="rail-status"><i />AI 分诊完成<time>{{ triage.createdAt }}</time></div>
          <section>
            <h3>已核验证据</h3>
            <div v-if="triage.verifiedFacts?.length" class="fact-list">
              <p v-for="fact in triage.verifiedFacts" :key="fact.name"><b>{{ fact.name }}</b><span>{{ fact.detail || fact.evidence }}</span></p>
            </div>
            <p v-else class="muted-copy">当前没有足以直接判定质量风险的 MES 证据。</p>
          </section>
          <section v-if="triage.followUpQuestions?.length">
            <div class="rail-heading"><h3>需要补问</h3><span>{{ triage.followUpQuestions.length }}</span></div>
            <ol class="question-list"><li v-for="q in triage.followUpQuestions" :key="q">{{ q }}</li></ol>
          </section>
          <section>
            <div class="rail-heading"><h3>处理路径</h3><span>{{ triage.actions?.length || 0 }}</span></div>
            <div v-for="action in triage.actions || []" :key="action.title" class="route-row">
              <b>{{ action.title }}</b><span>{{ departmentName(action.department) }} · {{ action.reason }}</span>
            </div>
          </section>
        </template>

        <div v-else class="rail-empty">
          <b>待分诊</b><p>完成批量分诊后，这里只展示当前工单的证据、补问项和处理路径。</p>
        </div>
      </aside>
    </main>

    <el-drawer v-model="drawerVisible" size="400px" :title="activeNode?.name || '证据详情'">
      <div v-if="activeNode" class="drawer-content">
        <dl><dt>业务编号</dt><dd>{{ activeNode.code }}</dd><dt>所属环节</dt><dd>{{ departmentName(activeNode.department) }}</dd><dt>业务摘要</dt><dd>{{ activeNode.summary }}</dd></dl>
        <h4>可核验证据</h4><p v-for="e in activeNode.evidences" :key="e">{{ e }}</p>
      </div>
    </el-drawer>

    <el-drawer v-model="stepDrawerVisible" size="420px" :title="(activeStep?.title || '') + ' · 完整记录'">
      <div v-if="activeStep" class="drawer-content">
        <dl><dt>业务编号</dt><dd>{{ activeStep.no }}</dd><template v-for="dd in activeStep.details" :key="dd.label"><dt>{{ dd.label }}</dt><dd>{{ dd.value }}</dd></template></dl>
        <template v-if="activeStep.people?.length"><h4>责任人</h4><p v-for="p in activeStep.people" :key="p.role+p.name">{{ p.role }}：{{ p.name }}</p></template>
        <template v-if="activeStep.rows?.length"><h4>明细记录</h4><p v-for="(row,index) in activeStep.rows" :key="index">{{ row }}</p></template>
        <p v-if="activeStep.missing" class="missing-note">该环节在系统中没有留痕记录。</p>
      </div>
    </el-drawer>

    <el-dialog v-model="confirmVisible" title="人工确认最终根因" width="480px">
      <el-form label-position="top">
        <el-form-item label="最终根因"><el-select v-model="confirmForm.cause" style="width:100%"><el-option v-for="c in analysis?.causes||[]" :key="c.name" :label="c.name" :value="c.name" /></el-select></el-form-item>
        <el-form-item label="责任/整改部门"><el-select v-model="confirmForm.department" style="width:100%"><el-option v-for="c in analysis?.causes||[]" :key="c.department" :label="departmentName(c.department)" :value="c.department" /></el-select></el-form-item>
        <el-form-item label="人工确认意见"><el-input v-model="confirmForm.remark" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="confirmVisible=false">取消</el-button><el-button type="primary" @click="submitConfirmRoot">确认</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { acceptCase, confirmRcaRootCause, dispatchRcaTasks, fetchAfterSalesKpi, fetchAfterSalesTriage, fetchCaseViews, fetchRcaAnalysis, fetchRcaTaskProgress, fetchTraceDetail } from '@/api/aftersale'

const router = useRouter()
const route = useRoute()
const cases=ref([]),keyword=ref(''),selectedCase=ref(null),triage=ref(null),analysis=ref(null)
const triageMap=ref({}),bulkTriaging=ref(false),bulkProgress=ref({done:0,total:0}),triaging=ref(false),analyzing=ref(false),accepting=ref(false)
const kpi=ref({total:0,open:0,processing:0}),traceChain=ref([]),traceRevealCount=ref(0),traceProgress=ref(0)
const tracePathRef=ref(null),traceScrollRef=ref(null),traceCursor=ref({x:0,y:235}),traceViewportWidth=ref(0)
const traceDragging=ref(false),traceManualUntil=ref(0)
const activeStep=ref(null),stepDrawerVisible=ref(false),activeNode=ref(null),drawerVisible=ref(false)
const selectedDepartments=ref([]),dispatching=ref(false),confirmVisible=ref(false),confirmForm=ref({cause:'',department:'',remark:''}),taskProgress=ref({total:0})
let graphAnimationFrame, traceResizeObserver, traceDragStartX = 0, traceDragStartScroll = 0, traceDragMoved = false
const triageStepDefs = [
  {
    title: '理解客户问题与业务诉求',
    desc: '解析客户描述、问题类型与影响范围',
    hint: '识别口语化故障现象，区分采购总量与实际故障数量'
  },
  {
    title: '核对订单、产品和售后履历',
    desc: '关联订单、批次、产品与历史售后记录',
    hint: '核对 MES 中可留痕的产品型号、发货批次与既往工单'
  },
  {
    title: '评估批次扩散与质量风险',
    desc: '检索同批次、同工序与设备报警线索',
    hint: '评估是否存在批次扩散、重复投诉与生产侧异常'
  },
  {
    title: '生成处置优先级和处理路径',
    desc: '综合证据给出紧急程度与后续动作',
    hint: '形成补问项、协查建议与是否进入质量追溯'
  }
]
const analyzeStepDefs = [
  { title: '定位产品与发货履历', desc: '回溯订单、出库与发货节点', hint: '确认问题产品对应的生产批次与流向' },
  { title: '穿透入库和成品质检', desc: '核对成品检验与放行记录', hint: '查看质检结论、不良项与放行责任人' },
  { title: '核对生产、领料与物料质检', desc: '关联产线、领料与来料检验', hint: '定位关键工序、物料批次与工艺参数' },
  { title: '回溯供应商并形成证据链', desc: '汇总多环节证据形成追溯链', hint: '串联采购、生产、质量与设备留痕' }
]
const triageSteps = triageStepDefs.map((s) => s.title)
const analyzeSteps = analyzeStepDefs.map((s) => s.title)
const currentFlowStepDefs = computed(() => (triaging.value || bulkTriaging.value) ? triageStepDefs : analyzeStepDefs)
const currentFlowSteps = computed(() => currentFlowStepDefs.value.map((s) => s.title))
const flowStepIndex = ref(0)
const processLog = ref([])
const triageProcessCase = ref(null)
const processProgress = ref(0)

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function buildTriageLogLine(stepIndex, caseItem) {
  const desc = String(caseItem?.problemDescription || caseItem?.problemTypeCn || '未填写问题描述')
  const material = caseItem?.materialName || '未登记产品'
  const batch = caseItem?.batchNo || '无批次'
  const customer = caseItem?.customerName || '未填写客户'
  const lines = [
    `读取客户反馈：${desc.slice(0, 48)}${desc.length > 48 ? '…' : ''}`,
    `核对产品档案：${material} · 批次 ${batch}`,
    `检索同批次扩散风险与生产侧报警、质检异常线索`,
    `综合 MES 证据，生成紧急程度、补问项与处理路径`
  ]
  return lines[stepIndex] || lines[lines.length - 1]
}

function buildTriageWaitLines(caseItem) {
  const material = caseItem?.materialName || '产品'
  const batch = caseItem?.batchNo || '无批次'
  const customer = caseItem?.customerName || '客户'
  const problem = String(caseItem?.problemTypeCn || caseItem?.problemDescription || '故障现象').slice(0, 20)
  return [
    `调用 AI 研判服务，解析 ${customer} 反馈的「${problem}」`,
    `查询订单与发货履历，确认 ${material} 流向`,
    `检索批次 ${batch} 关联的生产工序与设备报警记录`,
    `比对成品质检结论与同批次售后投诉扩散情况`,
    `梳理 MES 可核验证据与客户陈述差异点`,
    `综合研判紧急程度，生成补问项与处理路径`
  ]
}

function buildAnalyzeWaitLines(caseItem) {
  const material = caseItem?.materialName || '产品'
  const batch = caseItem?.batchNo || '无批次'
  return [
    `汇总 ${material} · 批次 ${batch} 的多环节业务留痕`,
    `穿透成品质检、入库放行与发货节点记录`,
    `核对生产领料、工序流转与物料质检结果`,
    `关联设备报警、维保记录与供应商来料检验`,
    `构建反向追溯证据链，定位可疑质量节点`,
    `整理追溯结论与协查部门建议`
  ]
}

function appendOrUpdateWaitingLog(lines, waitIdx, waitStarted) {
  if (waitIdx < lines.length) {
    processLog.value = [...processLog.value, lines[waitIdx]]
    return waitIdx + 1
  }
  const sec = Math.max(1, Math.floor((Date.now() - waitStarted) / 1000))
  const next = [...processLog.value]
  next[next.length - 1] = `AI 研判仍在进行，已等待 ${sec} 秒…`
  processLog.value = next
  return waitIdx
}

function buildAnalyzeLogLine(stepIndex, caseItem) {
  const batch = caseItem?.batchNo || '无批次'
  const material = caseItem?.materialName || '未登记产品'
  const lines = [
    `定位售后产品：${material} · 批次 ${batch}`,
    `穿透成品质检与入库放行记录`,
    `核对生产领料、工序流转与物料质检结果`,
    `回溯供应商与设备留痕，汇总追溯证据链`
  ]
  return lines[stepIndex] || lines[lines.length - 1]
}

async function animateFlowProcess(caseItem, mode = 'single', apiPromise = null) {
  const isBulk = mode === 'bulk'
  const isAnalyze = mode === 'analyze'
  const stepMs = isBulk ? 820 : isAnalyze ? 560 : 260
  const minDuration = isBulk ? 3000 : isAnalyze ? 2200 : 850
  const defs = isAnalyze ? analyzeStepDefs : triageStepDefs
  const buildLog = isAnalyze ? buildAnalyzeLogLine : buildTriageLogLine

  triageProcessCase.value = caseItem
  processLog.value = []
  flowStepIndex.value = 0
  processProgress.value = 0

  const started = Date.now()
  for (let i = 0; i < defs.length; i++) {
    flowStepIndex.value = i
    processProgress.value = Math.round(((i + 0.35) / defs.length) * 100)
    processLog.value = [...processLog.value, buildLog(i, caseItem)]
    if (i < defs.length - 1) await sleep(stepMs)
  }

  const elapsed = Date.now() - started
  if (elapsed < minDuration) await sleep(minDuration - elapsed)
  flowStepIndex.value = defs.length - 1
  processProgress.value = 92
  processLog.value = [...processLog.value, isAnalyze ? '追溯链路整理完成，进入证据汇总阶段' : '分析步骤完成，进入 AI 深度研判阶段']

  if (apiPromise) {
    const waitLines = isAnalyze ? buildAnalyzeWaitLines(caseItem) : buildTriageWaitLines(caseItem)
    let waitIdx = 0
    const waitStarted = Date.now()
    while (true) {
      const settled = await Promise.race([
        apiPromise.then(() => true).catch(() => true),
        sleep(700).then(() => false)
      ])
      if (settled) break
      waitIdx = appendOrUpdateWaitingLog(waitLines, waitIdx, waitStarted)
    }
  }

  processProgress.value = 100
  processLog.value = [...processLog.value, isAnalyze ? '追溯分析完成' : '分诊结论已整理完成']
}

function resetFlowProcess() {
  flowStepIndex.value = 0
  processLog.value = []
  processProgress.value = 0
  triageProcessCase.value = null
}

const activeCases=computed(()=>cases.value.filter(item=>item.caseStatus!=='CLOSED'))
const activeCaseCount=computed(()=>activeCases.value.length)
const triagedCount=computed(()=>activeCases.value.filter(item=>triageMap.value[item.caseNo]).length)
const urgentCount=computed(()=>activeCases.value.filter(item=>triageMap.value[item.caseNo]?.urgency==='RED').length)
const qualityCount=computed(()=>activeCases.value.filter(item=>triageMap.value[item.caseNo]?.qualityRelated).length)
const filteredCases=computed(()=>{
  const q=keyword.value.trim().toLowerCase()
  return activeCases.value.filter(item=>!q||[item.caseNo,item.customerName,item.batchNo,item.materialName,item.problemDescription].some(v=>(v||'').toLowerCase().includes(q))).sort((a,b)=>casePriority(b)-casePriority(a)||String(b.openedAt||'').localeCompare(String(a.openedAt||'')))
})
const stageTitle=computed(()=>analysis.value?'质量追溯':triage.value?'AI 分诊结论':'AI 分诊队列')
const stageSubtitle=computed(()=>analysis.value?'心电轨迹沿真实业务留痕反向推进':triage.value?`${selectedCase.value?.caseNo} · ${triage.value.categoryName}`:'对全部未关闭工单统一识别、排序和分流')
const aiConclusion=computed(()=>cleanAiText(triage.value?.aiUnderstanding||triage.value?.summary||''))

const TRACE_START=150,TRACE_STEP=205,TRACE_BASELINE=235,TRACE_LEAD=72
const traceWaveWidth=computed(()=>Math.max(1500,traceViewportWidth.value*1.75))
const waveUnit=computed(()=>{
  if(traceChain.value.length<=2)return TRACE_STEP
  return Math.max(TRACE_STEP,(traceWaveWidth.value-TRACE_START*2-TRACE_LEAD*2)/(traceChain.value.length-2))
})
const traceWavePath=computed(()=>{
  const count=traceChain.value.length
  if(!count)return ''
  const firstX=historyPointX(0)
  if(count===1)return `M ${firstX} ${TRACE_BASELINE}`
  const lastIndex=count-1,lastX=historyPointX(lastIndex)
  if(count===2)return `M ${firstX} ${TRACE_BASELINE} L ${lastX} ${TRACE_BASELINE}`
  const leadEnd=firstX+TRACE_LEAD,firstPeakX=historyPointX(1),firstPeakY=historyPointY(1)
  let d=`M ${firstX} ${TRACE_BASELINE} L ${leadEnd} ${TRACE_BASELINE}`
  let span=firstPeakX-leadEnd
  d+=` C ${leadEnd+span*.5} ${TRACE_BASELINE},${firstPeakX-span*.5} ${firstPeakY},${firstPeakX} ${firstPeakY}`
  for(let i=2;i<lastIndex;i++){
    const x0=historyPointX(i-1),x1=historyPointX(i),y0=historyPointY(i-1),y1=historyPointY(i)
    const control=(x1-x0)*.5
    d+=` C ${x0+control} ${y0},${x1-control} ${y1},${x1} ${y1}`
  }
  const previousX=historyPointX(lastIndex-1),previousY=historyPointY(lastIndex-1),exitStart=lastX-TRACE_LEAD
  span=exitStart-previousX
  d+=` C ${previousX+span*.5} ${previousY},${exitStart-span*.5} ${TRACE_BASELINE},${exitStart} ${TRACE_BASELINE}`
  d+=` L ${lastX} ${TRACE_BASELINE}`
  return d
})

function casePriority(item){
  const result=triageMap.value[item.caseNo]
  if(result)return ({RED:300,YELLOW:200,GREEN:100}[result.urgency]||0)
  return 0
}
function urgencyClass(result){return result?.urgency==='RED'?'urgent':result?.urgency==='YELLOW'?'attention':result?'routine':'untriaged'}
function urgencyText(result){return result?({RED:'紧急',YELLOW:'关注',GREEN:'一般'}[result.urgency]||'一般'):'待分诊'}
function historyPointX(index){
  const lastIndex=traceChain.value.length-1
  if(index<=0)return TRACE_START
  if(lastIndex===1)return TRACE_START+TRACE_LEAD*2
  if(index>=lastIndex)return TRACE_START+TRACE_LEAD*2+(lastIndex-1)*waveUnit.value
  return TRACE_START+TRACE_LEAD+(index-.5)*waveUnit.value
}
function historyPointY(index){
  if(index===0||index===traceChain.value.length-1)return TRACE_BASELINE
  return index%2===1?58:412
}
function eventPositionClass(index){if(index===0||index===traceChain.value.length-1)return 'endpoint';return index%2===1?'peak':'valley'}
function departmentName(value){return {PURCHASE:'采购',DEVICE:'设备',QUALITY:'质量',COST:'财务',PRODUCTION:'生产',AFTERSALE:'售后',ORDER:'订单'}[value]||value}
function cleanAiText(value){
  return String(value||'')
    .replace(/\s*\([^)]*(?:sameProblemBatchCases|sameBatchCases|productionAlarmCount)[^)]*\)/gi,'')
    .replace(/(?:sameProblemBatchCases|sameBatchCases|productionAlarmCount)\s*[:：]\s*\d+/gi,'')
    .replace(/\s{2,}/g,' ')
    .trim()
}

async function reloadCases() {
  try {
    const [list, stats] = await Promise.all([
      fetchCaseViews().catch(() => []),
      fetchAfterSalesKpi().catch(() => ({}))
    ])
    cases.value = list || []
    kpi.value = stats || {}
    if (selectedCase.value) {
      const current = cases.value.find((item) => item.caseNo === selectedCase.value.caseNo)
      if (current) selectedCase.value = current
    }
  } catch (e) {
    ElMessage.error(e?.message || '加载售后数据失败')
  }
}

async function hydrateCaseContext(item, { restoreTrace = false, loadTriage = false } = {}) {
  if (!item) return
  selectCase(item)
  if (triageMap.value[item.caseNo]) {
    triage.value = triageMap.value[item.caseNo]
  } else if (loadTriage) {
    try {
      const result = await fetchAfterSalesTriage(item.caseNo, false)
      triageMap.value = { ...triageMap.value, [item.caseNo]: result }
      triage.value = result
    } catch {
      triage.value = null
    }
  } else {
    triage.value = null
  }
  if (restoreTrace && triage.value?.qualityRelated) {
    try {
      const cached = await fetchRcaAnalysis(item.caseNo, false)
      if (cached?.cached && cached?.nodes?.length) {
        analysis.value = cached
        const trace = await fetchTraceDetail(item.caseNo).catch(() => null)
        traceChain.value = trace?.traceChain || []
        selectedDepartments.value = [...new Set((cached.actions || []).map((a) => a.department))]
        taskProgress.value = await fetchRcaTaskProgress(item.caseNo).catch(() => ({ total: 0 }))
        await nextTick()
        playGraph()
      }
    } catch { /* 无追溯记录则保持分诊视图 */ }
  }
}

function selectCase(item) {
  cancelAnimationFrame(graphAnimationFrame)
  selectedCase.value = item
  triage.value = triageMap.value[item.caseNo] || null
  analysis.value = null
  traceChain.value = []
  traceProgress.value = 0
  traceRevealCount.value = 0
}

async function triageAllCases() {
  const targets = activeCases.value.filter((c) => c.caseStatus !== 'CLOSED')
  if (!targets.length) {
    ElMessage.warning('暂无未关闭的售后工单')
    return
  }
  bulkTriaging.value = true
  bulkProgress.value = { done: 0, total: targets.length }
  analysis.value = null

  for (let i = 0; i < targets.length; i++) {
    const item = targets[i]
    bulkProgress.value = { done: i, total: targets.length }
    try {
      const apiPromise = fetchAfterSalesTriage(item.caseNo, false)
      const [result] = await Promise.all([
        apiPromise,
        animateFlowProcess(item, 'bulk', apiPromise)
      ])
      triageMap.value = { ...triageMap.value, [item.caseNo]: result }
      if (selectedCase.value?.caseNo === item.caseNo) triage.value = result
    } catch (e) {
      console.warn('[triage]', item.caseNo, e)
    }
    bulkProgress.value = { done: i + 1, total: targets.length }
  }

  bulkTriaging.value = false
  resetFlowProcess()
  const first = filteredCases.value[0]
  if (first) await hydrateCaseContext(first, { loadTriage: false })
  ElMessage.success(`AI 分诊完成，共处理 ${bulkProgress.value.done} 张工单`)
}

async function runTriage(force = false) {
  if (!selectedCase.value) return
  const item = selectedCase.value
  triaging.value = true
  analysis.value = null
  triage.value = null
  try {
    const apiPromise = fetchAfterSalesTriage(item.caseNo, force)
    const [result] = await Promise.all([
      apiPromise,
      animateFlowProcess(item, 'single', apiPromise)
    ])
    triageMap.value = { ...triageMap.value, [item.caseNo]: result }
    triage.value = result
  } catch (e) {
    ElMessage.error(e?.message || 'AI 分诊失败')
  } finally {
    triaging.value = false
    resetFlowProcess()
  }
}
async function acceptAndOpen(){
  if(!selectedCase.value)return
  accepting.value=true
  try{
    if(selectedCase.value.caseStatus==='OPEN')await acceptCase({caseNo:selectedCase.value.caseNo})
    await reloadCases()
    router.push({path:'/aftersale/case',query:{caseNo:selectedCase.value.caseNo}})
  }catch(e){ElMessage.error(e?.message||'工单受理失败')}
  finally{accepting.value=false}
}
async function startRca(){await runAnalysis(false)}
async function runAnalysis(force = false) {
  if (!selectedCase.value) return
  analyzing.value = true
  analysis.value = null
  try {
    const analysisPromise = fetchRcaAnalysis(selectedCase.value.caseNo, force)
    const tracePromise = fetchTraceDetail(selectedCase.value.caseNo).catch(() => null)
    const [result, trace] = await Promise.all([
      analysisPromise,
      tracePromise,
      animateFlowProcess(selectedCase.value, 'analyze', analysisPromise)
    ])
    analysis.value = result
    traceChain.value = trace?.traceChain || []
    selectedDepartments.value = [...new Set((result.actions || []).map((item) => item.department))]
    taskProgress.value = await fetchRcaTaskProgress(result.caseNo).catch(() => ({ total: 0 }))
  } catch (e) {
    ElMessage.error(e?.message || '质量追溯失败')
  } finally {
    analyzing.value = false
    resetFlowProcess()
  }
  if (analysis.value) playGraph()
}
async function playGraph(){
  cancelAnimationFrame(graphAnimationFrame);traceRevealCount.value=0;traceProgress.value=0
  if(!traceChain.value.length)return
  await nextTick();observeTraceViewport();await nextTick()
  const path=tracePathRef.value;if(!path)return
  traceScrollRef.value.scrollLeft=0
  const totalLength=path.getTotalLength(),duration=Math.max(5200,traceChain.value.length*900),start=performance.now()
  const nodeProgress=traceChain.value.map((_,index)=>pathProgressAtX(path,totalLength,historyPointX(index)))
  const animate=now=>{
    const raw=Math.min(1,Math.max(0,now-start-240)/duration),eased=raw<.5?2*raw*raw:1-Math.pow(-2*raw+2,2)/2
    traceProgress.value=eased;traceRevealCount.value=nodeProgress.filter(progress=>progress<=eased+.002).length
    const point=path.getPointAtLength(totalLength*eased);traceCursor.value={x:point.x,y:point.y};followTraceRunner(point.x)
    if(raw<1)graphAnimationFrame=requestAnimationFrame(animate)
    else traceRevealCount.value=traceChain.value.length
  }
  graphAnimationFrame=requestAnimationFrame(animate)
}
function pathProgressAtX(path,totalLength,targetX){
  let low=0,high=totalLength
  for(let i=0;i<22;i++){
    const middle=(low+high)/2
    if(path.getPointAtLength(middle).x<targetX)low=middle
    else high=middle
  }
  return ((low+high)/2)/totalLength
}
function followTraceRunner(x){
  const scroller=traceScrollRef.value;if(!scroller)return
  if(performance.now()<traceManualUntil.value)return
  const target=Math.max(0,Math.min(scroller.scrollWidth-scroller.clientWidth,x-scroller.clientWidth*.43))
  scroller.scrollLeft+=(target-scroller.scrollLeft)*.09
}
function startTraceDrag(event){
  if(event.button!==undefined&&event.button!==0)return
  traceDragging.value=true;traceDragMoved=false;traceManualUntil.value=performance.now()+1600;traceDragStartX=event.clientX;traceDragStartScroll=traceScrollRef.value?.scrollLeft||0
  traceScrollRef.value?.setPointerCapture?.(event.pointerId)
}
function moveTraceDrag(event){if(traceDragging.value&&traceScrollRef.value){if(Math.abs(event.clientX-traceDragStartX)>5)traceDragMoved=true;traceScrollRef.value.scrollLeft=traceDragStartScroll-(event.clientX-traceDragStartX)}}
function endTraceDrag(event){traceDragging.value=false;traceManualUntil.value=performance.now()+900;traceScrollRef.value?.releasePointerCapture?.(event.pointerId)}
function scrollTrace(event){
  const scroller=traceScrollRef.value;if(!scroller)return
  traceManualUntil.value=performance.now()+1200;scroller.scrollLeft+=Math.abs(event.deltaX)>Math.abs(event.deltaY)?event.deltaX:event.deltaY
}
function observeTraceViewport(){traceResizeObserver?.disconnect();if(!traceScrollRef.value)return;traceResizeObserver=new ResizeObserver(()=>traceViewportWidth.value=traceScrollRef.value?.clientWidth||0);traceResizeObserver.observe(traceScrollRef.value);traceViewportWidth.value=traceScrollRef.value.clientWidth}
function openStep(step){activeStep.value=step;stepDrawerVisible.value=true}
function focusNode(id){const node=analysis.value?.nodes?.find(item=>item.id===id);if(node){activeNode.value=node;drawerVisible.value=true}}
async function dispatchTasks(){if(!selectedDepartments.value.length){ElMessage.warning('请至少选择一个协查部门');return}dispatching.value=true;try{const result=await dispatchRcaTasks({caseNo:analysis.value.caseNo,departments:selectedDepartments.value});ElMessage.success(`已派发 ${result.taskCount} 项协查任务`)}finally{dispatching.value=false}}
function openConfirmRoot(){const top=analysis.value?.causes?.[0];confirmForm.value={cause:top?.name||'',department:top?.department||'',remark:''};confirmVisible.value=true}
async function submitConfirmRoot(){if(!confirmForm.value.cause||!confirmForm.value.department){ElMessage.warning('请选择最终根因和部门');return}await confirmRcaRootCause({analysisNo:analysis.value.analysisNo,...confirmForm.value});confirmVisible.value=false;ElMessage.success('最终根因已人工确认')}

onMounted(async () => {
  await reloadCases()
  const qCase = route.query.caseNo
  const target = (qCase && cases.value.find((c) => c.caseNo === qCase)) || filteredCases.value[0]
  if (target) {
    selectCase(target)
    if (triageMap.value[target.caseNo]) {
      triage.value = triageMap.value[target.caseNo]
    }
  }
})

onBeforeUnmount(() => {
  cancelAnimationFrame(graphAnimationFrame)
  traceResizeObserver?.disconnect()
})
</script>

<style scoped>
.after-workbench{min-height:100%;padding:18px;background:#f3f6f9;color:#142b48}.command-bar{height:78px;display:flex;align-items:center;justify-content:space-between;padding:0 22px;background:#fff;border-top:3px solid #173d62;border-bottom:1px solid #cfd9e3}.command-title{display:flex;align-items:center;gap:13px}.live-mark{width:9px;height:9px;border-radius:50%;background:#29c997;box-shadow:0 0 0 6px #29c99717}.command-title h1{margin:0;font-size:22px}.command-title p{margin:5px 0 0;color:#7a899a;font-size:12px}.command-actions{display:flex;align-items:center;gap:10px}.batch-progress{color:#2679c9;font-size:12px}.text-action,.plain-button{padding:8px 14px;border:1px solid #cad5df;background:#fff;color:#405469;cursor:pointer}.ai-action{display:flex;align-items:center;gap:8px;padding:10px 18px;border:0;background:#163d63;color:#fff;font-weight:700;cursor:pointer}.ai-action i{font-style:normal;color:#6fe2ff}.ai-action:disabled,.plain-button:disabled{opacity:.55;cursor:not-allowed}.data-ribbon{height:62px;display:flex;align-items:center;background:#fff;border-bottom:1px solid #cfd9e3}.data-ribbon>div{min-width:125px;padding:0 22px;border-right:1px solid #dde5ec}.data-ribbon strong,.data-ribbon span{display:block}.data-ribbon strong{font-size:20px}.data-ribbon span{margin-top:2px;color:#8291a1;font-size:10px}.data-ribbon p{margin-left:auto;padding-right:22px;color:#607286;font-size:11px}.data-ribbon p i,.rail-status>i{display:inline-block;width:7px;height:7px;margin-right:7px;border-radius:50%;background:#29c997;box-shadow:0 0 8px #29c997}.workbench-grid{display:grid;grid-template-columns:270px minmax(540px,1fr) 320px;height:670px;margin-top:12px;background:#fff;border-top:1px solid #cbd6e0;border-bottom:1px solid #cbd6e0}.case-queue,.main-stage,.insight-rail{min-width:0}.case-queue{border-right:1px solid #d7e0e8}.main-stage{border-right:1px solid #d7e0e8}.section-head,.stage-head{height:68px;display:flex;align-items:center;justify-content:space-between;padding:0 16px;border-bottom:1px solid #dce4eb}.section-head b,.section-head span,.stage-head h2,.stage-head p{display:block}.section-head b{font-size:14px}.section-head div span,.stage-head p{margin-top:4px;color:#8b99a8;font-size:10px}.section-head>span{font:700 13px monospace;color:#5481ad}.queue-search{padding:10px 12px;border-bottom:1px solid #e1e7ed}.queue-search input{box-sizing:border-box;width:100%;height:35px;padding:0 10px;border:0;border-bottom:1px solid #aebdca;outline:none;color:#253b53}.queue-list{height:calc(100% - 124px);overflow:auto}.queue-row{position:relative;display:flex;width:100%;padding:12px 12px 12px 17px;border:0;border-bottom:1px solid #e4e9ee;background:#fff;text-align:left;color:#25384d;cursor:pointer}.queue-row:hover,.queue-row.active{background:#eef5fb}.urgency-bar{position:absolute;left:0;top:0;bottom:0;width:4px;background:#bac6d1}.queue-row.urgent .urgency-bar{background:#e5484d}.queue-row.attention .urgency-bar{background:#e7a62b}.queue-row.routine .urgency-bar{background:#38a879}.queue-main{min-width:0;width:100%}.queue-line{display:flex;align-items:center;justify-content:space-between}.queue-line b{font:700 11px monospace}.queue-line em{font-size:9px;font-style:normal;color:#73869a}.queue-row.urgent .queue-line em{color:#d43d43}.queue-main>strong,.queue-main>small,.queue-meta{display:block}.queue-main>strong{margin-top:6px;font-size:12px}.queue-main>small{margin-top:5px;color:#6f8091;font-size:10px;line-height:1.45;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.queue-meta{margin-top:6px;color:#9aa7b4;font-size:9px}.queue-empty{padding:40px 15px;text-align:center;color:#99a7b5;font-size:11px}.stage-head h2{margin:0;font-size:16px}.stage-actions{display:flex;gap:8px}.scan-state,.ready-state{height:calc(100% - 69px);display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center}
.process-stage{height:calc(100% - 63px);display:flex;flex-direction:column;padding:22px 28px;box-sizing:border-box;background:linear-gradient(180deg,#fbfdff,#f3f8fc)}
.process-head{border-bottom:1px solid #dce5ee;padding-bottom:14px}
.process-head__badge{display:inline-block;margin-bottom:8px;padding:2px 8px;background:#e8f2fb;color:#2478c8;font-size:11px;font-weight:700;letter-spacing:.5px}
.process-head__title strong{display:block;font-size:18px;color:#173d62}
.process-head__sub{margin:8px 0 0;color:#7b8c9d;font-size:12px}
.process-body{flex:1;min-height:0;display:grid;grid-template-columns:minmax(280px,1.1fr) minmax(220px,.9fr);gap:18px;margin-top:18px;overflow:hidden}
.process-steps{margin:0;padding:0;list-style:none;overflow:auto}
.process-steps li{display:flex;gap:12px;padding:12px 0;border-bottom:1px dashed #e2eaf1}
.process-steps li:last-child{border-bottom:none}
.process-steps__idx{flex-shrink:0;width:26px;height:26px;display:grid;place-items:center;border-radius:50%;background:#e8eef4;color:#7b8da0;font-size:12px;font-weight:700}
.process-steps__main{min-width:0}
.process-steps__main b{display:block;font-size:14px;color:#2a4560}
.process-steps__main small{display:block;margin-top:4px;color:#8a9aae;font-size:11px;line-height:1.45}
.process-steps__main p{margin:8px 0 0;padding:8px 10px;background:#eef6fc;border-left:3px solid #3b8fd9;color:#4f6c86;font-size:11px;line-height:1.5}
.process-steps li.active .process-steps__idx{background:#2478c8;color:#fff;box-shadow:0 0 0 4px #2478c822}
.process-steps li.active .process-steps__main b{color:#1d6eb8}
.process-steps li.done .process-steps__idx{background:#29a877;color:#fff}
.process-steps li.done .process-steps__main b{color:#3d8f6d}
.process-steps li.pending{opacity:.55}
.process-log{display:flex;flex-direction:column;min-height:0;border:1px solid #dce6ef;background:#fff}
.process-log__head{padding:10px 12px;border-bottom:1px solid #e7edf3;color:#5f7388;font-size:12px;font-weight:700}
.process-log ul{flex:1;margin:0;padding:10px 12px 12px 28px;overflow:auto}
.process-log li{padding:5px 0;color:#4f657a;font-size:11px;line-height:1.55}
.process-log__empty{list-style:none;margin-left:-16px;color:#95a5b5}
.process-progress{height:4px;margin-top:14px;background:#e2ebf3;border-radius:2px;overflow:hidden}
.process-progress i{display:block;height:100%;background:linear-gradient(90deg,#3b8fd9,#29c997);transition:width .35s ease}.ready-state>span{font:700 11px monospace;letter-spacing:3px;color:#2783d7}.ready-state h3{margin:15px 0 0;font-size:22px}.ready-state p{margin-top:8px;color:#8392a2;font-size:11px}.ready-flow{display:flex;align-items:center;gap:12px;margin-top:35px}.ready-flow i{font-style:normal;color:#546b82;font-size:11px}.ready-flow b{color:#a7b4c1}.triage-focus{height:calc(100% - 69px);padding:34px 45px;box-sizing:border-box;overflow:auto}.decision-line{display:flex;align-items:center;border-top:2px solid #8295a8;border-bottom:1px solid #dbe3ea;padding:10px 0}.decision-line span{min-width:58px;font-weight:800}.decision-line b{font-size:12px}.decision-line em{margin-left:auto;color:#7b8c9e;font-size:10px;font-style:normal}.decision-line.urgent{border-top-color:#e5484d;color:#c9343a}.decision-line.attention{border-top-color:#e7a62b;color:#a86e00}.decision-line.routine{border-top-color:#38a879;color:#21845c}.triage-focus>h3{margin:32px 0;font-size:21px;line-height:1.75}.phenomena-list{border-top:1px solid #dbe3ea;border-bottom:1px solid #dbe3ea;padding:16px 0}.phenomena-list>span,.next-step-line>div>span{color:#8090a1;font-size:10px}.phenomena-list p{display:inline-block;margin:8px 18px 0 0;font-size:11px}.phenomena-list p:before{content:'·';margin-right:6px;color:#2f83d2}.next-step-line{display:flex;align-items:center;gap:10px;margin-top:28px}.next-step-line>div{min-width:0;flex:1}.next-step-line strong,.next-step-line small{display:block}.next-step-line strong{margin-top:6px;font-size:14px}.next-step-line small{margin-top:4px;color:#7e8e9f;font-size:10px}.accept-button,.trace-button{padding:11px 17px;border:0;color:#fff;font-weight:700;cursor:pointer}.accept-button{background:#16805d}.trace-button{background:#c53b43}.trace-stage{height:calc(100% - 69px);display:flex;flex-direction:column;background:linear-gradient(180deg,#f9fcff,#edf5fb)}.trace-viewport{flex:1;min-height:0;overflow-x:hidden;overflow-y:hidden}.ecg-track{position:relative;height:470px;margin:auto 0}.ecg-svg{position:absolute;inset:0;height:470px;overflow:visible}.ecg-base,.ecg-progress{fill:none;stroke-linecap:round;stroke-linejoin:round}.ecg-base{stroke:#bfd2e4;stroke-width:3}.ecg-progress{stroke:#2589e5;stroke-width:4;stroke-dasharray:1;filter:drop-shadow(0 0 5px #2589e566)}.runner-core{fill:#fff;stroke:#167ad1;stroke-width:4;filter:drop-shadow(0 0 6px #167ad1)}.runner-aura{fill:#3d9df02b;stroke:#3d9df055;animation:runnerPulse 1s ease-in-out infinite}.ecg-event{position:absolute;top:0;width:94px;height:470px;padding:0;border:0;background:transparent;color:#254665;opacity:0;transform:translateX(-50%) scale(.75);filter:blur(3px);cursor:pointer;transition:.55s cubic-bezier(.16,1,.3,1)}.ecg-event.visible{opacity:1;transform:translateX(-50%) scale(1);filter:none}.event-stem{position:absolute;left:50%;width:2px;background:#4f9be0;transform:translateX(-50%) scaleY(0);transition:transform .45s .1s}.ecg-event.visible .event-stem{transform:translateX(-50%) scaleY(1)}.event-dot{position:absolute;left:50%;display:grid;place-items:center;width:38px;height:38px;border:3px solid #fff;border-radius:50%;background:#247fd2;box-shadow:0 0 0 2px #69a8df;color:#fff;font:700 9px monospace;transform:translateX(-50%)}.ecg-event strong,.ecg-event small{position:absolute;left:0;right:0;display:block;text-align:center;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.ecg-event strong{font-size:11px}.ecg-event small{color:#72889d;font-size:8px}.ecg-event.peak .event-stem{top:58px;height:115px;transform-origin:top}.ecg-event.peak .event-dot{top:170px}.ecg-event.peak strong{top:218px}.ecg-event.peak small{top:237px}.ecg-event.valley .event-stem{top:297px;height:115px;transform-origin:bottom}.ecg-event.valley .event-dot{top:259px}.ecg-event.valley strong{top:225px}.ecg-event.valley small{top:210px}.ecg-event.current .event-dot{animation:eventPop .85s ease}.ecg-event.missing{filter:grayscale(1);opacity:.5}.trace-footer{height:42px;display:flex;align-items:center;gap:12px;padding:0 15px;border-top:1px solid #d4e0ea;color:#6e8296;font-size:9px}.trace-footer i{display:inline-block;width:7px;height:7px;margin-right:5px;border-radius:50%;background:#2589e5}.trace-footer p{margin-left:auto}.insight-rail{overflow:auto}.rail-status{height:51px;display:flex;align-items:center;padding:0 15px;border-bottom:1px solid #dbe3ea;color:#278562;font-size:10px}.rail-status time{margin-left:auto;color:#91a0af}.insight-rail section{padding:15px;border-bottom:1px solid #dbe3ea}.insight-rail h3{margin:0 0 10px;font-size:14px}.rail-conclusion,.muted-copy{margin:0;color:#627589;font-size:10px;line-height:1.7}.rail-heading{display:flex;justify-content:space-between}.rail-heading span{color:#8c9aaa;font-size:10px}.rail-row{display:flex;width:100%;align-items:center;padding:9px 0;border:0;border-bottom:1px solid #e7ecf0;background:#fff;text-align:left;cursor:pointer}.rail-row>i{width:25px;color:#91a1b1;font:700 9px monospace}.rail-row>span{min-width:0;flex:1}.rail-row b,.rail-row small{display:block}.rail-row b{font-size:11px}.rail-row small{margin-top:3px;color:#8191a1;font-size:8px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.rail-row em{font-size:20px;font-style:normal}.rail-check{display:flex;align-items:flex-start;padding:8px 0;border-bottom:1px solid #e7ecf0}.rail-check span{margin-left:5px}.rail-check b,.rail-check small{display:block}.rail-check b{font-size:10px}.rail-check small{margin-top:3px;color:#8998a8;font-size:8px}.rail-primary,.rail-secondary{width:100%;margin-top:10px;padding:9px;border:0;cursor:pointer}.rail-primary{background:#173e63;color:#fff}.rail-secondary{background:#fff;border:1px solid #bdcad5;color:#37526c}.fact-list p{margin:0;padding:9px 0;border-bottom:1px solid #e5ebf0}.fact-list b,.fact-list span{display:block}.fact-list b{font-size:10px}.fact-list span{margin-top:3px;color:#788a9d;font-size:9px;line-height:1.45}.question-list{margin:0;padding-left:20px}.question-list li{padding:5px 0;color:#617589;font-size:10px;line-height:1.45}.route-row{padding:9px 0;border-bottom:1px solid #e5ebf0}.route-row b,.route-row span{display:block}.route-row b{font-size:10px}.route-row span{margin-top:4px;color:#7f90a1;font-size:8px;line-height:1.45}.rail-empty{padding:70px 25px;text-align:center;color:#8897a6}.rail-empty b{font-size:18px}.rail-empty p{font-size:10px;line-height:1.7}.drawer-content dl{display:grid;grid-template-columns:90px 1fr;margin:0}.drawer-content dt,.drawer-content dd{margin:0;padding:10px 0;border-bottom:1px solid #e1e7ec;font-size:11px}.drawer-content dt{color:#8493a3}.drawer-content h4{margin-top:24px}.drawer-content p{padding:9px 10px;background:#f2f6f9;font-size:11px;line-height:1.5}.missing-note{color:#a26c16!important;background:#fff8e8!important}@keyframes scanMove{0%{left:-90px}100%{left:260px}}@keyframes runnerPulse{50%{r:22;opacity:.2}}@keyframes eventPop{45%{transform:translateX(-50%) scale(1.25)}}@media(max-width:1250px){.workbench-grid{grid-template-columns:235px minmax(500px,1fr) 280px}.data-ribbon>div{min-width:100px;padding:0 15px}}@media(max-width:980px){.workbench-grid{grid-template-columns:220px 1fr;height:auto}.insight-rail{grid-column:1/-1;min-height:350px}.main-stage,.case-queue{height:650px}.data-ribbon p{display:none}}
</style>
<style scoped>
:global(.layout-main:has(.after-workbench)){overflow:hidden}
.after-workbench{box-sizing:border-box;height:100%;min-height:570px;display:flex;flex-direction:column;overflow:hidden;padding:12px 18px 14px}
.command-bar{height:68px;flex:0 0 68px}.command-title h1{font-size:21px}.command-title p{font-size:13px}
.data-ribbon{height:56px;flex:0 0 56px}.data-ribbon strong{font-size:21px}.data-ribbon span{font-size:12px}.data-ribbon p{font-size:12px}
.workbench-grid{height:auto;min-height:0;flex:1;margin-top:10px}.case-queue,.main-stage,.insight-rail{height:100%;min-height:0;overflow:hidden}.insight-rail{overflow-y:auto}
.section-head,.stage-head{height:62px}.section-head div span,.stage-head p{font-size:12px}.section-head b{font-size:16px}.stage-head h2{font-size:18px}
.queue-search{padding:8px 12px}.queue-search input{height:34px;font-size:13px}.queue-list{height:calc(100% - 113px)}
.queue-row{padding:10px 12px 10px 17px}.queue-line b{font-size:12px}.queue-line em{font-size:12px;font-weight:600}.queue-row.urgent .queue-line em{color:#d43d43}.queue-row.attention .queue-line em{color:#aa730c}.queue-row.routine .queue-line em{color:#24845f}.queue-main>strong{font-size:14px}.queue-main>small{font-size:12px}.queue-meta{font-size:11px}
.triage-focus{height:calc(100% - 63px);padding:0 46px;display:flex;flex-direction:column;overflow:auto;background:linear-gradient(135deg,#fff 0,#fbfdff 70%,#f1f7fb 100%)}
.verdict-head{display:grid;grid-template-columns:130px 150px 1fr;align-items:center;min-height:70px;border-bottom:1px solid #d9e3eb;border-top:4px solid #8094a8}.verdict-head.urgent{border-top-color:#df3f48}.verdict-head.attention{border-top-color:#d79518}.verdict-head.routine{border-top-color:#26946c}.verdict-state{display:flex;align-items:center;gap:9px;font-size:17px;font-weight:800}.verdict-state i{width:9px;height:9px;border-radius:50%;background:#8094a8;box-shadow:0 0 0 6px #8094a814}.urgent .verdict-state{color:#c92f38}.urgent .verdict-state i{background:#df3f48}.attention .verdict-state{color:#a86f00}.attention .verdict-state i{background:#d79518}.routine .verdict-state{color:#247f5e}.routine .verdict-state i{background:#26946c}.verdict-kind{font-size:14px;font-weight:700}.verdict-case{text-align:right;color:#718498;font-size:12px}
.verdict-copy{flex:1;display:flex;flex-direction:column;justify-content:center;min-height:210px;padding:22px 0;border-bottom:1px solid #d9e3eb}.verdict-copy>span{color:#2782ce;font-size:12px;font-weight:700;letter-spacing:1px}.verdict-copy h3{max-width:900px;margin:18px 0 0;font-size:25px;line-height:1.65;font-weight:650;letter-spacing:.2px}
.diagnosis-split{flex:1;min-height:260px;display:grid;grid-template-columns:minmax(230px,.8fr) 82px minmax(320px,1.35fr);align-items:stretch;padding:24px 0;border-bottom:1px solid #d9e3eb}.customer-voice,.ai-verdict{position:relative;padding:24px 26px;border:1px solid #d7e2eb;background:#fff}.customer-voice:before{content:'“';position:absolute;right:20px;top:4px;color:#d9e7f3;font:700 70px Georgia}.diagnosis-label{position:relative;z-index:1;color:#287fc8;font-size:12px;font-weight:800;letter-spacing:1px}.customer-voice blockquote{position:relative;z-index:1;margin:20px 0 22px;color:#2f465d;font-size:15px;line-height:1.75}.customer-voice footer{padding-top:15px;border-top:1px solid #e2e8ee}.customer-voice footer b,.customer-voice footer span{display:block}.customer-voice footer b{font-size:14px}.customer-voice footer span{margin-top:5px;color:#7a8d9f;font-size:12px}.diagnosis-link{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;color:#8093a6;font-size:10px;text-align:center}.diagnosis-link i{width:1px;height:40px;background:linear-gradient(transparent,#7da9cf)}.diagnosis-link i:last-child{background:linear-gradient(#7da9cf,transparent)}.ai-verdict{border-left:4px solid #287fc8;background:linear-gradient(135deg,#f8fcff,#eef6fc)}.ai-verdict h3{margin:18px 0 0;font-size:21px;line-height:1.65;font-weight:650}.ai-verdict p{margin:18px 0 0;padding-top:16px;border-top:1px solid #d7e2eb;color:#61778c;font-size:13px;line-height:1.65}
.diagnosis-stack{flex:1;min-height:0;display:grid;grid-template-rows:auto minmax(190px,1fr);gap:16px;padding:20px 0;border-bottom:1px solid #d9e3eb}.customer-brief{padding:17px 20px;border-left:4px solid #8ba9c4;background:#f6f9fc}.brief-title{display:flex;align-items:center;gap:14px}.brief-title span{color:#668096;font-size:12px;font-weight:700}.brief-title b{font-size:14px}.customer-brief>p{margin:12px 0;color:#344d64;font-size:14px;line-height:1.65}.brief-meta{display:flex;gap:10px;flex-wrap:wrap}.brief-meta span{padding:4px 9px;background:#e8f0f6;color:#587188;font-size:11px}.ai-judgement{display:flex;flex-direction:column;justify-content:center;padding:22px 28px;border-top:3px solid #287fc8;background:linear-gradient(135deg,#f8fcff,#edf6fc)}.ai-judgement header{display:flex;align-items:center;justify-content:space-between}.ai-judgement header span{color:#247bc4;font-size:13px;font-weight:800;letter-spacing:1px}.ai-judgement header em{color:#70879b;font-size:11px;font-style:normal}.ai-judgement h3{max-width:920px;margin:18px 0 0;font-size:20px;line-height:1.65;font-weight:650}.ai-judgement p{margin:15px 0 0;padding-top:14px;border-top:1px solid #d5e1ea;color:#61778d;font-size:13px;line-height:1.6}
.signal-line{display:grid;grid-template-columns:145px 1fr;align-items:start;padding:22px 0;border-bottom:1px solid #d9e3eb}.signal-line>b{font-size:13px}.signal-line>div{display:flex;flex-wrap:wrap;gap:8px 18px}.signal-line span{position:relative;padding-left:12px;color:#405a73;font-size:13px}.signal-line span:before{content:'';position:absolute;left:0;top:7px;width:4px;height:4px;border-radius:50%;background:#2d86d2}
.verdict-footer{display:flex;align-items:center;gap:18px;padding:24px 0 28px}.verdict-footer.compact{margin-top:22px;border-top:1px solid #dce5ec}.verdict-footer>div:first-child{min-width:0;flex:1}.verdict-footer span,.verdict-footer strong,.verdict-footer small{display:block}.verdict-footer span{color:#8292a3;font-size:12px}.verdict-footer strong{margin-top:7px;font-size:17px}.verdict-footer small{margin-top:5px;color:#667b90;font-size:12px}.verdict-actions{display:flex;gap:10px}.accept-button,.trace-button{min-width:105px;font-size:14px}
.trace-stage{height:calc(100% - 63px)}.trace-viewport{overflow-x:auto;cursor:grab;touch-action:pan-y;scrollbar-width:thin;scrollbar-color:#8faecb #dfebf4;user-select:none}.trace-viewport.dragging{cursor:grabbing}.ecg-event.endpoint .event-stem{display:none}.ecg-event.endpoint .event-dot{top:216px}.ecg-event.endpoint strong{top:264px}.ecg-event.endpoint small{top:285px}.ecg-event.endpoint:first-of-type strong,.ecg-event.endpoint:first-of-type small{text-align:left;padding-left:27px}.ecg-event.endpoint:last-of-type strong,.ecg-event.endpoint:last-of-type small{text-align:right;padding-right:27px}.ecg-event.peak .event-stem{top:58px;height:224px;transform-origin:top}.ecg-event.peak .event-dot{top:282px}.ecg-event.peak strong{top:334px}.ecg-event.peak small{top:355px}.ecg-event.valley .event-stem{top:189px;height:223px;transform-origin:bottom}.ecg-event.valley .event-dot{top:143px}.ecg-event.valley strong{top:111px}.ecg-event.valley small{top:90px}.ecg-event strong{font-size:13px}.ecg-event small{font-size:11px}.event-dot{width:40px;height:40px;font-size:11px}.trace-footer{height:46px;font-size:12px}.trace-footer p{font-size:12px}
.rail-status{height:56px;font-size:12px}.insight-rail section{padding:18px}.insight-rail h3{font-size:16px}.rail-conclusion,.muted-copy{font-size:13px}.rail-heading span{font-size:12px}.rail-row{padding:11px 0}.rail-row>i{width:30px;font-size:11px}.rail-row b{font-size:13px}.rail-row small{font-size:11px}.rail-check b,.fact-list b,.route-row b{font-size:13px}.rail-check small,.fact-list span,.route-row span{font-size:11px}.question-list li{font-size:12px}.rail-empty p{font-size:12px}
@media(max-height:760px){.after-workbench{padding-top:8px}.command-bar{height:60px;flex-basis:60px}.data-ribbon{height:50px;flex-basis:50px}.verdict-copy h3{font-size:21px}.verdict-copy{min-height:170px}.verdict-footer{padding:18px 0}}
@media(max-width:1150px){.ai-judgement h3{font-size:18px}}
@media(max-width:980px){.after-workbench{height:auto;min-height:var(--layout-content-min-h, calc(100vh - 92px));overflow:visible}.workbench-grid{height:680px;flex:none}.insight-rail{height:auto}.triage-focus{padding:0 28px}.diagnosis-stack{grid-template-rows:auto auto}.ai-judgement{min-height:210px}.process-body{grid-template-columns:1fr;overflow:auto}}
</style>
