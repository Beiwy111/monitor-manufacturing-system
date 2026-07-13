/** 后端 roleCode → 前端 roleKey */
export function roleCodeToKey(roleCode) {
  const map = {
    ADMIN: 'admin',
    ORDER: 'order',
    PLANNER: 'planner',
    MANAGER: 'manager',
    OPERATOR: 'operator',
    QC: 'quality',
    PURCHASER: 'purchase',
    WAREHOUSE: 'warehouse',
    DEVICE: 'device',
    SERVICE: 'aftersale',
    COST: 'cost'
  }
  return map[(roleCode || '').toUpperCase()] || (roleCode || '').toLowerCase()
}

export function mapUserFromApi(user, roles = []) {
  const role = roles.find((r) => r.roleId === user.roleId)
  return {
    id: user.userId,
    username: user.username,
    realName: user.realName,
    roleId: user.roleId,
    roleKey: role ? roleCodeToKey(role.roleCode) : '',
    roleName: role ? role.roleName : '待分配',
    pendingRole: !user.roleId,
    phone: user.phone || '',
    department: user.department || '',
    email: user.email || '',
    status: user.status === 1 ? '启用' : '禁用',
    createdAt: formatDateTime(user.createdAt)
  }
}

export function mapRoleFromApi(role, users = []) {
  const roleKey = roleCodeToKey(role.roleCode)
  return {
    id: role.roleId,
    roleKey,
    roleName: role.roleName,
    permCount: 10,
    userCount: users.filter((u) => u.roleId === role.roleId).length,
    status: role.status === 1 ? '启用' : '禁用',
    remark: role.roleDescription || '',
    sort: 0
  }
}

export function mapOperationLogFromApi(log, users = []) {
  const user = users.find((u) => u.userId === log.userId)
  return {
    id: log.logId,
    module: log.moduleName || '',
    action: log.operationType || '',
    target: log.operationContent || '',
    operator: user?.realName || user?.username || '系统',
    roleKey: 'system',
    createdAt: formatDateTime(log.operatedAt)
  }
}

function formatDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 19)
}
