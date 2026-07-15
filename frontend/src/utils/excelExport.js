import * as XLSX from 'xlsx'

function sanitizeSheetName(name, index) {
  return String(name || `Sheet${index + 1}`)
    .replace(/[\\/?*[\]:]/g, '_')
    .slice(0, 31)
}

function normalizeFilename(filename) {
  const base = String(filename || 'export').replace(/[\\/:*?"<>|]/g, '_')
  if (base.endsWith('.xlsx')) return base
  if (base.endsWith('.xls')) return `${base.slice(0, -4)}.xlsx`
  return `${base}.xlsx`
}

function estimateColWidths(headers, rows) {
  const widths = headers.map((h) => Math.max(String(h || '').length, 8))
  rows.forEach((row) => {
    row.forEach((cell, idx) => {
      const len = String(cell ?? '').length
      if (len > widths[idx]) widths[idx] = Math.min(len, 40)
    })
  })
  return widths.map((w) => ({ wch: w + 2 }))
}

/**
 * 将表格数据导出为 Excel（.xlsx，标准 Office Open XML 格式）
 * @param {Array<{name:string, headers:string[], rows:any[][]}>} sheets
 * @param {string} filename
 */
export function exportExcelSheets(sheets, filename = 'export.xlsx') {
  const safeSheets = (sheets || []).map((sheet, i) => ({
    name: sanitizeSheetName(sheet.name, i),
    headers: Array.isArray(sheet.headers) ? sheet.headers : [],
    rows: (sheet.rows || []).map((row) => (Array.isArray(row) ? row : [row]).map((c) => c ?? ''))
  }))

  const book = XLSX.utils.book_new()
  safeSheets.forEach((sheet) => {
    const data = [sheet.headers, ...sheet.rows]
    const ws = XLSX.utils.aoa_to_sheet(data)
    ws['!cols'] = estimateColWidths(sheet.headers, sheet.rows)
    XLSX.utils.book_append_sheet(book, ws, sheet.name)
  })

  XLSX.writeFile(book, normalizeFilename(filename))
}
