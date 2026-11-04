[CmdletBinding()]
param([string]$Config = "server.properties")

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if (-not (Test-Path -LiteralPath $Config)) {
    throw "Không tìm thấy $Config. Hãy chạy configure-server.ps1 trước."
}
& .\mvnw.cmd -q compile javafx:run "-Dapp.mainClass=lab5.server.ServerLauncher" "-Djavafx.args=--config=$Config"
if ($LASTEXITCODE -ne 0) { throw "Lab 5 RMI Server dừng với lỗi." }
