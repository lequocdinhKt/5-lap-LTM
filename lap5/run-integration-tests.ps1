$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Read-EnvValue([string]$Path, [string]$Name) {
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

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $repositoryRoot ".env"
if (-not (Test-Path -LiteralPath $envFile)) { throw "Thiếu .env. Hãy chạy setup-windows.ps1 trước." }
$hostName = Read-EnvValue $envFile "MYSQL_HOST"
$port = Read-EnvValue $envFile "MYSQL_PORT"
$user = Read-EnvValue $envFile "MYSQL_USER"
$password = Read-EnvValue $envFile "MYSQL_PASSWORD"
if ([string]::IsNullOrWhiteSpace($hostName)) { $hostName = "127.0.0.1" }
if ([string]::IsNullOrWhiteSpace($port)) { $port = "3306" }
if ([string]::IsNullOrWhiteSpace($user)) { $user = "root" }
if ($null -eq $password) { throw "Thiếu MYSQL_PASSWORD trong .env." }

$mysql = Find-MySqlClient
$schema = Get-Content -Raw "scripts\schema.sql"
$seed = Get-Content -Raw "scripts\demo-seed.sql"
$previousPassword = $env:MYSQL_PWD
$env:MYSQL_PWD = $password
try {
    foreach ($name in @("lab5_test_a", "lab5_test_b")) {
        $sql = ($schema -replace 'lab5_warehouse', $name) + "`n" + ($seed -replace 'lab5_warehouse', $name)
        $sql | & $mysql "--host=$hostName" "--port=$port" "--user=$user" --protocol=TCP --default-character-set=utf8mb4
        if ($LASTEXITCODE -ne 0) { throw "Không tạo được database tạm $name trên MySQL local." }
    }

    $env:LAB5_INTEGRATION = "true"
    $env:LAB5_TEST_DB_USER = $user
    $env:LAB5_TEST_DB_PASSWORD = $password
    $env:LAB5_TEST_DB_URL_A = "jdbc:mysql://$hostName`:$port/lab5_test_a?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh"
    $env:LAB5_TEST_DB_URL_B = "jdbc:mysql://$hostName`:$port/lab5_test_b?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh"

    & .\mvnw.cmd -q test "-Dtest=lab5.integration.TwoServerRmiIntegrationTest"
    if ($LASTEXITCODE -ne 0) { throw "Integration test Lab 5 thất bại." }
} finally {
    "DROP DATABASE IF EXISTS lab5_test_a; DROP DATABASE IF EXISTS lab5_test_b;" |
        & $mysql "--host=$hostName" "--port=$port" "--user=$user" --protocol=TCP
    $env:MYSQL_PWD = $previousPassword
    Remove-Item Env:LAB5_INTEGRATION, Env:LAB5_TEST_DB_USER, Env:LAB5_TEST_DB_PASSWORD,
        Env:LAB5_TEST_DB_URL_A, Env:LAB5_TEST_DB_URL_B -ErrorAction SilentlyContinue
}

Write-Host "Integration test hai RMI server trên MySQL local đã thành công." -ForegroundColor Green
