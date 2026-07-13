<template>
  <section id="preview" class="home-section home-section--white ui-preview-section">
    <div class="home-section-inner preview-layout">
      <div class="preview-screen">
        <div class="screen-frame">
          <div class="screen-header">
            <span class="screen-dot"></span>
            <span class="screen-dot"></span>
            <span class="screen-dot"></span>
            <span class="screen-title">电脑显示器制造 MES · 生产监控</span>
          </div>
          <div class="screen-body">
            <div class="mes-empty-preview">
              <div class="empty-kpi-row">
                <div v-for="label in kpiLabels" :key="label" class="empty-kpi">
                  <div class="empty-kpi-label">{{ label }}</div>
                  <div class="empty-kpi-value">—</div>
                </div>
              </div>
              <div class="empty-table-block">
                <div class="block-head">生产工单 / 质检 / 库存</div>
                <p class="empty-hint">登录系统后展示来自数据库的实时业务记录，此处不预置演示数据。</p>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="preview-desc">
        <h2 class="home-section-title">系统界面预览</h2>
        <p class="desc-lead">面向显示器制造现场的生产执行监控界面，主管、操作员、质检员在同一平台协同作业。</p>
        <div class="feature-list">
          <div v-for="item in mesPreviewFeatures" :key="item.title" class="feature-item">
            <div class="feature-title">{{ item.title }}</div>
            <div class="feature-desc">{{ item.desc }}</div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { mesPreviewFeatures } from '@/mock/homeData'

const kpiLabels = ['计划达成率', '工单完成', '质检合格率', '设备在线率']
</script>

<style scoped>
.ui-preview-section {
  border-bottom: 1px solid #e4e7ed;
}
.preview-layout {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 48px;
  align-items: start;
}
.screen-frame {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}
.screen-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}
.screen-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #dcdfe6;
}
.screen-title {
  margin-left: 8px;
  font-size: 12px;
  color: #606266;
}
.screen-body {
  padding: 12px;
  background: #eef1f5;
}
.empty-kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 10px;
}
.empty-kpi {
  background: #fff;
  border: 1px dashed #dcdfe6;
  padding: 10px 12px;
  border-radius: 4px;
}
.empty-kpi-label {
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
}
.empty-kpi-value {
  font-size: 18px;
  font-weight: 600;
  color: #c0c4cc;
}
.empty-table-block {
  background: #fff;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  padding: 24px 16px;
  text-align: center;
}
.block-head {
  font-size: 12px;
  font-weight: 600;
  color: #1a2332;
  margin-bottom: 12px;
}
.empty-hint {
  margin: 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.7;
}
.preview-desc .home-section-title {
  margin-bottom: 12px;
}
.desc-lead {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  margin: 0 0 28px;
}
.feature-item {
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}
.feature-item:last-child {
  border-bottom: none;
}
.feature-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a2332;
  margin-bottom: 6px;
}
.feature-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}
@media (max-width: 960px) {
  .preview-layout { grid-template-columns: 1fr; }
  .empty-kpi-row { grid-template-columns: repeat(2, 1fr); }
}
</style>
