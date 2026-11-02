$ErrorActionPreference = "Stop"
$taskRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

& (Join-Path $taskRoot "run-server.ps1")
Start-Sleep -Seconds 2

foreach ($username in @("An", "Binh", "Chi", "Dung", "Ha", "Lan")) {
    & (Join-Path $taskRoot "run-client.ps1") -ServerAddress "localhost" `
        -Username $username -Password "lab1-$username"
}

Write-Host "Da mo 6 client. Lan dau bam Dang ky, cac lan sau bam Dang nhap." -ForegroundColor Green
