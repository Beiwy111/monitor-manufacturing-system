<template>
  <section id="process" class="process-section">
    <div class="section-inner">
      <h2 class="section-title">业务流程</h2>
      <p class="section-lead">从订单审核到售后追溯，覆盖显示器制造完整价值链。</p>
      <div class="process-bar">
        <div v-for="(step, index) in processSteps" :key="step.key" class="process-node">
          <div class="node-line" v-if="index > 0"></div>
          <div class="node-content">
            <div class="node-num">{{ index + 1 }}</div>
            <div class="node-name">{{ step.name }}</div>
            <div class="node-desc">{{ step.desc }}</div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { processSteps } from '@/mock/homeData'
</script>

<style scoped>
.process-section {
  background: #fff;
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
  margin: 0 0 36px;
  font-size: var(--fs-section-lead);
  font-weight: var(--body-weight);
  line-height: var(--lh-lead);
  color: var(--text-muted);
}
.process-bar {
  display: flex;
  align-items: flex-start;
}
.process-node {
  flex: 1;
  display: flex;
  align-items: flex-start;
  position: relative;
}
.node-line {
  position: absolute;
  top: 14px;
  right: 50%;
  left: -50%;
  height: 2px;
  background: var(--accent-color);
  z-index: 0;
}
.node-content {
  flex: 1;
  text-align: center;
  position: relative;
  z-index: 1;
  padding: 0 4px;
}
.node-num {
  width: 28px;
  height: 28px;
  margin: 0 auto 10px;
  background: var(--accent-color);
  color: #fff;
  font-size: var(--fs-caption);
  font-weight: var(--heading-weight);
  line-height: 28px;
  border-radius: 50%;
}
.node-name {
  font-size: var(--fs-body-sm);
  font-weight: var(--heading-weight);
  color: var(--heading-color);
  margin-bottom: 4px;
}
.node-desc {
  font-size: var(--fs-caption);
  font-weight: var(--body-weight);
  color: var(--text-muted);
  line-height: var(--lh-card-desc);
}
@media (max-width: 960px) {
  .process-bar {
    flex-direction: column;
    gap: 20px;
  }
  .node-line { display: none; }
  .node-content { text-align: left; display: flex; gap: 12px; align-items: flex-start; }
  .node-num { margin: 0; flex-shrink: 0; }
}
</style>
