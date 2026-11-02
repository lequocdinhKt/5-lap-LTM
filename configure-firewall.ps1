#Requires -RunAsAdministrator
$ErrorActionPreference = "Stop"

$rules = @(
    @{ Name = "LTM Lab1 TCP"; Protocol = "TCP"; Ports = "5000" },
    @{ Name = "LTM Lab1 UDP"; Protocol = "UDP"; Ports = "5001-5002" },
    @{ Name = "LTM Lab2 FTP Control"; Protocol = "TCP"; Ports = "2121" },
    @{ Name = "LTM Lab2 FTP Passive"; Protocol = "TCP"; Ports = "30000-30020" },
    @{ Name = "LTM Lab3 SMTP POP3"; Protocol = "TCP"; Ports = "3025,3110" },
    @{ Name = "LTM Lab4 HTTP"; Protocol = "TCP"; Ports = "8080" }
)

foreach ($rule in $rules) {
    $existing = Get-NetFirewallRule -DisplayName $rule.Name -ErrorAction SilentlyContinue
    if ($null -eq $existing) {
        New-NetFirewallRule -DisplayName $rule.Name -Direction Inbound -Action Allow `
            -Profile Private -Protocol $rule.Protocol -LocalPort $rule.Ports | Out-Null
        Write-Host "Đã thêm: $($rule.Name)"
    } else {
        Write-Host "Đã tồn tại: $($rule.Name)"
    }
}
