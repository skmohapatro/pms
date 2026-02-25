@echo off
echo ========================================
echo Starting Investment Portfolio with Podman (Detached Mode)
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
echo Building and starting all containers in background...
echo This may take several minutes on first run...
echo.

podman-compose -f podman-compose.yml up -d --build

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo All containers started successfully!
    echo Chat Backend: http://localhost:5000
    echo Backend:      http://localhost:8080
    echo Frontend:     http://localhost:4200
    echo H2 Console:   http://localhost:8080/h2-console
    echo ========================================
    echo.
    echo To view logs: podman-compose -f podman-compose.yml logs -f
    echo To stop:      podman-compose -f podman-compose.yml down
    echo.
) else (
    echo.
    echo ERROR: Failed to start containers
    echo Check the error messages above
    echo.
)

pause
