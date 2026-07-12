import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMesStore } from '@/stores/mes'
import { MES_LIVE_MODE } from '@/config/mes'
import { BOARD_PATH, getHomePath, MANAGER_ONLY_PATHS } from '@/utils/menuRoutes'
import MainLayout from '@/layouts/MainLayout.vue'

const dashboardRoutes = [
  { path: 'dashboard/admin', name: 'DashboardAdmin', component: () => import('@/views/dashboard/admin/AdminDashboard.vue'), meta: { title: '系统管理工作台', roleKey: 'admin' } },
  { path: 'dashboard/order', name: 'DashboardOrder', component: () => import('@/views/dashboard/order/OrderDashboard.vue'), meta: { title: '订单管理工作台', roleKey: 'order' } },
  { path: 'dashboard/planner', name: 'DashboardPlanner', component: () => import('@/views/dashboard/planner/PlannerDashboard.vue'), meta: { title: '计划员工作台', roleKey: 'planner' } },
  { path: 'dashboard/manager', name: 'DashboardManager', component: () => import('@/views/dashboard/manager/ManagerDashboard.vue'), meta: { title: '生产主管工作台', roleKey: 'manager' } },
  { path: 'dashboard/operator', name: 'DashboardOperator', component: () => import('@/views/dashboard/operator/OperatorDashboard.vue'), meta: { title: '生产操作员工作台', roleKey: 'operator' } },
  { path: 'dashboard/quality', name: 'DashboardQuality', component: () => import('@/views/dashboard/quality/QualityDashboard.vue'), meta: { title: '质检员工作台', roleKey: 'quality' } },
  { path: 'dashboard/warehouse', name: 'DashboardWarehouse', component: () => import('@/views/dashboard/warehouse/WarehouseDashboard.vue'), meta: { title: '仓库管理工作台', roleKey: 'warehouse' } },
  { path: 'dashboard/purchase', name: 'DashboardPurchase', component: () => import('@/views/dashboard/purchase/PurchaseDashboard.vue'), meta: { title: '采购员工作台', roleKey: 'purchase' } },
  { path: 'dashboard/device', name: 'DashboardDevice', component: () => import('@/views/dashboard/device/DeviceDashboard.vue'), meta: { title: '设备维护工作台', roleKey: 'device' } },
  { path: 'dashboard/aftersale', name: 'DashboardAftersale', component: () => import('@/views/dashboard/aftersale/AftersaleDashboard.vue'), meta: { title: '售后人员工作台', roleKey: 'aftersale' } },
  { path: 'dashboard/cost', name: 'DashboardCost', component: () => import('@/views/dashboard/cost/CostDashboard.vue'), meta: { title: '财务/成本工作台', roleKey: 'cost' } },
  { path: 'customer/home', name: 'CustomerHome', component: () => import('@/views/customer/CustomerHomeView.vue'), meta: { title: '客户首页', roleKey: 'customer' } }
]

