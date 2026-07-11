import request from '@/utils/request'

export function fetchSettlementViews() {
  return request.get('/cost/settlement/views')
}

export function fetchCostKpi() {
  return request.get('/cost/kpi')
}

export function fetchCostGroup() {
  return request.get('/cost/group')
}

export function confirmSettlement(settlementId, operator) {
  return request.post('/cost/settlement/confirm', { settlementId, operator })
}

export function exportSettlement(settlementId, operator) {
  return request.post('/cost/settlement/export', { settlementId, operator })
}

export function saveSettlement(data) {
  return request.post('/cost/settlement/save', data)
}
