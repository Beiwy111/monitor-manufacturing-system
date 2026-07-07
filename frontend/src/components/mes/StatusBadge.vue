<template>
  <span class="status-badge" :class="`status-badge--${type}`">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { STATUS_COLOR } from '@/mock/constants'

const props = defineProps({
  status: { type: String, default: '' }
})

const type = computed(() => STATUS_COLOR[props.status] || 'info')
const label = computed(() => props.status || '-')
</script>

<style scoped>
.status-badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.4;
  border-radius: 2px;
}
.status-badge--success { background: #e8f5e9; color: #2e7d32; }
.status-badge--primary { background: #e3f2fd; color: #1565c0; }
.status-badge--warning { background: #fff8e1; color: #e65100; }
.status-badge--danger { background: #ffebee; color: #c62828; }
.status-badge--info { background: #f5f5f5; color: #616161; }
</style>
