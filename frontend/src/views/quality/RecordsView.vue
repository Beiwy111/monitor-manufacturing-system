<template>
  <div class="ruoyi-page records-page">
    <div class="ruoyi-stats">
      <span class="ruoyi-stats__item">质检总数：<em>{{ list.length }}</em></span>
      <span class="ruoyi-stats__item">通过：<em>{{ passCount }}</em></span>
      <span class="ruoyi-stats__item ruoyi-stats__item--danger">不通过：<em>{{ failCount }}</em></span>
      <span class="ruoyi-stats__item ruoyi-stats__item--warn">需复检：<em>{{ recheckCount }}</em></span>
      <span class="ruoyi-stats__item">平均合格率：<em>{{ avgYield }}%</em></span>
      <span class="ruoyi-stats__item">不良总数：<em>{{ totalDefect }}</em></span>
    </div>

    <div class="ruoyi-toolbar">
      <span class="ruoyi-toolbar__title">质检记录</span>
      <el-input v-model="keyword" placeholder="质检单号/工单/批次/物料" clearable size="small" style="width:220px" />
      <el-select v-model="filterResult" clearable placeholder="检验结果" size="small" style="width:110px">
        <el-option label="通过"   value="QUALIFIED" />
        <el-option label="不通过" value="UNQUALIFIED" />
        <el-option label="待检"   value="PENDING" />
      </el-select>
      <el-select v-model="filterCategory" clearable placeholder="产品类型" size="small" style="width:110px">
        <el-option label="成品"   value="FINISHED_PRODUCT" />
        <el-option label="半成品" value="SEMI_FINISHED" />
      </el-select>
      <el-button size="small" :loading="loading" @click="load">刷新</el-button>
    </div>

    <div class="ruoyi-table-wrap">
      <el-table :data="filtered" border stripe highlight-current-row size="small"
        style="width:100%" v-loading="loading" @current-change="onSelect">
      <el-table-column prop="inspectionNo"      label="质检单号"   width="140" />
      <el-table-column prop="workOrderNo"       label="工单号"     width="140" show-overflow-tooltip />
      <el-table-column prop="materialName"      label="物料/产品"  min-width="130" show-overflow-tooltip />
      <el-table-column prop="batchNo"           label="批次"       width="140" show-overflow-tooltip />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.inspectionCategory==='SEMI_FINISHED'?'warning':'success'" size="small" effect="plain">
            {{ row.inspectionCategoryCn }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sampleQuantity"    label="送检"  width="65" align="center" />
      <el-table-column prop="qualifiedQuantity" label="合格"  width="65" align="center">
        <template #default="{ row }">
          <span style="color:#67c23a;font-weight:600">{{ row.qualifiedQuantity }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="unqualifiedQuantity" label="不良" width="65" align="center">
        <template #default="{ row }">
          <span :style="Number(row.unqualifiedQuantity)>0?'color:#f56c6c;font-weight:700':''">
            {{ row.unqualifiedQuantity }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="合格率" width="72" align="center">
        <template #default="{ row }">
          <el-tag :type="yieldType(row)" size="small">{{ yieldRate(row) }}%</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="检验结果" width="90">
        <template #default="{ row }">
          <el-tag :type="resultType(row.inspectionResult)" size="small">{{ row.inspectionResultCn }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.inspectionStatus)" size="small">{{ row.inspectionStatusCn }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="inspectedAt" label="检验时间" width="148" />
    </el-table>
    </div>

    <el-dialog v-model="detailVisible" title="质检记录详情" width="680px" :close-on-click-modal="true">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="质检单号">{{ detail.inspectionNo }}</el-descriptions-item>
          <el-descriptions-item label="检验类型">{{ detail.inspectionTypeCn }}</el-descriptions-item>
          <el-descriptions-item label="产品类型">
            <el-tag :type="detail.inspectionCategory==='SEMI_FINISHED'?'warning':'success'" size="small" effect="plain">
              {{ detail.inspectionCategoryCn }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="工单号">{{ detail.workOrderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="物料">{{ detail.materialName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="批次">{{ detail.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="送检数">{{ detail.sampleQuantity }}</el-descriptions-item>
          <el-descriptions-item label="合格数">
            <span style="color:#67c23a;font-weight:600">{{ detail.qualifiedQuantity }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="不良数">
            <span :style="Number(detail.unqualifiedQuantity)>0?'color:#f56c6c;font-weight:700':''">
              {{ detail.unqualifiedQuantity }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="合格率">
            <el-tag :type="yieldType(detail)" size="small">{{ yieldRate(detail) }}%</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="检验结果">
            <el-tag :type="resultType(detail.inspectionResult)" size="small">{{ detail.inspectionResultCn }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="检验时间">{{ detail.inspectedAt }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.remark" label="备注" :span="2">{{ detail.remark }}</el-descriptions-item>
        </el-descriptions>

        <!-- 检测项明细 -->
        <div v-if="detailItems.length" style="margin-top:16px">
          <div style="font-size:13px;font-weight:600;color:#2c3e50;margin-bottom:8px">
            检测项明细（{{ detailItems.length }} 项）
          </div>
          <el-table :data="detailItems" border size="small">
            <el-table-column prop="itemName"       label="检测项"   min-width="110" />
            <el-table-column prop="standardValue"  label="标准值"   width="100" show-overflow-tooltip />
            <el-table-column prop="unit"           label="单位"     width="60" />
            <el-table-column label="实测值" width="90">
              <template #default="{ row }">{{ row.measuredValue || '-' }}</template>
            </el-table-column>
            <el-table-column label="结果" width="75">
              <template #default="{ row }">
                <el-tag :type="itType(row.result)" size="small">{{ row.resultCn }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 关联不良品 -->
        <div v-if="detailNc.length" style="margin-top:16px">
          <div style="font-size:13px;font-weight:600;color:#f56c6c;margin-bottom:8px">
            关联不良品（{{ detailNc.length }} 条）
          </div>
          <el-table :data="detailNc" border size="small">
            <el-table-column prop="nonconformingNo" label="不良品单号" width="140" />
            <el-table-column prop="defectType"      label="缺陷类型"  min-width="100" />
            <el-table-column prop="quantity"        label="数量"      width="60" align="center" />
            <el-table-column label="严重度" width="75">
              <template #default="{ row }">
                <el-tag :type="sevType(row.severity)" size="small">{{ row.severityCn }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="处置状态" width="85">
              <template #default="{ row }">
                <el-tag :type="ncStatusType(row.handleStatus)" size="small">{{ row.handleStatusCn }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchInspectionViews, fetchInspectionDetail, fetchInspectionItems } from '@/api/quality'

const route = useRoute()
const routeCategory = computed(() => route.meta?.category || '')

const list           = ref([])
const loading        = ref(false)
const selected       = ref(null)
const detail         = ref(null)
const detailItems    = ref([])
const detailNc       = ref([])
const detailVisible  = ref(false)
const keyword        = ref('')
const filterResult   = ref('')
const filterCategory = ref(routeCategory.value)

const passCount   = computed(() => list.value.filter(r => r.inspectionStatus === 'PASSED').length)
const failCount   = computed(() => list.value.filter(r => r.inspectionStatus === 'FAILED').length)
const recheckCount = computed(() => list.value.filter(r => r.inspectionStatus === 'RECHECK_REQUIRED').length)
const totalDefect = computed(() => list.value.reduce((s, r) => s + (Number(r.unqualifiedQuantity) || 0), 0))
const avgYield = computed(() => {
  const finished = list.value.filter(r => Number(r.sampleQuantity) > 0)
  if (!finished.length) return '0.0'
  const sum = finished.reduce((s, r) => s + Number(yieldRate(r)), 0)
  return (sum / finished.length).toFixed(1)
})

const filtered = computed(() => {
  let data = list.value
  if (filterResult.value)   data = data.filter(r => r.inspectionResult === filterResult.value)
  if (filterCategory.value) data = data.filter(r => r.inspectionCategory === filterCategory.value)
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    data = data.filter(r =>
      (r.inspectionNo  || '').toLowerCase().includes(kw) ||
      (r.workOrderNo   || '').toLowerCase().includes(kw) ||
      (r.batchNo       || '').toLowerCase().includes(kw) ||
      (r.materialName  || '').toLowerCase().includes(kw)
    )
  }
  return data
})

function yieldRate(row) {
  const s = Number(row.sampleQuantity)
  const q = Number(row.qualifiedQuantity)
  if (!s || s <= 0) return 0
  if (!Number.isFinite(q) || q <= 0) return 0
  const r = Math.round(q / s * 100)
  return Math.min(100, Math.max(0, r))
}
function yieldType(row) {
  const y = yieldRate(row)
  if (y >= 95) return 'success'
  if (y >= 80) return 'warning'
  return 'danger'
}
function resultType(s)  {
  return { QUALIFIED:'success', UNQUALIFIED:'danger', PENDING:'info' }[s] || 'info'
}
function statusType(s)  {
  return { PASSED:'success', FAILED:'danger', RECHECK_REQUIRED:'warning', PENDING:'info', CLOSED:'info' }[s] || 'info'
}
function itType(r)      { return { PASSED:'success', FAILED:'danger', WARNING:'warning', PENDING:'info' }[r] || 'info' }
function sevType(s)     { return { MINOR:'', GENERAL:'warning', MAJOR:'danger', CRITICAL:'danger' }[s] || '' }
function ncStatusType(s){ return { PENDING:'danger', PROCESSING:'warning', DONE:'success' }[s] || 'info' }

async function load() {
  loading.value = true
  try {
    const res = await fetchInspectionViews()
    list.value = (res.data ?? res).map(r => ({
      ...r,
      inspectionResultCn: { QUALIFIED:'合格', UNQUALIFIED:'不合格', PENDING:'待判定' }[r.inspectionResult] || r.inspectionResult
    }))
  } catch { /* 静默 */ }
  finally { loading.value = false }
}

async function onSelect(row) {
  if (!row) return
  selected.value = row
  detailVisible.value = true
  detail.value = null; detailItems.value = []; detailNc.value = []
  try {
    const [d, it] = await Promise.all([
      fetchInspectionDetail(row.inspectionId).catch(() => null),
      fetchInspectionItems(row.inspectionId).catch(() => null)
    ])
    const dData = d ? (d.data ?? d) : {}
    detail.value     = { ...row, ...dData }
    detailItems.value = it ? (it.data ?? it) : []
    detailNc.value    = dData.nonconformingList || []
  } catch { /* 静默 */ }
}

onMounted(load)

watch(routeCategory, (val) => {
  filterCategory.value = val
  selected.value = null
  load()
})
</script>

<style scoped>
</style>
