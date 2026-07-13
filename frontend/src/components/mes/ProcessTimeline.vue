<template>
  <div class="mes-timeline">
    <div v-for="(item, i) in items" :key="i" class="mes-timeline-item">
      <span class="mes-timeline-dot" />
      <div class="mes-timeline-title">{{ item.title }}</div>
      <div v-if="item.desc" class="mes-timeline-desc">{{ item.desc }}</div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  items: { type: Array, default: () => [] }
})
</script>
