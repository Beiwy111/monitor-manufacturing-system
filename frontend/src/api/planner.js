import request from '@/utils/request'
import { postMesAction } from '@/api/mes'

export function fetchProcessSnapshot() {
  return request.get('/production/process/snapshot')
}

export function fetchOrderPlanningContext(orderId, opts = {}) {
  return request.get(`/mes/planner/order/${orderId}/context`, { silent: !!opts.silent })
}

export function postComparePlanSchemes(body) {
  return request.post('/mes/planner/schemes/compare', body)
}

export function postValidateProductionPlan(body, opts = {}) {
  return postMesAction(
    { action: 'validateProductionPlan', payload: body, operator: body.operator, roleKey: 'planner' },
    { silent: !!opts.silent }
  )
}

export function postSaveProductionPlan(body) {
  return postMesAction({ action: 'saveProductionPlan', payload: body, operator: body.operator, roleKey: 'planner' })
}

/** 多订单智能排产：按批次上限（500台）拆分为多个批次计划 */
export function postSaveBatchProductionPlans(body) {
  return postMesAction({ action: 'saveBatchProductionPlans', payload: body, operator: body.operator, roleKey: 'planner' })
}

export function postCopyProductionPlan(body) {
  return postMesAction({ action: 'copyProductionPlan', payload: body, operator: body.operator, roleKey: 'planner' })
}

export function postLoadManualPlanWizard(body) {
  return postMesAction({ action: 'loadManualPlanWizard', payload: body, operator: body.operator, roleKey: 'planner' })
}

export function postListPlanSchedules(body) {
  return postMesAction({ action: 'listPlanSchedules', payload: body, operator: body.operator, roleKey: 'planner' })
}

export function postListPlanHistory(body) {
  return postMesAction({ action: 'listPlanHistory', payload: body, operator: body.operator, roleKey: 'planner' })
}

export function postReorderProcessSteps(body) {
  return request.post('/production/process/steps/reorder', body)
}
