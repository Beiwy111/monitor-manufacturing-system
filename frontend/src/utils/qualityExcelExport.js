import { exportExcelSheets } from '@/utils/excelExport'

function yieldRate(sample, qualified) {
  const s = Number(sample)
  const q = Number(qualified)
  if (!s || s <= 0) return '0.0'
  return Math.min(100, Math.max(0, Math.round((q / s) * 1000) / 10)).toFixed(1)
}

function cellStr(v) {
  if (v == null) return ''
  return String(v)
}

function sanitizeSheetName(name) {
  return String(name || 'Sheet')
    .replace(/[\\/?*[\]:]/g, '_')
    .slice(0, 31)
}

function sanitizeFilename(name) {
  return String(name || 'export').replace(/[\\/:*?"<>|]/g, '_')
}

/**
 * 导出单份质检报告 Excel（含基本信息 + 检测项明细 + 逐台五步矩阵 + AI 分析）
 */
export function exportInspectionReportExcel({ detail, items = [], aiReport = null, unitMatrix = null }) {
  if (!detail) return
  const d = detail
  const infoHeaders = ['字段', '值']
  const infoRows = [
    ['质检单号', cellStr(d.inspectionNo)],
    ['检验类型', cellStr(d.inspectionTypeCn || d.inspectionType)],
    ['产品/物料', cellStr(d.materialName)],
    ['批次号', cellStr(d.batchNo)],
    ['工单号', cellStr(d.workOrderNo)],
    ['产品类别', cellStr(d.inspectionCategoryCn)],
    ['送检/抽样数', cellStr(d.sampleQuantity)],
    ['合格数', cellStr(d.qualifiedQuantity)],
    ['不良数', cellStr(d.unqualifiedQuantity)],
    ['合格率(%)', yieldRate(d.sampleQuantity, d.qualifiedQuantity)],
    ['检验状态', cellStr(d.inspectionStatusCn || d.inspectionStatus)],
    ['检验员', cellStr(d.inspectorName)],
    ['检验时间', cellStr(d.inspectedAt)],
    ['备注', cellStr(d.remark)]
  ]

  const itemHeaders = ['编号', '检测项', '标准值', '单位', '实测值', '结果', '缺陷等级', '备注']
  const itemRows = (items || []).map((it) => [
    cellStr(it.itemCode),
    cellStr(it.itemName),
    cellStr(it.standardValue),
    cellStr(it.unit),
    cellStr(it.measuredValue),
    cellStr(it.resultCn || it.result),
    cellStr(it.defectLevelCn || it.defectLevel),
    cellStr(it.remark)
  ])

  const sheets = [
    { name: sanitizeSheetName('质检基本信息'), headers: infoHeaders, rows: infoRows },
    { name: sanitizeSheetName('检测项明细'), headers: itemHeaders, rows: itemRows.length ? itemRows : [['', '暂无检测项', '', '', '', '', '', '']] }
  ]

  if (unitMatrix?.rows?.length) {
    const matrixHeaders = ['序号', '序列号', ...(unitMatrix.stationTitles || []), '综合判定']
    const matrixDataRows = unitMatrix.rows.map((r) => [
      cellStr(r.unitNo),
      cellStr(r.serialNo),
      ...(unitMatrix.stationIds || []).map((sid) => {
        if (r[sid] === true) return '合格'
        if (r[sid] === false) return '不合格'
        return '待检'
      }),
      r.overall === 'PASS' ? '合格' : r.overall === 'FAIL' ? '不合格' : '未完成'
    ])
    sheets.push({ name: sanitizeSheetName('逐台五步检测'), headers: matrixHeaders, rows: matrixDataRows })
  }

  if (aiReport?.aiAnalysis || aiReport?.conclusion) {
    const aiHeaders = ['章节', '内容']
    const sections = aiReport.analysisSections || {}
    const aiRows = Object.keys(sections).length
      ? Object.entries(sections).map(([k, v]) => [cellStr(k), cellStr(v)])
      : [['分析报告', cellStr(aiReport.aiAnalysis || aiReport.conclusion)]]
    sheets.push({ name: sanitizeSheetName('AI质量分析'), headers: aiHeaders, rows: aiRows })
  }

  const stamp = new Date()
  const fn = `质检报告_${sanitizeFilename(d.inspectionNo)}_${stamp.getFullYear()}${String(stamp.getMonth() + 1).padStart(2, '0')}${String(stamp.getDate()).padStart(2, '0')}.xls`
  exportExcelSheets(sheets, fn)
}

/**
 * 导出批次质检汇总表
 */
export function exportInspectionBatchExcel(rows, filename = '质检记录汇总') {
  const headers = ['质检单号', '物料/产品', '批次', '类别', '检验类型', '送检数', '合格数', '不良数', '合格率(%)', '状态', '检验时间']
  const dataRows = (rows || []).map((r) => [
    r.inspectionNo,
    r.materialName,
    r.batchNo,
    r.inspectionCategoryCn,
    r.inspectionTypeCn,
    r.sampleQuantity,
    r.qualifiedQuantity,
    r.unqualifiedQuantity,
    yieldRate(r.sampleQuantity, r.qualifiedQuantity),
    r.inspectionStatusCn,
    r.inspectedAt || r.updatedAt
  ])
  const stamp = new Date()
  const fn = `${filename}_${stamp.getFullYear()}${String(stamp.getMonth() + 1).padStart(2, '0')}${String(stamp.getDate()).padStart(2, '0')}.xls`
  exportExcelSheets([{ name: '质检汇总', headers, rows: dataRows }], fn)
}
