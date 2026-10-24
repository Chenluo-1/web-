# Install MinIO as a Windows service (run elevated).
# python + pywin32 host the service; it launches D:\ecom\minio\minio.exe.
$ErrorActionPreference = "Continue"
$log = "D:\ecom\minio\logs\install.log"
New-Item -ItemType Directory -Force -Path "D:\ecom\minio\logs" | Out-Null
$py  = "D:\Anaconda Prompt\Anaconda3\python.exe"
$svc = "D:\webxiangmu\ecom\deploy\minio_service.py"

"=== $(Get-Date -Format s) install ===" | Out-File $log -Append -Encoding utf8

# 1) stop any user-mode instance so port 9000 is free
Get-Process minio -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 2

# 2) register (idempotent: update if already installed)
& $py $svc --startup auto update *>> $log
if ($LASTEXITCODE -ne 0) { & $py $svc --startup auto install *>> $log }

# 3) auto-restart on crash
sc.exe failure ecom-minio reset= 86400 actions= restart/5000 *>> $log

# 4) start and report
Start-Service ecom-minio *>> $log
(sc.exe qc ecom-minio) *>> $log
(Get-Service ecom-minio | Format-List Name,Status,StartType) *>> $log
"=== done ===" | Out-File $log -Append -Encoding utf8
