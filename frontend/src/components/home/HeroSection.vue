<template>
  <section id="hero" class="hero-section">
    <div class="hero-bg" :style="{ backgroundImage: `url(${homeImages.heroBg})` }"></div>
    <div class="hero-overlay"></div>
    <div class="hero-inner">
      <div class="hero-breadcrumb">首页 &gt; 产品 &gt; 制造执行系统</div>
      <div class="hero-kicker">MANUFACTURING EXECUTION SYSTEM</div>
      <h1 class="hero-title">电脑显示器制造 MES 系统</h1>
      <p class="hero-desc">
        覆盖客户订单、生产计划、工单派工、现场报工、质量检验、仓储出入库、
        设备维护与售后追溯的全流程制造执行平台。
      </p>
      <a class="hero-cta" @click="$emit('enter')">进入系统</a>
    </div>
  </section>
</template>

<script setup>
import { homeImages } from '@/config/homeImages'

defineEmits(['enter'])
</script>

<style scoped>
.hero-section {
  position: relative;
  min-height: 440px;
  margin-top: var(--nav-height);
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}
.hero-bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
}
.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    rgba(0, 27, 63, 0.82) 0%,
    rgba(0, 27, 63, 0.55) 55%,
    rgba(0, 27, 63, 0.35) 100%
  );
}
.hero-inner {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  padding: 64px 32px 72px;
}
.hero-breadcrumb {
  font-size: var(--fs-hero-breadcrumb);
  font-weight: var(--body-weight);
  color: rgba(255, 255, 255, 0.55);
  margin-bottom: 20px;
}
.hero-kicker {
  font-size: var(--fs-hero-label);
  font-weight: var(--label-weight);
  letter-spacing: var(--ls-hero-label);
  color: var(--accent-bright);
  margin-bottom: 14px;
  text-transform: uppercase;
}
.hero-title {
  margin: 0 0 18px;
  font-size: var(--fs-hero-title);
  font-weight: var(--heading-weight);
  color: #fff;
  line-height: var(--lh-hero);
  letter-spacing: var(--ls-hero-title);
  max-width: 640px;
}
.hero-desc {
  margin: 0 0 32px;
  font-size: var(--fs-hero-desc);
  font-weight: var(--body-weight);
  color: rgba(255, 255, 255, 0.82);
  line-height: var(--lh-lead);
  max-width: 580px;
}
.hero-cta {
  display: inline-block;
  padding: 13px 36px;
  background: var(--accent-bright);
  color: #001b3f;
  font-size: var(--fs-btn);
  font-weight: var(--btn-weight);
  font-family: var(--font-sans);
  cursor: pointer;
  border-radius: 0;
  text-decoration: none;
  transition: background 0.15s;
}
.hero-cta:hover {
  background: var(--accent-hover);
}
@media (max-width: 768px) {
  .hero-section { min-height: 380px; }
  .hero-inner { padding: 48px 20px 56px; }
}
</style>
