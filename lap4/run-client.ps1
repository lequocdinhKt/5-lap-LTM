$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
& .\mvnw.cmd -q compile exec:java "-Dexec.mainClass=lab4.client.MiniBrowser"
if ($LASTEXITCODE -ne 0) { throw "Lab 4 Mini Browser dừng với lỗi." }
