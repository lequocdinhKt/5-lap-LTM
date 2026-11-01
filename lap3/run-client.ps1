[CmdletBinding()]
param(
    [string]$ServerHost = "127.0.0.1",
    [switch]$EmbeddedMailServer
)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$env:LAB3_LOCAL_MAIL_HOST = $ServerHost
$env:LAB3_EMBEDDED_MAIL_SERVER = if ($EmbeddedMailServer) { "true" } else { "false" }
& .\mvnw.cmd -q compile javafx:run
if ($LASTEXITCODE -ne 0) { throw "Lab 3 Mail Client dừng với lỗi." }
