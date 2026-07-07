<template>
  <section id="modules" class="module-section">
    <div class="section-inner">
      <h2 class="section-title">功能模块</h2>
      <p class="section-lead">按制造业务域划分，支持多角色协同与权限隔离。</p>
      <ul class="module-list">
        <li v-for="mod in coreModules" :key="mod.name">
          <span class="module-name">{{ mod.name }}</span>
          <span class="module-desc">{{ mod.desc }}</span>
        </li>
      </ul>
    </div>
  </section>
</template>

<script setup>
import { coreModules } from '@/mock/homeData'
</script>

<style scoped>
.module-section {
  background: #f4f6f8;
  padding: 64px 24px;
  border-top: 1px solid #e8ecf0;
}
.section-inner {
  max-width: 1200px;
  margin: 0 auto;
}
.section-title {
  margin: 0 0 14px;
  font-size: var(--fs-section-title);
  font-weight: var(--heading-weight);
  line-height: var(--lh-heading);
  letter-spacing: var(--ls-section-title);
  color: var(--heading-color);
}
.section-lead {
  margin: 0 0 32px;
  font-size: var(--fs-section-lead);
  font-weight: var(--body-weight);
  line-height: var(--lh-lead);
  color: var(--text-muted);
}
.module-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0;
  border-top: 1px solid #e8ecf0;
}
.module-list li {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #e8ecf0;
  align-items: start;
}
.module-name {
  font-size: var(--fs-body-sm);
  font-weight: var(--heading-weight);
  color: var(--heading-color);
}
.module-desc {
  font-size: var(--fs-caption);
  font-weight: var(--body-weight);
  color: var(--text-muted);
  line-height: var(--lh-card-desc);
}
@media (max-width: 640px) {
  .module-list { grid-template-columns: 1fr; }
  .module-list li { grid-template-columns: 1fr; gap: 4px; }
}
</style>
