<template>
  <div class="issue-page">
    <div class="issue-header">
      <div class="issue-header__left">
        <span class="issue-title">生产领料看板</span>
        <el-tag v-if="shortfalls > 0" type="danger" size="small" style="margin-left:10px">
          {{ shortfalls }} 种物料库存不足
        </el-tag>
        <el-tag v-else-if="!loading && issueLines.length > 0" type="success" size="small" style="margin-left:10px">
          物料充足
        </el-tag>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-alert v-if="!loading && activeOrders.length === 0" type="info" :closable="false"
      title="当前无在制工单，暂无领料需求" style="margin-bottom:14px" />

    <div v-if="activeOrders.length > 0" class="order-list">
      <div v-for="wo in activeOrders" :key="wo.workOrderId" class="wo-card">
        <div class="wo-card__header">
          <div class="wo-card__info">
            <el-tag type="primary" size="small">在制</el-tag>
            <span class="wo-card__no">{{ wo.workOrderNo }}</span>
            <span class="wo-card__product">{{ wo.productName }}</span>
          </div>
          <div class="wo-card__qty">
            计划量 <strong>{{ wo.plannedQuantity }}</strong> 台
          </div>
        </div>

        <el-table :data="wo.bomLines" border size="small" style="margin-top:8px">
          <el-table-column prop="materialCode" label="物料编码" width="120" />
          <el-table-column prop="materialName" label="物料名称" min-width="140" show-overflow-tooltip />
          <el-table-column label="需求量" width="110" align="right">
            <template #default="{ row }">
              <span style="font-weight:600">{{ row.requiredQty }}</span>
              <span style="color:#909399;font-size:11px;margin-left:3px">{{ row.unit }}</span>
            </template>
          </el-table-column>
          <el-table-column label="可用库存" width="110" align="right">
            <template #default="{ row }">
              <span :class="row.available < row.requiredQty ? 'shortage-qty' : 'ok-qty'">
                {{ row.available }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="缺口" width="90" align="right">
            <template #default="{ row }">
              <span v-if="row.available < row.requiredQty" class="shortage-qty">
                -{{ (row.requiredQty - row.available).toFixed(4).replace(/\.?0+$/, '') }}
              </span>
              <el-icon v-else style="color:#67c23a"><Select /></el-icon>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.available >= row.requiredQty ? 'success' : 'danger'" size="small">
                {{ row.available >= row.requiredQty ? '可领料' : '库存不足' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div v-if="loading" v-loading="loading" style="height:200px" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Select } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const activeOrders = ref([])

const shortfalls = computed(() =>
  activeOrders.value.reduce((sum, wo) =>
    sum + wo.bomLines.filter(l => l.available < l.requiredQty).length, 0)
)

async function load() {
  loading.value = true
  try {
    const [woRes, bomRes, invRes, matRes] = await Promise.all([
      request.get('/production/workOrder/list'),
      request.get('/material/bom/list'),
      request.get('/material/inventory/full'),
      request.get('/material/material/list')
    ])

    const workOrders = woRes || []
    const boms = bomRes || []
    const inventory = invRes?.data || invRes || []
    const materials = matRes || []

    const matMap = Object.fromEntries(materials.map(m => [m.materialId, m]))
    const invMap = {}
    for (const inv of inventory) {
      const existing = invMap[inv.materialId]
      if (!existing) {
        invMap[inv.materialId] = { available: Number(inv.quantityAvailable), unit: inv.unit }
      } else {
        existing.available += Number(inv.quantityAvailable)
      }
    }

    // group BOMs by parent
    const bomByParent = {}
    for (const b of boms) {
      if (!b.status && b.status !== undefined) continue
      if (!bomByParent[b.parentMaterialId]) bomByParent[b.parentMaterialId] = []
      bomByParent[b.parentMaterialId].push(b)
    }

    const producing = workOrders.filter(wo =>
      wo.status === 'PRODUCING' || wo.status === '生产中'
    )

    activeOrders.value = producing.map(wo => {
      const bomLines = (bomByParent[wo.materialId] || []).map(b => {
        const child = matMap[b.childMaterialId]
        const inv = invMap[b.childMaterialId] || { available: 0, unit: '' }
        const requiredQty = Number(b.quantity) * Number(wo.plannedQuantity)
        return {
          materialId: b.childMaterialId,
          materialCode: child?.materialCode || '-',
          materialName: child?.materialName || `物料${b.childMaterialId}`,
          unit: child?.unit || inv.unit || '件',
          bomQty: Number(b.quantity),
          requiredQty: parseFloat(requiredQty.toFixed(4)),
          available: inv.available
        }
      })
      const product = matMap[wo.materialId]
      return {
        workOrderId: wo.workOrderId,
        workOrderNo: wo.workOrderNo,
        productName: product?.materialName || `产品${wo.materialId}`,
        plannedQuantity: wo.plannedQuantity,
        bomLines
      }
    }).filter(wo => wo.bomLines.length > 0)
  } catch (e) {
    ElMessage.error('加载领料数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.issue-page { padding: 16px 20px; background: #f5f7fa; min-height: 100%; }
.issue-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 14px;
}
.issue-header__left { display: flex; align-items: center; }
.issue-title { font-size: 18px; font-weight: 700; color: #001b3f; }
.order-list { display: flex; flex-direction: column; gap: 16px; }
.wo-card {
  background: #fff; border-radius: 8px; padding: 14px 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
}
.wo-card__header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 4px; flex-wrap: wrap; gap: 8px;
}
.wo-card__info { display: flex; align-items: center; gap: 8px; }
.wo-card__no { font-size: 14px; font-weight: 700; color: #001b3f; }
.wo-card__product { font-size: 13px; color: #606266; }
.wo-card__qty { font-size: 13px; color: #606266; }
.shortage-qty { color: #f56c6c; font-weight: 700; }
.ok-qty { color: #67c23a; font-weight: 600; }
</style>
