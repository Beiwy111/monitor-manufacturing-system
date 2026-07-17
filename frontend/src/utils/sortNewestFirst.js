function parseSortTime(row) {
  if (!row) return 0
  const raw = row.createdAt ?? row.updatedAt ?? row.createTime ?? row.operatedAt ?? row.registeredAt ?? ''
  if (!raw) return 0
  const t = Date.parse(String(raw).replace(' ', 'T'))
  return Number.isNaN(t) ? 0 : t
}

function extractSortId(row) {
  if (!row) return 0
  const keys = [
    'orderId',
    'purchaseOrderId',
    'planId',
    'workOrderId',
    'requirementId',
    'sourceId',
    'dispatchId',
    'reportId',
    'deliveryId',
    'id',
    'userId'
  ]
  for (const key of keys) {
    const v = row[key]
    if (v == null || v === '') continue
    if (typeof v === 'number') return v
    const digits = String(v).match(/\d+/g)
    if (digits?.length) {
      const n = parseInt(digits[digits.length - 1], 10)
      if (!Number.isNaN(n)) return n
    }
  }
  return 0
}

export function compareNewestFirst(a, b) {
  const timeCmp = parseSortTime(b) - parseSortTime(a)
  if (timeCmp !== 0) return timeCmp
  return extractSortId(b) - extractSortId(a)
}

/** 新建记录排在最上方 */
export function sortNewestFirst(list) {
  if (!Array.isArray(list) || list.length <= 1) return list || []
  return [...list].sort(compareNewestFirst)
}
