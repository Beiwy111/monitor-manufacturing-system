const MS_DAY = 24 * 60 * 60 * 1000

function parseDate(value) {
  if (!value) return null
  const d = new Date(String(value).replace(/-/g, '/'))
  return Number.isNaN(d.getTime()) ? null : d
}

function daysUntil(dateStr) {
  const d = parseDate(dateStr)
  if (!d) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  d.setHours(0, 0, 0, 0)
  return Math.round((d - today) / MS_DAY)
}

function nearlyEqual(a, b, tolerance = 1) {
  return Math.abs(Number(a) - Number(b)) <= tolerance
}

export function findBomProduct(order, bomGuide) {
  const products = bomGuide?.products || []
  if (!products.length || !order) return null
  if (order.materialCode) {
    const byCode = products.find((p) => p.productCode === order.materialCode)
    if (byCode) return byCode
  }
  const model = String(order.productModel || '').trim()
  if (!model) return null
  return products.find((p) => p.productName === model || model.includes(p.productName) || p.productName.includes(model))
}

export function findProcessGuide(order, processGuide) {
  if (!order || !processGuide) return null
  const keys = [order.materialCode, order.productModel].filter(Boolean)
  for (const key of keys) {
    if (processGuide[key]) return processGuide[key]
  }
  return null
}

export function buildAuditChecklist(order, bomGuide, processGuide) {
  const bom = findBomProduct(order, bomGuide)
  const process = findProcessGuide(order, processGuide)
  const qty = Number(order?.quantity) || 0
  const amount = Number(order?.amount) || 0
  const unitPrice = Number(order?.unitPrice) || 0
  const expectedAmount = unitPrice > 0 && qty > 0 ? unitPrice * qty : 0
  const deliveryDays = daysUntil(order?.deliveryDate)
  const attachments = order?.attachments || []

  const completenessOk = Boolean(
    order?.customerName && order?.productModel && qty > 0 && amount > 0 && order?.deliveryDate
  )

  const modelOk = Boolean(bom || order?.materialCode)
  const amountOk = expectedAmount > 0 ? nearlyEqual(amount, expectedAmount, 0.5) : amount > 0
  const deliveryOk = deliveryDays != null && deliveryDays >= 7
  const bomOk = Boolean(bom?.components?.length)
  const processOk = Boolean(process?.steps?.length)

  return [
    {
      key: 'completeness',
      label: '信息完整性',
      status: completenessOk ? 'pass' : 'fail',
      message: completenessOk ? '客户、型号、数量、金额、交期均已填写' : '存在必填字段缺失'
    },
    {
      key: 'model',
      label: '型号有效性',
      status: modelOk ? 'pass' : 'fail',
      message: modelOk
        ? `已匹配成品 ${bom?.productCode || order.materialCode}`
        : '未在 BOM 成品库中找到对应型号'
    },
    {
      key: 'amount',
      label: '金额一致性',
      status: amountOk ? 'pass' : 'fail',
      message: amountOk
        ? (expectedAmount > 0 ? `数量×单价=${expectedAmount.toFixed(2)}，与订单金额一致` : '订单金额已填写')
        : `订单金额 ${amount} 与 数量×单价 ${expectedAmount.toFixed(2)} 不一致`
    },
    {
      key: 'delivery',
      label: '交期合理性',
      status: deliveryOk ? 'pass' : (deliveryDays != null && deliveryDays >= 0 ? 'warn' : 'fail'),
      message: deliveryDays == null
        ? '交期格式无效'
        : deliveryDays < 0
          ? '交期已过期'
          : deliveryDays < 7
            ? `距交期仅 ${deliveryDays} 天，偏紧`
            : `距交期 ${deliveryDays} 天，排产窗口充足`
    },
    {
      key: 'bom',
      label: 'BOM 是否存在',
      status: bomOk ? 'pass' : 'fail',
      message: bomOk ? `已配置 ${bom.components.length} 项物料` : '未找到有效 BOM 清单'
    },
    {
      key: 'process',
      label: '工艺路线是否存在',
      status: processOk ? 'pass' : 'fail',
      message: processOk ? `共 ${process.steps.length} 道工序` : '未配置工艺路线'
    },
    {
      key: 'attachment',
      label: '附件资料（可选）',
      status: attachments.length ? 'pass' : 'warn',
      message: attachments.length ? `已上传 ${attachments.length} 份附件` : '无附件，可凭系统字段审核'
    }
  ]
}

