$base = "http://localhost:8088"
$r = Invoke-RestMethod -Uri "$base/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"purchase01","password":"123456"}'
$h = @{ Authorization = "Bearer $($r.data.token)" }
Write-Host "LOGIN: code=$($r.code)"

# AI document confirm (mock path - tests the fix of item_status PENDING)
$body = '{"supplierName":"TestSupplier","paymentMethod":"Net30","items":[{"materialName":"LCD Panel","materialCode":"MAT-001","specification":"15.6 inch","unitPrice":280.00,"quantity":10,"deliveryDate":"2026-08-01","remark":"smoke"}]}'
$conf = Invoke-RestMethod -Uri "$base/purchase/ai/document/confirm" -Method POST -Headers $h -ContentType "application/json" -Body $body
Write-Host "AI_CONFIRM: code=$($conf.code) orderNo=$($conf.data.purchaseOrderNo) status=$($conf.data.status)"

# verify the order item has PENDING status (not DRAFT)
$items = Invoke-RestMethod -Uri "$base/purchase/purchaseOrderItem/list" -Headers $h
$newItem = $items | Where-Object { $_.purchaseOrderId -eq $conf.data.purchaseOrderId } | Select-Object -First 1
Write-Host "ITEM_STATUS: $($newItem.itemStatus) (expect PENDING)"

# workbench list - verify all paths still work after restart
$lst = Invoke-RestMethod -Uri "$base/purchase/workbench/list?status=PENDING" -Headers $h
Write-Host "WORKBENCH_LIST: code=$($lst.code) pendingCount=$($lst.data.Count)"

# supplier active
$sup = Invoke-RestMethod -Uri "$base/purchase/supplier/active" -Headers $h
Write-Host "SUPPLIER_ACTIVE: code=$($sup.code) count=$($sup.data.Count)"

Write-Host "--- DONE ---"
