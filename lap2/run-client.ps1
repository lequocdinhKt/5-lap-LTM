$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
& .\mvnw.cmd -q compile javafx:run
if ($LASTEXITCODE -ne 0) { throw "Lab 2 FTP Client dừng với lỗi." }
