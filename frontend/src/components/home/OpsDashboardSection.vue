<template>
  <section id="dashboard" class="home-section home-section--light ops-dashboard-section">
    <div class="home-section-inner">
      <h2 class="home-section-title">制造运营看板预览</h2>
      <p class="home-section-subtitle">生产、质量、设备、库存关键指标与今日待办一览，支持主管快速决策（演示数据）。</p>

      <div class="ops-kpi-row">
        <div v-for="kpi in opsKpis" :key="kpi.label" class="ops-kpi">
          <div class="ops-kpi-label">{{ kpi.label }}</div>
          <div class="ops-kpi-value" :class="'ops-' + kpi.status">{{ kpi.value }}</div>
          <div class="ops-kpi-sub">{{ kpi.sub }}</div>
        </div>
      </div>

      <div class="ops-content">
        <div class="ops-panel">
          <div class="panel-title">今日待办</div>
          <table class="ops-table">
            <thead>
              <tr>
                <th>事项</th>
                <th>责任角色</th>
                <th>优先级</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="todo in opsTodos" :key="todo.id">
                <td>{{ todo.title }}</td>
                <td>{{ todo.role }}</td>
                <td>{{ todo.priority }}</td>
                <td><span class="status-tag" :class="todoStatusClass(todo.status)">{{ todo.status }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="ops-panel">
          <div class="panel-title">产线状态概览</div>
          <div class="line-status-list">
            <div class="line-item">
              <span class="line-name">LCD 组装线 1</span>
              <span class="status-tag status-tag--processing">运行中</span>
              <div class="line-bar"><div class="line-fill" style="width:78%;background:#1677ff"></div></div>
              <span class="line-pct">78%</span>
            </div>
            <div class="line-item">
              <span class="line-name">背光贴合线 2</span>
              <span class="status-tag status-tag--warning">待料</span>
              <div class="line-bar"><div class="line-fill" style="width:45%;background:#faad14"></div></div>
              <span class="line-pct">45%</span>
            </div>
            <div class="line-item">
              <span class="line-name">老化测试线 3</span>
              <span class="status-tag status-tag--success">正常</span>
              <div class="line-bar"><div class="line-fill" style="width:92%;background:#52c41a"></div></div>
              <span class="line-pct">92%</span>
            </div>
            <div class="line-item">
              <span class="line-name">终检包装线 4</span>
              <span class="status-tag status-tag--danger">异常</span>
              <div class="line-bar"><div class="line-fill" style="width:30%;background:#ff4d4f"></div></div>
              <span class="line-pct">30%</span>
            </div>
          </div>
          <div class="panel-note">各产线负荷率为当日计划完成进度，异常产线已触发安灯通知。</div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { opsKpis, opsTodos } from '@/mock/homeData'

function todoStatusClass(status) {
  const map = {
    '待处理': 'status-tag--warning',
    '进行中': 'status-tag--processing',
    '预警': 'status-tag--warning',
    '异常': 'status-tag--danger'
  }
  return map[status] || 'status-tag--normal'
}
</script>

<style scoped>
.ops-kpi-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}
.ops-kpi {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
}
.ops-kpi-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.ops-kpi-value {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 4px;
}
.ops-kpi-value.ops-success { color: #52c41a; }
.ops-kpi-value.ops-processing { color: #1677ff; }
.ops-kpi-value.ops-warning { color: #faad14; }
.ops-kpi-value.ops-danger { color: #ff4d4f; }
.ops-kpi-sub {
  font-size: 11px;
  color: #909399;
}
.ops-content {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 16px;
}
.ops-panel {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
}
.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a2332;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}
.ops-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.ops-table th {
  text-align: left;
  padding: 8px 10px;
  background: #fafbfc;
  color: #606266;
  font-weight: 500;
  border-bottom: 1px solid #ebeef5;
}
.ops-table td {
  padding: 10px;
  border-bottom: 1px solid #f0f2f5;
  color: #303133;
}
.line-status-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.line-item {
  display: grid;
  grid-template-columns: 120px 60px 1fr 40px;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}
.line-name {
  color: #303133;
}
.line-bar {
  height: 8px;
  background: #f0f2f5;
  border-radius: 2px;
  overflow: hidden;
}
.line-fill {
  height: 100%;
  border-radius: 2px;
}
.line-pct {
  font-size: 12px;
  color: #909399;
  text-align: right;
}
.panel-note {
  margin-top: 16px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}
@media (max-width: 960px) {
  .ops-kpi-row { grid-template-columns: repeat(2, 1fr); }
  .ops-content { grid-template-columns: 1fr; }
  .line-item { grid-template-columns: 1fr; gap: 6px; }
}
</style>
