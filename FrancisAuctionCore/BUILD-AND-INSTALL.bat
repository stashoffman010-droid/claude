@echo off
REM ===========================================================================
REM  Francis Auction Core - build the plugin and drop it in your server.
REM
REM  1. Put your server's plugins folder on the PLUGINS_DIR line below.
REM  2. Double-click this file.
REM
REM  Needs Java 17+ and Maven on PATH. Leave PLUGINS_DIR empty to just build
REM  the jar and leave it in the "target" folder.
REM ===========================================================================
setlocal
set "PLUGINS_DIR="
REM  Example:
REM  set "PLUGINS_DIR=C:\Francisekk\plugins"

cd /d "%~dp0"
echo Building Francis Auction Core...
echo.
call mvn -q clean package
if errorlevel 1 (
  echo.
  echo Build FAILED. Check that Java 17+ and Maven are installed and that you are online.
  echo.
  pause
  exit /b 1
)

set "JAR=target\FrancisAuctionCore-2.0.0.jar"
if not exist "%JAR%" (
  echo.
  echo Build finished but %JAR% is missing. Check the Maven output above.
  echo.
  pause
  exit /b 1
)

if "%PLUGINS_DIR%"=="" (
  echo.
  echo Done. The plugin is at "%~dp0%JAR%".
  echo Set PLUGINS_DIR at the top of this file to have it copied automatically.
  echo.
  pause
  exit /b 0
)

if not exist "%PLUGINS_DIR%" (
  echo.
  echo Done building, but the folder "%PLUGINS_DIR%" does not exist.
  echo Fix PLUGINS_DIR at the top of this file, or copy "%~dp0%JAR%" over yourself.
  echo.
  pause
  exit /b 1
)

REM  Remove the old auction plugin so the server does not load both.
if exist "%PLUGINS_DIR%\DonutAuction-1.2.jar" del /q "%PLUGINS_DIR%\DonutAuction-1.2.jar"
copy /y "%JAR%" "%PLUGINS_DIR%\" >nul
if errorlevel 1 (
  echo.
  echo Could not copy into "%PLUGINS_DIR%". Is the server running, or the folder read-only?
) else (
  echo.
  echo Done. FrancisAuctionCore-2.0.0.jar is in "%PLUGINS_DIR%".
  echo Restart the server, then run /pl to check it.
)
echo.
pause
