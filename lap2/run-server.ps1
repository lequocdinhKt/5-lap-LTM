$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
& .\compile.ps1
$jar = Get-ChildItem "lib\mysql-connector-j-*.jar" | Select-Object -First 1
java -cp "out;$($jar.FullName)" lab2.server.FtpServer 2121 ftp-root
