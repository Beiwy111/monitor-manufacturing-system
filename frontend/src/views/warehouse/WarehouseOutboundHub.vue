<template>
  <div class="warehouse-hub">
    <el-tabs v-model="activeTab" class="warehouse-hub__tabs" @tab-change="syncRoute">
      <el-tab-pane label="领料记录" name="issue" lazy>
        <IssueView />
      </el-tab-pane>
      <el-tab-pane label="发货出库" name="delivery" lazy>
        <DeliveryView />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import IssueView from './IssueView.vue'
import DeliveryView from '@/views/delivery/DeliveryView.vue'

const route = useRoute()
const router = useRouter()
const activeTab = ref('issue')

function syncRoute(name) {
  router.replace({ query: { ...route.query, tab: name } })
}

function syncFromRoute() {
  const tab = route.query.tab
  if (tab === 'delivery' || tab === 'issue') {
    activeTab.value = tab
  }
}

watch(() => route.query.tab, syncFromRoute, { immediate: true })
</script>

<style scoped>
.warehouse-hub {
  padding: 0;
}

.warehouse-hub__tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.warehouse-hub__tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 600;
}
</style>
