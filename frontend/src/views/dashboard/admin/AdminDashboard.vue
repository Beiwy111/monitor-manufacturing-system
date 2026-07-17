<template>
  <RoleWorkbenchScreen
    :data="data"
    :loading="loading"
    theme="admin"
    :filter-days="days"
    @refresh="load"
    @filter-change="onFilterChange"
  >
    <template #header-actions>
      <el-button
        class="global-ai-button"
        type="primary"
        size="small"
        round
        :loading="aiLoading"
        @click="aiVisible = true"
      >
        <el-icon><MagicStick /></el-icon>
        一键生成AI全局分析
      </el-button>
    </template>
  </RoleWorkbenchScreen>

  <FinanceAiAnalysisDialog
    v-model="aiVisible"
    analysis-type="global"
    :days="days"
    @loading-change="aiLoading = $event"
  />
</template>

<script setup>
import { ref } from 'vue'
import { MagicStick } from '@element-plus/icons-vue'
import RoleWorkbenchScreen from '@/components/workbench/RoleWorkbenchScreen.vue'
import FinanceAiAnalysisDialog from '@/components/workbench/FinanceAiAnalysisDialog.vue'
import { useRoleWorkbenchDashboard } from '@/composables/useRoleWorkbenchDashboard'

const { loading, data, load, days, onFilterChange } = useRoleWorkbenchDashboard('admin')
const aiVisible = ref(false)
const aiLoading = ref(false)
</script>

<style scoped>
.global-ai-button {
  border: 0;
  color: #073848;
  background: linear-gradient(135deg, #a7edff 0%, #00beff 100%);
  box-shadow: 0 4px 14px rgba(0, 190, 255, 0.24);
}

.global-ai-button:hover,
.global-ai-button:focus {
  color: #062e3d;
  background: linear-gradient(135deg, #c8f4ff 0%, #43ceff 100%);
}
</style>
