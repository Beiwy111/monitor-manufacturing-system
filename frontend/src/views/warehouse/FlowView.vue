<template>
  <div class="flow-page">
    <div class="flow-header">
      <div class="flow-header__left">
        <span class="flow-title">库存流水</span>
        <el-tag type="info" size="small" style="margin-left:10px">共 {{ filtered.length }} 条</el-tag>
      </div>
      <div class="flow-header__right">
        <el-input v-model="filterMat" placeholder="物料名称/编码" clearable style="width:180px;margin-right:8px" />
        <el-select v-model="filterType" placeholder="交易类型" clearable style="width:150px;margin-right:8px">
          <el-option label="采购入库" value="PURCHASE_IN" />
          <el-option label="生产领料" value="ISSUE" />
          <el-option label="成品入库" value="FINISH_IN" />
          <el-option label="发货出库" value="DELIVERY_OUT" />
          <el-option label="盘点调整" value="ADJUSTMENT" />
          <el-option label="退货" value="RETURN" />
        </el-select>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div v-loading="loading" class="flow-table-wrap">
      <el-table :data="filtered" border stripe>
        <el-table-column prop="transactionNo" label="流水号" width="150" />
        <el-table-column label="物料" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="font-weight:500">{{ row.materialName || '-' }}</span>
            <span v-if="row.materialCode" style="color:#909399;font-size:12px;margin-left:6px">{{ row.materialCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="transactionType" label="类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="typeColor(row.transactionType)" size="small">
              {{ typeLabel(row.transactionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="100" align="right">
          <template #default="{ row }">
            <span :style="{ color: isIn(row.transactionType) ? '#67c23a' : '#f56c6c', fontWeight: 600 }">
              {{ isIn(row.transactionType) ? '+' : '-' }}{{ row.quantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="warehouseCode" label="仓库" width="90" />
        <el-table-column prop="locationCode" label="库位" width="90" />
        <el-table-column prop="batchNo" label="批次" width="110" show-overflow-tooltip />
        <el-table-column label="关联单据" width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.relatedPurchaseOrderId" style="color:#409eff;font-size:12px">
              PO#{{ row.relatedPurchaseOrderId }}
            </span>
            <span v-else-if="row.relatedWorkOrderId" style="color:#67c23a;font-size:12px">
              WO#{{ row.relatedWorkOrderId }}
            </span>
            <span v-else style="color:#bfbfbf">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="160" show-overflow-tooltip />
      </el-table>
      <div v-if="!loading && filtered.length === 0" class="flow-empty">暂无库存流水数据</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const list = ref([])
const loading = ref(false)
const filterMat = ref('')
const filterType = ref('')

const filtered = computed(() => {
  return list.value.filter(t => {
    if (filterType.value && t.transactionType !== filterType.value) return false
    if (filterMat.value) {
      const kw = filterMat.value.toLowerCase()
      if (!(t.materialName || '').toLowerCase().includes(kw) &&
          !(t.materialCode || '').toLowerCase().includes(kw)) return false
    }
    return true
  })
})

function typeLabel(t) {
  const map = {
    PURCHASE_IN: '采购入库', ISSUE: '生产领料', FINISH_IN: '成品入库',
    DELIVERY_OUT: '发货出库', ADJUSTMENT: '盘点调整', RETURN: '退货'
  }
  return map[t] || t || '-'
}

function typeColor(t) {
  const map = {
    PURCHASE_IN: 'success', ISSUE: 'warning', FINISH_IN: 'success',
    DELIVERY_OUT: 'danger', ADJUSTMENT: 'info', RETURN: 'warning'
  }
  return map[t] || ''
}

function isIn(t) {
  return ['PURCHASE_IN', 'FINISH_IN', 'RETURN', 'ADJUSTMENT'].includes(t)
}

async function load() {
  loading.value = true
  try {
    // fetch transactions and enrich with material info
    const txRes = await request.get('/material/transaction/list')
    const txList = txRes || []
    const matRes = await request.get('/material/material/list')
    const materials = matRes || []
    const matMap = Object.fromEntries(materials.map(m => [m.materialId, m]))

    list.value = txList.map(t => {
      const mat = matMap[t.materialId]
      return { ...t, materialName: mat?.materialName, materialCode: mat?.materialCode }
    }).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  } catch {
    ElMessage.error('加载库存流水失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.flow-page { padding: 16px 20px; background: #f5f7fa; min-height: 100%; }
.flow-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 14px; flex-wrap: wrap; gap: 10px;
}
.flow-header__left { display: flex; align-items: center; }
.flow-header__right { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.flow-title { font-size: 18px; font-weight: 700; color: #001b3f; }
.flow-table-wrap { background: #fff; border-radius: 6px; padding: 12px; min-height: 200px; }
.flow-empty { text-align: center; color: #909399; padding: 60px 0; font-size: 14px; }
</style>
