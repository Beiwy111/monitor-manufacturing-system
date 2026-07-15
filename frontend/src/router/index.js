import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMesStore } from '@/stores/mes'
import { MES_LIVE_MODE } from '@/config/mes'
import { BOARD_PATH, getHomePath, MANAGER_ONLY_PATHS } from '@/utils/menuRoutes'
import MainLayout from '@/layouts/MainLayout.vue'

const dashboardRoutes = [
  { path: 'chat', name: 'ChatHome', component: () => import('@/views/chat/ChatHomeView.vue'), meta: { title: '智能对话' } },
  { path: 'dashboard/admin', name: 'DashboardAdmin', component: () => import('@/views/dashboard/admin/AdminDashboard.vue'), meta: { title: '系统管理工作台', roleKey: 'admin' } },
  { path: 'dashboard/order', name: 'DashboardOrder', component: () => import('@/views/dashboard/order/OrderDashboard.vue'), meta: { title: '订单管理工作台', roleKey: 'order' } },
  { path: 'dashboard/planner', name: 'DashboardPlanner', component: () => import('@/views/dashboard/planner/PlannerDashboard.vue'), meta: { title: '计划员工作台', roleKey: 'planner' } },
  { path: 'dashboard/manager', redirect: BOARD_PATH },
  { path: 'dashboard/operator', name: 'DashboardOperator', component: () => import('@/views/dashboard/operator/OperatorDashboard.vue'), meta: { title: '生产操作员工作台', roleKey: 'operator' } },
  { path: 'dashboard/quality', name: 'DashboardQuality', component: () => import('@/views/dashboard/quality/QualityDashboard.vue'), meta: { title: '质检员工作台', roleKey: 'quality' } },
  { path: 'dashboard/warehouse', name: 'DashboardWarehouse', component: () => import('@/views/dashboard/warehouse/WarehouseDashboard.vue'), meta: { title: '仓库管理工作台', roleKey: 'warehouse' } },
  { path: 'dashboard/purchase', name: 'DashboardPurchase', component: () => import('@/views/dashboard/purchase/PurchaseDashboard.vue'), meta: { title: '采购员工作台', roleKey: 'purchase' } },
  { path: 'dashboard/device', name: 'DashboardDevice', component: () => import('@/views/dashboard/device/DeviceDashboard.vue'), meta: { title: '设备维护工作台', roleKey: 'device' } },
  { path: 'dashboard/aftersale', name: 'DashboardAftersale', component: () => import('@/views/dashboard/aftersale/AftersaleDashboard.vue'), meta: { title: '售后人员工作台', roleKey: 'aftersale' } },
  { path: 'dashboard/cost', name: 'DashboardCost', component: () => import('@/views/dashboard/cost/CostDashboard.vue'), meta: { title: '财务/成本工作台', roleKey: 'cost' } },
  { path: 'customer/home', name: 'CustomerHome', component: () => import('@/views/customer/CustomerHomeView.vue'), meta: { title: '产品中心', roleKey: 'customer' } }
]

