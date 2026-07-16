<template>
  <div class="qc5">
    <div class="qc5__header">
      <div class="qc5__header-main">
        <h3 class="qc5__title">显示器专业五步检测</h3>
        <p class="qc5__desc">按行业终检标准：坏点 → 色域 → 漏光 → 亮度均匀性 → 屏闪。下方表格默认全部合格，仅需标记异常项。</p>
      </div>
      <div class="qc5__header-actions">
        <el-button type="warning" plain @click="$emit('open-smart-vision')">
          🔬 AI 智能外观检测
        </el-button>
      </div>
      <div class="qc5__progress">
        <span>总进度</span>
        <el-progress :percentage="overallPct" :stroke-width="14" :color="progressColor" />
        <em>合格 {{ passCount }} / 共 {{ units.length }} 台</em>
      </div>
    </div>

    <!-- 本批次产品 -->
    <div class="qc5__units">
      <span class="qc5__units-label">本批次产品（逐台检测）</span>
      <div class="qc5__unit-chips">
        <button
          v-for="(u, idx) in units"
          :key="u.serialNo"
          type="button"
          class="qc5__unit-chip"
          :class="{
            'is-active': focusUnitIndex === idx,
            'is-done': unitOverallStatus(idx) === 'PASS',
            'is-fail': unitOverallStatus(idx) === 'FAIL'
          }"
          @click="selectUnit(idx)"
        >
          <span class="qc5__unit-no">#{{ u.unitNo }}</span>
          <span class="qc5__unit-sn">{{ u.serialNo }}</span>
          <el-tag v-if="unitOverallStatus(idx) === 'PASS'" type="success" size="small" effect="plain">合格</el-tag>
          <el-tag v-else-if="unitOverallStatus(idx) === 'FAIL'" type="danger" size="small" effect="plain">不合格</el-tag>
        </button>
      </div>
    </div>

    <!-- 五大工序（保留原图卡片） -->
    <div class="qc5__stations">
      <div
        v-for="s in stations"
        :key="s.id"
        class="qc5__station"
        :class="{
          'is-active': activeStationId === s.id,
          'is-done': stationFailCount(s.id) === 0,
          'is-partial': stationFailCount(s.id) > 0
        }"
        @click="selectStation(s.id)"
      >
        <div class="qc5__station-img">
          <img :src="s.image" :alt="s.title" />
          <span class="qc5__station-order">{{ s.order }}</span>
        </div>
        <div class="qc5__station-body">
          <h4>{{ s.title }}</h4>
          <p>{{ s.subtitle }}</p>
          <div class="qc5__station-meta">
            <el-tag size="small" :type="stationTagType(s.id)" effect="plain">
              {{ stationPassCount(s.id) }}/{{ units.length }} 台合格
            </el-tag>
            <span v-if="stationFailCount(s.id) > 0" class="qc5__cur-result bad">
              异常 {{ stationFailCount(s.id) }} 台
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 下方：表格质检操作区 -->
    <div class="qc5__inspect-section">
      <div class="qc5__inspect-section-hd">
        <h4>质检录入</h4>
        <span>默认全部合格，点击单元格切换；勾选多台后可批量标记不合格</span>
      </div>

      <div v-if="selectedRows.length" class="qc5__batch-bar">
        <span>已勾选 <strong>{{ selectedRows.length }}</strong> 台</span>
        <span class="qc5__batch-label">批量标记检测项为不合格：</span>
        <el-button
          v-for="s in stations"
          :key="s.id"
          size="small"
          plain
          class="qc5__batch-btn"
          @click="openBatchFailDialog(s.id)"
        >{{ s.title }}</el-button>
      </div>

      <el-table
        ref="tableRef"
        :data="matrixRows"
        border
        size="small"
        class="qc5__matrix"
        :row-class-name="rowClassName"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="42" fixed />
        <el-table-column prop="serialNo" label="序列号" width="148" fixed show-overflow-tooltip />
        <el-table-column
          v-for="s in stations"
          :key="s.id"
          :label="s.title"
          min-width="108"
          align="center"
          :class-name="activeStationId === s.id ? 'qc5__col--active' : ''"
        >
          <template #default="{ row }">
            <button
              type="button"
              class="qc5__cell"
              :class="cellClass(row, s.id)"
              @click="toggleCell(row._idx, s.id)"
            >{{ cellLabel(row, s.id) }}</button>
          </template>
        </el-table-column>
        <el-table-column label="综合" width="88" align="center" fixed="right">
          <template #default="{ row }">
            <span class="qc5__overall" :class="overallClass(row)">{{ overallLabel(row) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="qc5__actions">
        <el-button :loading="saving" @click="handleSave">保存</el-button>
        <el-button :loading="saving" :disabled="!hasNextUnit" @click="handleSaveAndNext">保存并下一台</el-button>
        <el-button type="primary" :loading="proceeding" @click="handleProceed">下一步：统计与报告</el-button>
      </div>
    </div>

    <el-dialog
      v-model="failDialogVisible"
      title="标记不合格"
      width="420px"
      :close-on-click-modal="false"
      append-to-body
    >
      <p v-if="failDialogContext" class="qc5__fail-hint">
        {{ failDialogContext.serialNo }} · {{ failDialogContext.stationTitle }}
        <template v-if="failDialogContext.batch">（等 {{ failDialogContext.batch }} 台）</template>
      </p>
      <el-form :model="failForm" label-width="80px" size="default">
        <el-form-item label="缺陷类型" required>
          <el-select v-model="failForm.defectType" style="width:100%">
            <el-option v-for="t in DEFECT_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="缺陷等级" required>
          <el-select v-model="failForm.defectLevel" style="width:100%">
            <el-option v-for="l in DEFECT_LEVELS" :key="l.value" :label="l.label" :value="l.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="缺陷位置">
          <el-input v-model="failForm.defectLocation" placeholder="如：左上角、边框右侧" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="failForm.remark" type="textarea" :rows="2" placeholder="补充说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="failDialogVisible = false">取消</el-button>
        <el-button type="danger" plain @click="confirmFail">确认不合格</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FP_STATIONS, buildUnitList, parseMatrixFromItems, storageKeyForMatrix } from '@/config/fpInspectionStations'

const props = defineProps({
  inspection: { type: Object, required: true },
  sampleQuantity: { type: Number, default: 1 },
  initialItems: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false },
  proceeding: { type: Boolean, default: false }
})

