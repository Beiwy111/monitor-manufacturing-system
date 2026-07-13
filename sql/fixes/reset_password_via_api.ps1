# 当 mysql 命令行无法连接时，通过已运行的后端 API 批量重置密码为 123456
# 前提：后端已启动 (http://localhost:8088)，且能用 admin / Mes@2026 登录（首次重置）
# 用法：powershell -ExecutionPolicy Bypass -File sql/fixes/reset_password_via_api.ps1

$base = 'http://localhost:8088'
$oldPwd = 'Mes@2026'
$newPwd = '123456'

Write-Host "登录 admin ..."
$login = Invoke-RestMethod -Uri "$base/auth/login" -Method POST -ContentType 'application/json' -Body (@{ username = 'admin'; password = $oldPwd } | ConvertTo-Json)
if ($login.code -ne 200) {
  # 若已重置过，尝试用新密码登录
  $login = Invoke-RestMethod -Uri "$base/auth/login" -Method POST -ContentType 'application/json' -Body (@{ username = 'admin'; password = $newPwd } | ConvertTo-Json)
}
if ($login.code -ne 200) { throw "admin 登录失败，请检查后端与账号" }

$users = @(Invoke-RestMethod -Uri "$base/system/user/list") | Where-Object { $_.status -eq 1 }
$ok = 0
foreach ($u in $users) {
  $body = @{
    action   = 'resetUserPassword'
    payload  = @{ userId = $u.userId; password = $newPwd }
    operator = 'admin'
    roleKey  = 'admin'
  } | ConvertTo-Json -Depth 5
  $r = Invoke-RestMethod -Uri "$base/mes/action" -Method POST -ContentType 'application/json' -Body $body
  if ($r.code -eq 200) { $ok++; Write-Host "  $($u.username)" }
}
Write-Host "完成：$ok / $($users.Count) 个启用用户密码已设为 $newPwd"
