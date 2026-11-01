@echo off
cd /d "%~dp0"

where java >nul 2>&1
if errorlevel 1 (
  echo Loi: Chua cai JDK. Can JDK 17 tro len.
  echo Tai tai: https://adoptium.net/
  pause
  exit /b 1
)

if not exist "db.properties" (
  copy /y "db.properties.example" "db.properties" >nul
  echo Da tao db.properties. Hay nhap mat khau MySQL vao file nay roi chay lai.
  pause
  exit /b 1
)

echo Dang chay Lab 3 - Mail Client...
call "%~dp0mvnw.cmd" -q javafx:run
if errorlevel 1 (
  echo.
  echo Neu loi, mo project bang IntelliJ IDEA roi Run file MailApp.java
  pause
)
