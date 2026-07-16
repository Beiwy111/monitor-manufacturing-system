export const COST_FORMULA = '材料成本 + 人工成本 + 设备成本 + 质检成本 + 返工报废成本 + 仓储物流成本 + 售后成本 + 其他成本'

export const PAYMENT_STATUS = {
  PENDING: '待回款',
  PARTIAL: '部分回款',
  RECEIVED: '已回款',
  OVERDUE: '已逾期'
}

export const ACCOUNTING_STATUS = {
  SETTLED: '已结清',
  PARTIAL: '部分回款',
  PENDING_COST: '待成本核算',
  PENDING_PAYMENT: '待回款',
  ACCOUNTED: '已核算'
}

export const RISK_LEVEL = {
  HIGH: '高风险',
  MEDIUM: '中风险',
  LOW: '低风险'
}

export function fmtMoney(v) {
  const n = Number(v)
  if (Number.isNaN(n)) return '-'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function fmtPct(v) {
  const n = Number(v)
  if (Number.isNaN(n)) return '-'
  return `${n.toFixed(2)}%`
}

export function marginTagType(margin) {
  const n = Number(margin)
  if (Number.isNaN(n)) return 'info'
  if (n < 0) return 'danger'
  if (n < 10) return 'warning'
  return 'success'
}

export function paymentTagType(status) {
  return { PENDING: 'info', PARTIAL: 'warning', RECEIVED: 'success', OVERDUE: 'danger' }[status] || 'info'
}

export function riskTagType(risk) {
  return { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }[risk] || 'info'
}

export function exportCsv(filename, headers, rows) {
  const escape = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const lines = [headers.map(escape).join(',')]
  rows.forEach((row) => lines.push(row.map(escape).join(',')))
  const blob = new Blob(['\uFEFF' + lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
  URL.revokeObjectURL(link.href)
}
