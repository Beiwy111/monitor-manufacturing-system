import { getMenusByRoleKey } from '@/mock/menus'

/** sys_menu.menuCode → 前端路由 path */
export const MENU_PATH_MAP = {
  'system:role': '/system/role',
  'system:user': '/system/user',
  'system:permission': '/system/permission',
  'system:operationLog': '/system/log',
  'system:menu': '/system/menu',
  'system:board': '/system/board',
  'material:material': '/warehouse/capacity',
  'material:bom': '/warehouse/capacity',
  'material:inventory': '/warehouse/capacity',
  'material:transaction': '/warehouse/capacity',
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
  'purchase:aiDocument': '/order/audit',
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
  'warehouse:purchaseIn': '/warehouse/inbound-hub',
  'warehouse:issue': '/warehouse/outbound-hub',
  'warehouse:inbound': '/warehouse/inbound-hub',
  'warehouse:inventory': '/warehouse/capacity',
  'warehouse:flow': '/warehouse/capacity',
  'warehouse:alert': '/warehouse/location-map',
  'warehouse:locationMap': '/warehouse/location-map',
  'warehouse:inboundHub': '/warehouse/inbound-hub',
  'warehouse:outboundHub': '/warehouse/outbound-hub',
  'warehouse:capacity': '/warehouse/capacity',
  'warehouse:workbench': '/warehouse/workbench',
  'order:list': '/order/list',
  'order:aiScreenshot': '/order/ai-screenshot',
  'order:audit': '/order/audit',
  'order:track': '/order/track',
  'plan:plan': '/production/plan',
  'production:myDispatch': '/production/my-dispatch',
  'production:exception': '/production/exception',
  'production:processGuide': '/production/process-guide',
  'report:productionProgress': '/report/production-progress',
  'device:equipment': '/device/equipment',
  'device:workshop': '/device/workshop-status',
  'device:status': '/device/status',
  'device:alarm': '/device/alarm',
  'device:maintenance': '/device/maintenance',
  'device:records': '/device/records',
  'purchase:workbench': '/purchase/workbench',
  'purchase:demand': '/purchase/demand',
  'purchase:order': '/purchase/order',
  'purchase:supplier': '/purchase/supplier',
  'purchase:arrival': '/purchase/arrival',
  'aftersale:case': '/aftersale/work-order',
  'aftersale:workbench': '/dashboard/aftersale',
  'aftersale:work-order': '/aftersale/work-order',
  'aftersale:plan': '/aftersale/plan',
  'aftersale:execution': '/aftersale/execution',
  'aftersale:closure': '/aftersale/closure',
  'aftersale:feedback': '/aftersale/work-order',
  'aftersale:process': '/aftersale/execution',
  'aftersale:trace': '/dashboard/aftersale',
  'cost:home': '/dashboard/cost',
  'cost:accounting': '/finance/cost-accounting',
  'cost:revenue': '/finance/revenue',
  'cost:screen': '/finance/screen',
  'cost:financeReport': '/finance/report',
  'cost:workOrder': '/finance/cost-accounting',
  'cost:material': '/finance/cost-accounting',
  'cost:labor': '/finance/cost-accounting',
  'cost:equipment': '/finance/cost-accounting',
  'cost:settlement': '/finance/cost-accounting',
  'cost:report': '/finance/report',
  'equipment:equipment': '/device/equipment',
  'equipment:alarm': '/device/alarm',
  'equipment:maintenance': '/device/maintenance',
  'afterSales:afterSalesCase': '/aftersale/work-order',
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
const REMOVED_MENU_PATHS = new Set(['/purchase/arrival', '/device/status', '/device/records'])
const REMOVED_MENU_CODES = new Set(['purchase:arrival', 'device:status', 'device:records'])

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
      { menuId: 9032, menuName: '订单跟踪', path: '/order/track' },
      { menuId: 9031, menuName: '订单审核', path: '/order/audit' },
      { menuId: 9030, menuName: 'AI识图下单', path: '/order/ai-screenshot' }
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
      { menuId: 9060, menuName: '采购工作台', path: '/purchase/workbench' },
      { menuId: 9061, menuName: '采购需求', path: '/purchase/demand' },
      { menuId: 9062, menuName: '采购订单', path: '/purchase/order' },
      { menuId: 9064, menuName: '供应商管理', path: '/purchase/supplier' }
    ]
  },
  warehouse: {
    warehouse: [
      { menuId: 8807, menuName: '仓储管理工作台', path: '/warehouse/workbench' },
      { menuId: 9071, menuName: '入库', path: '/warehouse/inbound-hub' },
      { menuId: 9072, menuName: '出库', path: '/warehouse/outbound-hub' },
      { menuId: 9073, menuName: '库存容量查询', path: '/warehouse/capacity' },
      { menuId: 9074, menuName: '库位图', path: '/warehouse/location-map' }
    ]
  },
  aftersale: {
    aftersale: [
      { menuId: 9100, menuName: '调查工作台', path: '/dashboard/aftersale' },
      { menuId: 9101, menuName: '售后工单', path: '/aftersale/work-order' },
      { menuId: 9102, menuName: '方案审批', path: '/aftersale/plan' },
      { menuId: 9103, menuName: '执行协同', path: '/aftersale/execution' },
      { menuId: 9104, menuName: '验证闭环', path: '/aftersale/closure' }
    ]
  },
  device: {
    equipment: [{ menuId: 9091, menuName: '3D 车间大屏', path: '/dashboard/device' }]
  },
  customer: {}
}

