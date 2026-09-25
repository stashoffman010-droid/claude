@echo off
REM Double-click this to build the full modpack.
setlocal
cd /d "%~dp0"
echo Building FPS Modpack Optimized for Minecraft 1.21.11 (Fabric)...
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "tools\build-pack.ps1"
if errorlevel 1 (
  echo.
  echo Build FAILED. Check that you are online and try again.
) else (
  echo.
  echo Done. Your pack is in the "dist" folder - import the .mrpack into the Modrinth App.
)
echo.
pause
