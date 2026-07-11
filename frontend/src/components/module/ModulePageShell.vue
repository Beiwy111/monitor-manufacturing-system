<template>
  <section class="module-page-shell">
    <slot />
  </section>
</template>

<style scoped>
.module-page-shell {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  padding: 12px;
  gap: 10px;
  background: #f5f7fa;
}
</style>