/** 角色侧栏：同一模块下子菜单的 path 顺序 */
const ROLE_CHILD_MENU_ORDER = {
  order: {
    order: [
      '/order/track',
      '/order/audit',
      '/order/list',
      '/order/ai-screenshot'
    ]
  },
  warehouse: {
    warehouse: [
      '/warehouse/workbench',
      '/warehouse/inbound-hub',
      '/warehouse/outbound-hub',
      '/warehouse/capacity',
      '/warehouse/location-map'
    ]
  },
  aftersale: {
    aftersale: [
      '/dashboard/aftersale',
      '/aftersale/work-order',
      '/aftersale/plan',
      '/aftersale/execution',
      '/aftersale/closure'
    ]
  }
}

const WAREHOUSE_ROLE_ALLOWED_PATHS = new Set([
  '/warehouse/workbench',
  '/warehouse/inbound-hub',
  '/warehouse/outbound-hub',
  '/warehouse/capacity',
  '/warehouse/location-map'
])

function sortRoleMenuChildren(menus, roleKey) {
  const moduleOrders = ROLE_CHILD_MENU_ORDER[roleKey]
  if (!moduleOrders) return menus

  return menus.map((m) => {
    const paths = moduleOrders[m.menuCode]
      || ((m.menuCode === 'order' || m.menuName === '订单管理' || m.menuName === '订单发货')
        ? moduleOrders.order
        : null)
    if (!paths?.length || !m.children?.length) return m

    const rank = new Map(paths.map((p, i) => [p, i]))
    const children = [...m.children].sort((a, b) => {
      const ra = rank.has(a.path) ? rank.get(a.path) : 999
      const rb = rank.has(b.path) ? rank.get(b.path) : 999
      return ra - rb || String(a.menuName).localeCompare(String(b.menuName), 'zh-CN')
    })
    return { ...m, children }
  })
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
  device: '设备管理',
  afterSales: '售后成本',
  aftersale: '售后管理',
  cost: '财务管理',
  customer: '客户门户',
  attendance: '考勤管理',
  devWorkbench: '角色工作台',
  plan: '计划管理'
}

/** 管理员侧边栏固定顺序（系统管理、考勤管理保持不变） */
const ADMIN_MENU_ORDER = [
  { type: 'system' },
  { type: 'attendance' },
  { roleKey: 'order', menuName: '订单管理', workbench: { menuId: 8801, menuName: '订单管理工作台', path: '/dashboard/order' } },
  { roleKey: 'purchase', menuName: '采购管理', workbench: { menuId: 8806, menuName: '采购员工作台', path: '/dashboard/purchase' } },
  { roleKey: 'planner', menuName: '计划管理', workbench: { menuId: 8802, menuName: '计划员工作台', path: '/dashboard/planner' } },
  {
    roleKey: 'manager',
    menuName: '生产主管',
    workbench: { menuId: 8811, menuName: '生产调度大屏', path: BOARD_PATH }
  },
  {
    roleKey: 'operator',
    menuName: '操作员',
    workbench: { menuId: 8804, menuName: '工序报工', path: '/production/report' }
  },
  { roleKey: 'quality', menuName: '质检', workbench: { menuId: 8805, menuName: '质检员工作台', path: '/dashboard/quality' } },
  { roleKey: 'warehouse', menuName: '仓储', workbench: { menuId: 8807, menuName: '仓储管理工作台', path: '/warehouse/workbench' } },
  { roleKey: 'device', menuName: '设备维护', workbench: { menuId: 8808, menuName: '设备维护工作台', path: '/dashboard/device' } },
  { roleKey: 'aftersale', menuName: '售后', workbench: { menuId: 8809, menuName: '售后调查工作台', path: '/dashboard/aftersale' } },
  { roleKey: 'cost', menuName: '财务管理', workbench: { menuId: 8810, menuName: '财务成本工作台', path: '/dashboard/cost' } }
]

