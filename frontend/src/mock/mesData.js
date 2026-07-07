import { DISPLAY_MATERIALS, EQUIPMENT_TYPES, PROCESS_STEPS } from './constants'

const now = () => new Date().toISOString().slice(0, 19).replace('T', ' ')

/** 初始 MES 业务数据 — 集中管理，后续替换为 API */
export function createInitialMesData() {
  const ts = now()
  return {
    sysUsers: [
      { id: 1, username: 'admin', realName: '系统管理员', roleKey: 'admin', roleName: '系统管理员', phone: '13800000001', status: '启用', createdAt: ts },
      { id: 2, username: 'sales', realName: '张销售', roleKey: 'order', roleName: '订单管理员', phone: '13800000002', status: '启用', createdAt: ts },
      { id: 3, username: 'manager', realName: '李主管', roleKey: 'manager', roleName: '生产主管', phone: '13800000003', status: '启用', createdAt: ts },
      { id: 4, username: 'operator', realName: '王操作', roleKey: 'operator', roleName: '生产操作员', phone: '13800000004', status: '启用', createdAt: ts },
      { id: 5, username: 'qc', realName: '赵质检', roleKey: 'quality', roleName: '质检员', phone: '13800000005', status: '启用', createdAt: ts },
      { id: 6, username: 'buyer', realName: '刘采购', roleKey: 'purchase', roleName: '采购员', phone: '13800000006', status: '启用', createdAt: ts },
      { id: 7, username: 'warehouse', realName: '陈仓管', roleKey: 'warehouse', roleName: '仓库管理员', phone: '13800000007', status: '启用', createdAt: ts },
      { id: 8, username: 'device', realName: '周设备', roleKey: 'device', roleName: '设备维护', phone: '13800000008', status: '启用', createdAt: ts },
      { id: 9, username: 'aftersale', realName: '吴售后', roleKey: 'aftersale', roleName: '售后人员', phone: '13800000009', status: '启用', createdAt: ts },
      { id: 10, username: 'cost', realName: '郑财务', roleKey: 'cost', roleName: '财务/成本', phone: '13800000010', status: '启用', createdAt: ts }
    ],
    sysRoles: [
      { id: 1, roleKey: 'admin', roleName: '系统管理员', permCount: 48, userCount: 1, status: '启用' },
      { id: 2, roleKey: 'order', roleName: '订单管理员', permCount: 12, userCount: 1, status: '启用' },
      { id: 3, roleKey: 'manager', roleName: '生产主管', permCount: 24, userCount: 1, status: '启用' },
      { id: 4, roleKey: 'operator', roleName: '生产操作员', permCount: 10, userCount: 1, status: '启用' },
      { id: 5, roleKey: 'quality', roleName: '质检员', permCount: 14, userCount: 1, status: '启用' },
      { id: 6, roleKey: 'purchase', roleName: '采购员', permCount: 12, userCount: 1, status: '启用' },
      { id: 7, roleKey: 'warehouse', roleName: '仓库管理员', permCount: 16, userCount: 1, status: '启用' },
      { id: 8, roleKey: 'device', roleName: '设备维护', permCount: 10, userCount: 1, status: '启用' },
      { id: 9, roleKey: 'aftersale', roleName: '售后人员', permCount: 10, userCount: 1, status: '启用' },
      { id: 10, roleKey: 'cost', roleName: '财务/成本', permCount: 12, userCount: 1, status: '启用' }
    ],
    sysPermissions: [
      { id: 1, code: 'order:create', name: '创建订单', module: '订单管理' },
      { id: 2, code: 'order:audit', name: '审核订单', module: '订单管理' },
      { id: 3, code: 'plan:publish', name: '发布计划', module: '生产管理' },
      { id: 4, code: 'wo:dispatch', name: '工单派工', module: '生产管理' },
      { id: 5, code: 'qc:inspect', name: '质检录入', module: '质量管理' },
      { id: 6, code: 'wh:inbound', name: '入库操作', module: '仓储管理' }
    ],
    sysMenus: [
      { id: 1, menuName: '系统管理', parentId: 0, path: '', sort: 1 },
      { id: 11, menuName: '用户管理', parentId: 1, path: '/system/user', sort: 1 },
      { id: 12, menuName: '角色管理', parentId: 1, path: '/system/role', sort: 2 }
    ],
    customers: [
      { id: 1, name: '华星光电科技', contact: '陈经理', phone: '021-88880001' },
      { id: 2, name: '京东方显示', contact: '林总监', phone: '010-66660002' },
      { id: 3, name: '联想显示器事业部', contact: '王采购', phone: '010-77770003' }
    ],
    orders: [
      {
        id: 'ORD-2026-001', customerId: 3, customerName: '联想显示器事业部', productModel: 'DM-27-LCD-FHD',
        panelType: 'LCD', quantity: 500, deliveryDate: '2026-04-15', status: '待审核',
        amount: 1250000, salesPerson: '张销售', remark: '27寸 FHD 办公显示器，需坏点 A 级',
        createdAt: ts, updatedAt: ts
      },
      {
        id: 'ORD-2026-002', customerId: 1, customerName: '华星光电科技', productModel: 'DM-32-OLED-4K',
        panelType: 'OLED', quantity: 200, deliveryDate: '2026-04-20', status: '已审核',
        amount: 960000, salesPerson: '张销售', remark: '32寸 OLED 4K 专业显示器',
        createdAt: ts, updatedAt: ts
      },
      {
        id: 'ORD-2026-003', customerId: 2, customerName: '京东方显示', productModel: 'DM-24-LCD-FHD',
        panelType: 'LCD', quantity: 1000, deliveryDate: '2026-03-30', status: '生产中',
        amount: 1500000, salesPerson: '张销售', remark: '24寸批量订单',
        createdAt: ts, updatedAt: ts, planId: 'PLAN-2026-001', workOrderId: 'WO-2026-001'
      }
    ],
    plans: [
      {
        id: 'PLAN-2026-001', orderId: 'ORD-2026-003', orderNo: 'ORD-2026-003',
        productModel: 'DM-24-LCD-FHD', quantity: 1000, planStart: '2026-03-01', planEnd: '2026-03-28',
        status: '执行中', manager: '李主管', remark: '三班倒生产',
        createdAt: ts, updatedAt: ts
      }
    ],
    workOrders: [
      {
        id: 'WO-2026-001', planId: 'PLAN-2026-001', orderId: 'ORD-2026-003', orderNo: 'ORD-2026-003',
        productModel: 'DM-24-LCD-FHD', quantity: 1000, completedQty: 320, qualifiedQty: 310,
        status: '生产中', line: '装配线 A', manager: '李主管',
        createdAt: ts, updatedAt: ts
      }
    ],
    dispatches: [
      {
        id: 'DIS-2026-001', workOrderId: 'WO-2026-001', workOrderNo: 'WO-2026-001',
        processStep: '背光组装', equipment: '装配线 A', operator: 'operator', operatorName: '王操作',
        planQty: 200, completedQty: 120, status: '生产中',
        planStart: '2026-03-05 08:00', planEnd: '2026-03-05 18:00',
        createdAt: ts, updatedAt: ts
      },
      {
        id: 'DIS-2026-002', workOrderId: 'WO-2026-001', workOrderNo: 'WO-2026-001',
        processStep: '点亮测试', equipment: '点亮测试设备 T1', operator: 'operator', operatorName: '王操作',
        planQty: 150, completedQty: 0, status: '已分配',
        planStart: '2026-03-06 08:00', planEnd: '2026-03-06 18:00',
        createdAt: ts, updatedAt: ts
      }
    ],
    workReports: [
      {
        id: 'RPT-2026-001', dispatchId: 'DIS-2026-001', workOrderId: 'WO-2026-001',
        processStep: '背光组装', operator: 'operator', operatorName: '王操作',
        reportQty: 80, qualifiedQty: 78, unqualifiedQty: 2, workHours: 6.5,
        status: '已确认', remark: '正常生产', createdAt: ts
      }
    ],
    inspections: [
      {
        id: 'QC-2026-001', reportId: 'RPT-2026-001', workOrderId: 'WO-2026-001', batchNo: 'BATCH-WO001-001',
        productModel: 'DM-24-LCD-FHD', qcType: '过程检验', qcItems: ['外观检查', '点亮测试'],
        sampleQty: 10, qualifiedQty: 10, unqualifiedQty: 0, result: '合格', status: '合格',
        inspector: 'qc', inspectorName: '赵质检', remark: '', createdAt: ts, updatedAt: ts
      },
      {
        id: 'QC-2026-002', reportId: null, workOrderId: 'WO-2026-001', batchNo: 'BATCH-WO001-002',
        productModel: 'DM-24-LCD-FHD', qcType: '终检', qcItems: QC_ITEMS_PLACEHOLDER(),
        sampleQty: 20, qualifiedQty: 0, unqualifiedQty: 0, result: '', status: '待检',
        inspector: '', inspectorName: '', remark: '', createdAt: ts, updatedAt: ts
      }
    ],
    defects: [],
    purchaseDemands: [
      { id: 'PD-001', materialCode: 'LCD-PANEL', materialName: 'LCD 面板', requiredQty: 500, stockQty: 120, gapQty: 380, status: '待采购', sourceOrder: 'ORD-2026-001', createdAt: ts }
    ],
    purchaseOrders: [
      {
        id: 'PO-2026-001', supplier: '深面板科技', materialCode: 'LCD-PANEL', materialName: 'LCD 面板',
        quantity: 600, unitPrice: 280, totalAmount: 168000, status: '部分到货',
        expectedDate: '2026-03-20', arrivedQty: 400, buyer: '刘采购', createdAt: ts, updatedAt: ts
      }
    ],
    suppliers: [
      { id: 1, name: '深面板科技', contact: '黄经理', materials: 'LCD/OLED 面板', phone: '0755-12345678' },
      { id: 2, name: '精控电子', contact: '吴工', materials: '主控板/电源板', phone: '0512-87654321' }
    ],
    inventory: DISPLAY_MATERIALS.map((m, i) => ({
      id: `INV-${m.code}`,
      materialCode: m.code,
      materialName: m.name,
      unit: m.unit,
      quantity: [120, 80, 350, 420, 380, 500, 480, 600][i] || 200,
      safeQty: 100,
      status: '正常',
      location: `A区-${String(i + 1).padStart(2, '0')}`,
      updatedAt: ts
    })).concat([
      { id: 'INV-FG-001', materialCode: 'FG-DM24', materialName: '成品 DM-24-LCD-FHD', unit: '台', quantity: 280, safeQty: 50, status: '正常', location: '成品仓-F1', updatedAt: ts }
    ]),
    stockFlows: [
      { id: 'SF-001', flowType: '采购入库', materialCode: 'LCD-PANEL', materialName: 'LCD 面板', quantity: 400, direction: '入', refNo: 'PO-2026-001', operator: '陈仓管', createdAt: ts }
    ],
    inboundTasks: [
      { id: 'IN-001', sourceType: '质检合格', refNo: 'QC-2026-001', productModel: 'DM-24-LCD-FHD', quantity: 78, status: '待入库', batchNo: 'BATCH-WO001-001', workOrderId: 'WO-2026-001', orderId: 'ORD-2026-003', createdAt: ts }
    ],
    issueTasks: [
      { id: 'IS-001', workOrderId: 'WO-2026-001', materialCode: 'BL-MODULE', materialName: '背光模组', requiredQty: 200, issuedQty: 120, status: '部分领料', createdAt: ts }
    ],
    deliveries: [
      { id: 'DLV-2026-001', orderId: 'ORD-2026-003', orderNo: 'ORD-2026-003', customerName: '京东方显示',
        productModel: 'DM-24-LCD-FHD', quantity: 200, status: '待出库', shipDate: '', trackingNo: '', createdAt: ts }
    ],
    equipment: EQUIPMENT_TYPES.map((type, i) => ({
      id: `EQ-${String(i + 1).padStart(3, '0')}`,
      name: `${type} ${i + 1}`,
      type,
      line: i < 2 ? '装配线 A' : '测试区 B',
      status: i === 2 ? '故障' : '运行中',
      lastMaint: '2026-02-15',
      downtimeHours: i === 2 ? 4.5 : 0,
      updatedAt: ts
    })),
    alarms: [
      {
        id: 'ALM-2026-001', type: '设备故障', source: '点亮测试设备 T1', workOrderId: 'WO-2026-001',
        level: '高', status: '已上报', reporter: 'operator', reporterName: '王操作',
        assignee: '', assigneeName: '', description: '点亮测试设备间歇性黑屏', createdAt: ts, updatedAt: ts
      }
    ],
    maintenanceRecords: [],
    aftersaleCases: [
      {
        id: 'AS-2026-001', orderId: 'ORD-2026-003', batchNo: 'BATCH-WO001-001', productModel: 'DM-24-LCD-FHD',
        customerName: '京东方显示', feedback: '部分批次出现坏点超标', status: '已创建',
        handler: '', result: '', createdAt: ts, updatedAt: ts
      }
    ],
    costSettlements: [
      {
        id: 'CS-2026-001', workOrderId: 'WO-2026-001', productModel: 'DM-24-LCD-FHD',
        materialCost: 85600, laborCost: 12400, equipmentCost: 3200, qualityCost: 800,
        totalCost: 102000, status: '草稿', createdAt: ts, updatedAt: ts
      }
    ],
    operationLogs: [
      { id: 1, module: '订单管理', action: '创建订单', target: 'ORD-2026-001', operator: '张销售', roleKey: 'order', createdAt: ts },
      { id: 2, module: '生产管理', action: '下达工单', target: 'WO-2026-001', operator: '李主管', roleKey: 'manager', createdAt: ts },
      { id: 3, module: '现场作业', action: '提交报工', target: 'RPT-2026-001', operator: '王操作', roleKey: 'operator', createdAt: ts }
    ],
    processGuide: {
      'DM-24-LCD-FHD': { steps: PROCESS_STEPS, keyPoints: 'LCD 面板需防静电；背光模组扭矩 0.8N·m；点亮测试 30 秒稳定' },
      'DM-27-LCD-FHD': { steps: PROCESS_STEPS, keyPoints: '27寸面板较重，需双人搬运；色彩检测 DeltaE<2' },
      'DM-32-OLED-4K': { steps: PROCESS_STEPS, keyPoints: 'OLED 面板禁止长时间点亮；老化测试 4 小时' }
    }
  }
}

function QC_ITEMS_PLACEHOLDER() {
  return ['外观检查', '点亮测试', '亮度检测', '色彩检测', '坏点检测', '老化测试', '包装检查']
}

export { now }
