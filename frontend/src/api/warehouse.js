import request from '@/utils/request'

export function fetchWarehouseInventory() {
  return request.get('/warehouse/inventory/list')
}

export function fetchWarehouseCatalog() {
  return request.get('/warehouse/inventory/catalog')
}

export function fetchWarehouseTransactions() {
  return request.get('/warehouse/transactions/list')
}

export function fetchBarcodeRules() {
  return request.get('/warehouse/barcode/rules')
}

export function saveBarcodeRule(rule) {
  return request.post('/warehouse/barcode/rules', rule)
}

export function generateBarcode(body) {
  return request.post('/warehouse/barcode/generate', body)
}

export function scanBarcode(body, operator) {
  return request.post('/warehouse/barcode/scan', body, {
    params: operator ? { operator } : {}
  })
}

export function fetchBarcodeList() {
  return request.get('/warehouse/barcode/list')
}

export function fetchBarcodeTrace(barcodeNo) {
  return request.get(`/warehouse/barcode/trace/${encodeURIComponent(barcodeNo)}`)
}

export function fetchScanLogs() {
  return request.get('/warehouse/scan/logs')
}
