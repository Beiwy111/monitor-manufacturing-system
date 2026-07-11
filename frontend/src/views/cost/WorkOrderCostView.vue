<template>
  <div class="cost-sub-page">
    <div class="page-header">
      <span class="page-title">工单成本</span>
      <div style="display:flex;gap:8px;align-items:center">
        <el-input v-model="keyword" placeholder="工单号/来源/备注" clearable size="small" style="width:200px" />
        <el-button size="small" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>
    <div class="summary-bar">
      <div class="sum-item"><span class="sum-val">{{ list.length }}</span><span class="sum-lbl">结算单</span></div>
      <div class="sum-item"><span class="sum-val">¥ {{ fmtAmt(sumOf('totalCost')) }}</span><span class="sum-lbl">总成本</span></div>
      <div class="sum-item"><span class="sum-val">¥ {{ fmtAmt(sumOf('materialCost')) }}</span><span class="sum-lbl">材料</span></div>
      <div class="sum-item"><span class="sum-val">¥ {{ fmtAmt(sumOf('laborCost')) }}</span><span class="sum-lbl">人工</span></div>
      <div class="sum-item"><span class="sum-val">¥ {{ fmtAmt(sumOf('equipmentCost')) }}</span><span class="sum-lbl">设备</span></div>
      <div class="sum-item highlight"><span class="sum-val">¥ {{ fmtAmt(sumOf('qualityCost')) }}</span><span class="sum-lbl">质量成本</span></div>
    </div>
    <el-table :data="filtered" border stripe size="small" style="width:100%" v-loading="loading">
      <el-table-column prop="settlementNo" label="结算单号" width="155" />
      <el-table-column prop="workOrderNo"  label="工单号"   width="140" show-overflow-tooltip />
      <el-table-column label="来源类型" width="115">
        <template #default="{ row }">
          <el-tag :type="srcType(row.sourceType)" size="small">{{ row.sourceTypeCn }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="materialCost"  label="材料(¥)"  width="95"  align="right" />
      <el-table-column prop="laborCost"     label="人工(¥)"  width="95"  align="right" />
      <el-table-column prop="equipmentCost" label="设备(¥)"  width="95"  align="right" />
      <el-table-column prop="qualityCost"   label="质量(¥)"  width="95"  align="right">
        <template #default="{ row }">
          <span :style="Number(row.qualityCost)>0?'color:#f56c6c;font-weight:600':''">
            {{ row.qualityCost ?? '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="合计(¥)" width="110" align="right">
        <template #default="{ row }"><b>{{ row.totalCost }}</b></template>
      </el-table-column>
      <el-table-column label="状态" width="85">
        <template #default="{ row }">
          <el-tag :type="stType(row.settlementStatus)" size="small">{{ row.settlementStatusCn }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="settlementPeriod" label="期间" width="85" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchSettlementViews } from '@/api/cost'

const list    = ref([])
const loading = ref(false)
const keyword = ref('')

const filtered = computed(() => {
  if (!keyword.value) return list.value
  const kw = keyword.value.toLowerCase()
  return list.value.filter(r =>
    (r.settlementNo || '').toLowerCase().includes(kw) ||
    (r.workOrderNo  || '').toLowerCase().includes(kw) ||
    (r.sourceId     || '').toLowerCase().includes(kw) ||
    (r.remark       || '').toLowerCase().includes(kw)
  )
})

function sumOf(key) {
  return filtered.value.reduce((s, r) => s + (Number(r[key]) || 0), 0)
}
function fmtAmt(v) {
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function stType(s) {
  return { DRAFT: 'info', CONFIRMED: 'primary', EXPORTED: 'success' }[s] || 'info'
}
function srcType(s) {
  return { NONCONFORMING_PRODUCT: 'danger', AFTER_SALES: 'warning',
           EQUIPMENT_MAINTENANCE: '', PURCHASE_RETURN: 'warning', WORK_ORDER: 'primary' }[s] || 'info'
}

async function load() {
  loading.value = true
  try { const r = await fetchSettlementViews(); list.value = r.data ?? r }
  catch { /* 静默 */ } finally { loading.value = false }
}
onMounted(load)
</script>

<style scoped>
.cost-sub-page { padding: 16px; background: #f5f7fa; min-height: 100%; display: flex; flex-direction: column; gap: 12px; }
.page-header   { display: flex; align-items: center; justify-content: space-between; }
.page-title    { font-size: 15px; font-weight: 600; color: #2c3e50; }
.summary-bar   { display: flex; gap: 10px; flex-wrap: wrap; }
.sum-item {
  background: #fff; border: 1px solid #e4e7ed; border-radius: 6px;
  padding: 8px 16px; display: flex; flex-direction: column; align-items: center;
}
.sum-item.highlight { border-top: 3px solid #f56c6c; }
.sum-val { font-size: 16px; font-weight: 700; color: #2c3e50; }
.sum-lbl { font-size: 11px; color: #8492a6; margin-top: 2px; }
</style>
