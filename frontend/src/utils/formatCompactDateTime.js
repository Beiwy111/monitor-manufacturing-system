/** 表格内展示：2026-07-16 09:05 */
export function formatCompactDateTime(value) {
  if (!value) return '—'
  const raw = String(value).trim()
  const m = raw.match(/^(\d{4}-\d{2}-\d{2})[T\s](\d{2}:\d{2})/)
  if (m) return `${m[1]} ${m[2]}`
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) return raw
  return raw.length > 16 ? raw.slice(0, 16) : raw
}
