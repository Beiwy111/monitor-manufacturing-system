import { defineStore } from 'pinia'
import { createInitialMesData, now } from '@/mock/mesData'

export const MES_STORAGE_KEY = 'mes-store-data'

let idSeq = 1000
const nextId = (prefix) => `${prefix}-${++idSeq}`

function loadMesState() {
  try {
    const raw = localStorage.getItem(MES_STORAGE_KEY)
    if (raw) return JSON.parse(raw)
  } catch {
    /* ignore */
  }
  return createInitialMesData()
}

function resolveOperator(store, payload) {
  if (payload.operator) {
    const user = store.sysUsers.find((u) => u.username === payload.operator)
    return {
      operator: payload.operator,
      operatorName: user?.realName || payload.operatorName || payload.operator
    }
  }
  if (payload.operatorName) {
    const user = store.sysUsers.find(
      (u) => u.realName === payload.operatorName || u.username === payload.operatorName
    )
    if (user) return { operator: user.username, operatorName: user.realName }
  }
  const fallback = store.sysUsers.find((u) => u.roleKey === 'operator')
  return { operator: fallback?.username || 'operator', operatorName: fallback?.realName || '王操作' }
}

function log(store, module, action, target, operator, roleKey) {
  store.operationLogs.unshift({
    id: ++idSeq,
    module,
    action,
    target,
    operator: operator || '系统',
    roleKey: roleKey || 'system',
    createdAt: now()
  })
  if (store.operationLogs.length > 200) store.operationLogs.length = 200
}

function reportQualifiedQty(store, dispatchId) {
  return store.workReports
    .filter((r) => r.dispatchId === dispatchId && r.status !== '已驳回')
    .reduce((sum, r) => sum + (r.qualifiedQty || 0), 0)
}

function syncWorkOrderStatus(store, workOrderId) {
  const wo = store.workOrders.find((w) => w.id === workOrderId)
  if (!wo) return
  const related = store.dispatches.filter((d) => d.workOrderId === workOrderId)
  if (!related.length) return
  const allDone = related.every((d) => d.status === '已完成')
  const anyPendingQc = related.some((d) => d.status === '待质检')
  const anyActive = related.some((d) => ['已分配', '已接收', '生产中'].includes(d.status))
  if (allDone) {
    wo.status = '已完成'
  } else if (anyPendingQc) {
    wo.status = '待质检'
  } else if (anyActive) {
    wo.status = '生产中'
  }
  wo.updatedAt = now()
}

