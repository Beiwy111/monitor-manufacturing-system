<template>
  <div class="print-page">
    <div class="page-header">
      <span class="page-title">报表打印</span>
      <span class="page-sub">选择打印类型，预览后点击打印</span>
    </div>

    <div class="card-grid">
      <div class="print-card" v-for="item in printTypes" :key="item.key" @click="openPreview(item)">
        <div class="card-icon" :style="{ background: item.bg }">{{ item.icon }}</div>
        <div class="card-body">
          <div class="card-title">{{ item.title }}</div>
          <div class="card-desc">{{ item.desc }}</div>
        </div>
        <el-tag :type="item.tagType" size="small" class="card-tag">{{ item.tag }}</el-tag>
      </div>
    </div>

    <el-dialog v-model="previewVisible" :title="current?.title" width="820px"
      :close-on-click-modal="false">
      <div class="toolbar-bar">
        <el-select v-if="current?.key==='inspection-report'" v-model="selectedId"
          placeholder="选择质检单" size="small" style="width:220px" @change="loadDetail">
          <el-option v-for="r in inspectionList" :key="r.inspectionId"
            :label="r.inspectionNo + ' - ' + (r.materialName||'')" :value="r.inspectionId" />
        </el-select>
        <el-select v-if="current?.key==='nonconforming-report'" v-model="selectedNcId"
          placeholder="选择不良品记录" size="small" style="width:220px" @change="loadNcDetail">
          <el-option v-for="r in ncList" :key="r.nonconformingId"
            :label="r.nonconformingNo + ' - ' + (r.defectType||'')" :value="r.nonconformingId" />
        </el-select>
        <el-button type="primary" size="small" :icon="Printer" @click="doPrint" style="margin-left:auto">
          打印
        </el-button>
        <el-button v-if="current?.key==='inspection-report'" type="success" size="small" @click="doExportExcel">
          导出 Excel
        </el-button>
        <el-button v-if="current?.key==='inspection-report' && detail" type="warning" size="small"
          :loading="aiLoading" :disabled="!canAiReport" @click="doGenerateAi">
          AI 质检报告
        </el-button>
        <el-button v-if="current?.key==='inspection-list'" type="success" size="small" @click="doExportListExcel">
          导出 Excel
        </el-button>
      </div>

      <div id="print-area" class="print-area" v-loading="detailLoading">
        <!-- 质检报告 -->
        <template v-if="current?.key==='inspection-report' && detail">
          <div class="report-header">
            <div class="report-logo">显示器制造 MES</div>
            <div class="report-title">质量检验报告</div>
            <div class="report-no">报告编号：{{ detail.inspectionNo }}</div>
          </div>
          <table class="info-table">
            <tr>
              <td class="lbl">质检单号</td><td>{{ detail.inspectionNo }}</td>
              <td class="lbl">检验类型</td><td>{{ detail.inspectionTypeCn }}</td>
            </tr>
            <tr>
              <td class="lbl">产品/物料</td><td>{{ detail.materialName || '-' }}</td>
              <td class="lbl">批次号</td><td>{{ detail.batchNo }}</td>
            </tr>
            <tr>
              <td class="lbl">工单号</td><td>{{ detail.workOrderNo || '-' }}</td>
              <td class="lbl">产品类别</td><td>{{ detail.inspectionCategoryCn }}</td>
            </tr>
            <tr>
              <td class="lbl">送检数量</td><td>{{ detail.sampleQuantity }}</td>
              <td class="lbl">合格数量</td><td class="pass">{{ detail.qualifiedQuantity }}</td>
            </tr>
            <tr>
              <td class="lbl">不良数量</td>
              <td :class="Number(detail.unqualifiedQuantity)>0?'fail':''">{{ detail.unqualifiedQuantity }}</td>
              <td class="lbl">合格率</td>
              <td :class="yieldRate(detail)>=95?'pass':'fail'">{{ yieldRate(detail) }}%</td>
            </tr>
            <tr>
              <td class="lbl">检验状态</td><td>{{ detail.inspectionStatusCn }}</td>
              <td class="lbl">检验时间</td><td>{{ detail.inspectedAt }}</td>
            </tr>
          </table>
          <div v-if="items.length" class="section-title">检测项明细</div>
          <table v-if="items.length" class="data-table">
            <thead>
              <tr><th>检测项目</th><th>标准值</th><th>单位</th><th>实测值</th><th>检测结果</th><th>备注</th></tr>
            </thead>
            <tbody>
              <tr v-for="it in items" :key="it.itemId">
                <td>{{ it.itemName }}</td>
                <td>{{ it.standardValue || '-' }}</td>
                <td>{{ it.unit || '-' }}</td>
                <td>{{ it.measuredValue || '-' }}</td>
                <td :class="it.result==='PASSED'?'pass':it.result==='FAILED'?'fail':''">
                  {{ {PASSED:'✓ 通过',FAILED:'✗ 不通过',WARNING:'⚠ 警告',PENDING:'待录入'}[it.result]||it.result }}
                </td>
                <td>{{ it.remark || '' }}</td>
              </tr>
            </tbody>
          </table>
          <div class="sign-row">
            <span>检验员：_______________</span>
            <span>审核：_______________</span>
            <span>日期：{{ today }}</span>
          </div>
          <div v-if="aiReport" class="section-title">AI 质量分析报告</div>
          <div v-if="aiReport" class="ai-report-block">
            <p v-for="(text, key) in (aiReport.analysisSections || {})" :key="key">
              <strong>【{{ key }}】</strong>{{ text }}
            </p>
            <p v-if="!Object.keys(aiReport.analysisSections || {}).length">{{ aiReport.aiAnalysis }}</p>
          </div>
        </template>

        <!-- 质检记录汇总 -->
        <template v-if="current?.key==='inspection-list'">
          <div class="report-header">
            <div class="report-logo">显示器制造 MES</div>
            <div class="report-title">质检记录汇总表</div>
            <div class="report-no">打印时间：{{ now }}</div>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>质检单号</th><th>物料/产品</th><th>批次</th><th>类别</th>
                <th>送检</th><th>合格</th><th>不良</th><th>合格率</th><th>状态</th><th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in inspectionList" :key="r.inspectionId">
                <td>{{ r.inspectionNo }}</td><td>{{ r.materialName }}</td>
                <td>{{ r.batchNo }}</td><td>{{ r.inspectionCategoryCn }}</td>
                <td>{{ r.sampleQuantity }}</td>
                <td class="pass">{{ r.qualifiedQuantity }}</td>
                <td :class="Number(r.unqualifiedQuantity)>0?'fail':''">{{ r.unqualifiedQuantity }}</td>
                <td :class="yieldRate(r)>=95?'pass':'fail'">{{ yieldRate(r) }}%</td>
                <td>{{ r.inspectionStatusCn }}</td><td>{{ r.inspectedAt }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td colspan="4" style="text-align:right;font-weight:600">合计</td>
                <td>{{ sumOf(inspectionList,'sampleQuantity') }}</td>
                <td class="pass">{{ sumOf(inspectionList,'qualifiedQuantity') }}</td>
                <td class="fail">{{ sumOf(inspectionList,'unqualifiedQuantity') }}</td>
                <td>{{ avgYield }}%</td><td colspan="2"></td>
              </tr>
            </tfoot>
          </table>
        </template>

        <!-- 不合格品处理报告 -->
        <template v-if="current?.key==='nonconforming-report' && ncDetail">
          <div class="report-header">
            <div class="report-logo">显示器制造 MES</div>
            <div class="report-title">不合格品处理报告</div>
            <div class="report-no">报告编号：{{ ncDetail.nonconformingNo }}</div>
          </div>
          <table class="info-table">
            <tr>
              <td class="lbl">不良品单号</td><td>{{ ncDetail.nonconformingNo }}</td>
              <td class="lbl">关联质检单</td><td>{{ ncDetail.inspectionNo || '-' }}</td>
            </tr>
            <tr>
              <td class="lbl">物料/产品</td><td>{{ ncDetail.materialName || '-' }}</td>
              <td class="lbl">批次号</td><td>{{ ncDetail.batchNo || '-' }}</td>
            </tr>
            <tr>
              <td class="lbl">缺陷类型</td><td>{{ ncDetail.defectType }}</td>
              <td class="lbl">严重程度</td>
              <td :class="['MAJOR','CRITICAL'].includes(ncDetail.severity)?'fail':'warn'">{{ ncDetail.severityCn }}</td>
            </tr>
            <tr>
              <td class="lbl">不良数量</td><td class="fail">{{ ncDetail.quantity }}</td>
              <td class="lbl">处置方式</td><td>{{ ncDetail.handleMethodCn }}</td>
            </tr>
            <tr>
              <td class="lbl">处置状态</td><td>{{ ncDetail.handleStatusCn }}</td>
              <td class="lbl">登记时间</td><td>{{ ncDetail.registeredAt }}</td>
            </tr>
            <tr v-if="ncDetail.defectDescription">
              <td class="lbl">缺陷描述</td><td colspan="3">{{ ncDetail.defectDescription }}</td>
            </tr>
          </table>
          <div class="sign-row">
            <span>登记人：_______________</span>
            <span>处置人：_______________</span>
            <span>日期：{{ today }}</span>
          </div>
        </template>

        <!-- 不合格品清单 -->
        <template v-if="current?.key==='nonconforming-list'">
          <div class="report-header">
            <div class="report-logo">显示器制造 MES</div>
            <div class="report-title">不合格品清单</div>
            <div class="report-no">打印时间：{{ now }}</div>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>不良品单号</th><th>关联质检</th><th>缺陷类型</th><th>数量</th>
                <th>严重程度</th><th>处置方式</th><th>状态</th><th>登记时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in ncList" :key="r.nonconformingId">
                <td>{{ r.nonconformingNo }}</td><td>{{ r.inspectionNo || '-' }}</td>
                <td>{{ r.defectType }}</td><td class="fail">{{ r.quantity }}</td>
                <td :class="['MAJOR','CRITICAL'].includes(r.severity)?'fail':'warn'">{{ r.severityCn }}</td>
                <td>{{ r.handleMethodCn }}</td><td>{{ r.handleStatusCn }}</td>
                <td>{{ r.registeredAt }}</td>
              </tr>
            </tbody>
          </table>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Printer } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { fetchInspectionViews, fetchInspectionDetail, fetchInspectionItems, fetchNonconformingViews } from '@/api/quality'
