<template>
  <div class="qc-flow-page">
    <!-- 顶部步骤条（紧凑） -->
    <div class="qc-flow-page__steps" :class="{ 'qc-flow-page__steps--inline': flowStep === 2 }">
      <el-steps :active="flowStep" finish-status="success" simple>
        <el-step title="选择分类" />
        <el-step title="选择单据" />
        <el-step title="执行质检" />
        <el-step title="质检报告" />
      </el-steps>
    </div>

    <!-- Step 0：分类选择 -->
    <section v-if="flowStep === 0" class="qc-flow-page__section">
      <h2 class="qc-flow-page__heading">请选择本次质检分类</h2>
      <p class="qc-flow-page__sub">先确定检验对象类型，再选择对应待检单据进入专业检测流程</p>
      <div class="qc-cat-grid">
        <button type="button" class="qc-cat-card qc-cat-card--fp" @click="pickCategory('FINISHED_PRODUCT')">
          <div class="qc-cat-card__icon">📺</div>
          <h3>成品质检</h3>
          <p>显示器终检 · 五步专业仿真检测<br>坏点 / 色域 / 漏光 / 均匀性 / 屏闪</p>
          <el-tag type="primary">待检 {{ pendingKpi.finishedProduct }} 单</el-tag>
        </button>
        <button type="button" class="qc-cat-card qc-cat-card--mat" @click="pickCategory('RAW_MATERIAL')">
          <div class="qc-cat-card__icon">📦</div>
          <h3>物料质检</h3>
          <p>来料检验 · 按行业标准检测项<br>外观 / 尺寸 / 电气性能等</p>
          <el-tag>待检 {{ pendingKpi.rawMaterial }} 单</el-tag>
        </button>
      </div>
    </section>

    <!-- Step 1：单据选择 -->
    <section v-else-if="flowStep === 1" class="qc-flow-page__section">
      <div class="qc-flow-page__bar">
        <el-button link type="primary" @click="backToCategory">← 重选分类</el-button>
        <h2 class="qc-flow-page__heading">{{ categoryLabel }} · 选择待检单据</h2>
        <div class="qc-flow-page__bar-actions">
          <el-input v-model="keyword" placeholder="质检单号 / 批次 / 产品" clearable size="default" style="width:220px" />
          <el-button v-if="isMaterial" type="primary" @click="openIncomingDialog">登记来料检</el-button>
          <el-button :loading="loading" @click="loadData">刷新</el-button>
        </div>
      </div>

      <div class="qc-order-grid">
        <div
          v-for="row in orderList"
          :key="row.inspectionId"
          class="qc-order-card"
          :class="{ 'is-selected': selected?.inspectionId === row.inspectionId }"
          @click="selectOrder(row)"
        >
          <div class="qc-order-card__hd">
            <strong>{{ row.inspectionNo }}</strong>
            <el-tag :type="statusTagType(row.inspectionStatus)" size="small">{{ row.inspectionStatusCn }}</el-tag>
          </div>
          <p>{{ row.materialName || '—' }}</p>
          <p class="qc-order-card__meta">批次 {{ row.batchNo }} · 送检 {{ row.sampleQuantity }} 件</p>
          <p v-if="row.workOrderNo" class="qc-order-card__meta">工单 {{ row.workOrderNo }}</p>
        </div>
      </div>
      <el-empty v-if="!orderList.length" description="暂无待检单据，请刷新或登记来料检" />

      <div v-if="selected" class="qc-flow-page__footer">
        <el-button type="primary" size="large" @click="goInspect">开始质检 →</el-button>
      </div>
    </section>

    <!-- Step 2：执行质检 -->
    <section v-else-if="flowStep === 2" class="qc-flow-page__section qc-flow-page__section--inspect">
      <div class="qc-flow-page__bar">
        <el-button link type="primary" @click="flowStep = 1">← 重选单据</el-button>
        <div class="qc-order-summary">
          <el-tag>{{ selected?.inspectionNo }}</el-tag>
          <span>{{ selected?.materialName }}</span>
          <span>批次 {{ selected?.batchNo }}</span>
          <el-tag :type="statusTagType(selected?.inspectionStatus)" size="small">{{ selected?.inspectionStatusCn }}</el-tag>
        </div>
        <el-button type="primary" :disabled="!canGoReport" @click="finishInspect">查看报告（可选）→</el-button>
      </div>

      <div v-if="isFinished" class="qc-flow-page__sample">
        <span>本批检测数量</span>
        <el-input-number v-model="sampleQuantity" :min="1" :max="999" />
        <el-button link type="primary" @click="applyAqlSample">按 AQL 建议</el-button>
      </div>

      <QcFiveStepWorkbench
        v-if="isFinished"
        ref="fiveStepRef"
        :inspection="selected"
        :sample-quantity="sampleQuantity"
        @complete="finishInspect"
      />

      <QcMaterialInspect
        v-else
        ref="materialRef"
        :inspection="selected"
        :items="items"
        @update:items="items = $event"
        @update:sampling="samplingData = $event"
      />

      <div class="qc-flow-page__sync-bar">
        <el-button v-if="isFinished" type="primary" :loading="saving" @click="syncFinishedToBackend">
          同步检测结果到质检单
        </el-button>
        <el-button v-else type="primary" :loading="saving" @click="syncMaterialToBackend">
          保存物料检测数据
        </el-button>
        <el-button type="success" :disabled="!canGoReport" @click="finishInspect">进入报告页（可选）→</el-button>
        <template v-if="canOperate && canGoReport">
          <el-button type="success" :loading="acting" @click="doPass">直接质检通过</el-button>
          <el-button type="danger" @click="openFailDialog">质检不通过</el-button>
        </template>
      </div>
    </section>

    <!-- Step 3：质检报告 -->
    <section v-else-if="flowStep === 3" class="qc-flow-page__section">
      <div class="qc-flow-page__bar">
        <el-button link type="primary" @click="flowStep = 2">← 返回检测</el-button>
        <h2 class="qc-flow-page__heading">质检报告</h2>
        <div class="qc-flow-page__bar-actions">
          <el-button v-if="canOperate" type="success" :loading="acting" @click="doPass">质检通过</el-button>
          <el-button v-if="canOperate" type="danger" @click="openFailDialog">质检不通过</el-button>
        </div>
      </div>

      <QualityReportPanel
        :detail="reportDetail"
        :items="reportItemsForPanel"
        :unit-matrix="unitMatrixExport"
        optional-ai
      />

      <p v-if="canOperate" class="qc-flow-page__pass-hint">无需生成 AI 报告，确认检测结果后可直接点击「质检通过」。</p>

      <div v-if="canOperate" class="qc-flow-page__remark">
        <el-input v-model="actionRemark" type="textarea" :rows="2" placeholder="质检备注（可选）" />
      </div>

      <div v-if="canOperate" class="qc-flow-page__decide-bar">
        <el-button type="success" size="large" :loading="acting" @click="doPass">质检通过</el-button>
        <el-button type="danger" size="large" @click="openFailDialog">质检不通过</el-button>
      </div>
      <p v-else-if="isFinalized" class="qc-flow-page__pass-hint">该质检单已完成判定，如需修改请从质检记录查看。</p>
    </section>

    <!-- 来料登记 -->
    <el-dialog v-model="incomingDialog" title="登记来料检验" width="480px" :close-on-click-modal="false">
      <el-form :model="incomingForm" label-width="90px" size="default">
        <el-form-item label="原材料" required>
          <el-select v-model="incomingForm.materialId" filterable placeholder="选择原材料" style="width:100%">
            <el-option v-for="m in rawMaterials" :key="m.materialId"
              :label="`${m.materialCode} · ${m.materialName}`" :value="m.materialId" />
          </el-select>
        </el-form-item>
        <el-form-item label="来料批次" required>
          <el-input v-model="incomingForm.batchNo" placeholder="如 BATCH-LCD-202607" />
        </el-form-item>
        <el-form-item label="到货数量">
          <el-input-number v-model="incomingForm.lotQuantity" :min="1" style="width:140px" />
        </el-form-item>
        <el-form-item label="抽检数量">
          <el-input-number v-model="incomingForm.sampleQuantity" :min="1" :max="incomingForm.lotQuantity" style="width:140px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="incomingDialog = false">取消</el-button>
        <el-button type="primary" :loading="incomingSaving" @click="doCreateIncoming">确认登记</el-button>
      </template>
    </el-dialog>

    <!-- 不通过 -->
    <el-dialog v-model="failDialog" title="质检不通过" width="480px" :close-on-click-modal="false">
      <el-form :model="failForm" label-width="90px" size="default">
        <el-form-item label="不良原因" required>
          <el-input v-model="failForm.defectReason" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="不良数量" required>
          <el-input-number v-model="failForm.defectQuantity" :min="1" />
        </el-form-item>
        <el-form-item label="缺陷类型">
          <el-select v-model="failForm.defectType" style="width:100%">
            <el-option v-for="t in DEFECT_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="failDialog = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="doFail">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { moduleStatusType } from '@/constants/moduleStatus'
