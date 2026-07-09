import request from '@/utils/request'

export function fetchMesSnapshot() {
  return request.get('/mes/snapshot')
}

export function postMesAction(body) {
  return request.post('/mes/action', body)
}

/** 生产主管大屏完整快照 */
export function fetchDashboardSnapshot() {
  return request.get('/mes/dashboard/snapshot')
}

export function fetchDashboardKpi() {
  return request.get('/mes/dashboard/kpi')
}