import { postRefreshQualityReport } from '@/api/mes'
import { useUserStore } from '@/stores/user'
import { exportInspectionReportExcel, exportInspectionBatchExcel } from '@/utils/qualityExcelExport'

const userStore = useUserStore()
const route = useRoute()
const routeCategory = computed(() => route.meta?.category || '')

const inspectionList = ref([])
const ncList         = ref([])
const previewVisible = ref(false)
const detailLoading  = ref(false)
const current        = ref(null)
const selectedId     = ref(null)
const selectedNcId   = ref(null)
const detail         = ref(null)
const ncDetail       = ref(null)
const items          = ref([])
const aiLoading      = ref(false)
const aiReport       = ref(null)

const canAiReport = computed(() =>
  detail.value && !['PENDING', 'RECHECK_REQUIRED'].includes(detail.value.inspectionStatus)
)

const today = new Date().toLocaleDateString('zh-CN')
const now   = new Date().toLocaleString('zh-CN')

const printTypes = [
  { key: 'inspection-report',   title: '质检报告',        tag: '单份', tagType: 'primary', icon: '📋', bg: '#ecf5ff', desc: '选择质检单，打印包含检测项明细的完整质量检验报告' },
  { key: 'inspection-list',     title: '质检记录汇总',    tag: '批量', tagType: 'success', icon: '📊', bg: '#f0f9eb', desc: '打印全部质检记录汇总表，含合格率统计和合计行' },
  { key: 'nonconforming-report',title: '不合格品处理报告',tag: '单份', tagType: 'danger',  icon: '⚠️', bg: '#fef0f0', desc: '选择不良品记录，打印包含缺陷信息和处置跟踪的报告' },
  { key: 'nonconforming-list',  title: '不合格品清单',    tag: '批量', tagType: 'warning', icon: '📝', bg: '#fdf6ec', desc: '打印全部不合格品汇总清单，便于质量例会使用' },
]

