[CmdletBinding()]
param(
    [switch]$SkipDocker,
    [switch]$PreserveConfig
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Write-Utf8NoBom([string]$Path, [string]$Content) {
    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $encoding)
}

function New-LocalPassword {
    $alphabet = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    $bytes = New-Object byte[] 24
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
    } finally {
        $rng.Dispose()
    }
    return -join ($bytes | ForEach-Object { $alphabet[$_ % $alphabet.Length] })
}

function Read-EnvValue([string]$Path, [string]$Name) {
    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }
    $line = Get-Content -LiteralPath $Path | Where-Object { $_ -match "^$([regex]::Escape($Name))=" } | Select-Object -First 1
    if ($null -eq $line) {
        return $null
    }
    return $line.Substring($line.IndexOf('=') + 1).Trim()
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Chưa có Java. Hãy cài Temurin JDK 21 x64 rồi chạy lại."
}

$javaVersion = (& java -version 2>&1 | Select-Object -First 1) -join ""
if ($javaVersion -notmatch 'version "21\.') {
    Write-Warning "Khuyến nghị Temurin JDK 21 x64 trên cả hai máy. Hiện tại: $javaVersion"
}

$envFile = Join-Path $PSScriptRoot ".env"
$password = Read-EnvValue $envFile "MYSQL_ROOT_PASSWORD"
$port = Read-EnvValue $envFile "MYSQL_PORT"
if ([string]::IsNullOrWhiteSpace($password) -or $password -eq "CHANGE_ME") {
    if ($SkipDocker) {
        $securePassword = Read-Host "Nhập mật khẩu MySQL root cài trực tiếp" -AsSecureString
        $password = [System.Net.NetworkCredential]::new("", $securePassword).Password
        if ([string]::IsNullOrWhiteSpace($password)) {
            throw "Mật khẩu MySQL không được để trống."
        }
    } else {
        $password = New-LocalPassword
    }
}
if ([string]::IsNullOrWhiteSpace($port)) {
    $port = "3306"
}
Write-Utf8NoBom $envFile "MYSQL_ROOT_PASSWORD=$password`nMYSQL_PORT=$port`n"

$configs = @(
    @{
        Path = "lap1\db.properties"
        Content = "db.url=jdbc:mysql://localhost:$port`ndb.name=lab1_chat`ndb.user=root`ndb.password=$password`n"
    },
    @{
        Path = "lap2\db.properties"
        Content = "db.url=jdbc:mysql://localhost:$port`ndb.name=lab2_ftp`ndb.user=root`ndb.password=$password`n"
    },
    @{
        Path = "lap3\db.properties"
        Content = "url=jdbc:mysql://localhost:$port/lab3_mail?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh`nuser=root`npassword=$password`n"
    }
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

if (-not $SkipDocker) {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Chưa có Docker Desktop. Cài Docker Desktop hoặc chạy với -SkipDocker nếu dùng MySQL cài trực tiếp."
    }
    & docker compose up -d
    if ($LASTEXITCODE -ne 0) {
        throw "Không khởi động được MySQL. Kiểm tra Docker Desktop và cổng $port."
    }
    Write-Host "MySQL đang khởi động. Xem trạng thái bằng: docker compose ps"
}

Write-Host "Thiết lập hoàn tất. Chạy .\verify-all.ps1 để kiểm tra build."
