import request from '@/utils/request'

export function fetchCaseViews() {
  return request.get('/afterSales/case/views')
}

export function fetchTraceDetail(caseNo) {
  return request.get('/afterSales/case/trace', { params: { caseNo } })
}

export function fetchAfterSalesKpi() {
  return request.get('/afterSales/kpi')
}

export function acceptCase(data) {
  return request.post('/afterSales/case/accept', data)
}

export function resolveCase(data) {
  return request.post('/afterSales/case/resolve', data)
}

export function closeCase(data) {
  return request.post('/afterSales/case/close', data)
}

export function insertCase(data) {
  return request.post('/afterSales/afterSalesCase/insert', data)
}

export function fetchRcaAnalysis(caseNo, force = false) {
  return request.get('/afterSales/rca/analysis', { params: { caseNo, force } })
}

export function dispatchRcaTasks(data) {
  return request.post('/afterSales/rca/dispatch', data)
}

export function fetchRcaTasks(department) {
  return request.get('/afterSales/rca/tasks', { params: { department } })
}

export function confirmRcaRootCause(data) {
  return request.post('/afterSales/rca/confirm-root-cause', data)
}

export function updateRcaTask(data) {
  return request.post('/afterSales/rca/task/update', data)
}

export function fetchRcaTaskProgress(caseNo) {
  return request.get('/afterSales/rca/task/progress', { params: { caseNo } })
}

export function fetchAfterSalesTriage(caseNo, force = false) {
  return request.get('/afterSales/triage', { params: { caseNo, force } })
}
