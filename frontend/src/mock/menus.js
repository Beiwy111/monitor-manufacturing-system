/** 各角色 Mock 菜单 */
export const roleMenus = {
  admin: [
    { menuId: 1, menuName: '系统管理', children: [
      { menuId: 11, menuName: '用户管理', path: '/system/user' },
      { menuId: 12, menuName: '角色管理', path: '/system/role' },
      { menuId: 13, menuName: '权限管理', path: '/system/permission' },
      { menuId: 14, menuName: '菜单管理', path: '/system/menu' },
      { menuId: 15, menuName: '操作日志', path: '/system/log' }
    ]},
    { menuId: 92, menuName: '考勤管理', children: [
      { menuId: 921, menuName: '考勤记录', path: '/attendance/record' },
      { menuId: 922, menuName: '考勤统计', path: '/attendance/statistics' }
    ]}
  ],
  order: [
    { menuId: 3, menuCode: 'order', menuName: '订单管理', children: [
      { menuId: 33, menuName: '订单跟踪', path: '/order/track' },
      { menuId: 32, menuName: '订单审核', path: '/order/audit' },
      { menuId: 31, menuName: '客户订单', path: '/order/list' },
      { menuId: 309, menuName: 'AI识图下单', path: '/order/ai-screenshot' }
    ]}
  ],
  planner: [
    { menuId: 12, menuName: '计划管理', children: [
      { menuId: 121, menuName: '生产计划工作台', path: '/production/plan' },
      { menuId: 122, menuName: '工序设置', path: '/production/process-setup' },
      { menuId: 123, menuName: '订单跟踪', path: '/order/track' }
    ]}
  ],
  manager: [
    { menuId: 40, menuName: '生产监控', children: [
      { menuId: 41, menuName: '生产调度大屏', path: '/system/board' }
    ]},
    { menuId: 4, menuName: '生产管理', children: [
      { menuId: 42, menuName: '生产工单', path: '/production/work-order' },
      { menuId: 43, menuName: '工单派工', path: '/production/dispatch' },
      { menuId: 44, menuName: '生产进度', path: '/production/progress' },
      { menuId: 45, menuName: '生产异常', path: '/production/exception' },
      { menuId: 46, menuName: '安灯报警', path: '/device/alarm' },
      { menuId: 47, menuName: '排班日历', path: '/production/shift-calendar' }
    ]}
  ],
  operator: [
    { menuId: 5, menuName: '现场作业', children: [
      { menuId: 51, menuName: '我的派工', path: '/production/my-dispatch' },
      { menuId: 53, menuName: '工序报工', path: '/production/report' },
      { menuId: 54, menuName: '安灯报警', path: '/device/alarm' },
      { menuId: 55, menuName: '工艺说明', path: '/production/process-guide' }
    ]},
    { menuId: 8, menuName: '报表中心', children: [
      { menuId: 81, menuName: '生产制令单进度表', path: '/report/production-progress' }
    ]}
  ],
  quality: [
    { menuId: 6, menuName: '物料质量管理', children: [
      { menuId: 61, menuName: '待检任务', path: '/quality/material/inspection' },
      { menuId: 62, menuName: '不合格品', path: '/quality/material/defect' },
      { menuId: 63, menuName: '复检处理', path: '/quality/material/reinspect' },
      { menuId: 64, menuName: '质检记录', path: '/quality/material/records' },
      { menuId: 65, menuName: '质量追溯', path: '/quality/material/trace' },
      { menuId: 66, menuName: '报表打印', path: '/quality/material/print' }
    ]},
    { menuId: 118, menuName: '成品质量管理', children: [
      { menuId: 119, menuName: '待检任务', path: '/quality/fp/inspection' },
      { menuId: 120, menuName: '不合格品', path: '/quality/fp/defect' },
      { menuId: 121, menuName: '复检处理', path: '/quality/fp/reinspect' },
      { menuId: 122, menuName: '质检记录', path: '/quality/fp/records' },
      { menuId: 123, menuName: '质量追溯', path: '/quality/fp/trace' },
      { menuId: 124, menuName: '报表打印', path: '/quality/fp/print' }
    ]}
  ],
  purchase: [
    { menuId: 7, menuCode: 'purchase', menuName: '采购管理', children: [
      { menuId: 9060, menuName: '采购工作台', path: '/purchase/workbench' },
      { menuId: 71, menuName: '采购需求', path: '/purchase/demand' },
      { menuId: 72, menuName: '采购订单', path: '/purchase/order' },
      { menuId: 73, menuName: '供应商管理', path: '/purchase/supplier' }
    ]}
  ],
  warehouse: [
    { menuId: 8, menuCode: 'warehouse', menuName: '仓储', children: [
      { menuId: 8807, menuName: '仓储管理工作台', path: '/warehouse/workbench' },
      { menuId: 81, menuName: '入库', path: '/warehouse/inbound-hub' },
      { menuId: 82, menuName: '出库', path: '/warehouse/outbound-hub' },
      { menuId: 83, menuName: '库存容量查询', path: '/warehouse/capacity' },
      { menuId: 84, menuName: '库位图', path: '/warehouse/location-map' }
    ]}
  ],
  device: [
    { menuId: 9, menuName: '设备管理', children: [
      { menuId: 91, menuName: '设备台账', path: '/device/equipment' },
      { menuId: 92, menuName: '生产车间状态', path: '/device/workshop-status' },
      { menuId: 93, menuName: '安灯报警', path: '/device/alarm' },
      { menuId: 94, menuName: '维修处理', path: '/device/maintenance' }
    ]}
  ],
  aftersale: [
    { menuId: 10, menuName: '售后管理', children: [
      { menuId: 100, menuName: '调查工作台', path: '/dashboard/aftersale' },
      { menuId: 101, menuName: '售后工单', path: '/aftersale/work-order' },
      { menuId: 102, menuName: '方案审批', path: '/aftersale/plan' },
      { menuId: 103, menuName: '执行协同', path: '/aftersale/execution' },
      { menuId: 104, menuName: '验证闭环', path: '/aftersale/closure' }
    ]}
  ],
  cost: [
    { menuId: 11, menuName: '财务管理', children: [
      { menuId: 111, menuName: '首页', path: '/dashboard/cost' },
      { menuId: 112, menuName: '成本核算', path: '/finance/cost-accounting' },
      { menuId: 113, menuName: '收益核算', path: '/finance/revenue' },
      { menuId: 114, menuName: '财务大屏', path: '/finance/screen' },
      { menuId: 115, menuName: '财务报表', path: '/finance/report' }
    ]}
  ],
  customer: [
    { menuId: 201, menuName: '客户门户', menuCode: 'customer', children: [
      { menuId: 202, menuName: '新建订单', path: '/customer/order/new', menuCode: 'customer:newOrder' },
      { menuId: 203, menuName: '我的订单', path: '/customer/orders', menuCode: 'customer:orders' },
      { menuId: 204, menuName: '产品与规格', path: '/customer/products', menuCode: 'customer:products' },
      { menuId: 205, menuName: '提交反馈', path: '/customer/feedback/submit', menuCode: 'customer:feedbackSubmit' },
      { menuId: 206, menuName: '我的反馈', path: '/customer/feedback/list', menuCode: 'customer:feedbackList' },
      { menuId: 207, menuName: '个人中心', path: '/customer/profile', menuCode: 'customer:profile' }
    ]}
  ]
}

export function getMenusByRoleKey(roleKey) {
  return roleMenus[roleKey] || []
}
