import request from '@/utils/request'

export function getCustomerDashboard() {
  return request.get('/customer/portal/dashboard')
}

export function getCustomerOrders() {
  return request.get('/customer/portal/orders')
}

export function getCustomerOrderDetail(orderId) {
  return request.get(`/customer/portal/orders/${orderId}`)
}

export function createCustomerOrder(data) {
  return request.post('/customer/portal/orders', data)
}

export function getCustomerProducts() {
  return request.get('/customer/portal/products')
}

export function getCustomerFeedbacks() {
  return request.get('/customer/portal/feedbacks')
}

export function submitCustomerFeedback(data) {
  return request.post('/customer/portal/feedbacks', data)
}

export function getCustomerProfile() {
  return request.get('/customer/portal/profile')
}

export function updateCustomerProfile(data) {
  return request.put('/customer/portal/profile', data)
}

export function uploadCustomerFile(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/customer/portal/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