import {
  fetchInspectionViews, fetchQualityKpi, fetchInspectionDetail,
  fetchInspectionItems, generateDefaultItems, saveInspectionItems,
  evaluateInspection, passInspection, failInspection,
  createIncomingInspection, updateInspectionSampling
} from '@/api/quality'
import request from '@/utils/request'
import { FP_STATIONS } from '@/config/fpInspectionStations'
import QcFiveStepWorkbench from '@/components/quality/QcFiveStepWorkbench.vue'
import QcMaterialInspect from '@/components/quality/QcMaterialInspect.vue'
import QualityReportPanel from '@/components/quality/QualityReportPanel.vue'

const route = useRoute()
const userStore = useUserStore()

const flowStep = ref(0)
const category = ref('')
const list = ref([])
const kpi = ref({})
const selected = ref(null)
const detail = ref(null)
const items = ref([])
const keyword = ref('')
const loading = ref(false)
const saving = ref(false)
const acting = ref(false)
const sampleQuantity = ref(5)
const samplingData = ref(null)
const actionRemark = ref('')

const fiveStepRef = ref(null)
const materialRef = ref(null)
/** 五步检测完成后的报告快照（离开检测页后组件卸载，靠此保持数据一致） */
const fpReportSnapshot = ref(null)
const reportItems = ref([])

