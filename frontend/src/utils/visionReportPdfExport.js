import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'
import { APP_TITLE } from '@/constants/brand'

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

function pct(v) {
  if (v == null || Number.isNaN(Number(v))) return '—'
  const n = Number(v)
  return n <= 1 ? `${Math.round(n * 100)}%` : `${Math.round(n)}%`
}

const PDF_STYLES = `
  .vision-pdf-wrap {
    position: fixed;
    left: -99999px;
    top: 0;
    width: 794px;
    padding: 28px 32px;
    background: #fff;
    color: #1f2937;
    font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
    font-size: 13px;
    line-height: 1.6;
    z-index: -1;
  }
  .vision-pdf-wrap h1 {
    margin: 0 0 6px;
    font-size: 22px;
    font-weight: 700;
    color: #111827;
  }
  .vision-pdf-wrap .sub {
    margin: 0 0 18px;
    color: #6b7280;
    font-size: 12px;
  }
  .vision-pdf-wrap .section-title {
    margin: 18px 0 8px;
    padding-bottom: 4px;
    border-bottom: 2px solid #dce3e8;
    font-size: 15px;
    font-weight: 600;
    color: #1f2937;
  }
  .vision-pdf-wrap table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 8px;
  }
  .vision-pdf-wrap td, .vision-pdf-wrap th {
    border: 1px solid #dce3e8;
    padding: 7px 10px;
    vertical-align: top;
    word-break: break-word;
  }
  .vision-pdf-wrap .lbl {
    width: 18%;
    background: #f8fafb;
    color: #667085;
    font-weight: 500;
  }
  .vision-pdf-wrap .pass { color: #067647; font-weight: 600; }
  .vision-pdf-wrap .fail { color: #b42318; font-weight: 600; }
  .vision-pdf-wrap .ai-sec {
    margin: 8px 0 12px;
    padding: 10px 12px;
    background: #f8fafb;
    border: 1px solid #e5eaf0;
    border-radius: 6px;
  }
  .vision-pdf-wrap .ai-sec h4 {
    margin: 0 0 6px;
    font-size: 13px;
    color: #344054;
  }
  .vision-pdf-wrap .ai-sec p {
    margin: 0;
    white-space: pre-wrap;
  }
  .vision-pdf-wrap .thumb-row {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-top: 8px;
  }
  .vision-pdf-wrap .thumb {
    width: 160px;
    border: 1px solid #dce3e8;
    border-radius: 6px;
    overflow: hidden;
    background: #0d1117;
  }
  .vision-pdf-wrap .thumb img {
    display: block;
    width: 100%;
    height: 110px;
    object-fit: contain;
    background: #0d1117;
  }
  .vision-pdf-wrap .thumb .cap {
    padding: 6px 8px;
    background: #fff;
    font-size: 11px;
    color: #475467;
  }
`

