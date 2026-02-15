@echo off
echo ========================================
echo Stopping Investment Portfolio Application
echo ========================================
echo.

echo [1/2] Stopping Backend Server (Port 8080)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    echo Killing process %%a on port 8080...
    taskkill /PID %%a /F /T 2>nul
)
echo Backend server stopped.

echo.
echo [2/2] Stopping Frontend Server (Port 4200)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :4200 ^| findstr LISTENING') do (
    echo Killing process %%a on port 4200...
    taskkill /PID %%a /F /T 2>nul
)
echo Frontend server stopped.

echo.
echo ========================================
echo All servers stopped.
echo ========================================
echo.
pause
