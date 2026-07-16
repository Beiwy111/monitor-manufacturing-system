<template>
  <div class="pi-page">
    <div class="pi-header">
      <div class="pi-header__left">
        <span class="pi-title">采购入库</span>
        <el-tag v-if="pendingOrders.length" type="warning" size="small" style="margin-left:10px">
          {{ pendingOrders.length }} 单待入库
        </el-tag>
      </div>
      <div class="pi-header__right">
        <el-button link type="primary" @click="router.push('/warehouse/location-map')">库位图</el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <section v-if="pendingOrders.length" class="pi-pending">
      <div class="pi-pending__title">待到货采购单 — 请选择库位后确认入库</div>
      <el-table :data="pendingOrders" border stripe style="width:100%">
        <el-table-column prop="purchaseOrderNo" label="采购单号" min-width="130" />
        <el-table-column prop="supplierName" label="供应商" min-width="120" show-overflow-tooltip />
        <el-table-column label="物料明细" min-width="220">
          <template #default="{ row }">
            <div v-for="item in row.items" :key="item.purchaseOrderItemId" class="pi-line">
              {{ item.materialName }} × {{ item.quantity }}{{ item.unit || '' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openPutaway(row)">选择库位入库</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <div class="pi-subhead">
      <span>在库库存快照</span>
      <el-input v-model="filterMat" placeholder="物料名称/编码" clearable style="width:180px" />
    </div>

    <div v-loading="loading" class="pi-table-wrap pi-table-wrap--compact">
      <el-table :data="filtered" border stripe style="width:100%">
        <el-table-column prop="materialCode" label="物料编码" min-width="120" show-overflow-tooltip />
        <el-table-column prop="materialName" label="物料名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="入库仓库" min-width="110" show-overflow-tooltip />
        <el-table-column label="库位储位" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.slotLabel || row.locationCode || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="quantityOnHand" label="当前在库" min-width="100" align="right">
          <template #default="{ row }">
            <span class="pi-num pi-num--ok">{{ row.quantityOnHand }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="quantityReserved" label="占用数量" min-width="100" align="right" />
        <el-table-column prop="quantityAvailable" label="可用数量" min-width="100" align="right">
          <template #default="{ row }">
            <span class="pi-num">{{ row.quantityAvailable }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="72" align="center" />
        <el-table-column prop="inventoryStatus" label="状态" width="88" align="center">
          <template #default="{ row }">
            <el-tag :type="row.inventoryStatus === 'NORMAL' ? 'success' : 'warning'" size="small">
              {{ row.inventoryStatus === 'NORMAL' ? '正常' : row.inventoryStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后操作时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatCompactDateTime(row.lastTransactionAt) }}</template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && filtered.length === 0" class="pi-empty">暂无采购入库数据</div>
    </div>

    <el-dialog
      v-model="putawayVisible"
      :title="`采购入库 — ${currentOrder?.purchaseOrderNo || ''}`"
      width="720px"
      destroy-on-close
    >
      <el-table :data="putawayLines" border stripe>
        <el-table-column prop="materialName" label="物料" min-width="140" />
        <el-table-column prop="quantity" label="数量" width="90" align="right" />
        <el-table-column label="存放库位" min-width="280">
          <template #default="{ row }">
            <LocationSlotPicker
              v-model="row.slotCode"
              :zone-code="row.zoneCode"
              :placeholder="`为 ${row.materialName} 选择库位`"
            />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="putawayVisible = false">取消</el-button>
        <el-button type="primary" :loading="putawayLoading" @click="submitPutaway">确认入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  fetchWarehouseInventory,
  fetchPendingPurchaseArrivals,
  confirmPurchaseArrivalWithSlots
} from '@/api/warehouse'
import { formatCompactDateTime } from '@/utils/formatCompactDateTime'
import LocationSlotPicker from '@/components/warehouse/LocationSlotPicker.vue'

const router = useRouter()
const list = ref([])
const pendingOrders = ref([])
const loading = ref(false)
const filterMat = ref('')
const putawayVisible = ref(false)
const putawayLoading = ref(false)
const currentOrder = ref(null)
const putawayLines = ref([])

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
    const [invRes, pendingRes] = await Promise.all([
      fetchWarehouseInventory(),
      fetchPendingPurchaseArrivals()
    ])
    const all = invRes?.data || invRes || []
    list.value = all.filter(i => {
      const wh = i.warehouseCode || ''
      return wh !== 'WH-02' && wh !== 'WH02' && !wh.startsWith('FG')
    })
    pendingOrders.value = pendingRes?.data || pendingRes || []
  } catch {
    ElMessage.error('加载库存数据失败')
  } finally {
    loading.value = false
  }
}

function openPutaway(order) {
  currentOrder.value = order
  putawayLines.value = (order.items || []).map(item => ({
    materialId: item.materialId,
    materialName: item.materialName,
    quantity: item.quantity,
    zoneCode: item.zoneCode || 'RM-WH',
    slotCode: ''
  }))
  putawayVisible.value = true
}

async function submitPutaway() {
  if (!currentOrder.value) return
  const missing = putawayLines.value.find(line => !line.slotCode)
  if (missing) {
    ElMessage.warning(`请为「${missing.materialName}」选择库位`)
    return
  }
  putawayLoading.value = true
  try {
    await confirmPurchaseArrivalWithSlots({
      purchaseOrderId: currentOrder.value.purchaseOrderId,
      assignments: putawayLines.value.map(line => ({
        materialId: line.materialId,
        slotCode: line.slotCode
      }))
    })
    ElMessage.success('采购入库成功，库位图已更新')
    putawayVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e?.message || '入库失败')
  } finally {
    putawayLoading.value = false
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
.pi-pending {
  background: #fff;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
}
.pi-pending__title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}
.pi-line { font-size: 13px; color: #606266; line-height: 1.6; }
.pi-subhead {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.pi-table-wrap { background: #fff; border-radius: 6px; padding: 12px; min-height: 200px; }
.pi-empty { text-align: center; color: #909399; padding: 60px 0; font-size: 14px; }
.pi-num { font-weight: 600; }
.pi-num--ok { color: #67c23a; }
</style>
