param(
    [string]$ServerAddress = "",
    [string]$Username = "",
    [string]$Password = "",
    [switch]$Connect
)

$ErrorActionPreference = "Stop"
$taskRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location -LiteralPath $taskRoot

$javaFxVersion = "21.0.6"
$javaFxDirectory = Join-Path $taskRoot "lib\javafx"
New-Item -ItemType Directory -Force $javaFxDirectory | Out-Null

foreach ($artifact in @("javafx-base", "javafx-graphics", "javafx-controls")) {
    foreach ($classifier in @("", "-win")) {
        $fileName = "$artifact-$javaFxVersion$classifier.jar"
        $destination = Join-Path $javaFxDirectory $fileName
        if (-not (Test-Path -LiteralPath $destination)) {
            $url = "https://repo1.maven.org/maven2/org/openjfx/$artifact/$javaFxVersion/$fileName"
            Write-Host "Dang tai $fileName..."
            Invoke-WebRequest -Uri $url -OutFile $destination
        }
    }
}

$mysqlVersion = "9.5.0"
$mysqlJar = Join-Path $taskRoot "lib\mysql-connector-j-$mysqlVersion.jar"
if (-not (Test-Path -LiteralPath $mysqlJar)) {
    $url = "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/$mysqlVersion/mysql-connector-j-$mysqlVersion.jar"
    Write-Host "Dang tai MySQL Connector/J $mysqlVersion..."
    Invoke-WebRequest -Uri $url -OutFile $mysqlJar
}

New-Item -ItemType Directory -Force "out" | Out-Null
Get-ChildItem -Path "out" -Recurse -Filter "*.class" | Remove-Item -Force
$sourceFiles = Get-ChildItem -Path "src" -Recurse -Filter "*.java"
javac --release 17 -encoding UTF-8 --module-path $javaFxDirectory --add-modules javafx.controls `
    -cp $mysqlJar -d "out" $sourceFiles.FullName
if ($LASTEXITCODE -ne 0) {
    throw "Bien dich that bai (javac exit code $LASTEXITCODE)."
}

$cp = "out;lib\mysql-connector-j-9.5.0.jar"
$clientArguments = @("--module-path", "lib\javafx", "--add-modules", "javafx.controls",
    "-cp", $cp, "lab1.client.ChatClient")
if ($ServerAddress -and $Username) {
    $clientArguments += @($ServerAddress, "5000", $Username, $Password)
    if ($Connect) { $clientArguments += "--connect" }
}
Start-Process -FilePath "javaw" -WorkingDirectory $taskRoot `
    -ArgumentList $clientArguments

Write-Host "Da mo giao dien Chat Client." -ForegroundColor Green