export function detectAuditRisks(order, bomGuide, processGuide) {
  const risks = []
  const checklist = buildAuditChecklist(order, bomGuide, processGuide)
  const qty = Number(order?.quantity) || 0
  const amount = Number(order?.amount) || 0
  const unitPrice = Number(order?.unitPrice) || 0
  const deliveryDays = daysUntil(order?.deliveryDate)
  const attachments = order?.attachments || []

  checklist.filter((c) => c.status === 'fail').forEach((c) => {
    risks.push({ level: 'danger', code: c.key, label: c.label, detail: c.message })
  })

  if (deliveryDays != null && deliveryDays >= 0 && deliveryDays < 14) {
    risks.push({
      level: 'warning',
      code: 'short_delivery',
      label: '交期过短',
      detail: `要求交期距今 ${deliveryDays} 天，建议核对产能与物料齐套`
    })
  }

  if (qty > 500 || (qty > 0 && qty < 5)) {
    risks.push({
      level: 'warning',
      code: 'abnormal_qty',
      label: '数量异常',
      detail: `订单数量 ${qty} 台，偏离常规批量区间（5~500）`
    })
  }

  if (unitPrice > 0 && qty > 0 && !nearlyEqual(amount, unitPrice * qty, 0.5)) {
    risks.push({
      level: 'danger',
      code: 'amount_error',
      label: '金额错误',
      detail: `系统金额 ${amount} 与 ${qty}×${unitPrice} 不符`
    })
  }

  if (!attachments.length && (order?.remark || '').includes('合同')) {
    risks.push({
      level: 'warning',
      code: 'missing_attachment',
      label: '资料缺失',
      detail: '备注提及合同但未上传附件'
    })
  }

  if (!findBomProduct(order, bomGuide)) {
    risks.push({
      level: 'danger',
      code: 'unknown_model',
      label: '型号未建档',
      detail: '成品 BOM 中无此型号，无法展开物料与工艺'
    })
  }

  if (order?.auditFlag === '待补充资料') {
    risks.push({ level: 'warning', code: 'supplement', label: '待补充资料', detail: order.auditOpinion || '上次审核要求补充资料' })
  }
  if (order?.auditFlag === '暂缓审核') {
    risks.push({ level: 'info', code: 'defer', label: '暂缓审核', detail: order.auditOpinion || '订单已标记暂缓' })
  }

  const seen = new Set()
  return risks.filter((r) => {
    const key = `${r.code}-${r.label}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

export function compareOcrWithOrder(order, ocrFields) {
  if (!ocrFields) return []
  const rows = [
    { field: 'customerName', label: '客户名称', order: order?.customerName, ocr: ocrFields.customerName },
    { field: 'productModel', label: '产品型号', order: order?.productModel, ocr: ocrFields.productModel },
    { field: 'quantity', label: '数量', order: order?.quantity, ocr: ocrFields.quantity },
    { field: 'deliveryDate', label: '交期', order: order?.deliveryDate, ocr: ocrFields.deliveryDate },
    { field: 'amount', label: '金额', order: order?.amount, ocr: ocrFields.amount }
  ]

  return rows.map((row) => {
    let match = false
    if (row.field === 'quantity') {
      match = nearlyEqual(row.order, row.ocr, 0)
    } else if (row.field === 'amount') {
      match = nearlyEqual(row.order, row.ocr, 1)
    } else if (row.field === 'productModel') {
      const a = String(row.order || '')
      const b = String(row.ocr || '')
      match = a && b && (a.includes(b) || b.includes(a))
    } else {
      match = String(row.order || '').trim() === String(row.ocr || '').trim()
    }
    return { ...row, match }
  })
}

export function formatCurrency(value) {
  const n = Number(value)
  if (Number.isNaN(n)) return '-'
  return `¥${n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

export const AUDIT_ACTIONS = [
  { action: 'pass', label: '审核通过', type: 'primary', needReason: false },
  { action: 'reject', label: '驳回作废', type: 'danger', needReason: true },
  { action: 'supplement', label: '要求补充资料', type: 'warning', needReason: true },
  { action: 'defer', label: '暂缓审核', type: 'info', needReason: true }
]
