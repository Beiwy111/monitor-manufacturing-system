<template>
  <div class="ruoyi-page">
    <div v-if="statusItems.length" class="ruoyi-stats">
      <span
        v-for="item in statusItems"
        :key="item.label"
        class="ruoyi-stats__item"
        :class="{
          'ruoyi-stats__item--warn': item.warn,
          'ruoyi-stats__item--danger': item.danger
        }"
      >
        {{ item.label }}：<em>{{ item.value }}</em>
      </span>
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

    <div class="ruoyi-table-wrap">
      <slot name="table" />
    </div>

    <DetailPanel :title="detailTitle" :rows="detailRows" always-show>
      <slot name="detail-extra" />
      <template v-if="hasDetailActions" #actions>
        <slot name="detail-actions" />
      </template>
    </DetailPanel>
  </div>
</template>

<script setup>
import { ref, watch, useSlots, computed, provide } from 'vue'
import SearchToolbar from './SearchToolbar.vue'
import DetailPanel from './DetailPanel.vue'

const props = defineProps({
  statusItems: { type: Array, default: () => [] },
  toolbarTitle: { type: String, default: '' },
  showSearch: { type: Boolean, default: true },
  searchPlaceholder: { type: String, default: '请输入关键字' },
  statusOptions: { type: Array, default: () => [] },
  toolbarActions: { type: Array, default: () => [] },
  detailTitle: { type: String, default: '详情' },
  detailRows: { type: Array, default: () => [] },
  modelKeyword: { type: String, default: '' },
  modelStatus: { type: String, default: '' }
})

const emit = defineEmits(['filter-change', 'toolbar-action', 'update:modelKeyword', 'update:modelStatus'])

const slots = useSlots()
const hasDetailActions = computed(() => Boolean(slots['detail-actions']))

const keyword = ref(props.modelKeyword)
const statusFilter = ref(props.modelStatus)

provide('mesPageFilters', { keyword, statusFilter })

watch(() => props.modelKeyword, (v) => { keyword.value = v })
watch(() => props.modelStatus, (v) => { statusFilter.value = v })

function onFilterChange(e) {
  emit('update:modelKeyword', e.keyword)
  emit('update:modelStatus', e.status)
  emit('filter-change', e)
}
</script>
