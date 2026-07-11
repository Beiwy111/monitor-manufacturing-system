# restart-backend.ps1
# Single-instance backend restart: kill whatever holds port 8088, then launch
# java -jar as a detached background process (survives closing the terminal).
# Usage: powershell -ExecutionPolicy Bypass -File restart-backend.ps1

$ErrorActionPreference = 'Stop'
$port = 8088
$dir  = $PSScriptRoot
$jar  = Join-Path $dir 'target\computer-0.0.1-SNAPSHOT.jar'

Write-Host "[1/3] Killing any process on port $port ..."
try {
  $owners = (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue).OwningProcess | Sort-Object -Unique
  foreach ($procId in $owners) {
    if ($procId) { Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue; Write-Host ("    killed PID " + $procId) }
  }
} catch { }
Start-Sleep -Seconds 2

if (-not (Test-Path $jar)) {
  Write-Host ("[!] jar not found: " + $jar)
  Write-Host "    Build it first:  .\mvnw -DskipTests clean package"
  exit 1
}

Write-Host "[2/3] Starting backend detached (independent process) ..."
Start-Process -FilePath 'java' `
  -ArgumentList '-jar', $jar `
  -WorkingDirectory $dir `
  -RedirectStandardOutput (Join-Path $dir 'app.log') `
  -RedirectStandardError  (Join-Path $dir 'app.err') `
  -WindowStyle Hidden

Write-Host "[3/3] Waiting for startup ..."
for ($i = 0; $i -lt 30; $i++) {
  Start-Sleep -Seconds 2
  $up = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
  if ($up) { Write-Host ("[OK] backend ready: http://localhost:" + $port + "  (log: backend\app.log)"); exit 0 }
}
Write-Host "[!] port $port not listening within 30s, check backend\app.err / app.log"
exit 1
