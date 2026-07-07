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

export function fetchInventoryList() {
  return request.get('/material/inventory/list')
}

export function fetchPurchaseOrderList() {
  return request.get('/purchase/purchaseOrder/list')
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
