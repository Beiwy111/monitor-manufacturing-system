/** 轻量化工作台：角色快捷入口 */
export const ROLE_QUICK_LINKS = {
  order: [
    { label: '订单审核', path: '/order/audit' },
    { label: '订单跟踪', path: '/order/track' },
    { label: '订单列表', path: '/order/list' }
  ],
  admin: [
    { label: '用户管理', path: '/system/user' },
    { label: '角色权限', path: '/system/role' },
    { label: '操作日志', path: '/system/log' }
  ],
  planner: [
    { label: '生产计划', path: '/production/plan' },
    { label: '订单跟踪', path: '/order/track' },
    { label: '设备列表', path: '/equipment/list' }
  ],
  operator: [
    { label: '我的派工', path: '/production/my-dispatch' },
    { label: '工序报工', path: '/production/report' },
    { label: '设备告警', path: '/equipment/alarm' }
  ],
  cost: [
    { label: '成本结算', path: '/cost/settlement' },
    { label: '成本报表', path: '/cost/report' },
    { label: '订单列表', path: '/order/list' }
  ],
  purchase: [
    { label: '采购工作台', path: '/purchase/workbench' },
    { label: '采购需求', path: '/purchase/demand' },
    { label: '采购订单', path: '/purchase/order' }
  ]
}

/** 任务卡片低饱和背景色 */
export const TASK_CARD_COLORS = ['#fef4cf', '#d8ecfd', '#fde3e5', '#e8e4f8', '#dff5ee']

/** 日历日期标记色 */
export const CALENDAR_MARK_COLORS = ['#f5c6cb', '#b8d4f0', '#fde68a', '#c4b5fd', '#86efac']

export const LITE_THEME = {
  bg: '#eef5f1',
  panel: '#ffffff',
  border: '#e8ece9',
  shadow: '0 4px 24px rgba(30, 50, 40, 0.06)',
  text: '#2c3540',
  muted: '#8a9199',
  accent: {
    order: '#6b8fc7',
    admin: '#9b8ec4',
    planner: '#5ba8a8',
    operator: '#6aab7a',
    cost: '#c9956a',
    purchase: '#6b9fd4'
  }
}
