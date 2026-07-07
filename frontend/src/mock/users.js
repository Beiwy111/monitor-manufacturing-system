/**
 * Mock 登录账号
 */
export const mockUsers = [
  { username: 'admin', password: '123456', roleKey: 'admin', roleCode: 'ADMIN', roleName: '系统管理员', realName: '系统管理员', dashboardPath: '/dashboard/admin' },
  { username: 'sales', password: '123456', roleKey: 'order', roleCode: 'ORDER', roleName: '订单管理员', realName: '张销售', dashboardPath: '/dashboard/order' },
  { username: 'manager', password: '123456', roleKey: 'manager', roleCode: 'MANAGER', roleName: '生产主管', realName: '李主管', dashboardPath: '/dashboard/manager' },
  { username: 'operator', password: '123456', roleKey: 'operator', roleCode: 'OPERATOR', roleName: '生产操作员', realName: '王操作', dashboardPath: '/dashboard/operator' },
  { username: 'qc', password: '123456', roleKey: 'quality', roleCode: 'QC', roleName: '质检员', realName: '赵质检', dashboardPath: '/dashboard/quality' },
  { username: 'buyer', password: '123456', roleKey: 'purchase', roleCode: 'PURCHASER', roleName: '采购员', realName: '刘采购', dashboardPath: '/dashboard/purchase' },
  { username: 'warehouse', password: '123456', roleKey: 'warehouse', roleCode: 'WAREHOUSE', roleName: '仓库管理员', realName: '陈仓管', dashboardPath: '/dashboard/warehouse' },
  { username: 'device', password: '123456', roleKey: 'device', roleCode: 'DEVICE', roleName: '设备维护人员', realName: '周设备', dashboardPath: '/dashboard/device' },
  { username: 'aftersale', password: '123456', roleKey: 'aftersale', roleCode: 'SERVICE', roleName: '售后人员', realName: '吴售后', dashboardPath: '/dashboard/aftersale' },
  { username: 'cost', password: '123456', roleKey: 'cost', roleCode: 'COST', roleName: '财务/成本人员', realName: '郑财务', dashboardPath: '/dashboard/cost' }
]

export function findMockUser(username, password) {
  return mockUsers.find((u) => u.username === username && u.password === password)
}
