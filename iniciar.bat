@echo off
rem Arranca el backend y luego el frontend de AgroTech (ver iniciar.ps1).
rem Se puede abrir con doble clic.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0iniciar.ps1"
echo.
pause
