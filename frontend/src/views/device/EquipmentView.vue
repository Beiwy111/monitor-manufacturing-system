<template>
  <ModulePageShell>
    <div class="eq-header">
      <span class="eq-title">设备台账</span>
      <el-tag type="info" size="small">四道工序 · 11 个生产车间 · 数据库活数据</el-tag>
      <el-button :loading="loading" style="margin-left:auto" @click="loadData">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="productionList" border stripe highlight-current-row @current-change="onRowClick">
      <el-table-column prop="equipmentCode" label="设备编码" width="110" />
      <el-table-column prop="equipmentName" label="设备名称" min-width="140" />
      <el-table-column prop="parentStepName" label="工序" width="100" />
      <el-table-column prop="workshop" label="车间" min-width="130" />
      <el-table-column prop="equipmentType" label="类型" width="100" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.statusCn }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastMaintenanceAt" label="最近维护" width="150" />
    </el-table>
    <div v-if="selected" class="eq-detail">
      <el-descriptions :column="3" border size="small" title="设备详情">
        <el-descriptions-item label="编码">{{ selected.equipmentCode }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ selected.equipmentName }}</el-descriptions-item>
        <el-descriptions-item label="工序">{{ selected.parentStepName }}</el-descriptions-item>
        <el-descriptions-item label="车间">{{ selected.workshop }}</el-descriptions-item>
        <el-descriptions-item label="工位">{{ selected.workstation || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ selected.statusCn }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </ModulePageShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ModulePageShell from '@/components/module/ModulePageShell.vue'
import { fetchEquipmentViews } from '@/api/business'
import { moduleStatusType } from '@/constants/moduleStatus'

const loading = ref(false)
const list = ref([])
const selected = ref(null)

const productionList = computed(() => list.value.filter((e) => e.isProductionWorkshop !== false))

function statusType(s) {
  return moduleStatusType('equipmentStatus', s)
}
function onRowClick(row) {
  selected.value = row
}
async function loadData() {
  loading.value = true
  try {
    list.value = await fetchEquipmentViews() || []
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}
onMounted(loadData)
</script>

<style scoped>
.eq-header { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.eq-title { font-size: 18px; font-weight: 700; color: #001b3f; }
.eq-detail { margin-top: 14px; }
</style>
