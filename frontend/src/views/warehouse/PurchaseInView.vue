<template>
  <div class="pi-page">
    <div class="pi-header">
      <div class="pi-header__left">
        <span class="pi-title">采购入库记录</span>
        <el-tag type="info" size="small" style="margin-left:10px">共 {{ filtered.length }} 条</el-tag>
      </div>
      <div class="pi-header__right">
        <el-input v-model="filterMat" placeholder="物料名称/编码" clearable style="width:180px;margin-right:8px" />
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div v-loading="loading" class="pi-table-wrap">
      <el-table :data="filtered" border stripe>
        <el-table-column label="物料" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="font-weight:500">{{ row.materialName || '-' }}</span>
            <span v-if="row.materialCode" style="color:#909399;font-size:12px;margin-left:6px">{{ row.materialCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="入库仓库" width="110" />
        <el-table-column prop="locationCode" label="库位" width="90" />
        <el-table-column prop="quantityOnHand" label="当前在库" width="110" align="right">
          <template #default="{ row }">
            <span style="font-weight:600;color:#67c23a">{{ row.quantityOnHand }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="quantityReserved" label="占用数量" width="100" align="right" />
        <el-table-column prop="quantityAvailable" label="可用数量" width="100" align="right">
          <template #default="{ row }">
            <span style="font-weight:600">{{ row.quantityAvailable }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="60" align="center" />
        <el-table-column prop="inventoryStatus" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.inventoryStatus === 'NORMAL' ? 'success' : 'warning'" size="small">
              {{ row.inventoryStatus === 'NORMAL' ? '正常' : row.inventoryStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastTransactionAt" label="最后操作时间" width="160" show-overflow-tooltip />
      </el-table>
      <div v-if="!loading && filtered.length === 0" class="pi-empty">暂无采购入库数据</div>
    </div>
    <div class="pi-tip">
      采购到货后通过「采购订单」页面确认到货，库存将自动更新。此页展示当前原材料库存快照。
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

const filtered = computed(() => {
  if (!filterMat.value) return list.value
  const kw = filterMat.value.toLowerCase()
  return list.value.filter(i =>
    (i.materialName || '').toLowerCase().includes(kw) ||
    (i.materialCode || '').toLowerCase().includes(kw)
  )
})

async function load() {
  loading.value = true
  try {
    const res = await request.get('/material/inventory/full')
    const all = res?.data || res || []
    // show raw materials only (not finished goods warehouse WH-02)
    list.value = all.filter(i => i.warehouseCode !== 'WH-02' && i.warehouseCode !== 'WH02')
  } catch {
    ElMessage.error('加载库存数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.pi-page { padding: 16px 20px; background: #f5f7fa; min-height: 100%; }
.pi-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 14px; flex-wrap: wrap; gap: 10px;
}
.pi-header__left { display: flex; align-items: center; }
.pi-header__right { display: flex; align-items: center; gap: 6px; }
.pi-title { font-size: 18px; font-weight: 700; color: #001b3f; }
.pi-table-wrap { background: #fff; border-radius: 6px; padding: 12px; min-height: 200px; }
.pi-empty { text-align: center; color: #909399; padding: 60px 0; font-size: 14px; }
.pi-tip {
  margin-top: 12px; padding: 8px 14px;
  background: #f0f9eb; border-radius: 6px;
  font-size: 13px; color: #67c23a;
}
</style>
