[CmdletBinding()]
param([switch]$Console)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if ($Console) {
    & .\mvnw.cmd -q compile exec:java "-Dexec.mainClass=lab1.server.ChatServer" `
        "-Dexec.args=--console 5000 5002 5001"
} else {
    & .\mvnw.cmd -q compile javafx:run "-Dapp.mainClass=lab1.server.ChatServer"
}
if ($LASTEXITCODE -ne 0) { throw "Lab 1 Chat Server dừng với lỗi." }
