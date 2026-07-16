<template>
  <div class="finance-screen">
    <header class="screen-head">
      <div>
        <h1>财务大屏</h1>
        <p>收入 · 成本 · 利润 · 回款全景</p>
      </div>
      <div class="head-actions">
        <el-select v-model="days" size="small" style="width:100px" @change="load">
          <el-option :value="7" label="近7天" />
          <el-option :value="30" label="近30天" />
          <el-option :value="90" label="近90天" />
        </el-select>
        <el-button size="small" :loading="loading" @click="load">刷新</el-button>
      </div>
    </header>

    <div v-if="summary" class="kpi-row">
      <div class="kpi-card"><em>总收入</em><b>¥ {{ fmtMoney(summary.totalIncome) }}</b></div>
      <div class="kpi-card"><em>总成本</em><b>¥ {{ fmtMoney(summary.totalCost) }}</b></div>
      <div class="kpi-card" :class="{ danger: Number(summary.totalProfit) < 0 }">
        <em>总利润</em><b>¥ {{ fmtMoney(summary.totalProfit) }}</b>
      </div>
      <div class="kpi-card"><em>平均毛利率</em><b>{{ fmtPct(summary.avgMargin) }}</b></div>
    </div>

    <div class="chart-grid" v-loading="loading">
      <section class="chart-panel chart-panel--wide">
        <h3>收入 / 成本 / 利润趋势</h3>
        <BoardChart :option="trendOption" />
      </section>
      <section class="chart-panel">
        <h3>毛利率趋势</h3>
        <BoardChart :option="marginOption" />
      </section>
      <section class="chart-panel">
        <h3>成本结构占比</h3>
        <BoardChart :option="structureOption" />
      </section>
      <section class="chart-panel">
        <h3>利润瀑布图</h3>
        <BoardChart :option="waterfallOption" />
      </section>
      <section class="chart-panel">
        <h3>订单盈亏分布</h3>
        <BoardChart :option="distributionOption" />
      </section>
      <section class="chart-panel">
        <h3>回款趋势</h3>
        <BoardChart :option="collectionOption" />
      </section>
      <section class="chart-panel">
        <h3>客户/订单利润 TOP</h3>
        <BoardChart :option="rankOption" />
      </section>
      <section class="chart-panel chart-panel--alert">
        <h3>亏损订单预警</h3>
        <ul v-if="lossAlerts.length" class="alert-list">
          <li v-for="item in lossAlerts" :key="item.orderNo">
            <b>{{ item.orderNo }}</b>
            <span>{{ item.customerName }}</span>
            <em>利润 ¥{{ fmtMoney(item.profit) }} · 毛利 {{ fmtPct(item.grossMargin) }}</em>
          </li>
        </ul>
        <p v-else class="empty">暂无亏损订单</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import BoardChart from '@/components/board/BoardChart.vue'
import { fetchFinanceScreen } from '@/api/finance'
import { fmtMoney, fmtPct } from '@/constants/financeWorkflow'

const loading = ref(false)
const days = ref(30)
const data = ref(null)

const summary = computed(() => data.value?.summary)
const lossAlerts = computed(() => data.value?.lossAlerts || [])

const LIGHT = ['#4a6fa5', '#c9956a', '#5a9a6a', '#c45a5a', '#7a6a9a', '#4a9090', '#b89a4a']

const trendOption = computed(() => ({
  color: LIGHT,
  tooltip: { trigger: 'axis' },
  legend: { data: ['收入', '成本', '利润'], bottom: 0 },
  grid: { left: 48, right: 20, top: 24, bottom: 36 },
  xAxis: { type: 'category', data: data.value?.dayLabels || [] },
  yAxis: { type: 'value', name: '元' },
  series: [
    { name: '收入', type: 'line', smooth: true, data: (data.value?.incomeTrend || []).map(Number) },
    { name: '成本', type: 'line', smooth: true, data: (data.value?.costTrend || []).map(Number) },
    { name: '利润', type: 'line', smooth: true, data: (data.value?.profitTrend || []).map(Number) }
  ]
}))

const marginOption = computed(() => ({
  color: ['#c9956a'],
  tooltip: { trigger: 'axis' },
  grid: { left: 48, right: 20, top: 24, bottom: 28 },
  xAxis: { type: 'category', data: data.value?.dayLabels || [] },
  yAxis: { type: 'value', name: '%' },
  series: [{ type: 'line', smooth: true, areaStyle: { opacity: 0.12 }, data: (data.value?.marginTrend || []).map(Number) }]
}))

