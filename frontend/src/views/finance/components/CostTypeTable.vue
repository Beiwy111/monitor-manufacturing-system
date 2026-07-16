<template>
  <div class="cost-type-table">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="结算单/工单/来源" clearable size="small" style="width:220px" />
      <span class="sum">合计 <b>¥ {{ fmtMoney(total) }}</b></span>
    </div>
    <el-table :data="filtered" border stripe size="small" max-height="480">
      <el-table-column prop="settlementNo" label="结算单号" width="145" />
      <el-table-column prop="workOrderNo" label="工单" width="125" />
      <el-table-column label="来源" width="110">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.sourceTypeCn }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sourceId" label="来源编号" width="120" show-overflow-tooltip />
      <el-table-column :label="label" width="100" align="right">
        <template #default="{ row }"><b>¥ {{ fmtMoney(row[costKey]) }}</b></template>
      </el-table-column>
      <el-table-column v-if="showExtra" prop="qualityCost" label="质检" width="90" align="right">
        <template #default="{ row }">{{ row.qualityCost > 0 ? fmtMoney(row.qualityCost) : '-' }}</template>
      </el-table-column>
      <el-table-column prop="totalCost" label="结算合计" width="100" align="right">
        <template #default="{ row }">¥ {{ fmtMoney(row.totalCost) }}</template>
      </el-table-column>
      <el-table-column prop="settlementPeriod" label="期间" width="85" />
      <el-table-column label="状态" width="82">
        <template #default="{ row }">
          <el-tag size="small">{{ row.settlementStatusCn }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { fmtMoney } from '@/constants/financeWorkflow'

const props = defineProps({
  rows: { type: Array, default: () => [] },
  costKey: { type: String, required: true },
  label: { type: String, default: '成本' },
  showExtra: { type: Boolean, default: false }
})

const keyword = ref('')
const filtered = computed(() => {
  if (!keyword.value) return props.rows
  const kw = keyword.value.toLowerCase()
  return props.rows.filter(r =>
    (r.settlementNo || '').toLowerCase().includes(kw) ||
    (r.workOrderNo || '').toLowerCase().includes(kw) ||
    (r.sourceId || '').toLowerCase().includes(kw)
  )
})
const total = computed(() => filtered.value.reduce((s, r) => s + (Number(r[props.costKey]) || 0), 0))
</script>

<style scoped>
.toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:10px; }
.sum { font-size:13px; color:#50657a; }
.sum b { color:#c9956a; }
</style>