const avgYield = computed(() => {
  const list = inspectionList.value.filter(r => Number(r.sampleQuantity) > 0)
  if (!list.length) return '0.0'
  return (list.reduce((s, r) => s + yieldRate(r), 0) / list.length).toFixed(1)
})

function yieldRate(row) {
  const s = Number(row.sampleQuantity)
  const q = Number(row.qualifiedQuantity)
  if (!s || s <= 0) return 0
  if (!Number.isFinite(q) || q <= 0) return 0
  const r = Math.round(q / s * 100)
  return Math.min(100, Math.max(0, r))
}
function sumOf(list, key) {
  return list.reduce((s, r) => s + (Number(r[key]) || 0), 0)
}

async function openPreview(item) {
  current.value        = item
  detail.value         = null
  ncDetail.value       = null
  items.value          = []
  selectedId.value     = inspectionList.value[0]?.inspectionId ?? null
  selectedNcId.value   = ncList.value[0]?.nonconformingId ?? null
  previewVisible.value = true
  if (item.key === 'inspection-report' && selectedId.value) await loadDetail(selectedId.value)
  if (item.key === 'nonconforming-report' && selectedNcId.value) loadNcDetail(selectedNcId.value)
}

async function loadDetail(id) {
  if (!id) return
  detailLoading.value = true
  aiReport.value = null
  try {
    const [d, it] = await Promise.all([
      fetchInspectionDetail(id).catch(() => null),
      fetchInspectionItems(id).catch(() => null)
    ])
    const base = inspectionList.value.find(r => r.inspectionId === id) || {}
    detail.value = { ...base, ...(d ? (d.data ?? d) : {}) }
    items.value  = it ? (it.data ?? it) : []
  } finally { detailLoading.value = false }
}

