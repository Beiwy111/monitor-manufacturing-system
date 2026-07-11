<template>
  <div class="inventory-page">
    <div class="inv-header">
      <div class="inv-header__left">
        <span class="inv-title">库存查询</span>
        <el-tag v-if="alertCount > 0" type="danger" size="small" style="margin-left:10px">
          {{ alertCount }} 种低于安全库存
        </el-tag>
      </div>
      <div class="inv-header__right">
        <el-input v-model="filterName" placeholder="物料名称/编码" clearable style="width:200px;margin-right:8px" />
        <el-select v-model="filterWarehouse" placeholder="仓库" clearable style="width:130px;margin-right:8px">
          <el-option v-for="w in warehouseOptions" :key="w" :label="w" :value="w" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width:120px;margin-right:8px">
          <el-option label="正常" value="NORMAL" />
          <el-option label="冻结" value="FROZEN" />
          <el-option label="盘点中" value="COUNTING" />
        </el-select>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div v-loading="loading" class="inv-table-wrap">
      <el-table :data="filtered" border stripe highlight-current-row>
        <el-table-column prop="materialCode" label="物料编码" width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="100" />
        <el-table-column prop="locationCode" label="库位" width="100" />
        <el-table-column prop="quantityOnHand" label="在库数量" width="110" align="right">
          <template #default="{ row }">
            <span :class="isAlert(row) ? 'alert-qty' : ''">
              {{ row.quantityOnHand }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="quantityReserved" label="占用数量" width="100" align="right" />
        <el-table-column prop="quantityAvailable" label="可用数量" width="100" align="right">
          <template #default="{ row }">
            <span style="font-weight:600">{{ row.quantityAvailable }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="100" align="right">
          <template #default="{ row }">
            <span :class="isAlert(row) ? 'alert-qty' : 'safe-qty'">{{ row.safetyStock ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="60" align="center" />
        <el-table-column prop="inventoryStatus" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.inventoryStatus)" size="small">
              {{ statusLabel(row.inventoryStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastTransactionAt" label="最后出入库" width="160" show-overflow-tooltip />
      </el-table>
      <div v-if="!loading && filtered.length === 0" class="inv-empty">暂无库存数据</div>
    </div>

    <div class="inv-summary" v-if="list.length > 0">
      共 {{ list.length }} 条库存记录，总在库：{{ totalOnHand.toLocaleString() }} 件
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const list = ref([])
const loading = ref(false)
const filterName = ref('')
const filterWarehouse = ref('')
const filterStatus = ref('')

const warehouseOptions = computed(() => [...new Set(list.value.map(i => i.warehouseName).filter(Boolean))])

const filtered = computed(() => {
  return list.value.filter(i => {
    if (filterName.value) {
      const kw = filterName.value.toLowerCase()
      if (!(i.materialName || '').toLowerCase().includes(kw) &&
          !(i.materialCode || '').toLowerCase().includes(kw)) return false
    }
    if (filterWarehouse.value && i.warehouseName !== filterWarehouse.value) return false
    if (filterStatus.value && i.inventoryStatus !== filterStatus.value) return false
    return true
  })
})

const alertCount = computed(() =>
  list.value.filter(i => isAlert(i)).length
)

const totalOnHand = computed(() =>
  list.value.reduce((s, i) => s + (Number(i.quantityOnHand) || 0), 0)
)

function isAlert(row) {
  return row.safetyStock != null &&
    Number(row.quantityOnHand) < Number(row.safetyStock)
}

function statusLabel(s) {
  const map = { NORMAL: '正常', FROZEN: '冻结', COUNTING: '盘点中' }
  return map[s] || s || '-'
}

function statusType(s) {
  const map = { NORMAL: 'success', FROZEN: 'warning', COUNTING: 'info' }
  return map[s] || ''
}

async function load() {
  loading.value = true
  try {
    const res = await request.get('/material/inventory/full')
    list.value = res?.data || res || []
  } catch {
    ElMessage.error('加载库存数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.inventory-page { padding: 16px 20px; background: #f5f7fa; min-height: 100%; }
.inv-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 14px; flex-wrap: wrap; gap: 10px;
}
.inv-header__left { display: flex; align-items: center; }
.inv-header__right { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.inv-title { font-size: 18px; font-weight: 700; color: #001b3f; }
.inv-table-wrap { background: #fff; border-radius: 6px; padding: 12px; min-height: 200px; }
.inv-empty { text-align: center; color: #909399; padding: 60px 0; font-size: 14px; }
.inv-summary { margin-top: 10px; font-size: 13px; color: #606266; text-align: right; }
.alert-qty { color: #f56c6c; font-weight: 700; }
.safe-qty { color: #67c23a; }
</style>
