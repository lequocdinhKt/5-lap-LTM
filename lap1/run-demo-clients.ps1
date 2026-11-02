[CmdletBinding()]
param(
    [string]$ServerHost = "localhost",
    [ValidateRange(1, 65535)][int]$Port = 5000,
    [ValidateRange(1, 6)][int]$ClientCount = 3,
    [string]$UsernamePrefix = "demo",
    [string]$Password = "",
    [switch]$AutoConnect,
    [switch]$Register
)

$ErrorActionPreference = "Stop"
$labDirectory = (Resolve-Path $PSScriptRoot).Path
$clientScript = Join-Path $labDirectory "run-client.ps1"

Write-Host "Đang mở $ClientCount client tới $ServerHost`:$Port ..." -ForegroundColor Cyan
for ($index = 1; $index -le $ClientCount; $index++) {
    $username = "$UsernamePrefix$index"
    $arguments = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $clientScript,
        "-ServerHost", $ServerHost,
        "-Port", $Port,
        "-Username", $username
    )
    if (-not [string]::IsNullOrWhiteSpace($Password)) { $arguments += @("-Password", $Password) }
    if ($AutoConnect) { $arguments += "-AutoConnect" }
    if ($Register) { $arguments += "-Register" }
    Start-Process -FilePath "powershell.exe" -WorkingDirectory $labDirectory -ArgumentList $arguments
    # Giãn nhịp mở cửa sổ để Maven Wrapper không tranh chấp cùng lúc.
    Start-Sleep -Milliseconds 600
}

if ($AutoConnect) {
    Write-Host "Đã mở $ClientCount cửa sổ và tự đăng nhập tài khoản $UsernamePrefix<STT>." -ForegroundColor Green
} else {
    Write-Host "Đã mở $ClientCount cửa sổ. Mỗi client được điền sẵn tên $UsernamePrefix<STT>; nhập mật khẩu rồi đăng nhập." -ForegroundColor Green
}
