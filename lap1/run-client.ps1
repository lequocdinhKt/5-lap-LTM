$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
& .\mvnw.cmd -q compile javafx:run "-Dapp.mainClass=lab1.client.ChatClient"
if ($LASTEXITCODE -ne 0) { throw "Lab 1 Chat Client dừng với lỗi." }
