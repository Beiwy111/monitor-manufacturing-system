<template>
  <RoleWorkbenchScreen
    :data="data"
    :loading="loading"
    theme="cost"
    :filter-days="days"
    @refresh="load"
    @filter-change="onFilterChange"
  >
    <template #header-actions>
      <el-button
        class="finance-ai-button"
        type="primary"
        size="small"
        round
        :loading="aiLoading"
        @click="aiVisible = true"
      >
        <el-icon><MagicStick /></el-icon>
        一键生成AI财务分析
      </el-button>
    </template>
  </RoleWorkbenchScreen>

  <FinanceAiAnalysisDialog
    v-model="aiVisible"
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

const { loading, data, load, days, onFilterChange } = useRoleWorkbenchDashboard('cost')
const aiVisible = ref(false)
const aiLoading = ref(false)
</script>

<style scoped>
.finance-ai-button {
  border: 0;
  color: #073848;
  background: linear-gradient(135deg, #7de4ff 0%, #00beff 100%);
  box-shadow: 0 4px 14px rgba(0, 190, 255, 0.24);
}

.finance-ai-button:hover,
.finance-ai-button:focus {
  color: #062e3d;
  background: linear-gradient(135deg, #a5ecff 0%, #39caff 100%);
}
</style>