const materialMap = ref({})
const rawMaterials = ref([])
const incomingDialog = ref(false)
const incomingSaving = ref(false)
const failDialog = ref(false)
const incomingForm = reactive({ materialId: null, batchNo: '', lotQuantity: 100, sampleQuantity: 10 })
const failForm = reactive({ defectReason: '', defectType: '外观缺陷', defectQuantity: 1, severity: 'GENERAL' })

const DEFECT_TYPES = ['外观缺陷', '色差', '坏点', '漏光', 'PCB不良', '其他']

const routeCategory = computed(() => route.meta?.category || '')
const isFinished = computed(() => category.value === 'FINISHED_PRODUCT')
const isMaterial = computed(() => category.value === 'RAW_MATERIAL')
const categoryLabel = computed(() =>
  isFinished.value ? '成品质检' : isMaterial.value ? '物料质检' : '质检'
)

const orderList = computed(() => {
  let data = list.value.filter(
    (r) => r.inspectionCategory === category.value && isPendingInspection(r)
  )
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    data = data.filter((r) =>
      (r.inspectionNo || '').toLowerCase().includes(kw) ||
      (r.batchNo || '').toLowerCase().includes(kw) ||
      (r.materialName || '').toLowerCase().includes(kw) ||
      (r.workOrderNo || '').toLowerCase().includes(kw)
    )
  }
  return data
})

/** 待检 KPI：仅统计 PENDING / 需复检 */
const pendingKpi = computed(() => {
  let fp = 0
  let mat = 0
  for (const r of list.value) {
    if (!isPendingInspection(r)) continue
    if (r.inspectionCategory === 'FINISHED_PRODUCT') fp += 1
    else if (r.inspectionCategory === 'RAW_MATERIAL') mat += 1
  }
  return { finishedProduct: fp, rawMaterial: mat }
})

function isPendingInspection(row) {
  const st = normalizeInspectionStatus(row)
  return st === 'PENDING' || st === 'RECHECK_REQUIRED'
}

