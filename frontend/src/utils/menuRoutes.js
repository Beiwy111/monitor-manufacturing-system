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
  'production:route': '/production/process-guide',
  'production:step': '/production/process-guide',
  'production:workOrder': '/production/work-order',
  'production:dispatch': '/production/dispatch',
  'production:report': '/production/report',
  'production:progress': '/production/progress',
  'purchase:purchaseOrder': '/purchase/order',
  'purchase:purchaseOrderItem': '/purchase/order',
  'quality:inspection': '/quality/inspection',
  'quality:nonconforming': '/quality/defect',
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
    equipment: [{ menuId: 9055, menuName: '安灯报警', path: '/device/alarm' }]
  }
}

/** 按模块 menuCode 推断前端一级模块名 */
const MODULE_NAME_MAP = {
  system: '系统管理',
  material: '物料库存',
  order: '订单发货',
  production: '生产管理',
  purchase: '采购管理',
  quality: '质量管理',
  equipment: '设备管理',
  afterSales: '售后成本'
}

export function resolveMenuPath(menu) {
  if (menu?.path) return menu.path
  if (menu?.menuCode && MENU_PATH_MAP[menu.menuCode]) {
    return MENU_PATH_MAP[menu.menuCode]
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
    result.forEach((m) => {
      if (m.menuCode === 'production') {
        m.children = m.children.filter((c) => allowedProduction.has(c.path))
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
