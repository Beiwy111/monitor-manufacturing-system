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

export function fetchPlans() {
  return request.get('/afterSales/workflow/plans')
}

export function fetchPlanByCase(caseNo) {
  return request.get('/afterSales/workflow/plan', { params: { caseNo } })
}

export function savePlan(data) {
  return request.post('/afterSales/workflow/plan/save', data)
}

export function submitPlan(data) {
  return request.post('/afterSales/workflow/plan/submit', data)
}

export function approvePlan(data) {
  return request.post('/afterSales/workflow/plan/approve', data)
}

export function rejectPlan(data) {
  return request.post('/afterSales/workflow/plan/reject', data)
}

export function fetchWorkflowTasks(caseNo) {
  return request.get('/afterSales/workflow/tasks', { params: caseNo ? { caseNo } : {} })
}

export function updateWorkflowTask(data) {
  return request.post('/afterSales/workflow/task/update', data)
}

export function advanceCase(data) {
  return request.post('/afterSales/workflow/case/advance', data)
}

export function fetchClosure(caseNo) {
  return request.get('/afterSales/workflow/closure', { params: { caseNo } })
}

export function fetchClosures() {
  return request.get('/afterSales/workflow/closures')
}

export function saveClosure(data) {
  return request.post('/afterSales/workflow/closure/save', data)
}

export function confirmCustomer(data) {
  return request.post('/afterSales/workflow/closure/confirm-customer', data)
}

export function closeWithClosure(data) {
  return request.post('/afterSales/workflow/closure/close', data)
}