function normalizeInspectionStatus(row) {
  if (!row) return ''
  const raw = row.inspectionStatus ?? row.status ?? ''
  const cnMap = {
    待检: 'PENDING',
    需复检: 'RECHECK_REQUIRED',
    质检通过: 'PASSED',
    通过: 'PASSED',
    质检不通过: 'FAILED',
    不通过: 'FAILED',
    已关闭: 'CLOSED'
  }
  if (cnMap[raw]) return cnMap[raw]
  return String(raw).toUpperCase()
}

const isFinalized = computed(() => {
  const st = normalizeInspectionStatus({ ...selected.value, ...detail.value })
  return ['PASSED', 'FAILED', 'CLOSED'].includes(st)
})

const canOperate = computed(() => {
  if (!selected.value && !detail.value) return false
  if (isFinalized.value) return false
  // 有待检状态，或已在报告页完成检测（有快照）均可判定
  const st = normalizeInspectionStatus({ ...selected.value, ...detail.value })
  return ['PENDING', 'RECHECK_REQUIRED', ''].includes(st) || !!fpReportSnapshot.value
})

const reportDetail = computed(() => {
  const base = detail.value ? { ...selected.value, ...detail.value } : { ...selected.value }
  // 成品质检：优先用本次实际抽检快照
  if (fpReportSnapshot.value) {
    return {
      ...base,
      sampleQuantity: fpReportSnapshot.value.sampleQuantity,
      qualifiedQuantity: fpReportSnapshot.value.qualifiedQuantity,
      unqualifiedQuantity: fpReportSnapshot.value.unqualifiedQuantity
    }
  }
  if (samplingData.value) {
    base.sampleQuantity = samplingData.value.sampleQuantity
    base.qualifiedQuantity = samplingData.value.qualifiedQuantity
    base.unqualifiedQuantity = samplingData.value.unqualifiedQuantity
  }
  return base
})

const reportItemsForPanel = computed(() => {
  if (isFinished.value) {
    if (reportItems.value.length) return reportItems.value
    if (fpReportSnapshot.value?.matrix?.length) {
      return buildFpReportItems({ matrix: fpReportSnapshot.value.matrix })
    }
  }
  return items.value
})

const unitMatrixExport = computed(() => {
  const matrix = fpReportSnapshot.value?.matrix
  if (!matrix?.length) {
    const rows = fiveStepRef.value?.matrixRows
    if (!rows?.length) return null
    return {
      stationTitles: FP_STATIONS.map((s) => s.title),
      stationIds: FP_STATIONS.map((s) => s.id),
      rows: rows.map((r) => ({ ...r }))
    }
  }
  return {
    stationTitles: FP_STATIONS.map((s) => s.title),
    stationIds: FP_STATIONS.map((s) => s.id),
    rows: matrix.map((r) => ({ ...r }))
  }
})

const canGoReport = computed(() => {
  if (isFinished.value) return fiveStepRef.value?.allDone
  const sum = materialRef.value?.getInspectionSummary?.()
  return sum && sum.items?.length > 0 && sum.items.every((i) => i.result === 'PASSED' || i.result === 'FAILED')
})

function statusTagType(s) {
  return moduleStatusType('qualityInspection', s)
}

function pickCategory(cat) {
  category.value = cat
  flowStep.value = 1
  loadData()
}

function backToCategory() {
  flowStep.value = 0
  selected.value = null
  loadData()
}

async function ensureMaterials() {
  if (Object.keys(materialMap.value).length && rawMaterials.value.length) return
  try {
    const res = await request.get('/material/material/list')
    const arr = res?.data ?? res ?? []
    const m = {}
    const raw = []
    for (const x of arr) {
      m[x.materialId] = x.materialName
      if (x.materialType === 'RAW') raw.push(x)
    }
    materialMap.value = m
    rawMaterials.value = raw
  } catch { /* ignore */ }
}

async function loadData() {
  loading.value = true
  try {
    await ensureMaterials()
    const [v, k] = await Promise.all([
      fetchInspectionViews().catch(() => null),
      fetchQualityKpi().catch(() => null)
    ])
    if (v) {
      const rows = v.data ?? v
      list.value = rows.map((r) => ({
        ...r,
        materialName: r.materialName || materialMap.value[r.materialId] || ''
      }))
    }
    if (k) kpi.value = k.data ?? k
  } finally {
    loading.value = false
  }
}

