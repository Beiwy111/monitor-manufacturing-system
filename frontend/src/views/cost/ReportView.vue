<template>
  <div class="report-page">
    <div class="page-header">
      <span class="page-title">成本报表</span>
      <el-button size="small" :loading="loading" @click="load">刷新</el-button>
    </div>

    <!-- 总览 KPI -->
    <div class="kpi-row">
      <div class="kpi-card" v-for="k in kpiCards" :key="k.label" :class="k.cls">
        <div class="kpi-val">{{ k.val }}</div>
        <div class="kpi-lbl">{{ k.label }}</div>
      </div>
    </div>

    <!-- 成本构成 + 来源分布 -->
    <div class="chart-row">
      <!-- 成本构成横向进度条 -->
      <div class="chart-card">
        <div class="chart-title">成本构成分析</div>
        <div class="breakdown">
          <div class="bd-row" v-for="item in breakdown" :key="item.label">
            <span class="bd-label">{{ item.label }}</span>
            <div class="bd-bar-wrap">
              <div class="bd-bar" :style="{ width: item.pct + '%', background: item.color }"></div>
            </div>
            <span class="bd-val">¥ {{ fmtAmt(item.val) }}</span>
            <span class="bd-pct">{{ item.pct }}%</span>
          </div>
        </div>
      </div>

      <!-- 来源类型分布 -->
      <div class="chart-card">
        <div class="chart-title">质量成本来源分布</div>
        <div class="source-list">
          <div class="src-row" v-for="g in groups" :key="g.sourceType">
            <el-tag :type="srcTagType(g.sourceType)" size="small" style="min-width:96px;text-align:center">
              {{ g.sourceTypeCn }}
            </el-tag>
            <div class="src-bar-wrap">
              <div class="src-bar" :style="{ width: srcPct(g.amount) + '%', background: srcColor(g.sourceType) }"></div>
            </div>
            <span class="src-amt">¥ {{ fmtAmt(g.amount) }}</span>
            <span class="src-cnt">{{ g.count }} 笔</span>
          </div>
          <div v-if="!groups.length" style="color:#c0c4cc;font-size:13px;padding:12px 0">暂无数据</div>
        </div>
      </div>
    </div>

    <!-- 明细汇总表 -->
    <div class="table-card">
      <div class="chart-title">结算明细汇总</div>
      <el-table :data="list" border stripe size="small" v-loading="loading" style="width:100%">
        <el-table-column prop="settlementNo"  label="结算单号"  width="155" />
        <el-table-column prop="workOrderNo"   label="工单号"    width="140" show-overflow-tooltip />
        <el-table-column label="来源类型" width="115">
          <template #default="{ row }">
            <el-tag :type="srcTagType(row.sourceType)" size="small">{{ row.sourceTypeCn }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="materialCost"  label="材料(¥)"   width="95"  align="right" />
        <el-table-column prop="laborCost"     label="人工(¥)"   width="95"  align="right" />
        <el-table-column prop="equipmentCost" label="设备(¥)"   width="95"  align="right" />
        <el-table-column prop="qualityCost"   label="质量(¥)"   width="95"  align="right">
          <template #default="{ row }">
            <span :style="Number(row.qualityCost)>0?'color:#f56c6c;font-weight:600':''">
              {{ row.qualityCost }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="合计(¥)" width="115" align="right">
          <template #default="{ row }"><b>{{ row.totalCost }}</b></template>
        </el-table-column>
        <el-table-column label="状态" width="82">
          <template #default="{ row }">
            <el-tag :type="stType(row.settlementStatus)" size="small">{{ row.settlementStatusCn }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="settlementPeriod" label="期间" width="85" />
      </el-table>

      <!-- 合计行 -->
      <div class="total-row">
        <span>合计（{{ list.length }} 笔）</span>
        <div class="total-cells">
          <span>材料 ¥{{ fmtAmt(sumOf('materialCost')) }}</span>
          <span>人工 ¥{{ fmtAmt(sumOf('laborCost')) }}</span>
          <span>设备 ¥{{ fmtAmt(sumOf('equipmentCost')) }}</span>
          <span style="color:#f56c6c">质量 ¥{{ fmtAmt(sumOf('qualityCost')) }}</span>
          <b>总计 ¥{{ fmtAmt(sumOf('totalCost')) }}</b>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchSettlementViews, fetchCostKpi, fetchCostGroup } from '@/api/cost'

const list   = ref([])
const kpi    = ref({})
const groups = ref([])
const loading = ref(false)

const kpiCards = computed(() => [
  { label: '结算总数',   val: kpi.value.total     ?? 0, cls: '' },
  { label: '草稿',      val: kpi.value.draft      ?? 0, cls: 'draft' },
  { label: '已确认',    val: kpi.value.confirmed  ?? 0, cls: 'confirmed' },
  { label: '已导出',    val: kpi.value.exported   ?? 0, cls: 'exported' },
  { label: '成本总额',  val: '¥' + fmtAmt(kpi.value.totalAmount ?? 0), cls: 'amount' },
  { label: '质量成本',  val: '¥' + fmtAmt(kpi.value.totalQualityCost ?? 0), cls: 'quality' },
  { label: '设备维修',  val: '¥' + fmtAmt(kpi.value.totalEquipmentCost ?? 0), cls: '' },
  { label: '材料成本',  val: '¥' + fmtAmt(kpi.value.totalMaterialCost ?? 0), cls: '' },
])

const totalCostAll = computed(() => sumOf('totalCost') || 1)

const breakdown = computed(() => {
  const items = [
    { label: '材料成本', key: 'materialCost',  color: '#409eff' },
    { label: '人工成本', key: 'laborCost',     color: '#67c23a' },
    { label: '设备成本', key: 'equipmentCost', color: '#e6a23c' },
    { label: '质量成本', key: 'qualityCost',   color: '#f56c6c' },
    { label: '其他成本', key: 'otherCost',     color: '#909399' },
  ]
  return items.map(i => {
    const val = sumOf(i.key)
    return { ...i, val, pct: Math.round(val / totalCostAll.value * 100) }
  }).filter(i => i.val > 0)
})

const maxGroupAmt = computed(() => Math.max(...groups.value.map(g => Number(g.amount) || 0), 1))

function srcPct(amt) { return Math.round(Number(amt) / maxGroupAmt.value * 100) }
function srcColor(s) {
  return { NONCONFORMING_PRODUCT:'#f56c6c', AFTER_SALES:'#e6a23c',
           EQUIPMENT_MAINTENANCE:'#409eff', PURCHASE_RETURN:'#f89898', WORK_ORDER:'#67c23a' }[s] || '#909399'
}
function srcTagType(s) {
  return { NONCONFORMING_PRODUCT:'danger', AFTER_SALES:'warning',
           EQUIPMENT_MAINTENANCE:'', PURCHASE_RETURN:'warning', WORK_ORDER:'primary' }[s] || 'info'
}
function stType(s) { return { DRAFT:'info', CONFIRMED:'primary', EXPORTED:'success' }[s] || 'info' }
function sumOf(k)  { return list.value.reduce((s, r) => s + (Number(r[k]) || 0), 0) }
function fmtAmt(v) {
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function load() {
  loading.value = true
  try {
    const [v, k, g] = await Promise.all([
      fetchSettlementViews().catch(() => null),
      fetchCostKpi().catch(() => null),
      fetchCostGroup().catch(() => null),
    ])
    if (v) list.value   = v.data ?? v
    if (k) kpi.value    = k.data ?? k
    if (g) groups.value = g.data ?? g
  } finally { loading.value = false }
}
onMounted(load)
</script>

<style scoped>
.report-page { padding: 16px; background: #f5f7fa; min-height: 100%; display: flex; flex-direction: column; gap: 14px; }
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-title  { font-size: 15px; font-weight: 600; color: #2c3e50; }

.kpi-row { display: flex; gap: 10px; flex-wrap: wrap; }
.kpi-card {
  background: #fff; border: 1px solid #e4e7ed; border-radius: 6px;
  padding: 10px 18px; display: flex; flex-direction: column; align-items: center;
  min-width: 90px; transition: box-shadow .15s;
}
.kpi-card.draft     { border-top: 3px solid #909399; }
.kpi-card.confirmed { border-top: 3px solid #409eff; }
.kpi-card.exported  { border-top: 3px solid #67c23a; }
.kpi-card.amount    { border-top: 3px solid #e6a23c; min-width: 120px; }
.kpi-card.quality   { border-top: 3px solid #f56c6c; min-width: 120px; }
.kpi-val { font-size: 18px; font-weight: 700; color: #2c3e50; }
.kpi-lbl { font-size: 11px; color: #8492a6; margin-top: 2px; }

.chart-row   { display: flex; gap: 14px; flex-wrap: wrap; }
.chart-card  {
  flex: 1; min-width: 300px; background: #fff;
  border: 1px solid #e4e7ed; border-radius: 6px; padding: 14px 16px;
}
.chart-title { font-size: 13px; font-weight: 600; color: #2c3e50; margin-bottom: 12px; }

.breakdown   { display: flex; flex-direction: column; gap: 10px; }
.bd-row      { display: flex; align-items: center; gap: 8px; }
.bd-label    { width: 64px; font-size: 12px; color: #606266; flex-shrink: 0; }
.bd-bar-wrap { flex: 1; height: 10px; background: #f0f2f5; border-radius: 5px; overflow: hidden; }
.bd-bar      { height: 100%; border-radius: 5px; transition: width .4s; min-width: 4px; }
.bd-val      { width: 100px; text-align: right; font-size: 12px; font-weight: 600; color: #2c3e50; }
.bd-pct      { width: 36px; text-align: right; font-size: 11px; color: #909399; }

.source-list { display: flex; flex-direction: column; gap: 10px; }
.src-row     { display: flex; align-items: center; gap: 8px; }
.src-bar-wrap{ flex: 1; height: 10px; background: #f0f2f5; border-radius: 5px; overflow: hidden; }
.src-bar     { height: 100%; border-radius: 5px; transition: width .4s; min-width: 4px; }
.src-amt     { width: 100px; text-align: right; font-size: 12px; font-weight: 600; color: #2c3e50; }
.src-cnt     { width: 36px; text-align: right; font-size: 11px; color: #909399; }

.table-card  { background: #fff; border: 1px solid #e4e7ed; border-radius: 6px; padding: 14px 16px; }
.total-row   {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 10px; padding-top: 10px; border-top: 2px solid #ebeef5;
  font-size: 13px; font-weight: 600; color: #2c3e50;
}
.total-cells { display: flex; gap: 20px; flex-wrap: wrap; }
</style>
