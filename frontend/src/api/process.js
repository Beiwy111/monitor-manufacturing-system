import request from '@/utils/request'

export function fetchProcessSnapshot() {
  return request.get('/production/process/snapshot')
}

export function saveProcessRoute(route, operator) {
  return request.post('/production/process/routes', route, {
    params: operator ? { operator } : {}
  })
}

export function disableProcessRoute(routeId) {
  return request.delete(`/production/process/routes/${routeId}`)
}

export function saveProcessStep(step) {
  return request.post('/production/process/steps', step)
}

export function disableProcessStep(stepId) {
  return request.delete(`/production/process/steps/${stepId}`)
}
