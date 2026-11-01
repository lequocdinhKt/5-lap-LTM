$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
& .\mvnw.cmd -q compile exec:java "-Dexec.mainClass=lab2.server.FtpServer" "-Dexec.args=2121 ftp-root"
if ($LASTEXITCODE -ne 0) { throw "Lab 2 FTP Server dừng với lỗi." }
