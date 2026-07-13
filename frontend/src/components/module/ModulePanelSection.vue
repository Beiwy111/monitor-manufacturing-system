<template>
  <section class="module-panel-section">
    <div v-if="title || $slots.title" class="module-panel-section__title">
      <slot name="title">{{ title }}</slot>
    </div>
    <slot />
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' }
})
</script>

<style scoped>
.module-panel-section {
  padding: 14px 16px;
  border-bottom: 1px solid #f0f2f5;
}

.module-panel-section:last-child {
  border-bottom: none;
}

.module-panel-section__title {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #2c3e50;
}
</style>
