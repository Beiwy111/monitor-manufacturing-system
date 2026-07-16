<template>
  <ModulePageShell>
    <div class="page-head">
      <div>
        <span class="page-title">成本核算</span>
        <p class="page-sub">{{ COST_FORMULA }}</p>
      </div>
      <el-button size="small" :loading="loading" @click="reload">刷新</el-button>
    </div>

    <div v-if="breakdown" class="formula-bar">
      <span v-for="item in formulaItems" :key="item.key" class="formula-item">
        <em>{{ item.label }}</em>
        <b>¥ {{ fmtMoney(item.value) }}</b>
      </span>
      <span class="formula-item formula-item--total">
        <em>总成本</em>
        <b>¥ {{ fmtMoney(breakdown.totalCost) }}</b>
      </span>
    </div>

    <el-tabs v-model="activeTab" class="finance-tabs">
      <el-tab-pane label="工单总览" name="overview">
        <el-table :data="overview" border stripe size="small" v-loading="loading" max-height="520">
          <el-table-column prop="workOrderNo" label="工单号" width="130" fixed />
          <el-table-column prop="orderNo" label="订单" width="125" />
          <el-table-column prop="customerName" label="客户" width="120" show-overflow-tooltip />
          <el-table-column prop="productName" label="产品" width="130" show-overflow-tooltip />
          <el-table-column prop="materialCost" label="材料" width="90" align="right" :formatter="moneyCol" />
          <el-table-column prop="laborCost" label="人工" width="90" align="right" :formatter="moneyCol" />
          <el-table-column prop="equipmentCost" label="设备" width="90" align="right" :formatter="moneyCol" />
          <el-table-column prop="qualityCost" label="质检" width="90" align="right" :formatter="moneyCol" />
          <el-table-column prop="reworkScrapCost" label="返工报废" width="95" align="right" :formatter="moneyCol" />
          <el-table-column prop="warehouseLogisticsCost" label="仓储物流" width="95" align="right" :formatter="moneyCol" />
          <el-table-column prop="afterSalesCost" label="售后" width="85" align="right" :formatter="moneyCol" />
          <el-table-column prop="otherCost" label="其他" width="85" align="right" :formatter="moneyCol" />
          <el-table-column prop="totalCost" label="总成本" width="100" align="right" fixed="right">
            <template #default="{ row }"><b>¥ {{ fmtMoney(row.totalCost) }}</b></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="材料成本" name="material">
        <CostTypeTable :rows="typeRows('materialCost')" cost-key="materialCost" label="材料成本" />
      </el-tab-pane>
      <el-tab-pane label="人工成本" name="labor">
        <CostTypeTable :rows="typeRows('laborCost')" cost-key="laborCost" label="人工成本" />
      </el-tab-pane>
      <el-tab-pane label="设备成本" name="equipment">
        <CostTypeTable :rows="typeRows('equipmentCost')" cost-key="equipmentCost" label="设备成本" />
      </el-tab-pane>
      <el-tab-pane label="其他成本" name="other">
        <CostTypeTable :rows="otherRows" cost-key="otherCost" label="其他/质检/售后等" show-extra />
      </el-tab-pane>

      <el-tab-pane label="订单结算" name="settlement">
        <SettlementPanel />
      </el-tab-pane>
    </el-tabs>
  </ModulePageShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import ModulePageShell from '@/components/module/ModulePageShell.vue'
import { fetchWorkOrderCostOverview, fetchCostBreakdown } from '@/api/finance'
import { fetchSettlementViews } from '@/api/cost'
import { COST_FORMULA, fmtMoney } from '@/constants/financeWorkflow'
import CostTypeTable from './components/CostTypeTable.vue'
import SettlementPanel from './components/SettlementPanel.vue'

const route = useRoute()
const activeTab = ref(route.query.tab || 'overview')
const loading = ref(false)
const overview = ref([])
const breakdown = ref(null)
const settlements = ref([])

const formulaItems = computed(() => breakdown.value ? [
  { key: 'material', label: '材料', value: breakdown.value.materialCost },
  { key: 'labor', label: '人工', value: breakdown.value.laborCost },
  { key: 'equipment', label: '设备', value: breakdown.value.equipmentCost },
  { key: 'quality', label: '质检', value: breakdown.value.qualityCost },
  { key: 'rework', label: '返工报废', value: breakdown.value.reworkScrapCost },
  { key: 'warehouse', label: '仓储物流', value: breakdown.value.warehouseLogisticsCost },
  { key: 'aftersale', label: '售后', value: breakdown.value.afterSalesCost },
  { key: 'other', label: '其他', value: breakdown.value.otherCost }
] : [])

const otherRows = computed(() => settlements.value.filter(r =>
  Number(r.qualityCost) > 0 || Number(r.otherCost) > 0
    || r.sourceType === 'AFTER_SALES' || r.sourceType === 'NONCONFORMING_PRODUCT'
))

function typeRows(key) {
  return settlements.value.filter(r => Number(r[key]) > 0)
}

function moneyCol(row, col, val) {
  return val > 0 ? fmtMoney(val) : '-'
}

async function reload() {
  loading.value = true
  try {
    const [ov, bd, st] = await Promise.all([
      fetchWorkOrderCostOverview(),
      fetchCostBreakdown(),
      fetchSettlementViews()
    ])
    overview.value = ov ?? []
    breakdown.value = bd ?? null
    settlements.value = st ?? []
  } finally { loading.value = false }
}

onMounted(reload)
</script>

<style scoped>
.page-head { display:flex; align-items:flex-start; justify-content:space-between; margin-bottom:12px; }
.page-title { font-size:18px; font-weight:700; color:#001b3f; }
.page-sub { margin:4px 0 0; font-size:12px; color:#6b7c8f; }
.formula-bar { display:flex; flex-wrap:wrap; gap:8px; margin-bottom:14px; padding:10px 12px; background:#f6f9fc; border:1px solid #dce6ef; }
.formula-item { display:flex; flex-direction:column; padding:6px 12px; background:#fff; border:1px solid #e4ebf2; min-width:88px; }
.formula-item em { font-style:normal; font-size:11px; color:#7a8fa3; }
.formula-item b { margin-top:3px; font-size:13px; color:#2a4560; }
.formula-item--total { border-left:3px solid #c9956a; }
.finance-tabs :deep(.el-tabs__header) { margin-bottom:10px; }
</style>
