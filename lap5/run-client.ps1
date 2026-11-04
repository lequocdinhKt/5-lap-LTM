[CmdletBinding()]
param(
    [string]$ServerHost = "127.0.0.1",
    [ValidateRange(1, 65535)][int]$Port = 1099,
    [ValidateSet("client-a", "client-b")][string]$ClientId = "client-a",
    [string]$Username = ""
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$arguments = "--server-host=$ServerHost --port=$Port --client-id=$ClientId --username=$Username"
& .\mvnw.cmd -q compile javafx:run "-Dapp.mainClass=lab5.client.ClientLauncher" "-Djavafx.args=$arguments"
if ($LASTEXITCODE -ne 0) { throw "Lab 5 RMI Client dừng với lỗi." }
