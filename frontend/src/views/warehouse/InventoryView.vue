<template>
  <div class="inventory-page">
    <el-tabs v-model="activeTab" class="inventory-tabs">
      <el-tab-pane label="成品库" name="finished">
        <CatalogSection
          title="成品库"
          warehouse-label="成品仓 FG-WH"
          :rows="finishedRows"
          :loading="loading"
          v-model:keyword="finishedKeyword"
          v-model:category-filter="finishedCategory"
          @refresh="loadData"
        />
      </el-tab-pane>

      <el-tab-pane label="原材料库" name="raw">
        <CatalogSection
          title="原材料库"
          warehouse-label="原材料仓"
          :rows="rawRows"
          :loading="loading"
          v-model:keyword="rawKeyword"
          v-model:category-filter="rawCategory"
          @refresh="loadData"
        />
      </el-tab-pane>

      <el-tab-pane label="出入库记录" name="flow">
        <div class="catalog-shell">
          <div class="catalog-toolbar">
            <div class="catalog-toolbar__left">
              <div class="catalog-toolbar__title">出入库记录</div>
              <span class="catalog-toolbar__meta">共 {{ filteredFlows.length }} 条流水</span>
            </div>
            <div class="catalog-toolbar__filters">
              <el-input v-model="flowKeyword" clearable placeholder="物料编码 / 名称 / 单据" style="width: 240px" />
              <el-select v-model="flowDirection" clearable placeholder="方向" style="width: 120px">
                <el-option label="入库" value="入" />
                <el-option label="出库" value="出" />
              </el-select>
              <el-button type="primary" @click="loadData">刷新</el-button>
            </div>
          </div>
          <div class="catalog-main catalog-main--full pi-table-wrap--compact">
            <el-table
              :data="filteredFlows"
              border
              stripe
              style="width:100%"
              v-loading="loading"
              class="catalog-table"
            >
              <el-table-column prop="flowType" label="业务类型" min-width="100" show-overflow-tooltip />
              <el-table-column prop="materialCode" label="物料编码" min-width="110" show-overflow-tooltip />
              <el-table-column prop="materialName" label="物料名称" min-width="140" show-overflow-tooltip />
              <el-table-column prop="direction" label="方向" width="72" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.direction === '入' ? 'success' : 'warning'" size="small">
                    {{ row.direction }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" min-width="90" align="right" />
              <el-table-column prop="warehouseCode" label="仓库" min-width="90" align="center" />
              <el-table-column prop="locationCode" label="库位储位" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">{{ row.slotLabel || row.locationCode || '—' }}</template>
              </el-table-column>
              <el-table-column prop="batchNo" label="批次" min-width="120" show-overflow-tooltip />
              <el-table-column prop="refNo" label="关联单据" min-width="130" show-overflow-tooltip />
              <el-table-column prop="operator" label="操作人" min-width="90" show-overflow-tooltip />
              <el-table-column label="时间" min-width="160" show-overflow-tooltip>
                <template #default="{ row }">{{ formatCompactDateTime(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import CatalogSection from './components/InventoryCatalogSection.vue'
import { fetchWarehouseCatalog, fetchWarehouseTransactions } from '@/api/warehouse'
import { formatCompactDateTime } from '@/utils/formatCompactDateTime'
import { useMesStore } from '@/stores/mes'

const mes = useMesStore()
const route = useRoute()
const activeTab = ref('finished')
const loading = ref(false)
const catalogRows = ref([])
const transactionRows = ref([])
const finishedKeyword = ref('')
const rawKeyword = ref('')
const finishedCategory = ref('')
const rawCategory = ref('')
const flowKeyword = ref('')
const flowDirection = ref('')

const finishedRows = computed(() =>
  catalogRows.value.filter((row) => row.warehouseCategory === 'FINISHED')
)
const rawRows = computed(() =>
  catalogRows.value.filter((row) => row.warehouseCategory !== 'FINISHED')
)

const mergedFlows = computed(() => {
  const runtime = (mes.stockFlows || []).map((row) => ({
    id: row.id,
    flowType: row.flowType,
    materialCode: row.materialCode || '',
    materialName: row.materialName || '',
    direction: row.direction || '入',
    quantity: row.quantity || 0,
    warehouseCode: row.warehouseCode || '',
    batchNo: row.batchNo || '',
    refNo: row.refNo || '',
    operator: row.operator || '',
    createdAt: row.createdAt || ''
  }))
  const db = transactionRows.value || []
  const seen = new Set()
  const list = []
  for (const row of [...runtime, ...db]) {
    const key = row.id || `${row.flowType}-${row.materialCode}-${row.createdAt}`
    if (seen.has(key)) continue
    seen.add(key)
    list.push(row)
  }
  return list.sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)))
})

const filteredFlows = computed(() => {
  const kw = flowKeyword.value.trim().toLowerCase()
  return mergedFlows.value.filter((row) => {
    if (flowDirection.value && row.direction !== flowDirection.value) return false
    if (!kw) return true
    return [row.flowType, row.materialCode, row.materialName, row.refNo, row.operator]
      .some((v) => String(v || '').toLowerCase().includes(kw))
  })
})

async function loadData() {
  loading.value = true
  try {
    const [catalog, transactions] = await Promise.all([
      fetchWarehouseCatalog(),
      fetchWarehouseTransactions(),
      mes.hydrateForPage()
    ])
    catalogRows.value = catalog || []
    transactionRows.value = transactions || []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'flow' || tab === 'raw' || tab === 'finished') {
      activeTab.value = tab
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.inventory-page {
  min-height: 100%;
  background: #f5f7fa;
}

.inventory-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 12px 16px 0;
  background: #fff;
}

.inventory-tabs :deep(.el-tabs__content) {
  padding: 0;
}

.catalog-shell {
  margin: 12px 16px 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.catalog-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
}

.catalog-toolbar__left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.catalog-toolbar__title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.catalog-toolbar__meta {
  font-size: 12px;
  color: #909399;
}

.catalog-toolbar__filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.catalog-main {
  padding: 12px;
  overflow: auto;
}

.catalog-main--full {
  min-height: 520px;
}

.catalog-table {
  width: 100%;
}
</style>
