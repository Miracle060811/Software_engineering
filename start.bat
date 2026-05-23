@echo off
setlocal EnableExtensions DisableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "SCRIPT_PATH=%SCRIPT_DIR%start.ps1"
set "POWERSHELL_CMD="

if "%~1"=="/?" goto :usage
if /I "%~1"=="-h" goto :usage
if /I "%~1"=="--help" goto :usage

if not exist "%SCRIPT_PATH%" (
	echo [TravelMate] start.ps1 was not found next to start.bat.
	exit /b 1
)

if exist "%ProgramFiles%\PowerShell\7\pwsh.exe" (
	set "POWERSHELL_CMD=%ProgramFiles%\PowerShell\7\pwsh.exe"
)

if not defined POWERSHELL_CMD if exist "%ProgramFiles(x86)%\PowerShell\7\pwsh.exe" (
	set "POWERSHELL_CMD=%ProgramFiles(x86)%\PowerShell\7\pwsh.exe"
)

if not defined POWERSHELL_CMD if exist "%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" (
	set "POWERSHELL_CMD=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
)

if not defined POWERSHELL_CMD (
	where /Q pwsh.exe
	if not errorlevel 1 set "POWERSHELL_CMD=pwsh.exe"
)

if not defined POWERSHELL_CMD (
	where /Q powershell.exe
	if not errorlevel 1 set "POWERSHELL_CMD=powershell.exe"
)

if not defined POWERSHELL_CMD (
	echo [TravelMate] PowerShell was not found. Install Windows PowerShell or PowerShell 7, then retry.
	exit /b 1
)

"%POWERSHELL_CMD%" -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_PATH%" %*
set "EXIT_CODE=%ERRORLEVEL%"
exit /b %EXIT_CODE%

:usage
echo TravelMate Windows launcher
echo.
echo Usage:
echo   start.bat [options]
echo.
echo Options are passed through to start.ps1:
echo   -DbPassword ^<password^>       MySQL password
echo   -DeepseekApiKey ^<key^>        DeepSeek API key
echo   -BackendOnly                 Start backend only
echo   -FrontendOnly                Start frontend only
echo   -SkipRedis                   Skip Redis auto-start/check
echo   -SkipFrontendInstall         Do not run npm install automatically
echo   -DryRun                      Print commands without starting services
echo.
echo Examples:
echo   start.bat
echo   start.bat -DbPassword 061010
echo   start.bat -BackendOnly -SkipRedis
echo   start.bat -FrontendOnly -SkipFrontendInstall
exit /b 0