const structureOption = computed(() => ({
  color: LIGHT,
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie', radius: ['42%', '68%'],
    data: (data.value?.costStructure || []).map(i => ({ name: i.name, value: Number(i.value) }))
  }]
}))

const waterfallOption = computed(() => {
  const steps = data.value?.waterfall || []
  let acc = 0
  const placeholders = []
  const values = []
  steps.forEach((s) => {
    const v = Number(s.value)
    if (s.type === 'total') {
      placeholders.push(0)
      values.push(v)
      acc = v
    } else {
      placeholders.push(acc + v)
      values.push(Math.abs(v))
      acc += v
    }
  })
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 16, top: 20, bottom: 48 },
    xAxis: { type: 'category', data: steps.map(s => s.name), axisLabel: { rotate: 30, fontSize: 10 } },
    yAxis: { type: 'value' },
    series: [
      { type: 'bar', stack: 'w', itemStyle: { borderColor: 'transparent', color: 'transparent' }, data: placeholders },
      { type: 'bar', stack: 'w', data: values, itemStyle: { color: (p) => steps[p.dataIndex]?.type === 'total' ? '#4a6fa5' : '#c45a5a' } }
    ]
  }
})

const distributionOption = computed(() => ({
  color: ['#5a9a6a', '#c45a5a'],
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: '62%', data: (data.value?.profitDistribution || []).map(i => ({ name: i.name, value: Number(i.value) })) }]
}))

const collectionOption = computed(() => ({
  color: ['#4a9090'],
  tooltip: { trigger: 'axis' },
  grid: { left: 48, right: 20, top: 24, bottom: 28 },
  xAxis: { type: 'category', data: data.value?.dayLabels || [] },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', data: (data.value?.collectionTrend || []).map(Number) }]
}))

const rankOption = computed(() => {
  const list = data.value?.profitRank || []
  return {
    color: ['#4a6fa5'],
    tooltip: { trigger: 'axis' },
    grid: { left: 100, right: 20, top: 12, bottom: 20 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: list.map(i => i.name).reverse() },
    series: [{ type: 'bar', data: list.map(i => Number(i.profit)).reverse() }]
  }
})

async function load() {
  loading.value = true
  try { data.value = await fetchFinanceScreen(days.value) }
  finally { loading.value = false }
}

onMounted(load)
</script>

<style scoped>
.finance-screen { min-height:100%; padding:16px 18px; background:#f3f6f9; }
.screen-head { display:flex; align-items:flex-start; justify-content:space-between; margin-bottom:14px; }
.screen-head h1 { margin:0; font-size:20px; color:#001b3f; }
.screen-head p { margin:4px 0 0; font-size:12px; color:#6b7c8f; }
.head-actions { display:flex; gap:8px; }
.kpi-row { display:flex; flex-wrap:wrap; gap:10px; margin-bottom:14px; }
.kpi-card { flex:1; min-width:140px; padding:12px 16px; background:#fff; border:1px solid #dce6ef; border-top:3px solid #c9956a; }
.kpi-card em { display:block; font-style:normal; font-size:11px; color:#7a8fa3; }
.kpi-card b { display:block; margin-top:6px; font-size:18px; color:#2a4560; }
.kpi-card.danger b { color:#d94848; }
.chart-grid { display:grid; grid-template-columns:repeat(2,1fr); gap:12px; }
.chart-panel { background:#fff; border:1px solid #dce6ef; padding:12px; min-height:260px; display:flex; flex-direction:column; }
.chart-panel--wide { grid-column:1/-1; min-height:300px; }
.chart-panel h3 { margin:0 0 8px; font-size:13px; font-weight:600; color:#2a4560; }
.chart-panel :deep(.board-chart) { flex:1; min-height:200px; }
.chart-panel--alert { min-height:260px; }
.alert-list { margin:0; padding:0; list-style:none; overflow:auto; max-height:220px; }
.alert-list li { padding:8px 0; border-bottom:1px dashed #e8edf2; font-size:12px; }
.alert-list b { display:block; color:#d94848; }
.alert-list span { color:#50657a; }
.alert-list em { display:block; margin-top:3px; color:#8a9aae; font-style:normal; }
.empty { color:#a0aec0; font-size:13px; text-align:center; padding:40px 0; }
@media (max-width: 1100px) { .chart-grid { grid-template-columns:1fr; } .chart-panel--wide { grid-column:auto; } }
</style>
