[CmdletBinding()]
param([string]$BindAddress = "0.0.0.0")
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
& .\mvnw.cmd -q compile exec:java "-Dexec.mainClass=lab3.network.LocalMailServerMain" `
    "-Dexec.args=$BindAddress"
if ($LASTEXITCODE -ne 0) { throw "Lab 3 Local Mail Server dừng với lỗi." }
