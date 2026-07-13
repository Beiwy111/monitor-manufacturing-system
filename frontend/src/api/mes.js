import request from '@/utils/request'

export function fetchMesSnapshot() {
  return request.get('/mes/snapshot')
}

export function postMesAction(body) {
  return request.post('/mes/action', body)
}

/** 智能生成生产计划 - 批量预览 */
export function fetchSmartPlanPreview() {
  return request.get('/mes/planner/smart-plans/preview')
}

/** 智能生成生产计划 - 批量创建 */
export function postGenerateSmartPlans(body) {
  return request.post('/mes/planner/smart-plans/generate', body)
}

/** 智能派工推荐 - 预览 */
export function fetchSmartDispatchPreview(planId) {
  return request.get('/mes/manager/smart-dispatch/preview', {
    params: planId ? { planId } : {}
  })
}

/** 智能派工推荐 - 确认 */
export function postConfirmSmartDispatch(body) {
  return request.post('/mes/manager/smart-dispatch/confirm', body)
}

/** 订单附件 OCR 识别（模拟） */
export function postOrderOcrRecognize(body) {
  return request.post('/mes/order/ocr/recognize', body)
}

/** 质检报告 - 刷新/重新生成（千问 AI） */
export function postRefreshQualityReport(body) {
  return request.post('/mes/quality/reports/refresh', body)
}

/** 智能派工 - 冲突校验（静默，避免编辑时重复弹错） */
export function postValidateSmartDispatch(body) {
  return request.post('/mes/manager/smart-dispatch/validate', body, { silent: true })
}

/** 主管计划上下文（订单/工艺/交期） */
export function fetchManagerPlanContext(planNo) {
  return request.get(`/mes/manager/plan/${planNo}/context`)
}

/** 生产主管大屏完整快照 */
export function fetchDashboardSnapshot() {
  return request.get('/mes/dashboard/snapshot')
}

export function fetchDashboardKpi() {
  return request.get('/mes/dashboard/kpi')
}