const businessRoutes = [
  { path: 'system/user', component: () => import('@/views/system/UserView.vue'), meta: { title: '用户管理' } },
  { path: 'system/role', component: () => import('@/views/system/RoleView.vue'), meta: { title: '角色管理' } },
  { path: 'system/permission', component: () => import('@/views/system/PermissionView.vue'), meta: { title: '权限管理' } },
  { path: 'system/menu', component: () => import('@/views/system/MenuView.vue'), meta: { title: '菜单管理' } },
  { path: 'system/log', component: () => import('@/views/system/OperationLogView.vue'), meta: { title: '操作日志' } },
  { path: 'attendance/record', component: () => import('@/views/attendance/AttendanceRecordView.vue'), meta: { title: '考勤记录', roleKey: 'admin' } },
  { path: 'attendance/statistics', component: () => import('@/views/attendance/AttendanceStatisticsView.vue'), meta: { title: '考勤统计', roleKey: 'admin' } },
  { path: 'system/board', component: () => import('@/views/system/BoardView.vue'), meta: { title: '生产调度大屏', layout: 'screen', roleKey: 'manager' } },
  { path: 'order/list', component: () => import('@/views/order/OrderListView.vue'), meta: { title: '客户订单' } },
  { path: 'order/ai-screenshot', component: () => import('@/views/order/OrderAiScreenshotView.vue'), meta: { title: 'AI识图下单' } },
  { path: 'order/audit', component: () => import('@/views/order/OrderAuditView.vue'), meta: { title: '订单审核' } },
  { path: 'order/track', component: () => import('@/views/order/OrderTrackView.vue'), meta: { title: '订单跟踪' } },
  { path: 'production/plan', component: () => import('@/views/production/PlanView.vue'), meta: { title: '生产计划工作台' } },
  { path: 'production/smart-scheduling', component: () => import('@/views/production/PlannerSmartSchedulingView.vue'), meta: { title: '智能排产', layout: 'screen' } },
  { path: 'production/work-order', component: () => import('@/views/production/WorkOrderView.vue'), meta: { title: '生产工单', roleKey: 'manager' } },
  { path: 'production/dispatch', component: () => import('@/views/production/DispatchView.vue'), meta: { title: '工单派工', roleKey: 'manager' } },
  { path: 'production/my-dispatch', component: () => import('@/views/production/MyDispatchView.vue'), meta: { title: '我的派工' } },
  { path: 'production/report', component: () => import('@/views/production/ReportView.vue'), meta: { title: '工序报工' } },
  { path: 'production/progress', component: () => import('@/views/production/ProgressView.vue'), meta: { title: '生产进度' } },
  { path: 'production/exception', component: () => import('@/views/production/ExceptionView.vue'), meta: { title: '生产异常' } },
  { path: 'production/shift-calendar', component: () => import('@/views/production/ShiftCalendarView.vue'), meta: { title: '排班日历', roleKey: 'manager' } },
  { path: 'production/process-setup', component: () => import('@/views/production/ProcessSetupView.vue'), meta: { title: '工序设置', roleKey: 'planner' } },
  { path: 'production/process-guide', component: () => import('@/views/production/ProcessGuideView.vue'), meta: { title: '工艺说明' } },
  { path: 'report/production-progress', component: () => import('@/views/report/ProductionProgressReportView.vue'), meta: { title: '生产制令单进度表', roleKey: 'operator' } },
  { path: 'quality/inspection', component: () => import('@/views/quality/InspectionView.vue'), meta: { title: '待检任务' } },
  { path: 'quality/defect', redirect: '/quality/material/defect' },
  { path: 'quality/reinspect', redirect: '/quality/material/reinspect' },
  { path: 'quality/records', redirect: '/quality/fp/records' },
  { path: 'quality/trace', redirect: '/quality/fp/trace' },
  { path: 'quality/print', redirect: '/quality/fp/print' },
  { path: 'quality/material/inspection', component: () => import('@/views/quality/InspectionView.vue'), meta: { title: '物料待检任务', category: 'RAW_MATERIAL' } },
  { path: 'quality/material/defect', component: () => import('@/views/quality/DefectView.vue'), meta: { title: '物料不合格品', category: 'RAW_MATERIAL' } },
  { path: 'quality/material/reinspect', component: () => import('@/views/quality/ReinspectView.vue'), meta: { title: '物料复检处理', category: 'RAW_MATERIAL' } },
  { path: 'quality/material/records', component: () => import('@/views/quality/RecordsView.vue'), meta: { title: '物料质检记录', category: 'RAW_MATERIAL' } },
  { path: 'quality/material/trace', component: () => import('@/views/quality/TraceView.vue'), meta: { title: '物料质量追溯', category: 'RAW_MATERIAL' } },
  { path: 'quality/material/print', component: () => import('@/views/quality/PrintView.vue'), meta: { title: '物料报表打印', category: 'RAW_MATERIAL' } },
  { path: 'quality/semi/inspection', redirect: '/quality/material/inspection' },
  { path: 'quality/semi/defect', redirect: '/quality/material/defect' },
  { path: 'quality/semi/reinspect', redirect: '/quality/material/reinspect' },
  { path: 'quality/semi/records', redirect: '/quality/material/records' },
  { path: 'quality/semi/trace', redirect: '/quality/material/trace' },
  { path: 'quality/semi/print', redirect: '/quality/material/print' },
  { path: 'quality/fp/inspection', component: () => import('@/views/quality/InspectionView.vue'), meta: { title: '成品待检任务', category: 'FINISHED_PRODUCT', mode: 'finished' } },
  { path: 'quality/fp/defect', component: () => import('@/views/quality/DefectView.vue'), meta: { title: '成品不合格品', category: 'FINISHED_PRODUCT' } },
  { path: 'quality/fp/reinspect', component: () => import('@/views/quality/ReinspectView.vue'), meta: { title: '成品复检处理', category: 'FINISHED_PRODUCT' } },
  { path: 'quality/fp/records', component: () => import('@/views/quality/RecordsView.vue'), meta: { title: '成品质检记录', category: 'FINISHED_PRODUCT' } },
  { path: 'quality/fp/trace', component: () => import('@/views/quality/TraceView.vue'), meta: { title: '成品质量追溯', category: 'FINISHED_PRODUCT' } },
  { path: 'quality/fp/print', component: () => import('@/views/quality/PrintView.vue'), meta: { title: '成品报表打印', category: 'FINISHED_PRODUCT' } },
  { path: 'warehouse/inbound-hub', component: () => import('@/views/warehouse/WarehouseInboundHub.vue'), meta: { title: '入库' } },
  { path: 'warehouse/outbound-hub', component: () => import('@/views/warehouse/WarehouseOutboundHub.vue'), meta: { title: '出库' } },
  { path: 'warehouse/capacity', component: () => import('@/views/warehouse/InventoryView.vue'), meta: { title: '库存容量查询' } },
  { path: 'warehouse/location-map', component: () => import('@/views/warehouse/LocationMapView.vue'), meta: { title: '库位图' } },
  { path: 'warehouse/inventory', redirect: '/warehouse/capacity' },
  { path: 'warehouse/inbound', redirect: (to) => ({ path: '/warehouse/inbound-hub', query: { tab: 'finished', ...to.query } }) },
  { path: 'warehouse/purchase-in', redirect: (to) => ({ path: '/warehouse/inbound-hub', query: { tab: 'purchase', ...to.query } }) },
  { path: 'warehouse/issue', redirect: (to) => ({ path: '/warehouse/outbound-hub', query: { tab: 'issue', ...to.query } }) },
  { path: 'warehouse/flow', redirect: (to) => ({ path: '/warehouse/capacity', query: { tab: 'flow', ...to.query } }) },
  { path: 'warehouse/alert', redirect: '/warehouse/location-map' },
  { path: 'purchase/demand', component: () => import('@/views/purchase/DemandView.vue'), meta: { title: '采购需求' } },
  { path: 'purchase/workbench', component: () => import('@/views/purchase/PurchaseWorkbenchView.vue'), meta: { title: '采购工作台', roleKey: 'purchase' } },
  { path: 'purchase/order', component: () => import('@/views/purchase/PurchaseOrderView.vue'), meta: { title: '采购订单' } },
  { path: 'purchase/supplier', component: () => import('@/views/purchase/SupplierView.vue'), meta: { title: '供应商管理' } },
  { path: 'purchase/arrival', redirect: '/purchase/order' },
  { path: 'purchase/ai-document', redirect: '/order/ai-screenshot' },
  { path: 'device/equipment', component: () => import('@/views/device/EquipmentView.vue'), meta: { title: '设备台账' } },
  { path: 'device/status', redirect: '/device/equipment' },
  { path: 'device/alarm', component: () => import('@/views/device/AlarmView.vue'), meta: { title: '安灯报警' } },
  { path: 'device/maintenance', component: () => import('@/views/device/EquipmentMaintenanceView.vue'), meta: { title: '维修处理' } },
  { path: 'device/records', component: () => import('@/views/device/RecordsView.vue'), meta: { title: '维护记录' } },
  { path: 'delivery/list', component: () => import('@/views/delivery/DeliveryView.vue'), meta: { title: '发货管理' } },
  { path: 'aftersale/case', component: () => import('@/views/aftersale/CaseView.vue'), meta: { title: '售后登记' } },
  { path: 'aftersale/feedback', component: () => import('@/views/aftersale/FeedbackView.vue'), meta: { title: '客户反馈' } },
  { path: 'aftersale/process', component: () => import('@/views/aftersale/ProcessView.vue'), meta: { title: '售后处理' } },
  { path: 'aftersale/trace', component: () => import('@/views/aftersale/TraceView.vue'), meta: { title: '质量追溯' } },
  { path: 'cost/work-order', component: () => import('@/views/cost/WorkOrderCostView.vue'), meta: { title: '工单成本' } },
  { path: 'cost/material', component: () => import('@/views/cost/MaterialCostView.vue'), meta: { title: '材料成本' } },
  { path: 'cost/labor', component: () => import('@/views/cost/LaborCostView.vue'), meta: { title: '人工成本' } },
  { path: 'cost/equipment', component: () => import('@/views/cost/EquipmentCostView.vue'), meta: { title: '设备成本' } },
  { path: 'cost/settlement', component: () => import('@/views/cost/SettlementView.vue'), meta: { title: '成本结算' } },
  { path: 'cost/report', component: () => import('@/views/cost/ReportView.vue'), meta: { title: '成本报表' } },
  { path: 'customer/order/new', component: () => import('@/views/customer/CustomerNewOrderView.vue'), meta: { title: '新建订单', roleKey: 'customer' } },
  { path: 'customer/orders', component: () => import('@/views/customer/CustomerOrdersView.vue'), meta: { title: '我的订单', roleKey: 'customer' } },
  { path: 'customer/products', component: () => import('@/views/customer/CustomerProductsView.vue'), meta: { title: '产品与规格', roleKey: 'customer' } },
  {
    path: 'customer/products/:materialId',
    redirect: (to) => ({
      path: '/customer/order/new',
      query: { materialId: to.params.materialId }
    })
  },
  { path: 'customer/feedback/submit', component: () => import('@/views/customer/CustomerFeedbackSubmitView.vue'), meta: { title: '提交反馈', roleKey: 'customer' } },
  { path: 'customer/feedback/list', component: () => import('@/views/customer/CustomerFeedbackListView.vue'), meta: { title: '我的反馈', roleKey: 'customer' } },
  { path: 'customer/profile', component: () => import('@/views/customer/CustomerProfileView.vue'), meta: { title: '个人中心', roleKey: 'customer' } }
]