const businessRoutes = [
  { path: 'system/user', component: () => import('@/views/system/UserView.vue'), meta: { title: '用户管理' } },
  { path: 'system/role', component: () => import('@/views/system/RoleView.vue'), meta: { title: '角色管理' } },
  { path: 'system/permission', component: () => import('@/views/system/PermissionView.vue'), meta: { title: '权限管理' } },
  { path: 'system/menu', component: () => import('@/views/system/MenuView.vue'), meta: { title: '菜单管理' } },
  { path: 'system/log', component: () => import('@/views/system/OperationLogView.vue'), meta: { title: '操作日志' } },
  { path: 'system/board', component: () => import('@/views/system/BoardView.vue'), meta: { title: '生产调度大屏', layout: 'screen', roleKey: 'manager' } },
  { path: 'order/list', component: () => import('@/views/order/OrderListView.vue'), meta: { title: '客户订单' } },
  { path: 'order/audit', component: () => import('@/views/order/OrderAuditView.vue'), meta: { title: '订单审核' } },
  { path: 'order/track', component: () => import('@/views/order/OrderTrackView.vue'), meta: { title: '订单跟踪' } },
  { path: 'production/plan', component: () => import('@/views/production/PlanView.vue'), meta: { title: '生产计划工作台' } },
  { path: 'production/work-order', component: () => import('@/views/production/WorkOrderView.vue'), meta: { title: '生产工单', roleKey: 'manager' } },
  { path: 'production/dispatch', component: () => import('@/views/production/DispatchView.vue'), meta: { title: '工单派工', roleKey: 'manager' } },
  { path: 'production/my-dispatch', component: () => import('@/views/production/MyDispatchView.vue'), meta: { title: '我的派工' } },
  { path: 'production/report', component: () => import('@/views/production/ReportView.vue'), meta: { title: '工序报工' } },
  { path: 'production/progress', component: () => import('@/views/production/ProgressView.vue'), meta: { title: '生产进度' } },
  { path: 'production/exception', component: () => import('@/views/production/ExceptionView.vue'), meta: { title: '生产异常' } },
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
  { path: 'warehouse/inventory', component: () => import('@/views/warehouse/InventoryView.vue'), meta: { title: '库存查询' } },
  { path: 'warehouse/inbound', component: () => import('@/views/warehouse/InboundView.vue'), meta: { title: '成品入库' } },
  { path: 'warehouse/purchase-in', component: () => import('@/views/warehouse/PurchaseInView.vue'), meta: { title: '采购入库' } },
  { path: 'warehouse/issue', component: () => import('@/views/warehouse/IssueView.vue'), meta: { title: '生产领料' } },
  { path: 'warehouse/flow', component: () => import('@/views/warehouse/FlowView.vue'), meta: { title: '库存流水' } },
  { path: 'warehouse/alert', component: () => import('@/views/warehouse/AlertView.vue'), meta: { title: '库存预警' } },
  { path: 'purchase/demand', component: () => import('@/views/purchase/DemandView.vue'), meta: { title: '采购需求' } },
  { path: 'purchase/order', component: () => import('@/views/purchase/PurchaseOrderView.vue'), meta: { title: '采购订单' } },
  { path: 'purchase/supplier', component: () => import('@/views/purchase/SupplierView.vue'), meta: { title: '供应商管理' } },
  { path: 'purchase/arrival', redirect: '/purchase/order' },
  { path: 'purchase/ai-document', component: () => import('@/views/purchase/AiDocumentView.vue'), meta: { title: 'AI 单据录入' } },
  { path: 'device/equipment', component: () => import('@/views/device/EquipmentView.vue'), meta: { title: '设备台账' } },
  { path: 'device/status', component: () => import('@/views/device/StatusView.vue'), meta: { title: '设备状态' } },
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
  { path: 'customer/feedback/submit', component: () => import('@/views/customer/CustomerFeedbackSubmitView.vue'), meta: { title: '提交反馈', roleKey: 'customer' } },
  { path: 'customer/feedback/list', component: () => import('@/views/customer/CustomerFeedbackListView.vue'), meta: { title: '我的反馈', roleKey: 'customer' } },
  { path: 'customer/profile', component: () => import('@/views/customer/CustomerProfileView.vue'), meta: { title: '个人中心', roleKey: 'customer' } }
]

const routes = [
  { path: '/', name: 'Home', component: () => import('@/views/home/HomeView.vue'), meta: { public: true } },
  { path: '/login', name: 'Login', component: () => import('@/views/login/LoginView.vue'), meta: { public: true } },
  { path: '/register', name: 'Register', component: () => import('@/views/login/RegisterView.vue'), meta: { public: true } },
  { path: '/', component: MainLayout, children: [...dashboardRoutes, ...businessRoutes] }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const home = () => userStore.dashboardPath || getHomePath(userStore.roleKey)

  if (to.meta.public) {
    if (userStore.isLoggedIn && (to.path === '/login' || to.path === '/register')) next(home())
    else next()
    return
  }
  if (!userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (!userStore.menus.length || !userStore.menus.some((m) => m.children?.some((c) => c.path))) {
    await userStore.loadMenus()
  }
  if (MES_LIVE_MODE) {
    const mesStore = useMesStore()
    if (!mesStore.hydrated) {
      try {
        await mesStore.hydrateFromApi()
      } catch {
        /* 不阻断页面进入 */
      }
    } else if (to.path !== from.path && !to.meta.public && to.path !== BOARD_PATH) {
      mesStore.hydrateFromApi().catch(() => {})
    }
  }
  if (MANAGER_ONLY_PATHS.has(to.path) && userStore.roleKey !== 'manager') {
    next(home())
    return
  }
  if (to.meta.roleKey && to.meta.roleKey !== userStore.roleKey) {
    next(home())
    return
  }
  next()
})

export default router
