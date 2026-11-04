$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$labs = @("lap1", "lap2", "lap3", "lap4", "lap5")
$failed = @()

foreach ($lab in $labs) {
    Write-Host "`n===== BUILD $($lab.ToUpperInvariant()) =====" -ForegroundColor Cyan
    Push-Location $lab
    try {
        & .\mvnw.cmd -q clean verify
        if ($LASTEXITCODE -ne 0) {
            $failed += $lab
        }
    } finally {
        Pop-Location
    }
}

if ($failed.Count -gt 0) {
    throw "Build thất bại: $($failed -join ', ')"
}

Write-Host "`n===== LAB 3 LOCAL SMTP/POP3 TEST =====" -ForegroundColor Cyan
Push-Location "lap3"
try {
    & .\mvnw.cmd -q test-compile exec:java "-Dexec.mainClass=lab3.LocalMailIntegrationTest" "-Dexec.classpathScope=test"
    if ($LASTEXITCODE -ne 0) {
        throw "Kiểm thử SMTP/POP3 của lab3 thất bại."
    }
} finally {
    Pop-Location
}

Write-Host "`n===== LAB 4 HTTP INTEGRATION TEST =====" -ForegroundColor Cyan
Push-Location "lap4"
try {
    & .\mvnw.cmd -q test-compile exec:java "-Dexec.mainClass=lab4.test.HttpIntegrationTest" "-Dexec.classpathScope=test"
    if ($LASTEXITCODE -ne 0) {
        throw "Kiểm thử HTTP của lab4 thất bại."
    }
} finally {
    Pop-Location
}

Write-Host "`nTất cả Lab 1-5 đã build thành công." -ForegroundColor Green
