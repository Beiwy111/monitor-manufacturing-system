<template>
  <div class="smart-scheduling-page">
    <PlannerAgentWorkspace
      ref="workspaceRef"
      fullscreen
      :combined-batch="combinedBatch"
      :on-close="goBack"
      :on-success="onSuccessDone"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMesStore } from '@/stores/mes'
import PlannerAgentWorkspace from '@/components/mes/PlannerAgentWorkspace.vue'
import { resolvePlannerOrderIds } from '@/composables/usePlannerAgent'

const route = useRoute()
const router = useRouter()
const mes = useMesStore()
const workspaceRef = ref(null)

const combinedBatch = route.query.combined === '1'

function parseOrderIds() {
  const fromList = route.query.orderIds
  if (typeof fromList === 'string' && fromList.trim()) {
    return fromList.split(',').map((s) => s.trim()).filter(Boolean)
  }
  return resolvePlannerOrderIds(route.query.orderId, [])
}

function goBack() {
  const back = route.query.from
  if (typeof back === 'string' && back.startsWith('/')) {
    router.push(back)
    return
  }
  router.push('/production/plan')
}

function onSuccessDone() {
  goBack()
}

onMounted(async () => {
  if (!mes.hydrated) {
    try {
      await mes.hydrateForPage()
    } catch {
      /* ignore */
    }
  }
  const ids = parseOrderIds()
  workspaceRef.value?.initOrders(ids)
})
</script>

<style scoped>
.smart-scheduling-page {
  height: var(--layout-viewport-h, calc(100vh - 52px));
  max-height: var(--layout-viewport-h, calc(100vh - 52px));
  overflow: hidden;
}
</style>
