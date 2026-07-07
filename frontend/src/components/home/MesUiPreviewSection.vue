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
            <div class="mes-kpi-row">
              <div v-for="kpi in mesPreviewKpis" :key="kpi.label" class="mes-kpi">
                <div class="mes-kpi-label">{{ kpi.label }}</div>
                <div class="mes-kpi-value" :class="'val-' + kpi.status">{{ kpi.value }}</div>
              </div>
            </div>
            <div class="mes-content-row">
              <div class="mes-table-block">
                <div class="block-head">生产工单</div>
                <table class="mes-table">
                  <thead>
                    <tr>
                      <th>工单号</th>
                      <th>产品型号</th>
                      <th>工序</th>
                      <th>计划</th>
                      <th>完成</th>
                      <th>状态</th>
                      <th>负责人</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="row in mesWorkOrders" :key="row.no">
                      <td>{{ row.no }}</td>
                      <td>{{ row.model }}</td>
                      <td>{{ row.step }}</td>
                      <td>{{ row.plan }}</td>
                      <td>{{ row.done }}</td>
                      <td><span class="status-tag" :class="statusClass(row.status)">{{ row.status }}</span></td>
                      <td>{{ row.owner }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div class="mes-side-col">
                <div class="mes-table-block">
                  <div class="block-head">质检结果</div>
                  <table class="mes-table mes-table-compact">
                    <thead>
                      <tr>
                        <th>检验单号</th>
                        <th>型号</th>
                        <th>合格</th>
                        <th>结果</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="row in mesQcRecords" :key="row.no">
                        <td>{{ row.no }}</td>
                        <td>{{ row.model }}</td>
                        <td>{{ row.pass }}</td>
                        <td><span class="status-tag" :class="row.result === '合格' ? 'status-tag--success' : 'status-tag--danger'">{{ row.result }}</span></td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div class="mes-chart-block">
                  <div class="block-head">工单状态分布</div>
                  <div v-for="item in statusDistribution" :key="item.label" class="bar-row">
                    <span class="bar-label">{{ item.label }}</span>
                    <div class="bar-track">
                      <div class="bar-fill" :style="{ width: item.value + '%', background: item.color }"></div>
                    </div>
                    <span class="bar-value">{{ item.value }}%</span>
                  </div>
                </div>
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
import {
  mesPreviewKpis,
  mesWorkOrders,
  mesQcRecords,
  statusDistribution,
  mesPreviewFeatures
} from '@/mock/homeData'

function statusClass(status) {
  const map = {
    '生产中': 'status-tag--processing',
    '待质检': 'status-tag--warning',
    '已入库': 'status-tag--success',
    '异常': 'status-tag--danger'
  }
  return map[status] || 'status-tag--normal'
}
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
.mes-kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 10px;
}
.mes-kpi {
  background: #fff;
  border: 1px solid #e4e7ed;
  padding: 10px 12px;
  border-radius: 4px;
}
.mes-kpi-label {
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
}
.mes-kpi-value {
  font-size: 18px;
  font-weight: 600;
  color: #1a2332;
}
.mes-kpi-value.val-success { color: #52c41a; }
.mes-kpi-value.val-processing { color: #1677ff; }
.mes-content-row {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 8px;
}
.mes-table-block,
.mes-chart-block {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px;
}
.block-head {
  font-size: 12px;
  font-weight: 600;
  color: #1a2332;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
}
.mes-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 11px;
}
.mes-table th {
  text-align: left;
  padding: 6px 8px;
  background: #fafbfc;
  color: #606266;
  font-weight: 500;
  border-bottom: 1px solid #ebeef5;
}
.mes-table td {
  padding: 6px 8px;
  color: #303133;
  border-bottom: 1px solid #f0f2f5;
}
.mes-side-col {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.bar-row {
  display: grid;
  grid-template-columns: 56px 1fr 36px;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 11px;
}
.bar-label { color: #606266; }
.bar-track {
  height: 8px;
  background: #f0f2f5;
  border-radius: 2px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 2px;
}
.bar-value {
  color: #909399;
  text-align: right;
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
  .mes-kpi-row { grid-template-columns: repeat(2, 1fr); }
  .mes-content-row { grid-template-columns: 1fr; }
}
</style>
