import request from '@/utils/request'

/** 角色工作台大屏（数据库实时聚合） */
export function fetchRoleWorkbenchDashboard(roleKey, params = {}) {
  return request.get('/workbench/dashboard', {
    params: { role: roleKey, ...params },
    timeout: 60000
  })
}

/** 财务角色：基于当前工作台周期生成 DeepSeek AI 财务分析 */
export function generateFinanceAiAnalysis(days = 7) {
  return request.post('/workbench/dashboard/cost/ai-analysis', null, {
    params: { days },
    timeout: 90000,
    silent: true
  })
}

/** 管理员角色：基于 MES 跨模块汇总数据生成 DeepSeek AI 全局分析 */
export function generateGlobalAiAnalysis(days = 7) {
  return request.post('/workbench/dashboard/admin/ai-analysis', null, {
    params: { days },
    timeout: 120000,
    silent: true
  })
}

/** 管理员：将一条 AI 全局分析行动建议通知给对应部门。 */
export function notifyGlobalAiAction(data) {
  return request.post('/workbench/dashboard/admin/ai-analysis/notify', data, {
    timeout: 30000,
    silent: true
  })
}

/** 当前角色的持久化业务消息。 */
export function fetchSystemNotifications() {
  return request.get('/workbench/dashboard/notifications', {
    timeout: 30000,
    silent: true
  })
}
