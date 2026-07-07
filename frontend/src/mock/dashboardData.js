/** 各角色工作台 Mock 数据 */

export const dashboardConfigs = {
  admin: {
    title: '系统管理工作台',
    subtitle: '用户权限、菜单配置与全局系统监控',
    kpis: [
      { label: '在线用户', value: '12', status: 'processing' },
      { label: '系统角色', value: '9', status: 'normal' },
      { label: '菜单项', value: '36', status: 'normal' },
      { label: '今日操作日志', value: '128', status: 'normal' },
      { label: '异常告警', value: '0', status: 'success' }
    ],
    todos: [
      { id: 1, title: '审核新用户注册申请', module: '用户管理', priority: '中', status: '待处理' },
      { id: 2, title: '检查权限配置变更', module: '权限管理', priority: '高', status: '进行中' },
      { id: 3, title: '导出操作日志', module: '操作日志', priority: '低', status: '待处理' }
    ],
    quickLinks: [
      { title: '用户管理', path: '/system/user' },
      { title: '角色管理', path: '/system/role' },
      { title: '操作日志', path: '/system/log' }
    ]
  },
  manager: {
    title: '生产主管工作台',
    subtitle: '生产计划下达、工单派工与进度监控',
    kpis: [
      { label: '在制工单', value: '24', status: 'processing' },
      { label: '计划达成率', value: '92.6%', status: 'success' },
      { label: '待派工', value: '5', status: 'warning' },
      { label: '生产异常', value: '2', status: 'danger' },
      { label: '安灯报警', value: '1', status: 'warning' }
    ],
    todos: [
      { id: 1, title: 'WO202607003 工序派工', module: '派工管理', priority: '高', status: '待处理' },
      { id: 2, title: 'PP202607002 计划调整', module: '生产计划', priority: '中', status: '进行中' },
      { id: 3, title: '产线2 安灯异常确认', module: '安灯报警', priority: '高', status: '待处理' }
    ],
    quickLinks: [
      { title: '生产计划', path: '/production/plan' },
      { title: '生产工单', path: '/production/work-order' },
      { title: '派工管理', path: '/production/dispatch' }
    ]
  },
  operator: {
    title: '生产操作员工作台',
    subtitle: '现场派工接收、生产执行与报工反馈',
    kpis: [
      { label: '我的派工', value: '3', status: 'processing' },
      { label: '当前工单', value: 'WO202607001', status: 'processing' },
      { label: '今日报工', value: '2 次', status: 'success' },
      { label: '待接收', value: '1', status: 'warning' },
      { label: '安灯状态', value: '正常', status: 'success' }
    ],
    todos: [
      { id: 1, title: '接收派工 DP202607005', module: '我的派工', priority: '高', status: '待处理' },
      { id: 2, title: 'WO202607001 组装工序报工', module: '提交报工', priority: '高', status: '进行中' },
      { id: 3, title: '设备点检确认', module: '现场作业', priority: '中', status: '待处理' }
    ],
    quickLinks: [
      { title: '我的派工', path: '/production/my-dispatch' },
      { title: '提交报工', path: '/production/report' },
      { title: '触发安灯', path: '/device/alarm' }
    ]
  },
  quality: {
    title: '质检员工作台',
    subtitle: '待检任务处理、检验录入与质量追溯',
    kpis: [
      { label: '待检任务', value: '8', status: 'warning' },
      { label: '今日检验', value: '15', status: 'processing' },
      { label: '合格率', value: '98.4%', status: 'success' },
      { label: '不合格品', value: '2', status: 'danger' },
      { label: '待复检', value: '1', status: 'warning' }
    ],
    todos: [
      { id: 1, title: 'WO202607001 终检', module: '待检任务', priority: '高', status: '待处理' },
      { id: 2, title: 'NC202607002 不合格品登记', module: '不合格品', priority: '高', status: '进行中' },
      { id: 3, title: '批次 BT202607 追溯查询', module: '质量追溯', priority: '中', status: '待处理' }
    ],
    quickLinks: [
      { title: '待检任务', path: '/quality/inspection' },
      { title: '质检录入', path: '/quality/inspection' },
      { title: '不合格品登记', path: '/quality/inspection' }
    ]
  },
  warehouse: {
    title: '仓库员工作台',
    subtitle: '物料出入库、库存查询与发货协同',
    kpis: [
      { label: '待入库', value: '4', status: 'warning' },
      { label: '待出库', value: '6', status: 'processing' },
      { label: '库存SKU', value: '186', status: 'normal' },
      { label: '库存预警', value: '3', status: 'warning' },
      { label: '今日流水', value: '22', status: 'normal' }
    ],
    todos: [
      { id: 1, title: 'PO202607003 采购入库', module: '采购入库', priority: '高', status: '待处理' },
      { id: 2, title: 'WO202607002 生产领料', module: '领料出库', priority: '高', status: '进行中' },
      { id: 3, title: 'FG202607001 成品入库', module: '成品入库', priority: '中', status: '待处理' }
    ],
    quickLinks: [
      { title: '库存查询', path: '/warehouse/inventory' },
      { title: '成品入库', path: '/warehouse/inbound' },
      { title: '发货出库', path: '/delivery/list' }
    ]
  },
  purchase: {
    title: '采购员工作台',
    subtitle: '采购订单、到货进度与供应商协同',
    kpis: [
      { label: '进行中订单', value: '7', status: 'processing' },
      { label: '待到货', value: '3', status: 'warning' },
      { label: '本月采购额', value: '¥128万', status: 'normal' },
      { label: '采购异常', value: '1', status: 'danger' },
      { label: '供应商', value: '24', status: 'normal' }
    ],
    todos: [
      { id: 1, title: 'PO202607004 催货跟进', module: '到货进度', priority: '高', status: '待处理' },
      { id: 2, title: 'LCD 面板供应商对账', module: '采购明细', priority: '中', status: '进行中' },
      { id: 3, title: 'PO202607002 到货验收', module: '采购订单', priority: '高', status: '待处理' }
    ],
    quickLinks: [
      { title: '采购订单', path: '/purchase/order' },
      { title: '到货进度', path: '/purchase/order' },
      { title: '采购异常', path: '/purchase/order' }
    ]
  },
  device: {
    title: '设备维护工作台',
    subtitle: '设备台账、状态监控与维修维护',
    kpis: [
      { label: '运行设备', value: '18', status: 'success' },
      { label: '维护中', value: '2', status: 'warning' },
      { label: '故障停机', value: '1', status: 'danger' },
      { label: '待处理报警', value: '2', status: 'warning' },
      { label: '本月维修', value: '5', status: 'normal' }
    ],
    todos: [
      { id: 1, title: '贴片线 SMT-02 故障维修', module: '维修记录', priority: '高', status: '进行中' },
      { id: 2, title: 'AL202607003 安灯报警处理', module: '安灯报警', priority: '高', status: '待处理' },
      { id: 3, title: 'AOI 设备月度保养', module: '维护计划', priority: '中', status: '待处理' }
    ],
    quickLinks: [
      { title: '设备台账', path: '/device/equipment' },
      { title: '安灯报警', path: '/device/alarm' },
      { title: '维修记录', path: '/device/equipment' }
    ]
  },
  aftersale: {
    title: '售后人员工作台',
    subtitle: '售后登记、客户反馈与质量追溯',
    kpis: [
      { label: '待处理案例', value: '4', status: 'warning' },
      { label: '本月登记', value: '12', status: 'normal' },
      { label: '处理中', value: '3', status: 'processing' },
      { label: '已关闭', value: '9', status: 'success' },
      { label: '追溯查询', value: '6', status: 'normal' }
    ],
    todos: [
      { id: 1, title: 'AS202607005 屏幕亮点投诉', module: '售后登记', priority: '高', status: '待处理' },
      { id: 2, title: '批次 BT202605 追溯', module: '质量追溯', priority: '中', status: '进行中' },
      { id: 3, title: '客户反馈回访', module: '客户反馈', priority: '中', status: '待处理' }
    ],
    quickLinks: [
      { title: '售后登记', path: '/aftersale/case' },
      { title: '发货查询', path: '/delivery/list' },
      { title: '质量追溯', path: '/quality/inspection' }
    ]
  },
  cost: {
    title: '财务/成本工作台',
    subtitle: '工单成本归集、结算与报表分析',
    kpis: [
      { label: '待结算工单', value: '6', status: 'warning' },
      { label: '本月总成本', value: '¥86万', status: 'normal' },
      { label: '材料成本占比', value: '62%', status: 'processing' },
      { label: '人工成本占比', value: '24%', status: 'normal' },
      { label: '设备成本占比', value: '14%', status: 'normal' }
    ],
    todos: [
      { id: 1, title: 'WO202607001 成本结算', module: '成本结算', priority: '高', status: '待处理' },
      { id: 2, title: '6月成本报表生成', module: '成本报表', priority: '中', status: '进行中' },
      { id: 3, title: '材料成本差异分析', module: '材料成本', priority: '中', status: '待处理' }
    ],
    quickLinks: [
      { title: '成本结算', path: '/cost/settlement' },
      { title: '工单成本', path: '/cost/settlement' },
      { title: '成本报表', path: '/cost/settlement' }
    ]
  }
}

export function getDashboardConfig(roleKey) {
  return dashboardConfigs[roleKey] || dashboardConfigs.admin
}
