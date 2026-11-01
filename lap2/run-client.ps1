$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
& .\mvnw.cmd -q compile exec:java "-Dexec.mainClass=lab2.client.FtpClient"
if ($LASTEXITCODE -ne 0) { throw "Lab 2 FTP Client dừng với lỗi." }
