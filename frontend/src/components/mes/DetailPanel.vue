<template>
  <div class="mes-side">
    <div class="mes-side-header">{{ title }}</div>
    <div class="mes-side-body">
      <template v-if="rows.length">
        <div v-for="row in rows" :key="row.label" class="mes-detail-row">
          <span class="mes-detail-label">{{ row.label }}</span>
          <span class="mes-detail-value">
            <StatusBadge v-if="row.badge" :status="row.value" />
            <template v-else>{{ row.value ?? '-' }}</template>
          </span>
        </div>
      </template>
      <div v-else class="mes-detail-empty">{{ emptyText }}</div>
      <slot />
    </div>
    <div v-if="$slots.actions" class="mes-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
import StatusBadge from './StatusBadge.vue'

defineProps({
  title: { type: String, default: '详情' },
  rows: { type: Array, default: () => [] },
  emptyText: { type: String, default: '请选择一条记录查看详情' }
})
</script>

<style scoped>
.mes-detail-empty {
  font-size: 13px;
  color: #909399;
  padding: 20px 0;
  text-align: center;
}
</style>
