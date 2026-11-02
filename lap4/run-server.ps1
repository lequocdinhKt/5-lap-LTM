[CmdletBinding()]
param([int]$Port = 8080)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
& .\mvnw.cmd -q compile exec:java "-Dexec.mainClass=lab4.server.MiniWebServer" `
    "-Dexec.args=$Port www"
if ($LASTEXITCODE -ne 0) { throw "Lab 4 Web Server dừng với lỗi." }
