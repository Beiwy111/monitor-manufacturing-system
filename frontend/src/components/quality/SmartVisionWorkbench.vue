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
            <p>YOLOv8 分割模型 · 显示器屏幕划痕 / 表面缺陷识别</p>
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
          <h4>上传检测图片</h4>
          <el-upload
            class="sv-upload"
            drag
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :show-file-list="false"
            :on-change="onFileChange"
          >
            <div class="sv-upload__inner">
              <div class="sv-upload__ring">
                <span>📷</span>
              </div>
              <p>拖入或点击选择屏幕照片</p>
              <small>JPG / PNG · 建议正面清晰拍摄</small>
              <em v-if="fileName">{{ fileName }}</em>
            </div>
          </el-upload>
          <el-button
            type="primary"
            size="large"
            class="sv-detect-btn"
            :disabled="!file"
            :loading="detecting"
            @click="detect"
          >
            {{ detecting ? 'AI 分析中…' : '开始 YOLO 检测' }}
          </el-button>
        </div>

        <div class="sv-card sv-card--tips">
          <h4>检测能力</h4>
          <ul>
            <li><b>屏幕划痕</b> — 表面线性 / 点状损伤</li>
            <li><b>缺陷定位</b> — 分割掩膜 + 检测框</li>
            <li><b>置信度评分</b> — 逐区域量化输出</li>
          </ul>
          <p class="sv-tip-note">结论需质检员人工确认，可一键写入检测备注。</p>
        </div>

        <div v-if="history.length" class="sv-card sv-card--history">
          <h4>本次会话记录</h4>
          <button
            v-for="(h, i) in history"
            :key="i"
            type="button"
            class="sv-history-row"
            @click="restoreHistory(h)"
          >
            <span :class="h.defect ? 'bad' : 'ok'">{{ h.defect ? '缺陷' : '正常' }}</span>
            <small>{{ h.time }}</small>
            <em>{{ h.summary }}</em>
          </button>
        </div>
      </aside>

      <!-- 中间：图像对比 -->
      <main class="sv-main">
        <div class="sv-view-tabs">
          <button
            v-for="tab in viewTabs"
            :key="tab.key"
            type="button"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="sv-canvas" :class="{ scanning: detecting }">
          <div v-if="detecting" class="sv-scan">
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
      </main>

      <!-- 右侧：结果面板 -->
      <aside class="sv-result">
        <div
          class="sv-verdict"
          :class="result ? (result.defect ? 'is-defect' : 'is-ok') : 'is-idle'"
        >
          <div class="sv-verdict__icon">{{ verdictIcon }}</div>
          <div>
            <strong>{{ verdictTitle }}</strong>
            <p>{{ verdictSub }}</p>
          </div>
        </div>

        <div v-if="result" class="sv-kpi-grid">
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

        <div v-if="result" class="sv-card sv-card--summary">
          <h4>检测结论</h4>
          <p>{{ result.summary }}</p>
          <el-progress
            v-if="result.maxConfidence"
            :percentage="Math.round((result.maxConfidence || 0) * 100)"
            :color="result.defect ? '#f56c6c' : '#67c23a'"
            :stroke-width="10"
          />
        </div>

        <div v-if="tableRows.length" class="sv-card sv-card--table">
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

        <div v-else-if="result && !result.defect" class="sv-card sv-card--pass">
          <span>✓</span>
          <p>未检出表面划痕，建议结合人工目视与其他工序综合判定。</p>
        </div>
      </aside>
    </div>

    <template #footer>
      <div class="sv-footer">
        <el-button @click="visible = false">关闭</el-button>
        <el-button v-if="result?.defect" type="warning" @click="applyToRemark(false)">
          写入备注 · 标记不合格
        </el-button>
        <el-button v-if="result && !result.defect" type="success" plain @click="applyToRemark(true)">
          写入备注 · 外观正常
        </el-button>
        <el-button type="primary" :disabled="!file" :loading="detecting" @click="detect">
          重新检测
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { detectAppearance } from '@/api/quality'

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

const props = defineProps({
  inspection: { type: Object, default: null },
  unitLabel: { type: String, default: '' }
})
const emit = defineEmits(['applied'])

const visible = defineModel({ type: Boolean, default: false })

const file = ref(null)
const fileName = ref('')
const previewUrl = ref('')
const detecting = ref(false)
const result = ref(null)
const activeTab = ref('compare')
const history = ref([])
const mockContext = ref(null)

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

const modelShortName = computed(() => {
  const m = result.value?.model || 'YOLOv8'
  if (m.length <= 14) return m
  return m.replace(/\([^)]+\)/, '').trim() || 'YOLOv8-Seg'
})

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

