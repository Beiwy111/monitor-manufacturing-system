<template>
  <div class="cost-sub-page">
    <div class="page-header">
      <span class="page-title">设备成本</span>
      <div style="display:flex;gap:8px;align-items:center">
        <el-input v-model="keyword" placeholder="结算单/工单/来源编号" clearable size="small" style="width:200px" />
        <el-button size="small" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>
    <div class="summary-bar">
      <div class="sum-item"><span class="sum-val">{{ filtered.length }}</span><span class="sum-lbl">结算单</span></div>
      <div class="sum-item highlight-orange"><span class="sum-val">¥ {{ fmtAmt(sumOf('equipmentCost')) }}</span><span class="sum-lbl">设备成本合计</span></div>
      <div class="sum-item highlight-red"><span class="sum-val">¥ {{ fmtAmt(sumOf('qualityCost')) }}</span><span class="sum-lbl">质量成本合计</span></div>
      <div class="sum-item"><span class="sum-val">{{ pct('equipmentCost') }}%</span><span class="sum-lbl">设备占比</span></div>
    </div>
    <el-table :data="filtered" border stripe size="small" style="width:100%" v-loading="loading">
      <el-table-column prop="settlementNo"  label="结算单号"   width="155" />
      <el-table-column prop="workOrderNo"   label="工单号"     width="140" show-overflow-tooltip />
      <el-table-column prop="sourceId"      label="来源编号"   width="130" show-overflow-tooltip />
      <el-table-column label="来源类型" width="115">
        <template #default="{ row }">
          <el-tag :type="srcType(row.sourceType)" size="small">{{ row.sourceTypeCn }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="设备成本(¥)" width="115" align="right">
        <template #default="{ row }">
          <span :style="Number(row.equipmentCost)>0?'color:#e6a23c;font-weight:600':''">
            {{ row.equipmentCost ?? '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="质量成本(¥)" width="115" align="right">
        <template #default="{ row }">
          <span :style="Number(row.qualityCost)>0?'color:#f56c6c;font-weight:600':''">
            {{ row.qualityCost ?? '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="totalCost"     label="总成本(¥)"  width="110" align="right" />
      <el-table-column prop="costReason"    label="成本原因"   min-width="150" show-overflow-tooltip />
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

const list = ref([]), loading = ref(false), keyword = ref('')

const filtered = computed(() => {
  if (!keyword.value) return list.value
  const kw = keyword.value.toLowerCase()
  return list.value.filter(r =>
    (r.settlementNo || '').toLowerCase().includes(kw) ||
    (r.workOrderNo  || '').toLowerCase().includes(kw) ||
    (r.sourceId     || '').toLowerCase().includes(kw)
  )
})

function sumOf(k) { return filtered.value.reduce((s, r) => s + (Number(r[k]) || 0), 0) }
function pct(k)   { const t = sumOf('totalCost'); return t > 0 ? Math.round(sumOf(k)/t*100) : 0 }
function fmtAmt(v) { return Number(v).toLocaleString('zh-CN', { minimumFractionDigits:2, maximumFractionDigits:2 }) }
function stType(s) { return { DRAFT:'info', CONFIRMED:'primary', EXPORTED:'success' }[s] || 'info' }
function srcType(s) {
  return { NONCONFORMING_PRODUCT:'danger', AFTER_SALES:'warning',
           EQUIPMENT_MAINTENANCE:'', PURCHASE_RETURN:'warning', WORK_ORDER:'primary' }[s] || 'info'
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
.sum-item.highlight-orange { border-top: 3px solid #e6a23c; }
.sum-item.highlight-red    { border-top: 3px solid #f56c6c; }
.sum-val { font-size: 16px; font-weight: 700; color: #2c3e50; }
.sum-lbl { font-size: 11px; color: #8492a6; margin-top: 2px; }
</style>
