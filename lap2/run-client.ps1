$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
& .\compile.ps1
java -cp out lab2.client.FtpClient
