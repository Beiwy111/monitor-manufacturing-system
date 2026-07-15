<template>
  <section class="warehouse-alert-panel">
    <header class="warehouse-alert-panel__head">
      <div>
        <h3 class="warehouse-alert-panel__title">库存预警</h3>
        <p class="warehouse-alert-panel__sub">低于安全库存的物料将在此提示，便于结合库位容量安排补货</p>
      </div>
      <div class="warehouse-alert-panel__actions">
        <el-tag v-if="alerts.length > 0" type="danger" size="small">{{ alerts.length }} 种需补货</el-tag>
        <el-tag v-else type="success" size="small">库存充足</el-tag>
        <el-button size="small" :loading="loading" @click="load">刷新</el-button>
      </div>
    </header>

    <div v-if="alerts.length > 0" class="warehouse-alert-panel__tip">
      以下物料当前在库数量低于安全库存，请及时补货或发起采购。
    </div>

    <div v-loading="loading" class="warehouse-alert-panel__body">
      <el-table v-if="alerts.length > 0" :data="alerts" border stripe size="small" class="warehouse-alert-panel__table">
        <el-table-column prop="materialCode" label="物料编码" width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="100" />
        <el-table-column prop="quantityOnHand" label="当前在库" width="100" align="right">
          <template #default="{ row }">
            <span class="alert-qty">{{ row.quantityOnHand }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="100" align="right" />
        <el-table-column label="缺口" width="90" align="right">
          <template #default="{ row }">
            <span class="gap-qty">
              -{{ (Number(row.safetyStock) - Number(row.quantityOnHand)).toFixed(4).replace(/\.?0+$/, '') }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="60" align="center" />
        <el-table-column prop="supplierName" label="默认供应商" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.supplierName" type="info" size="small">{{ row.supplierName }}</el-tag>
            <span v-else class="muted">未分配</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-else-if="!loading" class="warehouse-alert-panel__empty">
        <el-icon class="warehouse-alert-panel__ok"><CircleCheck /></el-icon>
        <span>当前所有物料库存均在安全水位以上</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const alerts = ref([])

async function load() {
  loading.value = true
  try {
    const res = await request.get('/material/inventory/full')
    const all = res?.data || res || []
    const matRes = await request.get('/material/material/list')
    const materials = matRes || []
    const supRes = await request.get('/purchase/supplier/list')
    const suppliers = supRes?.data || []
    const supMap = Object.fromEntries(suppliers.map((s) => [s.supplierId, s.supplierName]))

    alerts.value = all
      .filter((i) => i.safetyStock != null && Number(i.quantityOnHand) < Number(i.safetyStock))
      .map((i) => {
        const mat = materials.find((m) => m.materialId === i.materialId)
        return { ...i, supplierName: mat?.supplierId ? supMap[mat.supplierId] : null }
      })
      .sort((a, b) => {
        const gapA = Number(a.safetyStock) - Number(a.quantityOnHand)
        const gapB = Number(b.safetyStock) - Number(b.quantityOnHand)
        return gapB - gapA
      })
  } catch {
    ElMessage.error('加载预警数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)

defineExpose({ reload: load })
</script>

<style scoped>
.warehouse-alert-panel {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 18px 20px 20px;
}

.warehouse-alert-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.warehouse-alert-panel__title {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}

.warehouse-alert-panel__sub {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

.warehouse-alert-panel__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.warehouse-alert-panel__tip {
  background: #fff3f0;
  border: 1px solid #ffd8d8;
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #c0392b;
}

.warehouse-alert-panel__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 28px 0;
  color: #67c23a;
  font-size: 14px;
}

.warehouse-alert-panel__ok {
  font-size: 22px;
}

.alert-qty { color: #f56c6c; font-weight: 700; }
.gap-qty { color: #e6a23c; font-weight: 700; }
.muted { color: #bfbfbf; font-size: 12px; }
</style>
