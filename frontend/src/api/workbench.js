import request from '@/utils/request'

/** 角色工作台大屏（数据库实时聚合） */
export function fetchRoleWorkbenchDashboard(roleKey, params = {}) {
  return request.get('/workbench/dashboard', {
    params: { role: roleKey, ...params },
    timeout: 60000
  })
}
