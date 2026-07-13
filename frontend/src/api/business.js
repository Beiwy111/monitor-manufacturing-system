import request from '@/utils/request'

export function fetchList(path) {
  return request.get(path)
}

export function fetchOrderList() {
  return request.get('/order/customerOrder/list')
}

export function fetchProductionPlanList() {
  return request.get('/production/plan/list')
}

export function fetchWorkOrderList() {
  return request.get('/production/workOrder/list')
}

export function fetchDispatchList() {
  return request.get('/production/dispatch/list')
}

export function fetchWorkReportList() {
  return request.get('/production/report/list')
}

export function fetchInspectionList() {
  return request.get('/quality/inspection/list')
}

export function fetchNonconformingList() {
  return request.get('/quality/nonconforming/list')
}

export function fetchInventoryList() {
  return request.get('/material/inventory/list')
}

export function fetchInventoryFullList() {
  return request.get('/material/inventory/full')
}

export function fetchPurchaseOrderList() {
  return request.get('/purchase/purchaseOrder/list')
}

// ============ 采购工作台 ============

export function calculatePurchaseRequirements() {
  return request.post('/purchase/workbench/calculate')
}

export function fetchWorkbenchList(params) {
  return request.get('/purchase/workbench/list', { params })
}

export function fetchWorkbenchDetail(requirementId) {
  return request.get('/purchase/workbench/detail', { params: { requirementId } })
}

export function fetchWorkbenchByOrder() {
  return request.get('/purchase/workbench/byOrder')
}

export function generatePurchaseOrder(data) {
  return request.post('/purchase/workbench/generate', data)
}

export function selectRequirement(requirementId) {
  return request.post('/purchase/workbench/select', null, { params: { requirementId } })
}

export function cancelRequirement(requirementId) {
  return request.post('/purchase/workbench/cancel', null, { params: { requirementId } })
}

export function confirmArrival(purchaseOrderId) {
  return request.post('/purchase/order/confirmArrival', null, { params: { purchaseOrderId } })
}

export function fetchOrderDemandOverview() {
  return request.get('/purchase/workbench/orderDemands')
}

export function savePurchaseOrderDraft(data) {
  return request.post('/purchase/order/saveDraft', data)
}

export function revokePurchaseOrder(purchaseOrderId) {
  return request.post('/purchase/order/revoke', null, { params: { purchaseOrderId } })
}

// ============ AI 采购单据解析 ============

export function parseAiDocument(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/purchase/ai/document/parse', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 90000
  })
}

export function confirmAiDocument(data) {
  return request.post('/purchase/ai/document/confirm', data)
}

export function fetchDeliveryList() {
  return request.get('/order/delivery/list')
}

export function fetchEquipmentList() {
  return request.get('/equipment/equipment/list')
}

export function fetchAlarmList() {
  return request.get('/equipment/alarm/list')
}

// ===== 设备维保：安灯报警 + 维保闭环 =====
export function fetchEquipmentViews() {
  return request.get('/equipment/equipment/views')
}

export function fetchEquipmentKpi() {
  return request.get('/equipment/kpi')
}

export function fetchAlarmViews() {
  return request.get('/equipment/alarm/views')
}

export function fetchMaintenanceViews() {
  return request.get('/equipment/maintenance/views')
}

export function triggerAlarm(data) {
  return request.post('/equipment/triggerAlarm', data)
}

export function receiveAlarm(data) {
  return request.post('/equipment/alarm/receive', data)
}

export function resolveAlarm(data) {
  return request.post('/equipment/alarm/resolve', data)
}

export function startMaintenance(data) {
  return request.post('/equipment/startMaintenance', data)
}

export function finishMaintenance(data) {
  return request.post('/equipment/maintenance/finish', data)
}

export function fetchEquipmentHealth() {
  return request.get('/equipment/health/list').then(r => r.data ?? r)
}

export function fetchAfterSalesList() {
  return request.get('/afterSales/afterSalesCase/list')
}

export function fetchSettlementList() {
  return request.get('/afterSales/settlement/list')
}

export function fetchUserList() {
  return request.get('/system/user/list')
}

export function fetchRoleList() {
  return request.get('/system/role/list')
}

export function fetchOperationLogList() {
  return request.get('/system/operationLog/list')
}

// ============ 供应商管理 ============

export function fetchSupplierList() {
  return request.get('/purchase/supplier/list')
}

export function fetchActiveSupplierList() {
  return request.get('/purchase/supplier/active')
}

export function addSupplier(data) {
  return request.post('/purchase/supplier/add', data)
}

export function updateSupplier(data) {
  return request.put('/purchase/supplier/update', data)
}

export function deleteSupplier(supplierId) {
  return request.delete('/purchase/supplier/delete', { params: { supplierId } })
}
