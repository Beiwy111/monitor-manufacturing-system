/** sys_menu.menuCode → 前端路由 path */
export const MENU_PATH_MAP = {
  'system:role': '/system/role',
  'system:user': '/system/user',
  'system:permission': '/system/permission',
  'system:operationLog': '/system/log',
  'system:menu': '/system/menu',
  'system:board': '/system/board',
  'material:material': '/warehouse/inventory',
  'material:bom': '/warehouse/inventory',
  'material:inventory': '/warehouse/inventory',
  'material:transaction': '/warehouse/flow',
  'order:customerOrder': '/order/list',
  'order:orderItem': '/order/list',
  'order:delivery': '/delivery/list',
  'production:plan': '/production/plan',
  'production:planItem': '/production/plan',
  'production:route': '/production/process-setup',
  'production:step': '/production/process-setup',
  'production:workOrder': '/production/work-order',
  'production:dispatch': '/production/dispatch',
  'production:report': '/production/report',
  'production:progress': '/production/progress',
  'purchase:purchaseOrder': '/purchase/order',
  'purchase:purchaseOrderItem': '/purchase/order',
  'purchase:aiDocument': '/purchase/ai-document',
  'quality:inspection': '/quality/inspection',
  'quality:nonconforming': '/quality/material/defect',
  'quality:defect': '/quality/material/defect',
  'quality:reinspect': '/quality/material/reinspect',
  'quality:records': '/quality/material/records',
  'quality:trace': '/quality/material/trace',
  'quality:print': '/quality/material/print',
  'quality:material:inspection': '/quality/material/inspection',
  'quality:material:defect': '/quality/material/defect',
  'quality:material:reinspect': '/quality/material/reinspect',
  'quality:material:records': '/quality/material/records',
  'quality:material:trace': '/quality/material/trace',
  'quality:material:print': '/quality/material/print',
  'quality:semi:print': '/quality/material/print',
  'quality:fp:inspection': '/quality/fp/inspection',
  'quality:fp:defect': '/quality/fp/defect',
  'quality:fp:reinspect': '/quality/fp/reinspect',
  'quality:fp:records': '/quality/fp/records',
  'quality:fp:trace': '/quality/fp/trace',
  'quality:fp:print': '/quality/fp/print',
  'warehouse:purchaseIn': '/warehouse/purchase-in',
  'warehouse:issue': '/warehouse/issue',
  'warehouse:inbound': '/warehouse/inbound',
  'warehouse:inventory': '/warehouse/inventory',
  'warehouse:flow': '/warehouse/flow',
  'warehouse:alert': '/warehouse/alert',
  'order:list': '/order/list',
  'order:audit': '/order/audit',
  'order:track': '/order/track',
  'plan:plan': '/production/plan',
  'production:myDispatch': '/production/my-dispatch',
  'production:exception': '/production/exception',
  'production:processGuide': '/production/process-guide',
  'report:productionProgress': '/report/production-progress',
  'device:equipment': '/device/equipment',
  'device:status': '/device/status',
  'device:alarm': '/device/alarm',
  'device:maintenance': '/device/maintenance',
  'device:records': '/device/records',
  'purchase:demand': '/purchase/demand',
  'purchase:order': '/purchase/order',
  'purchase:supplier': '/purchase/supplier',
  'purchase:arrival': '/purchase/arrival',
  'aftersale:case': '/aftersale/case',
  'aftersale:feedback': '/aftersale/feedback',
  'aftersale:process': '/aftersale/process',
  'aftersale:trace': '/aftersale/trace',
  'cost:workOrder': '/cost/work-order',
  'cost:material': '/cost/material',
  'cost:labor': '/cost/labor',
  'cost:equipment': '/cost/equipment',
  'cost:settlement': '/cost/settlement',
  'cost:report': '/cost/report',
  'equipment:equipment': '/device/equipment',
  'equipment:alarm': '/device/alarm',
  'equipment:maintenance': '/device/maintenance',
  'afterSales:afterSalesCase': '/aftersale/case',
  'afterSales:settlement': '/cost/settlement',
  'customer:newOrder': '/customer/order/new',
  'customer:orders': '/customer/orders',
  'customer:products': '/customer/products',
  'customer:feedbackSubmit': '/customer/feedback/submit',
  'customer:feedbackList': '/customer/feedback/list',
  'customer:profile': '/customer/profile',
  'attendance:record': '/attendance/record',
  'attendance:statistics': '/attendance/statistics',
  'production:shiftCalendar': '/production/shift-calendar'
}