const emit = defineEmits(['save', 'proceed', 'open-smart-vision', 'complete'])

const stations = FP_STATIONS
const DEFECT_TYPES = ['外观缺陷', '色差', '坏点', '漏光', 'PCB不良', '其他']
const DEFECT_LEVELS = [
  { label: '轻微', value: 'MINOR' },
  { label: '一般', value: 'GENERAL' },
  { label: '严重', value: 'MAJOR' }
]

const units = ref([])
const unitRecords = reactive({})
const selectedRows = ref([])
const focusUnitIndex = ref(0)
const activeStationId = ref(stations[0]?.id || 'deadPixel')
const tableRef = ref(null)

const failDialogVisible = ref(false)
const failDialogContext = ref(null)
const failForm = reactive({
  defectType: '外观缺陷',
  defectLevel: 'GENERAL',
  defectLocation: '',
  remark: ''
})

stations.forEach((s) => { unitRecords[s.id] = [] })

function defaultRecord() {
  return { passed: true, saved: true, measuredValue: '合格' }
}

function loadFromStorage() {
  const id = props.inspection?.inspectionId
  if (!id) return null
  try {
    const raw = localStorage.getItem(storageKeyForMatrix(id))
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function persistToStorage() {
  const id = props.inspection?.inspectionId
  if (!id) return
  const data = {
    units: units.value,
    records: stations.reduce((acc, s) => {
      acc[s.id] = unitRecords[s.id].map((r) => ({ ...r }))
      return acc
    }, {})
  }
  localStorage.setItem(storageKeyForMatrix(id), JSON.stringify(data))
}

let initializedKey = ''

function initUnits() {
  const key = `${props.inspection?.inspectionId || ''}:${props.sampleQuantity}`
  if (initializedKey === key && units.value.length) return

  const fromStorage = loadFromStorage()
  const fromItems = fromStorage ? null : parseMatrixFromItems(
    props.initialItems,
    stations,
    props.inspection?.batchNo,
    props.sampleQuantity
  )
  const restored = fromStorage || fromItems

  units.value = restored?.units?.length
    ? restored.units
    : buildUnitList(props.sampleQuantity, props.inspection?.batchNo)

  stations.forEach((s) => {
    unitRecords[s.id] = units.value.map((_, idx) => {
      const saved = restored?.records?.[s.id]?.[idx]
      if (saved) {
        return {
          passed: saved.passed !== false,
          saved: true,
          measuredValue: saved.measuredValue || (saved.passed === false ? '不合格' : '合格'),
          remark: saved.remark || '',
          defectType: saved.defectType || '',
          defectLevel: saved.defectLevel || '',
          defectLocation: saved.defectLocation || ''
        }
      }
      return defaultRecord()
    })
  })
  focusUnitIndex.value = 0
  activeStationId.value = stations[0]?.id || 'deadPixel'
  selectedRows.value = []
  initializedKey = key
}

watch(
  () => [props.sampleQuantity, props.inspection?.inspectionId],
  initUnits,
  { immediate: true }
)

function ensureRecord(stationId, unitIdx) {
  if (!unitRecords[stationId][unitIdx]) {
    unitRecords[stationId][unitIdx] = defaultRecord()
  }
  return unitRecords[stationId][unitIdx]
}

function isCellPass(unitIdx, stationId) {
  const rec = unitRecords[stationId]?.[unitIdx]
  return rec?.passed !== false
}

const matrixRows = computed(() =>
  units.value.map((u, idx) => {
    const row = { serialNo: u.serialNo, unitNo: u.unitNo, _idx: idx }
    let anyFail = false
    stations.forEach((s) => {
      const pass = isCellPass(idx, s.id)
      row[s.id] = pass
      if (!pass) anyFail = true
    })
    row.overall = anyFail ? 'FAIL' : 'PASS'
    return row
  })
)

const passCount = computed(() => matrixRows.value.filter((r) => r.overall === 'PASS').length)
const failCount = computed(() => matrixRows.value.filter((r) => r.overall === 'FAIL').length)
const allDone = computed(() => units.value.length > 0)
const hasNextUnit = computed(() => focusUnitIndex.value < units.value.length - 1)

const overallPct = computed(() => {
  if (!units.value.length) return 0
  return Math.round((passCount.value / units.value.length) * 100)
})
const progressColor = computed(() => {
  const p = overallPct.value
  if (p >= 100) return '#67c23a'
  if (p >= 80) return '#409eff'
  return '#e6a23c'
})

function unitOverallStatus(unitIdx) {
  const row = matrixRows.value[unitIdx]
  if (!row) return 'PASS'
  return row.overall === 'FAIL' ? 'FAIL' : 'PASS'
}

function stationPassCount(stationId) {
  return unitRecords[stationId].filter((r) => r.passed !== false).length
}

function stationFailCount(stationId) {
  return unitRecords[stationId].filter((r) => r.passed === false).length
}

function stationTagType(stationId) {
  const fails = stationFailCount(stationId)
  if (fails === 0) return 'success'
  return 'warning'
}

function selectUnit(idx) {
  focusUnitIndex.value = idx
  scrollToUnit(idx)
}

function selectStation(id) {
  activeStationId.value = id
}

function cellLabel(row, stationId) {
  return row[stationId] ? '合格' : '不合格'
}

function cellClass(row, stationId) {
  return row[stationId] ? 'is-pass' : 'is-fail'
}

function overallLabel(row) {
  return row.overall === 'PASS' ? '合格' : '不合格'
}

function overallClass(row) {
  return row.overall === 'PASS' ? 'is-pass' : 'is-fail'
}

function rowClassName({ row }) {
  return row._idx === focusUnitIndex.value ? 'qc5__row--focus' : ''
}

function onSelectionChange(rows) {
  selectedRows.value = rows || []
}

function resetFailForm() {
  Object.assign(failForm, {
    defectType: '外观缺陷',
    defectLevel: 'GENERAL',
    defectLocation: '',
    remark: ''
  })
}

function openFailDialog(unitIndices, stationId, batchCount = 0) {
  const station = stations.find((s) => s.id === stationId)
  const firstIdx = unitIndices[0]
  failDialogContext.value = {
    unitIndices,
    stationId,
    stationTitle: station?.title || '',
    serialNo: units.value[firstIdx]?.serialNo || '',
    batch: batchCount
  }
  const existing = ensureRecord(stationId, firstIdx)
  if (existing.passed === false) {
    Object.assign(failForm, {
      defectType: existing.defectType || '外观缺陷',
      defectLevel: existing.defectLevel || 'GENERAL',
      defectLocation: existing.defectLocation || '',
      remark: existing.remark || ''
    })
  } else {
    resetFailForm()
  }
  failDialogVisible.value = true
}

function openBatchFailDialog(stationId) {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先勾选需要批量标记的产品')
    return
  }
  const indices = selectedRows.value.map((r) => r._idx)
  openFailDialog(indices, stationId, indices.length)
}

function toggleCell(unitIdx, stationId) {
  if (isCellPass(unitIdx, stationId)) {
    openFailDialog([unitIdx], stationId)
  } else {
    const rec = ensureRecord(stationId, unitIdx)
    rec.passed = true
    rec.saved = true
    rec.measuredValue = '合格'
    rec.remark = ''
    rec.defectType = ''
    rec.defectLevel = ''
    rec.defectLocation = ''
    persistToStorage()
  }
}

function confirmFail() {
  const ctx = failDialogContext.value
  if (!ctx) return
  const parts = [failForm.defectType, failForm.defectLocation, failForm.remark].filter(Boolean)
  const remark = parts.join(' · ')
  ctx.unitIndices.forEach((idx) => {
    const rec = ensureRecord(ctx.stationId, idx)
    rec.passed = false
    rec.saved = true
    rec.measuredValue = '不合格'
    rec.defectType = failForm.defectType
    rec.defectLevel = failForm.defectLevel
    rec.defectLocation = failForm.defectLocation
    rec.remark = remark
  })
  failDialogVisible.value = false
  persistToStorage()
  ElMessage.success(ctx.unitIndices.length > 1 ? `已批量标记 ${ctx.unitIndices.length} 台` : '已标记不合格')
}

function scrollToUnit(idx) {
  focusUnitIndex.value = idx
  nextTick(() => {
    const el = tableRef.value?.$el?.querySelector('.qc5__row--focus')
    el?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  })
}

function getInspectionSummary() {
  const matrix = matrixRows.value
  const passUnits = matrix.filter((r) => r.overall === 'PASS').length
  const failUnits = matrix.filter((r) => r.overall === 'FAIL').length
  const sampleQty = Number(props.sampleQuantity) || units.value.length
  return {
    units: units.value,
    matrix,
    stationRecords: stations.map((s) => ({
      station: s.title,
      stationId: s.id,
      records: unitRecords[s.id].map((r, i) => ({
        serialNo: units.value[i]?.serialNo,
        passed: r.passed !== false,
        saved: true,
        measuredValue: r.measuredValue || (r.passed === false ? '不合格' : '合格'),
        remark: r.remark || '',
        defectType: r.defectType || '',
        defectLevel: r.defectLevel || '',
        defectLocation: r.defectLocation || ''
      }))
    })),
    passUnits,
    failUnits,
    sampleQuantity: sampleQty,
    qualifiedQuantity: passUnits,
    unqualifiedQuantity: failUnits
  }
}

async function handleSave() {
  persistToStorage()
  emit('save', getInspectionSummary())
}

async function handleSaveAndNext() {
  persistToStorage()
  emit('save', getInspectionSummary())
  if (hasNextUnit.value) {
    scrollToUnit(focusUnitIndex.value + 1)
  }
}

async function handleProceed() {
  const sum = getInspectionSummary()
  const failUnits = sum.failUnits || 0
  const msg = failUnits > 0
    ? `本批共抽检 ${sum.sampleQuantity} 台，其中 ${failUnits} 台不合格。将进入统计与 AI 报告页，由您最终判定本批是否合格。`
    : `本批共抽检 ${sum.sampleQuantity} 台全部合格。将进入统计与 AI 报告页，由您最终判定本批是否合格。`
  try {
    await ElMessageBox.confirm(msg, '进入统计与报告', {
      type: 'info',
      confirmButtonText: '下一步',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  persistToStorage()
  emit('proceed', sum)
}

function applyVisionRemark(payload) {
  const stationId = activeStationId.value || stations[0]?.id || 'deadPixel'
  const rec = ensureRecord(stationId, focusUnitIndex.value)
  const prefix = rec.remark ? `${rec.remark}\n` : ''
  rec.remark = prefix + (payload?.summary || '')
  if (payload?.defect) {
    rec.passed = false
    rec.measuredValue = 'AI检测：外观不合格'
    rec.defectType = '外观缺陷'
    rec.defectLevel = 'MAJOR'
  }
  persistToStorage()
  ElMessage.success('AI 检测结论已写入当前产品')
}

const currentUnitLabel = computed(() => units.value[focusUnitIndex.value]?.serialNo || '')

defineExpose({
  getInspectionSummary,
  allDone,
  matrixRows,
  applyVisionRemark,
  currentUnitLabel,
  persistToStorage
})
</script>

<style scoped>
.qc5 {
  background: #fafbfc;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px 14px;
}

.qc5__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.qc5__progress {
  min-width: 220px;
  text-align: right;
  font-size: 13px;
  color: #909399;
}

.qc5__progress em {
  font-style: normal;
  font-weight: 600;
  color: #303133;
}

.qc5__units {
  margin-bottom: 12px;
}

.qc5__units-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.qc5__unit-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.qc5__unit-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: all .2s;
}

.qc5__unit-chip:hover { border-color: #409eff; }
.qc5__unit-chip.is-active { border-color: #409eff; box-shadow: 0 0 0 2px rgba(64,158,255,.2); }
.qc5__unit-chip.is-done { border-color: #b7ebc6; background: #f0f9eb; }
.qc5__unit-chip.is-fail { border-color: #f5c4c4; background: #fef0f0; }

.qc5__unit-no {
  font-weight: 700;
  color: #409eff;
}

.qc5__unit-sn {
  font-size: 12px;
  color: #606266;
}

.qc5__stations {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

@media (max-width: 1400px) {
  .qc5__stations { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 900px) {
  .qc5__stations { grid-template-columns: repeat(2, 1fr); }
}

.qc5__station {
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  background: #fff;
  transition: transform .2s, border-color .2s, box-shadow .2s;
}

.qc5__station:hover {
  transform: translateY(-2px);
  border-color: #409eff;
}

.qc5__station.is-active {
  border-color: #409eff;
  box-shadow: 0 8px 24px rgba(64, 158, 255, .25);
}

.qc5__station.is-done { border-color: #b7ebc6; }
.qc5__station.is-partial { border-color: #e6a23c; }

.qc5__station-img {
  position: relative;
  height: 200px;
  background: #0d1117;
  overflow: hidden;
}

.qc5__station-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.qc5__station-order {
  position: absolute;
  top: 10px;
  left: 10px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(64, 158, 255, .9);
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qc5__station-body {
  padding: 14px;
}

.qc5__station-body h4 {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}

.qc5__station-body p {
  margin: 0 0 10px;
  font-size: 12px;
  line-height: 1.5;
  color: #909399;
  min-height: 36px;
}

.qc5__station-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.qc5__cur-result { font-size: 12px; font-weight: 600; }
.qc5__cur-result.bad { color: #cf6b6b; }

.qc5__inspect-section {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
}

.qc5__inspect-section-hd {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.qc5__inspect-section-hd h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #303133;
}

.qc5__inspect-section-hd span {
  font-size: 12px;
  color: #909399;
}

.qc5__matrix :deep(.qc5__col--active) {
  background: #f5f9ff;
}

.qc5__title {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
}

.qc5__desc {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.qc5__batch-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
  padding: 8px 12px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
  font-size: 13px;
}

.qc5__batch-label {
  color: #8c6d1f;
}

.qc5__batch-btn {
  --el-button-hover-text-color: #cf6b6b;
  --el-button-hover-border-color: #f5c4c4;
  --el-button-hover-bg-color: #fdeeed;
}

.qc5__matrix {
  margin-bottom: 14px;
}

.qc5__matrix :deep(.qc5__row--focus) {
  background: #f0f7ff !important;
}

.qc5__cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  padding: 4px 10px;
  border: 1px solid transparent;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: opacity .15s, box-shadow .15s;
  background: transparent;
}

.qc5__cell:hover {
  opacity: 0.85;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, .06);
}

.qc5__cell.is-pass {
  background: #e8f8ef;
  color: #389e6b;
  border-color: #b7ebc6;
}

.qc5__cell.is-fail {
  background: #fdeeed;
  color: #cf6b6b;
  border-color: #f5c4c4;
}

.qc5__overall {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.qc5__overall.is-pass {
  background: #e8f8ef;
  color: #389e6b;
}

.qc5__overall.is-fail {
  background: #fdeeed;
  color: #cf6b6b;
}

.qc5__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  padding-top: 4px;
}

.qc5__fail-hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: #606266;
}
</style>
