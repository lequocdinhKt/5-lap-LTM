[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateSet("A", "B")][string]$Role,
    [Parameter(Mandatory = $true)][string]$LocalHost,
    [Parameter(Mandatory = $true)][string]$PeerHost,
    [switch]$SeedDemo
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Read-EnvValue([string]$Path, [string]$Name) {
    if (-not (Test-Path -LiteralPath $Path)) { return $null }
    $line = Get-Content -LiteralPath $Path |
        Where-Object { $_ -match "^$([regex]::Escape($Name))=" } |
        Select-Object -First 1
    if ($null -eq $line) { return $null }
    return $line.Substring($line.IndexOf('=') + 1).Trim()
}

function Find-MySqlClient {
    $command = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($null -ne $command) { return $command.Source }
    foreach ($candidate in @(
        "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe")) {
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    throw "Không tìm thấy mysql.exe. Hãy cài MySQL Server 8.x trước."
}

function Write-Utf8NoBom([string]$Path, [string]$Content) {
    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $encoding)
}

if ($LocalHost -notmatch '^(?:\d{1,3}\.){3}\d{1,3}$' -or $PeerHost -notmatch '^(?:\d{1,3}\.){3}\d{1,3}$') {
    throw "LocalHost và PeerHost phải là địa chỉ IPv4."
}

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $repositoryRoot ".env"
if (-not (Test-Path -LiteralPath $envFile)) { throw "Thiếu .env. Hãy chạy setup-windows.ps1 trước." }
$mysqlHost = Read-EnvValue $envFile "MYSQL_HOST"
$mysqlPort = Read-EnvValue $envFile "MYSQL_PORT"
$mysqlUser = Read-EnvValue $envFile "MYSQL_USER"
$password = Read-EnvValue $envFile "MYSQL_PASSWORD"
if ([string]::IsNullOrWhiteSpace($mysqlHost)) { $mysqlHost = "127.0.0.1" }
if ([string]::IsNullOrWhiteSpace($mysqlPort)) { $mysqlPort = "3306" }
if ([string]::IsNullOrWhiteSpace($mysqlUser)) { $mysqlUser = "root" }
if ($null -eq $password) { throw "Thiếu MYSQL_PASSWORD trong .env. Hãy chạy setup-windows.ps1." }

$serverId = if ($Role -eq "A") { "server-a" } else { "server-b" }
$coordinator = if ($Role -eq "A") { $LocalHost } else { $PeerHost }
$content = @"
server.id=$serverId
server.host=$LocalHost
rmi.registry.port=1099
rmi.service.port=1100
peer.server.host=$PeerHost
peer.rmi.port=1099
lock.coordinator.host=$coordinator
lock.coordinator.port=1099
db.url=jdbc:mysql://$mysqlHost`:$mysqlPort/lab5_warehouse?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh
db.username=$mysqlUser
db.password=$password
lock.lease.seconds=30
"@
Write-Utf8NoBom (Join-Path $PSScriptRoot "server.properties") $content

$mysql = Find-MySqlClient
$previousPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $password
    Get-Content -Raw (Join-Path $PSScriptRoot "scripts\schema.sql") |
        & $mysql "--host=$mysqlHost" "--port=$mysqlPort" "--user=$mysqlUser" --protocol=TCP --default-character-set=utf8mb4
    if ($LASTEXITCODE -ne 0) { throw "Không tạo được schema Lab 5 trên MySQL local." }
    if ($SeedDemo) {
        Get-Content -Raw (Join-Path $PSScriptRoot "scripts\demo-seed.sql") |
            & $mysql "--host=$mysqlHost" "--port=$mysqlPort" "--user=$mysqlUser" --protocol=TCP --default-character-set=utf8mb4
        if ($LASTEXITCODE -ne 0) { throw "Không nạp được dữ liệu demo Lab 5." }
    }
} finally {
    $env:MYSQL_PWD = $previousPassword
}

Write-Host "Đã cấu hình $serverId tại $LocalHost; peer $PeerHost; MySQL local $mysqlHost`:$mysqlPort." -ForegroundColor Green
Write-Host "Chạy .\run-server.ps1 để mở dashboard server."
