<template>
  <section id="hero" class="hero-section">
    <video
      class="hero-bg"
      :src="homeImages.heroVideo"
      :poster="homeImages.heroBg"
      autoplay
      loop
      muted
      playsinline
      disablePictureInPicture
    />
    <div class="hero-overlay"></div>
    <div class="hero-inner">
      <div class="hero-breadcrumb">首页 &gt; 产品 &gt; 制造执行系统</div>
      <div class="hero-kicker">MANUFACTURING EXECUTION SYSTEM</div>
      <h1 class="hero-display-title">
        <span class="hero-display-title__brand">{{ BRAND_NAME }}</span><span class="hero-display-title__mes">MES</span><span class="hero-display-title__suffix">—{{ APP_SUBTITLE }}</span>
      </h1>
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
import { APP_SUBTITLE, BRAND_NAME } from '@/constants/brand'

defineEmits(['enter'])
</script>

<style scoped>
.hero-section {
  position: relative;
  min-height: 400px;
  margin-top: var(--nav-height);
  display: flex;
  align-items: flex-end;
  overflow: hidden;
  background: #001b3f;
}
.hero-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 82% 28%;
  pointer-events: none;
}
.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    rgba(0, 27, 63, 0.94) 0%,
    rgba(0, 27, 63, 0.82) 38%,
    rgba(0, 27, 63, 0.48) 58%,
    rgba(0, 27, 63, 0.16) 78%,
    transparent 100%
  );
}
.hero-inner {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  padding: 48px 32px 52px;
}
.hero-breadcrumb {
  font-size: var(--fs-hero-breadcrumb);
  font-weight: var(--body-weight);
  color: rgba(255, 255, 255, 0.72);
  margin-bottom: 20px;
}
.hero-kicker {
  font-size: var(--fs-hero-label);
  font-weight: var(--label-weight);
  letter-spacing: 0.08em;
  color: rgba(255, 255, 255, 0.78);
  margin-bottom: 14px;
  text-transform: uppercase;
}
.hero-display-title {
  margin: 0 0 18px;
  max-width: 820px;
  font-family: var(--font-display);
  font-weight: 300;
  font-size: clamp(32px, 4.8vw, 52px);
  line-height: 1.28;
  letter-spacing: 0.02em;
}
.hero-display-title__brand {
  color: #fff;
  font-weight: 400;
}
.hero-display-title__mes {
  font-style: italic;
  font-weight: 500;
  color: #7dd3fc;
  letter-spacing: 0.04em;
}
.hero-display-title__suffix {
  color: rgba(255, 255, 255, 0.92);
  font-weight: 300;
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
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.55);
  font-size: var(--fs-btn);
  font-weight: var(--btn-weight);
  font-family: var(--font-sans);
  cursor: pointer;
  border-radius: 0;
  text-decoration: none;
  transition: background 0.15s, border-color 0.15s;
}
.hero-cta:hover {
  background: rgba(255, 255, 255, 0.24);
  border-color: rgba(255, 255, 255, 0.85);
}
@media (max-width: 768px) {
  .hero-section { min-height: 340px; }
  .hero-bg { object-position: 78% 22%; }
  .hero-inner { padding: 36px 20px 44px; }
  .hero-display-title {
    line-height: 1.35;
  }
}
</style>
