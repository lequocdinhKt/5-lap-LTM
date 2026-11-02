[CmdletBinding()]
param(
    [string]$ServerHost = "127.0.0.1",
    [ValidateRange(1, 4)][int]$ClientCount = 2,
    [switch]$EmbeddedMailServer
)

$ErrorActionPreference = "Stop"
if ($EmbeddedMailServer -and $ClientCount -gt 1) {
    throw "Chỉ mở 1 client khi dùng -EmbeddedMailServer vì mỗi client sẽ chiếm port 3025/3110."
}

$labDirectory = (Resolve-Path $PSScriptRoot).Path
$clientScript = Join-Path $labDirectory "run-client.ps1"

Write-Host "Đang mở $ClientCount mail client tới mail server $ServerHost ..." -ForegroundColor Cyan
for ($index = 1; $index -le $ClientCount; $index++) {
    $arguments = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $clientScript,
        "-ServerHost", $ServerHost
    )
    if ($EmbeddedMailServer) { $arguments += "-EmbeddedMailServer" }
    Start-Process -FilePath "powershell.exe" -WorkingDirectory $labDirectory -ArgumentList $arguments
    Write-Host "  → cửa sổ client $index" -ForegroundColor DarkGray
    Start-Sleep -Milliseconds 800
}

Write-Host "Đã mở $ClientCount cửa sổ. Đăng nhập tài khoản ứng dụng, rồi gửi và đọc thư bằng SMTP/POP3." -ForegroundColor Green