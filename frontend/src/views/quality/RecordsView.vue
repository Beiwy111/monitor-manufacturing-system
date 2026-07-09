<template>
  <div class="ruoyi-page quality-report-page">
    <div class="report-summary">
      <div class="summary-card">
        <span class="summary-card__label">自动质量报告</span>
        <strong>{{ reports.length }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-card__label">平均合格率</span>
        <strong>{{ avgYield }}%</strong>
      </div>
      <div class="summary-card">
        <span class="summary-card__label">不良总数</span>
        <strong>{{ defectTotal }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-card__label">AI 报告</span>
        <strong>{{ aiReportCount }}</strong>
      </div>
    </div>

    <div class="ruoyi-toolbar">
      <span class="ruoyi-toolbar__title">质检记录与自动报告</span>
      <el-button :loading="refreshing" @click="refreshAll">刷新列表</el-button>
      <span class="ruoyi-toolbar__meta">共 {{ reports.length }} 份报告</span>
    </div>

    <el-table :data="reports" border stripe highlight-current-row>
      <el-table-column prop="id" label="报告号" width="120" />
      <el-table-column prop="qcId" label="质检单" width="130" />
      <el-table-column prop="workOrderId" label="工单" width="130" />
      <el-table-column prop="productModel" label="型号" min-width="140" />
      <el-table-column prop="result" label="判定" width="90" />
      <el-table-column prop="yieldRate" label="合格率" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.yieldRate >= 95 ? 'success' : 'warning'" size="small">{{ row.yieldRate }}%</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.aiGenerated ? 'success' : 'info'" size="small">
            {{ row.aiGenerated ? '千问AI' : '规则模板' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="topDefect" label="主要不良" min-width="120" />
      <el-table-column prop="createdAt" label="生成时间" min-width="160" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openReport(row)">查看报告</el-button>
          <el-button link type="warning" :loading="refreshingId === row.qcId" @click="refreshReport(row)">
            刷新
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-divider content-position="left">质检明细</el-divider>
    <el-table :data="records" border stripe>
      <el-table-column prop="id" label="质检单" width="130" />
      <el-table-column prop="inspectorName" label="质检员" width="100" />
      <el-table-column prop="result" label="结果" width="90" />
      <el-table-column prop="qualifiedQty" label="合格数" width="90" align="right" />
      <el-table-column prop="unqualifiedQty" label="不合格数" width="100" align="right" />
      <el-table-column prop="createdAt" label="时间" min-width="160" />
      <el-table-column label="操作" width="72" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="removeInspection(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="reportVisible" title="智能质量报告" width="780px">
      <template v-if="currentReport">
        <div class="report-header">
          <el-tag :type="currentReport.aiGenerated ? 'success' : 'info'" size="small">
            {{ currentReport.aiGenerated ? '千问 AI 生成' : '规则模板生成' }}
          </el-tag>
          <el-button
            type="warning"
            size="small"
            :loading="refreshingId === currentReport.qcId"
            @click="refreshReport(currentReport)"
          >
            重新生成报告
          </el-button>
        </div>

        <el-descriptions :column="2" border size="small" class="report-desc">
          <el-descriptions-item label="报告号">{{ currentReport.id }}</el-descriptions-item>
          <el-descriptions-item label="质检单">{{ currentReport.qcId }}</el-descriptions-item>
          <el-descriptions-item label="工单">{{ currentReport.workOrderId }}</el-descriptions-item>
          <el-descriptions-item label="批次">{{ currentReport.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="产品型号">{{ currentReport.productModel }}</el-descriptions-item>
          <el-descriptions-item label="判定结果">{{ currentReport.result }}</el-descriptions-item>
        </el-descriptions>

        <div class="report-block">
          <div class="report-block__title">本批次统计</div>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="检测总数">{{ currentReport.sampleQty }}</el-descriptions-item>
            <el-descriptions-item label="合格数">{{ currentReport.qualifiedQty }}</el-descriptions-item>
            <el-descriptions-item label="不合格数">{{ currentReport.unqualifiedQty }}</el-descriptions-item>
            <el-descriptions-item label="合格率">{{ currentReport.yieldRate }}%</el-descriptions-item>
            <el-descriptions-item label="主要缺陷" :span="2">{{ currentReport.topDefect }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="report-block">
          <div class="report-block__title">本日质量汇总</div>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="质检批次">{{ currentReport.dailyInspectionCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="检测总数">{{ currentReport.dailyTotalSample || 0 }}</el-descriptions-item>
            <el-descriptions-item label="合格率">{{ currentReport.dailyYieldRate || 0 }}%</el-descriptions-item>
            <el-descriptions-item label="合格数">{{ currentReport.dailyTotalQualified || 0 }}</el-descriptions-item>
            <el-descriptions-item label="不合格数">{{ currentReport.dailyTotalUnqualified || 0 }}</el-descriptions-item>
            <el-descriptions-item label="主要缺陷">{{ currentReport.dailyTopDefect || '-' }}</el-descriptions-item>
            <el-descriptions-item label="涉及工单" :span="3">
              {{ (currentReport.relatedWorkOrders || []).join('、') || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="report-block ai-block">
          <div class="report-block__title">
            AI 质量分析报告
            <span class="report-block__hint">（质量概况 · 主要问题 · 改进建议）</span>
          </div>
          <template v-if="analysisSections.length">
            <div v-for="section in analysisSections" :key="section.title" class="ai-section">
              <div class="ai-section__title">{{ section.title }}</div>
              <p>{{ section.content }}</p>
            </div>
          </template>
          <p v-else class="ai-fallback">{{ currentReport.aiAnalysis || currentReport.conclusion }}</p>
        </div>

        <div v-if="defectRows.length" class="report-block">
          <div class="report-block__title">不良分布</div>
          <el-table :data="defectRows" border size="small">
            <el-table-column prop="name" label="不良类型" />
            <el-table-column prop="value" label="数量" width="100" align="right" />
          </el-table>
        </div>

        <div v-if="currentReport.suggestions?.length" class="report-block">
          <div class="report-block__title">系统建议（规则引擎）</div>
          <ul>
            <li v-for="item in currentReport.suggestions" :key="item">{{ item }}</li>
          </ul>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { useMesDelete } from '@/composables/useMesDelete'

const mes = useMesStore()
const userStore = useUserStore()
const { runDelete } = useMesDelete(mes, userStore)
const reportVisible = ref(false)
const currentReport = ref(null)
const refreshing = ref(false)
const refreshingId = ref('')

const reports = computed(() => mes.qualityReports || [])
const records = computed(() => mes.inspections.filter((i) => i.status !== '待检'))
const avgYield = computed(() => {
  if (!reports.value.length) return 0
  const total = reports.value.reduce((sum, r) => sum + Number(r.yieldRate || 0), 0)
  return (total / reports.value.length).toFixed(1)
})
const defectTotal = computed(() => reports.value.reduce((sum, r) => sum + Number(r.defectQty || 0), 0))
const aiReportCount = computed(() => reports.value.filter((r) => r.aiGenerated).length)
const defectRows = computed(() => {
  const dist = currentReport.value?.defectDistribution || {}
  return Object.entries(dist).map(([name, value]) => ({ name, value }))
})
const analysisSections = computed(() => {
  const sections = currentReport.value?.analysisSections
  if (!sections || typeof sections !== 'object') return []
  return Object.entries(sections).map(([title, content]) => ({ title, content }))
})

onMounted(async () => {
  try {
    await mes.hydrateFromApi()
  } catch {
    /* ignore */
  }
})

function openReport(row) {
  currentReport.value = row
  reportVisible.value = true
}

async function refreshAll() {
  refreshing.value = true
  try {
    await mes.hydrateFromApi()
    ElMessage.success('报告列表已刷新')
  } catch (e) {
    ElMessage.error(e?.message || '刷新失败')
  } finally {
    refreshing.value = false
  }
}

async function refreshReport(row) {
  if (!row?.qcId) return
  refreshingId.value = row.qcId
  try {
    const updated = await mes.generateQualityReport(
      row.qcId,
      userStore.username,
      userStore.roleKey
    )
    if (updated) {
      currentReport.value = updated
      ElMessage.success(updated.aiGenerated ? '千问 AI 报告已重新生成' : '报告已重新生成（使用规则模板）')
    }
  } catch (e) {
    ElMessage.error(e?.message || '重新生成报告失败')
  } finally {
    refreshingId.value = ''
  }
}

function removeInspection(row) {
  if (!row) return
  runDelete({
    action: 'deleteInspection',
    payload: { qcId: row.id },
    message: `确定删除质检记录 ${row.id}？关联质量报告将一并清理。`
  }).catch(() => {})
}
</script>

<style scoped>
.quality-report-page {
  min-height: calc(100vh - 130px);
}
.report-summary {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
.summary-card {
  flex: 1;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}
.summary-card__label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.summary-card strong {
  font-size: 22px;
  color: #001b3f;
}
.ruoyi-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ruoyi-toolbar__meta {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}
.report-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.report-desc {
  margin-bottom: 4px;
}
.report-block {
  margin-top: 16px;
}
.report-block__title {
  font-size: 14px;
  font-weight: 600;
  color: #001b3f;
  margin-bottom: 8px;
}
.report-block__hint {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}
.report-block p,
.report-block li {
  font-size: 13px;
  line-height: 1.7;
  color: #606266;
}
.ai-block {
  padding: 12px 14px;
  background: #f6ffed;
  border: 1px solid #d9f7be;
  border-radius: 8px;
}
.ai-section {
  margin-bottom: 10px;
}
.ai-section__title {
  font-size: 13px;
  font-weight: 600;
  color: #389e0d;
  margin-bottom: 4px;
}
.ai-fallback {
  margin: 0;
  white-space: pre-wrap;
}
</style>
