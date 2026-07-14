<template>
  <el-dialog
    v-model="visible"
    title="智能排产"
    width="1120px"
    destroy-on-close
    class="planner-dialog"
    :close-on-click-modal="!previewLoading && phase !== 'analyzing'"
    :show-close="!previewLoading"
    @closed="onClosed"
  >
    <PlannerAgentWorkspace
      ref="workspaceRef"
      :combined-batch="combinedBatch"
      :on-close="() => { visible = false }"
      :on-success="onSuccessDone"
    />
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch, nextTick } from 'vue'
import PlannerAgentWorkspace from '@/components/mes/PlannerAgentWorkspace.vue'
import { resolvePlannerOrderIds } from '@/composables/usePlannerAgent'
import { useMesStore } from '@/stores/mes'

const props = defineProps({
  modelValue: Boolean,
  defaultOrderId: { type: String, default: '' },
  defaultOrderIds: { type: Array, default: () => [] },
  combinedBatch: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'success'])

const mes = useMesStore()
const workspaceRef = ref(null)

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

watch(() => props.modelValue, async (open) => {
  if (open) {
    await nextTick()
    const ids = resolvePlannerOrderIds(props.defaultOrderId, props.defaultOrderIds)
    const validIds = ids.filter((id) => mes.pendingPlanOrders.some((o) => o.id === id))
    workspaceRef.value?.initOrders(validIds.length ? validIds : [])
  }
})

function onClosed() {
  workspaceRef.value?.resetState()
}

function onSuccessDone() {
  visible.value = false
  emit('success')
}
</script>

<style scoped>
.planner-dialog :deep(.el-dialog__body) {
  padding: 16px 20px 12px;
}

.planner-dialog :deep(.planner-workspace__footer) {
  padding: 0;
  border: none;
  background: transparent;
  margin-top: 8px;
}
</style>