const ADMIN_SECTION_MENU_ID = {
  order: 8101,
  purchase: 8102,
  planner: 8103,
  manager: 8104,
  operator: 8105,
  quality: 8106,
  warehouse: 8107,
  device: 8108,
  aftersale: 8109,
  cost: 8110
}

/** 管理员侧栏合并时仅纳入的子分组（避免物料/成品质检菜单重复扁平化） */
const ADMIN_ROLE_GROUP_WHITELIST = {
  quality: new Set(['物料质量管理'])
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
    menus = mergeAdminDevMenus(menus)
  } else {
    menus = mergeRoleExtras(menus, roleKey)
    menus = sortRoleMenuChildren(menus, roleKey)
  }

  if (!menus.length && fallbackMenus?.length) {
    return normalizeMenus(fallbackMenus, roleKey, null)
  }

  return dedupeEquipmentMenus(sanitizeMenus(stripManagerOnlyFromMenus(menus, roleKey)))
}

function mergeRoleExtras(menus, roleKey, { strict = true } = {}) {
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
      const moduleName = MODULE_NAME_MAP[moduleCode] || moduleCode
      group = result.find((m) => m.menuCode === moduleCode)
        || result.find((m) => m.menuName === moduleName)
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
    const toAdd = []
    items.forEach((item) => {
      if (!paths.has(item.path)) {
        toAdd.push(item)
        paths.add(item.path)
      }
    })
    if (toAdd.length) group.children.unshift(...toAdd)
  })
  if (strict && roleKey === 'operator') {
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
  if (strict && roleKey === 'warehouse') {
    const paths = new Set()
    const children = []
    const pushChild = (item) => {
      if (!item?.path || !WAREHOUSE_ROLE_ALLOWED_PATHS.has(item.path) || paths.has(item.path)) return
      children.push({ ...item })
      paths.add(item.path)
    }
    result.forEach((m) => {
      ;(m.children || []).forEach(pushChild)
    })
    ;(extras.warehouse || []).forEach(pushChild)
    const order = ROLE_CHILD_MENU_ORDER.warehouse.warehouse
    const rank = new Map(order.map((p, i) => [p, i]))
    children.sort((a, b) => (rank.get(a.path) ?? 999) - (rank.get(b.path) ?? 999))
    return [{
      menuId: 6,
      menuCode: 'warehouse',
      menuName: '仓储',
      children
    }]
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

function isSystemMenuGroup(m) {
  return m?.menuCode === 'system' || m?.menuName === '系统管理'
}

function isAttendanceMenuGroup(m) {
  return m?.menuCode === 'attendance' || m?.menuName === '考勤管理'
}

function cloneMenuGroup(group) {
  return {
    ...group,
    children: [...(group.children || [])]
  }
}

function mergeMenuGroups(target, incoming) {
  for (const group of incoming || []) {
    if (!group?.children?.length) continue
    const key = group.menuCode || group.menuName
    let existing = target.find(
      (m) => (m.menuCode && m.menuCode === group.menuCode)
        || m.menuName === group.menuName
    )
    if (!existing) {
      target.push(cloneMenuGroup(group))
      continue
    }
    const paths = new Set((existing.children || []).map((c) => c.path))
    group.children.forEach((item) => {
      if (item.path && !paths.has(item.path)) {
        existing.children.push({ ...item })
        paths.add(item.path)
      }
    })
  }
}

function ensureSystemGroup(menus) {
  let system = menus.find(isSystemMenuGroup)
  const extras = [{ menuId: 901, menuName: '菜单管理', path: '/system/menu' }]
  if (!system) {
    system = {
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
    }
    menus.unshift(system)
  } else {
    const paths = new Set(system.children.map((c) => c.path))
    extras.forEach((e) => {
      if (!paths.has(e.path)) system.children.push(e)
    })
  }
  return system
}

function ensureAttendanceGroup(menus) {
  let attendance = menus.find(isAttendanceMenuGroup)
  const attendanceItems = [
    { menuId: 9201, menuName: '考勤记录', path: '/attendance/record' },
    { menuId: 9202, menuName: '考勤统计', path: '/attendance/statistics' }
  ]
  if (!attendance) {
    attendance = {
      menuId: 92,
      menuCode: 'attendance',
      menuName: '考勤管理',
      children: [...attendanceItems]
    }
    menus.push(attendance)
  } else {
    const paths = new Set((attendance.children || []).map((c) => c.path))
    attendanceItems.forEach((e) => {
      if (!paths.has(e.path)) attendance.children.push(e)
    })
  }
  return attendance
}

function appendRoleExtrasToSection(section, roleKey) {
  const extras = ROLE_MENU_EXTRAS[roleKey]
  if (!extras || !section) return section
  const paths = new Set((section.children || []).map((c) => c.path))
  Object.values(extras).flat().forEach((item) => {
    if (item.path && !paths.has(item.path)) {
      section.children.push({ ...item })
      paths.add(item.path)
    }
  })
  return section
}

/** 将某角色 mock 菜单扁平化为管理员一级分组 */
function buildAdminRoleSection(roleKey, menuName, workbenchItems = []) {
  const groups = getMenusByRoleKey(roleKey)
  const whitelist = ADMIN_ROLE_GROUP_WHITELIST[roleKey]
  const effectiveGroups = whitelist
    ? groups.filter((g) => whitelist.has(g.menuName))
    : groups
  const children = []
  const paths = new Set()
  const usedNames = new Set()

  const pushChild = (item, groupLabel) => {
    if (!item?.path || paths.has(item.path)) return
    let label = item.menuName || item.path
    if (usedNames.has(label) && groupLabel) {
      label = `${groupLabel} · ${label}`
    }
    usedNames.add(item.menuName || label)
    children.push({ ...item, menuName: label })
    paths.add(item.path)
  }

  ;(Array.isArray(workbenchItems) ? workbenchItems : workbenchItems ? [workbenchItems] : [])
    .forEach((item) => pushChild(item, ''))

  for (const group of effectiveGroups) {
    const groupLabel = effectiveGroups.length > 1 ? (group.menuName || '') : ''
    for (const item of group.children || []) {
      pushChild(item, groupLabel)
    }
  }

  const section = {
    menuId: ADMIN_SECTION_MENU_ID[roleKey] || 8199,
    menuCode: `dev_${roleKey}`,
    menuName,
    children
  }
  appendRoleExtrasToSection(section, roleKey)
  return section
}

/** 管理员：按固定顺序组装各角色完整功能 */
function mergeAdminDevMenus(apiMenus) {
  const preserved = (apiMenus || [])
    .filter((m) => isSystemMenuGroup(m) || isAttendanceMenuGroup(m))
    .map(cloneMenuGroup)
  const scratch = [...preserved]
  ensureSystemGroup(scratch)
  ensureAttendanceGroup(scratch)

  const ordered = []
  for (const spec of ADMIN_MENU_ORDER) {
    if (spec.type === 'system') {
      const system = scratch.find(isSystemMenuGroup)
      if (system) ordered.push(cloneMenuGroup(system))
      continue
    }
    if (spec.type === 'attendance') {
      const attendance = scratch.find(isAttendanceMenuGroup)
      if (attendance) ordered.push(cloneMenuGroup(attendance))
      continue
    }
    if (spec.roleKey) {
      ordered.push(buildAdminRoleSection(spec.roleKey, spec.menuName, spec.workbench))
    }
  }
  return ordered.filter((m) => m?.children?.length)
}

function mergeAdminExtras(menus) {
  return mergeAdminDevMenus(menus)
}

/** 各角色登录后的默认首页（功能工作台；智能对话从侧栏进入） */
export const CHAT_PATH = '/chat'
const ROLE_HOME_PATH = {
  admin: '/dashboard/admin',
  manager: BOARD_PATH,
  order: '/dashboard/order',
  planner: '/dashboard/planner',
  operator: '/dashboard/operator',
  quality: '/dashboard/quality',
  purchase: '/dashboard/purchase',
  warehouse: '/dashboard/warehouse',
  device: '/dashboard/device',
  aftersale: '/dashboard/aftersale',
  cost: '/dashboard/cost',
  customer: '/customer/home'
}

const ROLE_HOME_TITLE = {
  admin: '系统管理工作台',
  manager: '生产调度大屏',
  order: '订单管理工作台',
  planner: '计划员工作台',
  operator: '生产操作员工作台',
  quality: '质检员工作台',
  purchase: '采购员工作台',
  warehouse: '仓储人员首页',
  device: '设备维护工作台',
  aftersale: '调查工作台',
  cost: '财务/成本工作台',
  customer: '产品中心'
}

export function getHomePath(roleKey) {
  return ROLE_HOME_PATH[roleKey] || '/system/user'
}

export function getHomeTitle(roleKey) {
  return ROLE_HOME_TITLE[roleKey] || '首页'
}

/** @deprecated 请使用 getHomePath(roleKey) */
export const HOME_PATH = BOARD_PATH

/** 生产调度大屏、生产工单、工单派工：生产主管与系统管理员可见 */
export function stripManagerOnlyFromMenus(menus, roleKey) {
  if (roleKey === 'manager' || roleKey === 'admin') return menus
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
