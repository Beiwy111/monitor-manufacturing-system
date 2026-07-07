<template>
  <div class="mes-log-panel">
    <div class="mes-log-header">{{ title }}（最近 {{ logs.length }} 条）</div>
    <div v-for="log in logs" :key="log.id" class="mes-log-item">
      <span class="mes-log-time">{{ log.createdAt }}</span>
      <span>{{ log.operator }} · {{ log.action }} · {{ log.target }}</span>
    </div>
    <div v-if="!logs.length" class="mes-log-item">暂无操作记录</div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '操作日志' },
  logs: { type: Array, default: () => [] }
})
</script>
