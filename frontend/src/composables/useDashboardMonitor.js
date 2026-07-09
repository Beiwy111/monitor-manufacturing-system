import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useMesStore } from '@/stores/mes'
import {
  fetchOrderList,
  fetchProductionPlanList,
  fetchWorkOrderList,
  fetchInspectionList,
  fetchInventoryList,
  fetchPurchaseOrderList,
  fetchDeliveryList,
  fetchAlarmList,
  fetchOperationLogList
} from '@/api/business'

/** 是否优先请求后端；数据以 mes store 实时快照为准 */
const PREFER_API = true

function formatNow() {
  return new Date().toISOString().slice(0, 19).replace('T', ' ')
}

function resolveNodeStatus(total, active, abnormal) {
  if (abnormal > 0) return 'abnormal'
  if (active > 0) return 'active'
  if (total === 0) return 'idle'
  return 'normal'
}

function latestTime(items, field = 'updatedAt') {
  if (!items?.length) return ''
  return items.reduce((max, item) => {
    const t = item[field] || item.createdAt || ''
    return t > max ? t : max
  }, '')
}

/**
 * 从 mes store 计算看板数据（数据来自 /mes/snapshot 实时快照）
 */
function computeDashboardSnapshot(mes) {
  const orders = mes.orders || []
  const plans = mes.plans || []
  const workOrders = mes.workOrders || []
  const dispatches = mes.dispatches || []
  const inspections = mes.inspections || []
  const defects = mes.defects || []
  const purchaseDemands = mes.purchaseDemands || []
  const purchaseOrders = mes.purchaseOrders || []
  const inventory = mes.inventory || []
  const issueTasks = mes.issueTasks || []
  const inboundTasks = mes.inboundTasks || []
  const deliveries = mes.deliveries || []
  const alarms = mes.alarms || []
  const equipment = mes.equipment || []
  const aftersaleCases = mes.aftersaleCases || []
  const logs = mes.operationLogs || []

  const pendingAudit = orders.filter((o) => o.status === '待审核')
  const activePlans = plans.filter((p) => ['已发布', '执行中'].includes(p.status))
  const pendingPurchase = purchaseDemands.filter((d) => d.status === '待采购')
  const activePurchase = purchaseOrders.filter((p) => !['已到货', '已取消'].includes(p.status))
  const lowStock = inventory.filter((i) => i.quantity < i.safeQty)
  const pendingIssue = issueTasks.filter((t) => t.status !== '已完成')
  const draftWo = workOrders.filter((w) => w.status === '草稿')
  const releasedWo = workOrders.filter((w) => ['已下达', '已派工'].includes(w.status))
  const activeWo = workOrders.filter((w) => ['生产中', '待质检'].includes(w.status))
  const activeDispatch = dispatches.filter((d) => ['已接收', '生产中', '待质检'].includes(d.status))
  const pendingQc = inspections.filter((i) => i.status === '待检')
  const failedQc = inspections.filter((i) => i.status === '不合格')
  const pendingDefects = defects.filter((d) => ['待处理', '返修中'].includes(d.status))
  const pendingInbound = inboundTasks.filter((t) => t.status === '待入库')
  const pendingDelivery = deliveries.filter((d) => ['待出库', '已出库', '运输中'].includes(d.status))
  const openAlarms = alarms.filter((a) => a.status !== '已关闭')
  const faultEquipment = equipment.filter((e) => e.status === '故障')
  const openAftersale = aftersaleCases.filter((c) => c.status !== '已关闭')

  const processNodes = [
    {
      key: 'order',
      name: '客户订单',
      total: orders.length,
      active: orders.filter((o) => !['已完成', '已作废', '已发货'].includes(o.status)).length,
      abnormal: orders.filter((o) => o.status === '已作废').length,
      status: resolveNodeStatus(orders.length, orders.filter((o) => o.status === '生产中').length, 0),
      updateTime: latestTime(orders)
    },
    {
      key: 'audit',
      name: '订单审核',
      total: orders.length,
      active: pendingAudit.length,
      abnormal: 0,
      status: resolveNodeStatus(pendingAudit.length, pendingAudit.length, 0),
      updateTime: latestTime(pendingAudit)
    },
    {
      key: 'plan',
      name: '生产计划',
      total: plans.length,
      active: activePlans.length,
      abnormal: plans.filter((p) => p.status === '已取消').length,
      status: resolveNodeStatus(plans.length, activePlans.length, 0),
      updateTime: latestTime(plans)
    },
    {
      key: 'purchase',
      name: '物料采购',
      total: purchaseDemands.length + purchaseOrders.length,
      active: pendingPurchase.length + activePurchase.length,
      abnormal: pendingPurchase.filter((d) => d.gapQty > 300).length,
      status: resolveNodeStatus(purchaseOrders.length, activePurchase.length, pendingPurchase.length),
      updateTime: latestTime([...purchaseDemands, ...purchaseOrders])
    },
    {
      key: 'stock',
      name: '库存备料',
      total: inventory.length,
      active: pendingIssue.length,
      abnormal: lowStock.length,
      status: resolveNodeStatus(inventory.length, pendingIssue.length, lowStock.length),
      updateTime: latestTime(inventory)
    },
    {
      key: 'workorder',
      name: '工单下达',
      total: workOrders.length,
      active: draftWo.length + releasedWo.length,
      abnormal: 0,
      status: resolveNodeStatus(workOrders.length, draftWo.length + releasedWo.length, 0),
      updateTime: latestTime(workOrders)
    },
    {
      key: 'production',
      name: '生产执行',
      total: dispatches.length,
      active: activeDispatch.length,
      abnormal: dispatches.filter((d) => d.status === '生产中' && d.completedQty < d.planQty * 0.5).length,
      status: resolveNodeStatus(dispatches.length, activeDispatch.length, openAlarms.length),
      updateTime: latestTime(dispatches)
    },
    {
      key: 'qc',
      name: '质量检验',
      total: inspections.length,
      active: pendingQc.length,
      abnormal: failedQc.length + pendingDefects.length,
      status: resolveNodeStatus(inspections.length, pendingQc.length, failedQc.length + pendingDefects.length),
      updateTime: latestTime(inspections)
    },
    {
      key: 'inbound',
      name: '成品入库',
      total: inboundTasks.length,
      active: pendingInbound.length,
      abnormal: 0,
      status: resolveNodeStatus(inboundTasks.length, pendingInbound.length, 0),
      updateTime: latestTime(inboundTasks)
    },
    {
      key: 'delivery',
      name: '发货追溯',
      total: deliveries.length + aftersaleCases.length,
      active: pendingDelivery.length + openAftersale.length,
      abnormal: openAftersale.length,
      status: resolveNodeStatus(deliveries.length, pendingDelivery.length, openAftersale.length),
      updateTime: latestTime([...deliveries, ...aftersaleCases])
    }
  ]

  const flowList = orders.map((o) => {
    const plan = plans.find((p) => p.orderId === o.id)
    const wo = workOrders.find((w) => w.orderId === o.id)
    const qc = inspections.find((i) => i.workOrderId === wo?.id && i.status === '待检')
    const inbound = inboundTasks.find((t) => t.orderId === o.id)
    const delivery = deliveries.find((d) => d.orderId === o.id)
    let stage = '客户订单'
    if (o.status === '待审核') stage = '待审核'
    else if (o.status === '已审核') stage = '待提交计划'
    else if (o.status === '待计划' && !plan) stage = '待计划'
    else if (plan?.status === '草稿' || plan?.status === '已发布') stage = '计划编制'
    else if (plan?.status === '已提交' && !wo) stage = '待生成工单'
    else if (wo && ['草稿', '已下达', '已派工'].includes(wo.status)) stage = '待生产'
    else if (wo && ['生产中', '待质检'].includes(wo.status)) stage = '生产中'
    else if (qc) stage = '待质检'
    else if (inbound?.status === '待入库') stage = '待入库'
    else if (delivery?.status === '待出库') stage = '待发货'
    else if (o.status === '已发货') stage = '已发货'
    else if (o.status === '已完成') stage = '已完成'
    return {
      orderId: o.id,
      orderNo: o.id,
      customerName: o.customerName,
      productModel: o.productModel,
      quantity: o.quantity,
      status: o.status,
      stage,
      planId: plan?.id || '-',
      workOrderId: wo?.id || '-',
      updatedAt: o.updatedAt || o.createdAt
    }
  })

  const progressList = workOrders.map((wo) => {
    const pct = wo.quantity ? Math.min(100, Math.round((wo.completedQty / wo.quantity) * 100)) : 0
    const relatedDispatches = dispatches.filter((d) => d.workOrderId === wo.id)
    const running = relatedDispatches.find((d) => ['生产中', '已接收'].includes(d.status))
    return {
      workOrderId: wo.id,
      orderId: wo.orderId,
      productModel: wo.productModel,
      line: wo.line || '-',
      manager: wo.manager || '-',
      quantity: wo.quantity,
      completedQty: wo.completedQty || 0,
      progress: pct,
      status: wo.status,
      processStep: running?.processStep || relatedDispatches[0]?.processStep || '-',
      operatorName: running?.operatorName || '-'
    }
  })

  const alertList = [
    ...openAlarms.map((a) => ({
      id: a.id,
      type: '设备异常',
      level: a.level || '中',
      title: a.description,
      ref: a.workOrderId || a.source,
      status: a.status,
      time: a.updatedAt || a.createdAt,
      source: 'alarm'
    })),
    ...pendingDefects.map((d) => ({
      id: d.id,
      type: '质检异常',
      level: d.severity === '严重' ? '高' : '中',
      title: `${d.defectLocation || '不合格品'} · ${d.quantity}台`,
      ref: d.workOrderId,
      status: d.status,
      time: d.updatedAt || d.createdAt,
      source: 'defect'
    })),
    ...lowStock.slice(0, 5).map((i) => ({
      id: i.id,
      type: '库存预警',
      level: '中',
      title: `${i.materialName} 低于安全库存`,
      ref: i.materialCode,
      status: '待处理',
      time: i.updatedAt,
      source: 'inventory'
    })),
    ...pendingQc.map((i) => ({
      id: i.id,
      type: '待检任务',
      level: '低',
      title: `${i.productModel} 待检验`,
      ref: i.workOrderId,
      status: i.status,
      time: i.updatedAt || i.createdAt,
      source: 'inspection'
    }))
  ].sort((a, b) => (b.time || '').localeCompare(a.time || ''))

  const eventLogs = logs.slice(0, 30)

  const hasHighAlert = alertList.some((a) => a.level === '高') || faultEquipment.length > 0

  const finishedOrders = orders.filter((o) => ['已完成', '已发货'].includes(o.status)).length
  const finishedWo = workOrders.filter((w) => w.status === '已完成').length
  const inspectedDone = inspections.filter((i) => i.status !== '待检')
  const passedQc = inspections.filter((i) => ['合格', '让步接收'].includes(i.status)).length
  const qualityPassRate = inspectedDone.length
    ? Math.round((passedQc / inspectedDone.length) * 100)
    : 100
  const runningEq = equipment.filter((e) => e.status === '运行中').length
  const deviceRunningRate = equipment.length ? Math.round((runningEq / equipment.length) * 100) : 100

  const factoryOverview = {
    orderCount: orders.length,
    activeWorkOrderCount: activeWo.length,
    finishedCount: finishedOrders + finishedWo,
    abnormalCount: alertList.length,
    qualityPassRate,
    deviceRunningRate,
    pendingQcCount: pendingQc.length
  }

  function toStation(key, name, total, active, abnormal, nodeStatus, updateTime, extra = {}) {
    let status = 'normal'
    if (faultEquipment.length && ['aging', 'alert'].includes(key)) status = 'stopped'
    else if (abnormal > 0) status = 'warning'
    else if (nodeStatus === 'active') status = 'running'
    else if (nodeStatus === 'idle' && total === 0) status = 'pending'
    return { key, name, total, active, abnormal, status, updateTime, ...extra }
  }

  const orderNode = processNodes.find((n) => n.key === 'order')
  const planNode = processNodes.find((n) => n.key === 'plan')
  const stockNode = processNodes.find((n) => n.key === 'stock')
  const purchaseNode = processNodes.find((n) => n.key === 'purchase')
  const prodNode = processNodes.find((n) => n.key === 'production')
  const qcNode = processNodes.find((n) => n.key === 'qc')
  const inboundNode = processNodes.find((n) => n.key === 'inbound')
  const deliveryNode = processNodes.find((n) => n.key === 'delivery')

  const processStations = [
    toStation('order', '订单接收', orderNode.total, orderNode.active, orderNode.abnormal, orderNode.status, orderNode.updateTime),
    toStation('plan', '生产计划', planNode.total, planNode.active, planNode.abnormal, planNode.status, planNode.updateTime),
    toStation(
      'warehouse',
      '物料仓储',
      stockNode.total,
      stockNode.active + purchaseNode.active,
      stockNode.abnormal + purchaseNode.abnormal,
      stockNode.active + purchaseNode.active > 0 ? 'active' : stockNode.status,
      stockNode.updateTime || purchaseNode.updateTime
    ),
    toStation('panel', '屏幕面板装配', prodNode.total, Math.ceil(prodNode.active / 2), 0, prodNode.status, prodNode.updateTime),
    toStation('assembly', '主板电源装配', prodNode.total, prodNode.active, openAlarms.length, prodNode.status, prodNode.updateTime),
    toStation('aging', '老化测试', equipment.length, runningEq, faultEquipment.length, faultEquipment.length ? 'abnormal' : 'active', latestTime(equipment)),
    toStation('quality', '质量检验', qcNode.total, qcNode.active, qcNode.abnormal, qcNode.status, qcNode.updateTime),
    toStation('packing', '包装入库', inboundNode.total, inboundNode.active, 0, inboundNode.status, inboundNode.updateTime),
    toStation('alert', '异常预警', alertList.length, alertList.filter((a) => a.level !== '低').length, alertList.filter((a) => a.level === '高').length, alertList.length ? 'abnormal' : 'idle', latestTime(alertList, 'time')),
    toStation('delivery', '发货追溯', deliveryNode.total, deliveryNode.active, deliveryNode.abnormal, deliveryNode.status, deliveryNode.updateTime)
  ]

  return {
    processNodes,
    processStations,
    factoryOverview,
    flowList,
    progressList,
    alertList,
    eventLogs,
    systemStatus: hasHighAlert ? '预警' : '正常',
    raw: {
      pendingAudit: pendingAudit.length,
      pendingQc: pendingQc.length,
      failedQc: failedQc.length,
      pendingDefects: pendingDefects.length,
      pendingInbound: pendingInbound.length,
      qualifiedQc: inspections.filter((i) => i.status === '合格').length,
      recentInspections: inspections.slice(0, 5),
      activeDispatches: activeDispatch,
      faultEquipment
    }
  }
}

