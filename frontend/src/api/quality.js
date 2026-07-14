import request from '@/utils/request'

// ─── 查询视图 ───────────────────────────────────────────────
export function fetchInspectionViews() {
  return request.get('/quality/inspection/views')
}

export function fetchInspectionDetail(inspectionId) {
  return request.get('/quality/inspection/detail', { params: { inspectionId } })
}

export function fetchNonconformingViews() {
  return request.get('/quality/nonconforming/views')
}

export function fetchRecheckViews() {
  return request.get('/quality/recheck/views')
}

export function fetchQualityKpi() {
  return request.get('/quality/kpi')
}

// ─── 检测项 ─────────────────────────────────────────────────
export function fetchInspectionItems(inspectionId) {
  return request.get('/quality/inspection/items', { params: { inspectionId } })
}

export function generateDefaultItems(inspectionId) {
  return request.post('/quality/inspection/generateDefaultItems', { inspectionId })
}

export function saveInspectionItems(inspectionId, items) {
  return request.post('/quality/inspection/items/save', { inspectionId, items })
}

export function evaluateInspection(inspectionId) {
  return request.post('/quality/inspection/evaluate', { inspectionId })
}

// ─── 质检状态流转 ────────────────────────────────────────────
export function passInspection(data) {
  return request.post('/quality/inspection/pass', data)
}

export function failInspection(data) {
  return request.post('/quality/inspection/fail', data)
}

export function requireRecheck(data) {
  return request.post('/quality/inspection/recheck', data)
}

export function recheckPass(data) {
  return request.post('/quality/inspection/recheckPass', data)
}

export function recheckFail(data) {
  return request.post('/quality/inspection/recheckFail', data)
}

export function closeInspection(data) {
  return request.post('/quality/inspection/close', data)
}

// ─── 不良品处置 ──────────────────────────────────────────────
export function handleNonconforming(data) {
  return request.post('/quality/nonconforming/handle', data)
}

export function createIncomingInspection(data) {
  return request.post('/quality/inspection/createIncoming', data)
}

export function updateInspectionSampling(data) {
  return request.post('/quality/inspection/updateSampling', data)
}

/** YOLO 智能外观检测：上传图片，返回缺陷类型与标注结果 */
export function detectAppearance(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/quality/vision/detect', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}
