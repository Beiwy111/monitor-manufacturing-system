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
    { menuId: 3, menuName: '订单管理', children: [
      { menuId: 31, menuName: '客户订单', path: '/order/list' },
      { menuId: 32, menuName: '订单审核', path: '/order/audit' },
      { menuId: 33, menuName: '订单跟踪', path: '/order/track' }
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
    { menuId: 7, menuName: '采购管理', children: [
      { menuId: 71, menuName: '采购需求', path: '/purchase/demand' },
      { menuId: 72, menuName: '采购订单', path: '/purchase/order' },
      { menuId: 73, menuName: '供应商管理', path: '/purchase/supplier' }
    ]}
  ],
  warehouse: [
    { menuId: 8, menuName: '仓储管理', children: [
      { menuId: 81, menuName: '采购入库', path: '/warehouse/purchase-in' },
      { menuId: 82, menuName: '生产领料', path: '/warehouse/issue' },
      { menuId: 83, menuName: '成品入库', path: '/warehouse/inbound' },
      { menuId: 84, menuName: '发货出库', path: '/delivery/list' },
      { menuId: 85, menuName: '库存查询', path: '/warehouse/inventory' },
      { menuId: 86, menuName: '库存流水', path: '/warehouse/flow' },
      { menuId: 87, menuName: '库存预警', path: '/warehouse/alert' }
    ]}
  ],
  device: [
    { menuId: 9, menuName: '设备管理', children: [
      { menuId: 91, menuName: '设备台账', path: '/device/equipment' },
      { menuId: 92, menuName: '设备状态', path: '/device/status' },
      { menuId: 93, menuName: '安灯报警', path: '/device/alarm' },
      { menuId: 94, menuName: '维修处理', path: '/device/maintenance' },
      { menuId: 95, menuName: '维护记录', path: '/device/records' }
    ]}
  ],
  aftersale: [
    { menuId: 10, menuName: '售后管理', children: [
      { menuId: 101, menuName: '售后登记', path: '/aftersale/case' },
      { menuId: 102, menuName: '客户反馈', path: '/aftersale/feedback' },
      { menuId: 103, menuName: '售后处理', path: '/aftersale/process' },
      { menuId: 104, menuName: '质量追溯', path: '/aftersale/trace' },
      { menuId: 105, menuName: '发货查询', path: '/delivery/list' }
    ]}
  ],
  cost: [
    { menuId: 11, menuName: '成本管理', children: [
      { menuId: 111, menuName: '工单成本', path: '/cost/work-order' },
      { menuId: 112, menuName: '材料成本', path: '/cost/material' },
      { menuId: 113, menuName: '人工成本', path: '/cost/labor' },
      { menuId: 114, menuName: '设备成本', path: '/cost/equipment' },
      { menuId: 115, menuName: '成本结算', path: '/cost/settlement' },
      { menuId: 116, menuName: '成本报表', path: '/cost/report' }
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
