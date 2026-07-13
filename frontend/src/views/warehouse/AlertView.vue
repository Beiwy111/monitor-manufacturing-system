<template>
  <div class="alert-page">
    <div class="alert-header">
      <div class="alert-header__left">
        <span class="alert-title">库存预警</span>
        <el-tag v-if="alerts.length > 0" type="danger" size="small" style="margin-left:10px">
          {{ alerts.length }} 种低于安全库存
        </el-tag>
        <el-tag v-else type="success" size="small" style="margin-left:10px">库存充足</el-tag>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <div v-if="alerts.length > 0" class="alert-tip">
      以下物料当前在库数量低于安全库存，请及时补货或发起采购。
    </div>

    <div v-loading="loading" class="alert-table-wrap">
      <el-table v-if="alerts.length > 0" :data="alerts" border stripe>
        <el-table-column prop="materialCode" label="物料编码" width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="100" />
        <el-table-column prop="quantityOnHand" label="当前在库" width="110" align="right">
          <template #default="{ row }">
            <span class="alert-qty">{{ row.quantityOnHand }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="110" align="right">
          <template #default="{ row }">
            <span style="color:#606266">{{ row.safetyStock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="缺口" width="100" align="right">
          <template #default="{ row }">
            <span class="gap-qty">
              -{{ (Number(row.safetyStock) - Number(row.quantityOnHand)).toFixed(4).replace(/\.?0+$/, '') }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="60" align="center" />
        <el-table-column prop="supplierName" label="默认供应商" width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.supplierName" type="info" size="small">{{ row.supplierName }}</el-tag>
            <span v-else style="color:#bfbfbf;font-size:12px">未分配</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && alerts.length === 0" class="alert-empty">
        <el-icon style="font-size:48px;color:#67c23a;margin-bottom:10px"><CircleCheck /></el-icon>
        <div>当前所有物料库存均在安全水位以上</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
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
    // join supplier info via material list
    const matRes = await request.get('/material/material/list')
    const materials = matRes || []
    const supRes = await request.get('/purchase/supplier/list')
    const suppliers = supRes?.data || []
    const supMap = Object.fromEntries(suppliers.map(s => [s.supplierId, s.supplierName]))

    alerts.value = all
      .filter(i => i.safetyStock != null && Number(i.quantityOnHand) < Number(i.safetyStock))
      .map(i => {
        const mat = materials.find(m => m.materialId === i.materialId)
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
</script>

<style scoped>
.alert-page { padding: 16px 20px; background: #f5f7fa; min-height: 100%; }
.alert-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 14px;
}
.alert-header__left { display: flex; align-items: center; }
.alert-title { font-size: 18px; font-weight: 700; color: #001b3f; }
.alert-tip {
  background: #fff3f0; border: 1px solid #ffd8d8; border-radius: 6px;
  padding: 8px 14px; margin-bottom: 12px; font-size: 13px; color: #c0392b;
}
.alert-table-wrap { background: #fff; border-radius: 6px; padding: 12px; min-height: 200px; }
.alert-empty { text-align: center; padding: 60px 0; color: #67c23a; font-size: 14px; }
.alert-qty { color: #f56c6c; font-weight: 700; }
.gap-qty { color: #f56c6c; font-weight: 600; }
</style>
