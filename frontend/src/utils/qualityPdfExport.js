import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'
import { APP_TITLE } from '@/constants/brand'

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

function escapeHtml(text) {
  return String(text ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function sanitizeFilename(name) {
  return String(name || 'export').replace(/[\\/:*?"<>|]/g, '_')
}

function resultLabel(result) {
  const map = {
    PASSED: '合格',
    FAILED: '不合格',
    WARNING: '警告',
    PENDING: '待检'
  }
  return map[result] || cellStr(result)
}

function buildReportHtml({ detail, items = [], aiReport = null, unitMatrix = null }) {
  const d = detail
  const stamp = new Date().toLocaleString('zh-CN')
  const yld = yieldRate(d.sampleQuantity, d.qualifiedQuantity)

  const infoRows = [
    ['质检单号', d.inspectionNo, '检验类型', d.inspectionTypeCn || d.inspectionType],
    ['产品/物料', d.materialName, '批次号', d.batchNo],
    ['工单号', d.workOrderNo, '产品类别', d.inspectionCategoryCn],
    ['送检/抽样数', d.sampleQuantity, '合格数', d.qualifiedQuantity],
    ['不良数', d.unqualifiedQuantity, '合格率', `${yld}%`],
    ['检验状态', d.inspectionStatusCn || d.inspectionStatus, '检验时间', d.inspectedAt || stamp]
  ]

  const infoTable = `
    <table class="info-table">
      ${infoRows.map((row) => `
        <tr>
          <td class="lbl">${escapeHtml(row[0])}</td><td>${escapeHtml(row[1])}</td>
          <td class="lbl">${escapeHtml(row[2])}</td><td>${escapeHtml(row[3])}</td>
        </tr>
      `).join('')}
    </table>
  `

  const itemTable = items.length ? `
    <div class="section-title">检测项明细</div>
    <table class="data-table">
      <thead>
        <tr>
          <th>编号</th><th>检测项</th><th>标准值</th><th>单位</th>
          <th>实测值</th><th>结果</th><th>缺陷等级</th><th>备注</th>
        </tr>
      </thead>
      <tbody>
        ${items.map((it) => `
          <tr>
            <td>${escapeHtml(it.itemCode)}</td>
            <td>${escapeHtml(it.itemName)}</td>
            <td>${escapeHtml(it.standardValue)}</td>
            <td>${escapeHtml(it.unit)}</td>
            <td>${escapeHtml(it.measuredValue)}</td>
            <td class="${it.result === 'PASSED' ? 'pass' : it.result === 'FAILED' ? 'fail' : ''}">
              ${escapeHtml(it.resultCn || resultLabel(it.result))}
            </td>
            <td>${escapeHtml(it.defectLevelCn || it.defectLevel)}</td>
            <td>${escapeHtml(it.remark)}</td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  ` : ''

  const matrixTable = unitMatrix?.rows?.length ? `
    <div class="section-title">逐台五步检测明细</div>
    <table class="data-table">
      <thead>
        <tr>
          <th>#</th><th>序列号</th>
          ${(unitMatrix.stationTitles || []).map((t) => `<th>${escapeHtml(t)}</th>`).join('')}
          <th>综合</th>
        </tr>
      </thead>
      <tbody>
        ${unitMatrix.rows.map((r) => `
          <tr>
            <td>${escapeHtml(r.unitNo)}</td>
            <td>${escapeHtml(r.serialNo)}</td>
            ${(unitMatrix.stationIds || []).map((sid) => {
              const v = r[sid]
              const label = v === true ? '合格' : v === false ? '不合格' : '—'
              const cls = v === true ? 'pass' : v === false ? 'fail' : ''
              return `<td class="${cls}">${label}</td>`
            }).join('')}
            <td class="${r.overall === 'PASS' ? 'pass' : r.overall === 'FAIL' ? 'fail' : ''}">
              ${r.overall === 'PASS' ? '合格' : r.overall === 'FAIL' ? '不合格' : '未完成'}
            </td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  ` : ''

  let aiBlock = ''
  if (aiReport?.aiAnalysis || aiReport?.conclusion || aiReport?.analysisSections) {
    const sections = aiReport.analysisSections || {}
    const sectionHtml = Object.keys(sections).length
      ? Object.entries(sections).map(([k, v]) => `
          <p><strong>【${escapeHtml(k)}】</strong>${escapeHtml(v)}</p>
        `).join('')
      : `<p>${escapeHtml(aiReport.aiAnalysis || aiReport.conclusion)}</p>`
    const src = aiReport.aiGenerated ? '由通义千问生成' : '系统模板生成'
    aiBlock = `
      <div class="section-title">AI 质量分析报告 <span class="ai-src">（${src}）</span></div>
      <div class="ai-report-block">${sectionHtml}</div>
    `
  }

  return `
    <div class="qc-pdf-root">
      <div class="report-header">
        <div class="report-logo">${escapeHtml(APP_TITLE)}</div>
        <div class="report-title">质量检验报告</div>
        <div class="report-no">报告编号：${escapeHtml(d.inspectionNo)} · 导出时间：${escapeHtml(stamp)}</div>
      </div>
      ${infoTable}
      ${matrixTable}
      ${itemTable}
      ${aiBlock}
      <div class="sign-row">
        <span>检验员：${escapeHtml(d.inspectorName || '_______________')}</span>
        <span>审核：_______________</span>
        <span>日期：${escapeHtml(stamp.split(' ')[0] || '')}</span>
      </div>
    </div>
  `
}

const PDF_STYLES = `
  .qc-pdf-export-wrap {
    position: fixed;
    left: -10000px;
    top: 0;
    width: 794px;
    background: #fff;
    z-index: -1;
  }
  .qc-pdf-root {
    font-family: "Microsoft YaHei", "PingFang SC", "SimSun", sans-serif;
    font-size: 12px;
    color: #000;
    padding: 24px;
    line-height: 1.5;
  }
  .report-header {
    text-align: center;
    margin-bottom: 16px;
    border-bottom: 2px solid #000;
    padding-bottom: 8px;
  }
  .report-logo { font-size: 11px; color: #555; }
  .report-title { font-size: 20px; font-weight: bold; margin: 6px 0; }
  .report-no { font-size: 11px; color: #555; }
  .info-table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 14px;
  }
  .info-table td {
    border: 1px solid #999;
    padding: 6px 8px;
  }
  .info-table .lbl {
    background: #f5f5f5;
    font-weight: bold;
    width: 14%;
  }
  .data-table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 14px;
    font-size: 11px;
  }
  .data-table th {
    background: #e8e8e8;
    border: 1px solid #999;
    padding: 5px 6px;
    text-align: center;
  }
  .data-table td {
    border: 1px solid #ccc;
    padding: 4px 6px;
    text-align: center;
  }
  .section-title {
    font-weight: bold;
    font-size: 13px;
    margin: 14px 0 8px;
    border-left: 3px solid #333;
    padding-left: 8px;
  }
  .ai-src { font-size: 11px; color: #666; font-weight: normal; }
  .ai-report-block {
    border: 1px solid #ddd;
    background: #fafafa;
    padding: 12px;
    margin-bottom: 14px;
  }
  .ai-report-block p { margin: 0 0 8px; text-align: justify; }
  .pass { color: #267326; font-weight: bold; }
  .fail { color: #cc0000; font-weight: bold; }
  .sign-row {
    margin-top: 24px;
    display: flex;
    justify-content: space-between;
    border-top: 1px solid #ccc;
    padding-top: 12px;
  }
`

async function renderElementToPdf(element, filename) {
  const canvas = await html2canvas(element, {
    scale: 2,
    useCORS: true,
    backgroundColor: '#ffffff',
    logging: false
  })

  const imgData = canvas.toDataURL('image/jpeg', 0.92)
  const pdf = new jsPDF('p', 'mm', 'a4')
  const pageWidth = pdf.internal.pageSize.getWidth()
  const pageHeight = pdf.internal.pageSize.getHeight()
  const imgWidth = pageWidth
  const imgHeight = (canvas.height * imgWidth) / canvas.width

  let heightLeft = imgHeight
  let position = 0

  pdf.addImage(imgData, 'JPEG', 0, position, imgWidth, imgHeight)
  heightLeft -= pageHeight

  while (heightLeft > 0) {
    position = heightLeft - imgHeight
    pdf.addPage()
    pdf.addImage(imgData, 'JPEG', 0, position, imgWidth, imgHeight)
    heightLeft -= pageHeight
  }

  pdf.save(filename)
}

/**
 * 导出单份质检报告 PDF（含基本信息、检测项、逐台矩阵、AI 分析）
 */
export async function exportInspectionReportPdf({ detail, items = [], aiReport = null, unitMatrix = null }) {
  if (!detail) throw new Error('暂无报告数据可导出')

  let styleEl = document.getElementById('qc-pdf-export-style')
  if (!styleEl) {
    styleEl = document.createElement('style')
    styleEl.id = 'qc-pdf-export-style'
    styleEl.textContent = PDF_STYLES
    document.head.appendChild(styleEl)
  }

  const wrap = document.createElement('div')
  wrap.className = 'qc-pdf-export-wrap'
  wrap.innerHTML = buildReportHtml({ detail, items, aiReport, unitMatrix })
  document.body.appendChild(wrap)

  try {
    const stamp = new Date()
    const fn = `质检报告_${sanitizeFilename(detail.inspectionNo)}_${stamp.getFullYear()}${String(stamp.getMonth() + 1).padStart(2, '0')}${String(stamp.getDate()).padStart(2, '0')}.pdf`
    await renderElementToPdf(wrap, fn)
  } finally {
    document.body.removeChild(wrap)
  }
}
