<template>
  <div class="settlement-panel">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="结算单/工单" clearable size="small" style="width:200px" />
      <el-button type="primary" size="small" @click="openDrawer(null)">+ 登记成本</el-button>
    </div>
    <el-table :data="filtered" border stripe size="small" highlight-current-row max-height="420" @current-change="onSelect">
      <el-table-column prop="settlementNo" label="结算单号" width="145" />
      <el-table-column prop="workOrderNo" label="工单" width="120" />
      <el-table-column label="来源" width="105">
        <template #default="{ row }"><el-tag size="small">{{ row.sourceTypeCn }}</el-tag></template>
      </el-table-column>
      <el-table-column label="合计" width="100" align="right">
        <template #default="{ row }"><b>¥ {{ fmtMoney(row.totalCost) }}</b></template>
      </el-table-column>
      <el-table-column label="状态" width="82">
        <template #default="{ row }"><el-tag size="small">{{ row.settlementStatusCn }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="settlementPeriod" label="期间" width="85" />
    </el-table>

    <el-drawer v-model="drawerVisible" :title="selected ? '结算详情' : '登记成本'" size="400px">
      <template v-if="selected">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="结算单号">{{ selected.settlementNo }}</el-descriptions-item>
          <el-descriptions-item label="工单">{{ selected.workOrderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ selected.sourceTypeCn }}</el-descriptions-item>
          <el-descriptions-item label="材料">¥ {{ fmtMoney(selected.materialCost) }}</el-descriptions-item>
          <el-descriptions-item label="人工">¥ {{ fmtMoney(selected.laborCost) }}</el-descriptions-item>
          <el-descriptions-item label="设备">¥ {{ fmtMoney(selected.equipmentCost) }}</el-descriptions-item>
          <el-descriptions-item label="质检">¥ {{ fmtMoney(selected.qualityCost) }}</el-descriptions-item>
          <el-descriptions-item label="其他">¥ {{ fmtMoney(selected.otherCost) }}</el-descriptions-item>
          <el-descriptions-item label="合计">¥ {{ fmtMoney(selected.totalCost) }}</el-descriptions-item>
        </el-descriptions>
        <div class="drawer-actions">
          <el-button v-if="selected.settlementStatus==='DRAFT'" type="primary" size="small" :loading="acting" @click="doConfirm">确认结算</el-button>
          <el-button v-if="selected.settlementStatus==='CONFIRMED'" type="success" size="small" :loading="acting" @click="doExport">标记已导出</el-button>
        </div>
      </template>
      <template v-else>
        <el-form :model="form" label-width="80px" size="small">
          <el-form-item label="来源类型"><el-input v-model="form.sourceType" placeholder="WORK_ORDER" /></el-form-item>
          <el-form-item label="材料"><el-input-number v-model="form.materialCost" :min="0" :precision="2" style="width:100%" /></el-form-item>
          <el-form-item label="人工"><el-input-number v-model="form.laborCost" :min="0" :precision="2" style="width:100%" /></el-form-item>
          <el-form-item label="设备"><el-input-number v-model="form.equipmentCost" :min="0" :precision="2" style="width:100%" /></el-form-item>
          <el-form-item label="质检"><el-input-number v-model="form.qualityCost" :min="0" :precision="2" style="width:100%" /></el-form-item>
          <el-form-item label="其他"><el-input-number v-model="form.otherCost" :min="0" :precision="2" style="width:100%" /></el-form-item>
          <el-form-item label="期间"><el-input v-model="form.settlementPeriod" placeholder="2026-07" /></el-form-item>
          <el-form-item label="原因"><el-input v-model="form.costReason" type="textarea" rows="2" /></el-form-item>
        </el-form>
        <el-button type="primary" size="small" :loading="acting" @click="doSave">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { fetchSettlementViews, confirmSettlement, exportSettlement, saveSettlement } from '@/api/cost'
import { fmtMoney } from '@/constants/financeWorkflow'

const userStore = useUserStore()
const list = ref([])
const keyword = ref('')
const selected = ref(null)
const drawerVisible = ref(false)
const acting = ref(false)
const form = reactive({
  sourceType: 'OTHER', materialCost: 0, laborCost: 0, equipmentCost: 0,
  qualityCost: 0, otherCost: 0, settlementPeriod: '2026-07', costReason: ''
})

const filtered = computed(() => {
  if (!keyword.value) return list.value
  const kw = keyword.value.toLowerCase()
  return list.value.filter(r => (r.settlementNo || '').toLowerCase().includes(kw) || (r.workOrderNo || '').toLowerCase().includes(kw))
})

function onSelect(row) { selected.value = row || null; if (row) { drawerVisible.value = true } }
function openDrawer(row) { selected.value = row; drawerVisible.value = true }

async function load() {
  list.value = await fetchSettlementViews() ?? []
}

async function doConfirm() {
  acting.value = true
  try {
    await confirmSettlement(selected.value.settlementId, userStore.userInfo?.username)
    ElMessage.success('已确认'); await load()
  } finally { acting.value = false }
}

async function doExport() {
  acting.value = true
  try {
    await exportSettlement(selected.value.settlementId, userStore.userInfo?.username)
    ElMessage.success('已导出'); await load()
  } finally { acting.value = false }
}

async function doSave() {
  acting.value = true
  try {
    await saveSettlement({
      settlementNo: 'CS' + Date.now(),
      ...form,
      settlementStatus: 'DRAFT'
    })
    ElMessage.success('已登记'); drawerVisible.value = false; await load()
  } finally { acting.value = false }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display:flex; gap:8px; margin-bottom:10px; }
.drawer-actions { margin-top:16px; display:flex; gap:8px; }
</style>