function loadNcDetail(id) {
  ncDetail.value = ncList.value.find(r => r.nonconformingId === id) || null
}

function doPrint() {
  const el = document.getElementById('print-area')
  if (!el) return
  const win = window.open('', '_blank', 'width=920,height=720')
  win.document.write(`<html><head><title>${current.value?.title || '质检报表'}</title>
    <style>
      body{font-family:'SimSun',serif;font-size:12px;color:#000;margin:20px}
      .report-header{text-align:center;margin-bottom:16px;border-bottom:2px solid #000;padding-bottom:8px}
      .report-logo{font-size:11px;color:#555}.report-title{font-size:18px;font-weight:bold;margin:6px 0}
      .report-no{font-size:11px;color:#555}
      .info-table{width:100%;border-collapse:collapse;margin-bottom:14px}
      .info-table td{border:1px solid #999;padding:5px 8px}
      .info-table .lbl{background:#f5f5f5;font-weight:bold;width:12%}
      .data-table{width:100%;border-collapse:collapse;margin-bottom:14px;font-size:11px}
      .data-table th{background:#e0e0e0;border:1px solid #999;padding:4px 6px;text-align:center}
      .data-table td{border:1px solid #ccc;padding:4px 6px}
      .data-table tfoot td{background:#f0f0f0;font-weight:bold;border:1px solid #999;padding:4px 6px}
      .section-title{font-weight:bold;font-size:13px;margin:12px 0 6px;border-left:3px solid #333;padding-left:8px}
      .pass{color:#267326;font-weight:bold}.fail{color:#cc0000;font-weight:bold}.warn{color:#b36b00;font-weight:bold}
      .sign-row{margin-top:24px;display:flex;justify-content:space-between;border-top:1px solid #ccc;padding-top:12px}
    </style></head><body>${el.innerHTML}</body></html>`)
  win.document.close()
  win.focus()
  setTimeout(() => { win.print(); win.close() }, 400)
  ElMessage.success('打印窗口已打开')
}

function doExportExcel() {
  if (!detail.value) return
  exportInspectionReportExcel({ detail: detail.value, items: items.value, aiReport: aiReport.value })
  ElMessage.success('质检报告已导出 Excel')
}

function doExportListExcel() {
  exportInspectionBatchExcel(inspectionList.value, routeCategory.value === 'FINISHED_PRODUCT' ? '成品质检汇总' : '物料质检汇总')
  ElMessage.success('汇总表已导出 Excel')
}

