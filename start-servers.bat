@echo off
echo ========================================
echo Starting Investment Portfolio Application
echo ========================================
echo.

echo [1/2] Starting Backend Server (Spring Boot on port 8080)...
start "Backend Server" cmd /k "cd /d c:\Personal\Investment\Inv_code_proj\backend && java -jar target\portfolio-1.0.0.jar"

echo Waiting 10 seconds for backend to initialize...
timeout /t 10 /nobreak > nul

echo.
echo [2/2] Starting Frontend Server (Angular on port 4200)...
start "Frontend Server" cmd /k "cd /d c:\Personal\Investment\Inv_code_proj\frontend && node C:\Users\Sudhir_Mohapatro\AppData\Roaming\npm\node_modules\@angular\cli\bin\ng.js serve --open"

echo.
echo ========================================
echo Both servers are starting...
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:4200
echo H2 Console: http://localhost:8080/h2-console
echo ========================================
echo.
echo Press any key to exit this window (servers will continue running)...
pause > nul
