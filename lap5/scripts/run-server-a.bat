@echo off
setlocal
pushd "%~dp0.."
if not exist "target\Lap8_LTM-1.0.0-SNAPSHOT-all.jar" (
    echo Missing JAR. Run mvn clean test package in the project folder first.
    pause
    popd
    exit /b 1
)
if not exist "server-a.properties" (
    echo Missing server-a.properties. Copy src\main\resources\server-a.properties.example to the project folder and edit it first.
    pause
    popd
    exit /b 1
)
java -cp "target\Lap8_LTM-1.0.0-SNAPSHOT-all.jar" com.warehouse.server.ServerLauncher "server-a.properties"
set "run_exit_code=%errorlevel%"
if not "%run_exit_code%"=="0" pause
popd
exit /b %run_exit_code%
