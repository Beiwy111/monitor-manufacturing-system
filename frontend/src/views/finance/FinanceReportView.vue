<template>
  <ModulePageShell>
    <div class="page-head">
      <div>
        <span class="page-title">财务报表</span>
        <p class="page-sub">月度汇总 · 成本差异 · 盈利排行</p>
      </div>
      <div class="head-actions">
        <el-date-picker v-model="period" type="month" value-format="YYYY-MM" size="small" placeholder="选择月份" @change="load" />
        <el-button size="small" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" size="small" :disabled="!report" @click="doExport">导出 CSV</el-button>
      </div>
    </div>

    <div v-loading="loading">
      <div v-if="report" class="summary-grid">
        <div class="sum-card"><em>月度收入</em><b>¥ {{ fmtMoney(report.income) }}</b></div>
        <div class="sum-card"><em>月度成本</em><b>¥ {{ fmtMoney(report.cost) }}</b></div>
        <div class="sum-card"><em>月度利润</em><b :class="{ neg: Number(report.profit)<0 }">¥ {{ fmtMoney(report.profit) }}</b></div>
        <div class="sum-card"><em>毛利率</em><b>{{ fmtPct(report.grossMargin) }}</b></div>
        <div class="sum-card"><em>已回款</em><b>¥ {{ fmtMoney(report.received) }}</b></div>
        <div class="sum-card"><em>应收账款</em><b>¥ {{ fmtMoney(report.receivable) }}</b></div>
      </div>

      <div v-if="report" class="report-grid">
        <ModulePanelSection>
          <div class="panel-title">成本结构（{{ report.period }}）</div>
          <el-table :data="costRows" border stripe size="small">
            <el-table-column prop="label" label="成本项" />
            <el-table-column label="金额" align="right">
              <template #default="{ row }">¥ {{ fmtMoney(row.value) }}</template>
            </el-table-column>
          </el-table>
        </ModulePanelSection>

        <ModulePanelSection>
          <div class="panel-title">成本差异</div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="预算成本">¥ {{ fmtMoney(report.costVariance?.budgetCost) }}</el-descriptions-item>
            <el-descriptions-item label="实际成本">¥ {{ fmtMoney(report.costVariance?.actualCost) }}</el-descriptions-item>
            <el-descriptions-item label="差异">
              <span :style="{ color: Number(report.costVariance?.variance) < 0 ? '#2a7a4b' : '#d94848' }">
                ¥ {{ fmtMoney(report.costVariance?.variance) }}
              </span>
            </el-descriptions-item>
          </el-descriptions>
        </ModulePanelSection>

        <ModulePanelSection>
          <div class="panel-title">客户盈利排行</div>
          <el-table :data="report.customerRank || []" border stripe size="small" max-height="240">
            <el-table-column prop="name" label="客户" />
            <el-table-column label="利润" align="right">
              <template #default="{ row }">¥ {{ fmtMoney(row.profit) }}</template>
            </el-table-column>
            <el-table-column label="毛利率" align="right">
              <template #default="{ row }">{{ fmtPct(row.grossMargin) }}</template>
            </el-table-column>
          </el-table>
        </ModulePanelSection>

        <ModulePanelSection>
          <div class="panel-title">产品盈利排行</div>
          <el-table :data="report.productRank || []" border stripe size="small" max-height="240">
            <el-table-column prop="name" label="产品" show-overflow-tooltip />
            <el-table-column label="利润" align="right">
              <template #default="{ row }">¥ {{ fmtMoney(row.profit) }}</template>
            </el-table-column>
            <el-table-column label="毛利率" align="right">
              <template #default="{ row }">{{ fmtPct(row.grossMargin) }}</template>
            </el-table-column>
          </el-table>
        </ModulePanelSection>

        <ModulePanelSection class="span-2">
          <div class="panel-title">亏损 / 低毛利订单</div>
          <el-table :data="report.lossOrders || []" border stripe size="small" max-height="220">
            <el-table-column prop="name" label="订单" width="130" />
            <el-table-column prop="customerName" label="客户" />
            <el-table-column prop="flagCn" label="标记" width="90" />
            <el-table-column label="利润" align="right">
              <template #default="{ row }">¥ {{ fmtMoney(row.profit) }}</template>
            </el-table-column>
            <el-table-column label="毛利率" align="right">
              <template #default="{ row }">{{ fmtPct(row.grossMargin) }}</template>
            </el-table-column>
          </el-table>
        </ModulePanelSection>
      </div>
    </div>
  </ModulePageShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import ModulePageShell from '@/components/module/ModulePageShell.vue'
import ModulePanelSection from '@/components/module/ModulePanelSection.vue'
import { fetchFinanceReport } from '@/api/finance'
import { fmtMoney, fmtPct, exportCsv } from '@/constants/financeWorkflow'

const loading = ref(false)
const period = ref(new Date().toISOString().slice(0, 7))
const report = ref(null)

const costRows = computed(() => {
  const b = report.value?.costBreakdown
  if (!b) return []
  return [
    { label: '材料成本', value: b.materialCost },
    { label: '人工成本', value: b.laborCost },
    { label: '设备成本', value: b.equipmentCost },
    { label: '质检成本', value: b.qualityCost },
    { label: '返工报废', value: b.reworkScrapCost },
    { label: '仓储物流', value: b.warehouseLogisticsCost },
    { label: '售后成本', value: b.afterSalesCost },
    { label: '其他成本', value: b.otherCost },
    { label: '总成本', value: b.totalCost }
  ]
})

async function load() {
  loading.value = true
  try { report.value = await fetchFinanceReport(period.value) }
  finally { loading.value = false }
}

function doExport() {
  const r = report.value
  if (!r) return
  exportCsv(`财务报表_${r.period}.csv`,
    ['指标', '数值'],
    [
      ['期间', r.period],
      ['月度收入', r.income],
      ['月度成本', r.cost],
      ['月度利润', r.profit],
      ['毛利率(%)', r.grossMargin],
      ['已回款', r.received],
      ['应收账款', r.receivable],
      ['预算成本', r.costVariance?.budgetCost],
      ['实际成本', r.costVariance?.actualCost],
      ['成本差异', r.costVariance?.variance],
      ['生成时间', r.generatedAt]
    ]
  )
}

onMounted(load)
</script>

<style scoped>
.page-head { display:flex; align-items:flex-start; justify-content:space-between; margin-bottom:12px; }
.page-title { font-size:18px; font-weight:700; color:#001b3f; }
.page-sub { margin:4px 0 0; font-size:12px; color:#6b7c8f; }
.head-actions { display:flex; gap:8px; align-items:center; }
.summary-grid { display:grid; grid-template-columns:repeat(6,1fr); gap:10px; margin-bottom:14px; }
.sum-card { padding:10px 12px; background:#fff; border:1px solid #dce6ef; }
.sum-card em { display:block; font-style:normal; font-size:11px; color:#7a8fa3; }
.sum-card b { display:block; margin-top:4px; font-size:15px; color:#2a4560; }
.sum-card b.neg { color:#d94848; }
.report-grid { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
.panel-title { font-size:14px; font-weight:600; margin-bottom:8px; color:#2a4560; }
.span-2 { grid-column:1/-1; }
@media (max-width: 1200px) { .summary-grid { grid-template-columns:repeat(3,1fr); } .report-grid { grid-template-columns:1fr; } .span-2 { grid-column:auto; } }
</style>