const routes = [
  { path: '/', name: 'Home', component: () => import('@/views/home/HomeView.vue'), meta: { public: true } },
  { path: '/products/:materialId', name: 'HomeProductDetail', component: () => import('@/views/home/HomeProductDetailView.vue'), meta: { public: true, title: '产品详情' } },
  { path: '/login', name: 'Login', component: () => import('@/views/login/LoginView.vue'), meta: { public: true } },
  { path: '/register', name: 'Register', component: () => import('@/views/login/RegisterView.vue'), meta: { public: true } },
  { path: '/', component: MainLayout, children: [...dashboardRoutes, ...businessRoutes] }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const home = () => userStore.dashboardPath || getHomePath(userStore.roleKey)

  if (to.meta.public) {
    if (userStore.isLoggedIn && (to.path === '/login' || to.path === '/register')) {
      const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : ''
      next(redirect || home())
      return
    }
    next()
    return
  }
  if (!userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (!userStore.menus.length) {
    await userStore.loadMenus()
  }
  if (MES_LIVE_MODE && to.path !== '/chat' && to.meta.layout !== 'screen') {
    useMesStore().hydrateForPage().catch(() => {})
  }
  if (MANAGER_ONLY_PATHS.has(to.path) && userStore.roleKey !== 'manager' && userStore.roleKey !== 'admin') {
    next(home())
    return
  }
  if (to.meta.roleKey && to.meta.roleKey !== userStore.roleKey && userStore.roleKey !== 'admin') {
    next(home())
    return
  }
  next()
})

export default router
