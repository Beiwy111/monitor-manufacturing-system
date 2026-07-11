<template>
  <div class="ruoyi-page inspection-page">
    <div class="ruoyi-stats">
      <span
        v-for="card in kpiCards"
        :key="card.key"
        class="ruoyi-stats__item ruoyi-stats__item--filter"
        :class="{
          'is-active': tabActive === card.key,
          'ruoyi-stats__item--warn': card.cls === 'recheck' || card.cls === 'pending',
          'ruoyi-stats__item--danger': card.cls === 'failed'
        }"
        @click="tabActive = card.key"
      >
        {{ card.label }}：<em>{{ kpi[card.kpiKey] ?? 0 }}</em>
      </span>
    </div>

    <div class="ruoyi-toolbar">
      <span class="ruoyi-toolbar__title">{{ pageTitle }}</span>
      <el-input v-model="keyword" placeholder="质检单/批次/物料" clearable size="small" style="width:190px" />
      <el-select v-if="!routeCategory" v-model="categoryFilter" clearable placeholder="分类" size="small" style="width:108px">
        <el-option label="物料来料" value="RAW_MATERIAL" />
        <el-option label="成品质检" value="FINISHED_PRODUCT" />
      </el-select>
      <el-button v-if="isMaterialMode" type="primary" size="small" @click="openIncomingDialog">登记来料检</el-button>
      <el-button size="small" :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <div class="qc-split-layout">
      <div class="qc-split-layout__main ruoyi-table-wrap">
        <el-table ref="tableRef" :data="filtered" border stripe highlight-current-row size="small"
          style="width:100%" @current-change="onSelect">
          <el-table-column prop="inspectionNo" label="质检单号" width="132" />
          <el-table-column label="分类" width="88">
            <template #default="{ row }">
              <el-tag :type="row.inspectionCategory==='FINISHED_PRODUCT'?'primary':''" size="small">
                {{ row.inspectionCategoryCn }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="inspectionTypeCn" label="检验类型" width="88" />
          <el-table-column prop="materialName" label="物料/产品" min-width="100" show-overflow-tooltip />
          <el-table-column prop="batchNo" label="批次" width="108" show-overflow-tooltip />
          <el-table-column prop="sampleQuantity" label="送检" width="55" align="center" />
          <el-table-column prop="unqualifiedQuantity" label="不良" width="55" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.unqualifiedQuantity>0" type="danger" size="small">{{ row.unqualifiedQuantity }}</el-tag>
              <span v-else style="color:#bbb">0</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="88">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.inspectionStatus)" size="small">{{ row.inspectionStatusCn }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="148" />
        </el-table>
      </div>

      <div class="qc-split-layout__side">
        <template v-if="selected">
        <div class="qc-section">
          <div class="qc-section__title">
            {{ selected.inspectionCategoryCn }} · {{ selected.inspectionTypeCn }}
            <el-tag :type="statusTagType(selected.inspectionStatus)" size="small" style="margin-left:8px">
              {{ selected.inspectionStatusCn }}
            </el-tag>
          </div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="质检单号">{{ selected.inspectionNo }}</el-descriptions-item>
            <el-descriptions-item label="工单号">{{ selected.workOrderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="物料/产品">{{ selected.materialName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="批次">{{ selected.batchNo }}</el-descriptions-item>
            <el-descriptions-item label="送检数">{{ selected.sampleQuantity }}</el-descriptions-item>
            <el-descriptions-item label="不合格数">
              <span :style="selected.unqualifiedQuantity>0?'color:#f56c6c;font-weight:600':''">
                {{ selected.unqualifiedQuantity }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="检验员">{{ selected.inspectorName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="检验时间">{{ selected.inspectedAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ selected.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div v-if="(isFinishedMode || isMaterialMode) && canOperate" class="qc-section">
          <div class="qc-section__title">抽样检测</div>
          <el-form label-width="80px" size="small" style="max-width:400px">
            <el-form-item v-if="isFinishedMode" label="批次总量">
              <el-input-number v-model="samplingForm.lotQuantity" :min="1" :max="99999" style="width:120px" />
            </el-form-item>
            <el-form-item label="抽样数量">
              <el-input-number v-model="samplingForm.sampleQuantity" :min="1" :max="samplingForm.lotQuantity || 9999" style="width:120px" />
              <el-button v-if="isFinishedMode" link type="primary" size="small" style="margin-left:8px" @click="applyAqlSample">按 AQL 建议</el-button>
            </el-form-item>
            <el-form-item label="合格数量">
              <el-input-number v-model="samplingForm.qualifiedQuantity" :min="0" :max="samplingForm.sampleQuantity" style="width:120px" />
            </el-form-item>
            <el-form-item label="不良数量">
              <el-input-number v-model="samplingForm.unqualifiedQuantity" :min="0" :max="samplingForm.sampleQuantity" style="width:120px" />
            </el-form-item>
          </el-form>
          <el-button size="small" type="primary" plain :loading="samplingSaving" @click="saveSampling">保存抽样数据</el-button>
          <span v-if="samplingForm.sampleQuantity" class="sampling-hint">
            合格率 {{ samplingYield }}%（{{ samplingForm.qualifiedQuantity }}/{{ samplingForm.sampleQuantity }}）
          </span>
        </div>

        <div v-if="isFinishedMode && isCompleted" class="qc-section">
          <QualityReportPanel :detail="reportDetail" :items="items" />
        </div>

        <div class="qc-section">
          <div class="qc-section__title">
            检测项明细
            <el-button v-if="canOperate" size="small" style="margin-left:8px"
              :loading="itemsLoading" @click="doGenerateItems">生成默认项</el-button>
          </div>
          <div v-if="items.length===0" class="empty-hint">
            暂无检测项——点击「生成默认项」自动填充行业标准检测项
          </div>
          <el-table v-else :data="items" border size="small" style="width:100%">
            <el-table-column prop="itemCode" label="编号" width="80" />
            <el-table-column prop="itemName" label="检测项" min-width="95" />
            <el-table-column prop="standardValue" label="标准值" width="110" show-overflow-tooltip />
            <el-table-column label="实测值" width="92">
              <template #default="{ row }">
                <el-input v-if="canOperate" v-model="row.measuredValue" size="small"
                  style="width:82px" @input="markDirty" />
                <span v-else>{{ row.measuredValue || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="unit" label="单位" width="52" align="center" />
            <el-table-column label="结果" width="82">
              <template #default="{ row }">
                <el-select v-if="canOperate" v-model="row.result" size="small" style="width:74px"
                  @change="markDirty">
                  <el-option label="待检" value="PENDING" />
                  <el-option label="合格" value="PASSED" />
                  <el-option label="警告" value="WARNING" />
                  <el-option label="不合格" value="FAILED" />
                </el-select>
                <el-tag v-else :type="itemTagType(row.result)" size="small">{{ row.resultCn }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="缺陷等级" width="82">
              <template #default="{ row }">
                <el-select v-if="canOperate&&row.result==='FAILED'" v-model="row.defectLevel"
                  size="small" style="width:74px" @change="markDirty">
                  <el-option label="轻微" value="MINOR" />
                  <el-option label="严重" value="MAJOR" />
                  <el-option label="致命" value="CRITICAL" />
                </el-select>
                <el-tag v-else-if="row.defectLevel" :type="defectTagType(row.defectLevel)" size="small">
                  {{ row.defectLevelCn }}
                </el-tag>
                <span v-else style="color:#bbb">-</span>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="canOperate&&items.length>0" class="items-footer">
            <el-button size="small" type="primary" :loading="savingItems" @click="doSaveItems">保存检测项</el-button>
            <el-button size="small" type="primary" plain :loading="savingItems||evaluating" @click="doSaveAndEvaluate">保存并判定</el-button>
            <el-button size="small" :loading="evaluating" @click="doEvaluate">仅汇总</el-button>
            <template v-if="evalResult">
              <el-tag :type="statusTagType(evalResult.suggestedResult)" style="margin-left:4px">
                建议：{{ evalResult.suggestedResultCn }}（通过{{ evalResult.passed }} 失败{{ evalResult.failed }} 待检{{ evalResult.pending }}）
              </el-tag>
              <el-button v-if="selected.inspectionStatus==='PENDING' && evalResult.suggestedResult!=='PENDING'"
                size="small" type="warning" plain @click="applySuggestion">按建议判定</el-button>
            </template>
          </div>
        </div>

        <div v-if="selected.inspectionStatus!=='CLOSED'" class="qc-section">
          <div class="qc-section__title">质检操作</div>
          <el-form label-width="70px" size="small" style="max-width:380px">
            <el-form-item label="备注">
              <el-input v-model="actionForm.remark" type="textarea" rows="2" />
            </el-form-item>
          </el-form>
          <div class="action-row">
            <template v-if="selected.inspectionStatus==='PENDING'">
              <el-button type="success" :loading="acting" @click="doPass">质检通过</el-button>
              <el-button type="danger" @click="openFailDialog">质检不通过</el-button>
              <el-button @click="openRecheckDialog">转复检</el-button>
            </template>
            <template v-else-if="selected.inspectionStatus==='RECHECK_REQUIRED'">
              <el-button type="success" :loading="acting" @click="doRecheckPass">复检通过</el-button>
              <el-button type="danger" @click="openRecheckFailDialog">复检不通过</el-button>
            </template>
            <template v-else>
              <el-button v-if="selected.inspectionStatus==='FAILED'" type="warning" size="small"
                @click="openRecheckDialog">转复检</el-button>
              <el-button size="small" :loading="acting" @click="doClose">关闭质检单</el-button>
            </template>
          </div>
        </div>
        <div v-else class="qc-section"><el-tag type="info">质检单已关闭</el-tag></div>

        <div v-if="detail?.nonconformingList?.length" class="qc-section">
          <div class="qc-section__title">关联不良品</div>
          <el-table :data="detail.nonconformingList" border size="small">
            <el-table-column prop="nonconformingNo" label="不良品单号" width="140" />
            <el-table-column prop="defectType" label="缺陷类型" width="90" />
            <el-table-column prop="quantity" label="数量" width="55" align="center" />
            <el-table-column prop="severityCn" label="严重程度" width="80" />
            <el-table-column label="处理状态" width="85">
              <template #default="{ row }">
                <el-tag :type="row.handleStatus==='DONE'?'success':'warning'" size="small">
                  {{ row.handleStatusCn }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
        </template>
        <div v-else class="ruoyi-detail__empty" style="padding: 48px 16px; text-align: center;">
          点击左侧质检单查看详情和操作
        </div>
      </div>
    </div>

    <!-- 质检不通过对话框 -->
    <el-dialog v-model="failDialog" title="质检不通过 · 填写不良信息" width="480px" :close-on-click-modal="false">
      <el-form :model="failForm" label-width="90px" size="small">
        <el-form-item label="不良原因" required>
          <el-input v-model="failForm.defectReason" type="textarea" rows="2" placeholder="必填" />
        </el-form-item>
        <el-form-item label="缺陷类型">
          <el-select v-model="failForm.defectType" style="width:100%">
            <el-option v-for="t in DEFECT_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="不良数量" required>
          <el-input-number v-model="failForm.defectQuantity" :min="1" />
        </el-form-item>
        <el-form-item label="严重程度">
          <el-radio-group v-model="failForm.severity">
            <el-radio value="MINOR">轻微</el-radio>
            <el-radio value="GENERAL">一般</el-radio>
            <el-radio value="MAJOR">严重</el-radio>
            <el-radio value="CRITICAL">致命</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="failForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="failDialog=false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="doFail">确认不通过</el-button>
      </template>
    </el-dialog>

    <!-- 转复检对话框 -->
    <el-dialog v-model="recheckDialog" title="转复检" width="400px">
      <el-form label-width="80px" size="small">
        <el-form-item label="复检原因">
          <el-input v-model="recheckReason" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recheckDialog=false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="doRecheck">确认</el-button>
      </template>
    </el-dialog>

    <!-- 复检不通过对话框 -->
    <el-dialog v-model="recheckFailDialog" title="复检不通过 · 填写不良信息" width="480px" :close-on-click-modal="false">
      <el-form :model="recheckFailForm" label-width="90px" size="small">
        <el-form-item label="不良原因" required>
          <el-input v-model="recheckFailForm.defectReason" type="textarea" rows="2" placeholder="必填" />
        </el-form-item>
        <el-form-item label="缺陷类型">
          <el-select v-model="recheckFailForm.defectType" style="width:100%">
            <el-option v-for="t in DEFECT_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="不良数量" required>
          <el-input-number v-model="recheckFailForm.defectQuantity" :min="1" />
        </el-form-item>
        <el-form-item label="严重程度">
          <el-radio-group v-model="recheckFailForm.severity">
            <el-radio value="MINOR">轻微</el-radio>
            <el-radio value="GENERAL">一般</el-radio>
            <el-radio value="MAJOR">严重</el-radio>
            <el-radio value="CRITICAL">致命</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="recheckFailForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recheckFailDialog=false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="doRecheckFail">确认</el-button>
      </template>
    </el-dialog>

    <!-- 来料检登记 -->
    <el-dialog v-model="incomingDialog" title="登记来料检验" width="480px" :close-on-click-modal="false">
      <el-form :model="incomingForm" label-width="90px" size="small">
        <el-form-item label="原材料" required>
          <el-select v-model="incomingForm.materialId" filterable placeholder="选择仓储原材料" style="width:100%">
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
        <el-button @click="incomingDialog=false">取消</el-button>
        <el-button type="primary" :loading="incomingSaving" @click="doCreateIncoming">确认登记</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<!-- SCRIPT_PLACEHOLDER -->

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { moduleStatusType } from '@/constants/moduleStatus'
import {
  fetchInspectionViews, fetchQualityKpi, fetchInspectionDetail,
  fetchInspectionItems, generateDefaultItems, saveInspectionItems, evaluateInspection,
  passInspection, failInspection, requireRecheck,
  recheckPass, recheckFail, closeInspection,
  createIncomingInspection, updateInspectionSampling
} from '@/api/quality'
import request from '@/utils/request'
import QualityReportPanel from '@/components/quality/QualityReportPanel.vue'

const route = useRoute()
const userStore = useUserStore()

const routeCategory = computed(() => route.meta?.category || '')
const isMaterialMode = computed(() => routeCategory.value === 'RAW_MATERIAL')
const isFinishedMode = computed(() => routeCategory.value === 'FINISHED_PRODUCT')
const pageTitle = computed(() => {
  if (routeCategory.value === 'RAW_MATERIAL') return '物料来料质检'
  if (routeCategory.value === 'FINISHED_PRODUCT') return '成品终检'
  if (routeCategory.value === 'SEMI_FINISHED') return '半成品质检'
  return '待检任务'
})

const list     = ref([])
const kpi      = ref({})
const selected = ref(null)
const detail   = ref(null)
const items    = ref([])
const evalResult = ref(null)

const keyword        = ref('')
const categoryFilter = ref(routeCategory.value)
const tabActive      = ref(routeCategory.value ? 'all' : 'PENDING')
const loading        = ref(false)
const itemsLoading   = ref(false)
const savingItems    = ref(false)
const evaluating     = ref(false)
const acting         = ref(false)
const itemsDirty     = ref(false)
const tableRef       = ref(null)
const materialMap    = ref({})
const rawMaterials   = ref([])
const incomingDialog = ref(false)
const incomingSaving = ref(false)
const samplingSaving = ref(false)
const incomingForm   = reactive({ materialId: null, batchNo: '', lotQuantity: 100, sampleQuantity: 10 })
const samplingForm   = reactive({ lotQuantity: 50, sampleQuantity: 5, qualifiedQuantity: 0, unqualifiedQuantity: 0 })

const isCompleted = computed(() =>
  selected.value && ['PASSED', 'FAILED', 'CLOSED'].includes(selected.value.inspectionStatus)
)
const reportDetail = computed(() => detail.value ? { ...selected.value, ...detail.value } : selected.value)
const samplingYield = computed(() => {
  const s = samplingForm.sampleQuantity
  const q = samplingForm.qualifiedQuantity
  if (!s) return '0.0'
  return Math.min(100, Math.max(0, Math.round((q / s) * 1000) / 10)).toFixed(1)
})

const kpiCards = computed(() => {
  const base = [
    { key: 'all', kpiKey: 'total', label: '全部', cls: '' },
    { key: 'PENDING', kpiKey: 'pending', label: '待检', cls: 'pending' },
    { key: 'PASSED', kpiKey: 'passed', label: '已通过', cls: 'passed' },
    { key: 'FAILED', kpiKey: 'failed', label: '不通过', cls: 'failed' },
    { key: 'RECHECK_REQUIRED', kpiKey: 'recheck', label: '需复检', cls: 'recheck' }
  ]
  if (isMaterialMode.value) {
    return [...base.slice(0, 2), { key: 'RAW_MATERIAL', kpiKey: 'rawMaterial', label: '物料', cls: 'semi' }, ...base.slice(2)]
  }
  if (isFinishedMode.value) {
    return [...base.slice(0, 2), { key: 'FINISHED_PRODUCT', kpiKey: 'finishedProduct', label: '成品', cls: 'finished' }, ...base.slice(2)]
  }
  return [
    ...base.slice(0, 2),
    { key: 'RAW_MATERIAL', kpiKey: 'rawMaterial', label: '物料', cls: 'semi' },
    { key: 'FINISHED_PRODUCT', kpiKey: 'finishedProduct', label: '成品', cls: 'finished' },
    ...base.slice(2)
  ]
})

const failDialog         = ref(false)
const recheckDialog      = ref(false)
const recheckFailDialog  = ref(false)
const recheckReason      = ref('')

const actionForm = reactive({ remark: '' })
const failForm = reactive({ defectReason: '', defectType: '外观缺陷', defectQuantity: 1, severity: 'GENERAL', remark: '' })
const recheckFailForm = reactive({ defectReason: '', defectType: '外观缺陷', defectQuantity: 1, severity: 'GENERAL', remark: '' })

const DEFECT_TYPES = ['外观缺陷', '色差', '坏点', '亮点', '暗点', '漏光', '色准超标', '亮度不足', 'PCB不良', '线路不良', '其他']

// 物料名映射（后端 view 仅返回 materialId，前端补全物料/产品名）
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
  } catch { /* 忽略 */ }
}

const canOperate = computed(() =>
  selected.value && !['CLOSED'].includes(selected.value.inspectionStatus)
)

const filtered = computed(() => {
  let data = list.value
  if (tabActive.value !== 'all') {
    if (['SEMI_FINISHED', 'FINISHED_PRODUCT', 'RAW_MATERIAL'].includes(tabActive.value)) {
      data = data.filter(r => r.inspectionCategory === tabActive.value)
    } else {
      data = data.filter(r => r.inspectionStatus === tabActive.value)
    }
  }
  if (categoryFilter.value) {
    data = data.filter(r => r.inspectionCategory === categoryFilter.value)
  }
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    data = data.filter(r =>
      (r.inspectionNo || '').toLowerCase().includes(kw) ||
      (r.batchNo || '').toLowerCase().includes(kw) ||
      (r.materialName || '').toLowerCase().includes(kw) ||
      (r.workOrderNo || '').toLowerCase().includes(kw)
    )
  }
  return data
})

function statusTagType(s) {
  return moduleStatusType('qualityInspection', s)
}
function itemTagType(s) {
  return moduleStatusType('qualityItem', s)
}
function defectTagType(s) {
  return moduleStatusType('defectLevel', s, '')
}
function markDirty() { itemsDirty.value = true }

async function loadData() {
  loading.value = true
  const keepId = selected.value?.inspectionId
  try {
    await ensureMaterials()
    const [v, k] = await Promise.all([
      fetchInspectionViews().catch(() => null),
      fetchQualityKpi().catch(() => null)
    ])
    if (v) {
      const rows = v.data ?? v
      list.value = rows.map(r => ({ ...r, materialName: r.materialName || materialMap.value[r.materialId] || '' }))
    }
    if (k) kpi.value = k.data ?? k
    await nextTick()
    // 保持原选中；否则自动选中当前筛选下第一条，避免空面板
    const target = (keepId && list.value.find(r => r.inspectionId === keepId)) || filtered.value[0] || null
    if (target) {
      tableRef.value?.setCurrentRow(target)
      // setCurrentRow 不触发 @current-change，手动驱动右侧面板更新
      if (!keepId || target.inspectionId !== selected.value?.inspectionId) {
        await onSelect(target)
      }
    } else {
      selected.value = null
    }
  } finally {
    loading.value = false
  }
}

async function onSelect(row) {
  selected.value = row
  detail.value = null
  items.value = []
  evalResult.value = null
  itemsDirty.value = false
  actionForm.remark = ''
  syncSamplingForm(row)
  if (!row) return
  const [d, its] = await Promise.all([
    fetchInspectionDetail(row.inspectionId).catch(() => null),
    fetchInspectionItems(row.inspectionId).catch(() => null)
  ])
  if (d) detail.value = d.data ?? d
  if (its) items.value = (its.data ?? its) || []
}

// 操作后刷新列表并保留/重选当前质检单，让状态原地更新、可无缝接下一步
function syncSamplingForm(row) {
  if (!row) return
  const lotMatch = String(row.remark || '').match(/来料批次\s+(\d+)/)
  samplingForm.lotQuantity = lotMatch ? Number(lotMatch[1]) : Math.max(Number(row.sampleQuantity) || 1, 50)
  samplingForm.sampleQuantity = Number(row.sampleQuantity) || 1
  samplingForm.qualifiedQuantity = Number(row.qualifiedQuantity) || 0
  samplingForm.unqualifiedQuantity = Number(row.unqualifiedQuantity) || 0
}

function applyAqlSample() {
  const lot = samplingForm.lotQuantity || 50
  let sample = 2
  if (lot <= 50) sample = Math.max(2, Math.ceil(lot * 0.1))
  else if (lot <= 200) sample = Math.ceil(lot * 0.08)
  else if (lot <= 500) sample = Math.ceil(lot * 0.05)
  else sample = Math.ceil(lot * 0.03)
  samplingForm.sampleQuantity = Math.min(lot, Math.max(2, sample))
}

async function saveSampling() {
  if (!selected.value) return
  if (samplingForm.qualifiedQuantity + samplingForm.unqualifiedQuantity > samplingForm.sampleQuantity) {
    ElMessage.warning('合格数与不良数之和不能超过抽样数')
    return
  }
  samplingSaving.value = true
  try {
    await updateInspectionSampling({
      inspectionId: selected.value.inspectionId,
      sampleQuantity: samplingForm.sampleQuantity,
      qualifiedQuantity: samplingForm.qualifiedQuantity,
      unqualifiedQuantity: samplingForm.unqualifiedQuantity
    })
    ElMessage.success('抽样数据已保存')
    await reselect(selected.value.inspectionId)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    samplingSaving.value = false
  }
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
  if (!incomingForm.batchNo.trim()) { ElMessage.warning('请填写批次号'); return }
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
    if (row) {
      tableRef.value?.setCurrentRow(row)
      await onSelect(row)
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '登记失败')
  } finally {
    incomingSaving.value = false
  }
}

async function reselect(inspectionId) {
  await loadData()
  const row = list.value.find((r) => r.inspectionId === inspectionId)
  if (row) {
    await onSelect(row)
    tableRef.value?.setCurrentRow(row)
  } else {
    selected.value = null
    tableRef.value?.setCurrentRow(null)
  }
}

async function doGenerateItems() {
  itemsLoading.value = true
  try {
    const res = await generateDefaultItems(selected.value.inspectionId)
    const generated = res.data ?? res
    items.value = generated.map(i => ({
      ...i,
      resultCn: resultCn(i.result),
      defectLevelCn: dlCn(i.defectLevel)
    }))
    itemsDirty.value = false
    ElMessage.success('已生成默认检测项，请填写实测值后保存')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '生成失败')
  } finally {
    itemsLoading.value = false
  }
}

async function doSaveItems() {
  savingItems.value = true
  try {
    await saveInspectionItems(selected.value.inspectionId, items.value)
    itemsDirty.value = false
    ElMessage.success('检测项已保存')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    savingItems.value = false
  }
}

async function doEvaluate() {
  evaluating.value = true
  try {
    const res = await evaluateInspection(selected.value.inspectionId)
    evalResult.value = res.data ?? res
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '判定失败')
  } finally {
    evaluating.value = false
  }
}

// 保存检测项后立即汇总判定，减少来回点击
async function doSaveAndEvaluate() {
  await doSaveItems()
  if (!itemsDirty.value) await doEvaluate()
}

// 依据汇总建议直接进入对应判定动作，打通「评估→判定」链路
function applySuggestion() {
  const s = evalResult.value?.suggestedResult
  if (s === 'PASSED') doPass()
  else if (s === 'FAILED') openFailDialog()
  else if (s === 'RECHECK_REQUIRED') openRecheckDialog()
  else ElMessage.warning('仍有检测项未录入，请先完成录入')
}

async function doPass() {
  await ElMessageBox.confirm('确认该批次质检通过？', '质检通过', { type: 'success' })
  acting.value = true
  try {
    await passInspection({ inspectionId: selected.value.inspectionId, remark: actionForm.remark, operator: userStore.userInfo?.username })
    ElMessage.success('质检通过')
    await reselect(selected.value.inspectionId)
  } catch (e) { ElMessage.error(e?.response?.data?.message || e?.message || '操作失败') }
  finally { acting.value = false }
}

function openFailDialog() {
  Object.assign(failForm, { defectReason: '', defectType: '外观缺陷', defectQuantity: 1, severity: 'GENERAL', remark: '' })
  failDialog.value = true
}

async function doFail() {
  if (!failForm.defectReason.trim()) { ElMessage.warning('请填写不良原因'); return }
  acting.value = true
  try {
    await failInspection({ inspectionId: selected.value.inspectionId, ...failForm, operator: userStore.userInfo?.username })
    ElMessage.warning('质检不通过，已生成不良品记录')
    failDialog.value = false; await reselect(selected.value.inspectionId)
  } catch (e) { ElMessage.error(e?.response?.data?.message || e?.message || '操作失败') }
  finally { acting.value = false }
}

function openRecheckDialog() { recheckReason.value = ''; recheckDialog.value = true }

async function doRecheck() {
  acting.value = true
  try {
    await requireRecheck({ inspectionId: selected.value.inspectionId, reason: recheckReason.value, operator: userStore.userInfo?.username })
    ElMessage.success('已转复检')
    recheckDialog.value = false; await reselect(selected.value.inspectionId)
  } catch (e) { ElMessage.error(e?.response?.data?.message || e?.message || '操作失败') }
  finally { acting.value = false }
}

async function doRecheckPass() {
  await ElMessageBox.confirm('确认复检通过？', '复检通过', { type: 'success' })
  acting.value = true
  try {
    await recheckPass({ inspectionId: selected.value.inspectionId, remark: actionForm.remark, operator: userStore.userInfo?.username })
    ElMessage.success('复检通过')
    await reselect(selected.value.inspectionId)
  } catch (e) { ElMessage.error(e?.response?.data?.message || e?.message || '操作失败') }
  finally { acting.value = false }
}

function openRecheckFailDialog() {
  Object.assign(recheckFailForm, { defectReason: '', defectType: '外观缺陷', defectQuantity: 1, severity: 'GENERAL', remark: '' })
  recheckFailDialog.value = true
}

async function doRecheckFail() {
  if (!recheckFailForm.defectReason.trim()) { ElMessage.warning('请填写不良原因'); return }
  acting.value = true
  try {
    await recheckFail({ inspectionId: selected.value.inspectionId, ...recheckFailForm, operator: userStore.userInfo?.username })
    ElMessage.warning('复检不通过，已更新不良品记录')
    recheckFailDialog.value = false; await reselect(selected.value.inspectionId)
  } catch (e) { ElMessage.error(e?.response?.data?.message || e?.message || '操作失败') }
  finally { acting.value = false }
}

async function doClose() {
  await ElMessageBox.confirm('确认关闭质检单？关闭后不可再判定。', '关闭', { type: 'warning' })
  acting.value = true
  try {
    await closeInspection({ inspectionId: selected.value.inspectionId, remark: actionForm.remark || '手动关闭', operator: userStore.userInfo?.username })
    ElMessage.success('质检单已关闭')
    await reselect(selected.value.inspectionId)
  } catch (e) { ElMessage.error(e?.response?.data?.message || e?.message || '操作失败') }
  finally { acting.value = false }
}

function resultCn(s) {
  return { PASSED: '合格', FAILED: '不合格', WARNING: '警告', PENDING: '待检' }[s] || '待检'
}
function dlCn(s) {
  return { MINOR: '轻微', MAJOR: '严重', CRITICAL: '致命' }[s] || ''
}

onMounted(loadData)

// 路由切换时（semi ↔ fp）重置品类过滤并刷新数据
watch(routeCategory, (val) => {
  categoryFilter.value = val
  tabActive.value = val ? (val === 'FINISHED_PRODUCT' ? 'PENDING' : 'all') : 'PENDING'
  selected.value = null
  loadData()
})
</script>

<style scoped>
.sampling-hint {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}
.items-footer { display: flex; align-items: center; margin-top: 8px; flex-wrap: wrap; gap: 6px; }
.action-row   { display: flex; gap: 8px; flex-wrap: wrap; }
.empty-hint   { color: #909399; font-size: 12px; padding: 8px 0; }
</style>
