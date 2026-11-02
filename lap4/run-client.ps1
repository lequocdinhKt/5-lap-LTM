$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
& .\mvnw.cmd -q compile javafx:run
if ($LASTEXITCODE -ne 0) { throw "Lab 4 Mini Browser dừng với lỗi." }
