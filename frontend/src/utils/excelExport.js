/**
 * 将表格数据导出为 Excel（.xls，Excel 可直接打开）
 * @param {Array<{name:string, headers:string[], rows:any[][]}>} sheets
 * @param {string} filename
 */
export function exportExcelSheets(sheets, filename = 'export.xls') {
  const escape = (v) => {
    const s = v == null ? '' : String(v)
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  }
  const sheetHtml = sheets.map((sheet) => {
    const headerRow = sheet.headers.map((h) => `<th>${escape(h)}</th>`).join('')
    const bodyRows = sheet.rows.map((row) =>
      `<tr>${row.map((cell) => `<td>${escape(cell)}</td>`).join('')}</tr>`
    ).join('')
    return `<h3>${escape(sheet.name)}</h3><table border="1"><thead><tr>${headerRow}</tr></thead><tbody>${bodyRows}</tbody></table><br/>`
  }).join('')

  const html = `<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel">
<head><meta charset="UTF-8"><!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets>
${sheets.map((s, i) => `<x:ExcelWorksheet><x:Name>${escape(s.name || `Sheet${i + 1}`)}</x:Name></x:ExcelWorksheet>`).join('')}
</x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]--></head><body>${sheetHtml}</body></html>`

  const blob = new Blob(['\ufeff', html], { type: 'application/vnd.ms-excel;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename.endsWith('.xls') || filename.endsWith('.xlsx') ? filename : `${filename}.xls`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