export const BOARD_PATH = '/system/board'

/** 仅生产主管可访问的路由 */
export const MANAGER_ONLY_PATHS = new Set([
  BOARD_PATH,
  '/production/work-order',
  '/production/dispatch',
  '/production/shift-calendar'
])

/** 已下线、需从导航中移除的菜单（按 path 或 menuCode 匹配） */
const REMOVED_MENU_PATHS = new Set(['/purchase/arrival'])
const REMOVED_MENU_CODES = new Set(['purchase:arrival'])

/** 设备模块 menuCode 别名（后端 sys_menu 可能为 device 或 equipment） */
const EQUIPMENT_MODULE_CODES = new Set(['equipment', 'device'])

function findEquipmentGroup(menus) {
  return (menus || []).find((m) => EQUIPMENT_MODULE_CODES.has(m.menuCode) || m.menuName === '设备管理')
}

/** 合并重复的「设备管理」分组，保留第一个并汇总子菜单 */
function dedupeEquipmentMenus(menus) {
  const list = menus || []
  const groups = list.filter((m) => EQUIPMENT_MODULE_CODES.has(m.menuCode) || m.menuName === '设备管理')
  if (groups.length <= 1) return list

  const primary = groups[0]
  const mergedChildren = [...(primary.children || [])]
  const paths = new Set(mergedChildren.map((c) => c.path))

  groups.slice(1).forEach((g) => {
    ;(g.children || []).forEach((item) => {
      if (item.path && !paths.has(item.path)) {
        mergedChildren.push(item)
        paths.add(item.path)
      }
    })
  })

  primary.children = mergedChildren
  const dropIds = new Set(groups.slice(1).map((g) => g.menuId))
  return list.filter((m) => !dropIds.has(m.menuId))
}

/** 清理菜单树中的下线项，同时用于 localStorage 缓存兜底数据 */
export function sanitizeMenus(menus) {
  return dedupeEquipmentMenus(
    (menus || [])
      .map((m) => ({
        ...m,
        children: (m.children || []).filter(
          (c) => !REMOVED_MENU_PATHS.has(c.path) && !REMOVED_MENU_CODES.has(c.menuCode)
        )
      }))
      .filter((m) => m.children?.length)
  )
}

/** 各角色在前端有、但 sys_menu 未单独建项的路由 */
const ROLE_MENU_EXTRAS = {
  order: {
    order: [
      { menuId: 9031, menuName: '订单审核', path: '/order/audit' },
      { menuId: 9032, menuName: '订单跟踪', path: '/order/track' }
    ]
  },
  planner: {
    production: [{ menuId: 9121, menuName: '工序设置', path: '/production/process-setup' }],
    order: [{ menuId: 9122, menuName: '订单跟踪', path: '/order/track' }]
  },
  manager: {
    production: [
      { menuId: 9041, menuName: '生产调度大屏', path: BOARD_PATH },
      { menuId: 9042, menuName: '生产工单', path: '/production/work-order' },
      { menuId: 9043, menuName: '工单派工', path: '/production/dispatch' },
      { menuId: 9044, menuName: '生产进度', path: '/production/progress' },
      { menuId: 9047, menuName: '排班日历', path: '/production/shift-calendar' }
    ],
    equipment: [{ menuId: 9046, menuName: '安灯报警', path: '/device/alarm' }]
  },
  operator: {
    production: [
      { menuId: 9051, menuName: '我的派工', path: '/production/my-dispatch' },
      { menuId: 9053, menuName: '生产报工', path: '/production/report' },
      { menuId: 9054, menuName: '工艺说明', path: '/production/process-guide' }
    ],
    report: [
      { menuId: 9056, menuName: '生产制令单进度表', path: '/report/production-progress' }
    ],
    equipment: [{ menuId: 9055, menuName: '安灯报警', path: '/device/alarm' }]
  },
  purchase: {
    purchase: [
      { menuId: 9061, menuName: '采购需求', path: '/purchase/demand' },
      { menuId: 9062, menuName: '采购订单', path: '/purchase/order' },
      { menuId: 9064, menuName: '供应商管理', path: '/purchase/supplier' },
      { menuId: 9065, menuName: 'AI 单据录入', path: '/purchase/ai-document' }
    ]
  },
  warehouse: {
    warehouse: [
      { menuId: 9071, menuName: '采购入库', path: '/warehouse/purchase-in' },
      { menuId: 9072, menuName: '生产领料', path: '/warehouse/issue' },
      { menuId: 9073, menuName: '成品入库', path: '/warehouse/inbound' },
      { menuId: 9074, menuName: '库存流水', path: '/warehouse/flow' },
      { menuId: 9075, menuName: '库存预警', path: '/warehouse/alert' }
    ]
  }
}

