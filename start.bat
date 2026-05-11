@echo off
setlocal

powershell -ExecutionPolicy Bypass -File "%~dp0start.ps1" %*
exit /b %errorlevel%