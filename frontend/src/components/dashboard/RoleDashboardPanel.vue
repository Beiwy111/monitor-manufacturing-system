<template>
  <div class="role-dashboard">
    <div class="dashboard-header">
      <h1>{{ config.title }}</h1>
      <p>{{ config.subtitle }}</p>
    </div>

    <div class="kpi-row">
      <div v-for="(kpi, index) in config.kpis" :key="index" class="kpi-item">
        <div class="kpi-label">{{ kpi.label }}</div>
        <div class="kpi-value" :class="'status-' + kpi.status">{{ kpi.value }}</div>
      </div>
    </div>

    <div class="content-split">
      <section class="panel-block">
        <div class="block-title">待办事项</div>
        <el-table :data="config.todos" border stripe size="small">
          <el-table-column prop="title" label="事项" min-width="200" />
          <el-table-column prop="module" label="模块" width="120" />
          <el-table-column prop="priority" label="优先级" width="80" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <span class="status-tag" :class="statusClass(row.status)">{{ row.status }}</span>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="panel-block">
        <div class="block-title">快捷入口</div>
        <div class="quick-links">
          <router-link
            v-for="link in config.quickLinks"
            :key="link.path"
            :to="link.path"
            class="quick-link"
          >
            {{ link.title }}
          </router-link>
        </div>
        <div class="block-title" style="margin-top: 24px">系统提示</div>
        <p class="hint-text">当前为角色工作台骨架页面，业务数据来自 Mock 配置。后续可对接后端动态菜单与实时看板接口。</p>
      </section>
    </div>
  </div>
</template>

<script setup>
defineProps({
  config: { type: Object, required: true }
})

function statusClass(status) {
  if (status === '进行中') return 'tag-processing'
  if (status === '待处理') return 'tag-warning'
  return 'tag-normal'
}
</script>

<style scoped>
.role-dashboard {
  padding: 0;
}
.dashboard-header h1 {
  margin: 0 0 8px;
  font-size: var(--fs-dashboard-title);
  font-weight: var(--heading-weight);
  line-height: var(--lh-heading);
  color: var(--heading-color);
}
.dashboard-header p {
  margin: 0 0 20px;
  font-size: var(--fs-body-sm);
  font-weight: var(--body-weight);
  line-height: var(--lh-lead);
  color: var(--text-subtle);
}
.kpi-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}
.kpi-item {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 14px 16px;
}
.kpi-label {
  font-size: var(--fs-kpi-label);
  font-weight: var(--nav-weight);
  color: var(--text-placeholder);
  margin-bottom: 6px;
}
.kpi-value {
  font-size: var(--fs-kpi-value);
  font-weight: var(--heading-weight);
  line-height: var(--lh-tight);
  color: var(--heading-color);
}
.kpi-value.status-success { color: #52c41a; }
.kpi-value.status-processing { color: #1677ff; }
.kpi-value.status-warning { color: #faad14; }
.kpi-value.status-danger { color: #ff4d4f; }
.kpi-value.status-normal { color: #1a2332; }
.content-split {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
}
.panel-block {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
}
.block-title {
  font-size: var(--fs-body-sm);
  font-weight: var(--heading-weight);
  color: var(--heading-color);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}
.quick-links {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.quick-link {
  display: block;
  padding: 10px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  color: #1677ff;
  text-decoration: none;
  font-size: var(--fs-body-sm);
  font-weight: var(--body-weight-medium);
}
.quick-link:hover {
  background: #f5f7fa;
}
.hint-text {
  margin: 0;
  font-size: var(--fs-caption);
  font-weight: var(--body-weight);
  color: var(--text-placeholder);
  line-height: var(--lh-lead);
}
.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: var(--fs-tag);
  font-weight: var(--body-weight-medium);
}
.tag-processing { background: #e6f4ff; color: #1677ff; }
.tag-warning { background: #fff7e6; color: #faad14; }
.tag-normal { background: #f5f7fa; color: #606266; }
@media (max-width: 1200px) {
  .kpi-row { grid-template-columns: repeat(3, 1fr); }
  .content-split { grid-template-columns: 1fr; }
}
</style>