const verdictIcon = computed(() => {
  if (!result.value) return '⏳'
  return result.value.defect ? '⚠️' : '✅'
})

const verdictTitle = computed(() => {
  if (!result.value) return '等待检测'
  return result.value.defect ? '发现外观缺陷' : '外观检测正常'
})

const verdictSub = computed(() => {
  if (!result.value) return '上传屏幕照片并启动 YOLO 推理'
  return result.value.summary || ''
})

const tableRows = computed(() =>
  (result.value?.detections || []).map((row) => ({
    label: DEFECT_LABEL[row.className] || row.className || '屏幕划痕',
    tagType: 'danger',
    confidenceText: `${Math.round((row.confidence || 0) * 100)}%`,
    boxText: (row.box || []).map((v) => Number(v).toFixed(0)).join(', ')
  }))
)

function onFileChange(uploadFile) {
  resetFile()
  file.value = uploadFile.raw
  fileName.value = uploadFile.name || uploadFile.raw?.name || '已选择图片'
  previewUrl.value = URL.createObjectURL(uploadFile.raw)
  result.value = null
  activeTab.value = 'original'
}

function resetFile() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  file.value = null
  fileName.value = ''
  previewUrl.value = ''
  result.value = null
}

function buildSummary(data, passed) {
  if (passed) return 'AI智能检测：未发现屏幕表面划痕，外观正常'
  if (!data?.defect) return 'AI智能检测：未发现屏幕表面划痕'
  const lines = (data.detections || []).map((d, i) => {
    const label = DEFECT_LABEL[d.className] || d.className || '屏幕划痕'
    return `${i + 1}. ${label} 置信度 ${Math.round((d.confidence || 0) * 100)}%`
  })
  return `AI智能检测：发现 ${data.count} 处${data.defectType || '屏幕划痕'}\n${lines.join('\n')}`
}

function pushHistory(data) {
  history.value.unshift({
    defect: data.defect,
    summary: data.summary || (data.defect ? `发现 ${data.count} 处缺陷` : '未检出缺陷'),
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
    data: { ...data }
  })
  if (history.value.length > 5) history.value.pop()
}

function restoreHistory(h) {
  result.value = h.data
  activeTab.value = 'compare'
}

async function detect() {
  if (!file.value) return
  detecting.value = true
  activeTab.value = 'compare'
  try {
    result.value = await detectAppearance(file.value)
    pushHistory(result.value)
    ElMessage.success(result.value.defect ? `检测到 ${result.value.count} 处缺陷` : '未检测到表面划痕')
  } catch (error) {
    const msg = error?.message || ''
    if (msg.includes('YOLO') || msg.includes('8000') || msg.includes('Network')) {
      ElMessage.error('检测服务未就绪：请先在 Mobile-Phone-Defect 目录运行 start-yolo.bat')
    } else {
      ElMessage.error(msg || 'AI 检测失败')
    }
  } finally {
    detecting.value = false
  }
}

function applyToRemark(passed) {
  if (!result.value) return
  emit('applied', {
    summary: buildSummary(result.value, passed),
    defect: !passed && result.value.defect,
    passed,
    result: result.value
  })
  ElMessage.success('已写入当前工序检测备注')
  visible.value = false
}

function reset() {
  resetFile()
  history.value = []
  activeTab.value = 'compare'
  mockContext.value = null
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
  grid-template-columns: 248px minmax(0, 1fr) minmax(300px, 360px);
  gap: 0;
  min-height: calc(100vh - 180px);
  max-height: calc(100vh - 140px);
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
  max-height: calc(100vh - 140px);
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

.sv-upload__inner em {
  display: block;
  margin-top: 8px;
  font-style: normal;
  font-size: 12px;
  color: #409eff;
  font-weight: 600;
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

.sv-history-row {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 4px 8px;
  width: 100%;
  text-align: left;
  padding: 8px 10px;
  margin-bottom: 6px;
  border: 1px solid #e8edf3;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}

.sv-history-row span.ok { color: #67c23a; font-weight: 700; font-size: 12px; }
.sv-history-row span.bad { color: #f56c6c; font-weight: 700; font-size: 12px; }
.sv-history-row small { font-size: 11px; color: #9aa5b5; }
.sv-history-row em { grid-column: 1 / -1; font-style: normal; font-size: 11px; color: #606266; }

.sv-view-tabs {
  display: flex;
  gap: 8px;
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

.sv-canvas {
  flex: 1;
  position: relative;
  min-height: 420px;
  background: #0d1117;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #2d333b;
}

.sv-canvas.scanning::after {
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

.sv-kpi-grid {
  margin: 0 0 10px;
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
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

.sv-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
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
