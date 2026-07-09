<template>
  <section id="roles" class="role-section">
    <div class="section-inner">
      <h2 class="section-title">角色工作台</h2>
      <p class="section-lead">
        不同岗位登录后进入专属工作台，看到不同菜单与待办任务。账号由系统管理员统一创建与分配。
      </p>
      <table class="role-table">
        <thead>
          <tr>
            <th>岗位角色</th>
            <th>主要职责</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="role in roleWorkbenches" :key="role.key">
            <td class="role-name">{{ role.name }}</td>
            <td>{{ role.desc }}</td>
            <td><a class="role-link" @click="$emit('login')">登录系统</a></td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { roleWorkbenches } from '@/mock/homeData'

defineEmits(['login'])
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
  color: var(--heading-color);
  font-weight: var(--body-weight-medium);
  white-space: nowrap;
}
.role-link {
  color: #008793;
  cursor: pointer;
  text-decoration: none;
  font-weight: var(--body-weight-medium);
}
.role-link:hover {
  text-decoration: underline;
}
</style>
