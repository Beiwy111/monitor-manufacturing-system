<template>
  <ModulePageShell>
    <div class="ws-header">
      <span class="ws-title">生产车间状态</span>
      <el-tag type="info" size="small">八道生产工序 · {{ workshops.length }} 车间 · 数据库活数据</el-tag>
      <el-button :loading="loading" style="margin-left:auto" @click="loadData">刷新</el-button>
    </div>

    <KpiStrip :cards="kpiCards" :metrics="kpi" />

    <div v-loading="loading" class="ws-card">
      <el-table :data="workshops" border stripe size="small" style="width:100%">
        <el-table-column prop="parentStepName" label="工序" min-width="100" />
        <el-table-column prop="name" label="车间" min-width="140" />
        <el-table-column label="设备" min-width="72" align="center">
          <template #default="{ row }">{{ row.total || row.machines?.length || 0 }}</template>
        </el-table-column>
        <el-table-column label="运行" min-width="72" align="center">
          <template #default="{ row }">{{ row.running || row.active || 0 }}</template>
        </el-table-column>
        <el-table-column label="故障" min-width="72" align="center">
          <template #default="{ row }">
            <span :style="{ color: Number(row.fault || row.abnormal) > 0 ? '#f56c6c' : '' }">
              {{ row.fault || row.abnormal || 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="88" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'running' ? 'success' : row.status === 'abnormal' ? 'danger' : 'info'"
              size="small"
            >
              {{ row.status === 'running' ? '运行中' : row.status === 'abnormal' ? '异常' : '待机' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </ModulePageShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ModulePageShell from '@/components/module/ModulePageShell.vue'
import KpiStrip from '@/components/module/KpiStrip.vue'
import { fetchEquipmentKpi, fetchEquipmentWorkshopOverview } from '@/api/business'
import { mergeWorkshopData } from '@/composables/useWorkshopScene'

const kpiCards = [
  { key: 'total', kpiKey: 'total', label: '设备总数', cls: '' },
  { key: 'normal', kpiKey: 'normal', label: '正常', cls: 'passed' },
  { key: 'fault', kpiKey: 'fault', label: '故障', cls: 'failed' },
  { key: 'maintaining', kpiKey: 'maintaining', label: '维保中', cls: 'recheck' },
  { key: 'openAlarms', kpiKey: 'openAlarms', label: '未闭环报警', cls: 'open' }
]

const loading = ref(false)
const kpi = ref({})
const workshops = ref([])

async function loadData() {
  loading.value = true
  try {
    const [kRes, ovRes] = await Promise.allSettled([
      fetchEquipmentKpi(),
      fetchEquipmentWorkshopOverview()
    ])
    const pick = (res, fallback) => (res.status === 'fulfilled' ? (res.value ?? fallback) : fallback)
    kpi.value = pick(kRes, {})
    workshops.value = mergeWorkshopData(pick(ovRes, {})?.workshops || [])
    if (kRes.status === 'rejected' && ovRes.status === 'rejected') {
      ElMessage.error(kRes.reason?.message || '加载车间状态失败')
    }
  } catch (e) {
    ElMessage.error(e?.message || '加载车间状态失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.ws-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.ws-title {
  font-size: 18px;
  font-weight: 700;
  color: #001b3f;
}
.ws-card {
  background: #fff;
  border-radius: 8px;
  padding: 12px 14px;
}
</style>
