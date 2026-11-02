$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$version = "9.5.0"
$jar = "lib\mysql-connector-j-$version.jar"
if (-not (Test-Path $jar)) {
    New-Item -ItemType Directory -Force lib | Out-Null
    $url = "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/$version/mysql-connector-j-$version.jar"
    Write-Host "Dang tai MySQL Connector/J $version..."
    Invoke-WebRequest -Uri $url -OutFile $jar
}

New-Item -ItemType Directory -Force out | Out-Null
$sources = Get-ChildItem -Path src -Recurse -Filter *.java
javac --release 17 -encoding UTF-8 -Xlint:all -d out $sources.FullName
if ($LASTEXITCODE -ne 0) {
    throw "Bien dich that bai."
}
Write-Host "Bien dich thanh cong."