async function selectOrder(row) {
  selected.value = row
  detail.value = null
  items.value = []
  fpReportSnapshot.value = null
  reportItems.value = []
  sampleQuantity.value = Math.max(Number(row.sampleQuantity) || 1, 1)
  const [d, its] = await Promise.all([
    fetchInspectionDetail(row.inspectionId).catch(() => null),
    fetchInspectionItems(row.inspectionId).catch(() => null)
  ])
  if (d) detail.value = d.data ?? d
  if (its) items.value = (its.data ?? its) || []
}

function goInspect() {
  if (!selected.value) {
    ElMessage.warning('请先选择质检单据')
    return
  }
  flowStep.value = 2
}

function applyAqlSample() {
  const lot = Math.max(sampleQuantity.value, Number(selected.value?.sampleQuantity) || 50)
  let sample = 2
  if (lot <= 50) sample = Math.max(2, Math.ceil(lot * 0.1))
  else if (lot <= 200) sample = Math.ceil(lot * 0.08)
  else sample = Math.ceil(lot * 0.05)
  sampleQuantity.value = Math.min(lot, Math.max(2, sample))
}

function matchItems(station, itemList) {
  const codes = new Set(station.itemCodes || [])
  const keys = station.itemNameKeys || []
  return itemList.filter((it) =>
    codes.has(it.itemCode) || keys.some((k) => (it.itemName || '').includes(k))
  )
}

/** 单台成品：五道工序任一不合格 → 该台不合格 */
function buildFpReportSnapshot(sum) {
  const matrix = sum.matrix || []
  const passUnits = matrix.filter((r) => r.overall === 'PASS').length
  const failUnits = matrix.filter((r) => r.overall === 'FAIL').length
  const sampleQty = Number(sum.sampleQuantity) || matrix.length
  return {
    sampleQuantity: sampleQty,
    qualifiedQuantity: passUnits,
    unqualifiedQuantity: failUnits,
    matrix,
    units: sum.units,
    stationRecords: sum.stationRecords
  }
}

/** 报告检测项：按实际抽检的每一台成品生成 */
function buildFpReportItems(sum) {
  return (sum.matrix || []).map((row, idx) => ({
    itemCode: `UNIT-${String(row.unitNo || idx + 1).padStart(2, '0')}`,
    itemName: `抽检成品 ${row.serialNo}`,
    standardValue: '五步检测全部合格',
    measuredValue: FP_STATIONS.map((s) => {
      const v = row[s.id]
      return `${s.title}:${v === true ? '合格' : v === false ? '不合格' : '—'}`
    }).join(' | '),
    result: row.overall === 'PASS' ? 'PASSED' : row.overall === 'FAIL' ? 'FAILED' : 'PENDING',
    resultCn: row.overall === 'PASS' ? '合格' : row.overall === 'FAIL' ? '不合格' : '未完成',
    defectLevel: row.overall === 'FAIL' ? 'MAJOR' : '',
    defectLevelCn: row.overall === 'FAIL' ? '严重' : '',
    unit: '台'
  }))
}

function formatStationMeasured(recs) {
  const passed = recs.filter((r) => r.passed).length
  const failed = recs.length - passed
  return `${recs.length}台:${passed}合格${failed > 0 ? `/${failed}不合格` : ''}`
}

