export function occupancyRate(occupied, capacity) {
  if (!capacity) return 0
  return Math.round((occupied / capacity) * 100)
}

/** @returns {'empty'|'partial-low'|'partial-mid'|'near-full'|'full'} */
export function occupancyLevel(occupied, capacity) {
  if (!capacity || occupied <= 0) return 'empty'
  const rate = occupied / capacity
  if (rate >= 1) return 'full'
  if (rate >= 0.85) return 'near-full'
  if (rate >= 0.5) return 'partial-mid'
  return 'partial-low'
}

export function levelLabel(level) {
  return {
    empty: '空闲',
    'partial-low': '部分占用',
    'partial-mid': '部分占用',
    'near-full': '接近满仓',
    full: '已满'
  }[level] || '未知'
}
