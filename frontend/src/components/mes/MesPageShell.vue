<template>
  <div class="mes-page">
    <div v-if="statusItems.length" class="mes-status-strip">
      <div v-for="item in statusItems" :key="item.label" class="mes-status-item">
        <div class="mes-status-label">{{ item.label }}</div>
        <div class="mes-status-value" :class="item.warn ? 'mes-status-value--warn' : item.danger ? 'mes-status-value--danger' : ''">
          {{ item.value }}
        </div>
      </div>
    </div>
    <SearchToolbar
      :title="toolbarTitle"
      :show-search="showSearch"
      :search-placeholder="searchPlaceholder"
      :status-options="statusOptions"
      :actions="toolbarActions"
      v-model:model-keyword="keyword"
      v-model:model-status="statusFilter"
      @change="onFilterChange"
      @action="$emit('toolbar-action', $event)"
    >
      <template #extra><slot name="toolbar-extra" /></template>
    </SearchToolbar>
    <div class="mes-body">
      <div class="mes-main">
        <div class="mes-table-wrap">
          <slot name="table" />
        </div>
        <OperationLogPanel v-if="showLog" :logs="logs" />
      </div>
      <DetailPanel :title="detailTitle" :rows="detailRows">
        <slot name="detail-extra" />
        <template v-if="hasDetailActions" #actions>
          <slot name="detail-actions" />
        </template>
      </DetailPanel>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, useSlots, computed } from 'vue'
import SearchToolbar from './SearchToolbar.vue'
import DetailPanel from './DetailPanel.vue'
import OperationLogPanel from './OperationLogPanel.vue'

const props = defineProps({
  statusItems: { type: Array, default: () => [] },
  toolbarTitle: { type: String, default: '' },
  showSearch: { type: Boolean, default: true },
  searchPlaceholder: { type: String, default: '搜索...' },
  statusOptions: { type: Array, default: () => [] },
  toolbarActions: { type: Array, default: () => [] },
  detailTitle: { type: String, default: '详情' },
  detailRows: { type: Array, default: () => [] },
  logs: { type: Array, default: () => [] },
  showLog: { type: Boolean, default: true },
  modelKeyword: { type: String, default: '' },
  modelStatus: { type: String, default: '' }
})

const emit = defineEmits(['filter-change', 'toolbar-action', 'update:modelKeyword', 'update:modelStatus'])

const slots = useSlots()
const hasDetailActions = computed(() => Boolean(slots['detail-actions']))

const keyword = ref(props.modelKeyword)
const statusFilter = ref(props.modelStatus)

watch(() => props.modelKeyword, (v) => { keyword.value = v })
watch(() => props.modelStatus, (v) => { statusFilter.value = v })

function onFilterChange(e) {
  emit('update:modelKeyword', e.keyword)
  emit('update:modelStatus', e.status)
  emit('filter-change', e)
}
</script>
