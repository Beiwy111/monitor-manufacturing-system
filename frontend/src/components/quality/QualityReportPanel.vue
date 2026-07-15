<template>
  <div class="qc-report-panel">
    <div class="qc-report-panel__toolbar">
      <span class="qc-report-panel__title">批次质检报告</span>
      <el-button size="small" type="success" :icon="Download" :disabled="!detail" @click="doExportExcel">
        导出 Excel
      </el-button>
      <el-button size="small" type="primary" :loading="aiLoading" :disabled="!canGenerateAi" @click="doGenerateAi">
        {{ aiReport?.aiGenerated ? '重新生成 AI 报告' : '生成 AI 质检报告' }}
      </el-button>
      <el-tag v-if="aiReport?.aiGenerated" type="success" size="small">千问 AI</el-tag>
      <el-tag v-else-if="aiReport" type="info" size="small">模板报告</el-tag>
    </div>

    <div v-if="detail" class="qc-report-panel__summary">
      <div class="qc-report-panel__stat">
        <em>{{ detail.sampleQuantity ?? 0 }}</em><span>抽检数</span>
      </div>
      <div class="qc-report-panel__stat qc-report-panel__stat--ok">
        <em>{{ detail.qualifiedQuantity ?? 0 }}</em><span>合格台数</span>
      </div>
      <div class="qc-report-panel__stat qc-report-panel__stat--bad">
        <em>{{ detail.unqualifiedQuantity ?? 0 }}</em><span>不合格台数</span>
      </div>
      <div class="qc-report-panel__stat">
        <em>{{ yieldPct }}%</em><span>合格率</span>
      </div>
    </div>

    <div v-if="unitMatrix?.rows?.length" class="qc-report-panel__matrix">
      <div class="qc-report-panel__matrix-hd">逐台五步检测明细（任一道工序不合格则该台不合格）</div>
      <el-table :data="unitMatrix.rows" border size="small" style="width:100%">
        <el-table-column prop="unitNo" label="#" width="48" align="center" />
        <el-table-column prop="serialNo" label="序列号" width="140" />
        <el-table-column
          v-for="(title, idx) in unitMatrix.stationTitles"
          :key="unitMatrix.stationIds[idx]"
          :label="title"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              v-if="row[unitMatrix.stationIds[idx]] === true"
              type="success"
              size="small"
            >合格</el-tag>
            <el-tag
              v-else-if="row[unitMatrix.stationIds[idx]] === false"
              type="danger"
              size="small"
            >不合格</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="综合" width="88" align="center" fixed="right">
          <template #default="{ row }">
            <el-tag
              :type="row.overall === 'PASS' ? 'success' : row.overall === 'FAIL' ? 'danger' : 'info'"
              size="small"
            >{{ row.overall === 'PASS' ? '合格' : row.overall === 'FAIL' ? '不合格' : '未完成' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="items.length" class="qc-report-panel__charts">
      <div class="qc-report-panel__chart-box">
        <div class="qc-report-panel__chart-hd">检测项结果分布</div>
        <BoardChart :option="itemBarOption" class="qc-report-panel__chart" />
      </div>
      <div class="qc-report-panel__chart-box">
        <div class="qc-report-panel__chart-hd">合格 / 不良占比</div>
        <BoardChart :option="yieldPieOption" class="qc-report-panel__chart" />
      </div>
    </div>

    <div v-if="aiReport" class="qc-report-panel__ai" v-loading="aiLoading">
      <div class="qc-report-panel__ai-hd">
        质量分析报告
        <span class="qc-report-panel__ai-src">{{ aiReport.aiGenerated ? '由通义千问生成' : '系统模板生成' }}</span>
      </div>
      <template v-if="aiSections.length">
        <div v-for="sec in aiSections" :key="sec.key" class="qc-report-panel__ai-sec">
          <div class="qc-report-panel__ai-sec-hd">【{{ sec.key }}】</div>
          <p>{{ sec.text }}</p>
        </div>
      </template>
      <p v-else class="qc-report-panel__ai-text">{{ aiReport.aiAnalysis || aiReport.conclusion }}</p>
    </div>
    <p v-else-if="canGenerateAi" class="qc-report-panel__hint">
      {{ props.optionalAi ? 'AI 质检报告为可选项，无需生成也可直接质检通过' : '检验完成后，点击「生成 AI 质检报告」获取千问智能分析' }}
    </p>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import BoardChart from '@/components/board/BoardChart.vue'
import { postRefreshQualityReport } from '@/api/mes'
import { exportInspectionReportExcel } from '@/utils/qualityExcelExport'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  detail: { type: Object, default: null },
  items: { type: Array, default: () => [] },
  unitMatrix: { type: Object, default: null },
  optionalAi: { type: Boolean, default: false },
  beforeGenerateAi: { type: Function, default: null }
})

const userStore = useUserStore()
const aiReport = ref(null)
const aiLoading = ref(false)