async function doGenerateAi() {
  if (!detail.value?.inspectionNo) return
  aiLoading.value = true
  try {
    const res = await postRefreshQualityReport({
      qcId: detail.value.inspectionNo,
      operator: userStore.userInfo?.username,
      roleKey: 'quality'
    })
    aiReport.value = res
    ElMessage.success(aiReport.value?.aiGenerated ? '千问 AI 报告已生成' : '报告已生成')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '生成失败')
  } finally {
    aiLoading.value = false
  }
}

async function loadLists() {
  const [iv, nv] = await Promise.all([
    fetchInspectionViews().catch(() => null),
    fetchNonconformingViews().catch(() => null)
  ])
  const cat = routeCategory.value
  if (iv) {
    const all = iv.data ?? iv
    inspectionList.value = cat ? all.filter(r => r.inspectionCategory === cat) : all
  }
  if (nv) {
    const allNc = nv.data ?? nv
    ncList.value = cat ? allNc.filter(r => r.inspectionCategory === cat) : allNc
  }
}

onMounted(loadLists)

watch(routeCategory, () => {
  previewVisible.value = false
  loadLists()
})
</script>

<style scoped>
.print-page  { padding: 16px; background: #f5f7fa; min-height: 100%; }
.page-header { margin-bottom: 20px; }
.page-title  { font-size: 16px; font-weight: 700; color: #2c3e50; display: block; }
.page-sub    { font-size: 12px; color: #909399; margin-top: 4px; display: block; }

.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.print-card {
  background: #fff; border: 1px solid #e4e7ed; border-radius: 8px;
  padding: 18px 16px; cursor: pointer; display: flex; align-items: flex-start; gap: 14px;
  transition: box-shadow .2s, border-color .2s; position: relative;
}
.print-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,.1); border-color: #409eff; }
.card-icon  { width: 48px; height: 48px; border-radius: 10px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; font-size: 22px; }
.card-body  { flex: 1; }
.card-title { font-size: 14px; font-weight: 600; color: #2c3e50; margin-bottom: 5px; }
.card-desc  { font-size: 12px; color: #8492a6; line-height: 1.5; }
.card-tag   { position: absolute; top: 14px; right: 14px; }

.toolbar-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; padding-bottom: 12px; border-bottom: 1px solid #ebeef5; }
.print-area  { max-height: 520px; overflow-y: auto; padding: 10px 4px; }

.report-header { text-align: center; margin-bottom: 16px; border-bottom: 2px solid #303133; padding-bottom: 10px; }
.report-logo   { font-size: 11px; color: #909399; }
.report-title  { font-size: 20px; font-weight: 700; color: #1a1a2e; margin: 6px 0; letter-spacing: 2px; }
.report-no     { font-size: 11px; color: #909399; }

.info-table    { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
.info-table td { border: 1px solid #dcdfe6; padding: 7px 10px; font-size: 13px; }
.info-table .lbl { background: #f5f7fa; font-weight: 600; color: #606266; width: 12%; }

.section-title { font-weight: 600; font-size: 13px; color: #2c3e50; margin: 14px 0 8px; padding-left: 8px; border-left: 3px solid #409eff; }
.data-table    { width: 100%; border-collapse: collapse; font-size: 12px; margin-bottom: 16px; }
.data-table th { background: #eef2f7; border: 1px solid #dcdfe6; padding: 6px 8px; text-align: center; font-weight: 600; color: #2c3e50; }
.data-table td { border: 1px solid #ebeef5; padding: 6px 8px; }
.data-table tfoot td { background: #f5f7fa; font-weight: 600; border: 1px solid #dcdfe6; padding: 6px 8px; }

.pass { color: #67c23a; font-weight: 600; }
.fail { color: #f56c6c; font-weight: 600; }
.warn { color: #e6a23c; font-weight: 600; }

.sign-row { margin-top: 24px; display: flex; justify-content: space-between; border-top: 1px dashed #dcdfe6; padding-top: 14px; font-size: 13px; color: #606266; }
.ai-report-block { font-size: 13px; line-height: 1.7; color: #4a5568; padding: 8px 0; }
.ai-report-block p { margin: 0 0 8px; }
</style>