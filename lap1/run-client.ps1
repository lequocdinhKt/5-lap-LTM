[CmdletBinding()]
param(
    [string]$ServerHost = "localhost",
    [ValidateRange(1, 65535)][int]$Port = 5000,
    [string]$Username = "",
    [string]$Password = "",
    [switch]$AutoConnect,
    [switch]$Register
)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$clientArgs = @($ServerHost, $Port, $Username, $Password)
if ($AutoConnect) { $clientArgs += "--connect" }
if ($Register) { $clientArgs += "--register" }
$argumentLine = ($clientArgs | ForEach-Object { '"' + ($_ -replace '"', '\"') + '"' }) -join ' '
& .\mvnw.cmd -q compile javafx:run "-Dapp.mainClass=lab1.client.ChatClient" "-Djavafx.args=$argumentLine"
if ($LASTEXITCODE -ne 0) { throw "Lab 1 Chat Client dừng với lỗi." }
