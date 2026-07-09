<template>
  <div v-if="visible" class="ruoyi-detail">
    <div class="ruoyi-detail__head">
      <span class="ruoyi-detail__title">{{ title }}</span>
    </div>
    <div v-if="rows.length" class="ruoyi-detail__body">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item v-for="row in rows" :key="row.label" :label="row.label">
          <StatusBadge v-if="row.badge" :status="row.value" />
          <template v-else>{{ row.value ?? '-' }}</template>
        </el-descriptions-item>
      </el-descriptions>
      <slot />
    </div>
    <div v-else class="ruoyi-detail__empty">{{ emptyText }}</div>
    <div v-if="$slots.actions" class="ruoyi-detail__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import StatusBadge from './StatusBadge.vue'

const props = defineProps({
  title: { type: String, default: '详情' },
  rows: { type: Array, default: () => [] },
  emptyText: { type: String, default: '请选择一条记录查看详情' },
  alwaysShow: { type: Boolean, default: false }
})

const visible = computed(() => props.alwaysShow || props.rows.length > 0)
</script>