/** 按模块 menuCode 推断前端一级模块名 */
const MODULE_NAME_MAP = {
  system: '系统管理',
  material: '物料库存',
  order: '订单发货',
  production: '生产管理',
  report: '报表中心',
  purchase: '采购管理',
  quality: '质量管理',
  warehouse: '仓储管理',
  equipment: '设备管理',
  afterSales: '售后成本',
  attendance: '考勤管理'
}

export function resolveMenuPath(menu) {
  if (menu?.path) return menu.path
  if (menu?.routePath) return menu.routePath
  if (menu?.menuCode && MENU_PATH_MAP[menu.menuCode]) {
    return MENU_PATH_MAP[menu.menuCode]
  }
  const businessPath = menu?.businessTable || menu?.business_table
  if (typeof businessPath === 'string' && businessPath.startsWith('/')) {
    return businessPath
  }
  return ''
}

/** 为后端菜单树补全 path，并过滤无路由的子项 */
export function normalizeMenus(apiMenus, roleKey, fallbackMenus) {
  let menus = (apiMenus || []).map((m) => normalizeNode(m)).filter(Boolean)

  if (roleKey === 'admin') {
    menus = mergeAdminExtras(menus)
  } else {
    menus = mergeRoleExtras(menus, roleKey)
  }

  if (!menus.length && fallbackMenus?.length) {
    return normalizeMenus(fallbackMenus, roleKey, null)
  }

  return dedupeEquipmentMenus(sanitizeMenus(stripManagerOnlyFromMenus(menus, roleKey)))
}

function mergeRoleExtras(menus, roleKey) {
  const extras = ROLE_MENU_EXTRAS[roleKey]
  if (!extras) return menus
  const result = menus.map((m) => ({
    ...m,
    children: [...(m.children || [])]
  }))
  Object.entries(extras).forEach(([moduleCode, items]) => {
    let group
    if (moduleCode === 'equipment') {
      group = findEquipmentGroup(result)
      if (!group) {
        group = {
          menuId: 9000 + moduleCode.length,
          menuCode: moduleCode,
          menuName: MODULE_NAME_MAP[moduleCode] || moduleCode,
          children: []
        }
        result.push(group)
      }
    } else {
      group = result.find((m) => m.menuCode === moduleCode)
      if (!group) {
        group = {
          menuId: 9000 + moduleCode.length,
          menuCode: moduleCode,
          menuName: MODULE_NAME_MAP[moduleCode] || moduleCode,
          children: []
        }
        result.push(group)
      }
    }
    const paths = new Set(group.children.map((c) => c.path))
    items.forEach((item) => {
      if (!paths.has(item.path)) group.children.push(item)
    })
  })
  if (roleKey === 'operator') {
    const allowedProduction = new Set([
      '/production/my-dispatch',
      '/production/report',
      '/production/process-guide'
    ])
    const allowedReport = new Set(['/report/production-progress'])
    result.forEach((m) => {
      if (m.menuCode === 'production') {
        m.children = m.children.filter((c) => allowedProduction.has(c.path))
      }
      if (m.menuCode === 'report') {
        m.children = m.children.filter((c) => allowedReport.has(c.path))
      }
      if (m.menuCode === 'equipment') {
        m.children = m.children.filter((c) => c.path === '/device/alarm')
      }
    })
  }
  return result.filter((m) => m.children?.length)
}

