<template>
  <el-drawer
    v-model="visible"
    title=""
    size="96%"
    class="smart-vision-drawer"
    :close-on-click-modal="false"
    destroy-on-close
    @closed="reset"
  >
    <template #header>
      <div class="sv-head">
        <div class="sv-head__brand">
          <span class="sv-head__icon">🔬</span>
          <div>
            <h2>AI 智能外观检测</h2>
            <p>YOLOv8 分割模型 · 支持最多 10 张照片 · 显示器屏幕划痕识别</p>
          </div>
        </div>
        <div class="sv-head__tags">
          <el-tag effect="dark" type="primary">实时推理</el-tag>
          <el-tag effect="plain">辅助判定 · 不自动改质检结果</el-tag>
        </div>
      </div>
    </template>

    <div class="sv-body">
      <!-- 左侧：上传与说明 -->
      <aside class="sv-side">
        <div class="sv-card sv-card--info">
          <h4>检测对象</h4>
          <dl>
            <dt>质检单号</dt><dd>{{ displayContext.inspectionNo }}</dd>
            <dt>产品</dt><dd>{{ displayContext.materialName }}</dd>
            <dt>批次</dt><dd>{{ displayContext.batchNo }}</dd>
            <dt>序列号</dt><dd>{{ displayContext.serialNo }}</dd>
          </dl>
          <p v-if="usingMockContext" class="sv-mock-tag">演示数据 · 进入具体质检单后自动关联</p>
        </div>

        <div class="sv-card sv-card--upload">
          <h4>上传检测图片 <small>{{ items.length }}/10</small></h4>
          <el-upload
            ref="uploadRef"
            class="sv-upload"
            drag
            multiple
            :auto-upload="false"
            :limit="10"
            accept="image/*"
            :show-file-list="false"
            :on-change="onFileChange"
            :on-exceed="onExceed"
          >
            <div class="sv-upload__inner">
              <div class="sv-upload__ring">
                <span>📷</span>
              </div>
              <p>拖入或点击选择屏幕照片</p>
              <small>JPG / PNG · 最多 10 张 · 可多选</small>
            </div>
          </el-upload>

          <div v-if="items.length" class="sv-thumbs">
            <div
              v-for="(it, idx) in items"
              :key="it.id"
              class="sv-thumb"
              :class="{ active: activeIndex === idx, defect: it.result?.defect, done: it.status === 'done', err: it.status === 'error' }"
              @click="selectItem(idx)"
            >
              <img :src="it.previewUrl" :alt="it.name" />
              <button type="button" class="sv-thumb__del" title="移除" @click.stop="removeItem(idx)">×</button>
              <em>{{ idx + 1 }}</em>
            </div>
          </div>

          <el-button
            type="primary"
            size="large"
            class="sv-detect-btn"
            :disabled="!items.length"
            :loading="detecting"
            @click="detect"
          >
            {{ detectButtonText }}
          </el-button>
        </div>

        <div class="sv-card sv-card--tips">
          <h4>检测能力</h4>
          <ul>
            <li><b>屏幕划痕</b> — 表面线性 / 点状损伤</li>
            <li><b>缺陷定位</b> — 分割掩膜 + 检测框</li>
            <li><b>多图汇总</b> — 网格排列 + AI 报告</li>
          </ul>
          <p class="sv-tip-note">结论需质检员人工确认，可一键写入检测备注或导出报告。</p>
        </div>
      </aside>

      <!-- 中间：图像区 -->
      <main class="sv-main">
        <!-- 多图网格 -->
        <template v-if="isMulti && !detailMode">
          <div class="sv-view-tabs">
            <button type="button" class="active">结果网格</button>
            <span class="sv-view-hint">点击卡片可查看单图详情</span>
          </div>
          <div class="sv-grid-wrap" :class="{ scanning: detecting }">
            <div v-if="detecting" class="sv-scan">
              <div class="sv-scan__line" />
              <span>YOLO 推理中 · {{ detectProgressText }}</span>
            </div>
            <div v-if="!hasAnyResult && !detecting" class="sv-placeholder sv-placeholder--grid">
              <span>🤖</span>
              <p>上传多张照片后点击「开始 YOLO 检测」</p>
            </div>
            <div v-else class="sv-grid">
              <button
                v-for="(it, idx) in items"
                :key="it.id"
                type="button"
                class="sv-grid-card"
                @click="openDetail(idx)"
              >
                <div class="sv-grid-card__imgs">
                  <img :src="it.previewUrl" alt="原图" />
                  <img v-if="it.result?.resultImage" :src="it.result.resultImage" alt="标注" />
                  <div v-else class="sv-grid-card__empty">
                    {{ it.status === 'error' ? '失败' : it.status === 'running' ? '检测中…' : '待检测' }}
                  </div>
                </div>
                <div class="sv-grid-card__meta">
                  <strong>{{ it.name }}</strong>
                  <el-tag
                    v-if="it.result"
                    :type="it.result.defect ? 'danger' : 'success'"
                    size="small"
                  >{{ it.result.defect ? `缺陷 ${it.result.count}` : '正常' }}</el-tag>
                  <el-tag v-else-if="it.status === 'error'" type="info" size="small">失败</el-tag>
                </div>
              </button>
            </div>
          </div>
        </template>

        <!-- 单图 / 详情模式 -->
        <template v-else>
          <div class="sv-view-tabs">
            <button
              v-if="isMulti"
              type="button"
              class="sv-back"
              @click="detailMode = false"
            >← 返回网格</button>
            <button
              v-for="tab in viewTabs"
              :key="tab.key"
              type="button"
              :class="{ active: activeTab === tab.key }"
              @click="activeTab = tab.key"
            >
              {{ tab.label }}
            </button>
            <span v-if="activeItem" class="sv-view-hint">{{ activeItem.name }}</span>
          </div>

          <div class="sv-canvas" :class="{ scanning: detecting && !isMulti }">
            <div v-if="detecting && !isMulti" class="sv-scan">
              <div class="sv-scan__line" />
              <span>YOLO 推理中 · 特征提取 & 缺陷分割…</span>
            </div>

            <div v-if="activeTab === 'compare' && previewUrl && result?.resultImage" class="sv-compare">
              <div class="sv-compare__pane">
                <label>原始图片</label>
                <img :src="previewUrl" alt="原始" />
              </div>
              <div class="sv-compare__divider" />
              <div class="sv-compare__pane">
                <label>AI 标注结果</label>
                <img :src="result.resultImage" alt="标注" />
              </div>
            </div>

            <div v-else-if="activeTab === 'original'" class="sv-single">
              <img v-if="previewUrl" :src="previewUrl" alt="原始图片" />
              <div v-else class="sv-placeholder">
                <span>🖼️</span>
                <p>上传图片后将在此预览</p>
              </div>
            </div>

            <div v-else class="sv-single">
              <img v-if="result?.resultImage" :src="result.resultImage" alt="检测结果" />
              <div v-else class="sv-placeholder">
                <span>🤖</span>
                <p>点击「开始 YOLO 检测」生成标注图</p>
              </div>
            </div>
          </div>
        </template>
      </main>

      <!-- 右侧：结果面板 -->
      <aside class="sv-result">
        <div
          class="sv-verdict"
          :class="verdictClass"
        >
          <div class="sv-verdict__icon">{{ verdictIcon }}</div>
          <div>
            <strong>{{ verdictTitle }}</strong>
            <p>{{ verdictSub }}</p>
          </div>
        </div>

        <!-- 多图汇总 KPI -->
        <div v-if="isMulti && hasAnyResult" class="sv-kpi-grid">
          <div class="sv-kpi"><b>{{ batchStats.totalImages }}</b><span>检测图片</span></div>
          <div class="sv-kpi"><b>{{ batchStats.defectImages }}</b><span>缺陷图</span></div>
          <div class="sv-kpi"><b>{{ batchStats.defectCount }}</b><span>缺陷区域</span></div>
          <div class="sv-kpi"><b>{{ batchStats.maxConfidenceText }}</b><span>最高置信度</span></div>
        </div>

        <!-- 单图 KPI -->
        <div v-else-if="result" class="sv-kpi-grid">
          <div class="sv-kpi">
            <b>{{ result.defectType || '—' }}</b>
            <span>缺陷类型</span>
          </div>
          <div class="sv-kpi">
            <b>{{ result.count ?? 0 }}</b>
            <span>缺陷区域</span>
          </div>
          <div class="sv-kpi">
            <b>{{ confidenceText }}</b>
            <span>最高置信度</span>
          </div>
          <div class="sv-kpi sv-kpi--model" :title="result.model || 'YOLOv8'">
            <b>{{ modelShortName }}</b>
            <span>检测模型</span>
          </div>
        </div>

        <div v-if="result && (!isMulti || detailMode)" class="sv-card sv-card--summary">
          <h4>检测结论</h4>
          <p>{{ result.summary }}</p>
          <el-progress
            v-if="result.maxConfidence"
            :percentage="Math.round((result.maxConfidence || 0) * 100)"
            :color="result.defect ? '#f56c6c' : '#67c23a'"
            :stroke-width="10"
          />
        </div>

        <div v-if="tableRows.length && (!isMulti || detailMode)" class="sv-card sv-card--table">
          <h4>缺陷明细（{{ tableRows.length }}）</h4>
          <ul class="sv-defect-list">
            <li v-for="(row, idx) in tableRows" :key="idx" class="sv-defect-item">
              <div class="sv-defect-item__hd">
                <span class="sv-defect-item__no">#{{ idx + 1 }}</span>
                <el-tag :type="row.tagType" size="small">{{ row.label }}</el-tag>
                <strong>{{ row.confidenceText }}</strong>
              </div>
              <div class="sv-defect-item__box">
                <span>检测框</span>
                <code>{{ row.boxText }}</code>
              </div>
            </li>
          </ul>
        </div>

        <div v-else-if="result && !result.defect && (!isMulti || detailMode)" class="sv-card sv-card--pass">
          <span>✓</span>
          <p>未检出表面划痕，建议结合人工目视与其他工序综合判定。</p>
        </div>

        <!-- AI 报告区 -->
        <div v-if="hasAnyResult" class="sv-card sv-card--report">
          <h4>
            AI 外观检测报告
            <el-tag v-if="aiReport?.aiGenerated" type="success" size="small">千问</el-tag>
            <el-tag v-else-if="aiReport" type="info" size="small">模板</el-tag>
          </h4>
          <div class="sv-report-actions">
            <el-button
              type="primary"
              size="small"
              :loading="reportLoading"
              @click="generateReport"
            >
              {{ aiReport ? '重新生成' : 'AI 一键生成报告' }}
            </el-button>
            <el-button
              type="warning"
              size="small"
              :loading="pdfLoading"
              :disabled="!aiReport"
              @click="exportPdf"
            >
              导出 PDF
            </el-button>
          </div>
          <div v-if="aiReport" class="sv-report-body">
            <template v-if="aiSections.length">
              <div v-for="sec in aiSections" :key="sec.key" class="sv-report-sec">
                <strong>【{{ sec.key }}】</strong>
                <p>{{ sec.text }}</p>
              </div>
            </template>
            <p v-else>{{ aiReport.fullText || aiReport.summary }}</p>
          </div>
          <p v-else class="sv-tip-note">检测完成后可一键生成报告并导出 PDF。</p>
        </div>
      </aside>
    </div>

    <template #footer>
      <div class="sv-footer">
        <el-button @click="visible = false">关闭</el-button>
        <el-button
          v-if="hasAnyResult && batchStats.defectImages > 0"
          type="warning"
          @click="applyToRemark(false)"
        >
          写入备注 · 标记不合格
        </el-button>
        <el-button
          v-if="hasAnyResult && batchStats.defectImages === 0"
          type="success"
          plain
          @click="applyToRemark(true)"
        >
          写入备注 · 外观正常
        </el-button>
        <el-button type="primary" :disabled="!items.length" :loading="detecting" @click="detect">
          重新检测
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { detectAppearance, generateVisionReport } from '@/api/quality'
import { exportVisionReportPdf } from '@/utils/visionReportPdfExport'

