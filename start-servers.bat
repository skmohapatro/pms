@echo off
echo ========================================
echo Starting Investment Portfolio Application
echo ========================================
echo.

echo [1/3] Starting Chat Backend (Flask on port 5000)...
start "Chat Backend" cmd /k "cd /d c:\Personal\Investment\pms\chat-backend && python app.py"

echo Waiting 5 seconds for chat backend to initialize...
timeout /t 5 /nobreak > nul

echo.
echo [2/3] Starting Backend Server (Spring Boot on port 8080)...
start "Backend Server" cmd /k "cd /d c:\Personal\Investment\pms\backend && mvn spring-boot:run"

echo Waiting 15 seconds for backend to initialize...
timeout /t 15 /nobreak > nul

echo.
echo [3/3] Starting Frontend Server (Angular on port 4200)...
start "Frontend Server" cmd /k "cd /d c:\Personal\Investment\pms\frontend && npm start"

echo.
echo ========================================
echo All servers are starting...
echo Chat Backend: http://localhost:5000
echo Backend:      http://localhost:8080
echo Frontend:     http://localhost:4200
echo H2 Console:   http://localhost:8080/h2-console
echo ========================================
echo.
echo Press any key to exit this window (servers will continue running)...
pause > nul
