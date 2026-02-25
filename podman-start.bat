@echo off
echo ========================================
echo Starting Investment Portfolio with Podman
echo ========================================
echo.

echo Checking if Podman is installed...
podman --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Podman is not installed or not in PATH
    echo Please install Podman from: https://podman.io/getting-started/installation
    pause
    exit /b 1
)

echo.
echo Starting Podman machine (if not already running)...
podman machine start >nul 2>&1

echo.
echo Building and starting all containers...
echo This may take several minutes on first run...
echo.

podman-compose -f podman-compose.yml up --build

echo.
echo ========================================
echo Podman containers stopped
echo ========================================
pause
