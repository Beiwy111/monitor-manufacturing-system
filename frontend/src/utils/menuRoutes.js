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
  'quality:inspection': '/quality/inspection',
  'quality:nonconforming': '/quality/defect',
  'quality:defect': '/quality/defect',
  'quality:reinspect': '/quality/reinspect',
  'quality:records': '/quality/records',
  'quality:trace': '/quality/trace',
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
  'afterSales:settlement': '/cost/settlement'
}

export const BOARD_PATH = '/system/board'

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
      { menuId: 9043, menuName: '工单派工', path: '/production/dispatch' },
      { menuId: 9044, menuName: '生产进度', path: '/production/progress' },
      { menuId: 9045, menuName: '生产异常', path: '/production/exception' }
    ],
    equipment: [{ menuId: 9046, menuName: '安灯报警', path: '/device/alarm' }]
  },
  operator: {
    production: [
      { menuId: 9051, menuName: '我的派工', path: '/production/my-dispatch' },
      { menuId: 9052, menuName: '当前工单', path: '/production/work-order' },
      { menuId: 9053, menuName: '生产报工', path: '/production/report' },
      { menuId: 9054, menuName: '工艺说明', path: '/production/process-guide' }
    ],
    report: [
      { menuId: 9056, menuName: '生产制令单进度表', path: '/report/production-progress' }
    ],
    equipment: [{ menuId: 9055, menuName: '安灯报警', path: '/device/alarm' }]
  },
  quality: {
    quality: [
      { menuId: 9061, menuName: '复检处理', path: '/quality/reinspect' },
      { menuId: 9062, menuName: '质检记录', path: '/quality/records' },
      { menuId: 9063, menuName: '质量追溯', path: '/quality/trace' }
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
  afterSales: '售后成本'
}

export function resolveMenuPath(menu) {
  if (menu?.path) return menu.path
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

  menus = stripBoardFromMenus(menus, roleKey)

  if (!menus.length && fallbackMenus?.length) {
    menus = fallbackMenus
  }

  return menus
}

function mergeRoleExtras(menus, roleKey) {
  const extras = ROLE_MENU_EXTRAS[roleKey]
  if (!extras) return menus
  const result = menus.map((m) => ({
    ...m,
    children: [...(m.children || [])]
  }))
  Object.entries(extras).forEach(([moduleCode, items]) => {
    let group = result.find((m) => m.menuCode === moduleCode)
    if (!group) {
      group = {
        menuId: 9000 + moduleCode.length,
        menuCode: moduleCode,
        menuName: MODULE_NAME_MAP[moduleCode] || moduleCode,
        children: []
      }
      result.push(group)
    }
    const paths = new Set(group.children.map((c) => c.path))
    items.forEach((item) => {
      if (!paths.has(item.path)) group.children.push(item)
    })
  })
  if (roleKey === 'operator') {
    const allowedProduction = new Set([
      '/production/my-dispatch',
      '/production/work-order',
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
  return menus.filter((m) => m.children?.length)
}

/** 各角色登录后的默认首页 */
const ROLE_HOME_PATH = {
  admin: '/system/user',
  manager: BOARD_PATH,
  order: '/order/list',
  planner: '/production/plan',
  operator: '/production/my-dispatch',
  quality: '/quality/inspection',
  purchase: '/purchase/order',
  warehouse: '/warehouse/inventory',
  device: '/device/equipment',
  aftersale: '/aftersale/case',
  cost: '/cost/report'
}

export function getHomePath(roleKey) {
  return ROLE_HOME_PATH[roleKey] || '/system/user'
}

/** @deprecated 请使用 getHomePath(roleKey) */
export const HOME_PATH = BOARD_PATH

/** 生产调度大屏仅生产主管可见 */
export function stripBoardFromMenus(menus, roleKey) {
  if (roleKey === 'manager') return menus
  return (menus || [])
    .map((m) => ({
      ...m,
      children: (m.children || []).filter((c) => c.path !== BOARD_PATH)
    }))
    .filter((m) => m.children?.length)
}