function normalizeNode(node) {
  if (!node) return null
  const children = (node.children || [])
    .map((c) => {
      const path = resolveMenuPath(c)
      if (!path) return null
      if (REMOVED_MENU_PATHS.has(path) || REMOVED_MENU_CODES.has(c.menuCode)) return null
      return { ...c, path }
    })
    .filter(Boolean)

  if (!children.length && node.menuLevel !== 1) return null

  return {
    ...node,
    menuName: node.menuName || MODULE_NAME_MAP[node.menuCode] || node.menuCode,
    children
  }
}

function mergeAdminExtras(menus) {
  const system = menus.find((m) => m.menuCode === 'system' || m.menuName === '系统管理')
  const extras = [{ menuId: 901, menuName: '菜单管理', path: '/system/menu' }]
  if (system) {
    const paths = new Set(system.children.map((c) => c.path))
    extras.forEach((e) => {
      if (!paths.has(e.path)) system.children.push(e)
    })
  } else {
    menus.unshift({
      menuId: 1,
      menuCode: 'system',
      menuName: '系统管理',
      children: [
        { menuId: 11, menuName: '用户管理', path: '/system/user' },
        { menuId: 12, menuName: '角色管理', path: '/system/role' },
        { menuId: 13, menuName: '权限管理', path: '/system/permission' },
        { menuId: 14, menuName: '菜单管理', path: '/system/menu' },
        { menuId: 15, menuName: '操作日志', path: '/system/log' }
      ]
    })
  }
  let attendance = menus.find((m) => m.menuCode === 'attendance' || m.menuName === '考勤管理')
  const attendanceItems = [
    { menuId: 9201, menuName: '考勤记录', path: '/attendance/record' },
    { menuId: 9202, menuName: '考勤统计', path: '/attendance/statistics' }
  ]
  if (!attendance) {
    attendance = { menuId: 92, menuCode: 'attendance', menuName: '考勤管理', children: [...attendanceItems] }
    menus.push(attendance)
  } else {
    const paths = new Set((attendance.children || []).map((c) => c.path))
    attendanceItems.forEach((e) => {
      if (!paths.has(e.path)) attendance.children.push(e)
    })
  }
  return menus.filter((m) => m.children?.length)
}

/** 各角色登录后的默认首页 */
const ROLE_HOME_PATH = {
  admin: '/system/user',
  manager: BOARD_PATH,
  order: '/order/list',
  planner: '/production/plan',
  operator: '/production/my-dispatch',
  quality: '/dashboard/quality',
  purchase: '/dashboard/purchase',
  warehouse: '/warehouse/inventory',
  device: '/dashboard/device',
  aftersale: '/aftersale/case',
  cost: '/cost/report',
  customer: '/customer/home'
}

export function getHomePath(roleKey) {
  return ROLE_HOME_PATH[roleKey] || '/system/user'
}

/** @deprecated 请使用 getHomePath(roleKey) */
export const HOME_PATH = BOARD_PATH

/** 生产调度大屏、生产工单、工单派工仅生产主管可见 */
export function stripManagerOnlyFromMenus(menus, roleKey) {
  if (roleKey === 'manager') return menus
  return (menus || [])
    .map((m) => ({
      ...m,
      children: (m.children || []).filter((c) => !MANAGER_ONLY_PATHS.has(c.path))
    }))
    .filter((m) => m.children?.length)
}

/** 生产调度大屏仅生产主管可见 */
export function stripBoardFromMenus(menus, roleKey) {
  if (roleKey === 'manager') return menus
  return stripManagerOnlyFromMenus(menus, roleKey)
}