const canGenerateAi = computed(() => !!(props.detail?.inspectionNo || props.detail?.inspectionId))

const yieldPct = computed(() => {
  const s = Number(props.detail?.sampleQuantity)
  const q = Number(props.detail?.qualifiedQuantity)
  if (!s) return '0.0'
  return Math.min(100, Math.max(0, Math.round((q / s) * 1000) / 10)).toFixed(1)
})

const itemBarOption = computed(() => {
  const passed = props.items.filter((i) => i.result === 'PASSED').length
  const failed = props.items.filter((i) => i.result === 'FAILED').length
  const warning = props.items.filter((i) => i.result === 'WARNING').length
  const pending = props.items.filter((i) => !i.result || i.result === 'PENDING').length
  return {
    grid: { left: 8, right: 8, top: 28, bottom: 8, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['合格', '不合格', '警告', '待检'], axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar', barWidth: '50%',
      data: [
        { value: passed, itemStyle: { color: '#67c23a' } },
        { value: failed, itemStyle: { color: '#f56c6c' } },
        { value: warning, itemStyle: { color: '#e6a23c' } },
        { value: pending, itemStyle: { color: '#c0c4cc' } }
      ],
      label: { show: true, position: 'top', fontSize: 11 }
    }]
  }
})

const yieldPieOption = computed(() => {
  const q = Number(props.detail?.qualifiedQuantity) || 0
  const u = Number(props.detail?.unqualifiedQuantity) || 0
  const data = []
  if (q > 0) data.push({ name: '合格', value: q, itemStyle: { color: '#67c23a' } })
  if (u > 0) data.push({ name: '不良', value: u, itemStyle: { color: '#f56c6c' } })
  if (!data.length) data.push({ name: '待统计', value: 1, itemStyle: { color: '#dcdfe6' } })
  return {
    tooltip: { trigger: 'item', formatter: '{b}：{c} 件（{d}%）' },
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['50%', '52%'],
      label: { fontSize: 11 }, data
    }]
  }
})

const aiSections = computed(() => {
  const s = aiReport.value?.analysisSections
  if (!s || typeof s !== 'object') return []
  return Object.entries(s).map(([key, text]) => ({ key, text }))
})

function doExportExcel() {
  try {
    if (!props.detail) {
      ElMessage.warning('暂无报告数据可导出')
      return
    }
    exportInspectionReportExcel({
      detail: props.detail,
      items: props.items,
      aiReport: aiReport.value,
      unitMatrix: props.unitMatrix
    })
    ElMessage.success('质检报告已导出 Excel')
  } catch (e) {
    console.error('export excel failed:', e)
    ElMessage.error(e?.message || '导出 Excel 失败')
  }
}

async function doGenerateAi() {
  const qcId = props.detail?.inspectionNo || props.detail?.inspectionId
  if (!qcId) return
  aiLoading.value = true
  try {
    if (props.beforeGenerateAi) {
      await props.beforeGenerateAi()
    }
    const res = await postRefreshQualityReport({
      qcId: String(qcId),
      operator: userStore.userInfo?.username,
      roleKey: 'quality'
    })
    aiReport.value = res
    ElMessage.success(aiReport.value?.aiGenerated ? '千问 AI 质检报告已生成' : '质检报告已生成（模板）')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || 'AI 报告生成失败')
  } finally {
    aiLoading.value = false
  }
}

watch(() => props.detail?.inspectionId, () => { aiReport.value = null })
</script>

<style scoped>
.qc-report-panel {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
  background: #fafbfc;
}

.qc-report-panel__toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.qc-report-panel__title {
  font-weight: 600;
  color: #303133;
  margin-right: 4px;
}

.qc-report-panel__summary {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.qc-report-panel__matrix {
  margin-bottom: 12px;
}

.qc-report-panel__matrix-hd {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.qc-report-panel__stat {
  flex: 1;
  text-align: center;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px 4px;
}

.qc-report-panel__stat em {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  font-style: normal;
}

.qc-report-panel__stat span {
  font-size: 11px;
  color: #909399;
}

.qc-report-panel__stat--ok em { color: #67c23a; }
.qc-report-panel__stat--bad em { color: #f56c6c; }

.qc-report-panel__charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 12px;
}

.qc-report-panel__chart-box {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px;
}

.qc-report-panel__chart-hd {
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
}

.qc-report-panel__chart {
  height: 160px;
}

.qc-report-panel__ai {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
}

.qc-report-panel__ai-hd {
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.qc-report-panel__ai-src {
  font-size: 11px;
  color: #909399;
  font-weight: 400;
  margin-left: 8px;
}

.qc-report-panel__ai-sec {
  margin-bottom: 10px;
}

.qc-report-panel__ai-sec-hd {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 4px;
}

.qc-report-panel__ai-sec p,
.qc-report-panel__ai-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: #4a5568;
}

.qc-report-panel__hint {
  font-size: 12px;
  color: #909399;
  margin: 0;
}
</style>
