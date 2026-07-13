<template>
  <ModulePageShell>
    <div class="eq-header">
      <span class="eq-title">维护记录</span>
      <el-tag type="info" size="small">生产车间设备维保记录（数据库）</el-tag>
      <el-button :loading="loading" style="margin-left:auto" @click="loadData">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="records" border stripe size="small">
      <el-table-column prop="maintenanceNo" label="单号" width="130" />
      <el-table-column prop="equipmentCode" label="设备编码" width="100" />
      <el-table-column prop="equipmentName" label="设备名称" min-width="130" />
      <el-table-column prop="maintenanceTypeCn" label="类型" width="72" align="center" />
      <el-table-column label="结果" width="88" align="center">
        <template #default="{ row }">
          <el-tag :type="row.inProgress ? 'warning' : 'success'" size="small">{{ row.maintenanceResultCn }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="downtimeMinutes" label="停机(分)" width="88" align="right" />
      <el-table-column prop="maintainerName" label="维护人" width="88" />
      <el-table-column prop="startTime" label="开始时间" width="150" />
      <el-table-column prop="endTime" label="结束时间" width="150" />
    </el-table>
  </ModulePageShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ModulePageShell from '@/components/module/ModulePageShell.vue'
import { fetchMaintenanceViews } from '@/api/business'

const loading = ref(false)
const records = ref([])

async function loadData() {
  loading.value = true
  try {
    records.value = await fetchMaintenanceViews() || []
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
</style>
