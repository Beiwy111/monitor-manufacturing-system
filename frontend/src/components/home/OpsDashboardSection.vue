<template>
  <section id="dashboard" class="home-section home-section--light ops-dashboard-section">
    <div class="home-section-inner">
      <h2 class="home-section-title">制造运营看板</h2>
      <p class="home-section-subtitle">
        登录后按角色查看实时订单、计划、工单、质检与库存数据；首页仅展示系统能力，不展示演示数字。
      </p>

      <div class="ops-capability-grid">
        <div v-for="item in capabilities" :key="item.title" class="ops-capability">
          <div class="ops-capability-title">{{ item.title }}</div>
          <div class="ops-capability-desc">{{ item.desc }}</div>
        </div>
      </div>

      <div class="ops-login-hint">
        <p>各岗位工作台 KPI、待办与产线进度均来自数据库实时快照（<code>/mes/snapshot</code>）。</p>
        <p>计划员、生产主管可在智能生成排产后继续修改数量、日期与优先级，再发布或提交。</p>
      </div>
    </div>
  </section>
</template>

<script setup>
const capabilities = [
  { title: '订单驱动', desc: '客户订单审核通过后进入计划编制，全流程状态可追溯。' },
  { title: '智能排产', desc: '实时展示订单→库存→物料→设备→人员的规划路径，输出可编辑的专业生产计划。' },
  { title: '现场执行', desc: '派工、报工、质检与仓储出入库数据实时回写工单。' },
  { title: '质量追溯', desc: '检验记录、不合格品与批次信息关联，支持反向追溯。' },
  { title: '设备安灯', desc: '设备状态与报警联动，异常推送至主管工作台。' },
  { title: '多角色协同', desc: '销售、计划、主管、操作员、质检、仓管按权限分工协作。' }
]
</script>

<style scoped>
.ops-capability-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}
.ops-capability {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
}
.ops-capability-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a2332;
  margin-bottom: 6px;
}
.ops-capability-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}
.ops-login-hint {
  background: #f0f9ff;
  border: 1px solid #d6e8ff;
  border-radius: 6px;
  padding: 16px 20px;
  font-size: 13px;
  color: #4f5f73;
  line-height: 1.8;
}
.ops-login-hint p { margin: 0 0 6px; }
.ops-login-hint p:last-child { margin-bottom: 0; }
.ops-login-hint code {
  font-size: 12px;
  background: #e8f4ff;
  padding: 1px 6px;
  border-radius: 3px;
}
@media (max-width: 960px) {
  .ops-capability-grid { grid-template-columns: 1fr; }
}
</style>
