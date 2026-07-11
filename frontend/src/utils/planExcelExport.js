import * as XLSX from 'xlsx'

const HEADERS = [
  '计划编号',
  '订单编号',
  '产品型号',
  '计划数量',
  '优先级',
  '计划开始时间',
  '计划完成时间',
  '车间',
  '预计工时',
  '完成进度',
  '计划状态',
  '延期风险'
]

function pad(n) {
  return String(n).padStart(2, '0')
}

export function formatPlanExportFilename(date = new Date()) {
  const y = date.getFullYear()
  const m = pad(date.getMonth() + 1)
  const d = pad(date.getDate())
  const h = pad(date.getHours())
  const min = pad(date.getMinutes())
  return `生产计划_${y}${m}${d}${h}${min}.xlsx`
}

export function exportPlanRows(rows, filename) {
  const data = rows.map((row) => [
    row.id,
    row.orderNo,
    row.productModel,
    row.quantity,
    row.priorityLabel || row.priority,
    row.planStart,
    row.planEnd,
    row.workshop,
    row.estimatedHours,
    `${row.progress ?? 0}%`,
    row.status,
    row.delayRisk
  ])

  const sheet = XLSX.utils.aoa_to_sheet([HEADERS, ...data])
  sheet['!cols'] = [
    { wch: 14 }, { wch: 14 }, { wch: 18 }, { wch: 10 }, { wch: 8 },
    { wch: 14 }, { wch: 14 }, { wch: 14 }, { wch: 10 }, { wch: 10 },
    { wch: 10 }, { wch: 10 }
  ]
  const book = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(book, sheet, '生产计划')
  XLSX.writeFile(book, filename || formatPlanExportFilename())
}