async function syncFinishedToBackend(sumOverride) {
  const sum = sumOverride || fiveStepRef.value?.getInspectionSummary?.()
  if (!selected.value || !sum) return false
  saving.value = true
  try {
    if (!items.value.length) {
      const res = await generateDefaultItems(selected.value.inspectionId)
      items.value = res.data ?? res ?? []
    }
    let itemList = [...items.value]
    for (const station of FP_STATIONS) {
      const recs = sum.stationRecords.find((s) => s.stationId === station.id)?.records?.filter((r) => r.saved) || []
      if (!recs.length) continue
      const anyFail = recs.some((r) => !r.passed)
      const measured = formatStationMeasured(recs)
      const matched = matchItems(station, itemList)
      if (matched.length) {
        matched.forEach((it) => {
          it.measuredValue = measured
          it.result = anyFail ? 'FAILED' : 'PASSED'
          it.resultCn = anyFail ? '不合格' : '合格'
          if (anyFail) it.defectLevel = it.defectLevel || 'MAJOR'
        })
      }
    }
    await saveInspectionItems(selected.value.inspectionId, itemList)
    try {
      await updateInspectionSampling({
        inspectionId: selected.value.inspectionId,
        sampleQuantity: sum.sampleQuantity,
        qualifiedQuantity: sum.qualifiedQuantity,
        unqualifiedQuantity: sum.unqualifiedQuantity
      })
    } catch (e) {
      // 已判定通过的质检单可能无法改抽样数，报告仍用快照
      console.warn('updateSampling skipped:', e?.message)
    }
    await evaluateInspection(selected.value.inspectionId)
    // 本地状态与抽检结果对齐，不依赖重新拉取
    selected.value = {
      ...selected.value,
      sampleQuantity: sum.sampleQuantity,
      qualifiedQuantity: sum.qualifiedQuantity,
      unqualifiedQuantity: sum.unqualifiedQuantity
    }
    return true
  } catch (e) {
    // 拦截器已弹出后端 message，此处仅记录，避免重复提示
    console.warn('syncFinishedToBackend failed:', e?.message)
    return false
  } finally {
    saving.value = false
  }
}

