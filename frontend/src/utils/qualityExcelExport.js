import { exportExcelSheets } from '@/utils/excelExport'

function yieldRate(sample, qualified) {
  const s = Number(sample)
  const q = Number(qualified)
  if (!s || s <= 0) return '0.0'
  return Math.min(100, Math.max(0, Math.round((q / s) * 1000) / 10)).toFixed(1)
}

/**
 * 导出单份质检报告 Excel（含基本信息 + 检测项明细 + AI 分析）
 */
export function exportInspectionReportExcel({ detail, items = [], aiReport = null }) {
  if (!detail) return
  const d = detail
  const infoHeaders = ['字段', '值']
  const infoRows = [
    ['质检单号', d.inspectionNo || ''],
    ['检验类型', d.inspectionTypeCn || d.inspectionType || ''],
    ['产品/物料', d.materialName || ''],
    ['批次号', d.batchNo || ''],
    ['工单号', d.workOrderNo || ''],
    ['产品类别', d.inspectionCategoryCn || ''],
    ['送检/抽样数', d.sampleQuantity ?? ''],
    ['合格数', d.qualifiedQuantity ?? ''],
    ['不良数', d.unqualifiedQuantity ?? ''],
    ['合格率(%)', yieldRate(d.sampleQuantity, d.qualifiedQuantity)],
    ['检验状态', d.inspectionStatusCn || ''],
    ['检验员', d.inspectorName || ''],
    ['检验时间', d.inspectedAt || ''],
    ['备注', d.remark || '']
  ]

  const itemHeaders = ['编号', '检测项', '标准值', '单位', '实测值', '结果', '缺陷等级', '备注']
  const itemRows = (items || []).map((it) => [
    it.itemCode || '',
    it.itemName || '',
    it.standardValue || '',
    it.unit || '',
    it.measuredValue || '',
    it.resultCn || it.result || '',
    it.defectLevelCn || it.defectLevel || '',
    it.remark || ''
  ])

  const sheets = [
    { name: '质检基本信息', headers: infoHeaders, rows: infoRows },
    { name: '检测项明细', headers: itemHeaders, rows: itemRows.length ? itemRows : [['', '暂无检测项', '', '', '', '', '', '']] }
  ]

  if (aiReport?.aiAnalysis || aiReport?.conclusion) {
    const aiHeaders = ['章节', '内容']
    const sections = aiReport.analysisSections || {}
    const aiRows = Object.keys(sections).length
      ? Object.entries(sections).map(([k, v]) => [k, v])
      : [['分析报告', aiReport.aiAnalysis || aiReport.conclusion || '']]
    sheets.push({ name: 'AI质量分析', headers: aiHeaders, rows: aiRows })
  }

  const stamp = new Date()
  const fn = `质检报告_${d.inspectionNo || 'export'}_${stamp.getFullYear()}${String(stamp.getMonth() + 1).padStart(2, '0')}${String(stamp.getDate()).padStart(2, '0')}.xls`
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
