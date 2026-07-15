<template>
  <section id="highlights" class="home-section home-section--light">
    <div class="home-section-inner highlights-layout">
      <div class="highlights-visual">
        <img :src="homeImages.highlightScene" alt="显示器制造检测场景" />
        <div class="visual-label">显示器装配 · 检测 · 老化测试</div>
      </div>
      <div class="highlights-content">
        <h2 class="home-section-title">项目亮点</h2>
        <p class="home-section-subtitle">专为 {{ APP_TITLE }} 设计，打通计划、执行、质量、仓储与追溯关键环节。</p>
        <div class="highlight-list">
          <div v-for="item in projectHighlights" :key="item.no" class="highlight-item">
            <div class="highlight-no">{{ item.no }}</div>
            <div class="highlight-body">
              <div class="highlight-title">{{ item.title }}</div>
              <div class="highlight-desc">{{ item.desc }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { homeImages } from '@/config/homeImages'
import { projectHighlights } from '@/mock/homeData'
import { APP_TITLE } from '@/constants/brand'
</script>

<style scoped>
.highlights-layout {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 48px;
  align-items: start;
}
.highlights-visual {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}
.highlights-visual img {
  width: 100%;
  height: 360px;
  object-fit: cover;
  display: block;
}
.visual-label {
  padding: 12px 16px;
  font-size: 13px;
  color: #606266;
  border-top: 1px solid #e4e7ed;
  background: #fff;
}
.highlight-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.highlight-item {
  display: flex;
  gap: 16px;
  padding: 20px 0;
  border-bottom: 1px solid #e4e7ed;
}
.highlight-item:last-child {
  border-bottom: none;
}
.highlight-no {
  flex-shrink: 0;
  width: 36px;
  font-size: 14px;
  font-weight: 600;
  color: #008793;
  padding-top: 2px;
}
.highlight-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a2332;
  margin-bottom: 6px;
}
.highlight-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.75;
}
@media (max-width: 960px) {
  .highlights-layout { grid-template-columns: 1fr; }
}
</style>