async function syncMaterialToBackend() {
  if (!selected.value || !materialRef.value) return
  saving.value = true
  try {
    const sum = materialRef.value.getInspectionSummary()
    if (!sum.items?.length) {
      ElMessage.warning('请先生成并填写检测项')
      return
    }
    await saveInspectionItems(selected.value.inspectionId, sum.items)
    items.value = sum.items
    await updateInspectionSampling({
      inspectionId: selected.value.inspectionId,
      sampleQuantity: sum.sampleQuantity,
      qualifiedQuantity: sum.qualifiedQuantity,
      unqualifiedQuantity: sum.unqualifiedQuantity
    })
    await evaluateInspection(selected.value.inspectionId)
    ElMessage.success('物料检测数据已保存')
    await selectOrder(selected.value)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function finishInspect() {
  if (isFinished.value) {
    if (!fiveStepRef.value?.allDone) {
      ElMessage.warning('请完成所有产品 × 五道工序的检测')
      return
    }
    const sum = fiveStepRef.value.getInspectionSummary()
    fpReportSnapshot.value = buildFpReportSnapshot(sum)
    reportItems.value = buildFpReportItems(sum)
    const synced = await syncFinishedToBackend(sum)
    if (synced) {
      ElMessage.success('五步检测结果已同步，正在生成报告')
    } else {
      ElMessage.warning('报告将基于本次实际抽检结果展示')
    }
  } else {
    if (!canGoReport.value) {
      ElMessage.warning('请完成所有检测项的合格/不合格判定')
      return
    }
    await syncMaterialToBackend()
  }
  if (selected.value?.inspectionId) {
    const d = await fetchInspectionDetail(selected.value.inspectionId).catch(() => null)
    if (d) detail.value = d.data ?? d
  }
  flowStep.value = 3
}

function openIncomingDialog() {
  Object.assign(incomingForm, {
    materialId: rawMaterials.value[0]?.materialId ?? null,
    batchNo: `BATCH-${Date.now().toString().slice(-8)}`,
    lotQuantity: 100,
    sampleQuantity: 10
  })
  incomingDialog.value = true
}

async function doCreateIncoming() {
  if (!incomingForm.materialId) { ElMessage.warning('请选择原材料'); return }
  incomingSaving.value = true
  try {
    const res = await createIncomingInspection({
      ...incomingForm,
      operator: userStore.userInfo?.username
    })
    const created = res?.data ?? res
    ElMessage.success('来料检已登记')
    incomingDialog.value = false
    await loadData()
    const row = list.value.find((r) => r.inspectionId === created.inspectionId)
    if (row) await selectOrder(row)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '登记失败')
  } finally {
    incomingSaving.value = false
  }
}

async function refreshSelectedAfterAction(preserveReport = true) {
  const id = selected.value?.inspectionId
  const snap = preserveReport ? fpReportSnapshot.value : null
  const savedReportItems = preserveReport ? [...reportItems.value] : []
  await loadData()
  const row = list.value.find((r) => r.inspectionId === id)
  if (!row) return
  selected.value = row
  const [d, its] = await Promise.all([
    fetchInspectionDetail(row.inspectionId).catch(() => null),
    fetchInspectionItems(row.inspectionId).catch(() => null)
  ])
  if (d) detail.value = d.data ?? d
  if (its) items.value = (its.data ?? its) || []
  if (snap) {
    const merged = {
      ...snap,
      sampleQuantity: Number(row.sampleQuantity) || snap.sampleQuantity,
      qualifiedQuantity: Number(row.qualifiedQuantity) || snap.qualifiedQuantity,
      unqualifiedQuantity: Number(row.unqualifiedQuantity) ?? snap.unqualifiedQuantity
    }
    fpReportSnapshot.value = merged
    reportItems.value = savedReportItems.length
      ? savedReportItems
      : buildFpReportItems({ matrix: snap.matrix })
    selected.value = {
      ...selected.value,
      sampleQuantity: merged.sampleQuantity,
      qualifiedQuantity: merged.qualifiedQuantity,
      unqualifiedQuantity: merged.unqualifiedQuantity
    }
  }
}

function buildPassPayload() {
  const snap = fpReportSnapshot.value
  const payload = {
    inspectionId: selected.value.inspectionId,
    remark: actionRemark.value,
    operator: userStore.userInfo?.username
  }
  if (snap) {
    payload.sampleQuantity = snap.sampleQuantity
    payload.qualifiedQuantity = snap.qualifiedQuantity
    payload.unqualifiedQuantity = snap.unqualifiedQuantity ?? 0
  } else if (samplingData.value) {
    payload.sampleQuantity = samplingData.value.sampleQuantity
    payload.qualifiedQuantity = samplingData.value.qualifiedQuantity
    payload.unqualifiedQuantity = samplingData.value.unqualifiedQuantity ?? 0
  }
  return payload
}

async function ensureReportSynced() {
  if (!selected.value) return true
  if (isFinished.value) {
    if (!fpReportSnapshot.value && fiveStepRef.value?.allDone) {
      const sum = fiveStepRef.value.getInspectionSummary()
      fpReportSnapshot.value = buildFpReportSnapshot(sum)
      reportItems.value = buildFpReportItems(sum)
    }
    if (fpReportSnapshot.value) {
      await syncFinishedToBackend(fpReportSnapshot.value)
    }
  } else if (materialRef.value) {
    const sum = materialRef.value.getInspectionSummary?.()
    if (sum?.items?.length) {
      samplingData.value = {
        sampleQuantity: sum.sampleQuantity,
        qualifiedQuantity: sum.qualifiedQuantity,
        unqualifiedQuantity: sum.unqualifiedQuantity
      }
      try {
        await saveInspectionItems(selected.value.inspectionId, sum.items)
        await updateInspectionSampling({
          inspectionId: selected.value.inspectionId,
          sampleQuantity: sum.sampleQuantity,
          qualifiedQuantity: sum.qualifiedQuantity,
          unqualifiedQuantity: sum.unqualifiedQuantity
        })
      } catch (e) {
        console.warn('material sync skipped:', e?.message)
      }
    }
  }
  return true
}

async function doPass() {
  if (!canGoReport.value) {
    ElMessage.warning(isFinished.value ? '请完成所有产品 × 五道工序的检测' : '请完成所有检测项判定')
    return
  }
  await ElMessageBox.confirm('确认该批次质检通过？', '质检通过', { type: 'success' })
  acting.value = true
  const passedId = selected.value?.inspectionId
  try {
    await ensureReportSynced()
    await passInspection(buildPassPayload())
    ElMessage.success('质检通过')
    fpReportSnapshot.value = null
    reportItems.value = []
    actionRemark.value = ''
    selected.value = null
    detail.value = null
    items.value = []
    await loadData()
    flowStep.value = 1
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
    if (passedId) {
      await loadData()
    }
  } finally {
    acting.value = false
  }
}

function openFailDialog() {
  const unqual = fpReportSnapshot.value?.unqualifiedQuantity || reportDetail.value?.unqualifiedQuantity || 1
  Object.assign(failForm, { defectReason: '', defectType: '外观缺陷', defectQuantity: Math.max(1, unqual) })
  failDialog.value = true
}

async function doFail() {
  if (!failForm.defectReason.trim()) { ElMessage.warning('请填写不良原因'); return }
  acting.value = true
  try {
    await ensureReportSynced()
    const payload = {
      inspectionId: selected.value.inspectionId,
      ...failForm,
      operator: userStore.userInfo?.username
    }
    const snap = fpReportSnapshot.value
    if (snap) {
      payload.sampleQuantity = snap.sampleQuantity
      payload.qualifiedQuantity = snap.qualifiedQuantity
      payload.unqualifiedQuantity = snap.unqualifiedQuantity ?? 0
    }
    await failInspection(payload)
    ElMessage.warning('质检不通过')
    failDialog.value = false
    fpReportSnapshot.value = null
    reportItems.value = []
    selected.value = null
    detail.value = null
    items.value = []
    await loadData()
    flowStep.value = 1
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  } finally {
    acting.value = false
  }
}

onMounted(async () => {
  await loadData()
  if (routeCategory.value) {
    category.value = routeCategory.value
    flowStep.value = 1
  }
})

watch(routeCategory, (val) => {
  if (val) {
    category.value = val
    flowStep.value = 1
    selected.value = null
    loadData()
  }
})
</script>

<style scoped>
.qc-flow-page {
  padding: 0;
}

.qc-flow-page__steps {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 16px 6px;
  margin-bottom: 8px;
}

.qc-flow-page__steps--inline {
  margin-bottom: 0;
  border-radius: 8px 8px 0 0;
  border-bottom: none;
  padding: 8px 12px 4px;
}

.qc-flow-page__section {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
}

.qc-flow-page__section--inspect {
  margin-top: 0;
  border-radius: 0 0 8px 8px;
  padding: 12px 16px 16px;
}

.qc-flow-page__heading {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
}

.qc-flow-page__sub {
  margin: 0 0 28px;
  color: #909399;
  font-size: 14px;
}

.qc-cat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  max-width: 900px;
  margin: 0 auto;
}