async function tryFetchApi() {
  const results = await Promise.allSettled([
    fetchOrderList(),
    fetchProductionPlanList(),
    fetchWorkOrderList(),
    fetchInspectionList(),
    fetchInventoryList(),
    fetchPurchaseOrderList(),
    fetchDeliveryList(),
    fetchAlarmList(),
    fetchOperationLogList()
  ])
  const ok = results.filter((r) => r.status === 'fulfilled' && r.value).length
  return ok > 0
}

export function useDashboardMonitor() {
  const mes = useMesStore()
  const loading = ref(false)
  const lastRefreshTime = ref('')
  const systemStatus = ref('正常')
  const selectedNodeKey = ref(null)
  const selectedFlowItem = ref(null)
  const selectedAlert = ref(null)

  const processNodes = ref([])
  const processStations = ref([])
  const factoryOverview = ref({
    orderCount: 0,
    activeWorkOrderCount: 0,
    finishedCount: 0,
    abnormalCount: 0,
    qualityPassRate: 0,
    deviceRunningRate: 0,
    pendingQcCount: 0
  })
  const flowList = ref([])
  const progressList = ref([])
  const alertList = ref([])
  const eventLogs = ref([])
  const rawMetrics = ref({})

  let refreshTimer = null
  let wsPlaceholder = null

  async function refreshDashboardData() {
    loading.value = true
    try {
      await mes.hydrateFromApi?.()
      if (PREFER_API) {
        await tryFetchApi().catch(() => {})
      }
      const snapshot = computeDashboardSnapshot(mes)
      processNodes.value = snapshot.processNodes
      processStations.value = snapshot.processStations
      factoryOverview.value = snapshot.factoryOverview
      flowList.value = snapshot.flowList
      progressList.value = snapshot.progressList
      alertList.value = snapshot.alertList
      eventLogs.value = snapshot.eventLogs
      rawMetrics.value = snapshot.raw
      systemStatus.value = snapshot.systemStatus
      lastRefreshTime.value = formatNow()
    } finally {
      loading.value = false
    }
  }

  function selectNode(key) {
    selectedNodeKey.value = key
    selectedFlowItem.value = null
    selectedAlert.value = null
  }

  function selectStation(key) {
    selectNode(key)
  }

  function selectFlowItem(item) {
    selectedFlowItem.value = item
    selectedNodeKey.value = null
    selectedAlert.value = null
  }

  function selectAlert(item) {
    selectedAlert.value = item
    selectedNodeKey.value = null
    selectedFlowItem.value = null
  }

  const selectedNode = computed(() =>
    processNodes.value.find((n) => n.key === selectedNodeKey.value) || null
  )

  const nodeDetail = computed(() => {
    const key = selectedNodeKey.value
    const raw = rawMetrics.value
    if (!key) return null
    const map = {
      order: { title: '客户订单', lines: [`订单总数 ${processNodes.value.find(n => n.key === 'order')?.total || 0}`] },
      audit: { title: '订单审核', lines: [`待审核 ${raw.pendingAudit || 0} 单`] },
      plan: { title: '生产计划', lines: [`计划总数 ${mes.plans.length}`, `执行中 ${mes.plans.filter(p => p.status === '执行中').length}`] },
      purchase: { title: '物料采购', lines: [`待采购需求 ${mes.purchaseDemands.filter(d => d.status === '待采购').length}`, `进行中采购单 ${mes.purchaseOrders.filter(p => !['已到货','已取消'].includes(p.status)).length}`] },
      stock: { title: '库存备料', lines: [`库存品种 ${mes.inventory.length}`, `低于安全库存 ${mes.inventory.filter(i => i.quantity < i.safeQty).length}`, `待领料 ${mes.issueTasks.filter(t => t.status !== '已完成').length}`] },
      workorder: { title: '工单下达', lines: [`工单总数 ${mes.workOrders.length}`, `待下达 ${mes.workOrders.filter(w => w.status === '草稿').length}`, `待派工 ${mes.workOrders.filter(w => w.status === '已下达').length}`] },
      production: { title: '生产执行', lines: [`进行中派工 ${raw.activeDispatches?.length || 0}`, ...(raw.activeDispatches || []).slice(0, 4).map(d => `${d.processStep} · ${d.operatorName} · ${d.completedQty}/${d.planQty}`)] },
      qc: { title: '质量检验', lines: [`待检 ${raw.pendingQc || 0}`, `合格 ${raw.qualifiedQc || 0}`, `不合格 ${raw.failedQc || 0}`, `不合格品待处理 ${raw.pendingDefects || 0}`], records: raw.recentInspections },
      quality: { title: '质量检验', lines: [`待检 ${raw.pendingQc || 0}`, `合格 ${raw.qualifiedQc || 0}`, `不合格 ${raw.failedQc || 0}`, `不合格品待处理 ${raw.pendingDefects || 0}`], records: raw.recentInspections },
      inbound: { title: '成品入库', lines: [`待入库 ${raw.pendingInbound || 0}`] },
      delivery: { title: '发货追溯', lines: [`待出库 ${mes.deliveries.filter(d => d.status === '待出库').length}`, `售后跟踪 ${mes.aftersaleCases.filter(c => c.status !== '已关闭').length}`] },
      warehouse: { title: '物料仓储', lines: [`库存品种 ${mes.inventory.length}`, `待领料 ${mes.issueTasks.filter(t => t.status !== '已完成').length}`, `采购进行中 ${mes.purchaseOrders.filter(p => !['已到货','已取消'].includes(p.status)).length}`] },
      panel: { title: '屏幕面板装配', lines: [`装配派工 ${raw.activeDispatches?.length || 0}`] },
      assembly: { title: '主板电源装配', lines: [`进行中 ${raw.activeDispatches?.length || 0}`, `安灯 ${mes.alarms.filter(a => a.status !== '已关闭').length}`] },
      aging: { title: '老化测试', lines: [`设备 ${mes.equipment.length}`, `运行 ${mes.equipment.filter(e => e.status === '运行中').length}`, `故障 ${mes.equipment.filter(e => e.status === '故障').length}`] },
      packing: { title: '包装入库', lines: [`待入库 ${raw.pendingInbound || 0}`] },
      alert: { title: '异常预警', lines: alertList.value.slice(0, 5).map(a => `${a.type}: ${a.title}`) }
    }
    return map[key] || null
  })

  const selectedStation = computed(() =>
    processStations.value.find((s) => s.key === selectedNodeKey.value) || null
  )

  const stationDetail = computed(() => {
    const station = selectedStation.value
    if (!station) return null
    const detail = nodeDetail.value
    let suggestion = '环节运行正常，持续监控即可'
    if (station.status === 'stopped') suggestion = '设备停机，请设备维护人员排查'
    else if (station.status === 'warning' || station.abnormal > 0) suggestion = '存在异常，请相关岗位及时处理'
    else if (station.status === 'pending') suggestion = '当前暂无任务，可安排下一批订单'
    else if (station.status === 'running') suggestion = '工位运行中，关注进度与质量'
    const relatedOrders = flowList.value.slice(0, 3).map((f) => f.orderNo).join('、') || '-'
    const relatedWo = progressList.value.slice(0, 3).map((p) => p.workOrderId).join('、') || '-'
    return {
      ...station,
      title: detail?.title || station.name,
      lines: detail?.lines || [],
      relatedOrders,
      relatedWorkOrders: relatedWo,
      suggestion
    }
  })

  const traceDetail = computed(() => {
    if (!selectedFlowItem.value) return null
    const id = selectedFlowItem.value.orderId || selectedFlowItem.value.orderNo
    return mes.traceChain(id)
  })

  /** 预留 WebSocket 实时推送入口 */
  function connectWebSocket(url) {
    if (wsPlaceholder) return wsPlaceholder
    // 后续可替换为: new WebSocket(url) 并在 onmessage 中调用 refreshDashboardData()
    wsPlaceholder = { url, close: () => { wsPlaceholder = null } }
    return wsPlaceholder
  }

  onMounted(() => {
    refreshDashboardData()
    refreshTimer = setInterval(refreshDashboardData, 10000)
  })

  onUnmounted(() => {
    if (refreshTimer) clearInterval(refreshTimer)
    if (wsPlaceholder?.close) wsPlaceholder.close()
  })

  return {
    loading,
    lastRefreshTime,
    systemStatus,
    processNodes,
    processStations,
    factoryOverview,
    flowList,
    progressList,
    alertList,
    eventLogs,
    selectedNodeKey,
    selectedFlowItem,
    selectedAlert,
    selectedNode,
    selectedStation,
    nodeDetail,
    stationDetail,
    traceDetail,
    refreshDashboardData,
    selectNode,
    selectStation,
    selectFlowItem,
    selectAlert,
    connectWebSocket
  }
}
