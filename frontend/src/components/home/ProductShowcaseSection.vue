<template>
  <section id="products" class="product-section">
    <div class="section-inner">
      <h2 class="section-title">探索电脑显示器制造 MES 核心能力</h2>
      <p class="section-lead">
        面向 LCD / OLED 显示器制造场景，覆盖订单、计划、生产、质量、仓储、设备与售后追溯，
        构建从客户需求到产品交付的全流程数字化制造能力。
      </p>
      <div class="product-grid">
        <article
          v-for="item in productShowcase"
          :key="item.id"
          class="product-card"
        >
          <div class="product-image">
            <img :src="item.image" :alt="item.title" loading="lazy" />
          </div>
          <div class="product-body">
            <div class="product-label">{{ item.label }}</div>
            <h3 class="product-title">{{ item.title }}</h3>
            <p class="product-desc">{{ item.desc }}</p>
            <a class="product-link" @click="scrollTo(item.anchor)">了解更多 →</a>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup>
import { productShowcase } from '@/config/homeImages'

function scrollTo(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
}
</script>

<style scoped>
.product-section {
  background: #fff;
  padding: 72px 24px;
  border-bottom: 1px solid #e8ecf0;
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
  margin: 0 0 48px;
  font-size: var(--fs-section-lead);
  font-weight: var(--body-weight);
  line-height: var(--lh-lead);
  color: var(--text-muted);
  max-width: 860px;
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 28px 24px;
}
.product-card {
  display: flex;
  flex-direction: column;
  border: 1px solid #e8ecf0;
  background: #fff;
  height: 100%;
  transition: border-color 0.2s;
}
.product-card:hover {
  border-color: var(--accent-color);
}
.product-card:hover .product-image img {
  opacity: 0.92;
}
.product-image {
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #f0f2f5;
  flex-shrink: 0;
}
.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: opacity 0.2s;
}
.product-body {
  padding: 20px 20px 24px;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.product-label {
  font-size: var(--fs-card-label);
  letter-spacing: var(--ls-card-label);
  color: var(--accent-color);
  font-weight: var(--label-weight);
  margin-bottom: 8px;
}
.product-title {
  margin: 0 0 10px;
  font-size: var(--fs-card-title);
  font-weight: var(--heading-weight);
  color: var(--heading-color);
  line-height: var(--lh-card-title);
}
.product-desc {
  margin: 0 0 16px;
  font-size: var(--fs-card-desc);
  font-weight: var(--body-weight);
  color: var(--text-muted);
  line-height: var(--lh-card-desc);
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.product-link {
  font-size: var(--fs-card-link);
  font-weight: var(--body-weight-medium);
  color: var(--accent-color);
  cursor: pointer;
  text-decoration: none;
  margin-top: auto;
}
.product-link:hover {
  color: var(--accent-hover);
  text-decoration: underline;
  text-underline-offset: 2px;
}

/* 平板：2 列 */
@media (max-width: 1024px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* 手机：1 列 */
@media (max-width: 640px) {
  .product-section {
    padding: 48px 16px;
  }
  .product-grid {
    grid-template-columns: 1fr;
    gap: 24px;
  }
  .product-desc {
    -webkit-line-clamp: unset;
    overflow: visible;
  }
}
</style>