function buildHtml({ context, stats, images = [], report = null }) {
  const stamp = new Date().toLocaleString('zh-CN')
  const c = context || {}
  const s = stats || report?.stats || {}

  const info = `
    <table>
      <tr>
        <td class="lbl">质检单号</td><td>${escapeHtml(c.inspectionNo)}</td>
        <td class="lbl">产品</td><td>${escapeHtml(c.materialName)}</td>
      </tr>
      <tr>
        <td class="lbl">批次</td><td>${escapeHtml(c.batchNo)}</td>
        <td class="lbl">序列号</td><td>${escapeHtml(c.serialNo)}</td>
      </tr>
      <tr>
        <td class="lbl">检测时间</td><td>${escapeHtml(stamp)}</td>
        <td class="lbl">综合结论</td><td class="${Number(s.defectImages) > 0 ? 'fail' : 'pass'}">${escapeHtml(s.verdict || report?.conclusion || '—')}</td>
      </tr>
    </table>
  `

  const summary = `
    <table>
      <tr>
        <td class="lbl">图片总数</td><td>${escapeHtml(s.totalImages ?? images.length)}</td>
        <td class="lbl">缺陷图数</td><td class="fail">${escapeHtml(s.defectImages ?? 0)}</td>
      </tr>
      <tr>
        <td class="lbl">正常图数</td><td class="pass">${escapeHtml(s.normalImages ?? 0)}</td>
        <td class="lbl">缺陷区域</td><td>${escapeHtml(s.defectCount ?? 0)}</td>
      </tr>
      <tr>
        <td class="lbl">最高置信度</td><td>${escapeHtml(pct(s.maxConfidence))}</td>
        <td class="lbl">外观通过率</td><td>${escapeHtml(s.passRate != null ? `${s.passRate}%` : '—')}</td>
      </tr>
    </table>
  `

  const imageRows = images.map((img, i) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(img.name)}</td>
      <td class="${img.defect ? 'fail' : 'pass'}">${img.defect ? '缺陷' : '正常'}</td>
      <td>${escapeHtml(img.count ?? 0)}</td>
      <td>${escapeHtml(pct(img.maxConfidence))}</td>
      <td>${escapeHtml(img.summary || '')}</td>
    </tr>
  `).join('')

  const imageTable = `
    <table>
      <thead>
        <tr>
          <th>#</th><th>图片</th><th>结论</th><th>缺陷区域</th><th>置信度</th><th>摘要</th>
        </tr>
      </thead>
      <tbody>${imageRows || '<tr><td colspan="6">无检测结果</td></tr>'}</tbody>
    </table>
  `

  const thumbs = images
    .filter((img) => img.resultImage)
    .slice(0, 6)
    .map((img) => `
      <div class="thumb">
        <img src="${img.resultImage}" alt="${escapeHtml(img.name)}" />
        <div class="cap">${escapeHtml(img.name)} · ${img.defect ? '缺陷' : '正常'}</div>
      </div>
    `).join('')

  const sections = report?.sections || {}
  const sectionKeys = Object.keys(sections)
  const aiHtml = sectionKeys.length
    ? sectionKeys.map((k) => `
        <div class="ai-sec">
          <h4>【${escapeHtml(k)}】</h4>
          <p>${escapeHtml(sections[k])}</p>
        </div>
      `).join('')
    : `<div class="ai-sec"><p>${escapeHtml(report?.fullText || report?.summary || '暂无 AI 分析')}</p></div>`

  return `
    <h1>${escapeHtml(APP_TITLE)} · YOLO 外观检测报告</h1>
    <p class="sub">${report?.aiGenerated ? '通义千问 AI 生成' : '系统模板报告'} · 导出时间 ${escapeHtml(stamp)}</p>
    <div class="section-title">检测对象</div>
    ${info}
    <div class="section-title">汇总统计</div>
    ${summary}
    <div class="section-title">分图结果</div>
    ${imageTable}
    ${thumbs ? `<div class="section-title">标注图预览</div><div class="thumb-row">${thumbs}</div>` : ''}
    <div class="section-title">AI 分析报告</div>
    ${aiHtml}
  `
}

async function renderElementToPdf(element, filename) {
  const canvas = await html2canvas(element, {
    scale: 2,
    useCORS: true,
    backgroundColor: '#ffffff',
    logging: false
  })
  const imgData = canvas.toDataURL('image/jpeg', 0.9)
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
 * 导出 YOLO 外观检测报告 PDF
 */
export async function exportVisionReportPdf({ context, stats, images = [], report = null }) {
  if (!images.length && !report) throw new Error('暂无检测结果可导出')

  let styleEl = document.getElementById('vision-pdf-export-style')
  if (!styleEl) {
    styleEl = document.createElement('style')
    styleEl.id = 'vision-pdf-export-style'
    styleEl.textContent = PDF_STYLES
    document.head.appendChild(styleEl)
  }

  const wrap = document.createElement('div')
  wrap.className = 'vision-pdf-wrap'
  wrap.innerHTML = buildHtml({ context, stats, images, report })
  document.body.appendChild(wrap)

  try {
    const stamp = new Date()
    const no = sanitizeFilename(context?.inspectionNo || 'YOLO')
    const fn = `外观检测报告_${no}_${stamp.getFullYear()}${String(stamp.getMonth() + 1).padStart(2, '0')}${String(stamp.getDate()).padStart(2, '0')}.pdf`
    await renderElementToPdf(wrap, fn)
  } finally {
    document.body.removeChild(wrap)
  }
}
