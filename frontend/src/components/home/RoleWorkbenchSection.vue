<template>
  <section id="roles" class="role-section">
    <div class="section-inner">
      <h2 class="section-title">角色工作台</h2>
      <p class="section-lead">
        不同岗位登录后进入专属工作台，看到不同菜单与待办任务。演示账号密码均为 123456。
      </p>
      <table class="role-table">
        <thead>
          <tr>
            <th>岗位角色</th>
            <th>主要职责</th>
            <th>演示账号</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="role in roleWorkbenches" :key="role.key">
            <td class="role-name">{{ role.name }}</td>
            <td>{{ role.desc }}</td>
            <td class="role-account">{{ role.account }}</td>
            <td><a class="role-link" @click="$emit('login-as', role.account)">进入</a></td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { roleWorkbenches } from '@/mock/homeData'

defineEmits(['login-as'])
</script>

<style scoped>
.role-section {
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
  margin: 0 0 32px;
  font-size: var(--fs-section-lead);
  font-weight: var(--body-weight);
  line-height: var(--lh-lead);
  color: var(--text-muted);
}
.role-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--fs-body-sm);
}
.role-table th {
  text-align: left;
  padding: 12px 16px;
  background: #f4f6f8;
  color: var(--heading-color);
  font-weight: var(--table-head-weight);
  font-size: var(--fs-table-head);
  border-bottom: 1px solid #e8ecf0;
}
.role-table td {
  padding: 14px 16px;
  border-bottom: 1px solid #e8ecf0;
  color: var(--text-muted);
  font-weight: var(--body-weight);
  vertical-align: top;
}
.role-name {
  font-weight: var(--heading-weight);
  color: var(--heading-color);
  white-space: nowrap;
}
.role-account {
  font-family: var(--font-sans);
  color: var(--text-placeholder);
}
.role-link {
  color: var(--accent-color);
  font-weight: var(--body-weight-medium);
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 2px;
}
.role-link:hover {
  color: var(--accent-hover);
}
@media (max-width: 768px) {
  .role-table thead { display: none; }
  .role-table td { display: block; padding: 8px 0; border: none; }
  .role-table tr { display: block; padding: 16px 0; border-bottom: 1px solid #e8ecf0; }
}
</style>
