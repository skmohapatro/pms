@echo off
echo ========================================
echo Stopping Investment Portfolio Containers
echo ========================================
echo.

podman-compose -f podman-compose.yml down

echo.
echo ========================================
echo All containers stopped
echo ========================================
pause