const MAX_FILES = 10

const MOCK_PRODUCTS = [
  '27寸 IPS 办公显示器',
  '32寸 4K 电竞显示器',
  '24寸 1080P 商用屏',
  '34寸 曲面带鱼屏',
  '23.8寸 窄边框显示器'
]

function genMockContext() {
  const suffix = String(Math.floor(Math.random() * 9000) + 1000)
  const now = new Date()
  const ym = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}`
  const batchSeq = String(Math.floor(Math.random() * 900) + 100)
  const batchNo = `BATCH-FP-${ym}-${batchSeq}`
  return {
    inspectionNo: `QC-AV-${suffix}`,
    materialName: MOCK_PRODUCTS[Math.floor(Math.random() * MOCK_PRODUCTS.length)],
    batchNo,
    serialNo: `SN-${batchNo.slice(-8)}-${String(Math.floor(Math.random() * 99) + 1).padStart(2, '0')}`
  }
}

let idSeq = 0
function nextId() {
  return `img-${Date.now()}-${++idSeq}`
}

const props = defineProps({
  inspection: { type: Object, default: null },
  unitLabel: { type: String, default: '' }
})
const emit = defineEmits(['applied'])

const visible = defineModel({ type: Boolean, default: false })

const items = ref([])
const activeIndex = ref(0)
const detailMode = ref(false)
const detecting = ref(false)
const detectCursor = ref(0)
const activeTab = ref('compare')
const mockContext = ref(null)
const aiReport = ref(null)
const reportLoading = ref(false)
const pdfLoading = ref(false)
const uploadRef = ref(null)

const usingMockContext = computed(() => !props.inspection?.inspectionNo)

const displayContext = computed(() => {
  const mock = mockContext.value || {}
  return {
    inspectionNo: props.inspection?.inspectionNo || mock.inspectionNo || '—',
    materialName: props.inspection?.materialName || mock.materialName || '—',
    batchNo: props.inspection?.batchNo || mock.batchNo || '—',
    serialNo: props.unitLabel || mock.serialNo || '—'
  }
})

const isMulti = computed(() => items.value.length > 1)
const activeItem = computed(() => items.value[activeIndex.value] || null)
const previewUrl = computed(() => activeItem.value?.previewUrl || '')
const result = computed(() => activeItem.value?.result || null)
const hasAnyResult = computed(() => items.value.some((it) => it.result))

const batchStats = computed(() => {
  const list = items.value.filter((it) => it.result)
  const totalImages = list.length
  let defectImages = 0
  let defectCount = 0
  let maxConfidence = 0
  for (const it of list) {
    if (it.result.defect) {
      defectImages++
      defectCount += Number(it.result.count) || 0
    }
    maxConfidence = Math.max(maxConfidence, Number(it.result.maxConfidence) || 0)
  }
  return {
    totalImages,
    defectImages,
    normalImages: Math.max(0, totalImages - defectImages),
    defectCount,
    maxConfidence,
    maxConfidenceText: maxConfidence ? `${Math.round(maxConfidence * 100)}%` : '—',
    passRate: totalImages > 0 ? Math.round(((totalImages - defectImages) * 1000) / totalImages) / 10 : 100,
    verdict: defectImages > 0 ? '发现外观缺陷' : totalImages ? '外观检测正常' : '等待检测'
  }
})

const modelShortName = computed(() => {
  const m = result.value?.model || 'YOLOv8'
  if (m.length <= 14) return m
  return m.replace(/\([^)]+\)/, '').trim() || 'YOLOv8-Seg'
})

const detectButtonText = computed(() => {
  if (!detecting.value) return items.value.length > 1 ? `开始 YOLO 检测（${items.value.length} 张）` : '开始 YOLO 检测'
  return `AI 分析中… ${detectCursor.value}/${items.value.length}`
})

const detectProgressText = computed(() => `${detectCursor.value}/${items.value.length}`)

watch(visible, (open) => {
  if (open && !props.inspection?.inspectionNo) {
    mockContext.value = genMockContext()
  }
})

const viewTabs = [
  { key: 'compare', label: '对比视图' },
  { key: 'original', label: '原始图' },
  { key: 'result', label: '标注图' }
]

const DEFECT_LABEL = {
  Scratched: '屏幕划痕',
  scratched: '屏幕划痕',
  SCRATCH: '屏幕划痕'
}

const confidenceText = computed(() =>
  result.value?.maxConfidence ? `${Math.round(result.value.maxConfidence * 100)}%` : '—'
)

const verdictClass = computed(() => {
  if (!hasAnyResult.value) return 'is-idle'
  return batchStats.value.defectImages > 0 ? 'is-defect' : 'is-ok'
})

const verdictIcon = computed(() => {
  if (!hasAnyResult.value) return '⏳'
  return batchStats.value.defectImages > 0 ? '⚠️' : '✅'
})

const verdictTitle = computed(() => {
  if (!hasAnyResult.value) return '等待检测'
  if (isMulti.value && !detailMode.value) return batchStats.value.verdict
  return result.value?.defect ? '发现外观缺陷' : '外观检测正常'
})

const verdictSub = computed(() => {
  if (!hasAnyResult.value) return '上传 1~10 张屏幕照片并启动 YOLO 推理'
  if (isMulti.value && !detailMode.value) {
    return `共 ${batchStats.value.totalImages} 张 · 缺陷图 ${batchStats.value.defectImages} · 缺陷区域 ${batchStats.value.defectCount}`
  }
  return result.value?.summary || ''
})

const tableRows = computed(() =>
  (result.value?.detections || []).map((row) => ({
    label: DEFECT_LABEL[row.className] || row.className || '屏幕划痕',
    tagType: 'danger',
    confidenceText: `${Math.round((row.confidence || 0) * 100)}%`,
    boxText: (row.box || []).map((v) => Number(v).toFixed(0)).join(', ')
  }))
)

const aiSections = computed(() => {
  const sections = aiReport.value?.sections
  if (!sections || typeof sections !== 'object') return []
  return Object.entries(sections).map(([key, text]) => ({ key, text }))
})

function onExceed() {
  ElMessage.warning(`最多上传 ${MAX_FILES} 张图片`)
}

function onFileChange(uploadFile) {
  const raw = uploadFile.raw
  if (!raw || !raw.type?.startsWith('image/')) return
  if (items.value.length >= MAX_FILES) {
    ElMessage.warning(`最多上传 ${MAX_FILES} 张图片`)
    return
  }
  // 避免同一选择事件里重复追加（el-upload multiple 会逐个触发）
  const dup = items.value.some((it) => it.file === raw || (it.name === raw.name && it.file?.size === raw.size))
  if (dup) return

  items.value.push({
    id: nextId(),
    file: raw,
    name: uploadFile.name || raw.name || `图片${items.value.length + 1}`,
    previewUrl: URL.createObjectURL(raw),
    result: null,
    status: 'pending',
    error: ''
  })
  activeIndex.value = items.value.length - 1
  aiReport.value = null
  if (items.value.length === 1) {
    detailMode.value = false
    activeTab.value = 'original'
  } else {
    detailMode.value = false
  }
}

function removeItem(idx) {
  const it = items.value[idx]
  if (it?.previewUrl) URL.revokeObjectURL(it.previewUrl)
  items.value.splice(idx, 1)
  aiReport.value = null
  uploadRef.value?.clearFiles?.()
  if (!items.value.length) {
    activeIndex.value = 0
    detailMode.value = false
    return
  }
  if (activeIndex.value >= items.value.length) activeIndex.value = items.value.length - 1
  if (items.value.length === 1) detailMode.value = false
}

function selectItem(idx) {
  activeIndex.value = idx
  if (isMulti.value) detailMode.value = true
  activeTab.value = items.value[idx]?.result ? 'compare' : 'original'
}

function openDetail(idx) {
  selectItem(idx)
}

function buildSingleSummary(data, passed) {
  if (passed) return 'AI智能检测：未发现屏幕表面划痕，外观正常'
  if (!data?.defect) return 'AI智能检测：未发现屏幕表面划痕'
  const lines = (data.detections || []).map((d, i) => {
    const label = DEFECT_LABEL[d.className] || d.className || '屏幕划痕'
    return `${i + 1}. ${label} 置信度 ${Math.round((d.confidence || 0) * 100)}%`
  })
  return `AI智能检测：发现 ${data.count} 处${data.defectType || '屏幕划痕'}\n${lines.join('\n')}`
}

function buildBatchSummary(passed) {
  const stats = batchStats.value
  if (passed || stats.defectImages === 0) {
    return `AI智能检测（${stats.totalImages} 张）：未发现屏幕表面划痕，外观正常`
  }
  const lines = items.value
    .filter((it) => it.result?.defect)
    .map((it, i) => `${i + 1}. ${it.name}：${it.result.count} 处${it.result.defectType || '缺陷'}（置信度 ${Math.round((it.result.maxConfidence || 0) * 100)}%）`)
  return `AI智能检测（${stats.totalImages} 张）：缺陷图 ${stats.defectImages} 张，缺陷区域合计 ${stats.defectCount} 处\n${lines.join('\n')}`
}

async function detect() {
  if (!items.value.length) return
  detecting.value = true
  aiReport.value = null
  detectCursor.value = 0
  if (items.value.length === 1) {
    activeTab.value = 'compare'
    detailMode.value = false
  } else {
    detailMode.value = false
  }

  let ok = 0
  let fail = 0
  let defectTotal = 0

  for (let i = 0; i < items.value.length; i++) {
    detectCursor.value = i + 1
    const it = items.value[i]
    it.status = 'running'
    it.error = ''
    try {
      const data = await detectAppearance(it.file)
      it.result = data
      it.status = 'done'
      ok++
      if (data.defect) defectTotal += data.count || 0
    } catch (error) {
      it.status = 'error'
      it.error = error?.message || '检测失败'
      it.result = null
      fail++
    }
  }

  detecting.value = false

  if (ok === 0) {
    const msg = items.value.find((it) => it.error)?.error || ''
    if (msg.includes('YOLO') || msg.includes('8000') || msg.includes('Network')) {
      ElMessage.error('检测服务未就绪：请先在 Mobile-Phone-Defect 目录运行 start-yolo.bat')
    } else {
      ElMessage.error(msg || 'AI 检测失败')
    }
    return
  }

  if (fail > 0) {
    ElMessage.warning(`完成 ${ok} 张，失败 ${fail} 张${defectTotal ? `，共检出 ${defectTotal} 处缺陷` : ''}`)
  } else if (defectTotal > 0) {
    ElMessage.success(`检测到 ${defectTotal} 处缺陷（${ok} 张）`)
  } else {
    ElMessage.success(ok > 1 ? `${ok} 张均未检测到表面划痕` : '未检测到表面划痕')
  }
}

function applyToRemark(passed) {
  if (!hasAnyResult.value) return
  const summary = isMulti.value
    ? buildBatchSummary(passed)
    : buildSingleSummary(result.value, passed)
  const defect = !passed && batchStats.value.defectImages > 0
  emit('applied', {
    summary,
    defect,
    passed,
    result: isMulti.value
      ? {
          defect,
          count: batchStats.value.defectCount,
          maxConfidence: batchStats.value.maxConfidence,
          summary,
          batch: true,
          images: items.value.filter((it) => it.result).map((it) => ({
            name: it.name,
            ...it.result
          }))
        }
      : result.value
  })
  ElMessage.success('已写入当前工序检测备注')
  visible.value = false
}

function buildReportPayload() {
  return {
    context: displayContext.value,
    images: items.value
      .filter((it) => it.result)
      .map((it) => ({
        name: it.name,
        defect: !!it.result.defect,
        count: it.result.count ?? 0,
        maxConfidence: it.result.maxConfidence ?? 0,
        summary: it.result.summary || '',
        defectType: it.result.defectType || '',
        detections: it.result.detections || []
      }))
  }
}

async function generateReport() {
  if (!hasAnyResult.value) return
  reportLoading.value = true
  try {
    aiReport.value = await generateVisionReport(buildReportPayload())
    ElMessage.success(aiReport.value?.aiGenerated ? 'AI 外观检测报告已生成' : '已生成模板报告（千问暂不可用）')
  } catch (e) {
    ElMessage.error(e?.message || '生成报告失败')
  } finally {
    reportLoading.value = false
  }
}

async function exportPdf() {
  if (!aiReport.value && !hasAnyResult.value) return
  pdfLoading.value = true
  try {
    if (!aiReport.value) await generateReport()
    const images = items.value
      .filter((it) => it.result)
      .map((it) => ({
        name: it.name,
        defect: !!it.result.defect,
        count: it.result.count ?? 0,
        maxConfidence: it.result.maxConfidence ?? 0,
        summary: it.result.summary || '',
        resultImage: it.result.resultImage || ''
      }))
    await exportVisionReportPdf({
      context: displayContext.value,
      stats: {
        ...batchStats.value,
        maxConfidence: batchStats.value.maxConfidence
      },
      images,
      report: aiReport.value
    })
    ElMessage.success('外观检测报告 PDF 已导出')
  } catch (e) {
    ElMessage.error(e?.message || '导出 PDF 失败')
  } finally {
    pdfLoading.value = false
  }
}

function reset() {
  for (const it of items.value) {
    if (it.previewUrl) URL.revokeObjectURL(it.previewUrl)
  }
  items.value = []
  activeIndex.value = 0
  detailMode.value = false
  detecting.value = false
  detectCursor.value = 0
  activeTab.value = 'compare'
  mockContext.value = null
  aiReport.value = null
  reportLoading.value = false
  pdfLoading.value = false
  uploadRef.value?.clearFiles?.()
}
</script>

<style scoped>
.smart-vision-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 16px 20px;
  border-bottom: 1px solid #e8edf3;
  background: linear-gradient(135deg, #f0f7ff 0%, #faf5ff 100%);
}

.smart-vision-drawer :deep(.el-drawer__body) {
  padding: 0;
  background: #f4f6f9;
  overflow: hidden;
}

.smart-vision-drawer :deep(.el-drawer__footer) {
  border-top: 1px solid #e8edf3;
  padding: 12px 20px;
  background: #fff;
}

.sv-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  width: 100%;
}

.sv-head__brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.sv-head__icon {
  font-size: 36px;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.15);
}

.sv-head__brand h2 {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 800;
  color: #1a1a2e;
}

.sv-head__brand p {
  margin: 0;
  font-size: 13px;
  color: #7b8798;
}

.sv-head__tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.sv-body {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) minmax(300px, 360px);
  gap: 0;
  min-height: calc(100vh - 124px);
  max-height: var(--layout-content-min-h, calc(100vh - 92px));
}

.sv-side {
  padding: 14px;
  background: #fff;
  border-right: 1px solid #e8edf3;
  overflow-y: auto;
  min-width: 0;
}

.sv-main {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
  overflow: hidden;
}

.sv-result {
  padding: 14px;
  background: #fff;
  border-left: 1px solid #e8edf3;
  overflow-y: auto;
  overflow-x: hidden;
  min-width: 0;
  max-height: var(--layout-content-min-h, calc(100vh - 92px));
}

.sv-card {
  background: #f8fafc;
  border: 1px solid #e8edf3;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 12px;
}

.sv-card h4 {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 700;
  color: #3d4654;
}

.sv-card h4 small {
  margin-left: 6px;
  font-weight: 500;
  color: #9aa5b5;
}

.sv-card--info dl {
  margin: 0;
  display: grid;
  gap: 8px;
}

.sv-card--info dt {
  font-size: 11px;
  color: #9aa5b5;
}

.sv-card--info dd {
  margin: 0 0 4px;
  font-size: 13px;
  font-weight: 600;
  color: #2c3340;
  word-break: break-all;
}

.sv-mock-tag {
  margin: 10px 0 0;
  font-size: 11px;
  color: #909399;
  padding: 4px 8px;
  background: #f0f2f5;
  border-radius: 6px;
}

.sv-upload :deep(.el-upload-dragger) {
  padding: 20px 12px;
  border-radius: 10px;
  border: 1px dashed #b8c9e0;
  background: #fbfdff;
}

.sv-upload__inner {
  text-align: center;
}

.sv-upload__ring {
  width: 64px;
  height: 64px;
  margin: 0 auto 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #7c3aed);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.sv-upload__inner p {
  margin: 0;
  font-size: 13px;
  color: #5c6678;
}

.sv-upload__inner small {
  display: block;
  margin-top: 6px;
  font-size: 11px;
  color: #9aa5b5;
}

.sv-thumbs {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
  margin-top: 10px;
}

.sv-thumb {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  background: #0d1117;
}

.sv-thumb.active {
  border-color: #409eff;
}

.sv-thumb.defect {
  box-shadow: inset 0 0 0 1px #f56c6c;
}

.sv-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sv-thumb__del {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
}

.sv-thumb em {
  position: absolute;
  left: 3px;
  bottom: 2px;
  font-style: normal;
  font-size: 10px;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  padding: 0 4px;
  border-radius: 4px;
}

.sv-detect-btn {
  width: 100%;
  margin-top: 12px;
}

.sv-card--tips ul {
  margin: 0;
  padding-left: 18px;
  font-size: 12px;
  color: #5c6678;
  line-height: 1.7;
}

.sv-tip-note {
  margin: 10px 0 0;
  font-size: 11px;
  color: #9aa5b5;
}

.sv-view-tabs {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.sv-view-tabs button {
  padding: 8px 16px;
  border: 1px solid #dce3ec;
  border-radius: 8px;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
  color: #606266;
}

.sv-view-tabs button.active {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
  font-weight: 600;
}

.sv-view-tabs .sv-back {
  background: #f4f6f8;
}

.sv-view-hint {
  font-size: 12px;
  color: #9aa5b5;
  margin-left: 4px;
}

.sv-canvas,
.sv-grid-wrap {
  flex: 1;
  position: relative;
  min-height: 420px;
  background: #0d1117;
  border-radius: 12px;
  overflow: auto;
  border: 1px solid #2d333b;
}

.sv-canvas.scanning::after,
.sv-grid-wrap.scanning::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(64, 158, 255, 0.06);
  pointer-events: none;
}

.sv-scan {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(13, 17, 23, 0.55);
  color: #fff;
  font-size: 14px;
  gap: 16px;
}

.sv-scan__line {
  width: 80%;
  height: 3px;
  background: linear-gradient(90deg, transparent, #409eff, #7c3aed, transparent);
  animation: sv-scan-move 1.6s ease-in-out infinite;
}

@keyframes sv-scan-move {
  0%, 100% { transform: translateY(-120px); opacity: 0.3; }
  50% { transform: translateY(120px); opacity: 1; }
}

.sv-compare {
  display: grid;
  grid-template-columns: 1fr 2px 1fr;
  height: 100%;
  min-height: 420px;
}

.sv-compare__pane {
  display: flex;
  flex-direction: column;
  padding: 12px;
}

.sv-compare__pane label {
  font-size: 12px;
  color: #8b949e;
  margin-bottom: 8px;
}

.sv-compare__pane img {
  flex: 1;
  width: 100%;
  object-fit: contain;
  border-radius: 8px;
}

.sv-compare__divider {
  background: #30363d;
}

.sv-single {
  height: 100%;
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.sv-single img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 8px;
}

.sv-placeholder {
  text-align: center;
  color: #8b949e;
}

.sv-placeholder span {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
}

.sv-placeholder--grid {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.sv-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
  padding: 14px;
}

.sv-grid-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  border: 1px solid #30363d;
  border-radius: 10px;
  background: #161b22;
  cursor: pointer;
  text-align: left;
  color: #e6edf3;
}

.sv-grid-card:hover {
  border-color: #409eff;
}

.sv-grid-card__imgs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  min-height: 100px;
}

.sv-grid-card__imgs img,
.sv-grid-card__empty {
  width: 100%;
  height: 100px;
  object-fit: contain;
  background: #0d1117;
  border-radius: 6px;
}

.sv-grid-card__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #8b949e;
}

.sv-grid-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: space-between;
}

.sv-grid-card__meta strong {
  font-size: 12px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.sv-verdict {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 14px;
}

.sv-verdict.is-idle {
  background: #f4f6f8;
  border: 1px solid #e8edf3;
}

.sv-verdict.is-ok {
  background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  border: 1px solid #6ee7b7;
}

.sv-verdict.is-defect {
  background: linear-gradient(135deg, #fff1f2, #ffe4e6);
  border: 1px solid #fda4af;
}

.sv-verdict__icon {
  font-size: 32px;
}

.sv-verdict strong {
  display: block;
  font-size: 16px;
  color: #1a1a2e;
}

.sv-verdict p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #606266;
  word-break: break-word;
  line-height: 1.5;
}

.sv-card--summary p {
  margin: 0 0 10px;
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
}

.sv-kpi-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 14px;
}

.sv-kpi {
  text-align: center;
  padding: 12px 8px;
  background: #f8fafc;
  border: 1px solid #e8edf3;
  border-radius: 10px;
}

.sv-kpi b {
  display: block;
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  word-break: break-word;
  line-height: 1.3;
}

.sv-kpi--model b {
  font-size: 12px;
}

.sv-kpi span {
  font-size: 11px;
  color: #9aa5b5;
}

.sv-defect-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sv-defect-item {
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 8px;
}

.sv-defect-item__hd {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.sv-defect-item__no {
  font-size: 12px;
  font-weight: 700;
  color: #909399;
}

.sv-defect-item__hd strong {
  margin-left: auto;
  font-size: 13px;
  color: #f56c6c;
}

.sv-defect-item__box {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 11px;
}

.sv-defect-item__box span {
  color: #9aa5b5;
}

.sv-defect-item__box code {
  font-family: ui-monospace, Consolas, monospace;
  font-size: 11px;
  color: #606266;
  background: #f4f6f8;
  padding: 6px 8px;
  border-radius: 6px;
  word-break: break-all;
  line-height: 1.5;
}

.sv-card--pass {
  text-align: center;
  padding: 24px 16px;
}

.sv-card--pass span {
  font-size: 36px;
  color: #67c23a;
}

.sv-card--pass p {
  margin: 10px 0 0;
  font-size: 13px;
  color: #606266;
}

.sv-card--report .sv-report-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.sv-report-body {
  max-height: 280px;
  overflow-y: auto;
  font-size: 13px;
  color: #4b5563;
  line-height: 1.65;
}

.sv-report-sec {
  margin-bottom: 10px;
}

.sv-report-sec strong {
  display: block;
  margin-bottom: 4px;
  color: #1f2937;
  font-size: 12px;
}

.sv-report-sec p {
  margin: 0;
  white-space: pre-wrap;
}

.sv-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 1200px) {
  .sv-body {
    grid-template-columns: 1fr;
  }
  .sv-side, .sv-result {
    border: none;
    border-bottom: 1px solid #e8edf3;
  }
}
</style>
