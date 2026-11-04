[CmdletBinding()]
param(
    [string]$MySqlHost = "127.0.0.1",
    [ValidateRange(1, 65535)][int]$MySqlPort = 3306,
    [string]$MySqlUser = "root",
    [switch]$PreserveConfig
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Write-Utf8NoBom([string]$Path, [string]$Content) {
    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $encoding)
}

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
    $candidates = @(
        "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    )
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    throw "Không tìm thấy mysql.exe. Hãy cài MySQL Server 8.x và thêm thư mục bin vào PATH."
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Chưa có Java. Hãy cài Temurin JDK 21 x64 rồi chạy lại."
}
$javaVersion = (& cmd /c "java -version 2>&1" | Select-Object -First 1) -join ""
if ($javaVersion -notmatch 'version "21\.') {
    Write-Warning "Khuyến nghị Temurin JDK 21 x64 trên cả hai máy. Hiện tại: $javaVersion"
}

$envFile = Join-Path $PSScriptRoot ".env"
$savedHost = Read-EnvValue $envFile "MYSQL_HOST"
$savedPort = Read-EnvValue $envFile "MYSQL_PORT"
$savedUser = Read-EnvValue $envFile "MYSQL_USER"
$password = Read-EnvValue $envFile "MYSQL_PASSWORD"
if ($PSBoundParameters.ContainsKey("MySqlHost") -eq $false -and -not [string]::IsNullOrWhiteSpace($savedHost)) {
    $MySqlHost = $savedHost
}
if ($PSBoundParameters.ContainsKey("MySqlPort") -eq $false -and -not [string]::IsNullOrWhiteSpace($savedPort)) {
    $MySqlPort = [int]$savedPort
}
if ($PSBoundParameters.ContainsKey("MySqlUser") -eq $false -and -not [string]::IsNullOrWhiteSpace($savedUser)) {
    $MySqlUser = $savedUser
}
if ([string]::IsNullOrWhiteSpace($password) -or $password -eq "CHANGE_ME") {
    $securePassword = Read-Host "Nhập mật khẩu MySQL local cho user $MySqlUser" -AsSecureString
    $password = (New-Object System.Net.NetworkCredential("", $securePassword)).Password
}

$mysql = Find-MySqlClient
$previousPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $password
    & $mysql "--host=$MySqlHost" "--port=$MySqlPort" "--user=$MySqlUser" --protocol=TCP --batch --skip-column-names --execute="SELECT 1;" *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Không kết nối được MySQL local tại $MySqlHost`:$MySqlPort bằng user $MySqlUser."
    }
} finally {
    $env:MYSQL_PWD = $previousPassword
}

Write-Utf8NoBom $envFile "MYSQL_HOST=$MySqlHost`nMYSQL_PORT=$MySqlPort`nMYSQL_USER=$MySqlUser`nMYSQL_PASSWORD=$password`n"
$baseUrl = "jdbc:mysql://$MySqlHost`:$MySqlPort"
$configs = @(
    @{ Path = "lap1\db.properties"; Content = "db.url=$baseUrl`ndb.name=lab1_chat`ndb.user=$MySqlUser`ndb.password=$password`n" },
    @{ Path = "lap2\db.properties"; Content = "db.url=$baseUrl`ndb.name=lab2_ftp`ndb.user=$MySqlUser`ndb.password=$password`n" },
    @{ Path = "lap3\db.properties"; Content = "url=$baseUrl/lab3_mail?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh`nuser=$MySqlUser`npassword=$password`n" }
)

foreach ($config in $configs) {
    $absolutePath = Join-Path $PSScriptRoot $config.Path
    if (-not $PreserveConfig -or -not (Test-Path -LiteralPath $absolutePath)) {
        Write-Utf8NoBom $absolutePath $config.Content
        Write-Host "Đã tạo $($config.Path)"
    } else {
        Write-Host "Giữ nguyên $($config.Path)."
    }
}

Write-Host "Đã kết nối MySQL local $MySqlHost`:$MySqlPort và tạo cấu hình Lab 1–3." -ForegroundColor Green
Write-Host "Lab 5: chạy lap5\configure-server.ps1 trên từng máy server."
Write-Host "Chạy .\verify-all.ps1 để kiểm tra build."
