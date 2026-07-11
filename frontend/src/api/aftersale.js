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