export const useMesStore = defineStore('mes', {
  state: () => ({
    ...loadMesState(),
    selectedId: null
  }),

  getters: {
    pendingOrders: (s) => s.orders.filter((o) => o.status === '待审核'),
    approvedOrders: (s) => s.orders.filter((o) => o.status === '已审核'),
    pendingPlanOrders: (s) => s.orders.filter((o) => o.status === '已审核'),
    pendingReleaseWorkOrders: (s) => s.workOrders.filter((w) => w.status === '草稿'),
    pendingDispatchWorkOrders: (s) => s.workOrders.filter((w) => w.status === '已下达'),
    operatorUsers: (s) => s.sysUsers.filter((u) => u.roleKey === 'operator' && u.status === '启用'),
    pendingInspections: (s) => s.inspections.filter((i) => i.status === '待检'),
    pendingInbound: (s) => s.inboundTasks.filter((t) => t.status === '待入库'),
    openAlarms: (s) => s.alarms.filter((a) => a.status !== '已关闭'),
    myDispatches: (s) => (username) => s.dispatches.filter((d) => d.operator === username),
    stats: (s) => ({
      orderCount: s.orders.length,
      planCount: s.plans.length,
      workOrderCount: s.workOrders.length,
      pendingQc: s.inspections.filter((i) => i.status === '待检').length,
      openAlarm: s.alarms.filter((a) => a.status !== '已关闭').length,
      stockAlert: s.inventory.filter((i) => i.quantity < i.safeQty).length,
      onlineUsers: s.sysUsers.filter((u) => u.status === '启用').length,
      roleCount: s.sysRoles.length,
      menuCount: s.sysMenus.length,
      todayLogs: s.operationLogs.length
    }),
    todosForRole: (s) => (roleKey, username) => {
      const todos = []
      if (roleKey === 'admin') {
        s.sysUsers.filter((u) => u.status === '禁用').forEach((u) => todos.push({ type: '用户', title: `用户 ${u.username} 已禁用`, ref: u.username, path: '/system/user' }))
        s.alarms.filter((a) => a.status === '已上报').forEach((a) => todos.push({ type: '安灯', title: a.description, ref: a.id, path: '/device/alarm' }))
      }
      if (roleKey === 'order') {
        s.orders.filter((o) => o.status === '待审核').forEach((o) => todos.push({ type: '订单', title: `${o.id} 待审核`, ref: o.id, path: '/order/list' }))
      }
      if (roleKey === 'manager') {
        s.orders.filter((o) => o.status === '已审核').forEach((o) => todos.push({ type: '订单', title: `${o.id} 待创建计划`, ref: o.id, path: `/production/plan?orderId=${o.id}` }))
        s.plans.filter((p) => p.status === '草稿').forEach((p) => todos.push({ type: '计划', title: `${p.id} 待发布`, ref: p.id, path: '/production/plan' }))
        s.workOrders.filter((w) => w.status === '草稿').forEach((w) => todos.push({ type: '工单', title: `${w.id} 待下达`, ref: w.id, path: '/production/work-order' }))
        s.workOrders.filter((w) => w.status === '已下达').forEach((w) => todos.push({ type: '工单', title: `${w.id} 待派工`, ref: w.id, path: `/production/dispatch?workOrderId=${w.id}` }))
        s.alarms.filter((a) => ['已上报', '已接收'].includes(a.status)).forEach((a) => todos.push({ type: '安灯', title: a.description, ref: a.id, path: '/device/alarm' }))
      }
      if (roleKey === 'operator') {
        s.dispatches.filter((d) => d.operator === username && d.status === '已分配').forEach((d) => todos.push({ type: '派工', title: `${d.processStep} 待接收`, ref: d.id, path: '/production/my-dispatch' }))
        s.dispatches.filter((d) => d.operator === username && d.status === '生产中' && d.completedQty >= d.planQty).forEach((d) => todos.push({ type: '质检', title: `${d.processStep} 待提交质检`, ref: d.id, path: '/production/report' }))
        s.dispatches.filter((d) => d.operator === username && ['已接收', '生产中'].includes(d.status) && d.completedQty < d.planQty).forEach((d) => todos.push({ type: '派工', title: `${d.processStep} 进行中`, ref: d.id, path: '/production/report' }))
        s.dispatches.filter((d) => d.operator === username && d.processStep === '返修' && d.status === '已分配').forEach((d) => todos.push({ type: '返修', title: `返修任务 ${d.planQty} 台`, ref: d.id, path: '/production/my-dispatch' }))
      }
      if (roleKey === 'quality') {
        s.inspections.filter((i) => i.status === '待检').forEach((i) => todos.push({ type: '质检', title: `${i.id} 待检验`, ref: i.id, path: '/quality/inspection' }))
        s.defects.filter((d) => d.status === '待处理').forEach((d) => todos.push({ type: '不合格', title: `${d.defectLocation || '未知部位'} · ${d.severity || ''}`, ref: d.id, path: '/quality/defect' }))
      }
      if (roleKey === 'purchase') {
        s.purchaseDemands.filter((d) => d.status === '待采购').forEach((d) => todos.push({ type: '采购', title: `${d.materialName} 缺口 ${d.gapQty}`, ref: d.id, path: '/purchase/demand' }))
      }
      if (roleKey === 'warehouse') {
        s.inboundTasks.filter((t) => t.status === '待入库').forEach((t) => todos.push({ type: '入库', title: `${t.productModel} ${t.quantity}台`, ref: t.id, path: '/warehouse/inbound' }))
        s.issueTasks.filter((t) => t.status !== '已完成').forEach((t) => todos.push({ type: '领料', title: `${t.materialName}`, ref: t.id, path: '/warehouse/issue' }))
        s.deliveries.filter((d) => d.status === '待出库').forEach((d) => todos.push({ type: '发货', title: d.id, ref: d.id, path: '/delivery/list' }))
        s.inventory.filter((i) => i.quantity < i.safeQty).forEach((i) => todos.push({ type: '预警', title: `${i.materialName} 低于安全库存`, ref: i.id, path: '/warehouse/alert' }))
      }
      if (roleKey === 'device') {
        s.alarms.filter((a) => ['已上报', '已接收'].includes(a.status)).forEach((a) => todos.push({ type: '安灯', title: a.description, ref: a.id, path: '/device/alarm' }))
        s.equipment.filter((e) => e.status === '故障').forEach((e) => todos.push({ type: '设备', title: `${e.name} 故障`, ref: e.id, path: '/device/equipment' }))
      }
      if (roleKey === 'aftersale') {
        s.aftersaleCases.filter((c) => c.status !== '已关闭').forEach((c) => todos.push({ type: '售后', title: c.feedback.slice(0, 20), ref: c.id, path: '/aftersale/case' }))
      }
      if (roleKey === 'cost') {
        s.costSettlements.filter((c) => c.status === '草稿').forEach((c) => todos.push({ type: '成本', title: `${c.id} 待确认`, ref: c.id, path: '/cost/settlement' }))
      }
      return todos
    },
    traceChain: (s) => (orderId) => {
      const order = s.orders.find((o) => o.id === orderId)
      if (!order) return null
      const plan = s.plans.find((p) => p.orderId === orderId)
      const wo = s.workOrders.find((w) => w.orderId === orderId)
      const dispatches = s.dispatches.filter((d) => d.workOrderId === wo?.id)
      const reports = s.workReports.filter((r) => r.workOrderId === wo?.id)
      const inspections = s.inspections.filter((i) => i.workOrderId === wo?.id)
      const defects = s.defects.filter((d) => d.workOrderId === wo?.id)
      const deliveries = s.deliveries.filter((d) => d.orderId === orderId)
      const aftersale = s.aftersaleCases.filter((c) => c.orderId === orderId)
      return { order, plan, wo, dispatches, reports, inspections, defects, deliveries, aftersale }
    }
  },

  actions: {
    setSelected(id) { this.selectedId = id },

    addLog(module, action, target, operator, roleKey) {
      log(this, module, action, target, operator, roleKey)
    },

    // —— 订单 ——
    createOrder(payload, operator, roleKey) {
      const id = nextId('ORD-2026')
      const customer = this.customers.find((c) => c.id === payload.customerId)
      const order = {
        id, customerId: payload.customerId, customerName: customer?.name || payload.customerName,
        productModel: payload.productModel, panelType: payload.panelType || 'LCD',
        quantity: payload.quantity, deliveryDate: payload.deliveryDate, status: '待审核',
        amount: payload.amount || 0, salesPerson: operator, remark: payload.remark || '',
        createdAt: now(), updatedAt: now()
      }
      this.orders.unshift(order)
      log(this, '订单管理', '创建订单', id, operator, roleKey)
      return order
    },
    auditOrder(orderId, pass, operator, roleKey) {
      const o = this.orders.find((x) => x.id === orderId)
      if (!o || o.status !== '待审核') return false
      o.status = pass ? '已审核' : '已作废'
      o.updatedAt = now()
      log(this, '订单管理', pass ? '审核通过' : '审核驳回', orderId, operator, roleKey)
      return true
    },
    submitOrder(orderId, operator, roleKey) {
      const o = this.orders.find((x) => x.id === orderId)
      if (!o) return false
      o.status = '待审核'
      o.updatedAt = now()
      log(this, '订单管理', '提交审核', orderId, operator, roleKey)
      return true
    },

    // —— 生产计划 ——
    createPlan(payload, operator, roleKey) {
      const order = this.orders.find((o) => o.id === payload.orderId)
      if (!order || order.status !== '已审核') return null
      const id = nextId('PLAN-2026')
      const plan = {
        id, orderId: order.id, orderNo: order.id, productModel: order.productModel,
        quantity: order.quantity, planStart: payload.planStart, planEnd: payload.planEnd,
        status: '草稿', manager: operator, remark: payload.remark || '',
        createdAt: now(), updatedAt: now()
      }
      this.plans.unshift(plan)
      order.status = '已计划'
      order.planId = id
      order.updatedAt = now()
      log(this, '生产管理', '创建生产计划', id, operator, roleKey)
      return plan
    },
    publishPlan(planId, operator, roleKey) {
      const p = this.plans.find((x) => x.id === planId)
      if (!p || p.status !== '草稿') return false
      p.status = '已发布'
      p.updatedAt = now()
      log(this, '生产管理', '发布生产计划', planId, operator, roleKey)
      return true
    },

    // —— 工单 ——
    createWorkOrder(planId, operator, roleKey) {
      const plan = this.plans.find((p) => p.id === planId)
      if (!plan || !['已发布', '执行中'].includes(plan.status)) return null
      const id = nextId('WO-2026')
      const wo = {
        id, planId: plan.id, orderId: plan.orderId, orderNo: plan.orderNo,
        productModel: plan.productModel, quantity: plan.quantity, completedQty: 0, qualifiedQty: 0,
        status: '草稿', line: '装配线 A', manager: operator,
        createdAt: now(), updatedAt: now()
      }
      this.workOrders.unshift(wo)
      plan.status = '执行中'
      plan.updatedAt = now()
      const order = this.orders.find((o) => o.id === plan.orderId)
      if (order) { order.status = '生产中'; order.workOrderId = id; order.updatedAt = now() }
      log(this, '生产管理', '创建生产工单', id, operator, roleKey)
      return wo
    },
    releaseWorkOrder(woId, operator, roleKey) {
      const wo = this.workOrders.find((w) => w.id === woId)
      if (!wo || wo.status !== '草稿') return false
      wo.status = '已下达'
      wo.updatedAt = now()
      if (!this.issueTasks.some((t) => t.workOrderId === woId)) {
        this.issueTasks.unshift({
          id: nextId('IS'), workOrderId: woId, materialCode: 'BL-MODULE', materialName: '背光模组',
          requiredQty: wo.quantity, issuedQty: 0, status: '待领料', createdAt: now()
        })
      }
      log(this, '生产管理', '下达工单', woId, operator, roleKey)
      return true
    },

    // —— 派工 ——
    createDispatch(payload, operator, roleKey) {
      const wo = this.workOrders.find((w) => w.id === payload.workOrderId)
      if (!wo || !['已下达', '已派工', '生产中'].includes(wo.status)) return null
      const { operator: opUser, operatorName } = resolveOperator(this, payload)
      const id = nextId('DIS-2026')
      const d = {
        id, workOrderId: wo.id, workOrderNo: wo.id, processStep: payload.processStep,
        equipment: payload.equipment, operator: opUser, operatorName,
        planQty: payload.planQty, completedQty: 0, status: '已分配',
        planStart: payload.planStart, planEnd: payload.planEnd,
        createdAt: now(), updatedAt: now()
      }
      this.dispatches.unshift(d)
      wo.status = wo.status === '已下达' ? '已派工' : wo.status
      wo.updatedAt = now()
      log(this, '生产管理', `派工给 ${operatorName}`, id, operator, roleKey)
      return d
    },
    acceptDispatch(dispatchId, operator, roleKey) {
      const d = this.dispatches.find((x) => x.id === dispatchId)
      if (!d || d.status !== '已分配' || d.operator !== operator) return false
      d.status = '已接收'
      d.updatedAt = now()
      log(this, '现场作业', '接收派工', dispatchId, operator, roleKey)
      return true
    },
    startDispatch(dispatchId, operator, roleKey) {
      const d = this.dispatches.find((x) => x.id === dispatchId)
      if (!d || d.operator !== operator || d.status !== '已接收') return false
      d.status = '生产中'
      d.updatedAt = now()
      const wo = this.workOrders.find((w) => w.id === d.workOrderId)
      if (wo) { wo.status = '生产中'; wo.updatedAt = now() }
      log(this, '现场作业', '开始生产', dispatchId, operator, roleKey)
      return true
    },

    // —— 报工 ——
    submitReport(payload, operator, roleKey) {
      const d = this.dispatches.find((x) => x.id === payload.dispatchId)
      if (!d || !['已接收', '生产中'].includes(d.status)) return null
      const id = nextId('RPT-2026')
      const rpt = {
        id, dispatchId: d.id, workOrderId: d.workOrderId, processStep: d.processStep,
        operator, operatorName: payload.operatorName, reportQty: payload.reportQty,
        qualifiedQty: payload.qualifiedQty, unqualifiedQty: payload.unqualifiedQty || 0,
        workHours: payload.workHours, status: '已提交', remark: payload.remark || '',
        createdAt: now()
      }
      this.workReports.unshift(rpt)
      d.completedQty += payload.reportQty
      if (d.status === '已接收') d.status = '生产中'
      d.updatedAt = now()
      const wo = this.workOrders.find((w) => w.id === d.workOrderId)
      if (wo) {
        wo.completedQty += payload.reportQty
        wo.updatedAt = now()
        if (wo.status !== '待质检') wo.status = '生产中'
      }
      log(this, '现场作业', '提交报工', id, operator, roleKey)
      return rpt
    },
    submitToInspection(dispatchId, operator, roleKey) {
      const d = this.dispatches.find((x) => x.id === dispatchId)
      if (!d || d.status !== '生产中' || d.completedQty < d.planQty) return null
      if (this.inspections.some((i) => i.dispatchId === dispatchId && i.status === '待检')) return null
      const qualifiedQty = reportQualifiedQty(this, dispatchId)
      if (qualifiedQty <= 0) return null
      const wo = this.workOrders.find((w) => w.id === d.workOrderId)
      const isRework = d.processStep === '返修' || !!d.defectId
      const qcId = nextId('QC-2026')
      this.inspections.unshift({
        id: qcId,
        reportId: null,
        dispatchId: d.id,
        defectId: d.defectId || '',
        workOrderId: d.workOrderId,
        batchNo: `BATCH-${d.workOrderId}-${Date.now().toString().slice(-4)}`,
        productModel: wo?.productModel || '',
        qcType: isRework ? '复检' : '终检',
        qcItems: ['外观检查', '点亮测试', '坏点检测'],
        submitQty: qualifiedQty,
        sampleQty: Math.min(10, qualifiedQty),
        qualifiedQty: 0,
        unqualifiedQty: 0,
        result: '',
        status: '待检',
        operatorName: d.operatorName,
        inspector: '',
        inspectorName: '',
        remark: '',
        createdAt: now(),
        updatedAt: now()
      })
      d.status = '待质检'
      d.updatedAt = now()
      if (wo) {
        wo.status = '待质检'
        wo.updatedAt = now()
      }
      log(this, '现场作业', `提交质检 ${qualifiedQty} 台`, dispatchId, operator, roleKey)
      return qcId
    },
    confirmReport(reportId, pass, operator, roleKey) {
      const r = this.workReports.find((x) => x.id === reportId)
      if (!r || r.status !== '已提交') return false
      r.status = pass ? '已确认' : '已驳回'
      log(this, '生产管理', pass ? '确认报工' : '驳回报工', reportId, operator, roleKey)
      return true
    },

    // —— 质检 ——
    submitInspection(qcId, payload, operator, roleKey) {
      const qc = this.inspections.find((x) => x.id === qcId)
      if (!qc || qc.status !== '待检') return false
      qc.qcType = payload.qcType
      qc.qcItems = payload.qcItems
      qc.sampleQty = payload.sampleQty
      qc.qualifiedQty = payload.qualifiedQty
      qc.unqualifiedQty = payload.unqualifiedQty
      qc.result = payload.result
      qc.status = payload.result
      qc.inspector = operator
      qc.inspectorName = payload.inspectorName
      qc.remark = payload.remark || ''
      qc.updatedAt = now()
      const wo = this.workOrders.find((w) => w.id === qc.workOrderId)
      const dispatch = qc.dispatchId ? this.dispatches.find((d) => d.id === qc.dispatchId) : null

      if (payload.result === '合格' || payload.result === '让步接收') {
        this.inboundTasks.unshift({
          id: nextId('IN'),
          sourceType: payload.result === '合格' ? '质检合格' : '让步接收',
          refNo: qcId,
          productModel: qc.productModel,
          quantity: payload.qualifiedQty,
          status: '待入库',
          batchNo: qc.batchNo,
          workOrderId: qc.workOrderId,
          orderId: wo?.orderId || '',
          createdAt: now()
        })
        if (dispatch) {
          dispatch.status = '已完成'
          dispatch.updatedAt = now()
        }
        if (wo) {
          wo.qualifiedQty += payload.qualifiedQty
          syncWorkOrderStatus(this, wo.id)
        }
        if (qc.defectId) {
          const defect = this.defects.find((d) => d.id === qc.defectId)
          if (defect) {
            defect.status = '已返修'
            defect.disposition = '已返修'
            defect.updatedAt = now()
          }
        }
      } else if (payload.result === '不合格') {
        if (payload.qualifiedQty > 0) {
          this.inboundTasks.unshift({
            id: nextId('IN'),
            sourceType: '质检合格',
            refNo: qcId,
            productModel: qc.productModel,
            quantity: payload.qualifiedQty,
            status: '待入库',
            batchNo: qc.batchNo,
            workOrderId: qc.workOrderId,
            orderId: wo?.orderId || '',
            createdAt: now()
          })
          if (wo) wo.qualifiedQty += payload.qualifiedQty
        }
        const defectQty = payload.unqualifiedQty || 0
        if (defectQty > 0) {
          const defId = nextId('DEF')
          this.defects.unshift({
            id: defId,
            qcId,
            dispatchId: qc.dispatchId || '',
            workOrderId: qc.workOrderId,
            batchNo: qc.batchNo,
            productModel: qc.productModel,
            quantity: defectQty,
            defectLocation: payload.defectLocation || '',
            failedItems: payload.failedItems || [],
            severity: payload.severity || '轻微',
            description: payload.description || payload.remark || '质检不合格',
            disposition: payload.severity === '严重' ? '建议报废' : '建议返修',
            status: '待处理',
            operator: dispatch?.operator || '',
            operatorName: dispatch?.operatorName || qc.operatorName || '',
            handler: '',
            createdAt: now(),
            updatedAt: now()
          })
        }
        if (dispatch) {
          dispatch.status = '已完成'
          dispatch.updatedAt = now()
        }
        if (wo) syncWorkOrderStatus(this, wo.id)
      }
      log(this, '质量管理', `质检${payload.result}`, qcId, operator, roleKey)
      return true
    },
    scrapDefect(defectId, operator, roleKey, remark = '') {
      const defect = this.defects.find((d) => d.id === defectId)
      if (!defect || defect.status !== '待处理') return false
      defect.status = '已报废'
      defect.disposition = '已报废'
      defect.handler = operator
      defect.remark = remark || defect.description
      defect.updatedAt = now()
      log(this, '质量管理', `报废不合格品 ${defect.quantity} 台`, defectId, operator, roleKey)
      syncWorkOrderStatus(this, defect.workOrderId)
      return true
    },
    reworkDefect(defectId, operator, roleKey) {
      const defect = this.defects.find((d) => d.id === defectId)
      if (!defect || defect.status !== '待处理') return false
      const wo = this.workOrders.find((w) => w.id === defect.workOrderId)
      const origDispatch = defect.dispatchId
        ? this.dispatches.find((d) => d.id === defect.dispatchId)
        : null
      const opUser = defect.operator || origDispatch?.operator || 'operator'
      const opName = defect.operatorName || origDispatch?.operatorName || '王操作'
      const id = nextId('DIS-2026')
      this.dispatches.unshift({
        id,
        workOrderId: defect.workOrderId,
        workOrderNo: defect.workOrderId,
        processStep: '返修',
        equipment: origDispatch?.equipment || '返修工位',
        operator: opUser,
        operatorName: opName,
        planQty: defect.quantity,
        completedQty: 0,
        status: '已分配',
        defectId: defect.id,
        planStart: now(),
        planEnd: now(),
        createdAt: now(),
        updatedAt: now()
      })
      defect.status = '返修中'
      defect.disposition = '返修'
      defect.handler = operator
      defect.updatedAt = now()
      if (wo) {
        wo.status = '生产中'
        wo.updatedAt = now()
      }
      log(this, '质量管理', `派返修 ${defect.quantity} 台给 ${opName}`, defectId, operator, roleKey)
      return id
    },

    // —— 仓储 ——
    confirmInbound(taskId, operator, roleKey) {
      const task = this.inboundTasks.find((t) => t.id === taskId)
      if (!task || task.status !== '待入库') return false
      task.status = '已入库'
      const inv = this.inventory.find((i) => i.materialName.includes('成品') || i.materialCode.includes('FG'))
      if (inv) {
        inv.quantity += task.quantity
        inv.updatedAt = now()
      } else {
        this.inventory.push({
          id: nextId('INV-FG'), materialCode: `FG-${task.productModel}`, materialName: `成品 ${task.productModel}`,
          unit: '台', quantity: task.quantity, safeQty: 50, status: '正常', location: '成品仓', updatedAt: now()
        })
      }
      this.stockFlows.unshift({
        id: nextId('SF'), flowType: '成品入库', materialName: task.productModel,
        materialCode: task.productModel, quantity: task.quantity, direction: '入',
        refNo: taskId, operator, createdAt: now()
      })
      log(this, '仓储管理', '成品入库', taskId, operator, roleKey)
      if (task.orderId && !this.deliveries.some((d) => d.orderId === task.orderId && d.status === '待出库')) {
        const order = this.orders.find((o) => o.id === task.orderId)
        if (order) {
          this.deliveries.unshift({
            id: nextId('DLV-2026'), orderId: order.id, orderNo: order.id,
            customerName: order.customerName, productModel: task.productModel,
            quantity: task.quantity, status: '待出库', shipDate: '', trackingNo: '', createdAt: now()
          })
        }
      }
      return true
    },
    issueMaterial(taskId, qty, operator, roleKey) {
      const task = this.issueTasks.find((t) => t.id === taskId)
      if (!task) return false
      const inv = this.inventory.find((i) => i.materialCode === task.materialCode)
      if (!inv || inv.quantity < qty) return false
      inv.quantity -= qty
      inv.updatedAt = now()
      task.issuedQty += qty
      task.status = task.issuedQty >= task.requiredQty ? '已完成' : '部分领料'
      this.stockFlows.unshift({
        id: nextId('SF'), flowType: '生产领料', materialCode: task.materialCode,
        materialName: task.materialName, quantity: qty, direction: '出',
        refNo: task.workOrderId, operator, createdAt: now()
      })
      log(this, '仓储管理', '生产领料', taskId, operator, roleKey)
      return true
    },
    shipDelivery(dlvId, operator, roleKey) {
      const d = this.deliveries.find((x) => x.id === dlvId)
      if (!d || d.status !== '待出库') return false
      d.status = '已出库'
      d.shipDate = now().slice(0, 10)
      d.trackingNo = `SF${Date.now()}`
      const order = this.orders.find((o) => o.id === d.orderId)
      if (order) { order.status = '已发货'; order.updatedAt = now() }
      this.stockFlows.unshift({
        id: nextId('SF'), flowType: '发货出库', materialName: d.productModel,
        materialCode: d.productModel, quantity: d.quantity, direction: '出',
        refNo: dlvId, operator, createdAt: now()
      })
      log(this, '仓储管理', '发货出库', dlvId, operator, roleKey)
      return true
    },

    // —— 采购 ——
    createPurchaseOrder(payload, operator, roleKey) {
      const id = nextId('PO-2026')
      const po = {
        id, supplier: payload.supplier, materialCode: payload.materialCode,
        materialName: payload.materialName, quantity: payload.quantity,
        unitPrice: payload.unitPrice, totalAmount: payload.quantity * payload.unitPrice,
        status: '已下达', expectedDate: payload.expectedDate, arrivedQty: 0,
        buyer: operator, createdAt: now(), updatedAt: now()
      }
      this.purchaseOrders.unshift(po)
      const demand = this.purchaseDemands.find((d) => d.materialCode === payload.materialCode && d.status === '待采购')
      if (demand) demand.status = '已下单'
      log(this, '采购管理', '创建采购订单', id, operator, roleKey)
      return po
    },
    receivePurchase(poId, qty, operator, roleKey) {
      const po = this.purchaseOrders.find((p) => p.id === poId)
      if (!po) return false
      po.arrivedQty += qty
      po.status = po.arrivedQty >= po.quantity ? '已到货' : '部分到货'
      po.updatedAt = now()
      const inv = this.inventory.find((i) => i.materialCode === po.materialCode)
      if (inv) { inv.quantity += qty; inv.updatedAt = now() }
      this.stockFlows.unshift({
        id: nextId('SF'), flowType: '采购入库', materialCode: po.materialCode,
        materialName: po.materialName, quantity: qty, direction: '入',
        refNo: poId, operator, createdAt: now()
      })
      log(this, '采购管理', '采购到货', poId, operator, roleKey)
      return true
    },

    // —— 安灯 ——
    createAlarm(payload, operator, roleKey) {
      const id = nextId('ALM-2026')
      const alarm = {
        id, type: payload.type, source: payload.source, workOrderId: payload.workOrderId || '',
        level: payload.level || '中', status: '已上报', reporter: operator,
        reporterName: payload.reporterName, assignee: '', assigneeName: '',
        description: payload.description, createdAt: now(), updatedAt: now()
      }
      this.alarms.unshift(alarm)
      log(this, '安灯报警', '触发安灯', id, operator, roleKey)
      return alarm
    },
    handleAlarm(alarmId, action, operator, roleKey, assigneeName) {
      const a = this.alarms.find((x) => x.id === alarmId)
      if (!a) return false
      if (action === 'receive') { a.status = '已接收'; a.assignee = operator; a.assigneeName = assigneeName }
      else if (action === 'processing') { a.status = '处理中' }
      else if (action === 'close') { a.status = '已关闭' }
      a.updatedAt = now()
      log(this, '安灯报警', action === 'close' ? '关闭报警' : '处理报警', alarmId, operator, roleKey)
      return true
    },

    // —— 设备 ——
    updateEquipment(eqId, payload, operator, roleKey) {
      const eq = this.equipment.find((e) => e.id === eqId)
      if (!eq) return false
      Object.assign(eq, payload, { updatedAt: now() })
      if (payload.repairNote) {
        this.maintenanceRecords.unshift({
          id: nextId('MR'), equipmentId: eqId, equipmentName: eq.name,
          content: payload.repairNote, downtimeHours: payload.downtimeHours || 0,
          operator, createdAt: now()
        })
      }
      log(this, '设备管理', '更新设备', eqId, operator, roleKey)
      return true
    },

    // —— 售后 ——
    createAftersale(payload, operator, roleKey) {
      const id = nextId('AS-2026')
      const c = {
        id, orderId: payload.orderId, batchNo: payload.batchNo || '',
        productModel: payload.productModel, customerName: payload.customerName,
        feedback: payload.feedback, status: '已创建', handler: '', result: '',
        createdAt: now(), updatedAt: now()
      }
      this.aftersaleCases.unshift(c)
      log(this, '售后管理', '登记售后', id, operator, roleKey)
      return c
    },
    processAftersale(caseId, payload, operator, roleKey) {
      const c = this.aftersaleCases.find((x) => x.id === caseId)
      if (!c) return false
      c.status = payload.status || '处理中'
      c.handler = operator
      c.result = payload.result || ''
      c.updatedAt = now()
      log(this, '售后管理', '处理售后', caseId, operator, roleKey)
      return true
    },

    // —— 成本 ——
    confirmCostSettlement(csId, operator, roleKey) {
      const cs = this.costSettlements.find((c) => c.id === csId)
      if (!cs || cs.status !== '草稿') return false
      cs.status = '已确认'
      cs.updatedAt = now()
      log(this, '成本管理', '确认结算', csId, operator, roleKey)
      return true
    },
    exportCostSettlement(csId, operator, roleKey) {
      const cs = this.costSettlements.find((c) => c.id === csId)
      if (!cs || cs.status !== '已确认') return false
      cs.status = '已导出'
      cs.updatedAt = now()
      log(this, '成本管理', '导出结算', csId, operator, roleKey)
      return true
    },

    // —— 系统管理 ——
    saveUser(user, operator, roleKey) {
      if (user.id) {
        const idx = this.sysUsers.findIndex((u) => u.id === user.id)
        if (idx >= 0) this.sysUsers[idx] = { ...this.sysUsers[idx], ...user }
      } else {
        user.id = ++idSeq
        user.createdAt = now()
        user.status = user.status || '启用'
        this.sysUsers.push(user)
      }
      log(this, '系统管理', user.id ? '编辑用户' : '新增用户', user.username, operator, roleKey)
    },
    toggleUserStatus(userId, operator, roleKey) {
      const u = this.sysUsers.find((x) => x.id === userId)
      if (!u) return false
      u.status = u.status === '启用' ? '禁用' : '启用'
      log(this, '系统管理', u.status === '启用' ? '启用用户' : '禁用用户', u.username, operator, roleKey)
      return true
    }
  }
})

export { useMesStore as default }
