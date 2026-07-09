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
    </div>

    <div class="ruoyi-toolbar">
      <span class="ruoyi-toolbar__title">质检记录与自动报告</span>
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
      <el-table-column prop="topDefect" label="主要不良" min-width="120" />
      <el-table-column prop="createdAt" label="生成时间" min-width="160" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openReport(row)">查看报告</el-button>
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
    </el-table>

    <el-dialog v-model="reportVisible" title="智能质量报告" width="720px">
      <template v-if="currentReport">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="报告号">{{ currentReport.id }}</el-descriptions-item>
          <el-descriptions-item label="质检单">{{ currentReport.qcId }}</el-descriptions-item>
          <el-descriptions-item label="工单">{{ currentReport.workOrderId }}</el-descriptions-item>
          <el-descriptions-item label="批次">{{ currentReport.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="产品型号">{{ currentReport.productModel }}</el-descriptions-item>
          <el-descriptions-item label="合格率">{{ currentReport.yieldRate }}%</el-descriptions-item>
          <el-descriptions-item label="抽样数">{{ currentReport.sampleQty }}</el-descriptions-item>
          <el-descriptions-item label="不合格数">{{ currentReport.unqualifiedQty }}</el-descriptions-item>
        </el-descriptions>

        <div class="report-block">
          <div class="report-block__title">系统结论</div>
          <p>{{ currentReport.conclusion }}</p>
        </div>

        <div class="report-block">
          <div class="report-block__title">改进建议</div>
          <ul>
            <li v-for="item in currentReport.suggestions || []" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div v-if="defectRows.length" class="report-block">
          <div class="report-block__title">不良分布</div>
          <el-table :data="defectRows" border size="small">
            <el-table-column prop="name" label="不良类型" />
            <el-table-column prop="value" label="数量" width="100" align="right" />
          </el-table>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useMesStore } from '@/stores/mes'

const mes = useMesStore()
const reportVisible = ref(false)
const currentReport = ref(null)

const reports = computed(() => mes.qualityReports || [])
const records = computed(() => mes.inspections.filter((i) => i.status !== '待检'))
const avgYield = computed(() => {
  if (!reports.value.length) return 0
  const total = reports.value.reduce((sum, r) => sum + Number(r.yieldRate || 0), 0)
  return (total / reports.value.length).toFixed(1)
})
const defectTotal = computed(() => reports.value.reduce((sum, r) => sum + Number(r.defectQty || 0), 0))
const defectRows = computed(() => {
  const dist = currentReport.value?.defectDistribution || {}
  return Object.entries(dist).map(([name, value]) => ({ name, value }))
})

function openReport(row) {
  currentReport.value = row
  reportVisible.value = true
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
.ruoyi-toolbar__meta {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
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
.report-block p,
.report-block li {
  font-size: 13px;
  line-height: 1.7;
  color: #606266;
}
</style>
