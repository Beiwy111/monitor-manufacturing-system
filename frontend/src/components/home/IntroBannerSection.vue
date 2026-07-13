<template>
  <section class="intro-banner">
    <div class="banner-inner">
      <p class="banner-text">
        面向显示器制造企业的制造执行系统，覆盖订单、计划、派工、报工、质检、仓储、设备、发货与售后追溯全流程。
      </p>
      <a class="banner-link" @click="$emit('process')">了解业务流程</a>
    </div>
  </section>
</template>

<script setup>
defineEmits(['process'])
</script>

<style scoped>
.intro-banner {
  background: #f4f6f8;
  border-bottom: 1px solid #e8ecf0;
  padding: 20px 24px;
}
.banner-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}
.banner-text {
  margin: 0;
  font-size: var(--fs-body-sm);
  font-weight: var(--body-weight);
  color: var(--text-muted);
  line-height: var(--lh-lead);
  flex: 1;
}
.banner-link {
  flex-shrink: 0;
  font-size: var(--fs-body-sm);
  font-weight: var(--body-weight-medium);
  color: var(--heading-color);
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}
.banner-link:hover {
  color: var(--accent-color);
}
@media (max-width: 640px) {
  .banner-inner { flex-direction: column; align-items: flex-start; }
}
</style>
