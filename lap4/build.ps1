$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
& .\mvnw.cmd -q clean package
if ($LASTEXITCODE -ne 0) { throw "Build Lab 4 thất bại." }
Write-Host "Build Lab 4 thành công."