.qc-cat-card {
  text-align: center;
  padding: 40px 28px;
  border: 2px solid #e4e7ed;
  border-radius: 16px;
  background: #fafbfc;
  cursor: pointer;
  transition: all .25s;
}

.qc-cat-card:hover {
  border-color: #409eff;
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(64, 158, 255, .15);
}

.qc-cat-card--fp:hover { border-color: #409eff; }
.qc-cat-card--mat:hover { border-color: #67c23a; }

.qc-cat-card__icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.qc-cat-card h3 {
  margin: 0 0 10px;
  font-size: 22px;
  color: #303133;
}

.qc-cat-card p {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.6;
  color: #909399;
}

.qc-flow-page__bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.qc-flow-page__bar-actions {
  margin-left: auto;
  display: flex;
  gap: 10px;
  align-items: center;
}

.qc-order-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.qc-order-card {
  border: 2px solid #ebeef5;
  border-radius: 10px;
  padding: 16px;
  cursor: pointer;
  transition: border-color .2s;
}

.qc-order-card:hover { border-color: #c6e2ff; }
.qc-order-card.is-selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.qc-order-card__hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.qc-order-card p {
  margin: 4px 0;
  font-size: 13px;
  color: #606266;
}

.qc-order-card__meta {
  font-size: 12px !important;
  color: #909399 !important;
}

.qc-flow-page__footer {
  margin-top: 24px;
  text-align: center;
}

.qc-order-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  flex-wrap: wrap;
  font-size: 14px;
}

.qc-flow-page__sample {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
}

.qc-flow-page__sync-bar {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  flex-wrap: wrap;
}

.qc-flow-page__sync-bar :deep(.el-button) {
  --el-button-text-color: #fff;
  min-width: 160px;
}

.qc-flow-page__sync-bar :deep(.el-button--primary) {
  --el-button-text-color: #fff;
}

.qc-flow-page__sync-bar :deep(.el-button--success) {
  --el-button-text-color: #fff;
}

.qc-flow-page__sync-bar :deep(.el-button.is-disabled) {
  --el-button-text-color: #a8abb2;
}

.qc-flow-page__remark {
  margin-top: 16px;
  max-width: 600px;
}

.qc-flow-page__pass-hint {
  margin: 10px 0 0;
  font-size: 13px;
  color: #909399;
}

.qc-flow-page__decide-bar {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 20px;
  padding: 16px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}
</style>
