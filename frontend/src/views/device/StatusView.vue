<template>
  <ModulePageShell>
    <div class="eq-header">
      <span class="eq-title">设备状态</span>
      <el-tag type="info" size="small">按八道生产工序 · 19 车间展示</el-tag>
      <el-button :loading="loading" style="margin-left:auto" @click="loadData">刷新</el-button>
    </div>
    <div v-for="stage in stageGroups" :key="stage.stepKey" class="stage-block">
      <div class="stage-block__title">{{ stage.stepName }}（{{ stage.items.length }} 台）</div>
      <el-table :data="stage.items" border stripe size="small">
        <el-table-column prop="equipmentCode" label="编码" width="100" />
        <el-table-column prop="equipmentName" label="设备" min-width="130" />
        <el-table-column prop="workshop" label="车间" width="130" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.statusCn }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="openAlarmCount" label="未闭环报警" width="100" align="center" />
      </el-table>
    </div>
  </ModulePageShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ModulePageShell from '@/components/module/ModulePageShell.vue'
import { fetchEquipmentViews } from '@/api/business'
import { PRODUCTION_STAGES } from '@/utils/productionProgress'
import { moduleStatusType } from '@/constants/moduleStatus'

const loading = ref(false)
const list = ref([])

const stageGroups = computed(() =>
  PRODUCTION_STAGES.map((stage) => ({
    stepKey: stage.stepKey,
    stepName: stage.stepName,
    items: list.value.filter((e) => e.parentStepKey === stage.stepKey)
  }))
)

function statusType(s) {
  return moduleStatusType('equipmentStatus', s)
}
async function loadData() {
  loading.value = true
  try {
    const rows = await fetchEquipmentViews() || []
    list.value = rows.filter((e) => e.isProductionWorkshop !== false)
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
.stage-block { margin-bottom: 16px; }
.stage-block__title { font-size: 14px; font-weight: 600; color: #001b3f; margin-bottom: 8px; }
</style>
