import request from '@/utils/request'

export function fetchWorkOrderCostOverview() {
  return request.get('/finance/cost/overview')
}

export function fetchCostBreakdown() {
  return request.get('/finance/cost/breakdown')
}

export function fetchOrderRevenue() {
  return request.get('/finance/revenue/orders')
}

export function fetchFinancePayments() {
  return request.get('/finance/revenue/payments')
}

export function fetchFinanceReceivables() {
  return request.get('/finance/revenue/receivables')
}

export function fetchProfitAnalysis() {
  return request.get('/finance/revenue/profit-analysis')
}

export function fetchFinanceScreen(days = 30) {
  return request.get('/finance/screen', { params: { days } })
}

export function fetchFinanceReport(period) {
  return request.get('/finance/report', { params: period ? { period } : {} })
}
