<template>
  <div v-if="logs.length" class="ruoyi-log">
    <div class="ruoyi-log__head">{{ title }}（最近 {{ logs.length }} 条）</div>
    <div class="ruoyi-log__list">
      <div v-for="log in logs" :key="log.id" class="ruoyi-log__item">
        <span class="ruoyi-log__time">{{ log.createdAt }}</span>
        <span>{{ log.operator }} · {{ log.action }} · {{ log.target }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '操作日志' },
  logs: { type: Array, default: () => [] }
})
</script>
